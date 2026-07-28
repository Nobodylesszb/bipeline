#!/usr/bin/env bash

set -euo pipefail

printf 'invoked\n' >"${CAPTURE_DIR}/scp-invoked"
printf '%s\n' "$@" >"${CAPTURE_DIR}/scp-arguments.txt"
