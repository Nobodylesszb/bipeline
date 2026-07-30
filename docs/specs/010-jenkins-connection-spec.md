# Jenkins Connection 规格

- 状态：Draft v0.1
- 日期：2026-07-30
- 范围：Jenkins 执行引擎第一阶段，只做连接保存、查询和连通性验证。

## 1. 目标

在 Pipeline 配置层完成后，先补齐 Jenkins 执行引擎的连接管理能力。

第一版只做：

```text
保存 Jenkins 地址和 API Token
→ 查询连接
→ 测试 Jenkins Remote API 连通性
```

不做：

- 创建 Jenkins Job
- 触发 Build
- 查询 Build 日志
- PipelineRun
- 重试策略执行

这些放到后续 Execution / PipelineRun 阶段。

## 2. API

所有接口使用 `POST`。

### 2.1 创建 Jenkins 连接

```http
POST /api/v1/jenkins-connections/create
```

请求：

```json
{
  "name": "本地 Jenkins",
  "baseUrl": "http://localhost:8080",
  "username": "admin",
  "apiToken": "your-api-token"
}
```

规则：

- `name` 全局唯一。
- `baseUrl` 保存时去掉末尾 `/`。
- Token 第一版本地 MVP 允许明文保存。
- 响应不返回完整 token，只返回脱敏字段。
- 创建后状态为 `UNVERIFIED`。

### 2.2 查询 Jenkins 连接列表

```http
POST /api/v1/jenkins-connections/list
```

请求：

```json
{}
```

### 2.3 验证 Jenkins 连接

```http
POST /api/v1/jenkins-connections/verify
```

请求：

```json
{
  "connectionId": 1
}
```

验证方式：

```text
GET {baseUrl}/api/json
Basic Auth: username:apiToken
```

成功条件：

- HTTP 状态码是 2xx。
- 响应头包含 `X-Jenkins`，或响应体可正常返回 Jenkins 根 API JSON。

成功后：

```text
verification_status = VERIFIED
last_verified_at = now
last_verification_message = Jenkins connection is accessible
```

失败后：

```text
verification_status = FAILED
last_verified_at = now
last_verification_message = 失败原因
```

## 3. 分层

```text
jenkins.api
  Controller / Request / Response / Mapper

jenkins.application
  Create / List / Verify use cases
  JenkinsClient port

jenkins.domain
  JenkinsConnection
  JenkinsConnectionRepository

jenkins.infrastructure
  persistence
  remote
```

`jenkins.domain` 不依赖 HTTP、Spring MVC、JPA 或 Jenkins SDK。

## 4. 后续预留

后续 PipelineRun 层会通过统一执行引擎抽象调用 Jenkins：

```text
ExecutionEngineClient
└── JenkinsExecutionEngine
```

JenkinsConnection 是 JenkinsExecutionEngine 的输入之一。
