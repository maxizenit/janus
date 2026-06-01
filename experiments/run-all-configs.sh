#!/usr/bin/env bash
# Experiment v2 orchestrator — runs the full measurement matrix across all five
# demo-client configurations (C-base, C-r4j, C-r4j-slow, C-react, C-proact).
#
# For each config: set the demo-client deployment env, seed the appropriate
# degradation policy (or none), wait for rollout, then run the measurement
# sweep (k6/run.sh) with warmup + RUNS samples per scenario.
#
# Configs:
#   C-base     profile=(unset) janus=false r4j=false slowcall=off  no policy
#   C-r4j      profile=r4j     janus=false r4j=true  slowcall=off  no policy (failure-rate CB)
#   C-r4j-slow profile=r4j     janus=false r4j=true  slowcall=on   no policy (latency-aware CB)
#   C-react    profile=janus   janus=true  r4j=false slowcall=off  reactive policy (no signal)
#   C-proact   profile=janus   janus=true  r4j=false slowcall=off  proactive policy (PromQL)
#
# C-proact seeds PROACT_POLICY (default = error-rate signal). For the latency
# scenario pass PROACT_POLICY=recommendations-fetch-proactive-latency.json so
# the Evaluator drives degradation from dependency latency, not error rate.
#
# Prereqs: minikube up, images loaded, jq + envsubst installed, platform
# deployed (kubectl apply -k deploy/k8s/base).
#
# Tunables (env): RUNS (10), WARMUP_DURATION (60s), DURATION (90s),
#   ONLY_CONFIG (one of base|r4j|r4j-slow|react|proact) for step-by-step runs,
#   PROACT_POLICY. See k6/run.sh for ONLY_SCENARIO (baseline|errors|flaky|timeout|latency).
set -uo pipefail

EXP_DIR="$(cd "$(dirname "$0")" && pwd)"
K6_DIR="${EXP_DIR}/k6"
POLICY_DIR="${EXP_DIR}/policies"
NAMESPACE="janus"

export RUNS="${RUNS:-10}"
export WARMUP_DURATION="${WARMUP_DURATION:-60s}"
export DURATION="${DURATION:-90s}"

# Step-by-step runs: ONLY_CONFIG=<tag> runs a single config; CONFIGS=<csv> runs
# a subset (default all 5). Combine with ONLY_SCENARIO (k6/run.sh) to run exactly
# the (config x scenario) pairs you want and analyse before the next. E.g. the
# optimised final matrix runs r4j-slow only on latency, baseline on base,r4j,react.
CONFIGS="${CONFIGS:-base,r4j,r4j-slow,react,proact}"
ONLY_CONFIG="${ONLY_CONFIG:-}"
should_run() {
  if [[ -n "${ONLY_CONFIG}" ]]; then [[ "${ONLY_CONFIG}" == "$1" ]]; return; fi
  [[ ",${CONFIGS}," == *",$1,"* ]]
}

# Which proactive policy C-proact seeds. Default = error-rate signal; for the
# latency scenario pass PROACT_POLICY=recommendations-fetch-proactive-latency.json
PROACT_POLICY="${PROACT_POLICY:-recommendations-fetch-proactive.json}"

set_config() {
  local profile="$1" janus_enabled="$2" r4j_enabled="$3" slowcall="${4:-off}"
  if [[ -z "${profile}" ]]; then
    kubectl -n "${NAMESPACE}" set env deployment/demo-client -c demo-client SPRING_PROFILES_ACTIVE- >/dev/null
  else
    kubectl -n "${NAMESPACE}" set env deployment/demo-client -c demo-client SPRING_PROFILES_ACTIVE="${profile}" >/dev/null
  fi
  kubectl -n "${NAMESPACE}" set env deployment/demo-client -c demo-client \
    JANUS_SDK_ENABLED="${janus_enabled}" RESILIENCE4J_ENABLED="${r4j_enabled}" >/dev/null
  # R4j slow-call detection: on => latency-aware circuit breaker (env overrides
  # the yaml defaults of 100% / 60s); off => remove overrides so the naive,
  # failure-rate-only circuit breaker applies. Env names use Spring relaxed
  # binding for resilience4j.circuitbreaker.instances.recommendations.*
  local sc_dur="RESILIENCE4J_CIRCUITBREAKER_INSTANCES_RECOMMENDATIONS_SLOWCALLDURATIONTHRESHOLD"
  local sc_rate="RESILIENCE4J_CIRCUITBREAKER_INSTANCES_RECOMMENDATIONS_SLOWCALLRATETHRESHOLD"
  if [[ "${slowcall}" == "on" ]]; then
    kubectl -n "${NAMESPACE}" set env deployment/demo-client -c demo-client "${sc_dur}=1000ms" "${sc_rate}=50" >/dev/null
  else
    kubectl -n "${NAMESPACE}" set env deployment/demo-client -c demo-client "${sc_dur}-" "${sc_rate}-" >/dev/null
  fi
  echo "  rollout..."
  kubectl -n "${NAMESPACE}" rollout status deployment/demo-client --timeout=3m
}

if should_run base; then
  echo "########## C-base (no protection) ##########"
  set_config "" "false" "false" "off"
  CONFIG_TAG=base "${K6_DIR}/run.sh"
fi

if should_run r4j; then
  echo "########## C-r4j (Resilience4j circuit breaker, failure-rate only) ##########"
  set_config "r4j" "false" "true" "off"
  CONFIG_TAG=r4j "${K6_DIR}/run.sh"
fi

if should_run r4j-slow; then
  echo "########## C-r4j-slow (Resilience4j circuit breaker, slow-call aware) ##########"
  set_config "r4j" "false" "true" "on"
  CONFIG_TAG=r4j-slow "${K6_DIR}/run.sh"
fi

if should_run react; then
  echo "########## C-react (Janus reactive, no Evaluator) ##########"
  POLICY_FILE="${POLICY_DIR}/recommendations-fetch-reactive.json" "${POLICY_DIR}/seed.sh" \
    || { echo "ERROR: reactive policy seed failed — aborting" >&2; exit 1; }
  set_config "janus" "true" "false" "off"
  CONFIG_TAG=react "${K6_DIR}/run.sh"
fi

if should_run proact; then
  echo "########## C-proact (Janus proactive + reactive; policy=${PROACT_POLICY}) ##########"
  POLICY_FILE="${POLICY_DIR}/${PROACT_POLICY}" "${POLICY_DIR}/seed.sh" \
    || { echo "ERROR: proactive policy seed failed — aborting" >&2; exit 1; }
  set_config "janus" "true" "false" "off"
  CONFIG_TAG=proact "${K6_DIR}/run.sh"
fi

echo
echo "Done. Per-config digests under ${K6_DIR}/results/"
