# CI 控制面领域模型与数据库草案

- 状态：Draft v0.1
- 日期：2026-07-29
- 所有者：bo
- 父规格：`001-gitops-cicd-platform.md`、`002-ci-foundation.md`、`003-ci-cd-product-prototype.md`
- 关联决策：`../decisions/002-flowci-inspired-product-model.md`
- 范围：Java / Spring Boot 控制面第一版领域模型、状态模型和数据库草案。

## 1. 目标

本文把 FlowCI 风格的产品交互落到 Java 控制面的可开发模型中。第一版目标是支持：

```text
配置代码源
→ 测试连通性
→ 创建项目并选择代码源
→ 创建 Java Maven 流水线
→ 手动运行
→ 查询运行状态、日志入口和 BuildResult
```

第一版不实现自由拖拽流水线、不实现开放插件市场、不实现复杂权限、不实现一个项目多个仓库。

## 2. 模块边界

Java 后端采用模块化单体。领域层不得依赖 Tekton、Kubernetes、Argo CD、GitLab SDK 或某个 Registry SDK 的 DTO。

```text
source
  CodeSource、Repository 读取、分支/Tag/Revision 查询；适配 GitLab、GitHub、Gitea、Generic Git

project
  Project 生命周期与项目仓库设置

pipeline
  PipelineConfiguration、PipelineStep、BuildProfile、StepType、模板选择、插件契约

execution
  PipelineRun、StepRun、ExecutionEngine、TektonExecutionEngine、日志定位、取消、重跑

result
  BuildResult、Artifact、Report、ReleaseCandidate

registry
  RegistryConnection、OCI 镜像元数据查询、push 结果投影与 digest 校验

package
  HelmRepositoryConnection、Chart 包元数据与 Helm 推送结果投影

gitops
  GitOps 仓库更新

deploy
  Environment、Deployment、Argo CD 状态投影
```

第一版可以把 `package`、`gitops`、`deploy` 先做成接口和空实现，但领域对象要预留边界。`source`、`registry` 和 `pipeline` 的 StepType 契约必须先落地，因为它们直接影响首条 Java CI 纵向切片。

## 3. 聚合与关系

第一版聚合关系：

```text
CodeSource
└── Project[]
    ├── Repository
    ├── PipelineConfiguration[]
    │   └── PipelineStep[]
    ├── PipelineRun[]
    │   └── StepRun[]
    ├── BuildResult[]
    └── Environment[]
```

约束：

- 一个 `Project` 第一版只引用一个 `CodeSource`。
- 一个 `Project` 第一版只配置一个 `Repository`。
- 一个 `Project` 可以创建多条 `PipelineConfiguration`。
- 一条 `PipelineConfiguration` 默认检出 `Project.Repository`。
- 一次 `PipelineRun` 必须保存当次运行使用的配置快照，避免后续修改流水线影响历史运行解释。
- `BuildResult` 描述 CI 输出，不等于可部署版本。
- `ReleaseCandidate` 由策略层从 `BuildResult` 生成，第一版可以只定义模型，不实现完整 CD。

## 4. 核心实体

### 4.1 CodeSource

代码源是可复用的外部 Git 平台连接配置。

字段：

```text
id
name
provider
baseUrl
authType
username
secretPlain
verificationStatus
lastVerifiedAt
lastVerificationMessage
createdAt
updatedAt
```

枚举：

```text
provider: GITLAB, GITHUB, GITEA, GENERIC_GIT
authType: NONE, USERNAME_PASSWORD, ACCESS_TOKEN, DEPLOY_TOKEN, SSH_KEY
verificationStatus: UNVERIFIED, VERIFIED, FAILED
```

第一版允许 `secretPlain` 明文保存到数据库，仅用于本地 MVP。接口返回时必须脱敏，不返回完整密钥。

### 4.2 Project

项目是平台的业务工作区，第一版绑定一个代码源和一个仓库。

字段：

```text
id
name
description
codeSourceId
status
createdAt
updatedAt
```

枚举：

```text
status: ACTIVE, ARCHIVED
```

### 4.3 Repository

仓库保存项目级源码位置。

字段：

```text
id
projectId
remotePath
remoteUrl
defaultBranch
contextDirectory
lastResolvedRevision
lastFetchedAt
createdAt
updatedAt
```

说明：

- `remotePath` 是用户输入的仓库路径，例如 `group/order-service.git`。
- `remoteUrl` 是由 `CodeSource.baseUrl + remotePath` 归一化得到的完整 Git URL。
- `lastResolvedRevision` 是缓存字段，不是 Git 的事实源。

### 4.4 BuildProfile

构建档案描述平台内置模板能力。

字段：

```text
id
name
language
templateKey
templateVersion
description
schemaJson
defaultConfigJson
enabled
createdAt
updatedAt
```

第一版内置：

```text
java-maven-image:v1
java-maven-scan-image:v1
java-gradle-image:v1
node-image:v1
python-image:v1
blank:v1
```

`BuildProfile` 可以先由代码注册，后续再迁移到模板仓库或数据库种子数据。

### 4.5 PipelineConfiguration

流水线配置是用户创建的可运行配置。

字段：

```text
id
projectId
name
description
defaultBranch
contextDirectory
buildProfileId
buildProfileVersion
configJson
triggerMode
status
version
createdAt
updatedAt
```

枚举：

```text
triggerMode: MANUAL, WEBHOOK, SCHEDULE
status: DRAFT, ACTIVE, DISABLED, ARCHIVED
```

第一版只实现 `MANUAL`。

`configJson` 保存用户参数，例如：

```json
{
  "jdkVersion": "25",
  "testCommand": "./mvnw test",
  "packageCommand": "./mvnw package -DskipTests",
  "dockerfile": "Dockerfile",
  "imageName": "ecommerce/order-service",
  "quality": {
    "sonar": false,
    "trivy": false
  }
}
```

### 4.6 PipelineStep

流水线步骤来自模板展开，用于画布预览和运行详情展示。

字段：

```text
id
pipelineId
stepKey
name
stageName
stepType
pluginVersion
dependsOnJson
configJson
displayOrder
enabled
createdAt
updatedAt
```

第一版内置 `stepType`：

```text
git-checkout
maven-test
maven-package
sonar-scan
trivy-scan
image-build
image-push
build-result
helm-package
helm-push
gitops-update
```

第一版 `sonar-scan`、`trivy-scan`、`helm-package`、`helm-push`、`gitops-update` 可以处于禁用或预留状态。

### 4.7 PipelineRun

流水线运行记录是平台对一次执行的投影。

字段：

```text
id
pipelineId
projectId
runNumber
triggerType
triggeredBy
requestedBranch
requestedRevision
resolvedRevision
status
externalRunId
executionEngine
pipelineSnapshotJson
startedAt
finishedAt
createdAt
updatedAt
```

枚举：

```text
triggerType: MANUAL, WEBHOOK, SCHEDULE, RETRY
status: QUEUED, RUNNING, SUCCEEDED, FAILED, CANCELLED, TIMED_OUT
executionEngine: TEKTON
```

约束：

- `externalRunId` 第一版保存 Tekton `PipelineRun` 名称。
- `pipelineSnapshotJson` 保存当次运行的流水线配置和步骤快照。
- 数据库不是运行状态事实源；状态字段是查询投影，必须能从执行引擎重新同步。
- `AGENT` 是未来候选执行模式，第一版不进入枚举和实现计划。

### 4.8 StepRun

步骤运行记录用于运行详情和日志定位。

字段：

```text
id
pipelineRunId
stepKey
name
stageName
stepType
status
externalTaskId
logRef
startedAt
finishedAt
createdAt
updatedAt
```

枚举：

```text
status: PENDING, RUNNING, SUCCEEDED, FAILED, SKIPPED, CANCELLED, TIMED_OUT
```

### 4.9 BuildResult

构建结果是 CI 的结构化输出。

字段：

```text
id
pipelineRunId
projectId
sourceJson
artifactsJson
reportsJson
provenanceJson
status
createdAt
updatedAt
```

枚举：

```text
status: PARTIAL, SUCCEEDED, FAILED
```

最小 JSON 契约：

```json
{
  "source": {
    "repository": "https://gitlab.example.com/group/order-service.git",
    "revision": "abc123"
  },
  "artifacts": [
    {
      "name": "application-image",
      "kind": "oci-image",
      "role": "runtime",
      "uri": "10.211.55.4:30443/ecommerce/order-service",
      "digest": "sha256:...",
      "platforms": ["linux/arm64"]
    }
  ],
  "reports": [
    {
      "kind": "unit-test",
      "status": "passed"
    }
  ],
  "provenance": {
    "templateRef": "java-maven-image",
    "templateVersion": "v1"
  }
}
```

### 4.10 ReleaseCandidate

发布候选是 CD 能消费的版本。

字段：

```text
id
buildResultId
projectId
sourceRevision
componentsJson
policyStatus
createdAt
updatedAt
```

枚举：

```text
policyStatus: PENDING, PASSED, FAILED
```

第一版可以在 CI 成功后只展示可生成候选，不必立即实现 GitOps 更新。

### 4.11 Environment

环境用于后续 CD 状态展示。

字段：

```text
id
projectId
name
type
status
gitopsPath
argoApplicationName
createdAt
updatedAt
```

枚举：

```text
type: DEV, TEST, STAGING, PROD
status: ACTIVE, DISABLED
```

第一版只预留 `DEV`。

### 4.12 IntegrationConnection

集成连接抽象外部服务配置，避免每类外部系统都直接散落在业务表中。

第一版先实现 `CodeSource`，但模型上预留以下连接类型：

```text
CODE_SOURCE
OCI_REGISTRY
HELM_REPOSITORY
SONARQUBE
KUBERNETES_CLUSTER
ARGO_CD
GITOPS_REPOSITORY
```

连接能力边界：

```text
CodeSource          读取仓库、分支、Tag、Revision
OCI Registry        推送/查询镜像、校验 digest
Helm Repository     推送/查询 Chart 包
SonarQube           提交扫描、读取 Quality Gate
Kubernetes Cluster  查询 Tekton/K8s 运行状态
Argo CD             查询 Application sync/health
GitOps Repository   提交期望状态变更
```

为了第一版简单，`CodeSource` 独立成表；其他连接可以在后续进入实现时再各自建表或统一到 `integration_connections`。

### 4.13 PluginContract

`PipelineStep.stepType` 是平台内置插件类型。每个插件必须有稳定契约，避免后续增加 Sonar、Trivy、Helm、镜像推送时改动核心执行模型。

插件契约字段：

```text
type
version
displayName
category
inputSchemaJson
outputSchemaJson
secretKeysJson
requiredConnectionsJson
timeoutSeconds
retryable
failurePolicy
enabled
```

枚举：

```text
category: SOURCE, BUILD, TEST, QUALITY, SECURITY, PACKAGE, IMAGE, RESULT, GITOPS, DEPLOY
failurePolicy: FAIL_FAST, CONTINUE, MARK_UNSTABLE
```

第一版必须为这些 StepType 建立契约：

```text
git-checkout
maven-test
maven-package
image-build
image-push
build-result
```

第一版预留但可不启用：

```text
sonar-scan
trivy-scan
helm-package
helm-push
gitops-update
```

插件测试要求：

```text
输入 Schema 校验
输出 Schema 校验
缺少连接或密钥时失败原因清晰
失败语义符合 failurePolicy
日志不得泄露 Token、密码、SSH Key
Tekton 模板映射必须包含必需参数和结果
插件输出能被 BuildResult 正确归一化
```

## 5. 数据库表草案

第一版建议表：

```text
code_sources
projects
repositories
build_profiles
plugin_contracts
pipeline_configurations
pipeline_steps
pipeline_runs
step_runs
build_results
release_candidates
environments
deployments
```

### 5.1 code_sources

```text
id uuid pk
name varchar(128) not null
provider varchar(32) not null
base_url varchar(512) not null
auth_type varchar(32) not null
username varchar(256)
secret_plain text
verification_status varchar(32) not null
last_verified_at timestamp
last_verification_message text
created_at timestamp not null
updated_at timestamp not null
```

索引：

```text
unique(name)
index(provider)
index(verification_status)
```

### 5.2 projects

```text
id uuid pk
name varchar(128) not null
description text
code_source_id uuid not null fk -> code_sources.id
status varchar(32) not null
created_at timestamp not null
updated_at timestamp not null
```

索引：

```text
unique(name)
index(code_source_id)
index(status)
```

### 5.3 repositories

```text
id uuid pk
project_id uuid not null fk -> projects.id
remote_path varchar(512) not null
remote_url varchar(1024) not null
default_branch varchar(256) not null
context_directory varchar(512) not null default '.'
last_resolved_revision varchar(128)
last_fetched_at timestamp
created_at timestamp not null
updated_at timestamp not null
```

索引：

```text
unique(project_id)
index(remote_url)
```

`unique(project_id)` 明确第一版一个项目只有一个仓库。

### 5.4 build_profiles

```text
id uuid pk
name varchar(128) not null
language varchar(64) not null
template_key varchar(128) not null
template_version varchar(64) not null
description text
schema_json jsonb not null
default_config_json jsonb not null
enabled boolean not null
created_at timestamp not null
updated_at timestamp not null
```

索引：

```text
unique(template_key, template_version)
index(language)
index(enabled)
```

### 5.5 plugin_contracts

```text
id uuid pk
type varchar(128) not null
version varchar(64) not null
display_name varchar(128) not null
category varchar(64) not null
input_schema_json jsonb not null
output_schema_json jsonb not null
secret_keys_json jsonb not null
required_connections_json jsonb not null
timeout_seconds integer not null
retryable boolean not null
failure_policy varchar(32) not null
enabled boolean not null
created_at timestamp not null
updated_at timestamp not null
```

索引：

```text
unique(type, version)
index(category)
index(enabled)
```

### 5.6 pipeline_configurations

```text
id uuid pk
project_id uuid not null fk -> projects.id
name varchar(128) not null
description text
default_branch varchar(256) not null
context_directory varchar(512) not null
build_profile_id uuid not null fk -> build_profiles.id
build_profile_version varchar(64) not null
config_json jsonb not null
trigger_mode varchar(32) not null
status varchar(32) not null
version integer not null
created_at timestamp not null
updated_at timestamp not null
```

索引：

```text
unique(project_id, name)
index(project_id, status)
index(build_profile_id)
```

### 5.7 pipeline_steps

```text
id uuid pk
pipeline_id uuid not null fk -> pipeline_configurations.id
step_key varchar(128) not null
name varchar(128) not null
stage_name varchar(128) not null
step_type varchar(128) not null
plugin_version varchar(64) not null
depends_on_json jsonb not null
config_json jsonb not null
display_order integer not null
enabled boolean not null
created_at timestamp not null
updated_at timestamp not null
```

索引：

```text
unique(pipeline_id, step_key)
index(pipeline_id, display_order)
index(step_type)
```

### 5.8 pipeline_runs

```text
id uuid pk
pipeline_id uuid not null fk -> pipeline_configurations.id
project_id uuid not null fk -> projects.id
run_number integer not null
trigger_type varchar(32) not null
triggered_by varchar(128)
requested_branch varchar(256)
requested_revision varchar(256)
resolved_revision varchar(128)
status varchar(32) not null
external_run_id varchar(256)
execution_engine varchar(32) not null
pipeline_snapshot_json jsonb not null
started_at timestamp
finished_at timestamp
created_at timestamp not null
updated_at timestamp not null
```

索引：

```text
unique(pipeline_id, run_number)
index(project_id, created_at)
index(pipeline_id, status)
index(external_run_id)
```

### 5.9 step_runs

```text
id uuid pk
pipeline_run_id uuid not null fk -> pipeline_runs.id
step_key varchar(128) not null
name varchar(128) not null
stage_name varchar(128) not null
step_type varchar(128) not null
status varchar(32) not null
external_task_id varchar(256)
log_ref text
started_at timestamp
finished_at timestamp
created_at timestamp not null
updated_at timestamp not null
```

索引：

```text
unique(pipeline_run_id, step_key)
index(pipeline_run_id, status)
```

### 5.10 build_results

```text
id uuid pk
pipeline_run_id uuid not null fk -> pipeline_runs.id
project_id uuid not null fk -> projects.id
source_json jsonb not null
artifacts_json jsonb not null
reports_json jsonb not null
provenance_json jsonb not null
status varchar(32) not null
created_at timestamp not null
updated_at timestamp not null
```

索引：

```text
unique(pipeline_run_id)
index(project_id, created_at)
index(status)
```

### 5.11 release_candidates

```text
id uuid pk
build_result_id uuid not null fk -> build_results.id
project_id uuid not null fk -> projects.id
source_revision varchar(128) not null
components_json jsonb not null
policy_status varchar(32) not null
created_at timestamp not null
updated_at timestamp not null
```

索引：

```text
unique(build_result_id)
index(project_id, created_at)
index(policy_status)
```

### 5.12 environments

```text
id uuid pk
project_id uuid not null fk -> projects.id
name varchar(128) not null
type varchar(32) not null
status varchar(32) not null
gitops_path varchar(512)
argo_application_name varchar(256)
created_at timestamp not null
updated_at timestamp not null
```

索引：

```text
unique(project_id, name)
index(project_id, type)
```

### 5.13 deployments

```text
id uuid pk
environment_id uuid not null fk -> environments.id
release_candidate_id uuid not null fk -> release_candidates.id
status varchar(32) not null
gitops_commit varchar(128)
argo_sync_status varchar(64)
argo_health_status varchar(64)
started_at timestamp
finished_at timestamp
created_at timestamp not null
updated_at timestamp not null
```

索引：

```text
index(environment_id, created_at)
index(release_candidate_id)
index(status)
```

## 6. 状态流转

### 6.1 PipelineConfiguration

```text
DRAFT → ACTIVE → DISABLED → ARCHIVED
ACTIVE → ARCHIVED
DISABLED → ACTIVE
```

只有 `ACTIVE` 的流水线可以运行。

### 6.2 PipelineRun

```text
QUEUED → RUNNING → SUCCEEDED
QUEUED → RUNNING → FAILED
QUEUED → RUNNING → CANCELLED
QUEUED → RUNNING → TIMED_OUT
QUEUED → CANCELLED
```

终态：

```text
SUCCEEDED
FAILED
CANCELLED
TIMED_OUT
```

终态运行不可再次变更状态；重跑必须创建新的 `PipelineRun`。

### 6.3 BuildResult

```text
PARTIAL → SUCCEEDED
PARTIAL → FAILED
```

失败运行也可以产生 `PARTIAL` 或 `FAILED` 的 `BuildResult`，用于保存测试报告和失败原因。

## 7. 第一版创建流程

### 7.1 添加代码源

```text
POST /code-sources
→ 保存 CodeSource
→ POST /code-sources/{id}/verification
→ 更新 verificationStatus
```

### 7.2 创建项目

```text
选择已验证 CodeSource
→ 输入 remotePath、defaultBranch
→ 平台读取分支
→ 创建 Project
→ 创建 Repository
```

### 7.3 创建流水线

```text
选择 BuildProfile
→ 填写 configJson
→ 创建 PipelineConfiguration
→ 根据模板展开 PipelineStep
→ 展示只读流程画布
```

### 7.4 手动运行

```text
创建 PipelineRun: QUEUED
→ 保存 pipelineSnapshotJson
→ ExecutionEngine.start()
→ 写入 externalRunId
→ 同步 StepRun
→ 采集 BuildResult
```

## 8. 防大改规则

- 领域对象不持有 Tekton CRD 类型。
- API DTO 不直接暴露数据库实体。
- `PipelineRun.status` 是投影，不是执行状态事实源。
- `PipelineConfiguration.configJson` 必须受 `BuildProfile.schemaJson` 约束。
- `PipelineStep.stepType` 必须来自内置 StepType 注册表。
- 新增 StepType 或外部连接前，必须先补 `PluginContract` 和插件契约测试。
- `BuildResult.artifactsJson` 必须允许多制品、多平台、SBOM 和签名。
- 删除 Project 第一版使用软删除或归档，不级联删除外部运行事实。
- 第一版只实现 `TektonExecutionEngine`；领域层不依赖 Tekton CRD，但运行适配层明确以 Tekton 为默认执行底座。

## 9. 待确认

- 第一版数据库使用 PostgreSQL 还是 H2/PostgreSQL 双模式。
- `secretPlain` 是否只在本地 profile 启用，生产 profile 是否启动时拒绝明文密钥。
- `BuildProfile` 是代码内置注册，还是启动时从 YAML 种子文件导入。
