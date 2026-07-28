# K3s installation and recovery

This runbook covers only the single-node K3s foundation for CI-01. It does not
install Tekton, Argo CD, Sonar, or application workloads.

## Locked inputs

The approved version, release tag commit, installer URL, and installer SHA-256
are recorded in `infra/versions/k3s.env`. The installer is fetched from the
official `k3s-io/k3s` release tag rather than from a mutable channel. The
official installer also verifies the downloaded K3s binary against the release
checksums.

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
