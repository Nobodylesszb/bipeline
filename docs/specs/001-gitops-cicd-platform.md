# GitOps CI/CD 平台规格说明

- 状态：Draft v0.7
- 日期：2026-07-28
- 所有者：bo
- 文档角色：本项目需求与验收的唯一事实源；后续计划、任务、代码与测试必须引用本规格中的需求编号。

## 1. 背景

团队希望建设一套前后端分离的 CI/CD 平台。平台以 Tekton 承担 CI，以 Argo CD 承担 CD，并使用 Git 保存部署期望状态。当前由一名开发者从本地实验环境起步，但设计必须允许后续增加团队成员、质量检查、更多语言和多环境治理。

平台采用 CI 优先的交付顺序。第一阶段先独立完成并验证 Java CI，不以 Argo CD 或自动部署作为前置条件；CI 达到验收门槛后，再进入 GitOps/CD。最终目标仍是一条可靠的 Java 应用交付闭环：

```text
创建流水线
→ 拉取代码
→ Maven 测试
→ 可选质量检查
→ 构建并推送镜像
→ 生成 BuildResult
→ 质量与安全策略判定
→ 生成 ReleaseCandidate
→ 更新 GitOps 配置
→ Argo CD 自动部署开发环境
→ 展示流水线与部署结果
```

## 2. 当前约束

- 初期只有一名开发者。
- 开发环境运行在本机 Parallels 虚拟机中。
- 虚拟机为 Ubuntu Server 24.04.4 ARM64，8 CPU、12 GB 内存、120 GB 动态磁盘。
- 虚拟机 SSH 别名为 `k8s-test-one`，当前地址为 `10.211.55.4`。
- 第一阶段使用单节点 K3s，不以高可用或生产级容量为目标。
- 不引入不必要的新依赖，不提前建设插件市场或通用工作流引擎。
- 平台必须避免绑定本地环境，未来应可迁移至标准 Kubernetes 或托管 Kubernetes。

### 2.1 已确定的后端技术约束

- **TC-001** 平台后端必须使用 Java 实现，不得以 Go、Node.js、Python 或其他语言替代核心后端服务。
- **TC-002** 后端应用框架采用 Spring Boot。
- **TC-003** 第一阶段后端采用模块化单体架构，不拆分为微服务。
- **TC-004** Java 与 Spring Boot 的具体版本必须选择仍处于官方支持期的稳定版本，并在实施计划形成前锁定。
- **TC-005** Java 代码必须以 Robert C. Martin《代码整洁之道》的可读性、单一职责和持续重构原则为质量基线，但不得机械套用规则或为“整洁”引入无业务价值的抽象。

## 3. 产品目标

### G-001：降低项目接入成本

一个符合约定的 Java Maven 项目应通过少量配置创建流水线，不要求开发者手写完整 Tekton、Argo CD 或 Kubernetes 配置。

### G-002：形成可审计的自动交付闭环

每次构建、制品生成、GitOps 配置变更和部署结果都应可追踪，并能关联到源码提交。

### G-003：建立稳定扩展点

单元测试、Sonar、覆盖率、安全扫描、镜像签名和新语言等能力应以版本化任务、构建档案和流水线模板扩展，不修改平台核心执行模型。Java 是首个实现，后续必须支持 Python、Node.js、Go 等语言生态。

### G-004：控制单人维护成本

控制面采用模块化单体和薄适配层，复用 Tekton、Argo CD 与 Git 原生能力，不自研流水线调度器或部署同步器。

## 4. 非目标

第一阶段明确不包含：

- 生产级高可用 Kubernetes 集群；
- 多集群调度和跨地域容灾；
- 可视化拖拽流水线设计器；
- 自研流水线 DSL、任务调度器或 GitOps 同步器；
- 第一阶段同时实现多语言项目模板；但首版架构必须保留多语言扩展能力；
- 复杂审批流、计费、配额或插件市场；
- 完整灰度发布引擎；
- 对 GitLab、Harbor、SonarQube 等基础设施的替代实现。

## 5. 用户与主要场景

### 5.1 平台管理员

- 配置 Kubernetes、Git、镜像仓库和质量平台连接；
- 维护版本化流水线模板；
- 查看平台组件健康状态和审计记录。

### 5.2 应用开发者

- 选择代码仓库并创建流水线；
- 查看测试、构建、制品和部署结果；
- 手动重新运行失败流水线；
- 查看当前开发环境运行的镜像版本。

### 5.3 首要用户旅程

1. 用户选择一个 Java Maven Git 仓库。
2. 用户填写应用名、默认分支、构建参数和开发环境配置。
3. 平台引用已发布的 Java 流水线模板。
4. 平台初始化该应用的 GitOps 配置。
5. 平台创建或注册 Argo CD Application。
6. 平台触发首次 Tekton PipelineRun。
7. CI 输出 `BuildResult`，策略通过后生成 `ReleaseCandidate`。
8. 发布流程依据候选版本更新开发环境 GitOps 镜像版本。
9. Argo CD 同步新版本。
10. 用户在平台中看到源码提交、流水线、镜像、策略结果和部署结果的关联关系。

## 6. 系统边界

```text
Web 前端
   ↓ REST API
Java / Spring Boot 模块化单体
   ├── 应用与组件
   ├── 流水线与模板
   ├── 制品与构建记录
   ├── 环境与部署
   ├── 外部集成
   └── 审计
      ↓                 ↓
Tekton / Kubernetes   GitLab / OCI Registry
      ↓                 ↓
   PipelineRun      GitOps 配置仓库
                           ↓
                        Argo CD
                           ↓
                     Kubernetes 应用
```

### 6.1 事实源

| 信息 | 唯一事实源 |
|---|---|
| 应用源码 | 应用 Git 仓库 |
| 流水线模板 | 平台模板 Git 仓库 |
| 构建执行状态 | Tekton / Kubernetes API |
| 镜像制品 | OCI Registry |
| 环境期望状态 | GitOps 仓库 |
| 集群同步状态 | Argo CD / Kubernetes API |
| 平台元数据、权限和审计 | 平台数据库 |

平台数据库可以缓存外部状态，但不得成为构建状态或部署期望状态的第二事实源。

### 6.2 构建结果与发布候选

- CI 必须用 `BuildResult` 描述一次构建实际产生的源码快照、零到多个制品、报告和构建来源，不宣称这些制品已经获准部署。
- `artifacts[]` 可以同时包含 JAR、Wheel、一个或多个 OCI 镜像、SBOM 和签名；制品通过 `kind` 与 `role` 表达用途，不使用单值 `packageArtifact` 或 `deployableArtifact`。
- `reports[]` 独立保存单元测试、覆盖率、Sonar 和安全扫描等结果，不把报告伪装成制品。
- 服务型项目进入 CD 前，策略层必须从 `BuildResult` 生成 `ReleaseCandidate`；第一阶段可部署组件只允许引用带不可变 digest 的 OCI 镜像。
- 公共库项目可以只发布 Maven Artifact、Wheel、npm Package 等包，不生成 `ReleaseCandidate`，也不进入 CD。
- Argo CD 和 GitOps 配置只理解 `ReleaseCandidate` 中的运行时组件，不理解 JAR、Wheel、Maven、Python 或 CI Task 语义。

CI 归一化输出示例：

```yaml
buildResult:
  source:
    repository: https://gitlab.example.com/team/order-service.git
    revision: abc123
  artifacts:
    - name: application-jar
      kind: java-jar
      role: package
      uri: https://artifacts.example.com/order-service/app.jar
      digest: sha256:...
    - name: application-image
      kind: oci-image
      role: runtime
      uri: harbor.example.com/order-service
      digest: sha256:...
      platforms:
        - linux/arm64
        - linux/amd64
  reports:
    - kind: unit-test
      status: passed
  provenance:
    templateRef: java-maven
    templateVersion: v1
```

策略通过后生成的 CD 输入示例：

```yaml
releaseCandidate:
  application: order-service
  sourceRevision: abc123
  components:
    - name: application
      artifact:
        kind: oci-image
        uri: harbor.example.com/order-service
        digest: sha256:...
  policyStatus: passed
```

稳定边界为：

```text
BuildResult
→ 质量与安全策略
→ ReleaseCandidate
→ 更新 GitOps 仓库
→ Argo CD 部署
```

第一版只要求 Java 服务产生一个 OCI 运行时镜像，但数据结构不得限制未来的多镜像、多平台、SBOM 或签名结果。

### 6.3 Harness Open Source 参考设计边界

Harness Open Source 作为产品交互、信息架构和领域命名的外部参考，不作为本平台的运行时依赖，也不作为待二次开发的代码基础。参考基线为 2026-07-28 查阅的官方文档与 Apache-2.0 开源仓库：

- [Harness Open Source](https://developer.harness.io/docs/open-source)
- [Pipeline Overview](https://developer.harness.io/docs/open-source/pipelines/overview)
- [Harness Open Source Repository](https://github.com/harness/harness)

选择性采用以下设计：

- 以项目、代码仓库、流水线、运行记录和制品形成清晰的对象层级；
- 流水线与代码仓库关联，一个应用后续可配置多条不同用途的流水线；
- 流水线配置版本化保存，支持手动运行、Webhook 触发和统一运行详情；
- 使用 Stage、Step、Condition、Secret、Trigger 等用户易理解的概念，但由平台映射到 Tekton，而不是暴露 Kubernetes CRD 细节；
- 在一个运行详情中统一展示阶段状态、日志、报告、制品和来源信息。

明确不采用以下实现：

- 不使用挂载宿主机 `/var/run/docker.sock` 的执行模型；
- 不以 Harness 内置代码仓库、Registry 或数据库替代 GitLab、OCI Registry 等独立事实源；
- 不复制 Harness/Drone Pipeline DSL、自研执行引擎或直接基于其 Go 代码二次开发；
- 不允许 CI Pipeline 直接部署 Kubernetes；发布仍须经过 `ReleaseCandidate`、GitOps 仓库和 Argo CD；
- 不改变 Java、Spring Boot、模块化单体的控制面技术约束。

概念映射如下：

| Harness 概念 | 本平台概念 | 实现边界 |
|---|---|---|
| Project / Repository | 应用、组件、源码仓库绑定 | 源码仍以外部 Git 仓库为事实源 |
| Pipeline | 流水线配置、`build-profile`、模板版本 | 平台生成并引用版本化 Tekton Pipeline |
| Stage / Step | Pipeline / Task | 用户界面使用领域名称，适配器处理 CRD |
| Execution | PipelineRun / TaskRun 投影 | Tekton/Kubernetes API 是执行状态事实源 |
| Artifact | `BuildResult.artifacts[]` | OCI Registry 或包仓库是制品事实源 |
| Trigger | 手动触发、Webhook、定时策略 | 触发器不得改变流水线模板语义 |
| Secret | Connection + SecretRef | 凭据保存在 Kubernetes Secret 等受控存储 |
| Deploy step | `ReleaseCandidate` → GitOps → Argo CD | 禁止直接部署目标应用 |

## 7. 功能需求

### 7.1 应用接入

- **FR-001** 平台必须支持登记 Java Maven 应用的 Git 仓库、默认分支和构建上下文。
- **FR-002** 创建流水线必须是幂等操作；重复请求不得创建冲突的 Tekton、GitOps 或 Argo CD 资源。
- **FR-003** 平台必须支持验证 Git 仓库和 Kubernetes 连接配置，并返回可理解的失败原因。
- **FR-004** 第一阶段必须支持一个应用对应一个开发环境。

### 7.2 流水线模板

- **FR-010** 流水线必须引用带版本号的模板，运行记录必须保存实际使用的模板版本。
- **FR-011** 第一版 Java CI 模板必须包含代码检出、Maven 测试、镜像构建、镜像推送和 `BuildResult` 发布阶段；不得包含 GitOps 更新或应用部署。
- **FR-012** Sonar 必须作为可启停的独立 Tekton Task 接入，而不是硬编码在平台后端中。
- **FR-013** 质量或安全策略失败时不得生成 `ReleaseCandidate`，也不得更新 GitOps；已经构建的镜像可以保留为不可发布的 CI 制品以供审计。
- **FR-014** Task 的输入、输出、超时和失败语义必须形成稳定契约。
- **FR-015** 平台必须通过 `build-profile` 和 `template-version` 选择语言构建模板，不得在核心领域模型中使用 Java/Python/Node.js 条件分支编排构建步骤。
- **FR-016** 通用流水线契约必须保持语言无关；JDK、Maven goals、Python version、pytest arguments 等参数必须归属于对应构建档案。
- **FR-017** 新增语言必须能够通过注册构建档案、版本化 Task 和 Pipeline 完成，不要求修改流水线执行引擎。
- **FR-018** Java 验收完成后，必须以 Python 作为第二种语言验证扩展模型，再决定是否继续支持 Node.js 和 Go。
- **FR-019** 创建流水线必须采用“选择应用与仓库 → 选择构建档案与模板版本 → 填写少量参数 → 保存 → 运行”的引导流程，不要求用户编辑完整 Tekton YAML。

### 7.3 执行与触发

- **FR-020** 用户必须能够通过平台手动触发 PipelineRun。
- **FR-021** 后续版本必须能够通过 Git Webhook 触发流水线，首版可先保留接口而不启用公网回调。
- **FR-022** 平台必须显示 PipelineRun、TaskRun、当前阶段、开始时间、结束时间和失败原因。
- **FR-023** 用户必须能够重新运行失败流水线，同时保留原运行记录。
- **FR-024** 平台模型必须允许一个应用配置多条流水线；第一阶段只要求一条 Java CI 流水线，但不得把“一应用一流水线”固化为长期约束。

### 7.4 制品

- **FR-030** 每个成功镜像必须关联源码提交 SHA、流水线运行 ID、镜像仓库地址和不可变标识。
- **FR-031** 部署不得仅依赖可变的 `latest` 标签。
- **FR-032** 平台必须能够查询并展示某个环境当前声明使用的镜像版本。
- **FR-033** 每次 CI 运行必须产生结构化 `BuildResult`，至少包含 `source`、`artifacts[]`、`reports[]` 和 `provenance`；失败运行允许 `artifacts[]` 为空，但必须保留已产生的报告和失败状态。
- **FR-034** 每个制品必须包含稳定名称、`kind`、`role`、URI 和可用时的 digest；OCI 镜像必须包含 digest 与非空 `platforms[]`。
- **FR-035** JAR、Wheel 等 `role = package` 的制品是可选结果，不得成为服务部署的前置假设；公共库只有包制品时不得进入 GitOps/CD。
- **FR-036** CD 必须按 `ReleaseCandidate.components[]` 中的制品类型判断可部署性，不得根据项目语言判断；组件中的制品均具有运行时角色。
- **FR-037** 策略层必须独立于 CI 执行模型，依据 `BuildResult.reports[]`、制品元数据和环境策略决定是否生成 `ReleaseCandidate`。
- **FR-038** `ReleaseCandidate` 必须包含应用、源码提交、一个或多个运行时组件及 `policyStatus = passed`；第一阶段每个服务只要求一个 OCI 镜像组件。
- **FR-039** 未通过策略、缺少 OCI digest 或目标平台不兼容的构建不得生成可被 CD 消费的 `ReleaseCandidate`。

### 7.5 GitOps 与部署

- **FR-040** CI Task 和发布流程均不得直接对目标应用执行 `kubectl apply` 或 `helm upgrade`；通过策略的 `ReleaseCandidate` 必须由交付编排更新 GitOps 配置后触发部署。
- **FR-041** GitOps 仓库必须按应用与环境组织配置，并保留提交历史。
- **FR-042** 开发环境在生成通过策略的 `ReleaseCandidate` 后默认自动更新 GitOps 镜像版本；仅 PipelineRun 成功不足以触发部署。
- **FR-043** Argo CD 必须从 GitOps 仓库同步期望状态，并向平台提供同步与健康状态。
- **FR-044** 用户必须能够从部署记录定位对应的 GitOps 提交、镜像和源码提交。
- **FR-045** GitOps 更新必须避免再次触发业务源码流水线形成循环。

### 7.6 外部集成

- **FR-050** Git、Registry、Sonar 和 Argo CD 集成必须位于独立适配模块中。
- **FR-051** 外部系统凭据不得提交到 Git，也不得以明文形式写入普通数据库字段或日志。
- **FR-052** 单个外部集成不可用时，平台必须保留明确的阶段状态和可重试入口。

### 7.7 审计

- **FR-060** 平台必须记录流水线创建、配置变更、手动触发和部署操作。
- **FR-061** 审计记录至少包含操作者、时间、对象、动作和结果。

## 8. 非功能需求

- **NFR-001 可维护性**：后端必须使用 Java 与 Spring Boot，并采用模块化单体；模块间通过明确接口交互，禁止直接跨模块访问内部持久化实现。
- **NFR-002 可移植性**：平台清单不得依赖 Parallels 或 K3s 私有 API。
- **NFR-003 可观测性**：平台和关键流水线阶段必须输出结构化日志，并暴露基础健康检查和指标。
- **NFR-004 安全性**：Kubernetes ServiceAccount 按最小权限配置；Tekton 与 Argo CD 职责及权限分离。
- **NFR-005 可恢复性**：删除并重建平台缓存数据后，应能从 Git、Tekton、Registry 和 Argo CD 恢复主要外部状态。
- **NFR-006 资源约束**：第一阶段所有核心组件在 12 GB 虚拟机内存下可运行；SonarQube 可作为独立可选组件评估，不得使基础闭环依赖它。
- **NFR-007 ARM64**：首版所选镜像必须支持 ARM64；未来发布到 AMD64 环境时必须具备明确的多架构构建策略。
- **NFR-008 多语言可扩展性**：应用、流水线、运行记录和制品等通用模型不得依赖 Maven、JDK 或 Java 专属概念；语言特有配置必须封装在构建档案边界内。

### 8.1 Java 代码整洁约束

- **CQ-001 命名表达意图**：包、类、方法、变量和测试名称必须使用业务语言表达目的；禁止无明确语义的 `Data`、`Info`、`Util`、`Helper`、`Manager` 等泛化命名。
- **CQ-002 单一职责**：类和方法必须具有单一、可描述的职责，并保持一致的抽象层级；出现多个变化原因时必须拆分职责，而不是继续增加条件分支。
- **CQ-003 小而清晰的方法**：方法应优先通过提前返回、明确命名和提取领域概念降低嵌套；不设置机械行数上限，但复杂到无法用一句话描述时必须重构。
- **CQ-004 依赖方向明确**：领域规则不得依赖 Web、数据库、GitLab、Tekton 或 Argo CD 的具体客户端；外部系统通过端口和适配器接入。
- **CQ-005 消除知识重复**：重复的业务规则必须只有一个权威实现；不得为了消除表面相似代码而合并语义不同的概念。
- **CQ-006 显式错误处理**：不得吞掉异常或只记录后继续；领域失败、外部系统失败和重试条件必须具有明确类型、上下文和处理策略。
- **CQ-007 注释解释原因**：注释用于说明约束、权衡和非显然原因，不得重复代码本身；过期注释必须随代码一起删除。
- **CQ-008 测试保护行为**：核心领域行为、边界转换和失败路径必须有自动化测试；修复缺陷前先增加能复现问题的回归测试。
- **CQ-009 保持简单**：优先复用 Java 与 Spring 现有能力；没有第二个真实用例前，不提前设计通用框架、插件机制或扩展层。
- **CQ-010 持续重构**：功能实现必须包含必要的重构步骤，重构不得改变外部可观察行为，并须由测试证明。

## 9. 第一阶段验收标准

- **AC-001** 在空白单节点 K3s 上，可以通过文档化步骤安装所需基础组件并通过健康检查。
- **AC-002** 一个示例 Java Maven 仓库可以在不手写完整 Tekton Pipeline 的情况下创建流水线。
- **AC-003** 首次运行能够完成测试、构建镜像、推送 Registry，并输出包含 OCI 镜像 digest、`platforms[]`、测试报告和模板来源的 `BuildResult`。
- **AC-004** Argo CD 能自动部署该镜像，且应用达到 Healthy 与 Synced 状态。
- **AC-005** 平台能够展示源码提交、PipelineRun、镜像、GitOps 提交和部署状态之间的关联。
- **AC-006** Maven 测试失败时不产生运行时镜像；质量或安全策略失败时不产生 `ReleaseCandidate`，两者均不得产生新的 GitOps 部署提交。
- **AC-007** 回退 GitOps 提交后，Argo CD 能恢复之前的应用版本。
- **AC-008** 重复执行应用接入不会生成重复或冲突资源。
- **AC-009** Git 仓库、平台日志和普通数据库查询结果中不出现明文凭据。
- **AC-010** 关键模板和平台清单在 ARM64 开发环境中验证通过。
- **AC-011** Java 后端变更必须通过格式检查、静态分析和自动化测试，并在评审中逐项检查 `CQ-001` 至 `CQ-010`；存在例外时必须在变更记录中说明理由。
- **AC-012** 通用 CI API 和数据模型使用 `build-profile`、`template-version` 及语言无关结果契约；其中不得出现 Maven、JDK 等 Java 专属必填字段。
- **AC-013** Java 与 Python 服务的 `BuildResult` 均能表达 `kind = oci-image`、`role = runtime` 的制品；只有 JAR 或 Wheel 包制品的公共库运行不会生成发布候选或部署动作。
- **AC-014** CD 只接受 `policyStatus = passed` 且全部运行时组件均带 OCI digest 的 `ReleaseCandidate`；无法直接以原始 `BuildResult` 更新 GitOps 仓库。
- **AC-015** 用户无需编辑 Tekton CRD 即可从应用和仓库创建流水线，并在同一运行详情中查看阶段状态、日志入口、报告、制品和模板版本。

## 10. 建议交付阶段

这些阶段描述产品切片，不是实施计划；实施任务必须在本规格评审后另行生成。

### 阶段 A：环境基线

安装并验证 K3s、CI 所需基础存储、Registry 与 Tekton。此阶段不安装 Argo CD。

### 阶段 B：Java CI 手工闭环

不开发平台 UI，先用版本化 Tekton YAML 和命令验证 Java 应用从源码检出、Maven 测试到镜像推送的完整链路。详细要求见 `002-ci-foundation.md`。

### 阶段 C：GitOps/CD 手工闭环

安装 Argo CD，建立最小 GitOps 仓库，并验证指定镜像能够通过 Git 配置部署和回滚。此阶段仍不开发平台 UI。

### 阶段 D：CI/CD 串联验证

将已验收的 CI 镜像产出与 GitOps/CD 串联，验证从源码提交到开发环境部署的完整链路。

### 阶段 E：Java 控制面最小版本

使用 Java / Spring Boot 实现应用登记、流水线创建、手动触发、运行状态、制品状态和部署状态接口。

### 阶段 F：前端最小版本

实现应用、流水线、运行记录、制品和部署状态页面。

### 阶段 G：质量与自动触发

接入 Sonar Task、质量门禁和 Git Webhook，并补齐审计、失败恢复和安全扫描扩展点；随后以 Python 构建档案验证多语言扩展模型。

## 11. 待评审决策

基础 CI 的工具链选择见 [`ADR-001：CI 工具链基线`](../decisions/001-ci-toolchain-baseline.md)。当前状态如下：

| 决策 | 状态 |
|---|---|
| 第一阶段 Registry | 已决定使用 Zot 2.1.18；本地部署，保持 OCI 标准边界，不安装 Harbor |
| Maven 缓存与镜像构建器 | 已决定第一阶段关闭缓存并验证 BuildKit rootless |
| ARM64 开发构建 | 已决定第一阶段只构建 `linux/arm64`；未来 AMD64/多架构发布策略仍待评审 |
| 私有 GitLab CI 克隆认证 | 已决定使用项目级只读 Deploy Token；实例地址、仓库命名和平台级集成仍待评审 |
| Sonar | 已明确延期并优先评估外部服务；具体供应方式仍待评审 |
| 前端选择 Vue 3 还是 React | 待评审 |
| GitOps 仓库采用共享仓库还是按团队或应用拆分 | 待评审 |
| 平台初期认证采用本地账户、GitLab OAuth 还是通用 OIDC | 待评审 |
| 本地 Ingress 域名和 TLS 策略 | 待评审 |
| 平台数据库第一阶段是否部署在 K3s 内部 | 待评审，不阻塞基础 CI |
| Java 格式化、静态分析、架构约束和覆盖率工具组合 | 待评审，不阻塞基础 CI |

## 12. 规格变更规则

- 新功能必须关联已有需求编号，或先修改本规格增加需求编号。
- 改变系统边界、事实源、安全模型或验收标准时，必须先更新规格并记录理由。
- 实施中发现的约束不得仅写在代码注释或任务描述中，必须回写本规格。
- 本规格从 Draft 变为 Approved 后，才能生成正式实施计划。
