# K3s installation and recovery

This runbook covers only the single-node K3s foundation for CI-01. It does not
install Tekton, Argo CD, Sonar, or application workloads.

## Locked inputs

The approved version, release tag commit, installer URL, installer SHA-256,
ARM64 air-gap image URL, and air-gap image SHA-256 are recorded in
`infra/versions/k3s.env`. All artifacts come from the official `k3s-io/k3s`
release tag rather than from a mutable channel. The official installer also
verifies the downloaded K3s binary against the release checksums.

Changing any locked value requires updating ADR-001 and rerunning compatibility
acceptance. Do not replace the URL with `get.k3s.io`, `stable`, or `latest`.

## Preview

Previewing performs no network download and makes no VM changes:

```bash
./scripts/ci/install-k3s.sh --dry-run k8s-test-one
```

Review the target, version, tag commit, URL, and checksum before installation.

## Installation

Installation must be explicitly requested:

```bash
./scripts/ci/install-k3s.sh --apply k8s-test-one
```

The script performs the read-only VM prerequisite check first, verifies the
downloaded installer checksum locally, and then sends the verified installer to
the VM over SSH. Remote privilege escalation uses non-interactive `sudo -n`.

If the same K3s version is already present, the script exits successfully
without reinstalling it. If a different version is present, the script stops
with exit code 42 rather than upgrading or downgrading implicitly.

## Restricted registry access

The local VM could reach GitHub only slowly and could not connect to Docker Hub.
K3s therefore could not pull `rancher/mirrored-pause:3.6`, leaving all system
Pods in `ContainerCreating`.

Do not solve this by adding an unreviewed third-party registry mirror. Download
the locked ARM64 air-gap archive on a host that can reach the official K3s
release, verify its SHA-256, copy it to the VM, and install it as:

```text
/var/lib/rancher/k3s/agent/images/k3s-airgap-images-arm64.tar.zst
```

Restarting K3s imports the archive into containerd. Runtime evidence must show
the import of pause, CoreDNS, local-path-provisioner, metrics-server, and
Traefik images before the system deployments are considered ready.

## CI foundation resources

Preview the target and locked K3s version without changing the cluster:

```bash
./scripts/ci/apply-ci-foundation.sh --dry-run k8s-test-one
```

Apply the namespace, least-privilege ServiceAccount, and smoke resources:

```bash
./scripts/ci/apply-ci-foundation.sh --apply k8s-test-one
```

The apply path validates each manifest on the server before persisting it. The
namespace is validated and created first because a server-side dry-run does not
persist it for dependent resources. The script then validates and creates the
ServiceAccount and smoke resources.

Success requires all of the following:

- `bipeline-ci` is active and enforces the restricted Pod Security profile.
- `foundation-smoke` PVC is `Bound` through `local-path`.
- `foundation-smoke` Pod reaches `Succeeded` and resolves the Kubernetes
  service through cluster DNS.
- `ci-runner` cannot read Pods and does not automatically mount an API token.

The sanitized evidence is stored on the VM at
`/tmp/bipeline-ci-foundation-verification.txt`. The smoke Pod and PVC are
deliberately retained for inspection; delete them only when their evidence is
no longer required.

## Initial diagnostics

After installation, inspect the service and node without changing state:

```bash
ssh k8s-test-one 'sudo systemctl status k3s --no-pager'
ssh k8s-test-one 'sudo journalctl -u k3s --no-pager -n 200'
ssh k8s-test-one 'sudo k3s kubectl get nodes -o wide'
ssh k8s-test-one 'sudo k3s kubectl get pods -A'
ssh k8s-test-one 'sudo k3s kubectl get storageclass'
```

## Recovery boundaries

For a service-level failure, inspect logs before restarting:

```bash
ssh k8s-test-one 'sudo systemctl restart k3s'
```

The official installer creates `/usr/local/bin/k3s-uninstall.sh`. Running it
removes the local cluster and its workloads and is therefore destructive. Do
not run it automatically or as a troubleshooting shortcut. Before uninstalling
a cluster that contains data, capture the required manifests, credentials, and
datastore backup and obtain explicit approval.

Reinstalling a different K3s version is not a recovery action. Update ADR-001,
the version lock, and compatibility evidence first.
