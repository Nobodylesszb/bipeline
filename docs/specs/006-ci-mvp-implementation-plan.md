# CI MVP 实施计划

- 状态：Draft v0.1
- 日期：2026-07-29
- 所有者：bo
- 父规格：`003-ci-cd-product-prototype.md`、`004-ci-control-plane-domain-model.md`、`005-ci-control-plane-api.md`
- 关联决策：`../decisions/002-flowci-inspired-product-model.md`
- 范围：Java / Spring Boot 控制面第一版实现计划；不包含完整前端和生产部署。

## 1. 目标

第一版 MVP 只追求一条能跑通的 Java Maven CI 纵向切片：

```text
CodeSource
→ Project
→ Repository
→ Java Maven Pipeline
→ Tekton PipelineRun
→ Step 状态与日志
→ BuildResult
→ Zot 镜像 digest
```

不在本计划内实现：

```text
自由拖拽流水线
开放插件市场
多项目多仓库
Webhook 自动触发
完整 CD
多语言真实执行
复杂权限
生产密钥管理
```

## 2. 实施原则

- V1 只实现 `TektonExecutionEngine`。
- 产品交互参考 FlowCI，但不 fork FlowCI。
- 领域层不依赖 Tekton CRD、GitLab SDK DTO、Registry SDK DTO。
- CodeSource 密钥本地 MVP 允许明文入库，但响应和日志必须脱敏。
- StepType 必须先有 `PluginContract`，再被 Pipeline 模板引用。
- 每个阶段必须有可验证结果，不用“大平台完成度”衡量进展。
- 当前实际 Registry 以 Zot 为准；`ADR-001` 中 Distribution 旧决策后续需要修订。

## 3. 阶段总览

```text
MVP-00 规格与决策收口
MVP-01 Spring Boot 项目骨架
MVP-02 数据库迁移与基础实体
MVP-03 CodeSource 与 Repository
MVP-04 BuildProfile 与 PluginContract
MVP-05 Pipeline 创建与流程预览
MVP-06 Run 模型与 TektonExecutionEngine 接口
MVP-07 Tekton Java Maven 纵向切片
MVP-08 日志、状态同步与 BuildResult
MVP-09 Registry/Zot digest 投影
MVP-10 API 验收与最小前端准备
```

## 4. MVP-00：规格与决策收口

目的：确保实现前没有重大方向摇摆。

任务：

```text
确认 V1 只实现 TektonExecutionEngine
确认 FlowCI 仅作为交互与模型参考
确认 CodeSource 先明文入库、响应脱敏
确认第一版一个 Project 一个 Repository
确认第一版默认 Registry 为 Zot
修订 ADR-001 中 Registry 旧描述
```

完成条件：

```text
ADR-002 Accepted for planning
003/004/005 三份规格互相一致
Registry 决策不再 Distribution/Zot 混用
```

## 5. MVP-01：Spring Boot 项目骨架

目的：建立可持续开发的 Java 后端基础。

任务：

```text
创建 Spring Boot 4.1 项目
使用 Java 25
配置 Maven Wrapper
建立模块化包结构
配置统一异常响应
配置基础测试框架
配置格式化和静态检查占位
```

建议包结构：

```text
com.pipeline.platform
├── source
├── project
├── pipeline
├── execution
├── result
├── registry
├── artifact
├── gitops
├── deploy
└── shared
```

完成条件：

```text
mvn test 通过
健康检查接口可运行
错误响应格式符合 005
```

## 6. MVP-02：数据库迁移与基础实体

目的：实现 004 中第一版表结构。

任务：

```text
选择 PostgreSQL 或 H2/PostgreSQL 双模式
引入数据库迁移工具
创建 code_sources
创建 projects
创建 repositories
创建 build_profiles
创建 plugin_contracts
创建 pipeline_configurations
创建 pipeline_steps
创建 pipeline_runs
创建 step_runs
创建 build_results
```

第一版可延后建表：

```text
release_candidates
environments
deployments
```

测试：

```text
迁移脚本可重复执行
唯一约束生效
基础 repository 保存/查询测试通过
```

完成条件：

```text
本地测试库能启动并创建所有 MVP 表
```

## 7. MVP-03：CodeSource 与 Repository

目的：支持代码源配置、连通性测试和项目仓库读取。

任务：

```text
实现 CodeSource CRUD
实现响应脱敏
实现 CodeSource verification
实现 Project 创建
创建 Project 时创建 Repository
实现 branches/tags/revision 查询
```

适配边界：

```text
GitProviderClient
├── GitLabGitProviderClient
├── GitHubGitProviderClient
├── GiteaGitProviderClient
└── GenericGitProviderClient
```

第一版可以先实现：

```text
GenericGitProviderClient
GitLabGitProviderClient 最小能力
```

测试：

```text
创建代码源后响应不返回 secret
未 VERIFIED CodeSource 不能创建项目
凭据错误返回 CODE_SOURCE_VERIFICATION_FAILED
仓库不可访问返回 REPOSITORY_NOT_ACCESSIBLE
```

完成条件：

```text
可以通过 API 配置代码源、验证、创建项目、读取分支
```

## 8. MVP-04：BuildProfile 与 PluginContract

目的：让流水线模板和插件能力可注册、可查询、可测试。

任务：

```text
启动时注册内置 BuildProfile
启动时注册内置 PluginContract
实现 build-profile 查询 API
实现 plugin-contract 查询 API
实现 plugin input schema validation API
```

V1 必须注册：

```text
java-maven-image:v1
git-checkout:v1
maven-test:v1
maven-package:v1
image-build:v1
image-push:v1
build-result:v1
```

测试：

```text
BuildProfile schema 能校验合法配置
缺少 imageName 时创建 Pipeline 失败
禁用 PluginContract 时模板展开失败
插件测试接口不真实执行插件，只做 schema 校验
```

完成条件：

```text
前端可查询模板分类和模板详情
```

## 9. MVP-05：Pipeline 创建与流程预览

目的：用户选择模板后生成可运行流水线配置。

任务：

```text
实现 POST /projects/{projectId}/pipelines
校验 BuildProfile schema
展开 PipelineStep
保存 PipelineConfiguration
实现 GET /pipelines/{id}
实现 GET /pipelines/{id}/diagram
```

Java Maven 默认步骤：

```text
checkout
maven-test
maven-package
image-build
image-push
build-result
```

测试：

```text
同项目 Pipeline 名称唯一
非法 config 返回 VALIDATION_FAILED
diagram nodes/edges 与模板一致
Pipeline 创建后 status 为 ACTIVE
```

完成条件：

```text
可以创建 main-ci 并看到只读流程预览
```

## 10. MVP-06：Run 模型与 TektonExecutionEngine 接口

目的：把平台运行记录和 Tekton 执行隔离清楚。

任务：

```text
实现 POST /pipelines/{pipelineId}/runs
创建 PipelineRun QUEUED
保存 pipelineSnapshotJson
定义 ExecutionEngine 接口
实现 TektonExecutionEngine 空适配
实现 StepRun 初始化
实现 cancel/retry 基础语义
```

接口形状：

```text
ExecutionHandle start(RunStartCommand command)
RunStatusSnapshot getStatus(ExternalRunId id)
StepLogChunk getLogs(ExternalRunId id, StepKey stepKey, Cursor cursor)
void cancel(ExternalRunId id)
```

测试：

```text
只有 ACTIVE Pipeline 可运行
每次运行 runNumber 递增
重跑创建新 PipelineRun
终态运行不能取消
```

完成条件：

```text
不接 Tekton 时，也能完整创建运行记录和 StepRun
```

## 11. MVP-07：Tekton Java Maven 纵向切片

目的：真实创建 Tekton PipelineRun。

任务：

```text
定义 Java Maven Tekton Pipeline/Task 模板
实现 PipelineConfiguration → Tekton PipelineRun 转换
处理 Git 凭据传递
处理 Workspace
处理 Maven 测试与打包
处理 BuildKit 镜像构建
处理 image-push 到 Zot
创建 externalRunId
```

安全约束：

```text
不得把 Git token 打进 PipelineRun 参数
不得把 token 打进日志
不得挂载宿主机 Docker Socket
```

测试：

```text
公开 Java Maven 仓库跑通
私有 Git 仓库认证失败可识别
Maven 测试失败后 image-build/image-push 跳过
Tekton PipelineRun 名称写入 externalRunId
```

完成条件：

```text
手动运行 main-ci 能在 K3s 里创建 Tekton PipelineRun
```

## 12. MVP-08：日志、状态同步与 BuildResult

目的：让用户能看懂运行结果。

任务：

```text
实现 Run 状态同步
实现 StepRun 状态同步
实现日志查询
实现日志脱敏
采集 Tekton Results
归一化 BuildResult
实现 GET /runs/{runId}/build-result
```

测试：

```text
Succeeded/Failed/Cancelled/TimedOut 映射正确
失败默认定位第一个失败 Step
日志不包含 Git token
BuildResult 包含 source、artifacts、reports、provenance
```

完成条件：

```text
前端可以展示运行详情、Step 状态、日志入口和 BuildResult
```

## 13. MVP-09：Registry/Zot digest 投影

目的：确认镜像真正进入 Zot，并以 digest 作为成功依据。

任务：

```text
实现默认 RegistryConnection
实现 Zot digest 查询或接收 Tekton 输出 digest
校验 image-push 结果
把 URI、tag、digest、platforms 写入 BuildResult.artifacts[]
```

测试：

```text
push 成功但无 digest 不算成功
错误 Registry 凭据导致 image-push 失败
同一 commit 重跑保留不同 run 记录
```

完成条件：

```text
BuildResult 中能看到 Zot image uri、tag、digest
```

## 14. MVP-10：API 验收与最小前端准备

目的：在写完整前端前，用 API 完成端到端验收。

任务：

```text
整理 API 示例请求
整理本地环境配置
整理错误码
补齐 README
为前端输出页面数据结构
```

验收脚本：

```text
创建 CodeSource
验证 CodeSource
创建 Project
查询 Branch
创建 Pipeline
查询 Diagram
运行 Pipeline
查询 Run
查询 Logs
查询 BuildResult
```

完成条件：

```text
不用 kubectl，仅通过 API 完成一次 Java Maven CI
```

## 15. 测试策略

单元测试：

```text
领域状态流转
BuildProfile schema 校验
PluginContract schema 校验
密钥脱敏
错误码映射
BuildResult 归一化
```

集成测试：

```text
数据库迁移
CodeSource repository
Project + Repository 创建
Pipeline 创建
Run 创建与重跑
```

系统测试：

```text
Tekton smoke
Java Maven 成功流水线
Java Maven 测试失败流水线
Zot push digest 验证
日志脱敏验证
```

## 16. 第一版完成定义

MVP 完成必须同时满足：

```text
API 可以完成端到端 Java Maven CI
Tekton PipelineRun 真实执行
Zot 中存在带 digest 的镜像
BuildResult 包含 source/artifacts/reports/provenance
失败时能定位 Step 和日志
响应不返回完整密钥
日志不泄露密钥
mvn test 通过
```

## 17. 后续阶段

MVP 完成后再进入：

```text
最小前端
Sonar Step
Trivy Step
GitOps update
Argo CD 状态展示
Webhook 触发
Python BuildProfile
Helm package/push
```
