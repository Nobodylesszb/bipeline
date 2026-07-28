#!/usr/bin/env bash

set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly VERIFIER="${SCRIPT_DIR}/../verify-vm-prerequisites.sh"
capture_file="$(mktemp)"
trap 'rm -f "${capture_file}"' EXIT

SSH_BIN="${SCRIPT_DIR}/fixtures/ssh-valid.sh" "${VERIFIER}" test-target >/dev/null

COMMAND_CAPTURE_FILE="${capture_file}" \
  SSH_BIN="${SCRIPT_DIR}/fixtures/ssh-command-contract.sh" \
  "${VERIFIER}" test-target >/dev/null

if ! grep -F 'printf "memory_kib=%s\n"' "${capture_file}" >/dev/null; then
  printf 'expected memory collector to emit a real newline escape\n' >&2
  exit 1
fi

if ! grep -F 'printf "root_available_kib=%s\n"' "${capture_file}" >/dev/null; then
  printf 'expected disk collector to emit a real newline escape\n' >&2
  exit 1
fi

if SSH_BIN="${SCRIPT_DIR}/fixtures/ssh-invalid.sh" "${VERIFIER}" test-target >/dev/null 2>&1; then
  printf 'expected invalid VM facts to fail verification\n' >&2
  exit 1
fi

printf 'VM prerequisite integration tests passed\n'
