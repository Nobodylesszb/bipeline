# Plan 001: 建立可复用的 Java CI 基础闭环

> This plan is an outcome contract, not a step-by-step script. Understand the
> requirement and the recorded decisions, then design the implementation
> yourself against the live repository and target VM. Run milestone validations
> as you go only when self-executing; delegated implementation and verification
> must remain separate. Stop on any STOP condition. When complete, update this
> plan in `plans/README.md`.
>
> Drift check: this workspace had no Git repository when planned. Establish a
> Git baseline before execution, then replace this note with the baseline commit
> and use `git diff --stat <planned-sha>..HEAD -- infra/ pipelines/ profiles/ examples/ scripts/ docs/runbooks/`.

## Status

- Priority: P1
- Effort: L
- Risk: HIGH
- Depends on: none
- Category: feature
- Execution: review pause; execution mode is chosen after plan approval
- Planned at: unversioned workspace, 2026-07-28

## Requirement

在 `k8s-test-one` 的 Ubuntu Server 24.04.4 ARM64 单节点虚拟机上，实现 CI-01 至 CI-06：固定版本的 K3s 和 Tekton 能够从确定的 Git revision 检出 Java Maven 源码，使用 Maven Wrapper 执行测试，在测试成功后通过无 Docker Socket、非特权的 rootless BuildKit 构建并推送 ARM64 OCI 镜像，最后发布结构化 `BuildResult`。相同模板必须服务两个 Java 示例项目，并满足失败短路、凭据保护、资源限制和可追踪性要求。

完成后，CI 仍不包含 Argo CD、GitOps 更新、部署、Java 控制面、前端、Sonar、Webhook 或 Python 实现。这些边界来自 `docs/specs/002-ci-foundation.md:64-74` 和 `docs/specs/002-ci-foundation.md:230-283`。

## Decisions & tradeoffs

- **顺序必须保持 CI-01 → CI-06**：不得为了尽快看到业务镜像跳过环境、Tekton smoke、Git 和测试阶段。Rejected: 先写完整 Pipeline 再倒查环境问题——会把多个故障域混在一次运行中。Based on: `docs/specs/002-ci-foundation.md:76-100`。
- **版本组合固定为 K3s 1.35.6 与 Tekton 1.12.2 LTS**：安装输入必须记录版本、来源与完整性信息，不使用 latest 或 RC。Rejected: 自动安装最新版本——无法复现，且不符合单人维护目标。Based on: `docs/decisions/001-ci-toolchain-baseline.md:19-27,37-42`。
- **Java 25 + Maven Wrapper 3.9.16 是首个档案**：缺少 Wrapper 必须明确失败，不能回退节点 Maven。Rejected: 依赖节点全局 Maven——构建结果会受节点漂移影响。Based on: `docs/decisions/001-ci-toolchain-baseline.md:44-49`。
- **Registry 使用 Distribution 3.1.1、ClusterIP、认证和本地 CA TLS** `(decided while planning)`：CI 内部使用稳定服务名访问，K3s/containerd 与 BuildKit 显式信任该 CA。Rejected: HTTP insecure registry——会让开发环境形成无法迁移的安全例外；Rejected: Harbor——超出基础闭环资源需求。Based on: `docs/decisions/001-ci-toolchain-baseline.md:51-57,127-133`。
- **BuildKit 采用 v0.31.2 rootless Job**：不得使用 privileged、Docker Socket 或 `--oci-worker-no-process-sandbox`。Rejected: 安全降级作为兼容性回退——违反 CI-SEC-005/006；不兼容时应停止并重开 ADR。Based on: `docs/decisions/001-ci-toolchain-baseline.md:59-65,114-125` 与 `docs/specs/002-ci-foundation.md:411-418`。
- **每次运行拥有独立 5 GiB PVC，首版不启用 Maven 缓存**：并发运行不得共享可写源码 Workspace。Rejected: 共享 PVC 同时承载源码和缓存——会引入污染与竞态。Based on: `docs/decisions/001-ci-toolchain-baseline.md:67-72`。
- **模板和构建档案以 Git 为事实源**：使用显式兼容版本；发布后的版本不可原地改变语义。Rejected: 把模板正文存数据库——形成第二事实源。Based on: `docs/decisions/001-ci-toolchain-baseline.md:89-93` 与 `docs/specs/002-ci-foundation.md:214-228`。
- **Task 结果归一化为 BuildResult，但 CI 不生成 ReleaseCandidate**：测试报告与制品分开；运行时镜像必须包含 digest 和 `linux/arm64`。Rejected: CI 直接更新 GitOps——混淆构建事实和发布决定。Based on: `docs/specs/002-ci-foundation.md:315-379,468-474`。
- **运行和报告保留 7 天且每应用最多 20 次**：结果采集完成前不能删除运行，删除运行后删除专属 PVC。Rejected: 为首版引入对象存储、数据库或 Tekton Pruner——增加非必要组件。Based on: `docs/decisions/001-ci-toolchain-baseline.md:81-87`。

## Direction

### Milestone 0: 可执行基线与仓库布局成立

工作区进入 Git 版本控制，规格与 ADR 已 Approved/Accepted，执行所需文件按照以下稳定责任边界组织：

- `infra/`：K3s 前置检查、Registry、namespace、ServiceAccount 和平台基础清单；
- `pipelines/tasks/`：可独立验证、显式版本化的通用或 Java Task；
- `pipelines/pipelines/`：只做 Task 组合的版本化 Pipeline；
- `profiles/`：`java-maven` 构建档案及参数/结果 Schema；
- `examples/`：至少两个独立 Java Maven 示例应用和故障夹具；
- `scripts/ci/`：安装、静态校验、验收和清理入口；
- `docs/runbooks/`：操作、恢复、已知限制和证据索引。

Validation: `test -d infra -a -d pipelines/tasks -a -d pipelines/pipelines -a -d profiles -a -d examples -a -d scripts/ci -a -d docs/runbooks` -> exit 0；所有配置文件能被版本控制列出。

### Milestone 1: CI-01 环境基线可重复建立

只安装固定版本 K3s 和 CI 基础依赖。节点、DNS、时间、ARM64、local-path StorageClass、独立 CI namespace、最小权限 ServiceAccount、Registry PVC、认证与 TLS 均形成机器可检查状态。不得安装 Tekton 之外的后续平台组件。

Validation: `./scripts/ci/verify-environment.sh` -> exit 0；输出节点架构、K3s 版本、StorageClass/PVC、Registry 健康与 TLS/认证检查，不输出凭据。

### Milestone 2: CI-02 Tekton smoke 契约成立

Tekton v1.12.2 controller、webhook 和 resolver Ready。独立 smoke Task 与双 Task Pipeline 能验证结果传递、独立 Workspace、成功、失败和超时终态；相同成功 Pipeline 连续三次结果一致。

Validation: `./scripts/ci/run-acceptance.sh --suite tekton-smoke` -> exit 0；失败或超时样例必须被断言为预期终态，而不是让验收脚本失败得不明原因。

### Milestone 3: CI-03 Git 快照具有确定性

Git Task 接受通用仓库 URL、revision 和 context directory，输出完整 commit SHA。先验证公开仓库，再验证通过 Secret 引用的只读 GitLab Deploy Token。分支、Tag、SHA、无效地址、无权限和不存在 revision 均有可区分结果。

Validation: `./scripts/ci/run-acceptance.sh --suite git-checkout` -> exit 0；日志和资源导出不包含 Token。

### Milestone 4: CI-04 Java 测试可独立复用

Java Maven Task 使用 ARM64 JDK 25 镜像并只执行仓库 Maven Wrapper。成功测试产生 JUnit XML、机器可读摘要和与 PipelineRun/commit SHA 的关联；测试失败、缺少 pom、缺少 Wrapper、JDK 不兼容和依赖下载失败各自可识别，且测试失败阻止后续镜像 Task。

Validation: `./scripts/ci/run-acceptance.sh --suite java-test` -> exit 0。

### Milestone 5: CI-05 Rootless OCI 构建闭环通过

BuildKit rootless 在 K3s 上以 Job/Task 方式运行，不使用 Docker Socket、privileged 或禁止的 no-process-sandbox。它从已测试 Workspace 构建 `linux/arm64` 镜像，通过认证 TLS 推送 Registry，输出不可变引用、digest、平台与构建时间；Registry 失败不得伪造成功结果。

Validation: `./scripts/ci/run-acceptance.sh --suite image-build` -> exit 0，并包含按 digest 拉取及 ARM64 启动验证。

### Milestone 6: CI-06 Java 模板与 BuildResult v1 固化

版本化 Java Pipeline 组合已独立通过的 Task，不硬编码仓库、项目名或凭据。两个示例应用复用同一模板，得到满足稳定 Schema 的 `BuildResult`；Task/Pipeline 均声明超时、资源 request/limit 和失败语义，finally 只承担结果采集与清理，不掩盖失败。

Validation: `./scripts/ci/run-acceptance.sh --suite java-template` -> exit 0；静态契约检查确认通用模型不含 Maven/JDK 必填字段，模板不含 GitOps 或部署命令。

### Milestone 7: 保留策略与 CI Exit Gate 有证据

清理入口按 7 天/20 次策略工作，且只在 BuildResult 采集后删除 PipelineRun、TaskRun 和运行专属 PVC。运行手册能够从空白 VM 重建基础 CI；验收证据按 CI-AC 编号索引，已知限制明确记录。

Validation: `./scripts/ci/run-acceptance.sh --suite exit-gate` -> exit 0，并生成不含秘密的证据摘要。

## Landmines

- 当前目录不是 Git 仓库，无法满足模板版本控制、计划 drift check 和证据提交要求；执行前必须建立 Git 基线。Observed at planning time; `git rev-parse --short HEAD` failed。
- Rootless BuildKit 是最大不确定性；Kubernetes user namespace、AppArmor 和 snapshotter 任一不兼容都必须触发 STOP，不允许改用 privileged。Based on: `docs/decisions/001-ci-toolchain-baseline.md:127-130`。
- Registry 使用本地 CA 后，BuildKit 推送和 K3s/containerd 拉取是两条不同的信任路径；只验证其中一条会产生“能推不能跑”或“能拉不能推”。Based on: `docs/specs/002-ci-foundation.md:195-212`。
- Tekton 失败状态与业务预期失败必须由验收脚本区分，否则失败用例会被误报成测试框架故障。Based on: `docs/specs/002-ci-foundation.md:381-390,430-458`。
- 原始报告在运行 PVC 上仅保留 7 天；BuildResult 必须在 PVC 删除前固化必要摘要。Based on: `docs/decisions/001-ci-toolchain-baseline.md:81-87`。

## Scope

In scope:

- `infra/**`
- `pipelines/tasks/**`
- `pipelines/pipelines/**`
- `profiles/java-maven/**`
- `examples/java-*/**`
- `scripts/ci/**`
- `docs/runbooks/ci-*.md`
- `docs/decisions/001-ci-toolchain-baseline.md`（仅在兼容性证据通过后更新状态）
- `plans/README.md`（仅更新执行状态）

Out of scope:

- Java/Spring Boot 控制面和前端——属于 POST-CI；
- Argo CD、GitOps 仓库及部署——必须等 CI Exit Gate；
- Sonar、Webhook、Python、Harbor、MinIO、Tekton Dashboard/Triggers/Chains/Results/Operator——ADR 已延期；
- 生产高可用、多集群、AMD64/多架构构建——当前仅验证本地 ARM64；
- 创建外部 GitLab 项目或生产凭据——需要独立外部授权。

## Commands

| Purpose | Command | Expected result |
|---|---|---|
| Shell syntax | `find scripts/ci -type f -name '*.sh' -exec bash -n {} +` | exit 0 |
| Kubernetes schema (acceptance) | `ssh k8s-test-one 'sudo k3s kubectl apply --dry-run=server -f <manifest-directory>'` | exit 0，无 schema/RBAC 错误 |
| Environment (acceptance) | `./scripts/ci/verify-environment.sh` | exit 0 |
| Full CI (acceptance) | `./scripts/ci/run-acceptance.sh --suite exit-gate` | exit 0，CI-AC-001～011 证据完整 |
| Secret scan | `./scripts/ci/check-secret-leaks.sh` | exit 0，无明文凭据命中 |

## Done criteria

- [ ] 所有列出的命令通过。
- [ ] CI-01 至 CI-06 按顺序完成，没有安装 out-of-scope 组件。
- [ ] ADR-001 的六项运行时兼容性门槛全部有证据，状态更新为 `Validated`。
- [ ] CI-AC-001 至 CI-AC-011 全部通过 Plan 002 的测试。
- [ ] 两个 Java 示例应用复用同一模板并产生符合契约的 BuildResult。
- [ ] 任何 Pipeline、Task、日志、证据和 Git 文件中均无明文凭据。
- [ ] 实现遵循 Decisions & tradeoffs 中的全部决定。
- [ ] 未修改 out-of-scope 路径。
- [ ] `plans/README.md` 状态已更新。

## STOP conditions

- `docs/specs/002-ci-foundation.md` 尚未 Approved，或 ADR-001 的决定发生漂移。
- 工作区仍未纳入 Git 版本控制。
- VM 实际架构、资源、OS 或 SSH 入口与规格不一致。
- K3s 1.35.6 与 Tekton 1.12.2 无法通过 smoke test。
- BuildKit 只有使用 Docker Socket、privileged 或 `--oci-worker-no-process-sandbox` 才能运行。
- Registry TLS/认证必须通过禁用校验才能工作。
- 需要创建外部仓库、凭据或公开网络入口但没有相应授权。
- 任一验证命令在一次合理修复后仍连续失败两次。
- 正确结果需要修改 out-of-scope 文件或放宽安全需求。

## Maintenance notes

- 所有版本升级都先改 ADR，再改 lock/manifest，最后重跑 Plan 002，不允许直接替换镜像标签。
- BuildResult v1 是后续 Java 控制面的输入契约；模板内部重构不得改变它的字段语义。
- 本地 CA、Registry 凭据和 GitLab Deploy Token 只记录 Secret 名称与用途，文档和证据中不得记录值。
- 首次闭环稳定前保持单并发；提高并发前重新评估 12 GB 节点资源和 Workspace 策略。
