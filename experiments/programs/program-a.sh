#!/usr/bin/env bash
# Experiment v2 — Program A (НФТ-4): platform-component failure isolation.
#
# Claim: the failure of any platform component does NOT bring down the protected
# application. Demo Server stays in `ok`, Demo Client runs C-proact with an active
# policy under steady load; midway through each run a component pod is killed. The
# client's success rate must stay ~100% (Sidecar holds the cached policy/state,
# the SDK falls back to its stale strategy LAST_VALUE), and we record how long the
# component takes to return to Ready (kubelet restart).
#
# Per component (policy-store, state-store, evaluator, sidecar): RUNS repetitions.
# Output: per-run CSV (component, run, fail_rate, reqs, recovery_s).
set -uo pipefail

command -v jq >/dev/null 2>&1 || { echo "ERROR: jq required" >&2; exit 1; }
command -v envsubst >/dev/null 2>&1 || { echo "ERROR: envsubst required" >&2; exit 1; }

EXP_DIR="$(cd "$(dirname "$0")/.." && pwd)"
K6_DIR="${EXP_DIR}/k6"
POLICY_DIR="${EXP_DIR}/policies"
NAMESPACE="janus"
RUNS="${RUNS:-5}"
KILL_AT="${KILL_AT:-30}" # seconds into the load before killing the pod

export RPS="${RPS:-30}"
export DURATION="${DURATION:-90s}"
export PRE_VUS="${PRE_VUS:-50}"
export MAX_VUS="${MAX_VUS:-100}"
export TARGET_HOST="demo-client"
export TARGET_PORT="8091"
export SCENARIO_MODE="ok"
export SCENARIO_DELAY_MS="0"
export SCENARIO_STATUS="0"
export SCENARIO_ERROR_RATE="0"
export SCENARIO_MAX_CONCURRENT="0"
export RAMP_STAGES=""
export RAMP_START="5"
export RESET_ON_TEARDOWN="true"

COMPONENTS="${COMPONENTS:-policy-store state-store evaluator sidecar}"

echo "Configuring demo-client = C-proact (Janus SDK) with active policy..."
POLICY_FILE="${POLICY_DIR}/recommendations-fetch-proactive.json" "${POLICY_DIR}/seed.sh" >/dev/null 2>&1 \
  || echo "  (policy seed warning — continuing)"
kubectl -n "${NAMESPACE}" set env deployment/demo-client \
  SPRING_PROFILES_ACTIVE=janus JANUS_SDK_ENABLED=true RESILIENCE4J_ENABLED=false >/dev/null
kubectl -n "${NAMESPACE}" set env deployment/demo-client \
  RESILIENCE4J_CIRCUITBREAKER_INSTANCES_RECOMMENDATIONS_SLOWCALLDURATIONTHRESHOLD- \
  RESILIENCE4J_CIRCUITBREAKER_INSTANCES_RECOMMENDATIONS_SLOWCALLRATETHRESHOLD- >/dev/null
kubectl -n "${NAMESPACE}" rollout status deployment/demo-client --timeout=3m

TS="$(date +%Y%m%d-%H%M%S)"
OUT="${K6_DIR}/results/${TS}-program-a"
mkdir -p "${OUT}"
DIGEST="${OUT}/summary.csv"
echo "component,run,fail_rate,reqs,recovery_s" >"${DIGEST}"

kubectl apply -k "${K6_DIR}" >/dev/null

for comp in ${COMPONENTS}; do
  echo "########## Program A — kill ${comp} ##########"
  for i in $(seq 1 "${RUNS}"); do
    job="k6-proga-${comp}-${i}"
    export JOB_NAME="${job}"
    kubectl -n "${NAMESPACE}" delete job "${job}" --ignore-not-found --wait=true >/dev/null 2>&1
    envsubst <"${K6_DIR}/job.yaml" | kubectl -n "${NAMESPACE}" apply -f - >/dev/null
    # Let the load settle, then kill the component pod mid-run.
    sleep "${KILL_AT}"
    killts=$(date +%s)
    pod=$(kubectl get pod -n "${NAMESPACE}" -l "app.kubernetes.io/name=${comp}" \
      --field-selector=status.phase=Running -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
    kubectl -n "${NAMESPACE}" delete pod "${pod}" --grace-period=0 --force >/dev/null 2>&1
    # Recovery: time until a Ready pod of the component is back.
    kubectl -n "${NAMESPACE}" wait --for=condition=Ready pod \
      -l "app.kubernetes.io/name=${comp}" --timeout=180s >/dev/null 2>&1
    recovery=$(( $(date +%s) - killts ))
    # Collect the k6 result.
    kubectl -n "${NAMESPACE}" wait --for=condition=complete --timeout=5m "job/${job}" >/dev/null 2>&1 \
      || echo "  ! ${job} did not complete cleanly"
    log="$(kubectl -n "${NAMESPACE}" logs "job/${job}" --tail=-1 2>/dev/null)"
    json="$(printf '%s\n' "${log}" | sed -n '/=== JSON SUMMARY START ===/,/=== JSON SUMMARY END ===/p' | sed '1d;$d')"
    kubectl -n "${NAMESPACE}" delete job "${job}" --ignore-not-found >/dev/null 2>&1
    fail=$(printf '%s' "${json}" | jq -r '.metrics.http_req_failed.values.rate // "NA"')
    reqs=$(printf '%s' "${json}" | jq -r '.metrics.http_reqs.values.count // 0')
    echo "${comp},${i},${fail},${reqs},${recovery}" >>"${DIGEST}"
    echo "  [${comp}] run ${i}/${RUNS}: fail_rate=${fail} recovery=${recovery}s"
  done
done

echo
echo "Done. Program A digest -> ${DIGEST}"
