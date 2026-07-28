#!/usr/bin/env bash

set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
readonly LOCK_FILE="${REPOSITORY_ROOT}/infra/versions/k3s.env"
readonly CURL_BIN="${CURL_BIN:-curl}"
readonly SHA256_BIN="${SHA256_BIN:-shasum}"
readonly SSH_BIN="${SSH_BIN:-ssh}"
readonly PREFLIGHT_BIN="${PREFLIGHT_BIN:-${SCRIPT_DIR}/verify-vm-prerequisites.sh}"

k3s_version=''
k3s_release_tag_commit=''
k3s_install_script_url=''
k3s_install_script_sha256=''
temporary_directory=''

usage() {
  printf 'Usage: %s --dry-run|--apply [ssh-target]\n' "$(basename "$0")" >&2
}

fail() {
  printf 'K3s installation preparation failed: %s\n' "$1" >&2
  exit 1
}

cleanup() {
  if [[ -n "${temporary_directory}" && -d "${temporary_directory}" ]]; then
    rm -rf -- "${temporary_directory}"
  fi
}

load_lock() {
  [[ -f "${LOCK_FILE}" ]] || fail "version lock not found: ${LOCK_FILE}"

  while IFS='=' read -r key value; do
    case "${key}" in
      K3S_VERSION) k3s_version="${value}" ;;
      K3S_RELEASE_TAG_COMMIT) k3s_release_tag_commit="${value}" ;;
      K3S_INSTALL_SCRIPT_URL) k3s_install_script_url="${value}" ;;
      K3S_INSTALL_SCRIPT_SHA256) k3s_install_script_sha256="${value}" ;;
    esac
  done <"${LOCK_FILE}"

  [[ "${k3s_version}" =~ ^v[0-9]+\.[0-9]+\.[0-9]+\+k3s[0-9]+$ ]] \
    || fail 'locked K3s version is missing or invalid'
  [[ "${k3s_release_tag_commit}" =~ ^[0-9a-f]{40}$ ]] \
    || fail 'locked K3s tag commit is missing or invalid'
  [[ "${k3s_install_script_url}" == https://raw.githubusercontent.com/k3s-io/k3s/*/install.sh ]] \
    || fail 'installer URL is not an approved k3s-io GitHub source'
  [[ "${k3s_install_script_url}" != *latest* ]] \
    || fail 'installer URL must not use latest'
  [[ "${k3s_install_script_sha256}" =~ ^[0-9a-f]{64}$ ]] \
    || fail 'installer SHA-256 is missing or invalid'
}

print_plan() {
  printf 'mode=dry-run\n'
  printf 'target=%s\n' "$1"
  printf 'version=%s\n' "${k3s_version}"
  printf 'release_tag_commit=%s\n' "${k3s_release_tag_commit}"
  printf 'installer_url=%s\n' "${k3s_install_script_url}"
  printf 'installer_sha256=%s\n' "${k3s_install_script_sha256}"
  printf 'apply=false\n'
}

apply_installation() {
  local ssh_target="$1"
  local installer_file
  local actual_sha256
  local remote_command

  "${PREFLIGHT_BIN}" "${ssh_target}"

  temporary_directory="$(mktemp -d)"
  trap cleanup EXIT
  installer_file="${temporary_directory}/install-k3s.sh"

  "${CURL_BIN}" -fsSL "${k3s_install_script_url}" -o "${installer_file}"
  actual_sha256="$("${SHA256_BIN}" -a 256 "${installer_file}" | awk '{print $1}')"

  [[ "${actual_sha256}" == "${k3s_install_script_sha256}" ]] \
    || fail "installer checksum mismatch: expected ${k3s_install_script_sha256}, got ${actual_sha256}"

  remote_command="set -eu
if command -v k3s >/dev/null 2>&1; then
  installed_version=\"\$(k3s --version | awk 'NR == 1 { print \$3 }')\"
  if [ \"\${installed_version}\" = '${k3s_version}' ]; then
    printf 'K3s %s is already installed\\n' \"\${installed_version}\"
    exit 0
  fi
  printf 'Refusing to replace installed K3s %s with ${k3s_version}\\n' \"\${installed_version}\" >&2
  exit 42
fi
sudo -n env INSTALL_K3S_VERSION='${k3s_version}' INSTALL_K3S_EXEC=server sh -s -"

  "${SSH_BIN}" \
    -o BatchMode=yes \
    -o ConnectTimeout=10 \
    -o ConnectionAttempts=1 \
    "${ssh_target}" \
    "${remote_command}" <"${installer_file}"
}

main() {
  local mode="${1:-}"
  local ssh_target="${2:-k8s-test-one}"

  case "${mode}" in
    --dry-run|--apply) ;;
    *)
      usage
      exit 2
      ;;
  esac

  (( $# <= 2 )) || fail 'too many arguments'
  [[ "${ssh_target}" =~ ^[A-Za-z0-9][A-Za-z0-9._-]*$ ]] \
    || fail 'SSH target must be a configured host alias or hostname'
  load_lock

  if [[ "${mode}" == '--dry-run' ]]; then
    print_plan "${ssh_target}"
    return
  fi

  apply_installation "${ssh_target}"
}

main "$@"
