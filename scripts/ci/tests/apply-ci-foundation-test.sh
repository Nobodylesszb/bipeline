#!/usr/bin/env bash

set -euo pipefail

readonly TEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly APPLIER="${TEST_DIR}/../apply-ci-foundation.sh"
capture_dir="$(mktemp -d)"
trap 'rm -rf "${capture_dir}"' EXIT

fail() {
  printf 'apply-ci-foundation test failed: %s\n' "$1" >&2
  exit 1
}

dry_run_output="$(SCP_BIN=/does/not/exist SSH_BIN=/does/not/exist "${APPLIER}" --dry-run test-target)"
[[ "${dry_run_output}" == *'mode=dry-run'* ]] || fail 'dry-run mode was not reported'
[[ "${dry_run_output}" == *'target=test-target'* ]] || fail 'target was not reported'
[[ "${dry_run_output}" == *'apply=false'* ]] || fail 'dry-run did not report apply=false'

if CAPTURE_DIR="${capture_dir}" \
  SCP_BIN="${TEST_DIR}/fixtures/scp-apply-capture.sh" \
  SSH_BIN="${TEST_DIR}/fixtures/ssh-apply-capture.sh" \
  "${APPLIER}" --apply '-oProxyCommand=unsafe' >/dev/null 2>&1; then
  fail 'SSH option injection target was accepted'
fi

[[ ! -e "${capture_dir}/scp-invoked" ]] || fail 'SCP was invoked for an invalid target'
[[ ! -e "${capture_dir}/ssh-invoked" ]] || fail 'SSH was invoked for an invalid target'

CAPTURE_DIR="${capture_dir}" \
  SCP_BIN="${TEST_DIR}/fixtures/scp-apply-capture.sh" \
  SSH_BIN="${TEST_DIR}/fixtures/ssh-apply-capture.sh" \
  "${APPLIER}" --apply test-target >/dev/null

grep -F 'infra/base/namespace.yaml' "${capture_dir}/scp-arguments.txt" >/dev/null \
  || fail 'namespace manifest was not uploaded'
grep -F 'infra/base/service-account.yaml' "${capture_dir}/scp-arguments.txt" >/dev/null \
  || fail 'ServiceAccount manifest was not uploaded'
grep -F 'infra/smoke/foundation-smoke.yaml' "${capture_dir}/scp-arguments.txt" >/dev/null \
  || fail 'smoke manifest was not uploaded'
grep -F 'test-target:/tmp/' "${capture_dir}/scp-arguments.txt" >/dev/null \
  || fail 'manifests were not uploaded to the requested target'
grep -Fx -- '-t' "${capture_dir}/ssh-arguments.txt" >/dev/null \
  || fail 'apply must allocate a terminal for sudo'
grep -F "expected_version='v1.35.6+k3s1'" "${capture_dir}/ssh-arguments.txt" >/dev/null \
  || fail 'apply did not guard the installed K3s version'
grep -F 'sudo k3s kubectl apply --dry-run=server -f /tmp/namespace.yaml' "${capture_dir}/ssh-arguments.txt" >/dev/null \
  || fail 'namespace manifest server-side dry-run is missing'
grep -F 'sudo k3s kubectl apply --dry-run=server -f /tmp/service-account.yaml' "${capture_dir}/ssh-arguments.txt" >/dev/null \
  || fail 'ServiceAccount manifest server-side dry-run is missing'
grep -F 'sudo k3s kubectl apply --dry-run=server -f /tmp/foundation-smoke.yaml' "${capture_dir}/ssh-arguments.txt" >/dev/null \
  || fail 'smoke manifest server-side dry-run is missing'
grep -F 'sudo k3s kubectl apply -f /tmp/namespace.yaml' "${capture_dir}/ssh-arguments.txt" >/dev/null \
  || fail 'namespace manifest apply command is missing'
grep -F 'sudo k3s kubectl apply -f /tmp/service-account.yaml' "${capture_dir}/ssh-arguments.txt" >/dev/null \
  || fail 'ServiceAccount manifest apply command is missing'
grep -F 'sudo k3s kubectl apply -f /tmp/foundation-smoke.yaml' "${capture_dir}/ssh-arguments.txt" >/dev/null \
  || fail 'smoke manifest apply command is missing'
grep -F "kubectl wait --for=jsonpath='{.status.phase}'=Bound" "${capture_dir}/ssh-arguments.txt" >/dev/null \
  || fail 'PVC Bound wait is missing'
grep -F "kubectl wait --for=jsonpath='{.status.phase}'=Succeeded" "${capture_dir}/ssh-arguments.txt" >/dev/null \
  || fail 'smoke Pod Succeeded wait is missing'
grep -F 'kubectl auth can-i get pods' "${capture_dir}/ssh-arguments.txt" >/dev/null \
  || fail 'ServiceAccount RBAC denial check is missing'
grep -F '/tmp/bipeline-ci-foundation-verification.txt' "${capture_dir}/ssh-arguments.txt" >/dev/null \
  || fail 'sanitized verification output is missing'
grep -F 'rm -f /tmp/namespace.yaml /tmp/service-account.yaml /tmp/foundation-smoke.yaml' "${capture_dir}/ssh-arguments.txt" >/dev/null \
  || fail 'remote manifest cleanup is missing'

remote_command="$(cat "${capture_dir}/ssh-arguments.txt")"
expected_order="sudo k3s kubectl apply --dry-run=server -f /tmp/namespace.yaml
sudo k3s kubectl apply -f /tmp/namespace.yaml
sudo k3s kubectl apply --dry-run=server -f /tmp/service-account.yaml
sudo k3s kubectl apply -f /tmp/service-account.yaml
sudo k3s kubectl apply --dry-run=server -f /tmp/foundation-smoke.yaml
sudo k3s kubectl apply -f /tmp/foundation-smoke.yaml"
[[ "${remote_command}" == *"${expected_order}"* ]] \
  || fail 'server-side dry-run and apply order does not satisfy namespace dependency'

printf 'CI foundation apply contract tests passed\n'
