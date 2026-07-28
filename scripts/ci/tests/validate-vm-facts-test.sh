#!/usr/bin/env bash

set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly VALIDATOR="${SCRIPT_DIR}/../validate-vm-facts.sh"

valid_facts() {
  cat <<'EOF'
hostname=k8s-test-one
architecture=aarch64
os_id=ubuntu
os_version=24.04
cpu_count=8
memory_kib=12216308
root_available_kib=49949828
EOF
}

invalid_architecture_facts() {
  valid_facts | sed 's/architecture=aarch64/architecture=x86_64/'
}

valid_facts | "${VALIDATOR}" >/dev/null

if invalid_architecture_facts | "${VALIDATOR}" >/dev/null 2>&1; then
  printf 'expected x86_64 architecture to be rejected\n' >&2
  exit 1
fi

printf 'VM facts validation tests passed\n'
