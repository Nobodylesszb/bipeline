#!/usr/bin/env bash

set -euo pipefail

readonly REQUIRED_DIRECTORIES=(
  "infra"
  "pipelines/tasks"
  "pipelines/pipelines"
  "profiles/java-maven"
  "examples"
  "scripts/ci"
  "docs/runbooks"
)

missing=0

for directory in "${REQUIRED_DIRECTORIES[@]}"; do
  if [[ ! -d "${directory}" ]]; then
    printf 'missing required directory: %s\n' "${directory}" >&2
    missing=1
  fi
done

if (( missing != 0 )); then
  exit 1
fi

printf 'repository layout is valid\n'
