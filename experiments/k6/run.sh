#!/usr/bin/env bash
# Experiment v2 — measurement sweep for the CURRENTLY deployed demo-client
# configuration (C-base / C-react / C-proact / C-r4j; set externally — see
# experiments/run-all-configs.sh and ../policies).
#
# Per scenario: an optional warmup (lets the JVM, the Resilience4j circuit
# and/or the Evaluator reach steady state) followed by RUNS measurement runs.
# Each measurement run is a separate k6 job = one statistical sample. The
# demo-server fault mode is held across warmup + all runs of a scenario
# (RESET_ON_TEARDOWN=false), so the proactive degradation level does not decay
# between samples; a final reset job returns the server to OK.
#
# Load model: open (constant-arrival-rate, see scripts/options.js). Effective
# rps and dropped_iterations are recorded per run alongside latency percentiles.
#
# Output: per-run k6 JSON summaries + a per-run CSV digest under results/.
#
# Tunables (env): CONFIG_TAG, RUNS (10), WARMUP_DURATION (60s), DURATION (90s),
# RPS (50), PRE_VUS (50), MAX_VUS (300).
set -uo pipefail

command -v jq >/dev/null 2>&1 || { echo "ERROR: jq is required" >&2; exit 1; }
command -v envsubst >/dev/null 2>&1 || { echo "ERROR: envsubst is required" >&2; exit 1; }

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
NAMESPACE="janus"
CONFIG_TAG="${CONFIG_TAG:-unknown}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
RESULTS_DIR="${SCRIPT_DIR}/results/${TIMESTAMP}-${CONFIG_TAG}"
mkdir -p "${RESULTS_DIR}"

export RPS="${RPS:-50}"
export DURATION="${DURATION:-90s}"
export WARMUP_DURATION="${WARMUP_DURATION:-60s}"
export PRE_VUS="${PRE_VUS:-50}"
export MAX_VUS="${MAX_VUS:-300}"
export TARGET_PORT="${TARGET_PORT:-8091}"
RUNS="${RUNS:-10}"
export TARGET_HOST="${TARGET:-demo-client}"

# Re-apply the k6 scripts ConfigMap so any local edits take effect.
kubectl apply -k "${SCRIPT_DIR}" >/dev/null

# scenario_id|MODE|DELAY_MS|STATUS|ERROR_RATE
SCENARIOS=(
  "baseline|ok|0|0|0"
  "errors|error|0|500|1.0"
  "flaky|flaky|0|500|0.5"
  "timeout|slow|5000|0|0"
)

DIGEST="${RESULTS_DIR}/summary.csv"
echo "config,scenario,run,iterations,reqs_per_sec,dropped_iterations,fail_rate,p50_ms,p95_ms,p99_ms,max_ms" >"${DIGEST}"

# run_k6 <job_name> <duration> <reset_on_teardown> -> prints k6 stdout
run_k6() {
  local job_name="$1" duration="$2" reset="$3"
  export JOB_NAME="${job_name}" DURATION="${duration}" RESET_ON_TEARDOWN="${reset}"
  kubectl -n "${NAMESPACE}" delete job "${job_name}" --ignore-not-found --wait=true >/dev/null 2>&1
  envsubst <"${SCRIPT_DIR}/job.yaml" | kubectl -n "${NAMESPACE}" apply -f - >/dev/null
  kubectl -n "${NAMESPACE}" wait --for=condition=complete --timeout=10m "job/${job_name}" >/dev/null 2>&1 \
    || echo "    ! ${job_name} did not complete cleanly" >&2
  kubectl -n "${NAMESPACE}" logs "job/${job_name}" --tail=-1 2>/dev/null
  kubectl -n "${NAMESPACE}" delete job "${job_name}" --ignore-not-found >/dev/null 2>&1
}

for line in "${SCENARIOS[@]}"; do
  IFS='|' read -r scenario_id mode delay status erate <<<"${line}"
  export SCENARIO_MODE="${mode}" SCENARIO_DELAY_MS="${delay}" \
         SCENARIO_STATUS="${status}" SCENARIO_ERROR_RATE="${erate}"
  echo "==> scenario=${scenario_id} config=${CONFIG_TAG} (warmup=${WARMUP_DURATION}, runs=${RUNS})"

  if [[ "${WARMUP_DURATION}" != "0" && "${WARMUP_DURATION}" != "0s" ]]; then
    echo "    warmup ${WARMUP_DURATION}..."
    run_k6 "k6-${CONFIG_TAG}-${scenario_id}-warmup" "${WARMUP_DURATION}" "false" >/dev/null 2>&1
  fi

  for i in $(seq 1 "${RUNS}"); do
    log="$(run_k6 "k6-${CONFIG_TAG}-${scenario_id}-run-${i}" "${DURATION}" "false")"
    json="$(printf '%s\n' "${log}" \
      | sed -n '/=== JSON SUMMARY START ===/,/=== JSON SUMMARY END ===/p' | sed '1d;$d')"
    if [[ -z "${json}" ]]; then
      echo "    ! run ${i}/${RUNS}: no JSON summary; skipping"
      continue
    fi
    printf '%s' "${json}" >"${RESULTS_DIR}/k6-${CONFIG_TAG}-${scenario_id}-run-${i}.json"
    printf '%s' "${json}" | jq -r --arg c "${CONFIG_TAG}" --arg s "${scenario_id}" --arg r "${i}" '
      [ $c, $s, ($r|tonumber),
        (.metrics.iterations.values.count // 0),
        (.metrics.http_reqs.values.rate // 0),
        (.metrics.dropped_iterations.values.count // 0),
        (.metrics.http_req_failed.values.rate // 0),
        (.metrics.http_req_duration.values.med // 0),
        (.metrics.http_req_duration.values["p(95)"] // 0),
        (.metrics.http_req_duration.values["p(99)"] // 0),
        (.metrics.http_req_duration.values.max // 0)
      ] | @csv' >>"${DIGEST}"
    echo "    run ${i}/${RUNS} ok"
  done
done

# Final: return demo-server to OK mode.
export SCENARIO_MODE="ok" SCENARIO_DELAY_MS="0" SCENARIO_STATUS="0" SCENARIO_ERROR_RATE="0"
run_k6 "k6-${CONFIG_TAG}-reset" "5s" "true" >/dev/null 2>&1

echo "Done (${CONFIG_TAG}). Digest: ${DIGEST}"
