# CI 控制面 API 规格

- 状态：Draft v0.1
- 日期：2026-07-29
- 所有者：bo
- 父规格：`003-ci-cd-product-prototype.md`、`004-ci-control-plane-domain-model.md`
- 关联决策：`../decisions/002-flowci-inspired-product-model.md`
- 范围：Java / Spring Boot 控制面第一版 REST API 与 DTO。

## 1. API 目标

第一版 API 支持 FlowCI 风格的最小 CI 闭环：

```text
配置代码源
→ 测试连通性
→ 创建项目
→ 查询仓库分支
→ 创建流水线
→ 选择模板
→ 查看流程预览
→ 手动运行
→ 查询运行详情、日志入口和 BuildResult
```

第一版只实现 Jenkins 执行路线。API 不暴露 Jenkins DTO、Jenkinsfile 脚本、GitLab SDK DTO 或 Registry SDK DTO。

## 2. 通用约定

### 2.1 路径与版本

```text
/api/v1
```

### 2.2 ID

所有资源 ID 使用数据库自增数字 ID。

```json
{
  "id": 1
}
```

### 2.3 时间

时间使用 ISO-8601。

```json
{
  "createdAt": "2026-07-29T14:20:00+08:00"
}
```

### 2.4 错误响应

```json
{
  "code": "CODE_SOURCE_VERIFICATION_FAILED",
  "message": "代码源验证失败",
  "details": {
    "reason": "凭据无效或没有读取仓库权限"
  },
  "traceId": "req-abc123"
}
```

常用错误码：

```text
VALIDATION_FAILED
RESOURCE_NOT_FOUND
RESOURCE_CONFLICT
CODE_SOURCE_VERIFICATION_FAILED
REPOSITORY_NOT_ACCESSIBLE
PIPELINE_NOT_RUNNABLE
RUN_ALREADY_TERMINAL
EXECUTION_ENGINE_UNAVAILABLE
PLUGIN_CONTRACT_INVALID
```

### 2.5 脱敏规则

任何 API 响应都不得返回完整密钥。第一版虽然允许本地 MVP 明文入库，但响应只能返回：

```json
{
  "secretMasked": "********",
  "secretLastFour": "a1b2"
}
```

### 2.6 幂等规则

写接口第一版先不强制实现全局幂等键，但以下操作必须避免明显重复：

```text
CodeSource name 唯一
Project name 唯一
Pipeline name 在 Project 内唯一
Run retry 必须创建新 runNumber
```

后续可增加：

```text
Idempotency-Key
```

## 3. CodeSource API

### 3.1 创建代码源

```http
POST /api/v1/code-sources
```

请求：

```json
{
  "name": "公司 GitLab",
  "provider": "GITLAB",
  "baseUrl": "https://gitlab.example.com",
  "authType": "DEPLOY_TOKEN",
  "username": "ci-reader",
  "secret": "plain-token-for-local-mvp"
}
```

响应：

```json
{
  "id": "cs-uuid",
  "name": "公司 GitLab",
  "provider": "GITLAB",
  "baseUrl": "https://gitlab.example.com",
  "authType": "DEPLOY_TOKEN",
  "username": "ci-reader",
  "secretMasked": "********",
  "verificationStatus": "UNVERIFIED",
  "createdAt": "2026-07-29T14:20:00+08:00",
  "updatedAt": "2026-07-29T14:20:00+08:00"
}
```

### 3.2 查询代码源列表

```http
GET /api/v1/code-sources?provider=GITLAB&status=VERIFIED
```

响应：

```json
{
  "items": [
    {
      "id": "cs-uuid",
      "name": "公司 GitLab",
      "provider": "GITLAB",
      "baseUrl": "https://gitlab.example.com",
      "authType": "DEPLOY_TOKEN",
      "verificationStatus": "VERIFIED",
      "lastVerifiedAt": "2026-07-29T14:21:00+08:00"
    }
  ]
}
```

### 3.3 测试代码源连通性

```http
POST /api/v1/code-sources/{codeSourceId}/verification
```

可选请求：

```json
{
  "repositoryPath": "group/order-service.git"
}
```

响应：

```json
{
  "status": "VERIFIED",
  "message": "代码源可访问",
  "capabilities": {
    "listRepositories": true,
    "readBranches": true,
    "readTags": true,
    "readRevision": true
  },
  "verifiedAt": "2026-07-29T14:21:00+08:00"
}
```

### 3.4 查询代码源仓库

```http
GET /api/v1/code-sources/{codeSourceId}/repositories?keyword=order
```

响应：

```json
{
  "items": [
    {
      "path": "group/order-service.git",
      "name": "order-service",
      "defaultBranch": "main",
      "webUrl": "https://gitlab.example.com/group/order-service"
    }
  ]
}
```

## 4. Project API

### 4.1 创建项目

```http
POST /api/v1/projects
```

请求：

```json
{
  "name": "order-service",
  "description": "订单服务",
  "codeSourceId": "cs-uuid",
  "repository": {
    "remotePath": "group/order-service.git",
    "defaultBranch": "main",
    "contextDirectory": "."
  }
}
```

响应：

```json
{
  "id": "project-uuid",
  "name": "order-service",
  "description": "订单服务",
  "status": "ACTIVE",
  "codeSource": {
    "id": "cs-uuid",
    "name": "公司 GitLab",
    "provider": "GITLAB"
  },
  "repository": {
    "id": "repo-uuid",
    "remotePath": "group/order-service.git",
    "remoteUrl": "https://gitlab.example.com/group/order-service.git",
    "defaultBranch": "main",
    "contextDirectory": "."
  },
  "createdAt": "2026-07-29T14:25:00+08:00"
}
```

约束：

```text
codeSourceId 必须存在。
只允许已 VERIFIED 的 CodeSource 创建项目。
remotePath 必须能被 CodeSource 访问。
```

### 4.2 查询项目详情

```http
GET /api/v1/projects/{projectId}
```

响应包含项目、代码源摘要、仓库摘要和最近运行摘要。

### 4.3 查询项目仓库

```http
GET /api/v1/projects/{projectId}/repository
```

响应：

```json
{
  "id": "repo-uuid",
  "remotePath": "group/order-service.git",
  "remoteUrl": "https://gitlab.example.com/group/order-service.git",
  "defaultBranch": "main",
  "contextDirectory": ".",
  "lastResolvedRevision": "abc123",
  "lastFetchedAt": "2026-07-29T14:30:00+08:00"
}
```

### 4.4 查询分支、Tag、Revision

```http
GET /api/v1/projects/{projectId}/repository/branches
GET /api/v1/projects/{projectId}/repository/tags
GET /api/v1/projects/{projectId}/repository/revisions/{revision}
```

Revision 响应：

```json
{
  "revision": "main",
  "resolvedRevision": "abc123",
  "message": "Add checkout adapter",
  "author": "bo",
  "committedAt": "2026-07-29T13:50:00+08:00"
}
```

## 5. BuildProfile 与 PluginContract API

### 5.1 查询构建档案

```http
GET /api/v1/build-profiles?language=JAVA&enabled=true
```

响应：

```json
{
  "items": [
    {
      "id": "profile-uuid",
      "name": "Java Maven · 测试、构建、镜像推送",
      "language": "JAVA",
      "templateKey": "java-maven-image",
      "templateVersion": "v1",
      "description": "检出源码、执行 Maven 测试、打包、构建镜像并推送 Registry",
      "enabled": true
    }
  ]
}
```

### 5.2 查询构建档案详情

```http
GET /api/v1/build-profiles/{profileId}
```

响应包含 `schemaJson`、`defaultConfigJson` 和模板步骤预览。

### 5.3 查询插件契约

```http
GET /api/v1/plugin-contracts?category=IMAGE&enabled=true
```

响应：

```json
{
  "items": [
    {
      "type": "image-push",
      "version": "v1",
      "displayName": "镜像推送",
      "category": "IMAGE",
      "failurePolicy": "FAIL_FAST",
      "requiredConnections": ["OCI_REGISTRY"],
      "enabled": true
    }
  ]
}
```

### 5.4 测试插件契约

```http
POST /api/v1/plugin-contracts/{type}/versions/{version}/validation
```

请求：

```json
{
  "input": {
    "imageName": "ecommerce/order-service",
    "tag": "abc123"
  }
}
```

响应：

```json
{
  "valid": true,
  "message": "插件输入符合契约"
}
```

第一版该接口可以只做 Schema 校验，不真实执行插件。

## 6. Pipeline API

### 6.1 创建流水线

```http
POST /api/v1/projects/{projectId}/pipelines
```

请求：

```json
{
  "name": "main-ci",
  "description": "主干 CI",
  "defaultBranch": "main",
  "contextDirectory": ".",
  "buildProfileId": "profile-uuid",
  "config": {
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
}
```

响应：

```json
{
  "id": "pipeline-uuid",
  "projectId": "project-uuid",
  "name": "main-ci",
  "status": "ACTIVE",
  "triggerMode": "MANUAL",
  "buildProfile": {
    "id": "profile-uuid",
    "templateKey": "java-maven-image",
    "templateVersion": "v1"
  },
  "steps": [
    {
      "stepKey": "checkout",
      "name": "源码检出",
      "stageName": "source",
      "stepType": "git-checkout",
      "enabled": true
    },
    {
      "stepKey": "image-push",
      "name": "镜像推送",
      "stageName": "image",
      "stepType": "image-push",
      "enabled": true
    }
  ],
  "createdAt": "2026-07-29T14:35:00+08:00"
}
```

约束：

```text
config 必须符合 BuildProfile.schemaJson。
展开的 stepType 必须存在启用的 PluginContract。
Pipeline 创建成功后默认 ACTIVE。
```

### 6.2 查询流水线列表

```http
GET /api/v1/projects/{projectId}/pipelines
```

### 6.3 查询流水线详情

```http
GET /api/v1/pipelines/{pipelineId}
```

响应包含 `config`、`steps` 和最近一次运行摘要。

### 6.4 查询流程预览

```http
GET /api/v1/pipelines/{pipelineId}/diagram
```

响应：

```json
{
  "nodes": [
    {
      "id": "checkout",
      "label": "源码检出",
      "stepType": "git-checkout",
      "stageName": "source",
      "status": "CONFIGURED"
    },
    {
      "id": "maven-test",
      "label": "Maven 测试",
      "stepType": "maven-test",
      "stageName": "test",
      "status": "CONFIGURED"
    }
  ],
  "edges": [
    {
      "from": "checkout",
      "to": "maven-test"
    }
  ]
}
```

## 7. Run API

### 7.1 手动运行流水线

```http
POST /api/v1/pipelines/{pipelineId}/runs
```

请求：

```json
{
  "branch": "main",
  "revision": "main",
  "variables": {
    "skipTests": false
  },
  "triggeredBy": "bo"
}
```

响应：

```json
{
  "id": 1,
  "pipelineId": 1,
  "projectId": 1,
  "runNumber": 1,
  "status": "QUEUED",
  "executionEngine": "JENKINS",
  "externalRunId": null,
  "createdAt": "2026-07-29T14:40:00+08:00"
}
```

约束：

```text
Pipeline 必须是 ACTIVE。
revision 必须能解析成完整 commit SHA。
创建运行时必须保存 pipelineSnapshotJson。
调用 JenkinsExecutionEngine 后回填 externalRunId。
```

### 7.2 查询运行列表

```http
GET /api/v1/pipelines/{pipelineId}/runs?status=FAILED
```

### 7.3 查询运行详情

```http
GET /api/v1/runs/{runId}
```

响应：

```json
{
  "id": "run-uuid",
  "runNumber": 1,
  "status": "RUNNING",
  "triggerType": "MANUAL",
  "triggeredBy": "bo",
  "requestedRevision": "main",
  "resolvedRevision": "abc123",
  "executionEngine": "JENKINS",
  "externalRunId": "main-ci-run-abc123",
  "steps": [
    {
      "stepKey": "checkout",
      "name": "源码检出",
      "status": "SUCCEEDED",
      "logRef": "tekton://main-ci-run-abc123/checkout"
    },
    {
      "stepKey": "maven-test",
      "name": "Maven 测试",
      "status": "RUNNING",
      "logRef": "tekton://main-ci-run-abc123/maven-test"
    }
  ]
}
```

### 7.4 查询日志

```http
GET /api/v1/runs/{runId}/logs?stepKey=maven-test&follow=false
```

响应：

```json
{
  "runId": "run-uuid",
  "stepKey": "maven-test",
  "content": "sanitized log content",
  "nextCursor": null
}
```

日志响应必须脱敏。

### 7.5 取消运行

```http
POST /api/v1/runs/{runId}/cancellation
```

响应：

```json
{
  "id": "run-uuid",
  "status": "CANCELLED"
}
```

终态运行不能取消。

### 7.6 重跑

```http
POST /api/v1/runs/{runId}/retry
```

响应为新的 `PipelineRun`。重跑不得修改原运行。

## 8. BuildResult API

### 8.1 查询构建结果

```http
GET /api/v1/runs/{runId}/build-result
```

响应：

```json
{
  "id": "build-result-uuid",
  "runId": "run-uuid",
  "status": "SUCCEEDED",
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
      "tag": "abc123",
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

### 8.2 查询项目制品

```http
GET /api/v1/projects/{projectId}/artifacts?kind=oci-image
```

响应从 `BuildResult.artifacts[]` 投影，不直接扫描 Registry。

## 9. Registry 与 Helm 连接 API

第一版可以只做 Zot 默认配置，不开放完整页面。但 API 边界预留如下：

```http
POST /api/v1/registry-connections
GET  /api/v1/registry-connections
POST /api/v1/registry-connections/{connectionId}/verification

POST /api/v1/helm-repositories
GET  /api/v1/helm-repositories
POST /api/v1/helm-repositories/{repositoryId}/verification
```

第一版 `image-push` 插件必须能引用默认 Registry 连接，并把镜像 URI、tag、digest 写入 `BuildResult.artifacts[]`。

## 10. 第一版 API 验收

- 可以创建代码源并测试连通性。
- 可以创建项目并读取仓库分支。
- 可以查询构建档案和插件契约。
- 可以创建 Java Maven 流水线并得到流程预览。
- 可以手动运行流水线并得到 `PipelineRun`。
- 可以查询运行详情和 Step 状态。
- 可以查询日志，且日志不泄露密钥。
- 可以查询 `BuildResult`，其中包含源码 revision、镜像 URI、tag、digest 和测试报告。
