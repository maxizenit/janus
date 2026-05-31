#!/usr/bin/env bash
# Experiment v2 — НФТ-2: propagation latency.
#   Сц.1  — policy change in Policy Store -> visible in SDK (via Sidecar cache).
#   Сц.1b — state override (Admin UI source) in State Store -> visible in SDK.
#
# For each run we change a field via gRPC (grpcurl), stamp t0, then poll the SDK
# debug endpoint (/api/debug/degradations) until the new value is reflected (t1).
# delay = t1 - t0. The field is toggled between two values each run so every run
# observes a real change. For Сц.1b the Evaluator is scaled to 0 so it does not
# overwrite the manual override.
#
# Output: per-run CSV (scenario, run, delay_ms).
set -uo pipefail

EXP_DIR="$(cd "$(dirname "$0")/.." && pwd)"
NAMESPACE="janus"
RUNS="${RUNS:-10}"
DEG="recommendations.fetch"
PS_SVC="org.janus.api.policystore.PolicyStoreService"
SS_SVC="org.janus.api.statestore.StateStoreService"
GRPCURL_IMAGE="${GRPCURL_IMAGE:-fullstorydev/grpcurl:v1.9.1-alpine}"

DC=$(kubectl get pod -n "${NAMESPACE}" -l app.kubernetes.io/name=demo-client \
  --field-selector=status.phase=Running -o jsonpath='{.items[0].metadata.name}')

# Long-lived grpcurl pod so per-run gRPC calls are fast (no Job startup overhead).
kubectl -n "${NAMESPACE}" delete pod grpcurl-nfr2 --ignore-not-found --grace-period=0 --force >/dev/null 2>&1
kubectl -n "${NAMESPACE}" run grpcurl-nfr2 --image="${GRPCURL_IMAGE}" --restart=Never \
  --command -- sleep 7200 >/dev/null
kubectl -n "${NAMESPACE}" wait --for=condition=Ready pod/grpcurl-nfr2 --timeout=60s >/dev/null

grpc() { kubectl -n "${NAMESPACE}" exec -i grpcurl-nfr2 -- grpcurl -plaintext -d @ "$1" "$2" >/dev/null 2>&1; }
sdk_field() {
  kubectl -n "${NAMESPACE}" exec "${DC}" -- curl -s http://localhost:8091/api/debug/degradations \
    | jq -r ".states[\"${DEG}\"]${1} // \"NA\""
}

TS=$(date +%Y%m%d-%H%M%S)
OUT="${EXP_DIR}/k6/results/${TS}-nfr2-propagation"
mkdir -p "${OUT}"
DIGEST="${OUT}/summary.csv"
echo "scenario,run,delay_ms" >"${DIGEST}"

echo "########## Сц.1: policy propagation (Policy Store -> SDK) ##########"
for i in $(seq 1 "${RUNS}"); do
  target=$([ $((i % 2)) -eq 0 ] && echo 0.9 || echo 0.7)
  payload="{\"degradationId\":\"${DEG}\",\"maxFallbackRatio\":${target},\"updateMask\":\"maxFallbackRatio\"}"
  t0=$(date +%s%3N)
  printf '%s' "${payload}" | grpc policy-store:9090 "${PS_SVC}/UpdateDegradationPolicy"
  for _ in $(seq 1 300); do
    [ "$(sdk_field .maxFallbackRatio)" = "${target}" ] && break
    sleep 0.1
  done
  t1=$(date +%s%3N)
  echo "sc1,${i},$((t1 - t0))" >>"${DIGEST}"
  echo "  sc1 run ${i}/${RUNS}: rmax->${target} delay=$((t1 - t0))ms"
done

echo "########## Сц.1b: state override propagation (Admin UI -> SDK) ##########"
kubectl -n "${NAMESPACE}" scale deployment/evaluator --replicas=0 >/dev/null
kubectl -n "${NAMESPACE}" rollout status deployment/evaluator --timeout=2m >/dev/null
for i in $(seq 1 "${RUNS}"); do
  target=$([ $((i % 2)) -eq 0 ] && echo 0.5 || echo 0.3)
  payload="{\"source\":\"ADMIN_UI\",\"updates\":[{\"degradationId\":\"${DEG}\",\"value\":${target},\"ttl\":\"120s\"}]}"
  t0=$(date +%s%3N)
  printf '%s' "${payload}" | grpc state-store:9090 "${SS_SVC}/UpdateDegradationStates"
  for _ in $(seq 1 300); do
    [ "$(sdk_field .value)" = "${target}" ] && break
    sleep 0.1
  done
  t1=$(date +%s%3N)
  echo "sc1b,${i},$((t1 - t0))" >>"${DIGEST}"
  echo "  sc1b run ${i}/${RUNS}: value->${target} delay=$((t1 - t0))ms"
done
kubectl -n "${NAMESPACE}" scale deployment/evaluator --replicas=1 >/dev/null

kubectl -n "${NAMESPACE}" delete pod grpcurl-nfr2 --ignore-not-found --grace-period=0 --force >/dev/null 2>&1
echo
echo "Done. НФТ-2 digest -> ${DIGEST}"
