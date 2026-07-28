#!/usr/bin/env bash

set -euo pipefail

readonly MIN_CPU_COUNT=8
readonly MIN_MEMORY_KIB=11534336
readonly MIN_ROOT_AVAILABLE_KIB=41943040

hostname_value=''
architecture=''
os_id=''
os_version=''
cpu_count=''
memory_kib=''
root_available_kib=''

while IFS='=' read -r key value; do
  case "${key}" in
    hostname) hostname_value="${value}" ;;
    architecture) architecture="${value}" ;;
    os_id) os_id="${value}" ;;
    os_version) os_version="${value}" ;;
    cpu_count) cpu_count="${value}" ;;
    memory_kib) memory_kib="${value}" ;;
    root_available_kib) root_available_kib="${value}" ;;
  esac
done

fail() {
  printf 'VM prerequisite failed: %s\n' "$1" >&2
  exit 1
}

[[ "${hostname_value}" == 'k8s-test-one' ]] || fail "hostname must be k8s-test-one, got ${hostname_value:-missing}"
[[ "${architecture}" == 'aarch64' || "${architecture}" == 'arm64' ]] || fail "architecture must be ARM64, got ${architecture:-missing}"
[[ "${os_id}" == 'ubuntu' ]] || fail "OS must be Ubuntu, got ${os_id:-missing}"
[[ "${os_version}" == '24.04' ]] || fail "Ubuntu version must be 24.04, got ${os_version:-missing}"

[[ "${cpu_count}" =~ ^[0-9]+$ ]] || fail 'CPU count is missing or invalid'
[[ "${memory_kib}" =~ ^[0-9]+$ ]] || fail 'memory size is missing or invalid'
[[ "${root_available_kib}" =~ ^[0-9]+$ ]] || fail 'root disk availability is missing or invalid'

(( cpu_count >= MIN_CPU_COUNT )) || fail "at least ${MIN_CPU_COUNT} CPUs are required, got ${cpu_count}"
(( memory_kib >= MIN_MEMORY_KIB )) || fail "at least 11 GiB usable memory is required, got ${memory_kib} KiB"
(( root_available_kib >= MIN_ROOT_AVAILABLE_KIB )) || fail "at least 40 GiB root disk availability is required, got ${root_available_kib} KiB"

printf 'hostname=%s\n' "${hostname_value}"
printf 'architecture=%s\n' "${architecture}"
printf 'ubuntu_version=%s\n' "${os_version}"
printf 'cpu_count=%s\n' "${cpu_count}"
printf 'memory_kib=%s\n' "${memory_kib}"
printf 'root_available_kib=%s\n' "${root_available_kib}"
printf 'VM prerequisites are valid\n'
