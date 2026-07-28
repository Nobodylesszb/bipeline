#!/usr/bin/env bash

set -euo pipefail

remote_command=''
for argument in "$@"; do
  remote_command="${argument}"
done

printf '%s' "${remote_command}" >"${COMMAND_CAPTURE_FILE}"

cat <<'EOF'
hostname=k8s-test-one
architecture=aarch64
os_id=ubuntu
os_version=24.04
cpu_count=8
memory_kib=12216308
root_available_kib=49949828
EOF
