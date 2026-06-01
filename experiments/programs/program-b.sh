#!/usr/bin/env bash
# Experiment v2 — Program B (НФТ-5): Evaluator horizontal scaling & leader failover.
#
# Evaluator runs with 3 replicas. Leadership per degradationId is a Redis lock
# (key janus:evaluator:leader:<id>, owner = pod HOSTNAME, lease 30s). We read the
# current leader from Redis, kill its pod, and measure how long until a different
# live replica takes over the lock (failover time). Repeat RUNS times. The killed
# pod is restored (kubelet) between rounds so 3 replicas are available again.
#
# Output: per-run CSV (run, old_leader, new_leader, failover_s).
set -uo pipefail

EXP_DIR="$(cd "$(dirname "$0")/.." && pwd)"
POLICY_DIR="${EXP_DIR}/policies"
NAMESPACE="janus"
RUNS="${RUNS:-5}"
DEG="${DEG:-recommendations.fetch}"
KEY="janus:evaluator:leader:${DEG}"

REDIS=$(kubectl get pod -n "${NAMESPACE}" -l app.kubernetes.io/name=redis \
  --field-selector=status.phase=Running -o jsonpath='{.items[0].metadata.name}')
rget() { kubectl exec -n "${NAMESPACE}" "${REDIS}" -- redis-cli GET "${KEY}" 2>/dev/null | tr -d '\r\n'; }

echo "Seeding policy + scaling evaluator to 3 replicas..."
POLICY_FILE="${POLICY_DIR}/recommendations-fetch-proactive.json" "${POLICY_DIR}/seed.sh" >/dev/null 2>&1 || true
kubectl -n "${NAMESPACE}" scale deployment/evaluator --replicas=3 >/dev/null
kubectl -n "${NAMESPACE}" rollout status deployment/evaluator --timeout=3m
echo "Waiting for a leader to be elected..."
for _ in $(seq 1 30); do l=$(rget); [[ -n "${l}" ]] && break; sleep 2; done
echo "  initial leader: $(rget)"

TS="$(date +%Y%m%d-%H%M%S)"
OUT="${EXP_DIR}/k6/results/${TS}-program-b"
mkdir -p "${OUT}"
DIGEST="${OUT}/summary.csv"
echo "run,old_leader,new_leader,failover_s" >"${DIGEST}"

for i in $(seq 1 "${RUNS}"); do
  leader=$(rget)
  if [[ -z "${leader}" ]]; then echo "  run ${i}: no leader, skipping"; continue; fi
  echo "  run ${i}: leader=${leader} — killing..."
  kubectl -n "${NAMESPACE}" delete pod "${leader}" --grace-period=0 --force >/dev/null 2>&1
  killts=$(date +%s)
  newleader=""
  for _ in $(seq 1 60); do
    cur=$(rget)
    if [[ -n "${cur}" && "${cur}" != "${leader}" ]]; then newleader="${cur}"; break; fi
    sleep 1
  done
  failover=$(( $(date +%s) - killts ))
  echo "${i},${leader},${newleader:-NONE},${failover}" >>"${DIGEST}"
  echo "  run ${i}: new leader=${newleader:-NONE}, failover=${failover}s"
  # Restore 3 replicas (killed pod rejoins) before next round.
  kubectl -n "${NAMESPACE}" rollout status deployment/evaluator --timeout=3m >/dev/null 2>&1
  sleep 5
done

echo "Restoring evaluator to 1 replica..."
kubectl -n "${NAMESPACE}" scale deployment/evaluator --replicas=1 >/dev/null
echo
echo "Done. Program B digest -> ${DIGEST}"
