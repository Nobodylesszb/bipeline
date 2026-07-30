# 容器化多语言构建规格

- 状态：Draft v0.1
- 日期：2026-07-30
- 范围：Jenkins 执行引擎下的 Java、Python、Node.js 多版本 OCI 镜像构建。
- 前置规格：`009-pipeline-control-plane-spec.md`、`010-jenkins-connection-spec.md`

## 1. 目标

平台中的可部署应用最终统一产出 OCI 镜像。

第一阶段支持：

```text
Java 8 / 11 / 17 / 21
Python 3.10 / 3.11 / 3.12 / 3.13
Node.js 22 / 24
```

每次 PipelineRun 使用独立的临时构建容器。Jenkins 负责任务调度，Docker BuildKit
负责隔离的编译、打包和镜像生成，Zot 负责保存最终 OCI 镜像。

```text
PipelineRun
  → Jenkins Job
  → Docker BuildKit
  → 独立构建容器
  → OCI 镜像
  → Zot
  → 保存 image tag 和 digest
```

## 2. 核心决策

### 2.1 Jenkins 主机不安装多套语言运行时

Jenkins 主机只要求：

- Docker CLI。
- 可访问的 Docker daemon / BuildKit。
- 访问源码仓库和 Zot 的网络。

Jenkins 主机不要求预装：

- JDK 8 / 11 / 17 / 21。
- Maven 或 Gradle。
- Python。
- Node.js。

编译工具和运行时版本由平台选择的构建镜像提供。

### 2.2 每次构建相互隔离

每个 PipelineRun 必须拥有：

- 独立工作目录。
- 独立构建容器。
- 独立环境变量。
- 独立镜像标签。
- 独立日志上下文。

构建结束后删除临时容器和临时凭证。

允许共享依赖缓存，例如 Maven `.m2`，但缓存不能保存：

- Git Token。
- Registry 密码。
- 项目源码。
- 上一次构建的产物。

### 2.3 平台模板与自定义步骤并存

现有手工 Stage/Step 流水线继续保留，构建模式记为：

```text
CUSTOM_STEPS
```

新增平台模板模式：

```text
PLATFORM_TEMPLATE
```

已有流水线不强制迁移。只有新建或主动修改的流水线才使用 BuildProfile。

### 2.4 Java 使用 Eclipse Temurin

Eclipse Temurin 是平台默认的 OpenJDK 发行版。

用户只选择 Java 版本，不能直接提交任意 builder/runtime 镜像地址。平台通过启用的
BuildProfile 将版本映射为经过验证的镜像。

首批版本：

| Java 版本 | Builder | Runtime |
| --- | --- | --- |
| 8 | Maven + Temurin JDK 8 | Temurin JRE 8 |
| 11 | Maven + Temurin JDK 11 | Temurin JRE 11 |
| 17 | Maven + Temurin JDK 17 | Temurin JRE 17 |
| 21 | Maven + Temurin JDK 21 | Temurin JRE 21 |

BuildProfile 保存完整镜像引用。PipelineRun 额外保存实际拉取到的镜像 digest，避免浮动
标签变化导致构建不可追溯。

## 3. BuildProfile 模型

复用现有 `build_profiles` 表，不在 Jenkins 模块复制语言模板。

BuildProfile 至少表达：

```text
template_key
template_version
language
schema_json
default_config_json
enabled
```

第一批模板：

```text
java-maven-image
java-gradle-image
python-image
node-service-image
node-static-image
```

### 3.1 Java 配置

```json
{
  "javaVersion": "17",
  "buildTool": "MAVEN",
  "testPolicy": "SKIP",
  "dockerfileMode": "PLATFORM_TEMPLATE",
  "contextDirectory": ".",
  "applicationPort": 8080,
  "jvmOptions": []
}
```

第一版约束：

```text
buildJdkVersion = targetJavaVersion = runtimeJavaVersion
```

项目 `pom.xml` 或 Gradle 配置仍是源码和字节码兼容性的事实来源。平台不能静默修改项目
的 `source`、`target` 或 `release`。

如果项目配置与所选 Java 版本冲突，构建必须失败并给出明确错误。后续高级模式可以分别
开放：

```text
buildJdkVersion
targetJavaVersion
runtimeJavaVersion
```

### 3.2 Python 配置

```json
{
  "pythonVersion": "3.12",
  "testPolicy": "SKIP",
  "dependencyFile": "requirements.txt",
  "dockerfileMode": "PLATFORM_TEMPLATE",
  "contextDirectory": ".",
  "applicationPort": 8000,
  "startCommand": "uvicorn app.main:app --host 0.0.0.0 --port 8000"
}
```

首批允许版本：

```text
3.10
3.11
3.12
3.13
```

### 3.3 Node.js 配置

Node.js 必须区分两种应用形态：

```text
SERVICE
  Node.js 后端、SSR、Next.js standalone 等需要 Node.js 运行时的应用

STATIC
  Vue、React、Vite 等构建后只需要静态文件的前端应用
```

平台不把“前端/后端”作为执行层概念，而是保存实际部署形态：

```text
STATIC
  构建产物是 HTML/CSS/JS 等静态文件，最终镜像由 Nginx 等静态服务器运行。

SERVICE
  最终镜像需要持续运行 Node.js 进程并监听端口。
```

Next.js、Nuxt、Remix 等项目必须按实际部署方式选择：

- SSR、API Route、Server Action、standalone server：`SERVICE`。
- 明确执行静态导出且产物不需要 Node.js：`STATIC`。

Electron 不属于当前 Web OCI 部署模板。Monorepo 必须先指定 `contextDirectory`，再对该目录
执行探测和构建。

Node.js 后端服务配置：

```json
{
  "nodeVersion": "24",
  "applicationType": "SERVICE",
  "packageManager": "NPM",
  "installCommand": "npm ci",
  "buildCommand": "npm run build",
  "startCommand": "npm start",
  "testPolicy": "SKIP",
  "dockerfileMode": "PLATFORM_TEMPLATE",
  "contextDirectory": ".",
  "applicationPort": 3000
}
```

静态前端配置：

```json
{
  "nodeVersion": "24",
  "applicationType": "STATIC",
  "packageManager": "PNPM",
  "installCommand": "pnpm install --frozen-lockfile",
  "buildCommand": "pnpm build",
  "outputDirectory": "dist",
  "testPolicy": "SKIP",
  "dockerfileMode": "PLATFORM_TEMPLATE",
  "contextDirectory": ".",
  "applicationPort": 80
}
```

第一版生产构建默认只开放 Node.js 22 和 24：

- Node.js 22：Maintenance LTS。
- Node.js 24：Active LTS，作为默认版本。
- Node.js 20 及以下已经 EOL，不作为新流水线可选版本。
- Node.js 26 当前不是 LTS，在进入 LTS 后再加入默认生产版本列表。

支持的包管理器：

```text
NPM
PNPM
YARN
```

平台优先根据锁文件建议包管理器：

```text
package-lock.json → NPM
pnpm-lock.yaml    → PNPM
yarn.lock         → YARN
```

检测结果只用于推荐，创建流水线时仍需保存明确的包管理器选择。pnpm 和 Yarn 版本优先由
项目 `package.json` 的 `packageManager` 字段及 Corepack 管理，平台不静默改写项目锁文件。

#### Node.js 应用形态探测

平台提供启发式探测，但探测结果不能直接决定生产构建模板。

探测输入：

```text
projectId
revision
contextDirectory
```

探测证据包括：

- `vite.config.*`、`vue.config.*`、`angular.json` 等静态前端构建特征。
- `nest-cli.json`、Express/Koa/Fastify 等服务端特征。
- `next.config.*`、`nuxt.config.*` 等 SSR/全栈特征。
- `package.json` 中的 scripts、dependencies、devDependencies 和 packageManager。
- 项目锁文件。

探测结果：

```json
{
  "language": "NODE",
  "suggestedProfileKey": "node-static-image",
  "suggestedApplicationType": "STATIC",
  "confidence": "MEDIUM",
  "evidence": [
    "vite.config.ts",
    "package.json scripts.build"
  ],
  "requiresConfirmation": true
}
```

规则：

- 自动探测只出现在项目接入或流水线创建向导中。
- 用户必须确认 `STATIC` 或 `SERVICE`。
- 确认后将 `profileKey` 和 `applicationType` 保存到数据库。
- PipelineRun 使用已保存配置，不在每次运行时重新判断。
- 仓库内容变化后，用户可以主动执行“重新探测”，但平台不能静默切换模板。
- 无法可靠判断、SSR、全栈和冲突证据统一返回 `LOW` 置信度并要求人工选择。

## 4. Pipeline 配置

`pipelines` 增加以下概念：

```text
build_mode
build_profile_id
build_profile_version
build_config_json
```

兼容规则：

- `CUSTOM_STEPS`：`build_profile_id` 可以为空，继续使用现有 Stage/Step。
- `PLATFORM_TEMPLATE`：必须指定启用的 BuildProfile 和通过 schema 校验的配置。
- Pipeline 激活前必须完成 BuildProfile 校验。
- PipelineRun 必须保存构建配置快照，运行期间不读取可能已被修改的新模板。

## 5. API

所有接口继续使用 `POST`。

### 5.1 查询 BuildProfile

```http
POST /api/v1/build-profiles/list
```

请求：

```json
{
  "language": "JAVA"
}
```

响应需要包含：

```json
{
  "items": [
    {
      "profileKey": "java-maven-image",
      "profileVersion": "1.0",
      "language": "JAVA",
      "supportedRuntimeVersions": ["8", "11", "17", "21"],
      "defaultRuntimeVersion": "17"
    }
  ]
}
```

### 5.2 探测项目构建类型

```http
POST /api/v1/build-profiles/detect
```

请求：

```json
{
  "projectId": 1,
  "revision": "main",
  "contextDirectory": "."
}
```

该接口只返回推荐结果和证据，不创建或修改 Pipeline。

### 5.3 预览模板生成结果

```http
POST /api/v1/build-profiles/preview
```

请求：

```json
{
  "profileKey": "java-maven-image",
  "profileVersion": "1.0",
  "config": {
    "javaVersion": "17",
    "testPolicy": "SKIP"
  }
}
```

预览只返回 BuildPlan，不创建 Pipeline，不调用 Jenkins。

### 5.4 使用模板创建流水线

```http
POST /api/v1/pipelines/create
```

Java 示例：

```json
{
  "projectId": 1,
  "name": "main-ci",
  "description": "Java 17 镜像构建",
  "triggerType": "MANUAL",
  "branchName": "main",
  "buildMode": "PLATFORM_TEMPLATE",
  "buildProfile": {
    "profileKey": "java-maven-image",
    "profileVersion": "1.0",
    "config": {
      "javaVersion": "17",
      "testPolicy": "SKIP"
    }
  }
}
```

Python 示例：

```json
{
  "projectId": 1,
  "name": "python-ci",
  "triggerType": "MANUAL",
  "branchName": "main",
  "buildMode": "PLATFORM_TEMPLATE",
  "buildProfile": {
    "profileKey": "python-image",
    "profileVersion": "1.0",
    "config": {
      "pythonVersion": "3.12",
      "testPolicy": "SKIP",
      "startCommand": "python main.py"
    }
  }
}
```

Node.js 静态前端示例：

```json
{
  "projectId": 1,
  "name": "web-frontend-ci",
  "triggerType": "MANUAL",
  "branchName": "main",
  "buildMode": "PLATFORM_TEMPLATE",
  "buildProfile": {
    "profileKey": "node-static-image",
    "profileVersion": "1.0",
    "config": {
      "nodeVersion": "24",
      "packageManager": "PNPM",
      "buildCommand": "pnpm build",
      "outputDirectory": "dist",
      "testPolicy": "SKIP"
    }
  }
}
```

### 5.5 页面探测与确认交互

创建流水线页面在用户选择仓库、分支和 `contextDirectory` 后，自动调用：

```http
POST /api/v1/build-profiles/detect
```

页面必须明确区分：

```text
系统探测结果 ≠ 已保存的流水线配置
```

推荐交互：

```text
┌─────────────────────────────────────────────────────────┐
│ 检测到 Node.js 项目                                      │
│                                                         │
│ 推荐构建类型：静态前端镜像                               │
│ 置信度：中                                              │
│                                                         │
│ 判断依据：                                              │
│ ✓ 发现 vite.config.ts                                   │
│ ✓ package.json 包含 build 命令                          │
│ ✓ 默认输出目录为 dist                                   │
│                                                         │
│ 请选择最终部署方式：                                    │
│                                                         │
│ [ 静态前端（推荐） ]   [ Node.js 服务 ]                 │
│                                                         │
│ 静态前端：Node 构建，Nginx 运行                          │
│ Node 服务：最终镜像持续运行 Node.js 进程                 │
└─────────────────────────────────────────────────────────┘
```

页面规则：

- 探测过程中显示加载状态，不允许重复触发。
- 探测成功后展示语言、推荐类型、置信度和证据。
- 用户必须选择并确认一种部署形态，才能进入下一步。
- 推荐类型默认高亮，但不能在用户无确认时直接保存。
- 用户可以选择与推荐结果不同的类型。
- 手工覆盖推荐结果时展示提醒，但不能阻止保存。
- `LOW` 置信度使用警告样式，并默认不选中任何类型。
- Next.js、Nuxt、Remix、冲突证据和 Monorepo 默认提示“需要人工确认”。
- 探测失败时允许用户手工选择，页面展示失败原因和“重新探测”入口。
- 修改分支或 `contextDirectory` 后，旧探测结果失效并重新探测。

低置信度示例：

```text
无法可靠判断该项目的部署方式

发现 next.config.js。该项目可能是：
○ Node.js SSR 服务
○ 静态导出站点

请根据项目实际启动方式选择。
```

用户确认后，页面提交明确值：

```json
{
  "buildProfile": {
    "profileKey": "node-static-image",
    "profileVersion": "1.0",
    "config": {
      "applicationType": "STATIC",
      "nodeVersion": "24",
      "packageManager": "PNPM"
    }
  }
}
```

详情页面同时展示：

- 当前保存的应用形态。
- 上次探测建议。
- 上次探测置信度与证据。
- 是否由用户覆盖系统建议。
- “重新探测”按钮。

## 6. BuildPlan

BuildProfile 不能直接生成 Jenkins XML。应用层先生成与执行引擎无关的 BuildPlan：

```text
BuildProfile + Pipeline + Repository
  → BuildPlan
  → JenkinsBuildPlanTranslator
  → Jenkins Job
```

BuildPlan 至少包含：

```json
{
  "checkout": {
    "branch": "main",
    "commitSha": null
  },
  "builderImage": "maven:3.9.x-eclipse-temurin-17",
  "runtimeImage": "eclipse-temurin:17-jre",
  "steps": [
    {"type": "BUILD_IMAGE"},
    {"type": "PUSH_IMAGE"},
    {"type": "VERIFY_DIGEST"}
  ],
  "artifacts": [
    {"kind": "OCI_IMAGE", "role": "DEPLOYABLE"}
  ]
}
```

Jenkins、未来的 Tekton，以及本地执行器只能依赖 BuildPlan，不能读取 BuildProfile 的
原始 JSON 并自行解释业务规则。

## 7. Java 镜像构建

平台模板生成多阶段 Dockerfile：

```dockerfile
FROM ${BUILDER_IMAGE} AS builder

WORKDIR /workspace
COPY . .
RUN mvn clean package -DskipTests

FROM ${RUNTIME_IMAGE}

WORKDIR /app
COPY --from=builder /workspace/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

`SKIP` 第一版使用：

```text
-DskipTests
```

它跳过测试执行，但仍编译测试代码。平台不使用
`-Dmaven.test.skip=true` 代替该语义。

## 8. Python 镜像构建

平台模板生成：

```dockerfile
FROM python:${PYTHON_VERSION}-slim

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .

EXPOSE ${APPLICATION_PORT}
CMD ${START_COMMAND}
```

用户提交的启动命令必须经过模板参数校验，不能直接拼接到 Jenkins 控制命令中。

## 9. Node.js 镜像构建

### 9.1 Node.js 后端服务

Node.js 服务使用多阶段镜像：

```dockerfile
FROM node:${NODE_VERSION}-bookworm-slim AS builder

WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM node:${NODE_VERSION}-bookworm-slim

WORKDIR /app
ENV NODE_ENV=production
COPY --from=builder /workspace ./
EXPOSE ${APPLICATION_PORT}
CMD ["npm", "start"]
```

实际模板必须根据所选包管理器处理对应锁文件。模板不能同时复制不存在的锁文件。

### 9.2 静态前端

静态前端在 Node.js 容器中构建，在 Nginx 容器中运行：

```dockerfile
FROM node:${NODE_VERSION}-bookworm-slim AS builder

WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine

COPY --from=builder /workspace/dist /usr/share/nginx/html
EXPOSE 80
```

Node.js 只存在于 builder 阶段，不进入最终静态前端镜像。

项目的安装、构建和启动命令属于 BuildProfile 配置，但在进入 BuildPlan 前必须经过命令
策略校验。第一版只允许模板支持的命令形态，不接受拼接平台凭证或 Docker 控制命令。

## 10. Registry 与构建结果

模板流水线执行前必须绑定 RegistryConnection。

构建成功后至少保存：

```text
registry_connection_id
image_repository
image_tag
image_digest
builder_image
builder_image_digest
runtime_image
runtime_image_digest
```

第一版可部署制品只支持：

```text
OCI_IMAGE
```

Wheel、Maven Package、npm Package 属于后续包仓库发布能力，不阻塞 OCI 镜像构建。

## 11. Jenkins 执行约束

当前 Jenkins 实现切换到容器化构建前，必须解决：

1. Jenkins 网络调用不能长期占用数据库事务。
2. PipelineRun 编号必须支持并发原子分配。
3. Git 和 Registry 凭证不能写入永久 Jenkins Job 配置。
4. 同一个 Jenkins Job 的排队构建不能被后续运行覆盖配置。
5. `commitSha` 不为空时必须检出指定提交。
6. 日志流必须主动同步 Jenkins 构建状态。

本地 MVP 可以让 Jenkins 调用宿主机 Docker daemon。正式多租户环境不得把不受信任的
构建直接暴露给宿主机 Docker socket，应迁移到独立 Kubernetes Pod 和 rootless
BuildKit 等隔离方案。

## 12. 非目标

第一阶段不做：

- 一次 PipelineRun 并行构建多个 Java 版本。
- 用户提交任意 builder/runtime 镜像。
- 自动修改 `pom.xml` 或 Gradle 文件。
- 发布 Wheel、Maven Package 或 npm Package。
- Windows 容器构建。
- 多架构镜像。
- 使用 EOL Node.js 版本创建新的生产流水线。

## 13. 验收标准

### 13.1 BuildProfile

- 可以查询 Java、Python 和 Node.js 模板。
- Java 只接受 8、11、17、21。
- Python 只接受 3.10、3.11、3.12、3.13。
- Node.js 生产模板只接受 22、24，默认使用 24。
- Node.js 必须区分 `SERVICE` 和 `STATIC`。
- Node.js 支持 NPM、PNPM、YARN，并保存明确的包管理器选择。
- 不支持的版本在创建或预览阶段返回校验错误。

### 13.2 隔离

- 两个 PipelineRun 可以使用不同 Java/Python/Node.js 版本。
- 两次构建使用不同工作目录和容器。
- 构建结束后不存在包含源码凭证的临时文件。
- 一个构建失败不修改另一个构建的工作目录和产物。

### 13.3 可追溯

- PipelineRun 保存 BuildProfile 和配置快照。
- 构建结果保存最终镜像 tag 和 digest。
- 能查询本次构建实际使用的 builder/runtime 镜像及 digest。

### 13.4 兼容

- 现有 `CUSTOM_STEPS` 流水线可以继续查询和运行。
- 数据库迁移不会要求已有 Pipeline 立即绑定 BuildProfile。
- Jenkins 主机没有安装对应 JDK/Python 时仍能通过构建容器完成打包。
- Jenkins 主机没有安装 Node.js 或前端包管理器时仍能完成 Node.js 构建。
