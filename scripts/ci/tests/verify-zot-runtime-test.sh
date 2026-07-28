#!/usr/bin/env bash

set -euo pipefail

readonly TEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly VERIFIER="${TEST_DIR}/../verify-zot-runtime.sh"

fail() {
  printf 'verify-zot-runtime test failed: %s\n' "$1" >&2
  exit 1
}

grep -F 'anonymous_status' "${VERIFIER}" >/dev/null \
  || fail 'anonymous access assertion is missing'
grep -F '401' "${VERIFIER}" >/dev/null \
  || fail 'anonymous access must be rejected'
grep -F 'ci-pusher' "${VERIFIER}" >/dev/null \
  || fail 'CI push identity is not exercised'
grep -F 'k8s-puller' "${VERIFIER}" >/dev/null \
  || fail 'Kubernetes pull identity is not exercised'
grep -F '403' "${VERIFIER}" >/dev/null \
  || fail 'read-only pull identity push denial is missing'
grep -F 'application/vnd.oci.image.manifest.v1+json' "${VERIFIER}" >/dev/null \
  || fail 'OCI manifest push is missing'
grep -F 'rollout restart statefulset/zot' "${VERIFIER}" >/dev/null \
  || fail 'persistence restart is missing'
grep -F 'manifest_digest_before' "${VERIFIER}" >/dev/null \
  || fail 'pre-restart digest capture is missing'
grep -F 'manifest_digest_after' "${VERIFIER}" >/dev/null \
  || fail 'post-restart digest capture is missing'
grep -F 'bipeline-zot-verification.txt' "${VERIFIER}" >/dev/null \
  || fail 'sanitized evidence output is missing'

if grep -E -- '(^|[[:space:]])(-k|--insecure)([[:space:]]|$)' "${VERIFIER}" >/dev/null; then
  fail 'TLS verification is disabled'
fi

printf 'Zot runtime verification contract tests passed\n'
