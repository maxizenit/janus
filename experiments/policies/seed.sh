#!/usr/bin/env bash
# Seeds the policy-store with the experiment's degradation policy.
# Idempotent: deletes existing policy first (ignoring NOT_FOUND), then creates.

set -euo pipefail

# Prevent Git Bash on Windows from rewriting absolute container paths
export MSYS_NO_PATHCONV=1

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# On Git Bash on Windows, convert MSYS-style path to native Windows path for kubectl.
if command -v cygpath >/dev/null 2>&1; then
  SCRIPT_DIR="$(cygpath -w "${SCRIPT_DIR}")"
fi
NAMESPACE="${NAMESPACE:-janus}"
GRPCURL_IMAGE="${GRPCURL_IMAGE:-fullstorydev/grpcurl:v1.9.1-alpine}"
POLICY_STORE_ADDR="${POLICY_STORE_ADDR:-policy-store:9090}"
POLICY_FILE="${POLICY_FILE:-${SCRIPT_DIR}/recommendations-fetch.json}"
SERVICE="org.janus.api.policystore.PolicyStoreService"
JOB_NAME="policy-seed"
CONFIGMAP_NAME="policy-seed"

if [[ ! -f "${POLICY_FILE}" ]]; then
  echo "ERROR: policy file not found: ${POLICY_FILE}" >&2
  exit 1
fi

# Extract degradationId from the policy JSON without jq dependency
DEG_ID="$(grep -oE '"degradationId"[[:space:]]*:[[:space:]]*"[^"]+"' "${POLICY_FILE}" \
  | head -1 \
  | sed -E 's/.*"([^"]+)"$/\1/')"

if [[ -z "${DEG_ID}" ]]; then
  echo "ERROR: could not extract degradationId from ${POLICY_FILE}" >&2
  exit 1
fi

echo "==> Seeding policy '${DEG_ID}' into ${POLICY_STORE_ADDR}"

# Clean up any previous attempt
kubectl -n "${NAMESPACE}" delete job "${JOB_NAME}" --ignore-not-found >/dev/null
kubectl -n "${NAMESPACE}" delete configmap "${CONFIGMAP_NAME}" --ignore-not-found >/dev/null

# Build a fresh ConfigMap with both payloads
kubectl -n "${NAMESPACE}" create configmap "${CONFIGMAP_NAME}" \
  --from-file=create-payload.json="${POLICY_FILE}" \
  --from-literal=delete-payload.json="{\"degradationId\":\"${DEG_ID}\"}" \
  >/dev/null

cat <<YAML | kubectl -n "${NAMESPACE}" apply -f - >/dev/null
apiVersion: batch/v1
kind: Job
metadata:
  name: ${JOB_NAME}
  labels:
    app.kubernetes.io/name: policy-seed
    app.kubernetes.io/part-of: janus
spec:
  backoffLimit: 0
  ttlSecondsAfterFinished: 300
  template:
    metadata:
      labels:
        app.kubernetes.io/name: policy-seed
        app.kubernetes.io/part-of: janus
    spec:
      restartPolicy: Never
      containers:
        - name: grpcurl
          image: ${GRPCURL_IMAGE}
          imagePullPolicy: IfNotPresent
          command: ["/bin/sh", "-c"]
          args:
            - |
              echo "Deleting existing policy (if any)..."
              if cat /etc/policy-seed/delete-payload.json | /bin/grpcurl -plaintext -d @ \\
                ${POLICY_STORE_ADDR} ${SERVICE}/DeleteDegradationPolicy; then
                echo "  delete OK"
              else
                echo "  delete returned non-zero (likely NOT_FOUND on first run); ignoring"
              fi
              echo "Creating policy..."
              cat /etc/policy-seed/create-payload.json | /bin/grpcurl -plaintext -d @ \\
                ${POLICY_STORE_ADDR} ${SERVICE}/CreateDegradationPolicy
          volumeMounts:
            - name: payload
              mountPath: /etc/policy-seed
              readOnly: true
      volumes:
        - name: payload
          configMap:
            name: ${CONFIGMAP_NAME}
YAML

if ! kubectl -n "${NAMESPACE}" wait --for=condition=complete --timeout=60s "job/${JOB_NAME}" >/dev/null 2>&1; then
  echo "ERROR: seed job did not complete; logs:" >&2
  kubectl -n "${NAMESPACE}" logs "job/${JOB_NAME}" >&2 || true
  kubectl -n "${NAMESPACE}" delete job "${JOB_NAME}" --ignore-not-found >/dev/null
  kubectl -n "${NAMESPACE}" delete configmap "${CONFIGMAP_NAME}" --ignore-not-found >/dev/null
  exit 1
fi

kubectl -n "${NAMESPACE}" logs "job/${JOB_NAME}"
kubectl -n "${NAMESPACE}" delete job "${JOB_NAME}" --ignore-not-found >/dev/null
kubectl -n "${NAMESPACE}" delete configmap "${CONFIGMAP_NAME}" --ignore-not-found >/dev/null

echo "==> Policy seeded successfully."
