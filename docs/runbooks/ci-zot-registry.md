# Zot Registry 运行手册

## 目标

本地 CI 使用 Zot `v2.1.18` 保存 OCI 镜像。Zot 与业务应用解耦：

- CI 使用 `ci-pusher` 向 `bipeline/**` 推送镜像；
- K3s 使用 `k8s-puller` 只读拉取镜像；
- CD 只消费带 digest 的镜像引用，不负责构建镜像。

Registry 地址为 `10.211.55.4:30443`，使用本地 CA TLS、bcrypt
htpasswd 认证和 8 GiB `local-path` PVC。

## 部署

在仓库根目录执行：

```bash
./scripts/ci/deploy-zot.sh --dry-run k8s-test-one
./scripts/ci/deploy-zot.sh --apply k8s-test-one
```

`--apply` 会生成一次性运行材料，创建 Kubernetes Secret，配置 K3s
containerd 对本地 CA 的信任，并执行 TLS、ACL、OCI 推拉和持久化验收。
首次从 GHCR 拉取 Zot 镜像可能超过 5 分钟，因此初次 rollout 允许 10 分钟。

## 凭据与证书

运行材料只保存在本机：

```text
~/.config/bipeline/zot/
```

该目录及文件权限分别为 `0700` 和 `0600`，不得提交到 Git、复制到验收
报告或输出到终端日志。部署脚本发现目录内容不完整时会停止，不会静默轮换
凭据。

主要文件：

- `ci-dockerconfig.json`：CI 推送凭据；
- `pull-dockerconfig.json`：K3s/工作负载拉取凭据；
- `ca.crt`：客户端和构建器信任的 CA；
- `admin.password`：仅用于维护和验收。

## 验收

成功部署会在虚拟机生成不含密码的报告：

```text
/tmp/bipeline-zot-verification.txt
```

合格结果必须同时满足：

- 匿名访问返回 `401`；
- `ci-pusher` 推送返回 `201`；
- `k8s-puller` 读取返回 `200`；
- `k8s-puller` 推送返回 `403`；
- Zot 重启前后 manifest digest 相同；
- `zot-data` PVC 为 `Bound`，Zot Pod 为 `Ready`。

读取状态时使用：

```bash
ssh k8s-test-one \
  'sudo k3s kubectl get pod,pvc,service -n bipeline-registry'
```

## 故障定位

若首次部署在 rollout 阶段超时，先检查 Pod 事件中的镜像拉取耗时。不要在
Pod 已经 Ready 时删除 PVC 或重新生成凭据。

```bash
ssh k8s-test-one \
  'sudo k3s kubectl describe pod zot-0 -n bipeline-registry'
ssh k8s-test-one \
  'sudo k3s kubectl logs zot-0 -n bipeline-registry --tail=200'
```

若配置或认证失败，保留现有 PVC，修正清单后重新运行部署脚本。只有明确需要
销毁 Registry 数据时才删除 PVC。

## 当前边界

- 这是单节点开发环境，不提供高可用和外网入口；
- 暂不启用 CVE 数据库更新；
- 不安装 Harbor，也不依赖 Zot 私有 API；
- Java CI 的 BuildKit 信任链将在 CI-05 单独验收；
- Argo CD 和 GitOps/CD 不属于当前 CI 基础阶段。
