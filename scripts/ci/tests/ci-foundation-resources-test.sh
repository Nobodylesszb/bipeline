#!/usr/bin/env bash

set -euo pipefail

readonly TEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd "${TEST_DIR}/../../.." && pwd)"
readonly LOCK_FILE="${REPOSITORY_ROOT}/infra/versions/k3s.env"
readonly NAMESPACE_MANIFEST="${REPOSITORY_ROOT}/infra/base/namespace.yaml"
readonly SERVICE_ACCOUNT_MANIFEST="${REPOSITORY_ROOT}/infra/base/service-account.yaml"
readonly SMOKE_MANIFEST="${REPOSITORY_ROOT}/infra/smoke/foundation-smoke.yaml"

fail() {
  printf 'CI foundation resource test failed: %s\n' "$1" >&2
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

assert_contains "${LOCK_FILE}" \
  'K3S_AIRGAP_IMAGES_URL=https://github.com/k3s-io/k3s/releases/download/v1.35.6+k3s1/k3s-airgap-images-arm64.tar.zst' \
  'official ARM64 air-gap image URL is not locked'
assert_contains "${LOCK_FILE}" \
  'K3S_AIRGAP_IMAGES_SHA256=850056a90a804ae95977bed34bcaf7d0cbc9fcb794700a25f34454c2b2fd6b50' \
  'official ARM64 air-gap image checksum is not locked'
assert_not_contains "${LOCK_FILE}" 'latest' 'version lock contains a mutable latest reference'

assert_contains "${NAMESPACE_MANIFEST}" 'kind: Namespace' 'CI namespace is missing'
assert_contains "${NAMESPACE_MANIFEST}" 'name: bipeline-ci' 'CI namespace name is missing'
assert_contains "${NAMESPACE_MANIFEST}" 'pod-security.kubernetes.io/enforce: restricted' 'restricted Pod Security enforcement is missing'
assert_contains "${NAMESPACE_MANIFEST}" 'pod-security.kubernetes.io/enforce-version: v1.35' 'Pod Security version must match locked Kubernetes 1.35'
assert_not_contains "${NAMESPACE_MANIFEST}" 'pod-security.kubernetes.io/enforce-version: latest' 'Pod Security enforcement must not use latest'
assert_contains "${SERVICE_ACCOUNT_MANIFEST}" 'kind: ServiceAccount' 'CI ServiceAccount is missing'
assert_contains "${SERVICE_ACCOUNT_MANIFEST}" 'name: ci-runner' 'CI ServiceAccount name is missing'
assert_contains "${SERVICE_ACCOUNT_MANIFEST}" 'automountServiceAccountToken: false' 'ServiceAccount token automount must be disabled'
assert_not_contains "${SERVICE_ACCOUNT_MANIFEST}" 'kind: ClusterRole' 'cluster-scoped RBAC is forbidden'
assert_not_contains "${SERVICE_ACCOUNT_MANIFEST}" 'kind: ClusterRoleBinding' 'cluster-scoped RBAC binding is forbidden'

assert_contains "${SMOKE_MANIFEST}" 'kind: PersistentVolumeClaim' 'smoke PVC is missing'
assert_contains "${SMOKE_MANIFEST}" 'storageClassName: local-path' 'smoke PVC must use local-path'
assert_contains "${SMOKE_MANIFEST}" 'kind: Pod' 'smoke Pod is missing'
assert_contains "${SMOKE_MANIFEST}" 'serviceAccountName: ci-runner' 'smoke Pod must use the CI ServiceAccount'
assert_contains "${SMOKE_MANIFEST}" 'image: rancher/mirrored-library-busybox:1.37.0' 'smoke Pod must use the locked offline ARM64 image'
assert_contains "${SMOKE_MANIFEST}" 'imagePullPolicy: IfNotPresent' 'smoke Pod must prefer the imported image'
assert_contains "${SMOKE_MANIFEST}" 'nslookup kubernetes.default.svc.cluster.local' 'smoke Pod must verify cluster DNS'
assert_contains "${SMOKE_MANIFEST}" 'allowPrivilegeEscalation: false' 'smoke Pod must block privilege escalation'
assert_contains "${SMOKE_MANIFEST}" 'readOnlyRootFilesystem: true' 'smoke Pod root filesystem must be read-only'
assert_contains "${SMOKE_MANIFEST}" 'runAsNonRoot: true' 'smoke Pod must run as non-root'
assert_contains "${SMOKE_MANIFEST}" 'seccompProfile:' 'smoke Pod must declare a seccomp profile'
assert_contains "${SMOKE_MANIFEST}" 'drop:' 'smoke Pod must drop Linux capabilities'
assert_contains "${SMOKE_MANIFEST}" 'requests:' 'smoke Pod must declare resource requests'
assert_contains "${SMOKE_MANIFEST}" 'limits:' 'smoke Pod must declare resource limits'

printf 'CI foundation resource contract tests passed\n'
