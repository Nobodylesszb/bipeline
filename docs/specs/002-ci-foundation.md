# Java CI 子系统规格说明

- 状态：Draft v0.6
- 日期：2026-07-28
- 所有者：bo
- 父规格：`001-gitops-cicd-platform.md`
- 范围：仅 CI；不包含 GitOps 配置更新、Argo CD 或 Kubernetes 应用部署。

## 1. 决策摘要

本项目必须先完成 CI，再进入 CD。CI 的第一条纵向切片是：

```text
手动触发 PipelineRun
→ 检出指定 Git 提交
→ 执行 Java Maven 测试
→ 构建 ARM64 OCI 镜像
→ 推送镜像仓库
→ 输出结构化 BuildResult
```

完成该切片前，不开始 Argo CD、GitOps 仓库、部署审批或平台部署页面。

Harness Open Source 的 Repository、Pipeline、Stage、Step、Trigger、Secret 和统一运行详情仅作为后续产品交互参考。基础 CI 仍由 Tekton 执行；不得引入 Harness/Drone 执行引擎、宿主机 Docker Socket 或 Harness 自带部署步骤。本规格优先稳定底层契约，参考设计对应的引导式创建和运行详情在父规格 `FR-019`、`FR-024` 与 `AC-015` 中验收。

## 2. CI 阶段目标

### CI-G-001：证明 Tekton 执行基础可靠

单节点 K3s 中的 Tekton 能稳定创建 PipelineRun、调度 Task、共享工作区并保存足够的失败信息。

### CI-G-002：证明 Java 构建链路可靠

指定 Java Maven 仓库和提交后，流水线能重复执行测试，并在测试成功后构建镜像。

### CI-G-003：产出不可变且可追踪的制品

镜像必须与源码提交和 PipelineRun 建立关联，并输出镜像 digest；不得只依赖 `latest`。

### CI-G-004：形成可复用的模板契约

Java CI 流程必须通过版本化 Tekton Task、Pipeline 和 `build-profile` 表达，后续 Sonar、覆盖率、安全扫描和新语言能够在不修改核心执行模型的情况下接入。

### CI-G-005：为 Java 控制面提供稳定接口

在开发 Spring Boot 控制面前，必须先稳定 Pipeline 参数、结果、状态和失败语义，避免后端绑定临时 YAML 结构。

## 3. 范围

### 3.1 本规格包含

- CI 所需的 K3s 基础能力；
- Tekton Pipelines；
- Java Maven 示例项目；
- Git 仓库检出；
- Maven 单元测试；
- OCI 镜像构建与推送；
- 版本化 Task 与 Pipeline；
- 手动触发与状态查询；
- 失败短路、日志和基础审计数据；
- Sonar 接入所需扩展点；
- 后续 Java 控制面需要消费的稳定契约。

### 3.2 本规格不包含

- Argo CD；
- GitOps 仓库及其更新；
- 应用部署和回滚；
- Git Webhook 的公网接入；
- 生产级高可用；
- 在 Java CI 通过 Exit Gate 前同时实现多语言构建；通过后必须用 Python 验证扩展模型；
- 可视化流水线编辑器；
- 完整用户权限系统；
- 制品签名、SBOM 和安全扫描的实际实现。

## 4. 目标拆分与依赖顺序

```text
CI-00 规格确认
  ↓
CI-01 基础环境就绪
  ↓
CI-02 Tekton 最小运行验证
  ↓
CI-03 Git 检出与提交解析
  ↓
CI-04 Maven 测试
  ↓
CI-05 镜像构建与推送
  ↓
CI-06 Java Pipeline 模板固化
  ↓
CI Exit Gate
  ↓
允许进入 GitOps/CD 手工闭环
```

后一个目标不得以绕过前一目标验收标准的方式提前实现。

Java 控制面、前端、Sonar 和 Webhook 属于端到端 CI/CD 链路验证后的产品化目标，不阻塞从 CI 进入 CD。

## 5. 各目标详细规格

### CI-00：规格确认

**目的**：在安装组件前锁定最小 CI 边界和关键技术选择。

**必须确认**：

- Tekton Pipelines 的目标版本；
- Java 与 Spring Boot 的版本；
- 示例 Maven 项目结构；
- 第一阶段 Registry；
- 镜像构建器；
- Git 仓库认证方式；
- Pipeline 参数与结果契约。

**完成条件**：本规格状态从 Draft 变为 Approved，所有阻塞项均有明确决定。

### CI-01：基础环境就绪

**目的**：提供运行 CI 的最小 Kubernetes 基础，不安装 CD 组件。

**范围**：

- 单节点 K3s 正常运行；
- `kubectl` 可用；
- 默认 StorageClass 可创建和绑定 PVC；
- 创建 CI 专用 namespace；
- Registry 可从集群访问；
- ARM64 节点和镜像兼容性已确认；
- 时间、DNS 和基础网络正常。

**输出**：环境检查记录，不输出业务镜像。

**完成条件**：节点 Ready，测试 Pod、PVC 和 Registry 连通性均通过。

### CI-02：Tekton 最小运行验证

**目的**：证明 Tekton 控制器、TaskRun 和 PipelineRun 在本地 K3s 中工作正常。

**范围**：

- 安装 Tekton Pipelines；
- 使用独立 ServiceAccount；
- 运行不访问外部系统的 smoke Task；
- 运行包含至少两个 Task 的 smoke Pipeline；
- 验证 Task 间结果传递和 Workspace 挂载；
- 验证成功、失败和超时三种终态。

**完成条件**：相同 smoke Pipeline 连续运行三次均得到预期结果；故意失败与超时能够被明确识别。

### CI-03：Git 检出与提交解析

**目的**：从明确的仓库和 revision 得到确定的源码快照。

**输入**：仓库 URL、revision、可选 context directory、Git 凭据引用。

**输出**：解析后的完整 commit SHA、源码 Workspace、仓库来源信息。

**约束**：

- 不得把 Git 用户名、密码或 Token 写入 Pipeline 参数、日志或 Git；
- 构建记录必须保存解析后的完整 commit SHA，而不是只保存分支名；
- 检出失败必须停止后续 Task；
- 首个样例可使用公开仓库验证，再增加私有 GitLab 仓库验证。

**完成条件**：分支、Tag 和完整 SHA 三种 revision 均能解析为确定提交；无效地址和无权限仓库返回不同失败原因。

### CI-04：Maven 测试

**目的**：使用确定的 JDK 与 Maven 环境验证 Java 项目行为。

**输入**：源码 Workspace、context directory、Maven goals、JDK 版本、可选 Maven settings Secret。

**默认行为**：

```text
优先执行项目 Maven Wrapper
→ 执行单元测试
→ 保存测试报告
→ 输出测试摘要
```

**约束**：

- 测试失败时不得继续构建或推送镜像；
- 构建环境必须固定 JDK 主版本和构建镜像版本；
- Maven 私服凭据只能通过 Secret 挂载；
- Maven 缓存是优化项，不得成为正确性前提；
- 测试报告必须可关联到 PipelineRun 和 commit SHA。

**完成条件**：正常样例成功；故意失败的测试阻止后续阶段；缺少 `pom.xml`、JDK 不兼容和依赖下载失败能够被区分。

### CI-05：镜像构建与推送

**目的**：在不挂载宿主机 Docker Socket 的前提下构建并推送 OCI 镜像。

**输入**：源码 Workspace、Dockerfile 或构建上下文、镜像仓库地址、镜像名称、不可变标签、Registry 凭据引用。

**输出**：完整镜像引用、镜像 digest、非空 `platforms[]`、构建时间。

**约束**：

- 不得把 `/var/run/docker.sock` 挂载到 Tekton Task；
- 第一阶段 `platforms[]` 必须至少包含当前 K3s 所需的 `linux/arm64`；未来可增加 `linux/amd64` 等平台；
- 标签至少包含 commit SHA 或等价不可变标识；
- 流水线成功以 Registry 返回 digest 为准；
- Registry 推送失败不得被记录为成功；
- 重跑同一提交时必须保留可追踪性，不得覆盖唯一运行记录。

**完成条件**：Registry 中可按 digest 拉取镜像；镜像在 ARM64 测试容器中成功启动；错误凭据和不可达 Registry 返回明确失败。

### CI-06：Java Pipeline 模板固化

**目的**：把已验证的独立 Task 组合成稳定、版本化的 Java CI 模板。

**要求**：

- Task 与 Pipeline 清单存放在版本控制中；
- 模板使用显式版本号；
- 参数、结果和 Workspace 契约符合第 6 节；
- 每个 Task 具有合理超时、资源 request/limit 和失败语义；
- 失败默认短路，清理动作使用 finally Task；
- 模板不得包含某个业务项目的硬编码地址或凭据；
- 模板升级不得静默改变已有运行的语义。

**完成条件**：两个不同名称但相同结构的 Java 示例项目可复用同一模板完成 CI。

### POST-CI-01：Java 控制面接入

**目的**：使用 Java / Spring Boot 通过 Kubernetes API 创建和查询 CI 资源。

**最小能力**：

- 登记 Java 应用；
- 创建流水线配置；
- 手动触发 PipelineRun；
- 查询 PipelineRun 和 TaskRun 状态；
- 查询日志定位信息；
- 展示 commit SHA、镜像引用和 digest；
- 重新运行失败流水线，并保留原记录。

**边界**：

- 后端只编排和投影 Tekton 状态，不实现执行引擎；
- 后端数据库不成为 PipelineRun 状态事实源；
- Kubernetes 客户端必须位于适配器模块；
- 领域层不得依赖 Kubernetes CRD 类型。

**完成条件**：通过 API 可完成一次创建、运行、查询和重跑流程；重启后端不影响正在执行的 PipelineRun。

### POST-CI-02：CI 前端最小版本

**目的**：让用户不使用 `kubectl` 即可完成最基本的 CI 操作。

**页面范围**：

- 应用列表与创建；
- 流水线配置；
- 手动运行；
- 运行列表与阶段状态；
- 失败原因与日志入口；
- 制品引用和 digest。

**非目标**：拖拽编排、部署页面、复杂权限和运营大盘。

**完成条件**：用户能从前端创建并运行一个 Java CI 流水线，并定位一次失败测试。

### POST-CI-03：Sonar 与自动触发扩展

**目的**：验证 CI 模板扩展点，而不是改变核心执行模型。

**范围**：

- Sonar Scan 作为独立 Task；
- Quality Gate 决定是否允许继续构建镜像；
- Sonar 关闭时不影响基础 CI；
- Git Webhook 触发接口；
- 重复 Webhook 事件幂等处理；
- CI 审计和重试策略。

**完成条件**：开启 Sonar 后，扫描结果进入 `BuildResult.reports[]`，质量门禁失败会阻止生成 `ReleaseCandidate`；关闭 Sonar 后，同一模板仍可运行基础 CI。

## 6. Pipeline 稳定契约

### 6.1 参数

#### 通用参数

| 参数 | 必填 | 说明 |
|---|---:|---|
| `application-name` | 是 | 平台内稳定应用标识 |
| `build-profile` | 是 | 构建档案，例如 `java-maven`、`python` |
| `template-version` | 是 | 构建档案绑定的 Pipeline 兼容版本 |
| `git-url` | 是 | 源码仓库地址 |
| `git-revision` | 是 | 分支、Tag 或 SHA |
| `context-dir` | 否 | 单仓多项目时的相对路径，默认仓库根目录 |
| `image-repository` | 是 | 不包含可变标签的目标仓库 |
| `image-tag` | 否 | 默认由 commit SHA 和运行标识生成 |
| `sonar-enabled` | 否 | 默认 `false` |

凭据、Token 和密码不是普通参数，必须以 Kubernetes Secret 引用传递。

#### `java-maven` 构建档案参数

| 参数 | 必填 | 说明 |
|---|---:|---|
| `jdk-version` | 是 | 构建使用的 JDK 主版本 |
| `maven-goals` | 否 | 默认测试目标；必须受 Java 模板规则约束 |
| `maven-settings-secret` | 否 | Maven 私服配置 Secret 引用 |

Python 等后续语言使用自己的参数 Schema，不得把语言专属参数提升为通用必填参数。

### 6.2 结果

| 结果 | 来源 | 说明 |
|---|---|---|
| `commit-sha` | Git Task | 完整源码提交 SHA |
| `test-summary` | Maven Task | 可机器读取的测试摘要或报告位置 |
| `package-artifacts` | Package Task | 可选；零到多个 JAR、Wheel 等包的结构化引用 |
| `image-url` | Image Task | 带不可变标签的镜像引用 |
| `image-digest` | Image Task | Registry 返回的内容 digest |
| `image-platforms` | Image Task | 非空平台列表；第一阶段至少包含 `linux/arm64` |
| `template-ref` | Pipeline | 实际使用的构建档案或模板标识 |
| `template-version` | Pipeline | 实际使用的模板版本 |

平台读取 Task 结果后必须归一化为 `BuildResult`：

```yaml
buildResult:
  source:
    repository: <git-url>
    revision: <commit-sha>
  artifacts:
    - name: application-jar
      kind: java-jar
      role: package
      uri: <artifact-uri>
      digest: <artifact-digest>
    - name: application-image
      kind: oci-image
      role: runtime
      uri: <image-url>
      digest: <image-digest>
      platforms:
        - linux/arm64
  reports:
    - kind: unit-test
      status: passed
      uri: <report-uri>
  provenance:
    templateRef: <template-ref>
    templateVersion: <template-version>
```

- `artifacts[]` 是多值集合；不得恢复为单值 `packageArtifact` 或 `deployableArtifact`。
- `reports[]` 与制品分离，可扩展单元测试、覆盖率、Sonar 和安全扫描结果。
- `provenance` 必须足以定位实际模板及版本；后续可扩展构建器、SBOM 和签名来源。
- Java 服务可以保留 JAR，Python 服务可以保留 Wheel，但服务的运行时制品必须是带 digest 的 OCI 镜像。
- 公共库可以只有 `role = package` 的制品，不进入 GitOps/CD。
- CI 只陈述构建产生了什么，不判定制品是否获准部署。

`BuildResult` 不直接进入 GitOps/CD。父规格定义的独立策略层对报告和制品元数据进行判定，通过后才生成 `ReleaseCandidate`：

```yaml
releaseCandidate:
  application: order-service
  sourceRevision: <commit-sha>
  components:
    - name: application
      artifact:
        kind: oci-image
        uri: <image-url>
        digest: <image-digest>
  policyStatus: passed
```

CD 只消费通过策略的 `ReleaseCandidate`，不根据 Java、Python 等语言判断是否可部署。第一版只实现一个 Java OCI 运行时镜像，但契约必须允许未来一个构建产生多个镜像、包、SBOM、签名和多平台信息。

### 6.3 终态

```text
Pending → Running → Succeeded
                  ↘ Failed
                  ↘ Cancelled
                  ↘ TimedOut
```

平台不得把未知或仍在运行的状态映射为成功。

### 6.4 构建档案扩展模型

每个构建档案必须声明：

| 字段 | 说明 |
|---|---|
| `name` | 稳定标识，例如 `java-maven`、`python` |
| `version` | 档案兼容版本 |
| `pipeline-ref` | 对应的版本化 Tekton Pipeline |
| `parameter-schema` | 语言专属参数及校验规则 |
| `capabilities` | test、package、image、sonar 等能力声明 |
| `result-contract` | 必须满足的通用 `BuildResult` 契约 |

- **CI-EXT-001** 平台核心只解析档案元数据，不解释 Maven、pytest、npm 或 Go 命令。
- **CI-EXT-002** 通用 Task 包括 Git 检出、镜像元数据和结果发布；语言测试与打包由档案专属 Task 提供。
- **CI-EXT-003** 不允许构建一个通过大量语言条件分支运行的通用巨型 Pipeline。
- **CI-EXT-004** 新档案不得改变已有档案的参数含义和结果语义。
- **CI-EXT-005** Python 作为第二个档案，至少覆盖依赖安装、pytest 和 OCI 镜像构建，用于验证扩展边界。

## 7. 安全约束

- **CI-SEC-001** Git、Maven 私服和 Registry 凭据必须使用独立 Secret。
- **CI-SEC-002** Task 使用的 ServiceAccount 必须采用最小权限。
- **CI-SEC-003** 构建 Task 不得获得集群管理员权限。
- **CI-SEC-004** 流水线日志必须对常见 Token、密码和认证头做泄漏检查。
- **CI-SEC-005** 不运行来自不受信仓库的特权容器。
- **CI-SEC-006** 镜像构建不得依赖宿主机 Docker Socket。

## 8. 可靠性与资源约束

- **CI-NFR-001** 基础 CI 在 12 GB 虚拟机内存中可完成，且不得造成节点持续内存压力。
- **CI-NFR-002** 每个 Task 必须配置超时；Pipeline 必须具有总超时。
- **CI-NFR-003** Task 必须声明合理的 CPU 与内存 request/limit。
- **CI-NFR-004** 对外部系统的重试必须有上限，不得无限重试。
- **CI-NFR-005** PipelineRun 和临时 Workspace 必须具有清理策略，但清理不得先于结果采集。
- **CI-NFR-006** 相同输入必须解析到相同源码提交；构建时间等非确定信息不得改变提交身份。
- **CI-NFR-007** 日志必须包含 PipelineRun、TaskRun、应用和 commit SHA 的关联标识。

## 9. CI 验收场景

### CI-AC-001：基础成功路径

给定一个测试通过的 Java Maven 仓库，手动运行后得到 Succeeded，以及包含完整 commit SHA、镜像 URL、digest、`platforms[]`、单元测试报告和模板来源的 `BuildResult`。

### CI-AC-002：测试失败短路

给定一个存在失败测试的提交，PipelineRun 为 Failed，镜像构建与推送 Task 不执行。

### CI-AC-003：Git 失败

无效仓库地址、无权限仓库和不存在 revision 分别产生可区分的失败信息。

### CI-AC-004：Registry 失败

错误凭据或不可达 Registry 导致 PipelineRun 失败，不产生伪造 digest。

### CI-AC-005：可重复运行

同一提交连续运行三次，均使用同一 commit SHA；每次运行记录可区分，制品可追踪。

### CI-AC-006：ARM64 可运行

构建镜像可在当前 ARM64 K3s 节点启动并通过最小健康验证。

### CI-AC-007：凭据不泄漏

Pipeline、PipelineRun、TaskRun、日志和普通 Git 文件中不出现明文凭据。

### CI-AC-008：模板复用

两个 Java Maven 示例项目复用同一版本模板成功完成 CI，无项目地址硬编码。

### CI-AC-009：语言无关契约

通用 PipelineRun 请求只依赖通用参数；Java 专属参数通过 `java-maven` 档案校验和传递，核心模型不存在 Maven 或 JDK 必填字段。

### CI-AC-010：构建结果归一化

Java 服务流水线成功后，`BuildResult.artifacts[]` 中包含带 digest 与 `linux/arm64` 平台的 OCI 运行时制品；JAR 是否作为包制品保留不影响构建结果。未来 Python 服务遵循相同契约，Wheel 仅作为可选包制品。

### CI-AC-011：CI 与发布策略解耦

CI 可以输出包含镜像和质量报告的 `BuildResult`，但不能直接更新 GitOps 或声明可部署；只有父规格所定义的策略层可以生成 `ReleaseCandidate`。

### POST-CI-AC-001：控制面恢复

Java 控制面重启后，能够重新读取仍在运行或已经结束的 Tekton 状态。

### POST-CI-AC-002：Sonar 可选

Sonar 关闭时基础 CI 不受影响；开启后扫描结果进入 `BuildResult.reports[]`，质量门禁失败会阻止生成 `ReleaseCandidate`，但允许保留已构建镜像供审计。

## 10. CI Exit Gate

只有同时满足以下条件，才允许开始 GitOps/CD：

1. `CI-AC-001` 至 `CI-AC-011` 全部通过；
2. 基础 Java Pipeline 模板已版本化；
3. CI 参数与 `BuildResult` 契约已冻结为首个兼容版本；
4. 凭据泄漏检查通过；
5. ARM64 镜像成功运行；
6. 已记录已知限制和未完成优化项；
7. CI 失败不会被平台错误展示为成功。
8. CI 模板不包含 GitOps 更新或应用部署逻辑。

`POST-CI-AC-001` 和 `POST-CI-AC-002` 在端到端 CI/CD 链路验证后执行，不得反向阻塞基础 CI 或 GitOps/CD 手工闭环。

## 11. 待决策项

`CI-01` 至 `CI-06` 的实施阻塞项已在 [`ADR-001：CI 工具链基线`](../decisions/001-ci-toolchain-baseline.md) 中形成决定：

| 原待决策项 | 状态 |
|---|---|
| K3s、Tekton、Java、Spring Boot、Maven、Registry、构建器版本 | 已决定，等待运行时兼容性验证 |
| Maven Wrapper、Workspace、缓存、Git 认证、报告与保留策略 | 已决定 |
| 构建档案注册方式 | 已决定使用 Git 版本化清单 |
| Sonar、CI 前端、Python 第二档案 | 明确延期，不阻塞基础 CI |

CI 规格仍保持 Draft，直到 CI 实施计划与测试计划完成评审。ADR 中的版本组合通过本地 smoke test 后，其状态才能从 `Accepted for planning` 改为 `Validated`。

## 12. 下一规格动作

1. 评审本规格的范围和目标顺序。
2. 基于 `ADR-001` 生成 CI 实施计划和测试计划，并把第 5 节运行时兼容性门槛排在计划最前。
3. 评审规格、ADR、实施计划和测试计划的追踪关系。
4. 将本规格状态从 Draft 更新为 Approved。
5. 依次执行 CI-01 至 CI-06；兼容性门槛通过后将 `ADR-001` 标记为 `Validated`。
6. CI Exit Gate 通过后，编写独立的 GitOps/CD 子规格，再进入 CD 实施。

在完成以上动作前，不开始安装 CI 组件或编写平台代码。
