#!/usr/bin/env bash

set -euo pipefail

readonly ENDPOINT="${1:?registry endpoint is required}"
readonly CA_FILE="${2:?CA certificate path is required}"
readonly ADMIN_PASSWORD_FILE="${3:?admin password path is required}"
readonly CI_PASSWORD_FILE="${4:?CI password path is required}"
readonly PULL_PASSWORD_FILE="${5:?pull password path is required}"
readonly BASE_URL="https://${ENDPOINT}"
readonly REPOSITORY='bipeline/foundation-smoke'
readonly TAG='v1'
work_dir="$(mktemp -d)"
trap 'rm -rf "${work_dir}"' EXIT

fail() {
  printf 'Zot runtime verification failed: %s\n' "$1" >&2
  exit 1
}

assert_status() {
  local actual="$1"
  local expected="$2"
  local message="$3"

  [[ "${actual}" == "${expected}" ]] \
    || fail "${message}: expected ${expected}, got ${actual}"
}

write_curl_config() {
  local username="$1"
  local password_file="$2"
  local output_file="$3"
  local password

  password="$(cat "${password_file}")"
  printf 'user = "%s:%s"\n' "${username}" "${password}" >"${output_file}"
  chmod 600 "${output_file}"
}

digest_file() {
  sha256sum "$1" | awk '{ print "sha256:" $1 }'
}

upload_blob() {
  local source_file="$1"
  local digest="$2"
  local headers_file="${work_dir}/upload-headers"
  local location
  local upload_url
  local status

  status="$(
    curl --silent --show-error \
      --config "${work_dir}/ci.curl" \
      --cacert "${CA_FILE}" \
      --dump-header "${headers_file}" \
      --output /dev/null \
      --write-out '%{http_code}' \
      --request POST \
      "${BASE_URL}/v2/${REPOSITORY}/blobs/uploads/"
  )"
  assert_status "${status}" '202' 'CI identity could not start a blob upload'

  location="$(
    awk 'tolower($1) == "location:" {
      sub(/\r$/, "", $2)
      print $2
      exit
    }' "${headers_file}"
  )"
  [[ -n "${location}" ]] || fail 'blob upload location header is missing'
  if [[ "${location}" == http://* || "${location}" == https://* ]]; then
    upload_url="${location}"
  else
    upload_url="${BASE_URL}${location}"
  fi

  case "${upload_url}" in
    *\?*) upload_url="${upload_url}&digest=${digest}" ;;
    *) upload_url="${upload_url}?digest=${digest}" ;;
  esac
  status="$(
    curl --silent --show-error \
      --config "${work_dir}/ci.curl" \
      --cacert "${CA_FILE}" \
      --output /dev/null \
      --write-out '%{http_code}' \
      --request PUT \
      --header 'Content-Type: application/octet-stream' \
      --data-binary "@${source_file}" \
      "${upload_url}"
  )"
  assert_status "${status}" '201' "CI identity could not upload blob ${digest}"
}

write_curl_config zot-admin "${ADMIN_PASSWORD_FILE}" "${work_dir}/admin.curl"
write_curl_config ci-pusher "${CI_PASSWORD_FILE}" "${work_dir}/ci.curl"
write_curl_config k8s-puller "${PULL_PASSWORD_FILE}" "${work_dir}/pull.curl"

anonymous_status="$(
  curl --silent --show-error \
    --cacert "${CA_FILE}" \
    --output /dev/null \
    --write-out '%{http_code}' \
    "${BASE_URL}/v2/"
)"
assert_status "${anonymous_status}" '401' 'anonymous registry access was not rejected'

printf '{}\n' >"${work_dir}/config.json"
printf 'bipeline-zot-persistence-smoke\n' >"${work_dir}/layer.txt"
config_digest="$(digest_file "${work_dir}/config.json")"
layer_digest="$(digest_file "${work_dir}/layer.txt")"
config_size="$(wc -c <"${work_dir}/config.json" | tr -d ' ')"
layer_size="$(wc -c <"${work_dir}/layer.txt" | tr -d ' ')"

upload_blob "${work_dir}/config.json" "${config_digest}"
upload_blob "${work_dir}/layer.txt" "${layer_digest}"

cat >"${work_dir}/manifest.json" <<EOF
{
  "schemaVersion": 2,
  "mediaType": "application/vnd.oci.image.manifest.v1+json",
  "config": {
    "mediaType": "application/vnd.bipeline.smoke.config.v1+json",
    "digest": "${config_digest}",
    "size": ${config_size}
  },
  "layers": [
    {
      "mediaType": "application/vnd.bipeline.smoke.layer.v1",
      "digest": "${layer_digest}",
      "size": ${layer_size}
    }
  ],
  "annotations": {
    "org.opencontainers.image.title": "bipeline-zot-foundation-smoke"
  }
}
EOF

ci_push_status="$(
  curl --silent --show-error \
    --config "${work_dir}/ci.curl" \
    --cacert "${CA_FILE}" \
    --dump-header "${work_dir}/manifest-put-headers" \
    --output /dev/null \
    --write-out '%{http_code}' \
    --request PUT \
    --header 'Content-Type: application/vnd.oci.image.manifest.v1+json' \
    --data-binary "@${work_dir}/manifest.json" \
    "${BASE_URL}/v2/${REPOSITORY}/manifests/${TAG}"
)"
assert_status "${ci_push_status}" '201' 'CI identity could not push the OCI manifest'

puller_read_status="$(
  curl --silent --show-error \
    --config "${work_dir}/pull.curl" \
    --cacert "${CA_FILE}" \
    --dump-header "${work_dir}/manifest-before-headers" \
    --output "${work_dir}/manifest-before.json" \
    --write-out '%{http_code}' \
    --header 'Accept: application/vnd.oci.image.manifest.v1+json' \
    "${BASE_URL}/v2/${REPOSITORY}/manifests/${TAG}"
)"
assert_status "${puller_read_status}" '200' 'pull identity could not read the OCI manifest'
manifest_digest_before="$(
  awk 'tolower($1) == "docker-content-digest:" {
    sub(/\r$/, "", $2)
    print $2
    exit
  }' "${work_dir}/manifest-before-headers"
)"
[[ "${manifest_digest_before}" == sha256:* ]] \
  || fail 'registry did not return a manifest digest before restart'

puller_push_status="$(
  curl --silent --show-error \
    --config "${work_dir}/pull.curl" \
    --cacert "${CA_FILE}" \
    --output /dev/null \
    --write-out '%{http_code}' \
    --request POST \
    "${BASE_URL}/v2/${REPOSITORY}/blobs/uploads/"
)"
assert_status "${puller_push_status}" '403' 'read-only pull identity was allowed to push'

sudo k3s kubectl rollout restart statefulset/zot -n bipeline-registry >/dev/null
sudo k3s kubectl rollout status statefulset/zot \
  -n bipeline-registry --timeout=5m >/dev/null

post_restart_status="$(
  curl --silent --show-error \
    --retry 12 \
    --retry-delay 2 \
    --retry-all-errors \
    --config "${work_dir}/pull.curl" \
    --cacert "${CA_FILE}" \
    --dump-header "${work_dir}/manifest-after-headers" \
    --output "${work_dir}/manifest-after.json" \
    --write-out '%{http_code}' \
    --header 'Accept: application/vnd.oci.image.manifest.v1+json' \
    "${BASE_URL}/v2/${REPOSITORY}/manifests/${TAG}"
)"
assert_status "${post_restart_status}" '200' 'manifest was unavailable after Zot restart'
manifest_digest_after="$(
  awk 'tolower($1) == "docker-content-digest:" {
    sub(/\r$/, "", $2)
    print $2
    exit
  }' "${work_dir}/manifest-after-headers"
)"
[[ "${manifest_digest_after}" == "${manifest_digest_before}" ]] \
  || fail 'manifest digest changed after Zot restart'
cmp -s "${work_dir}/manifest-before.json" "${work_dir}/manifest-after.json" \
  || fail 'manifest content changed after Zot restart'

verification_file=/tmp/bipeline-zot-verification.txt
{
  printf 'zot_endpoint=%s\n' "${ENDPOINT}"
  printf 'anonymous_status=%s\n' "${anonymous_status}"
  printf 'ci_push_status=%s\n' "${ci_push_status}"
  printf 'puller_read_status=%s\n' "${puller_read_status}"
  printf 'puller_push_status=%s\n' "${puller_push_status}"
  printf 'manifest_digest_before=%s\n' "${manifest_digest_before}"
  printf 'manifest_digest_after=%s\n' "${manifest_digest_after}"
  printf 'persistence=passed\n'
  sudo k3s kubectl get pvc zot-data -n bipeline-registry
  sudo k3s kubectl get pod -l app.kubernetes.io/name=zot \
    -n bipeline-registry -o wide
} >"${verification_file}"
chmod 600 "${verification_file}"
