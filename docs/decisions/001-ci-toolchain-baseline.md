# ADR-001：CI 工具链基线

- 状态：Accepted for planning
- 日期：2026-07-28
- 决策范围：`CI-00` 至 `CI-06`
- 关联规格：`../specs/001-gitops-cicd-platform.md`、`../specs/002-ci-foundation.md`
- 复审条件：运行时兼容性门槛失败、官方停止支持、安全约束无法满足，或进入生产环境规划。

## 1. 背景

第一阶段需要在 Ubuntu Server 24.04.4 ARM64、8 CPU、12 GB 内存的单节点虚拟机中建立 Java CI。当前只有一名开发者，因此应优先降低组件数量和维护成本，同时不能以宿主机 Docker Socket、可变版本或语言硬编码换取短期便利。

本 ADR 冻结生成 CI 实施计划所需的技术选择，但不证明组件已经在本机运行。所有版本仍须通过第 5 节的运行时兼容性门槛后才能进入已验证基线。

## 2. 官方基线核对

核对日期为 2026-07-28。

| 组件 | 官方现状 | 本项目选择 | 理由 |
|---|---|---|---|
| K3s | 最新稳定功能线为 `v1.36.2+k3s1`；`v1.35.6+k3s1` 仍为稳定补丁版本并提供 ARM64 资产 | `v1.35.6+k3s1` | 使用低一条 minor 的稳定线，减少新版本生态兼容风险 |
| Tekton Pipelines | 最新功能版为 `v1.14.1`；官方文档当前将 `v1.12.x` 标为 LTS，要求 Kubernetes `1.28+` | `v1.12.2` | 单人维护优先选择 LTS；K3s 1.35 满足最低版本要求 |
| Java | Spring Boot 4.1 支持 Java 17 至 26 | Eclipse Temurin 25 LTS | 新项目直接采用当前 LTS，避免较早进入 JDK 升级周期 |
| Spring Boot | 当前稳定文档版本为 `4.1.0` | `4.1.0` | 新建控制面不背负 Spring Framework 6 到 7 的迁移债务 |
| Maven | 官方推荐稳定版为 `3.9.16`；3.10 和 4.0 仍为预览版本 | Maven Wrapper 固定 `3.9.16` | 构建版本随仓库确定，不依赖节点预装 Maven |
| BuildKit | 最新稳定版为 `v0.31.2`，官方提供 Kubernetes Job 与 rootless 模式 | `moby/buildkit:v0.31.2-rootless` | 支持 Dockerfile、ARM64 和无 Docker Socket 构建 |
| OCI Registry | Distribution 最新稳定版为 `v3.1.1` | `registry:3.1.1` | 比 Harbor 轻量，足以验证 push、pull 和 digest 契约 |

已通过 OCI manifest 只读检查确认以下镜像包含 `linux/arm64`：

- `moby/buildkit:v0.31.2-rootless`
- `registry:3.1.1`
- `eclipse-temurin:25-jdk`

## 3. 已接受决策

### CI-DEC-001：集群与 Tekton

- K3s 固定为 `v1.35.6+k3s1`，禁止安装脚本静默获取 latest。
- Tekton Pipelines 固定为 `v1.12.2` LTS，安装清单保存来源 URL、版本和校验记录。
- 第一阶段只安装 Tekton Pipelines，不安装 Dashboard、Triggers、Chains、Results、Operator 或 Argo CD。
- K3s 默认 local-path StorageClass 可用于开发环境 Workspace；不得依赖 K3s 私有 API。

### CI-DEC-002：Java 构建基线

- 平台控制面目标基线为 Java 25 LTS 与 Spring Boot 4.1.0。
- 第一个 Java 示例项目使用 Java 25，并提交 Maven Wrapper，Wrapper 固定 Maven 3.9.16。
- CI 默认执行 `./mvnw`；仓库没有 Wrapper 时明确失败，不回退到节点 Maven。
- `java-maven` 构建档案仍保留 `jdk-version` 参数，未来可增加 Java 21 等受支持档案版本。

### CI-DEC-003：镜像仓库

- 第一阶段使用 CNCF Distribution `registry:3.1.1`，不安装 Harbor。
- Registry 使用持久卷保存数据，仅开放给本地受控网络。
- 即使处于本地环境，也必须启用认证；凭据通过 Kubernetes Secret 提供。
- CI 成功以 Registry 返回并可重新拉取的 digest 为准。
- Harbor 的扫描、复制、项目治理和 UI 等能力在出现真实需求后重新评估。

### CI-DEC-004：镜像构建器

- 使用 BuildKit `v0.31.2` rootless Kubernetes Job 模式。
- 禁止挂载 `/var/run/docker.sock`，禁止使用特权 Pod 作为失败回退。
- 优先验证 Kubernetes user namespace（`hostUsers: false`）路径；不得默认采用官方文档标记为不推荐的 `--oci-worker-no-process-sandbox`。
- 如果 K3s 环境无法在上述约束下运行 BuildKit，`CI-05` 必须暂停并重新评审构建器；不得静默降低安全基线。
- 第一阶段只构建 `linux/arm64`；`BuildResult.platforms[]` 仍保持多平台结构。

### CI-DEC-005：Workspace 与缓存

- 每个 PipelineRun 使用独立 `volumeClaimTemplate`，不得让并发运行共享可写 Workspace。
- Workspace 默认请求 5 GiB，具体资源值在环境计划中验证后调整。
- 第一阶段关闭 Maven 依赖缓存，以先验证无缓存时的正确性和可重复性。
- 后续启用缓存时，缓存与源码 Workspace 分离，缓存损坏不得改变构建正确性。

### CI-DEC-006：Git 认证

- 第一条成功路径使用公开 Git 仓库，避免认证问题掩盖 Tekton 基础问题。
- 私有 GitLab 验收使用项目级、只读、仅含 `read_repository` 权限的 Deploy Token，通过 HTTPS 克隆。
- Token 只存入 CI namespace 的 Kubernetes Secret，不进入 Pipeline 参数、YAML、日志或普通数据库字段。
- 必须验证 GitLab TLS 证书；不得通过关闭证书校验解决连接问题。

### CI-DEC-007：报告、运行与 Workspace 保留

- 单元测试 Task 产生 JUnit XML，并将机器可读摘要写入 Tekton Result；`BuildResult.reports[]` 保存状态和报告位置。
- 原始报告保存在运行专属 Workspace，第一阶段保留 7 天。
- Succeeded、Failed、Cancelled 和 TimedOut 的 PipelineRun/TaskRun 均保留 7 天；同一应用最多保留最近 20 次运行，取先达到的限制。
- 删除 PipelineRun 前必须完成 `BuildResult` 采集；删除运行后同步删除其专属 PVC。
- 第一阶段允许用版本控制内的维护脚本执行清理，不为此提前引入 Tekton Pruner、对象存储或平台数据库。

### CI-DEC-008：构建档案注册

- 构建档案和模板元数据存放在平台模板 Git 仓库中，以目录和版本化清单注册。
- 数据库未来只保存稳定引用和查询投影，不保存模板的第二份权威定义。
- 模板发布后不可原地修改语义；不兼容变更必须发布新版本。

### CI-DEC-009：延期项

- Sonar、前端、Webhook 公网入口、Python 档案、Argo CD、Harbor、Maven 缓存和平台数据库均不阻塞 `CI-01` 至 `CI-06`。
- Sonar 后续优先评估外部服务，避免在 12 GB 节点中常驻重型组件。
- Python 包管理器、前端框架和多架构发布策略在进入对应阶段前另写 ADR，不在当前决策中猜测。

## 4. 被拒绝的方案

| 方案 | 当前拒绝理由 |
|---|---|
| Harness Open Source 作为执行引擎 | Pipeline 依赖 Docker Socket，且会替代而不是验证 Tekton 核心路线；仅保留产品设计参考 |
| K3s `latest` 或 RC | 无法复现，升级会静默改变 Kubernetes 和内置组件版本 |
| Tekton `latest` 或 v1.14 功能线 | 当前已有 v1.12 LTS，基础阶段没有必须使用 1.14 的能力 |
| Harbor | 资源和运维成本高于首个 CI 闭环的实际需求 |
| 宿主机 Docker Socket | 赋予构建容器过大的宿主机控制能力，违反 `CI-SEC-006` |
| 缺少 Wrapper 时使用系统 Maven | 构建结果依赖节点状态，破坏可重复性 |
| 第一阶段启用共享 Maven 缓存 | 会增加并发、权限和缓存污染变量，不利于定位基础链路问题 |
| 立即部署 SonarQube、MinIO、Tekton Dashboard | 增加内存和故障面，但不证明基础 CI 正确 |

## 5. 运行时兼容性门槛

实施计划必须先安排以下验证，全部通过后才把本 ADR 的 `Accepted for planning` 改为 `Validated`：

1. K3s 节点报告 `linux/arm64`，默认 StorageClass 能动态创建和绑定 PVC。
2. Tekton v1.12.2 controller、webhook 与 resolver 在 K3s v1.35.6 上 Ready，并完成成功、失败和超时 smoke test。
3. 所有实际安装及 Task 镜像均按 digest 解析到 ARM64，不依赖可变 latest。
4. BuildKit rootless 在不使用 Docker Socket、不使用 privileged、不启用 `--oci-worker-no-process-sandbox` 的条件下完成一次镜像构建和推送。
5. Registry 能按 digest 推送、拉取并启动 ARM64 示例镜像。
6. 12 GB 节点在串行执行一次 Java 构建时无持续 MemoryPressure；首次只允许一个构建并发。

任何一项失败都必须记录实际错误和环境证据，再重新打开对应 `CI-DEC`；不得直接改用风险更高的方案。

## 6. 已知风险

- Tekton 仅声明 Kubernetes 最低版本，K3s 1.35 的组合仍需本地 smoke test 才能视为已验证。
- Rootless BuildKit 在 Kubernetes 中依赖内核 user namespace、AppArmor 和 snapshotter 行为，是当前最大技术风险。
- Spring Boot 4.1 与 Java 25 是新项目的长期基线，但未来接入旧 Java 项目时仍需额外的 JDK 21 构建档案。
- 本地 Registry 的认证和 TLS 策略仍须在环境实施计划中具体化；不能把开发环境等同于可信网络。
- 运行报告保存在 PVC 仅适合第一阶段，跨集群和长期留存需要后续对象存储设计。

## 7. 官方来源

- [K3s Releases](https://github.com/k3s-io/k3s/releases)
- [Tekton Pipelines Releases](https://github.com/tektoncd/pipeline/releases)
- [Install Tekton Pipelines](https://tekton.dev/docs/pipelines/install/)
- [Spring Boot System Requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Apache Maven Download](https://maven.apache.org/download.cgi)
- [BuildKit Releases](https://github.com/moby/buildkit/releases)
- [BuildKit Rootless Mode](https://github.com/moby/buildkit/blob/master/docs/rootless.md)
- [BuildKit Kubernetes Examples](https://github.com/moby/buildkit/tree/master/examples/kubernetes)
- [Distribution Releases](https://github.com/distribution/distribution/releases)
