# Project 与 Repository 最小闭环规格

- 状态：Draft v0.1
- 日期：2026-07-29
- 所有者：bo
- 父规格：`005-ci-control-plane-api.md`
- 范围：第一版 Project 创建、项目列表、仓库验证、分支查询和仓库绑定。

## 1. 目标

在已经完成 `CodeSource` 保存和 Gitea/GitLab 连通性验证后，补齐创建流水线前必须具备的项目与仓库绑定能力。

第一版只支持：

```text
一个 Project
→ 一个已验证 CodeSource
→ 一个 Repository
→ 一个 defaultBranch
```

所有接口统一使用 `POST`，参数放在 JSON body，方便 Swagger 和前端表单测试。

## 2. 不做内容

- 不创建 Jenkins Job。
- 不创建 WebHook。
- 不管理 Deploy Key。
- 不支持一个项目多个仓库。
- 不支持多 CodeSource 绑定。
- 不做仓库目录扫描。

## 3. API

### 3.1 创建项目

```http
POST /api/v1/projects/create
```

请求：

```json
{
  "name": "hotel-link",
  "description": "酒店供应链项目",
  "codeSourceId": 1
}
```

规则：

- `name` 全局唯一。
- `codeSourceId` 必须存在。
- `CodeSource.verificationStatus` 必须是 `VERIFIED`。
- 新项目状态为 `ACTIVE`。

### 3.2 查询项目列表

```http
POST /api/v1/projects/list
```

请求：

```json
{}
```

返回包含项目基础信息和已绑定仓库摘要。

### 3.3 验证仓库

```http
POST /api/v1/code-sources/repository/verify
```

请求：

```json
{
  "codeSourceId": 1,
  "repositoryPath": "bobo_3776/hotel_link_supply"
}
```

规则：

- `codeSourceId` 必须存在并已验证。
- `repositoryPath` 第一版格式固定为 `owner/repo`。
- Gitea 使用 `RepositoryApi.repoGet(owner, repo)`。
- GitLab 第一版可先返回“不支持仓库验证”，后续实现。

返回：

```json
{
  "repositoryPath": "bobo_3776/hotel_link_supply",
  "name": "hotel_link_supply",
  "fullName": "bobo_3776/hotel_link_supply",
  "defaultBranch": "main",
  "cloneUrl": "http://192.168.0.245:3050/bobo_3776/hotel_link_supply.git",
  "sshUrl": "ssh://git@192.168.0.245:2222/bobo_3776/hotel_link_supply.git",
  "accessible": true
}
```

### 3.4 查询仓库分支

```http
POST /api/v1/code-sources/repository/branches
```

请求：

```json
{
  "codeSourceId": 1,
  "repositoryPath": "bobo_3776/hotel_link_supply"
}
```

返回：

```json
{
  "items": [
    {
      "name": "main",
      "commitId": "abc123"
    }
  ]
}
```

### 3.5 绑定项目仓库

```http
POST /api/v1/projects/repository/bind
```

请求：

```json
{
  "projectId": 1,
  "codeSourceId": 1,
  "repositoryPath": "bobo_3776/hotel_link_supply",
  "defaultBranch": "main",
  "contextDirectory": "."
}
```

规则：

- `projectId` 必须存在且状态为 `ACTIVE`。
- 请求中的 `codeSourceId` 必须等于项目创建时绑定的 `codeSourceId`。
- 仓库必须可访问。
- `defaultBranch` 必须存在于真实分支列表。
- `contextDirectory` 空值默认 `"."`。
- 第一版一个项目只能绑定一个仓库，重复绑定采用覆盖更新。

## 4. 模块边界

```text
project.api
  Controller、Request、Response、Mapper

project.application
  创建项目、查询项目、验证仓库、查询分支、绑定仓库

project.domain
  Project、Repository、ProjectRepository、RepositoryBindingRepository

project.infrastructure.persistence
  JPA 实体和 Spring Data

source.application.port
  GitProviderClient 扩展仓库查询能力

source.infrastructure.gitea
  Gitea SDK 适配
```

`project` 模块不直接依赖 Gitea SDK；它只依赖 `GitProviderClient` 抽象。

## 5. 验收

Swagger 中可以完成：

```text
创建项目
→ 验证 bobo_3776/hotel_link_supply
→ 查询分支
→ 绑定仓库
→ 查询项目列表看到仓库摘要
```
