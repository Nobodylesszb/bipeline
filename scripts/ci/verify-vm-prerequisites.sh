#!/usr/bin/env bash

set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly VALIDATOR="${SCRIPT_DIR}/validate-vm-facts.sh"
readonly SSH_BIN="${SSH_BIN:-ssh}"
readonly SSH_TARGET="${1:-k8s-test-one}"

"${SSH_BIN}" \
  -o BatchMode=yes \
  -o ConnectTimeout=10 \
  -o ConnectionAttempts=1 \
  "${SSH_TARGET}" \
  'set -eu
   . /etc/os-release
   printf "hostname=%s\n" "$(hostname)"
   printf "architecture=%s\n" "$(uname -m)"
   printf "os_id=%s\n" "${ID}"
   printf "os_version=%s\n" "${VERSION_ID}"
   printf "cpu_count=%s\n" "$(nproc)"
   awk '\''/^MemTotal:/ { printf "memory_kib=%s\n", $2 }'\'' /proc/meminfo
   df -Pk / | awk '\''NR == 2 { printf "root_available_kib=%s\n", $4 }'\''' \
  | "${VALIDATOR}"
