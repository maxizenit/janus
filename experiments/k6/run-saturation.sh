#!/usr/bin/env bash
# Experiment v2 — phase 3: pre-emptive degradation under a saturating ramp.
#
# The dependency (demo-server, Mode.SATURATE) owns a bounded resource of
# SCENARIO_MAX_CONCURRENT permits, each held PROCESSING_MS. A ramping arrival
# rate drives it through its saturation point (rps_sat = maxConcurrent*1000/ms).
# The dependency exposes `demo_saturation` in [0,1]. We compare three strategies:
#   base       — no protection: latency spikes once the resource saturates.
#   r4j-slow   — latency-aware circuit breaker: reacts to the latency spike
#                (the SYMPTOM), so it opens only AFTER saturation has hit.
#   proact     — proactive policy on demo_saturation (the CAUSE): degrades
#                pre-emptively as utilisation nears the threshold, before
#                per-call latency rises — the signal a CB cannot observe.
#
# One ramping k6 job per config. Set ONLY_CONFIG=base|r4j-slow|proact to run one.
#
# Tunables (env): MAX_CONCURRENT (10), PROCESSING_MS (200), RAMP_STAGES, RAMP_START,
#   PRE_VUS (100), MAX_VUS (800).
set -uo pipefail

command -v jq >/dev/null 2>&1 || { echo "ERROR: jq is required" >&2; exit 1; }
command -v envsubst >/dev/null 2>&1 || { echo "ERROR: envsubst is required" >&2; exit 1; }

EXP_DIR="$(cd "$(dirname "$0")/.." && pwd)"
K6_DIR="${EXP_DIR}/k6"
POLICY_DIR="${EXP_DIR}/policies"
NAMESPACE="janus"

export PRE_VUS="${PRE_VUS:-100}"
export MAX_VUS="${MAX_VUS:-800}"
export RAMP_START="${RAMP_START:-5}"
# target:duration pairs. Default ramps through rps_sat=50 (N=10, 200ms) into deep
# saturation, then drains.
export RAMP_STAGES="${RAMP_STAGES:-50:60s,120:120s,5:15s}"
export SCENARIO_MODE="saturate"
export SCENARIO_DELAY_MS="${PROCESSING_MS:-200}"
export SCENARIO_MAX_CONCURRENT="${MAX_CONCURRENT:-10}"
export SCENARIO_STATUS="200"
export SCENARIO_ERROR_RATE="0"
export RESET_ON_TEARDOWN="true"
export TARGET_HOST="${TARGET:-demo-client}"
export TARGET_PORT="${TARGET_PORT:-8091}"
export DURATION="0s" # unused under ramp (options.js uses stages), but job.yaml refs it

ONLY_CONFIG="${ONLY_CONFIG:-}"
should_run() { [[ -z "${ONLY_CONFIG}" || "${ONLY_CONFIG}" == "$1" ]]; }

set_config() {
  local profile="$1" janus_enabled="$2" r4j_enabled="$3" slowcall="${4:-off}"
  if [[ -z "${profile}" ]]; then
    kubectl -n "${NAMESPACE}" set env deployment/demo-client SPRING_PROFILES_ACTIVE- >/dev/null
  else
    kubectl -n "${NAMESPACE}" set env deployment/demo-client SPRING_PROFILES_ACTIVE="${profile}" >/dev/null
  fi
  kubectl -n "${NAMESPACE}" set env deployment/demo-client \
    JANUS_SDK_ENABLED="${janus_enabled}" RESILIENCE4J_ENABLED="${r4j_enabled}" >/dev/null
  local d="RESILIENCE4J_CIRCUITBREAKER_INSTANCES_RECOMMENDATIONS_SLOWCALLDURATIONTHRESHOLD"
  local r="RESILIENCE4J_CIRCUITBREAKER_INSTANCES_RECOMMENDATIONS_SLOWCALLRATETHRESHOLD"
  if [[ "${slowcall}" == "on" ]]; then
    kubectl -n "${NAMESPACE}" set env deployment/demo-client "${d}=1000ms" "${r}=50" >/dev/null
  else
    kubectl -n "${NAMESPACE}" set env deployment/demo-client "${d}-" "${r}-" >/dev/null
  fi
  echo "  rollout..."
  kubectl -n "${NAMESPACE}" rollout status deployment/demo-client --timeout=3m
}

run_ramp() {
  local tag="$1"
  local ts outdir job log json
  ts="$(date +%Y%m%d-%H%M%S)"
  outdir="${K6_DIR}/results/${ts}-${tag}-sat"
  mkdir -p "${outdir}"
  kubectl apply -k "${K6_DIR}" >/dev/null
  job="k6-${tag}-saturate"
  export JOB_NAME="${job}"
  kubectl -n "${NAMESPACE}" delete job "${job}" --ignore-not-found --wait=true >/dev/null 2>&1
  envsubst <"${K6_DIR}/job.yaml" | kubectl -n "${NAMESPACE}" apply -f - >/dev/null
  echo "  [${tag}] ramp running (stages=${RAMP_STAGES}, N=${SCENARIO_MAX_CONCURRENT}, ms=${SCENARIO_DELAY_MS})..."
  kubectl -n "${NAMESPACE}" wait --for=condition=complete --timeout=15m "job/${job}" >/dev/null 2>&1 \
    || echo "  ! ${job} did not complete cleanly"
  log="$(kubectl -n "${NAMESPACE}" logs "job/${job}" --tail=-1 2>/dev/null)"
  json="$(printf '%s\n' "${log}" | sed -n '/=== JSON SUMMARY START ===/,/=== JSON SUMMARY END ===/p' | sed '1d;$d')"
  kubectl -n "${NAMESPACE}" delete job "${job}" --ignore-not-found >/dev/null 2>&1
  if [[ -z "${json}" ]]; then echo "  ! [${tag}] no JSON summary"; return; fi
  printf '%s' "${json}" >"${outdir}/summary.json"
  printf '  [%s] ' "${tag}"
  printf '%s' "${json}" | jq -rc '{rps:(.metrics.http_reqs.values.rate),fail:(.metrics.http_req_failed.values.rate),p50:(.metrics.http_req_duration.values.med),p95:(.metrics.http_req_duration.values["p(95)"]),p99:(.metrics.http_req_duration.values["p(99)"]),dropped:(.metrics.dropped_iterations.values.count),fallback:(.metrics.fallback_rate.values.rate)}'
  echo "  [${tag}] summary -> ${outdir}/summary.json"
}

if should_run base; then
  echo "########## SAT C-base (no protection) ##########"
  set_config "" "false" "false" "off"
  run_ramp base
fi

if should_run r4j-slow; then
  echo "########## SAT C-r4j-slow (latency-aware CB — reacts to symptom) ##########"
  set_config "r4j" "false" "true" "on"
  run_ramp r4j-slow
fi

if should_run proact; then
  echo "########## SAT C-proact (saturation policy — reacts to cause) ##########"
  POLICY_FILE="${POLICY_DIR}/recommendations-fetch-proactive-saturation.json" "${POLICY_DIR}/seed.sh" \
    || { echo "ERROR: saturation policy seed failed — aborting" >&2; exit 1; }
  set_config "janus" "true" "false" "off"
  run_ramp proact
fi

echo
echo "Done. Saturation (phase 3) results under ${K6_DIR}/results/*-sat/"
