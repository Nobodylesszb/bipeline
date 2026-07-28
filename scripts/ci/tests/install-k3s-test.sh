#!/usr/bin/env bash

set -euo pipefail

readonly TEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly INSTALLER="${TEST_DIR}/../install-k3s.sh"
capture_dir="$(mktemp -d)"
trap 'rm -rf "${capture_dir}"' EXIT

fail() {
  printf 'install-k3s test failed: %s\n' "$1" >&2
  exit 1
}

dry_run_output="$(CURL_BIN=/does/not/exist SSH_BIN=/does/not/exist "${INSTALLER}" --dry-run test-target)"

[[ "${dry_run_output}" == *'mode=dry-run'* ]] || fail 'dry-run mode was not reported'
[[ "${dry_run_output}" == *'target=test-target'* ]] || fail 'SSH target was not reported'
[[ "${dry_run_output}" == *'version=v1.35.6+k3s1'* ]] || fail 'locked K3s version was not reported'
[[ "${dry_run_output}" != *'latest'* ]] || fail 'dry-run output contains a mutable latest reference'

rm -f "${capture_dir}/ssh-invoked"
if CAPTURE_DIR="${capture_dir}" \
  CURL_BIN="${TEST_DIR}/fixtures/curl-installer.sh" \
  PREFLIGHT_BIN=/usr/bin/true \
  SHA256_BIN="${TEST_DIR}/fixtures/sha256-valid.sh" \
  SSH_BIN="${TEST_DIR}/fixtures/ssh-install-capture.sh" \
  "${INSTALLER}" --apply '-oProxyCommand=unsafe' >/dev/null 2>&1; then
  fail 'SSH option injection target was accepted'
fi

[[ ! -e "${capture_dir}/ssh-invoked" ]] \
  || fail 'SSH was invoked for an invalid target'

CAPTURE_DIR="${capture_dir}" \
  CURL_BIN="${TEST_DIR}/fixtures/curl-installer.sh" \
  PREFLIGHT_BIN=/usr/bin/true \
  SHA256_BIN="${TEST_DIR}/fixtures/sha256-valid.sh" \
  SSH_BIN="${TEST_DIR}/fixtures/ssh-install-capture.sh" \
  "${INSTALLER}" --apply test-target >/dev/null

grep -Fx 'test-target' "${capture_dir}/ssh-arguments.txt" >/dev/null \
  || fail 'apply did not use the requested SSH target'
grep -F "INSTALL_K3S_VERSION='v1.35.6+k3s1'" "${capture_dir}/ssh-arguments.txt" >/dev/null \
  || fail 'apply did not pin INSTALL_K3S_VERSION'
grep -F 'INSTALL_K3S_EXEC=server' "${capture_dir}/ssh-arguments.txt" >/dev/null \
  || fail 'apply did not select the server role'
grep -F 'command -v k3s' "${capture_dir}/ssh-arguments.txt" >/dev/null \
  || fail 'apply did not guard against an existing K3s version'
grep -F '#!/bin/sh' "${capture_dir}/ssh-stdin.txt" >/dev/null \
  || fail 'verified installer was not sent over SSH stdin'

rm -f "${capture_dir}/ssh-invoked"
if CAPTURE_DIR="${capture_dir}" \
  CURL_BIN="${TEST_DIR}/fixtures/curl-installer.sh" \
  PREFLIGHT_BIN=/usr/bin/true \
  SHA256_BIN="${TEST_DIR}/fixtures/sha256-invalid.sh" \
  SSH_BIN="${TEST_DIR}/fixtures/ssh-install-capture.sh" \
  "${INSTALLER}" --apply test-target >/dev/null 2>&1; then
  fail 'checksum mismatch was accepted'
fi

[[ ! -e "${capture_dir}/ssh-invoked" ]] \
  || fail 'SSH was invoked after checksum verification failed'

printf 'K3s installer contract tests passed\n'
