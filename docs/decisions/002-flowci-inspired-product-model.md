# ADR-002：参考 FlowCI 的产品交互与流水线模型

- 状态：Accepted for planning
- 日期：2026-07-29
- 决策范围：CI/CD 平台产品模型、前端交互、流水线配置模型
- 关联规格：`../specs/003-ci-cd-product-prototype.md`
- 复审条件：FlowCI 模型无法支撑首个 Java CI 纵向切片，或执行引擎抽象导致第一版复杂度不可接受。

## 1. 背景

平台已确定采用 Java / Spring Boot 自研控制面，并希望先做出一个可运行、可扩展的 CI/CD MVP。前期讨论中发现，仅描述底层 CI/CD 组件不足以指导产品实现；平台还需要清晰的创建流程、模板选择、流水线画布、Step 配置和运行详情体验。

FlowCI 是 Java 生态的开源 CI/CD Server，具有模板、Job / Step / Plugin、运行日志、Agent 和可视化流水线等产品概念。它的交互模式适合作为本平台第一版产品设计参考。

## 2. 决策

采用 FlowCI 的产品交互和流水线模型作为参考，但不直接 fork FlowCI 作为核心代码底座。

第一版产品形态：

```text
配置代码源
→ 测试连通性并保存
→ 创建项目并选择代码源
→ 配置项目仓库
→ 创建流水线
→ 选择模板
→ 展示只读流程画布
→ 手动运行
→ 查看运行详情、日志、制品和报告
```

第一版领域模型：

```text
CodeSource
Project
Repository
PipelineConfiguration
PipelineStep
PipelineRun
BuildResult
ReleaseCandidate
Environment
Deployment
```

FlowCI 概念映射：

```text
FlowCI Job     → PipelineConfiguration
FlowCI Step    → PipelineStep
FlowCI Plugin  → StepType / BuiltInPlugin
FlowCI Agent   → ExecutionWorker / ExecutionEngine
FlowCI Run     → PipelineRun
```

## 3. 采用内容

- 参考 FlowCI 的项目创建和流水线创建交互。
- 参考 FlowCI 的语言分类模板选择方式。
- 采用 Job / Step / Plugin 的用户心智模型。
- 采用流程画布表达流水线阶段和并行关系。
- 采用运行详情中集中展示状态、日志、报告和制品的体验。
- 保留 Step 可并行、可配置运行环境、可扩展插件的长期方向。

## 4. 不采用内容

- 不直接 fork FlowCI 作为平台核心实现。
- 不要求业务仓库提交 `.flowci.yml` 或其他平台专用 CI 文件。
- 不把 FlowCI Agent 作为第一版执行模型。
- 不在第一版实现完整动态 Agent 自动伸缩。
- 不在第一版开放第三方插件市场。
- 不让 CI Step 直接部署应用；部署必须通过 `ReleaseCandidate`、GitOps 仓库和 Argo CD。

## 5. 执行引擎边界

平台核心领域层不得依赖某一个执行引擎。执行能力通过 `ExecutionEngine` 抽象隔离：

```text
V1: JenkinsExecutionEngine
Future: TektonExecutionEngine / AgentExecutionEngine
```

第一阶段只实现 `JenkinsExecutionEngine`。`TektonExecutionEngine` 和 `AgentExecutionEngine` 仅作为未来候选方向，不进入第一版实现计划。后续如果增加 Tekton 或 FlowCI 风格 Agent 路线，应只替换适配器和模板，不重写 Project、Pipeline、Run 和 BuildResult。

## 6. 第一版约束

- 一个 Project 只引用一个 CodeSource。
- 一个 Project 只配置一个 Repository。
- 一个 Project 可以创建多条 Pipeline。
- Pipeline 配置保存到平台数据库。
- 模板和 Step 类型必须版本化。
- 第一版流程画布只读，不做自由拖拽。
- 第一版插件全部内置，不支持上传第三方插件。
- 本地 MVP 阶段允许 Git Token / SSH Key 明文保存到数据库。

## 7. 后果

正向影响：

- 产品体验更具体，不再只有底层组件架构。
- 第一版能用模板和画布快速获得平台感。
- 后端可以围绕稳定领域对象设计数据库和接口。
- 后续支持 Tekton 或 Agent 执行时，不需要改变用户侧模型。

代价：

- 需要设计并维护自己的 PipelineConfiguration 和 StepType 模型。
- 即使执行层使用 Jenkins，也要做一层产品模型到 Jenkins Pipeline Script 的转换。
- FlowCI 只是参考来源，不能直接复用其所有代码和生态。

## 8. 被拒绝方案

| 方案 | 拒绝理由 |
|---|---|
| 直接 fork FlowCI | 会继承其 Agent 执行模型和历史实现，后续接入 GitOps/Argo CD 与自定义产品模型可能更重 |
| 直接暴露 Tekton YAML | 用户体验差，业务用户需要理解 Kubernetes CRD，后续大改风险高 |
| 业务仓库强制提交 Jenkinsfile | 与平台保存流水线配置、统一生成执行脚本的目标冲突 |
| 第一版做自由拖拽流水线编辑器 | 实现成本高，容易拖慢 Java CI 纵向切片 |
| 业务仓库内保存 CI 配置 | 与当前目标冲突；第一版要求平台数据库保存流水线配置 |

## 9. 验证方式

本 ADR 是否成立，以第一条 Java Maven CI 纵向切片验证：

```text
代码源可保存并验证
项目可选择代码源并读取仓库分支
流水线可选择 Java Maven 模板
流程画布可展示模板步骤
手动运行可生成执行记录
执行成功后可展示 BuildResult
失败时可定位到 Step 日志
```
