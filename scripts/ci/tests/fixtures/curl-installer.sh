#!/usr/bin/env bash

set -euo pipefail

output_file=''
while (( $# > 0 )); do
  if [[ "$1" == '-o' ]]; then
    output_file="$2"
    shift 2
    continue
  fi
  shift
done

[[ -n "${output_file}" ]]
printf '#!/bin/sh\nexit 0\n' >"${output_file}"
