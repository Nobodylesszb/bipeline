#!/usr/bin/env bash

set -euo pipefail
umask 077

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
readonly LOCK_FILE="${REPOSITORY_ROOT}/infra/versions/zot.env"
readonly NAMESPACE_MANIFEST="${REPOSITORY_ROOT}/infra/registry/namespace.yaml"
readonly CONFIG_MANIFEST="${REPOSITORY_ROOT}/infra/registry/config.yaml"
readonly WORKLOAD_MANIFEST="${REPOSITORY_ROOT}/infra/registry/zot.yaml"
readonly RUNTIME_VERIFIER="${SCRIPT_DIR}/verify-zot-runtime.sh"
readonly SCP_BIN="${SCP_BIN:-scp}"
readonly SSH_BIN="${SSH_BIN:-ssh}"
readonly OPENSSL_BIN="${OPENSSL_BIN:-openssl}"
readonly HTPASSWD_BIN="${HTPASSWD_BIN:-htpasswd}"
readonly CONFIG_ROOT="${BIPLINE_CONFIG_DIR:-${HOME}/.config/bipeline}"
readonly RUNTIME_DIR="${CONFIG_ROOT}/zot"
readonly K3S_VERSION='v1.35.6+k3s1'

# shellcheck disable=SC1090
source "${LOCK_FILE}"
readonly ZOT_VERSION ZOT_IMAGE ZOT_NODE_PORT

usage() {
  printf 'Usage: %s --dry-run|--apply [ssh-target]\n' "$(basename "$0")" >&2
}

fail() {
  printf 'Zot deployment failed: %s\n' "$1" >&2
  exit 1
}

print_plan() {
  printf 'mode=dry-run\n'
  printf 'target=%s\n' "$1"
  printf 'zot_version=%s\n' "${ZOT_VERSION}"
  printf 'zot_image=%s\n' "${ZOT_IMAGE}"
  printf 'node_port=%s\n' "${ZOT_NODE_PORT}"
  printf 'runtime_material=%s\n' "${RUNTIME_DIR}"
  printf 'apply=false\n'
}

resolve_endpoint_host() {
  local ssh_target="$1"
  local endpoint_host="${ZOT_ENDPOINT_HOST:-}"

  if [[ -z "${endpoint_host}" ]]; then
    endpoint_host="$(
      "${SSH_BIN}" -G "${ssh_target}" |
        awk '$1 == "hostname" { print $2; exit }'
    )"
  fi

  [[ "${endpoint_host}" =~ ^[A-Za-z0-9][A-Za-z0-9.:-]*$ ]] \
    || fail 'could not resolve a safe Zot endpoint host'
  printf '%s\n' "${endpoint_host}"
}

write_docker_config() {
  local username="$1"
  local password="$2"
  local endpoint="$3"
  local output_file="$4"
  local encoded_auth

  encoded_auth="$(
    printf '%s:%s' "${username}" "${password}" |
      "${OPENSSL_BIN}" base64 -A
  )"
  printf '{"auths":{"%s":{"username":"%s","password":"%s","auth":"%s"}}}\n' \
    "${endpoint}" "${username}" "${password}" "${encoded_auth}" >"${output_file}"
}

generate_runtime_material() {
  local endpoint_host="$1"
  local endpoint="${endpoint_host}:${ZOT_NODE_PORT}"
  local admin_password
  local ci_password
  local pull_password
  local san_type='DNS'
  local cert_subject="${endpoint_host}"
  local required_file

  mkdir -p "${RUNTIME_DIR}"
  chmod 700 "${RUNTIME_DIR}"

  for required_file in \
    admin.password ci-pusher.password k8s-puller.password htpasswd \
    ca.crt tls.crt tls.key ci-dockerconfig.json pull-dockerconfig.json \
    registries.yaml; do
    if [[ ! -s "${RUNTIME_DIR}/${required_file}" ]]; then
      break
    fi
  done
  if [[ -s "${RUNTIME_DIR}/${required_file}" ]]; then
    return
  fi

  if find "${RUNTIME_DIR}" -mindepth 1 -type f -print -quit | grep -q .; then
    fail "runtime material is incomplete; inspect ${RUNTIME_DIR} instead of rotating credentials implicitly"
  fi

  admin_password="$("${OPENSSL_BIN}" rand -hex 24)"
  ci_password="$("${OPENSSL_BIN}" rand -hex 24)"
  pull_password="$("${OPENSSL_BIN}" rand -hex 24)"

  printf '%s\n' "${admin_password}" >"${RUNTIME_DIR}/admin.password"
  printf '%s\n' "${ci_password}" >"${RUNTIME_DIR}/ci-pusher.password"
  printf '%s\n' "${pull_password}" >"${RUNTIME_DIR}/k8s-puller.password"

  printf '%s\n' "${admin_password}" |
    "${HTPASSWD_BIN}" -iBc "${RUNTIME_DIR}/htpasswd" zot-admin >/dev/null 2>&1
  printf '%s\n' "${ci_password}" |
    "${HTPASSWD_BIN}" -iB "${RUNTIME_DIR}/htpasswd" ci-pusher >/dev/null 2>&1
  printf '%s\n' "${pull_password}" |
    "${HTPASSWD_BIN}" -iB "${RUNTIME_DIR}/htpasswd" k8s-puller >/dev/null 2>&1

  "${OPENSSL_BIN}" req -x509 -newkey rsa:3072 -nodes \
    -keyout "${RUNTIME_DIR}/ca.key" \
    -out "${RUNTIME_DIR}/ca.crt" \
    -sha256 -days 3650 \
    -subj '/CN=bipeline-local-registry-ca' >/dev/null 2>&1
  "${OPENSSL_BIN}" req -newkey rsa:3072 -nodes \
    -keyout "${RUNTIME_DIR}/tls.key" \
    -out "${RUNTIME_DIR}/tls.csr" \
    -subj "/CN=${cert_subject}" >/dev/null 2>&1

  if [[ "${endpoint_host}" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    san_type='IP'
  fi
  {
    printf 'subjectAltName=%s:%s\n' "${san_type}" "${endpoint_host}"
    printf 'extendedKeyUsage=serverAuth\n'
    printf 'keyUsage=digitalSignature,keyEncipherment\n'
  } >"${RUNTIME_DIR}/tls.ext"
  "${OPENSSL_BIN}" x509 -req \
    -in "${RUNTIME_DIR}/tls.csr" \
    -CA "${RUNTIME_DIR}/ca.crt" \
    -CAkey "${RUNTIME_DIR}/ca.key" \
    -CAcreateserial \
    -out "${RUNTIME_DIR}/tls.crt" \
    -sha256 -days 825 \
    -extfile "${RUNTIME_DIR}/tls.ext" >/dev/null 2>&1

  write_docker_config \
    ci-pusher "${ci_password}" "${endpoint}" \
    "${RUNTIME_DIR}/ci-dockerconfig.json"
  write_docker_config \
    k8s-puller "${pull_password}" "${endpoint}" \
    "${RUNTIME_DIR}/pull-dockerconfig.json"

  {
    printf 'mirrors:\n'
    printf '  "%s":\n' "${endpoint}"
    printf '    endpoint:\n'
    printf '      - "https://%s"\n' "${endpoint}"
    printf 'configs:\n'
    printf '  "%s":\n' "${endpoint}"
    printf '    tls:\n'
    printf '      ca_file: /etc/rancher/k3s/zot-ca.crt\n'
  } >"${RUNTIME_DIR}/registries.yaml"

  chmod 600 "${RUNTIME_DIR}"/*
}

apply_zot() {
  local ssh_target="$1"
  local endpoint_host="$2"
  local endpoint="${endpoint_host}:${ZOT_NODE_PORT}"
  local remote_command

  "${SCP_BIN}" \
    -o BatchMode=yes \
    -o ConnectTimeout=10 \
    "${NAMESPACE_MANIFEST}" \
    "${CONFIG_MANIFEST}" \
    "${WORKLOAD_MANIFEST}" \
    "${RUNTIME_VERIFIER}" \
    "${RUNTIME_DIR}/admin.password" \
    "${RUNTIME_DIR}/ci-pusher.password" \
    "${RUNTIME_DIR}/k8s-puller.password" \
    "${RUNTIME_DIR}/htpasswd" \
    "${RUNTIME_DIR}/ca.crt" \
    "${RUNTIME_DIR}/tls.crt" \
    "${RUNTIME_DIR}/tls.key" \
    "${RUNTIME_DIR}/ci-dockerconfig.json" \
    "${RUNTIME_DIR}/pull-dockerconfig.json" \
    "${RUNTIME_DIR}/registries.yaml" \
    "${ssh_target}:/tmp/"

  remote_command="set -eu
expected_version='${K3S_VERSION}'
installed_version=\$(k3s --version | awk 'NR == 1 { print \$3 }')
test \"\${installed_version}\" = \"\${expected_version}\"
cleanup() {
  exit_code=\$?
  rm -f /tmp/namespace.yaml /tmp/config.yaml /tmp/zot.yaml /tmp/verify-zot-runtime.sh
  rm -f /tmp/htpasswd /tmp/tls.crt /tmp/tls.key /tmp/ca.crt
  rm -f /tmp/admin.password /tmp/ci-pusher.password /tmp/k8s-puller.password
  rm -f /tmp/ci-dockerconfig.json /tmp/pull-dockerconfig.json /tmp/registries.yaml
  trap - EXIT
  exit \"\${exit_code}\"
}
trap cleanup EXIT
chmod 600 /tmp/htpasswd /tmp/tls.crt /tmp/tls.key /tmp/ca.crt
chmod 600 /tmp/admin.password /tmp/ci-pusher.password /tmp/k8s-puller.password
chmod 600 /tmp/ci-dockerconfig.json /tmp/pull-dockerconfig.json /tmp/registries.yaml
chmod 700 /tmp/verify-zot-runtime.sh
sudo k3s kubectl apply --dry-run=server -f /tmp/namespace.yaml
sudo k3s kubectl apply -f /tmp/namespace.yaml
sudo k3s kubectl create secret generic zot-auth \
  --from-file=htpasswd=/tmp/htpasswd \
  -n bipeline-registry --dry-run=client -o yaml |
  sudo k3s kubectl apply -f -
sudo k3s kubectl create secret tls zot-tls \
  --cert=/tmp/tls.crt --key=/tmp/tls.key \
  -n bipeline-registry --dry-run=client -o yaml |
  sudo k3s kubectl apply -f -
sudo k3s kubectl create configmap zot-ca \
  --from-file=ca.crt=/tmp/ca.crt \
  -n bipeline-registry --dry-run=client -o yaml |
  sudo k3s kubectl apply -f -
sudo k3s kubectl create secret generic zot-ci-dockerconfig \
  --type=kubernetes.io/dockerconfigjson \
  --from-file=.dockerconfigjson=/tmp/ci-dockerconfig.json \
  -n bipeline-ci --dry-run=client -o yaml |
  sudo k3s kubectl apply -f -
sudo k3s kubectl create secret generic zot-pull-dockerconfig \
  --type=kubernetes.io/dockerconfigjson \
  --from-file=.dockerconfigjson=/tmp/pull-dockerconfig.json \
  -n bipeline-ci --dry-run=client -o yaml |
  sudo k3s kubectl apply -f -
sudo k3s kubectl apply --dry-run=server -f /tmp/config.yaml
sudo k3s kubectl apply -f /tmp/config.yaml
sudo k3s kubectl apply --dry-run=server -f /tmp/zot.yaml
sudo k3s kubectl apply -f /tmp/zot.yaml
sudo k3s kubectl wait --for=jsonpath='{.status.phase}'=Bound \
  pvc/zot-data -n bipeline-registry --timeout=3m
sudo k3s kubectl rollout status statefulset/zot \
  -n bipeline-registry --timeout=10m
if sudo test -e /etc/rancher/k3s/registries.yaml &&
   ! sudo cmp -s /tmp/registries.yaml /etc/rancher/k3s/registries.yaml; then
  printf 'existing /etc/rancher/k3s/registries.yaml is unmanaged; refusing to overwrite\\n' >&2
  exit 44
fi
sudo install -m 0644 /tmp/ca.crt /etc/rancher/k3s/zot-ca.crt
sudo install -m 0644 /tmp/registries.yaml /etc/rancher/k3s/registries.yaml
sudo systemctl restart k3s
sudo k3s kubectl wait --for=condition=Ready node/k8s-test-one --timeout=5m
sudo k3s kubectl rollout status statefulset/zot \
  -n bipeline-registry --timeout=5m
/tmp/verify-zot-runtime.sh \
  '${endpoint}' \
  /tmp/ca.crt \
  /tmp/admin.password \
  /tmp/ci-pusher.password \
  /tmp/k8s-puller.password"

  "${SSH_BIN}" \
    -t \
    -o BatchMode=yes \
    -o ConnectTimeout=10 \
    "${ssh_target}" \
    "${remote_command}"
}

main() {
  local mode="${1:-}"
  local ssh_target="${2:-k8s-test-one}"
  local endpoint_host

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
  [[ -f "${NAMESPACE_MANIFEST}" ]] || fail "manifest not found: ${NAMESPACE_MANIFEST}"
  [[ -f "${CONFIG_MANIFEST}" ]] || fail "manifest not found: ${CONFIG_MANIFEST}"
  [[ -f "${WORKLOAD_MANIFEST}" ]] || fail "manifest not found: ${WORKLOAD_MANIFEST}"
  [[ -x "${RUNTIME_VERIFIER}" ]] || fail "runtime verifier not executable: ${RUNTIME_VERIFIER}"

  if [[ "${mode}" == '--dry-run' ]]; then
    print_plan "${ssh_target}"
    return
  fi

  endpoint_host="$(resolve_endpoint_host "${ssh_target}")"
  generate_runtime_material "${endpoint_host}"
  apply_zot "${ssh_target}" "${endpoint_host}"
}

main "$@"
