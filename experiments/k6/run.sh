#!/usr/bin/env bash
# Runs all k6 experiments sequentially: 4 scenarios on the demo-client deployment.
# The current demo-client configuration (C-base / C-react / C-proact / C-r4j) must
# be set externally before running this script — see demo/client/README.md.
# Per job: stdout log + extracted JSON summary. After all jobs: a CSV digest.

set -euo pipefail

if ! command -v jq >/dev/null 2>&1; then
  echo "ERROR: jq is required to aggregate k6 results (https://jqlang.github.io/jq/)." >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
NAMESPACE="janus"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
RESULTS_DIR="${SCRIPT_DIR}/results/${TIMESTAMP}"
mkdir -p "${RESULTS_DIR}"

# Defaults (overridable via env)
export RPS="${RPS:-50}"
export DURATION="${DURATION:-60s}"
export PRE_VUS="${PRE_VUS:-20}"
export MAX_VUS="${MAX_VUS:-200}"
export TARGET_PORT="${TARGET_PORT:-8091}"

# Re-apply ConfigMap with the latest scripts
kubectl apply -k "${SCRIPT_DIR}"

# scenario_id|SCENARIO_MODE|SCENARIO_DELAY_MS|SCENARIO_STATUS|SCENARIO_ERROR_RATE
SCENARIOS=(
  "baseline|ok|0|0|0"
  "timeout|slow|5000|0|0"
  "errors|error|0|500|1.0"
  "flaky|flaky|0|500|0.5"
)

TARGET="${TARGET:-demo-client}"
CONFIG_TAG="${CONFIG_TAG:-unknown}"

run_job() {
  local scenario_id="$1"
  IFS='|' read -r _ mode delay_ms status error_rate <<<"$2"

  export SCENARIO_MODE="${mode}"
  export SCENARIO_DELAY_MS="${delay_ms}"
  export SCENARIO_STATUS="${status}"
  export SCENARIO_ERROR_RATE="${error_rate}"
  export TARGET_HOST="${TARGET}"
  export JOB_NAME="k6-${CONFIG_TAG}-${scenario_id}"

  local log_file="${RESULTS_DIR}/${JOB_NAME}.log"
  local json_file="${RESULTS_DIR}/${JOB_NAME}.json"

  echo
  echo "==> ${JOB_NAME}"
  echo "    config=${CONFIG_TAG} scenario=${SCENARIO_MODE} target=${TARGET_HOST}"
  echo "    log=${log_file}"

  kubectl -n "${NAMESPACE}" delete job "${JOB_NAME}" --ignore-not-found --wait=true >/dev/null

  envsubst <"${SCRIPT_DIR}/job.yaml" | kubectl -n "${NAMESPACE}" apply -f - >/dev/null

  if ! kubectl -n "${NAMESPACE}" wait --for=condition=complete --timeout=10m "job/${JOB_NAME}" >/dev/null 2>&1; then
    if kubectl -n "${NAMESPACE}" wait --for=condition=failed --timeout=5s "job/${JOB_NAME}" >/dev/null 2>&1; then
      echo "    ! Job failed"
    else
      echo "    ! Job did not complete in time"
    fi
  fi

  kubectl -n "${NAMESPACE}" logs "job/${JOB_NAME}" --tail=-1 >"${log_file}" 2>&1 || true

  # Extract the JSON-only block emitted by handleSummary() in recommendations.js
  if sed -n '/=== JSON SUMMARY START ===/,/=== JSON SUMMARY END ===/p' "${log_file}" \
      | sed '1d;$d' > "${json_file}"; then
    if [[ ! -s "${json_file}" ]]; then
      echo "    ! No JSON summary found in logs"
      rm -f "${json_file}"
    fi
  fi

  kubectl -n "${NAMESPACE}" delete job "${JOB_NAME}" --ignore-not-found >/dev/null
}

for scenario_line in "${SCENARIOS[@]}"; do
  scenario_id="${scenario_line%%|*}"
  run_job "${scenario_id}" "${scenario_line}"
done

# Aggregate JSON summaries into a digest CSV
DIGEST="${RESULTS_DIR}/summary.csv"
{
  echo "experiment,config,scenario,iterations,reqs_per_sec,fail_rate,p50_ms,p95_ms,p99_ms,max_ms"
  for json_file in "${RESULTS_DIR}"/k6-*.json; do
    [[ -f "${json_file}" ]] || continue
    name="$(basename "${json_file}" .json)"
    rest="${name#k6-}"
    config="${rest%%-*}"
    scenario="${rest#*-}"

    jq -r --arg n "${name}" --arg c "${config}" --arg s "${scenario}" '
      [
        $n,
        $c,
        $s,
        (.metrics.iterations.values.count // 0),
        ((.metrics.http_reqs.values.rate // 0) | tostring),
        ((.metrics.http_req_failed.values.rate // 0) | tostring),
        ((.metrics.http_req_duration.values.med // 0) | tostring),
        ((.metrics.http_req_duration.values["p(95)"] // 0) | tostring),
        ((.metrics.http_req_duration.values["p(99)"] // 0) | tostring),
        ((.metrics.http_req_duration.values.max // 0) | tostring)
      ] | @csv
    ' "${json_file}"
  done
} >"${DIGEST}"

echo
echo "All experiments finished. Results: ${RESULTS_DIR}"
echo "Digest: ${DIGEST}"
