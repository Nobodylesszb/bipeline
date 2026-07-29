# CI/CD 平台产品原型

- 状态：Draft v0.1
- 日期：2026-07-29
- 所有者：bo
- 父规格：`001-gitops-cicd-platform.md`、`002-ci-foundation.md`
- 范围：低保真产品原型；用于指导 Java 后端接口、前端页面和数据库模型。

## 1. 原型目标

本原型描述第一版自研 CI/CD 平台的用户界面和主流程。平台采用：

```text
Java 控制面
→ Jenkins 执行 CI
→ Zot 保存 OCI 镜像
→ GitOps 仓库保存部署期望状态
→ Argo CD 执行 CD
```

第一版不要求业务仓库内存在 CI 配置文件。业务仓库只保存源码，流水线配置由平台数据库保存，运行时由 Java 后端生成 Jenkins Pipeline Script 并触发 Jenkins Build。

## 2. 信息架构

```text
平台首页
├── 项目
│   ├── 项目概览
│   ├── 代码仓库设置
│   ├── 流水线
│   ├── 运行记录
│   ├── 制品
│   └── 环境
├── 集成
│   ├── 代码源
│   ├── 镜像仓库
│   ├── Kubernetes
│   ├── Argo CD
│   └── SonarQube
├── 构建档案
│   ├── Java Maven
│   ├── Java Gradle
│   ├── Python
│   └── Node.js
└── 系统
    ├── 审计
    └── 健康检查
```

第一版前端只实现：

```text
项目列表
项目详情
代码源配置
项目仓库设置
流水线创建
流水线运行
运行详情
制品结果
开发环境部署状态
```

## 3. 核心对象关系

第一版采用单仓库项目模型，先不考虑一个项目绑定多个代码源或多个代码仓库。

```text
Project
├── CodeSourceRef
├── Repository
├── PipelineConfiguration[]
├── PipelineRun[]
├── BuildResult[]
└── Environment[]
```

示例：

```text
电商项目
├── Repository: order-service.git
├── Pipeline: main-ci
├── Pipeline: nightly-scan
└── Pipeline: release-ci
```

第一版先在平台集成入口维护可复用的 `CodeSource`，创建项目时选择一个已验证的代码源，再填写仓库地址、默认分支和项目级参数。后续扩展到多仓库项目时，再把 `CodeSourceRef` 和 `Repository` 拆成 `CodeSource[]` 与 `RepositoryBinding[]`，但第一版页面和接口不暴露这个复杂度。

## 4. FlowCI 参考边界

本平台参考 FlowCI 的产品设计和流水线概念，但不直接照搬其完整实现。

采用的设计：

```text
模板创建流水线
语言分类模板
Job / Step / Plugin 概念
Step 可并行
Step 可选择运行环境
缓存作为加速能力
运行日志与在线调试体验
```

暂不采用的设计：

```text
不要求业务仓库提交 flow.ci YAML
不直接 fork FlowCI 作为核心代码底座
不在第一版实现完整动态 Agent 自动伸缩
不在第一版开放第三方插件市场
不让 CI Step 直接绕过 GitOps/Argo CD 部署应用
```

第一版内部命名可以参考 FlowCI：

```text
FlowCI Job     → PipelineConfiguration
FlowCI Step    → PipelineStep
FlowCI Plugin  → StepType / BuiltInPlugin
FlowCI Agent   → ExecutionWorker / ExecutionEngine
FlowCI Run     → PipelineRun
```

## 5. 主流程

```mermaid
flowchart LR
  A["配置代码源"] --> B["测试连通性并保存"]
  B --> C["创建项目并选择代码源"]
  C --> D["配置项目仓库"]
  D --> E["创建流水线"]
  E --> F["手动运行 CI"]
  F --> G["Jenkins 执行 Build"]
  G --> H["推送镜像到 Zot"]
  H --> I["生成 BuildResult"]
  I --> J["生成 ReleaseCandidate"]
  J --> K["更新 GitOps 仓库"]
  K --> L["Argo CD 同步部署"]
```

第一版可以先关闭自动 CD，只在 CI 成功后展示“可发布候选版本”。当 CD 阶段开启时，才允许更新 GitOps 仓库并触发 Argo CD。

## 6. 页面原型

### 6.1 项目列表

```text
┌────────────────────────────────────────────────────────────────────┐
│ Pipeline 平台                                      [+ 创建项目]     │
├────────────────────────────────────────────────────────────────────┤
│ 搜索项目: [ 电商                                      ]             │
├────────────────────────────────────────────────────────────────────┤
│ 项目名称       代码仓库              流水线数   最近运行   状态      │
│ order-service  order-service.git     3         main-ci 成功 正常 [进入]│
│ payment        payment-service.git   1         暂无运行    未运行[进入]│
└────────────────────────────────────────────────────────────────────┘
```

关键交互：

- 点击“创建项目”只创建平台内的 `Project`。
- 项目不等于 GitLab Group，也不等于 Kubernetes Namespace。
- 第一版项目直接绑定一个 Git 仓库和多条流水线。

### 6.2 项目概览

```text
┌────────────────────────────────────────────────────────────────────┐
│ 电商项目                                                           │
│ order-service.git · 3 条流水线 · 1 个环境 · 最近运行 8 分钟前      │
├───────────────┬────────────────────────────────────────────────────┤
│ 侧边导航      │ CI/CD 总览                                         │
│ 概览          │                                                    │
│ 代码仓库设置  │ 最近流水线运行                                     │
│ 流水线        │ order-service / main-ci      成功    2m14s [详情]  │
│ 运行记录      │ order-service / nightly-scan 失败    35s   [详情]  │
│ 制品          │                                                    │
│ 环境          │ 代码仓库                                            │
│ 设置          │ order-service.git                                   │
│               │                                                    │
│               │ 流水线                                              │
│               │ main-ci · nightly-scan · release-ci                 │
└───────────────┴────────────────────────────────────────────────────┘
```

项目概览只回答三个问题：

- 这个项目绑定的是哪个仓库？
- 最近 CI 是否健康？
- 有没有可部署或已部署的版本？

### 6.3 代码源配置

代码源是平台级可复用配置。用户先在“集成 / 代码源”中添加 GitLab、GitHub、Gitea 或 Generic Git，测试连通性后保存入库。创建项目时复用代码源，不重复填写 Base URL、认证方式和凭据。

```text
┌────────────────────────────────────────────────────────────────────┐
│ 集成 / 代码源                                      [+ 添加代码源]   │
├────────────────────────────────────────────────────────────────────┤
│ 名称              类型       地址                         状态      │
│ 公司 GitLab       GitLab     https://gitlab.example.com    已验证    │
│ GitHub            GitHub     https://github.com            已验证    │
│ 本地 Gitea        Gitea      https://gitea.local           未验证    │
└────────────────────────────────────────────────────────────────────┘
```

添加代码源：

```text
┌──────────────────────── 添加代码源 ───────────────────────────┐
│ 名称             [ 公司 GitLab                              ] │
│ 类型             [ GitLab v ]                                 │
│ Base URL         [ https://gitlab.example.com               ] │
│ 认证方式         [ Deploy Token v ]                           │
│ Username         [ ci-reader                                ] │
│ Token            [ ********                                ] │
│                                                            │
│                                      [测试连接] [保存]        │
└────────────────────────────────────────────────────────────┘
```

连通性测试必须验证：

```text
Base URL 可访问
凭据有效
能够读取仓库列表或指定仓库
能够读取分支和 Tag
失败时返回可理解原因
```

约束：

- 本地 MVP 阶段允许 Token、SSH Key 明文保存到平台数据库。
- 该策略仅用于本地开发和原型验证，后续生产化必须替换为加密字段或外部 Secret Manager。
- 创建项目只能选择已保存的代码源。
- 删除代码源前必须检查是否被项目引用。

### 6.4 项目 Git 设置

```text
┌────────────────────────────────────────────────────────────────────┐
│ order-service / Git 设置                                           │
├────────────────────────────────────────────────────────────────────┤
│ 代码源            公司 GitLab                              [切换]   │
│ Git 类型          GitLab                                            │
│ Base URL          https://gitlab.example.com                        │
│ 仓库地址          group/order-service.git                           │
│ 默认分支          main                                              │
│ 认证方式          Deploy Token                                      │
│ 连接状态          已验证                                 [重新验证] │
└────────────────────────────────────────────────────────────────────┘
```

创建项目时填写 Git 设置：

```text
┌──────────────────────── 创建项目 ─────────────────────────────┐
│ 项目名称         [ order-service                           ] │
│ 代码源           [ 公司 GitLab v ]                            │
│ 仓库地址         [ group/order-service.git                  ] │
│ 默认分支         [ main                                    ] │
│                                                            │
│                                      [读取分支] [创建]        │
└────────────────────────────────────────────────────────────┘
```

约束：

- 第一版一个项目选择一个代码源和一个仓库。
- 代码源保存 Base URL、认证方式和凭据。
- 项目保存仓库地址、默认分支和上下文目录。
- 多代码源引用、多仓库绑定留到第二阶段。

### 6.5 项目仓库信息

```text
┌────────────────────────────────────────────────────────────────────┐
│ order-service / 代码仓库                                           │
├────────────────────────────────────────────────────────────────────┤
│ 仓库地址       https://gitlab.example.com/group/order-service.git   │
│ 默认分支       main                                      [刷新分支] │
│ 最近提交       a1b2c3d · Add checkout adapter                       │
│ 分支           main · develop · release                             │
│ Tag            v1.0.0 · v1.1.0                                      │
└────────────────────────────────────────────────────────────────────┘
```

注意：

- 平台可以读取分支、Tag 和提交信息。
- 第一版所有 Pipeline 默认检出项目仓库。
- 单条 Pipeline 可以覆盖默认分支和上下文目录。

### 6.6 流水线列表

```text
┌────────────────────────────────────────────────────────────────────┐
│ 电商项目 / 流水线                              [+ 创建流水线]       │
├────────────────────────────────────────────────────────────────────┤
│ 流水线名称     构建档案       默认分支   最近状态                  │
│ main-ci        Java Maven     main       成功             [运行]   │
│ nightly-scan   Java Maven     main       未运行           [运行]   │
│ release-ci     Java Maven     release    失败             [运行]   │
└────────────────────────────────────────────────────────────────────┘
```

第一版流水线不是自由拖拽编排，而是“构建档案 + 参数”。

### 6.7 创建流水线

```text
┌──────────────────────── 创建流水线 ───────────────────────────┐
│ 基本信息                                                       │
│ 流水线名称        [ main-ci                                ]   │
│ 代码仓库          order-service.git                             │
│ 默认分支          [ main v ]                                    │
│                                                            │
│ 构建档案                                                       │
│ 类型              [ Java Maven v ]                              │
│ 版本              [ v1 v ]                                      │
│ JDK               [ 25 v ]                                      │
│ 测试命令          [ ./mvnw test                             ]   │
│ 打包命令          [ ./mvnw package -DskipTests              ]   │
│                                                            │
│ 镜像配置                                                       │
│ Registry          [ Zot - 10.211.55.4:30443 v ]                 │
│ 镜像名称          [ ecommerce/order-service                 ]   │
│ Dockerfile        [ Dockerfile                              ]   │
│ 构建上下文        [ .                                      ]   │
│                                                            │
│ 质量检查                                                       │
│ SonarQube         [ 关闭 ]                                      │
│ Trivy             [ 关闭 ]                                      │
│                                                            │
│                                      [保存] [保存并运行]        │
└────────────────────────────────────────────────────────────┘
```

后端保存的是 `PipelineConfiguration`，不是 Jenkinsfile。运行时由 `JenkinsExecutionEngine` 根据配置生成 Jenkins Pipeline Script。

### 6.8 选择流水线模板

第一版支持类似成熟 DevOps 平台的模板选择界面。用户先选择语言分类，再选择一个构建模板。模板卡片展示主要阶段，但不允许用户在第一版直接拖拽编辑。

```text
┌────────────────────────────────────────────────────────────────────┐
│ 添加流水线                                                         │
├────────────────────────────────────────────────────────────────────┤
│ ✓ 完善信息 ─────────────────────────────── ② 选择模板             │
├───────────────┬────────────────────────────────────────────────────┤
│ 空模板        │ Java                                               │
│ Java          │                                                    │
│ Node          │ ┌────────────────────────────────────────────────┐ │
│ Go            │ │ Maven · 测试、构建、镜像推送                  │ │
│ Python        │ │ 源码检出 ── Maven测试 ── Maven打包 ── 镜像推送 │ │
│ PHP           │ └────────────────────────────────────────────────┘ │
│ .NET Core     │                                                    │
│ C++           │ ┌────────────────────────────────────────────────┐ │
│ 其他          │ │ Maven · 扫描、测试、构建、镜像推送            │ │
│ 自定义模板    │ │ 源码检出 ── 代码扫描 ── 自动化测试            │ │
│               │ │                 └── Maven打包 ── 镜像推送     │ │
│               │ └────────────────────────────────────────────────┘ │
│               │                                                    │
│               │ ┌────────────────────────────────────────────────┐ │
│               │ │ Gradle · 测试、构建、镜像推送                 │ │
│               │ │ 源码检出 ── Gradle测试 ── Gradle打包 ── 镜像推送│ │
│               │ └────────────────────────────────────────────────┘ │
├───────────────┴────────────────────────────────────────────────────┤
│                                      [取消] [上一步] [确定]        │
└────────────────────────────────────────────────────────────────────┘
```

模板分类：

```text
空模板
Java
Node
Go
Python
PHP
.NET Core
C++
其他
自定义模板
```

第一版必须内置：

```text
Java / Maven · 测试、构建、镜像推送
Java / Maven · 扫描、测试、构建、镜像推送
Java / Gradle · 测试、构建、镜像推送
Node · 测试、构建、镜像推送
Python · 测试、构建、镜像推送
空模板
```

注意：模板里看到的“部署”在本平台中不能表示登录主机发布。第一版应表达为“镜像推送”或“GitOps 部署到 K8s”。CD 阶段开启后，部署动作必须通过 GitOps 仓库和 Argo CD 完成。

### 6.9 流水线设计画布

第二版支持类似画布的流水线设计页，用于展示和编辑 Stage / Job / Step。第一版可以先做只读预览。

```text
┌────────────────────────────────────────────────────────────────────┐
│ 设计 | 流程设计 | 触发设置 | 变量                                  │
├────────────────────────────────────────────────────────────────────┤
│ 阶段-1          阶段-2             阶段-3            阶段-4         │
│                                                                    │
│ [通用 Git] ── + ── [代码扫描] ── + ── [Maven 构建] ── + ── [镜像推送]│
│                   │                    │                          │
│                   └── [自动化测试] ─────┘                          │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

画布能力分阶段实现：

```text
V1：只读预览，由模板生成，不支持拖拽编辑。
V2：允许启停内置步骤，例如 Sonar、Trivy、自动化测试。
V3：允许新增自定义命令步骤，但仍由平台转换为 Jenkins Pipeline Script。
V4：支持高级模式导入/导出平台 YAML，但不直接暴露 Jenkinsfile。
```

### 6.10 运行列表

```text
┌────────────────────────────────────────────────────────────────────┐
│ 电商项目 / 运行记录                                                │
├────────────────────────────────────────────────────────────────────┤
│ Run ID      流水线      分支     Commit    状态      耗时           │
│ #104        main-ci     main     a1b2c3d   成功      2m14s [详情]   │
│ #103        main-ci     main     f4e5d6c   失败      31s   [详情]   │
│ #102        nightly     main     a1b2c3d   成功      4m02s [详情]   │
└────────────────────────────────────────────────────────────────────┘
```

运行记录来自数据库投影，但真实执行状态以 Jenkins API 为准。

### 6.11 运行详情

```text
┌────────────────────────────────────────────────────────────────────┐
│ Run #104 · order-service / main-ci                  成功           │
│ 分支 main · Commit a1b2c3d · 触发人 bo · 2m14s                     │
├────────────────────────────────────────────────────────────────────┤
│ 阶段                                                               │
│ ✓ checkout        8s       [日志]                                  │
│ ✓ maven-test      42s      [日志]                                  │
│ ✓ maven-package   36s      [日志]                                  │
│ ✓ image-build     38s      [日志]                                  │
│ ✓ image-push      10s      [日志]                                  │
│ ✓ build-result    1s       [查看结果]                              │
├────────────────────────────────────────────────────────────────────┤
│ 制品                                                               │
│ OCI Image                                                          │
│ 10.211.55.4:30443/ecommerce/order-service:a1b2c3d                  │
│ digest: sha256:xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx                     │
├────────────────────────────────────────────────────────────────────┤
│ 报告                                                               │
│ 单元测试：通过 · 128 tests · 0 failed                              │
│ SonarQube：未启用                                                   │
│ Trivy：未启用                                                       │
├────────────────────────────────────────────────────────────────────┤
│ 操作                                      [重跑] [取消] [发布到开发] │
└────────────────────────────────────────────────────────────────────┘
```

失败状态示例：

```text
✕ maven-test      31s      单元测试失败 [日志]
- image-build     跳过
- image-push      跳过
- build-result    记录失败结果
```

### 6.12 发布候选与部署

```text
┌────────────────────────────────────────────────────────────────────┐
│ 电商项目 / 环境 / dev                                              │
├────────────────────────────────────────────────────────────────────┤
│ 应用              当前镜像                    同步状态             │
│ order-service     sha256:aaaa                 Synced / Healthy      │
│ payment-service   sha256:bbbb                 OutOfSync             │
│ web-frontend      sha256:cccc                 Synced / Healthy      │
├────────────────────────────────────────────────────────────────────┤
│ 最近发布                                                           │
│ order-service #104  a1b2c3d  bo  2026-07-29 14:20  成功            │
└────────────────────────────────────────────────────────────────────┘
```

第一版 CD 页面只展示开发环境，不做复杂环境晋级。

## 7. MVP 页面优先级

第一批必须实现：

```text
P0-01 项目列表
P0-02 添加代码源
P0-03 测试代码源连通性
P0-04 创建项目并选择代码源
P0-05 项目 Git 设置
P0-06 项目仓库信息
P0-07 选择流水线模板
P0-08 创建 Java Maven 流水线
P0-09 流程设计只读预览
P0-10 手动运行流水线
P0-11 运行详情
P0-12 BuildResult 展示
```

第二批实现：

```text
P1-01 GitOps 更新
P1-02 Argo CD 状态展示
P1-03 发布到开发环境
P1-04 SonarQube 开关
P1-05 Webhook 触发
P1-06 内置步骤启停
```

暂不实现：

```text
自由拖拽式流水线编辑器
复杂权限模型
插件市场
多环境审批流
多集群部署
```

## 8. 后端接口映射

```text
POST /api/v1/projects
GET  /api/v1/projects
GET  /api/v1/projects/{projectId}
PATCH /api/v1/projects/{projectId}/git-settings
POST /api/v1/projects/{projectId}/git-settings/verification

POST /api/v1/code-sources
GET  /api/v1/code-sources
GET  /api/v1/code-sources/{codeSourceId}
PATCH /api/v1/code-sources/{codeSourceId}
POST /api/v1/code-sources/{codeSourceId}/verification
GET  /api/v1/code-sources/{codeSourceId}/repositories

GET  /api/v1/projects/{projectId}/repository
GET  /api/v1/projects/{projectId}/repository/branches
GET  /api/v1/projects/{projectId}/repository/tags
GET  /api/v1/projects/{projectId}/repository/revisions/{revision}

POST /api/v1/projects/{projectId}/pipelines
GET  /api/v1/projects/{projectId}/pipelines

POST /api/v1/pipelines/{pipelineId}/runs
GET  /api/v1/pipelines/{pipelineId}/runs
GET  /api/v1/runs/{runId}
GET  /api/v1/runs/{runId}/logs
GET  /api/v1/runs/{runId}/build-result
POST /api/v1/runs/{runId}/cancellation
POST /api/v1/runs/{runId}/retry

GET  /api/v1/projects/{projectId}/environments
GET  /api/v1/environments/{environmentId}/deployments
POST /api/v1/release-candidates/{candidateId}/deployments
```

第二阶段再增加：

```text
POST /api/v1/projects/{projectId}/code-sources
GET  /api/v1/projects/{projectId}/code-sources
POST /api/v1/projects/{projectId}/repositories
GET  /api/v1/projects/{projectId}/repositories
GET  /api/v1/repositories/{repositoryId}/pipelines
```

## 9. FlowCI 风格交互原则

第一版交互参考 FlowCI 的“创建即配置、模板即入口、画布即理解、日志即反馈”。

### 9.1 创建项目

创建项目不拆成多层资源。用户在一个向导中完成：

```text
项目名称
选择代码源
仓库地址
默认分支
读取分支
```

成功后直接进入项目详情，而不是再要求用户绑定仓库。

### 9.2 添加流水线

添加流水线采用两步向导：

```text
第一步：完善信息
  - 流水线名称
  - 默认分支
  - 上下文目录
  - 触发方式

第二步：选择模板
  - 左侧语言分类
  - 右侧模板卡片
  - 卡片展示主要阶段
  - 确认后生成 PipelineConfiguration
```

### 9.3 流程设计

流程设计页用于降低理解成本，不是第一版的自由编排器。

```text
V1：模板生成只读画布。
V2：点击节点配置参数和启停内置 Step。
V3：允许添加自定义命令 Step。
```

节点展示规则：

```text
节点名称使用业务词：源码管理、代码扫描、自动化测试、Maven 构建、镜像推送。
节点状态使用颜色和图标：未配置、待运行、运行中、成功、失败、跳过。
节点错误直接显示在节点上，并可点击进入配置或日志。
```

### 9.4 运行详情

运行详情参考 FlowCI 的即时反馈体验：

```text
顶部展示运行摘要：状态、分支、Commit、触发人、耗时。
中间展示阶段图和 Step 状态。
底部展示当前选中 Step 的日志。
右侧展示制品、报告和变量快照。
```

失败时默认定位到第一个失败 Step。用户不需要先理解 Jenkins Job/Build 细节才定位问题。

### 9.5 模板与插件

模板卡片表达的是平台内置能力，不是直接暴露底层执行配置。

```text
模板：一组有顺序和并行关系的 Step。
插件：Step 的实现类型，例如 git-checkout、maven-build、sonar-scan。
参数：用户需要填写的最小配置。
结果：BuildResult 中的 artifact 和 report。
```

第一版插件全部内置，不支持上传第三方插件。

## 10. 第一版前端导航

```text
顶部：项目选择器、全局搜索、系统健康
左侧：概览、代码仓库设置、流水线、运行记录、制品、环境、设置
主区：列表、详情、创建表单
右侧：暂不做复杂抽屉，只在运行详情中保留日志入口
```

前端应隐藏 Jenkins、Kubernetes、Argo CD 的底层对象名称，除非用户进入调试信息。普通用户看到的是：

```text
流水线
阶段
步骤
运行
日志
制品
环境
部署
```

## 11. 关键产品约束

- 业务仓库不要求存在 CI 配置文件。
- 第一版一个 Project 只引用一个代码源和一个代码仓库。
- 第一版一个 Pipeline 默认检出 Project 的代码仓库。
- 一个 Project 可以创建多条 Pipeline。
- Pipeline 配置保存到平台数据库。
- BuildProfile 和 Jenkins Pipeline Script 模板使用版本化定义。
- 运行状态以 Jenkins API 为事实源。
- 镜像制品以 Zot digest 为事实源。
- 部署期望状态以 GitOps 仓库为事实源。
- 部署执行状态以 Argo CD/Kubernetes 为事实源。

## 12. 防大改边界

为了降低后续大改风险，第一版必须把容易变化的实现细节隔离在适配层中。领域模型不直接依赖 Tekton、FlowCI、Agent、Argo CD 或某一种 Git 平台。

必须稳定的产品概念：

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

允许替换的实现：

```text
GitLab / GitHub / Gitea / Generic Git
V1: JenkinsExecutionEngine
Future: TektonExecutionEngine / AgentExecutionEngine
Zot / Harbor / 其他 OCI Registry
Argo CD / 其他 GitOps Controller
SonarQube / 其他代码质量平台
```

Java 后端必须保留这些边界：

```text
source     负责代码源和仓库读取
pipeline   负责流水线配置和模板选择
execution  负责运行记录和执行引擎抽象
registry   负责镜像仓库访问
gitops     负责 GitOps 仓库更新
deploy     负责部署状态投影
```

第一版可以写得简单，但不能把以下内容硬编码进核心领域层：

```text
Jenkins Job/Build DTO
Tekton CRD 类型
Kubernetes Secret 类型
某一个 GitLab API DTO
某一个 Registry 地址
某一个 Java Maven 模板步骤
某一个 Argo CD Application 结构
```

第一版只实现 Jenkins 执行路线。如果未来要从 Jenkins 改成 Tekton 或 FlowCI 风格 Agent，或从 Zot 改成 Harbor，应只替换适配器和模板，不重写 Project、Pipeline、Run、BuildResult 这些核心对象。

## 13. 原型验收标准

- 用户可以不接触 `kubectl` 创建并运行一条 Java Maven CI 流水线。
- 用户可以从运行详情定位失败阶段和失败日志。
- 用户可以看到源码提交、镜像 tag、镜像 digest 和 BuildResult 的关联。
- 用户可以理解“CI 成功”和“已部署”是两个不同状态。
- 前端页面不暴露 Jenkinsfile 编辑器。
- 业务仓库不需要提交平台专用 CI 配置。
