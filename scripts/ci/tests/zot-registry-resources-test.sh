#!/usr/bin/env bash

set -euo pipefail

readonly TEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd "${TEST_DIR}/../../.." && pwd)"
readonly ADR="${REPOSITORY_ROOT}/docs/decisions/001-ci-toolchain-baseline.md"
readonly LOCK_FILE="${REPOSITORY_ROOT}/infra/versions/zot.env"
readonly NAMESPACE_MANIFEST="${REPOSITORY_ROOT}/infra/registry/namespace.yaml"
readonly CONFIG_MANIFEST="${REPOSITORY_ROOT}/infra/registry/config.yaml"
readonly WORKLOAD_MANIFEST="${REPOSITORY_ROOT}/infra/registry/zot.yaml"

fail() {
  printf 'Zot registry resource test failed: %s\n' "$1" >&2
  exit 1
}

assert_contains() {
  local file="$1"
  local text="$2"
  local message="$3"

  grep -F -- "${text}" "${file}" >/dev/null 2>&1 || fail "${message}"
}

assert_not_contains() {
  local file="$1"
  local text="$2"
  local message="$3"

  if grep -F -- "${text}" "${file}" >/dev/null 2>&1; then
    fail "${message}"
  fi
}

assert_contains "${ADR}" 'Zot `v2.1.18`' 'ADR does not select the locked Zot release'
assert_not_contains "${ADR}" 'registry:3.1.1' 'ADR still selects CNCF Distribution'

assert_contains "${LOCK_FILE}" 'ZOT_VERSION=v2.1.18' 'Zot version is not locked'
assert_contains "${LOCK_FILE}" \
  'ZOT_IMAGE=ghcr.io/project-zot/zot:v2.1.18@sha256:96913b282b93ee7bc415555c14887e81ece9078631f3189c2e8c1fbb5f888af6' \
  'Linux ARM64 Zot image digest is not locked'
assert_contains "${LOCK_FILE}" 'ZOT_NODE_PORT=30443' 'Zot NodePort is not locked'
assert_not_contains "${LOCK_FILE}" 'latest' 'Zot lock contains a mutable latest reference'

assert_contains "${NAMESPACE_MANIFEST}" 'name: bipeline-registry' 'registry namespace is missing'
assert_contains "${NAMESPACE_MANIFEST}" \
  'pod-security.kubernetes.io/enforce: restricted' \
  'registry namespace does not enforce restricted Pod Security'

assert_contains "${CONFIG_MANIFEST}" '"rootDirectory": "/var/lib/registry"' \
  'Zot persistent storage directory is missing'
assert_contains "${CONFIG_MANIFEST}" '"compat": ["docker2s2"]' \
  'Docker schema digest compatibility is missing'
assert_contains "${CONFIG_MANIFEST}" '"path": "/run/zot-auth/htpasswd"' \
  'htpasswd must be mounted from a Secret'
assert_contains "${CONFIG_MANIFEST}" '"cert": "/run/zot-tls/tls.crt"' \
  'TLS certificate path is missing'
assert_contains "${CONFIG_MANIFEST}" '"key": "/run/zot-tls/tls.key"' \
  'TLS private key path is missing'
assert_contains "${CONFIG_MANIFEST}" '"users": ["ci-pusher"]' \
  'CI push identity policy is missing'
assert_contains "${CONFIG_MANIFEST}" '"actions": ["read", "create", "update"]' \
  'CI push identity has the wrong actions'
assert_contains "${CONFIG_MANIFEST}" '"users": ["k8s-puller"]' \
  'Kubernetes pull identity policy is missing'
assert_contains "${CONFIG_MANIFEST}" '"actions": ["read"]' \
  'Kubernetes pull identity must be read-only'
assert_contains "${CONFIG_MANIFEST}" '"defaultPolicy": []' \
  'unmatched authenticated users must be denied'
assert_not_contains "${CONFIG_MANIFEST}" '"anonymousPolicy"' \
  'anonymous repository access must not be enabled'
assert_not_contains "${CONFIG_MANIFEST}" '"cve"' \
  'CVE database updates are deferred in the network-restricted foundation'

assert_contains "${WORKLOAD_MANIFEST}" 'kind: PersistentVolumeClaim' 'registry PVC is missing'
assert_contains "${WORKLOAD_MANIFEST}" 'storageClassName: local-path' \
  'registry PVC must use local-path'
assert_contains "${WORKLOAD_MANIFEST}" 'storage: 8Gi' 'registry PVC size must be explicit'
assert_contains "${WORKLOAD_MANIFEST}" 'kind: StatefulSet' 'Zot StatefulSet is missing'
assert_contains "${WORKLOAD_MANIFEST}" 'serviceAccountName: zot' \
  'Zot must use its dedicated ServiceAccount'
assert_contains "${WORKLOAD_MANIFEST}" 'automountServiceAccountToken: false' \
  'Zot must not mount a Kubernetes API token'
assert_contains "${WORKLOAD_MANIFEST}" 'runAsNonRoot: true' 'Zot must run as non-root'
assert_contains "${WORKLOAD_MANIFEST}" 'readOnlyRootFilesystem: true' \
  'Zot root filesystem must be read-only'
assert_contains "${WORKLOAD_MANIFEST}" 'allowPrivilegeEscalation: false' \
  'Zot must block privilege escalation'
assert_contains "${WORKLOAD_MANIFEST}" 'type: NodePort' 'Zot NodePort Service is missing'
assert_contains "${WORKLOAD_MANIFEST}" 'nodePort: 30443' 'Zot NodePort does not match the lock'
assert_contains "${WORKLOAD_MANIFEST}" 'scheme: HTTPS' 'Zot probes must use HTTPS'
assert_contains "${WORKLOAD_MANIFEST}" 'requests:' 'Zot resource requests are missing'
assert_contains "${WORKLOAD_MANIFEST}" 'limits:' 'Zot resource limits are missing'
assert_not_contains "${WORKLOAD_MANIFEST}" 'kind: Secret' \
  'runtime credentials or private keys must not be committed'

printf 'Zot registry resource contract tests passed\n'
