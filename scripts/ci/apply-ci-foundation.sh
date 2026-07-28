#!/usr/bin/env bash

set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
readonly NAMESPACE_MANIFEST="${REPOSITORY_ROOT}/infra/base/namespace.yaml"
readonly SERVICE_ACCOUNT_MANIFEST="${REPOSITORY_ROOT}/infra/base/service-account.yaml"
readonly SMOKE_MANIFEST="${REPOSITORY_ROOT}/infra/smoke/foundation-smoke.yaml"
readonly SCP_BIN="${SCP_BIN:-scp}"
readonly SSH_BIN="${SSH_BIN:-ssh}"
readonly K3S_VERSION='v1.35.6+k3s1'

usage() {
  printf 'Usage: %s --dry-run|--apply [ssh-target]\n' "$(basename "$0")" >&2
}

fail() {
  printf 'CI foundation apply failed: %s\n' "$1" >&2
  exit 1
}

print_plan() {
  printf 'mode=dry-run\n'
  printf 'target=%s\n' "$1"
  printf 'k3s_version=%s\n' "${K3S_VERSION}"
  printf 'namespace_manifest=%s\n' "${NAMESPACE_MANIFEST}"
  printf 'service_account_manifest=%s\n' "${SERVICE_ACCOUNT_MANIFEST}"
  printf 'smoke_manifest=%s\n' "${SMOKE_MANIFEST}"
  printf 'apply=false\n'
}

apply_manifests() {
  local ssh_target="$1"
  local remote_command

  "${SCP_BIN}" \
    -o BatchMode=yes \
    -o ConnectTimeout=10 \
    "${NAMESPACE_MANIFEST}" \
    "${SERVICE_ACCOUNT_MANIFEST}" \
    "${SMOKE_MANIFEST}" \
    "${ssh_target}:/tmp/"

  remote_command="set -eu
expected_version='${K3S_VERSION}'
installed_version=\$(k3s --version | awk 'NR == 1 { print \$3 }')
test \"\${installed_version}\" = \"\${expected_version}\"
cleanup() {
  exit_code=\$?
  rm -f /tmp/namespace.yaml /tmp/service-account.yaml /tmp/foundation-smoke.yaml
  trap - EXIT
  exit \"\${exit_code}\"
}
trap cleanup EXIT
sudo k3s kubectl apply --dry-run=server -f /tmp/namespace.yaml
sudo k3s kubectl apply -f /tmp/namespace.yaml
sudo k3s kubectl apply --dry-run=server -f /tmp/service-account.yaml
sudo k3s kubectl apply -f /tmp/service-account.yaml
sudo k3s kubectl apply --dry-run=server -f /tmp/foundation-smoke.yaml
sudo k3s kubectl apply -f /tmp/foundation-smoke.yaml
sudo k3s kubectl wait --for=jsonpath='{.status.phase}'=Bound \
  pvc/foundation-smoke -n bipeline-ci --timeout=3m
sudo k3s kubectl wait --for=jsonpath='{.status.phase}'=Succeeded \
  pod/foundation-smoke -n bipeline-ci --timeout=3m
if sudo k3s kubectl auth can-i get pods \
  --as=system:serviceaccount:bipeline-ci:ci-runner \
  -n bipeline-ci --quiet; then
  printf 'ci-runner unexpectedly has permission to get Pods\\n' >&2
  exit 43
fi
verification_file=/tmp/bipeline-ci-foundation-verification.txt
{
  sudo k3s kubectl get namespace bipeline-ci
  sudo k3s kubectl get serviceaccount ci-runner -n bipeline-ci
  sudo k3s kubectl get pvc foundation-smoke -n bipeline-ci
  sudo k3s kubectl get pod foundation-smoke -n bipeline-ci -o wide
  sudo k3s kubectl logs foundation-smoke -n bipeline-ci
  sudo k3s kubectl auth can-i get pods \
    --as=system:serviceaccount:bipeline-ci:ci-runner \
    -n bipeline-ci || true
} >\"\${verification_file}\" 2>&1
chmod 600 \"\${verification_file}\""

  "${SSH_BIN}" \
    -t \
    -o BatchMode=yes \
    -o ConnectTimeout=10 \
    "${ssh_target}" \
    "${remote_command}"
}

main() {
  local mode="${1:-}"
  local ssh_target="${2:-k8s-test-one}"

  case "${mode}" in
    --dry-run|--apply) ;;
    *)
      usage
      exit 2
      ;;
  esac

  (( $# <= 2 )) || fail 'too many arguments'
  [[ "${ssh_target}" =~ ^[A-Za-z0-9][A-Za-z0-9._-]*$ ]] \
    || fail 'SSH target must be a configured host alias or hostname'
  [[ -f "${NAMESPACE_MANIFEST}" ]] || fail "namespace manifest not found: ${NAMESPACE_MANIFEST}"
  [[ -f "${SERVICE_ACCOUNT_MANIFEST}" ]] \
    || fail "ServiceAccount manifest not found: ${SERVICE_ACCOUNT_MANIFEST}"
  [[ -f "${SMOKE_MANIFEST}" ]] || fail "smoke manifest not found: ${SMOKE_MANIFEST}"

  if [[ "${mode}" == '--dry-run' ]]; then
    print_plan "${ssh_target}"
    return
  fi

  apply_manifests "${ssh_target}"
}

main "$@"
