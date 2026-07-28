#!/usr/bin/env bash

set -euo pipefail

printf 'invoked\n' >"${CAPTURE_DIR}/ssh-invoked"
printf '%s\n' "$@" >"${CAPTURE_DIR}/ssh-arguments.txt"
cat >"${CAPTURE_DIR}/ssh-stdin.txt"
