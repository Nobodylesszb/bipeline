# Java 控制面项目结构规范

- 状态：Draft v0.1
- 日期：2026-07-29
- 所有者：bo
- 父规格：`004-ci-control-plane-domain-model.md`、`005-ci-control-plane-api.md`、`006-ci-mvp-implementation-plan.md`
- 范围：Spring Boot 控制面代码结构、依赖方向、命名规则和测试结构。

## 1. 目标

本文规定 Java 控制面的初始项目结构，防止后续实现越写越散。代码质量基线采用《代码整洁之道》的核心原则：

```text
小类
短方法
清晰命名
单一职责
显式边界
持续重构
测试保护行为
```

第一版采用模块化单体，不拆微服务，不做过度抽象。

## 2. 根目录结构

```text
pipeline/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── README.md
├── docs/
│   ├── decisions/
│   └── specs/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/pipeline/platform/
│       │   └── resources/
│       └── test/
│           └── java/
│               └── com/pipeline/platform/
└── templates/
    └── tekton/
```

说明：

- `backend` 是 Java / Spring Boot 控制面。
- `templates/tekton` 存放平台级 Tekton Task/Pipeline 模板。
- 业务仓库不存放平台 CI 配置。
- 如果第一版为了简单不建 Maven multi-module，也仍保留 `backend/` 目录，避免后续前端或模板混在根目录。

## 3. Java 包结构

根包：

```text
com.pipeline.platform
```

推荐结构：

```text
com.pipeline.platform
├── PlatformApplication.java
├── shared
│   ├── api
│   ├── error
│   ├── time
│   ├── id
│   └── security
├── source
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
├── project
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
├── pipeline
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
├── execution
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
├── result
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
├── registry
│   ├── application
│   ├── domain
│   └── infrastructure
├── artifact
│   ├── application
│   ├── domain
│   └── infrastructure
├── gitops
│   ├── application
│   ├── domain
│   └── infrastructure
└── deploy
    ├── application
    ├── domain
    └── infrastructure
```

注意：

- 不使用 `package` 作为 Java 包名，因为 `package` 是 Java 关键字。
- Helm Chart、Jar、Wheel 等包制品相关代码放在 `artifact` 模块。
- REST Controller 只放在各模块 `api` 包。

## 4. 分层职责

每个业务模块使用一致的四层：

```text
api
  Controller、Request DTO、Response DTO、API Mapper

application
  UseCase、Command、Query、事务边界、编排领域对象

domain
  Entity、Value Object、Domain Service、Repository 接口、领域异常

infrastructure
  JPA Entity、Spring Data Repository、外部客户端、适配器、配置类
```

依赖方向：

```text
api → application → domain
infrastructure → domain
application → domain
application → infrastructure 仅通过接口和 Spring 注入
domain 不依赖 api/application/infrastructure
```

禁止：

```text
Controller 直接访问 JPA Repository
Controller 写业务规则
domain 引用 Spring MVC、JPA、Tekton、GitLab DTO
infrastructure 类型泄露到 API 响应
跨模块直接访问对方 infrastructure
```

## 5. 模块职责

### 5.1 shared

放通用但稳定的基础设施：

```text
ApiResponse
ErrorResponse
ErrorCode
BusinessException
ClockProvider
IdGenerator
SecretMasker
```

规则：

- `shared` 不能变成杂物间。
- 只有两个以上模块真实复用的能力才能进入 `shared`。

### 5.2 source

负责代码源和 Git 仓库读取。

核心类：

```text
CodeSource
CodeSourceRepository
CodeSourceVerifier
GitProviderClient
RepositoryRevisionResolver
SecretMasker
```

适配器：

```text
GitLabGitProviderClient
GitHubGitProviderClient
GiteaGitProviderClient
GenericGitProviderClient
```

第一版至少实现 Generic Git 和 GitLab 最小能力。

### 5.3 project

负责项目生命周期和项目仓库设置。

核心类：

```text
Project
ProjectRepository
ProjectCreator
ProjectReader
Repository
ProjectRepositorySettings
```

规则：

- 一个 Project 第一版只允许一个 Repository。
- 创建 Project 必须引用 VERIFIED CodeSource。

### 5.4 pipeline

负责流水线配置、模板、步骤和流程预览。

核心类：

```text
BuildProfile
PluginContract
PipelineConfiguration
PipelineStep
PipelineCreator
PipelineDiagramBuilder
PluginContractValidator
```

规则：

- `PipelineConfiguration.configJson` 必须由 `BuildProfile.schemaJson` 校验。
- `PipelineStep.stepType` 必须存在启用的 `PluginContract`。
- 第一版流程图只读，不做自由拖拽。

### 5.5 execution

负责运行记录和 Tekton 执行。

核心类：

```text
PipelineRun
StepRun
ExecutionEngine
TektonExecutionEngine
RunStarter
RunStatusSynchronizer
RunCanceller
RunRetryService
RunLogReader
```

规则：

- V1 只实现 `TektonExecutionEngine`。
- `PipelineRun.status` 是平台投影，Tekton/Kubernetes 是执行事实源。
- 重跑必须创建新 `PipelineRun`，不能覆盖原记录。

### 5.6 result

负责构建结果、制品、报告和发布候选。

核心类：

```text
BuildResult
BuildArtifact
BuildReport
BuildResultCollector
BuildResultNormalizer
ReleaseCandidate
```

规则：

- `BuildResult` 不等于可部署版本。
- `ReleaseCandidate` 后续由策略层从 `BuildResult` 生成。
- `artifacts` 必须支持多制品、多平台、SBOM、签名扩展。

### 5.7 registry

负责镜像仓库连接和 digest 校验。

核心类：

```text
RegistryConnection
RegistryClient
ImageReference
ImageDigest
ImagePushResult
```

规则：

- V1 默认 Registry 是 Zot。
- `image-push` 成功必须有 digest。
- 业务流程不能写死 Zot 地址。

### 5.8 artifact

负责 Helm Chart、Jar、Wheel 等包制品扩展。

核心类：

```text
HelmRepositoryConnection
ChartReference
PackageArtifact
```

第一版只预留边界，真实 Helm 推送后续实现。

### 5.9 gitops

负责 GitOps 仓库更新。

第一版只预留接口，CI 不直接部署应用。

核心类：

```text
GitOpsRepository
GitOpsUpdater
GitOpsChange
```

### 5.10 deploy

负责环境和部署状态投影。

第一版只预留 DEV 环境模型，完整 Argo CD 状态后续实现。

核心类：

```text
Environment
Deployment
DeploymentStatusReader
ArgoApplicationStatus
```

## 6. 类命名规则

使用意图清晰的名字，不用空泛后缀堆叠。

推荐：

```text
CreateCodeSourceRequest
CodeSourceResponse
VerifyCodeSourceUseCase
CodeSourceVerifier
GitProviderClient
PipelineCreator
PipelineDiagramBuilder
RunStarter
BuildResultCollector
SecretMasker
```

避免：

```text
CodeSourceManager
PipelineUtil
CommonService
DataHandler
BaseController
AbstractProcessor
```

允许 `Service` 后缀的场景：

```text
确实表达领域服务，例如 RunRetryService
不是因为不知道这个类该叫什么
```

## 7. 方法与类大小

建议约束：

```text
Controller 方法只做参数接收、调用 UseCase、返回 Response
UseCase 方法表达一个业务动作
单个方法超过 40 行需要重新审视
单个类超过 250 行需要拆分职责
DTO 不包含业务行为
Entity 不直接处理外部 IO
```

这些不是机械硬规则，但偏离时必须有明确理由。

## 8. DTO 与领域对象

API DTO 不等于领域对象。

```text
CreateCodeSourceRequest
CodeSourceResponse
CreateProjectRequest
ProjectResponse
CreatePipelineRequest
PipelineResponse
RunResponse
BuildResultResponse
```

Mapper 放在 `api` 包：

```text
CodeSourceApiMapper
ProjectApiMapper
PipelineApiMapper
RunApiMapper
BuildResultApiMapper
```

规则：

- Request DTO 可以带 `secret`。
- Response DTO 不返回完整 `secret`。
- JPA Entity 不直接作为 Response。

## 9. 持久化结构

JPA Entity 放在 `infrastructure.persistence`。

示例：

```text
source/infrastructure/persistence/CodeSourceJpaEntity
source/infrastructure/persistence/CodeSourceJpaRepository
source/infrastructure/persistence/JpaCodeSourceRepository
```

命名说明：

- `CodeSourceJpaEntity` 是数据库映射。
- `CodeSourceRepository` 是 domain repository 接口。
- `JpaCodeSourceRepository` 是接口实现。

这样领域层不会直接依赖 Spring Data。

## 10. 外部适配器结构

外部系统适配器放在各模块 `infrastructure`：

```text
source/infrastructure/gitlab/GitLabGitProviderClient
source/infrastructure/github/GitHubGitProviderClient
execution/infrastructure/tekton/TektonExecutionEngine
registry/infrastructure/zot/ZotRegistryClient
deploy/infrastructure/argocd/ArgoCdDeploymentStatusReader
```

适配器职责：

```text
把外部 DTO 转成领域对象
隐藏认证细节
隐藏分页、重试、错误码映射
不把外部异常直接抛给 Controller
```

## 11. 测试目录结构

```text
backend/src/test/java/com/pipeline/platform/
├── source
│   ├── domain
│   ├── application
│   └── api
├── project
├── pipeline
├── execution
├── result
├── registry
└── support
```

测试命名：

```text
CodeSourceVerifierTest
CreateProjectUseCaseTest
PipelineCreatorTest
RunStarterTest
BuildResultNormalizerTest
CodeSourceControllerTest
```

测试分层：

```text
domain/application: 单元测试
api: WebMvcTest 或轻量集成测试
infrastructure: 适配器测试或 Testcontainers 后续补
end-to-end: 后续独立目录
```

第一版必须覆盖：

```text
密钥脱敏
CodeSource verified 约束
Pipeline config schema 校验
PluginContract 校验
Run 状态流转
BuildResult 归一化
```

## 12. 禁止事项

第一版禁止：

```text
把所有代码放进 service 包
把所有 DTO 放进全局 dto 包
把 Controller 写成业务编排中心
把 JPA Entity 当领域对象直接到处传
把 Tekton/Kubernetes 类型放进 domain
把 GitLab/GitHub 响应对象直接返回给前端
用 Map<String, Object> 代替明确 DTO，除 configJson/schemaJson 等有意保留的扩展字段
新增没有业务意义的 BaseService/BaseEntity
为了复用提前抽象插件市场
```

## 13. 第一版包落地顺序

按这个顺序创建代码，避免架构空转：

```text
shared
source
project
pipeline
execution
result
registry
```

暂时只建接口或占位：

```text
artifact
gitops
deploy
```

## 14. 验收标准

项目结构合格必须满足：

```text
包结构与本文一致
领域层无 Spring MVC/JPA/Tekton/GitLab 依赖
Controller 不直接访问数据库
密钥脱敏集中实现
StepType 和 PluginContract 有明确注册位置
Tekton 代码只存在 execution.infrastructure.tekton
Zot 代码只存在 registry.infrastructure.zot
至少有 source/project/pipeline/execution/result 的单元测试骨架
```
