#!/usr/bin/env bash
# Experiment v2 orchestrator — runs the full measurement matrix across all four
# demo-client configurations (C-base, C-r4j, C-react, C-proact).
#
# For each config: set the demo-client deployment env, seed the appropriate
# degradation policy (or none), wait for rollout, then run the measurement
# sweep (k6/run.sh) with warmup + RUNS samples per scenario.
#
# Configs:
#   C-base   profile=(unset) janus=false r4j=false   no policy
#   C-r4j    profile=r4j     janus=false r4j=true     no policy (R4j is in-app)
#   C-react  profile=janus   janus=true  r4j=false    reactive policy (no signal)
#   C-proact profile=janus   janus=true  r4j=false    proactive policy (PromQL)
#
# Prereqs: minikube up, images loaded, jq + envsubst installed, platform
# deployed (kubectl apply -k deploy/k8s/base).
#
# Tunables (env): RUNS (10), WARMUP_DURATION (60s), DURATION (90s).
set -uo pipefail

EXP_DIR="$(cd "$(dirname "$0")" && pwd)"
K6_DIR="${EXP_DIR}/k6"
POLICY_DIR="${EXP_DIR}/policies"
NAMESPACE="janus"

export RUNS="${RUNS:-10}"
export WARMUP_DURATION="${WARMUP_DURATION:-60s}"
export DURATION="${DURATION:-90s}"

set_config() {
  local profile="$1" janus_enabled="$2" r4j_enabled="$3"
  if [[ -z "${profile}" ]]; then
    kubectl -n "${NAMESPACE}" set env deployment/demo-client SPRING_PROFILES_ACTIVE- >/dev/null
  else
    kubectl -n "${NAMESPACE}" set env deployment/demo-client SPRING_PROFILES_ACTIVE="${profile}" >/dev/null
  fi
  kubectl -n "${NAMESPACE}" set env deployment/demo-client \
    JANUS_SDK_ENABLED="${janus_enabled}" RESILIENCE4J_ENABLED="${r4j_enabled}" >/dev/null
  echo "  rollout..."
  kubectl -n "${NAMESPACE}" rollout status deployment/demo-client --timeout=3m
}

echo "########## C-base (no protection) ##########"
set_config "" "false" "false"
CONFIG_TAG=base "${K6_DIR}/run.sh"

echo "########## C-r4j (Resilience4j circuit breaker) ##########"
set_config "r4j" "false" "true"
CONFIG_TAG=r4j "${K6_DIR}/run.sh"

echo "########## C-react (Janus reactive, no Evaluator) ##########"
POLICY_FILE="${POLICY_DIR}/recommendations-fetch-reactive.json" "${POLICY_DIR}/seed.sh" \
  || { echo "ERROR: reactive policy seed failed — aborting" >&2; exit 1; }
set_config "janus" "true" "false"
CONFIG_TAG=react "${K6_DIR}/run.sh"

echo "########## C-proact (Janus proactive + reactive) ##########"
POLICY_FILE="${POLICY_DIR}/recommendations-fetch-proactive.json" "${POLICY_DIR}/seed.sh" \
  || { echo "ERROR: proactive policy seed failed — aborting" >&2; exit 1; }
set_config "janus" "true" "false"
CONFIG_TAG=proact "${K6_DIR}/run.sh"

echo
echo "All four configurations complete. Per-config digests under ${K6_DIR}/results/"
