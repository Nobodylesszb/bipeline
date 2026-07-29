# ADR-003：第一版采用 Jenkins 作为 CI 执行引擎

- 状态：Accepted
- 日期：2026-07-29
- 决策范围：CI 执行引擎、外部工具链、第一版纵向切片
- 关联规格：`../specs/003-ci-cd-product-prototype.md`、`../specs/004-ci-control-plane-domain-model.md`、`../specs/006-ci-mvp-implementation-plan.md`

## 1. 背景

平台最初评估过 Tekton/K8s 原生路线，但当前阶段只有一名开发者，本地环境和 Kubernetes 组件尚未稳定。为了尽快做出完整 CI/CD 闭环，第一版需要优先选择更容易本地部署、调试、观测和排错的执行引擎。

Jenkins 生态成熟，天然支持 GitLab、Maven、SonarQube、Docker/镜像仓库、Harbor 和 Kubernetes 部署链路。已有大量 Jenkinsfile 和脚本样板可参考，能显著降低第一版实现风险。

## 2. 决策

第一版采用 Jenkins 作为 CI 执行引擎。

```text
V1: JenkinsExecutionEngine
Future: TektonExecutionEngine / AgentExecutionEngine
```

业务项目仓库第一版不需要提交 `Jenkinsfile`。流水线配置保存到平台数据库，Java 后端根据 `PipelineConfiguration` 和 `PipelineStep` 生成 Jenkins Pipeline Script，并通过 Jenkins API 创建或更新 Job、触发构建、查询状态和读取日志。

## 3. 工具链链路

第一版参考成熟 Jenkins CI/CD 链路：

```text
GitLab
→ Jenkins
→ Maven Test / Package
→ SonarQube，可选
→ Docker Build，可选
→ Harbor / Zot Push，可选
→ Kubernetes Deploy，可选
```

首个闭环只做：

```text
GitLab
→ Jenkins
→ Maven Test
→ PipelineRun / StepRun / BuildResult 投影
```

后续再按插件步骤逐步接入 Sonar、Registry 和 K8s。

## 4. 边界

- `execution.domain` 不依赖 Jenkins SDK、Jenkins JSON、Jenkinsfile 语法或 HTTP DTO。
- Jenkins 适配代码只允许放在 `execution.infrastructure.jenkins`。
- Jenkins Pipeline Script 是运行时生成物，不是平台核心领域模型。
- 用户看到的是平台 Step，不是 Jenkins Stage 的底层配置。
- 业务仓库不强制包含 `Jenkinsfile`。

## 5. 采用内容

- 采用 Jenkins API 管理 Job 和 Build。
- 采用 Jenkins Pipeline Script 作为执行载体。
- 参考传统 Jenkinsfile 的 stage 拆分方式。
- 支持 GitLab、Harbor、Sonar、K8s 这条成熟链路。
- 保留 `ExecutionEngine` 抽象，避免后续换 Tekton 时重写核心模型。

## 6. 不采用内容

- 不直接复用第三方 demo 项目的业务代码。
- 不把 Jenkinsfile 作为用户必须维护的项目文件。
- 不让平台核心领域对象持有 Jenkins Job、Build、Stage DTO。
- 不在第一版实现自由编辑 Jenkins Pipeline Script。
- 不在第一版同时实现 Tekton。

## 7. 后果

正向影响：

- 第一版更容易快速跑通完整 CI。
- 本地调试和日志查看成本低于 Tekton/K8s 原生链路。
- 可参考的 Jenkinsfile、Sonar、Harbor、K8s 样板更多。
- 业务项目无需改造即可接入平台。

代价：

- Jenkins 成为第一版运行事实源，平台需要同步 Jenkins 构建状态。
- 后端需要维护 PipelineConfiguration 到 Jenkins Pipeline Script 的转换。
- 后续切换 Tekton 时需要新增适配器和模板生成器，但核心模型不应重写。

## 8. 被拒绝方案

| 方案 | 拒绝理由 |
|---|---|
| 第一版继续 Tekton | 本地环境复杂度高，拉长首个闭环验证周期 |
| 业务仓库强制 Jenkinsfile | 与“平台管理流水线配置”的产品目标冲突 |
| 直接 fork Jenkins demo 项目 | demo 是业务服务，不是 CI 平台控制面 |
| 自研 Agent 执行器 | 第一版调度、日志、隔离和安全成本过高 |

## 9. 验收方式

```text
保存 GitLab 代码源
创建 Project 和 Repository
选择 Java Maven CI 模板
平台生成 Jenkins Pipeline Script
平台通过 Jenkins API 创建或更新 Job
平台触发 Jenkins Build
平台查询 Build 状态并同步 PipelineRun / StepRun
失败时能查看 Jenkins 日志入口
成功时生成 BuildResult
```
