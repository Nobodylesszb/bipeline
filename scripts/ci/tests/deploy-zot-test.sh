#!/usr/bin/env bash

set -euo pipefail

readonly TEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly DEPLOYER="${TEST_DIR}/../deploy-zot.sh"
capture_dir="$(mktemp -d)"
config_dir="$(mktemp -d)"
generated_dir="$(mktemp -d)"
trap 'rm -rf "${capture_dir}" "${config_dir}" "${generated_dir}"' EXIT

fail() {
  printf 'deploy-zot test failed: %s\n' "$1" >&2
  exit 1
}

dry_run_output="$(
  BIPLINE_CONFIG_DIR="${config_dir}" \
    SCP_BIN=/does/not/exist \
    SSH_BIN=/does/not/exist \
    "${DEPLOYER}" --dry-run test-target
)"
[[ "${dry_run_output}" == *'mode=dry-run'* ]] || fail 'dry-run mode was not reported'
[[ "${dry_run_output}" == *'target=test-target'* ]] || fail 'target was not reported'
[[ "${dry_run_output}" == *'zot_version=v2.1.18'* ]] || fail 'locked Zot version was not reported'
[[ "${dry_run_output}" == *'apply=false'* ]] || fail 'dry-run did not report apply=false'
[[ -z "$(find "${config_dir}" -mindepth 1 -print -quit)" ]] \
  || fail 'dry-run created runtime secrets'

if BIPLINE_CONFIG_DIR="${config_dir}" \
  SCP_BIN="${TEST_DIR}/fixtures/scp-apply-capture.sh" \
  SSH_BIN="${TEST_DIR}/fixtures/ssh-apply-capture.sh" \
  "${DEPLOYER}" --apply '-oProxyCommand=unsafe' >/dev/null 2>&1; then
  fail 'SSH option injection target was accepted'
fi

CAPTURE_DIR="${capture_dir}" \
  BIPLINE_CONFIG_DIR="${generated_dir}" \
  ZOT_ENDPOINT_HOST=10.0.0.2 \
  SCP_BIN="${TEST_DIR}/fixtures/scp-apply-capture.sh" \
  SSH_BIN="${TEST_DIR}/fixtures/ssh-apply-capture.sh" \
  "${DEPLOYER}" --apply test-target >/dev/null

for generated_file in \
  admin.password ci-pusher.password k8s-puller.password htpasswd \
  ca.crt tls.crt tls.key ci-dockerconfig.json pull-dockerconfig.json \
  registries.yaml; do
  [[ -s "${generated_dir}/zot/${generated_file}" ]] \
    || fail "runtime material was not generated: ${generated_file}"
  [[ "$(stat -f '%Lp' "${generated_dir}/zot/${generated_file}")" == '600' ]] \
    || fail "runtime material permissions are not 600: ${generated_file}"
done
grep -F 'zot-admin:' "${generated_dir}/zot/htpasswd" >/dev/null \
  || fail 'admin htpasswd entry is missing'
grep -F 'ci-pusher:' "${generated_dir}/zot/htpasswd" >/dev/null \
  || fail 'CI htpasswd entry is missing'
grep -F 'k8s-puller:' "${generated_dir}/zot/htpasswd" >/dev/null \
  || fail 'pull htpasswd entry is missing'
while IFS= read -r password_file; do
  password="$(cat "${password_file}")"
  if grep -F "${password}" "${generated_dir}/zot/htpasswd" >/dev/null; then
    fail 'plaintext password leaked into htpasswd'
  fi
done < <(find "${generated_dir}/zot" -name '*.password' -type f)

mkdir -p "${config_dir}/zot"
printf 'admin-password\n' >"${config_dir}/zot/admin.password"
printf 'ci-password\n' >"${config_dir}/zot/ci-pusher.password"
printf 'pull-password\n' >"${config_dir}/zot/k8s-puller.password"
printf 'hashed-users\n' >"${config_dir}/zot/htpasswd"
printf 'ca-cert\n' >"${config_dir}/zot/ca.crt"
printf 'server-cert\n' >"${config_dir}/zot/tls.crt"
printf 'server-key\n' >"${config_dir}/zot/tls.key"
printf '{"auths":{}}\n' >"${config_dir}/zot/ci-dockerconfig.json"
printf '{"auths":{}}\n' >"${config_dir}/zot/pull-dockerconfig.json"
printf 'mirrors: {}\n' >"${config_dir}/zot/registries.yaml"

CAPTURE_DIR="${capture_dir}" \
  BIPLINE_CONFIG_DIR="${config_dir}" \
  ZOT_ENDPOINT_HOST=10.0.0.2 \
  SCP_BIN="${TEST_DIR}/fixtures/scp-apply-capture.sh" \
  SSH_BIN="${TEST_DIR}/fixtures/ssh-apply-capture.sh" \
  "${DEPLOYER}" --apply test-target >/dev/null

grep -F 'infra/registry/namespace.yaml' "${capture_dir}/scp-arguments.txt" >/dev/null \
  || fail 'registry namespace manifest was not uploaded'
grep -F 'infra/registry/config.yaml' "${capture_dir}/scp-arguments.txt" >/dev/null \
  || fail 'Zot config manifest was not uploaded'
grep -F 'infra/registry/zot.yaml' "${capture_dir}/scp-arguments.txt" >/dev/null \
  || fail 'Zot workload manifest was not uploaded'
grep -F 'scripts/ci/verify-zot-runtime.sh' "${capture_dir}/scp-arguments.txt" >/dev/null \
  || fail 'runtime verifier was not uploaded'
grep -F "${config_dir}/zot/htpasswd" "${capture_dir}/scp-arguments.txt" >/dev/null \
  || fail 'runtime htpasswd was not uploaded'
grep -F "${config_dir}/zot/tls.crt" "${capture_dir}/scp-arguments.txt" >/dev/null \
  || fail 'runtime TLS certificate was not uploaded'
grep -F "${config_dir}/zot/tls.key" "${capture_dir}/scp-arguments.txt" >/dev/null \
  || fail 'runtime TLS key was not uploaded'

remote_command="$(cat "${capture_dir}/ssh-arguments.txt")"
previous_line=0
for expected_command in \
  'sudo k3s kubectl apply --dry-run=server -f /tmp/namespace.yaml' \
  'sudo k3s kubectl apply -f /tmp/namespace.yaml' \
  'sudo k3s kubectl create secret generic zot-auth' \
  'sudo k3s kubectl create secret tls zot-tls' \
  'sudo k3s kubectl apply --dry-run=server -f /tmp/config.yaml' \
  'sudo k3s kubectl apply -f /tmp/config.yaml' \
  'sudo k3s kubectl apply --dry-run=server -f /tmp/zot.yaml' \
  'sudo k3s kubectl apply -f /tmp/zot.yaml'; do
  current_line="$(
    grep -nF "${expected_command}" "${capture_dir}/ssh-arguments.txt" |
      head -1 |
      cut -d: -f1
  )"
  [[ -n "${current_line}" && "${current_line}" -gt "${previous_line}" ]] \
    || fail 'namespace, runtime Secrets, config, and workload order is incorrect'
  previous_line="${current_line}"
done
[[ "${remote_command}" == *'sudo k3s kubectl rollout status statefulset/zot'* ]] \
  || fail 'Zot rollout wait is missing'
[[ "${remote_command}" == *'statefulset/zot'*'--timeout=10m'* ]] \
  || fail 'initial Zot rollout does not allow for a slow first image pull'
[[ "${remote_command}" == *'sudo k3s kubectl wait'*'pvc/zot-data'* ]] \
  || fail 'Zot PVC Bound wait is missing'
[[ "${remote_command}" == *'sudo install -m 0644 /tmp/ca.crt /etc/rancher/k3s/zot-ca.crt'* ]] \
  || fail 'containerd CA installation is missing'
[[ "${remote_command}" == *'sudo systemctl restart k3s'* ]] \
  || fail 'K3s restart after containerd trust update is missing'
[[ "${remote_command}" == *'/tmp/verify-zot-runtime.sh'* ]] \
  || fail 'runtime acceptance was not executed'
[[ "${remote_command}" == *'rm -f /tmp/namespace.yaml /tmp/config.yaml /tmp/zot.yaml'* ]] \
  || fail 'remote manifest cleanup is missing'
[[ "${remote_command}" == *'/tmp/htpasswd /tmp/tls.crt /tmp/tls.key'* ]] \
  || fail 'remote sensitive-file cleanup is missing'
[[ "${remote_command}" != *'admin-password'* ]] || fail 'admin password leaked into command'
[[ "${remote_command}" != *'ci-password'* ]] || fail 'CI password leaked into command'
[[ "${remote_command}" != *'pull-password'* ]] || fail 'pull password leaked into command'

printf 'Zot deployment contract tests passed\n'
