# Pipeline 控制面最小闭环规格

- 状态：Draft v0.1
- 日期：2026-07-29
- 范围：流水线配置保存、查询、启用、禁用；不执行 Jenkins。

## 1. 目标

在 `CodeSource → Project → Repository Binding` 已经打通之后，补齐创建流水线所需的控制面配置。

第一版就采用三层结构：

```text
Pipeline
└── Stage
    └── Step
```

但第一版只负责保存和查询配置，不执行构建。

## 2. 不做内容

- 不创建 Jenkins Job。
- 不触发 Jenkins Build。
- 不接 WebHook。
- 不保存构建日志。
- 不生成运行记录。

## 3. 表结构策略

早期 V1 已经存在实验表：

- `pipeline_configurations`
- `pipeline_steps`

它们还没有真正进入业务使用。为了避免新模型被旧命名和旧约束拖住，V4 迁移将旧表重命名为 legacy 表，并新建干净的三张表：

```text
pipelines
pipeline_stages
pipeline_steps
```

## 4. API

所有接口使用 `POST`。

### 4.1 创建流水线

```http
POST /api/v1/pipelines/create
```

请求：

```json
{
  "projectId": 1,
  "name": "main-ci",
  "description": "主分支 CI",
  "triggerType": "MANUAL",
  "branchName": "master",
  "stages": [
    {
      "name": "default",
      "displayName": "默认阶段",
      "steps": [
        {
          "type": "SHELL",
          "name": "test",
          "displayName": "运行测试",
          "config": {
            "command": "mvn test"
          }
        }
      ]
    }
  ]
}
```

规则：

- `projectId` 必须存在。
- Project 必须是 `ACTIVE`。
- Project 必须已经绑定仓库。
- 同一个 Project 下 Pipeline 名称唯一。
- 创建后状态为 `DRAFT`。
- `branchName` 为空时使用项目绑定仓库的 `defaultBranch`。
- 至少包含一个 stage。
- 每个 stage 至少包含一个 step。

### 4.2 查询流水线列表

```http
POST /api/v1/pipelines/list
```

请求：

```json
{
  "projectId": 1
}
```

### 4.3 查询流水线详情

```http
POST /api/v1/pipelines/detail
```

请求：

```json
{
  "pipelineId": 1
}
```

### 4.4 启用流水线

```http
POST /api/v1/pipelines/activate
```

请求：

```json
{
  "pipelineId": 1
}
```

规则：

- `DRAFT` 可以启用。
- `DISABLED` 可以重新启用。
- 其他状态不可启用。

### 4.5 禁用流水线

```http
POST /api/v1/pipelines/disable
```

请求：

```json
{
  "pipelineId": 1
}
```

规则：

- 只有 `ACTIVE` 可以禁用。

## 5. 第一版 Step 类型

先开放字符串枚举：

- `CHECKOUT`
- `SHELL`
- `TEST`
- `SONAR_SCAN`
- `BUILD_IMAGE`
- `PUSH_IMAGE`

第一版不校验每种 step 的 config schema，只保存 JSON。后续通过 PluginContract 做 schema 校验。

## 6. 验收

Swagger 或 curl 能完成：

```text
创建 main-ci
→ 查询列表
→ 查询详情看到 stages/steps
→ 启用
→ 禁用
```
