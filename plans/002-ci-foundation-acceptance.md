# Plan 002: 建立 CI Foundation 验收测试与证据链

> This plan is an outcome contract, not a step-by-step script. Design the test
> harness against the implementation produced by Plan 001. Runtime acceptance
> is performed only after implementation review. Stop on any STOP condition.
> When complete, update this plan in `plans/README.md`.
>
> Drift check: this workspace had no Git repository when planned. After a Git
> baseline exists, compare `scripts/ci/`, `examples/`, `pipelines/`, `profiles/`
> and `docs/runbooks/` against the planned commit before executing acceptance.

## Status

- Priority: P1
- Effort: M
- Risk: MED
- Depends on: `plans/001-ci-foundation-implementation.md`
- Category: tests
- Execution: review pause; verification must be independent from delegated implementation
- Planned at: unversioned workspace, 2026-07-28

## Requirement

提供一套可重复、退出码明确、不会泄漏凭据的验收入口，证明 Plan 001 实现满足 `CI-AC-001` 至 `CI-AC-011`、ADR-001 运行时兼容性门槛以及 CI Exit Gate。测试必须同时覆盖成功路径、预期失败路径、资源/安全约束和跨对象追踪，不能只凭 PipelineRun 显示 Succeeded 判断通过。

验收输出必须允许后来者从证据定位 application、Git repository、完整 commit SHA、PipelineRun、TaskRun、JUnit 报告、OCI image、digest、platforms、模板版本和最终状态，但不得复制 Secret 值。

## Decisions & tradeoffs

- **一个统一入口、多个具名 suite**：`run-acceptance.sh --suite <name>` 是外部合同，suite 必须可单独重跑。Rejected: 依赖操作者逐条复制 kubectl 命令——不可重复且难以汇总退出状态。Based on: Plan 001 Direction。
- **测试数据使用版本化夹具和固定 revision**：成功、失败、缺文件和两个项目复用场景必须可重建。Rejected: 临时在线示例仓库或移动分支——会使相同输入产生不同结果。Based on: `docs/specs/002-ci-foundation.md:153-168,448-462`。
- **预期失败由断言转化为 suite 成功**：例如错误凭据应产生明确 Failed，而不是让验收脚本以未解释错误退出。Rejected: 只检查 shell exit code——无法区分产品行为和测试工具故障。Based on: `docs/specs/002-ci-foundation.md:381-390,436-446`。
- **证据最小化且脱敏**：保存资源名称、状态、时间、digest、SHA 和报告摘要；Secret 只保存引用名称。Rejected: 导出完整 namespace 或环境变量——泄漏面过大。Based on: `docs/specs/002-ci-foundation.md:411-428,456-458`。
- **运行时测试串行**：12 GB 单节点首次只允许一个构建并发。Rejected: 并行跑完整矩阵——会让资源争用混淆功能失败。Based on: `docs/decisions/001-ci-toolchain-baseline.md:114-123`。
- **CI 与发布边界是负向验收项**：测试必须证明模板不存在 GitOps 更新、kubectl apply 或 ReleaseCandidate 生成。Rejected: 仅因尚未安装 Argo CD 就认为解耦成立——模板仍可能埋入直接部署逻辑。Based on: `docs/specs/002-ci-foundation.md:472-495`。

## Direction

### Milestone 1: 测试夹具和断言协议稳定

至少具备两个正常 Java Maven Wrapper 项目，以及失败测试、缺少 Wrapper、缺少 pom、错误 revision、Registry 失败和认证失败的确定性夹具。每个 suite 定义前置状态、动作、预期 Tekton 终态、必须出现/禁止出现的 Task、预期结果字段和证据文件名。

Validation: `./scripts/ci/run-acceptance.sh --list` -> exit 0，列出本计划所有 suite；夹具 revision 均为完整 SHA 或受版本控制的本地标识。

### Milestone 2: 环境与 Tekton 兼容性被机器验证

覆盖节点 ARM64、K3s/Tekton 固定版本、Pod Ready、DNS/时间、PVC、Registry TLS/认证、Tekton success/failure/timeout、Task 结果传递和三次重复 smoke。检查实际 PodSpec 不包含 hostPath Docker Socket、privileged 或禁止参数。

Validation: `./scripts/ci/run-acceptance.sh --suite compatibility` -> exit 0。

### Milestone 3: Git 与 Maven 行为完整覆盖

覆盖 branch、tag、full SHA、context-dir、公开仓库、私有只读认证、无效地址、无权限、不存在 revision、正常测试、失败测试、缺 Wrapper、缺 pom、JDK 不兼容和依赖下载失败。失败测试必须断言镜像 Task 未创建或未执行。

Validation: `./scripts/ci/run-acceptance.sh --suite source-and-test` -> exit 0。

### Milestone 4: OCI 构建、不可变性与 ARM64 可运行被证明

覆盖成功 push、错误 Registry 凭据、不可达 Registry、digest 拉取、平台检查、ARM64 启动、相同提交重跑三次以及每次运行身份独立。验证 BuildKit 安全上下文和参数满足 ADR。

Validation: `./scripts/ci/run-acceptance.sh --suite artifact` -> exit 0。

### Milestone 5: 模板复用、BuildResult 和边界被证明

两个 Java 项目使用相同模板版本，通用输入不包含 Java 专属必填字段，Java 参数只存在于 build profile；BuildResult 的 `source`、`artifacts[]`、`reports[]`、`provenance` 完整，OCI 运行时制品包含 digest 和 platforms。模板不得生成 ReleaseCandidate、修改 GitOps 或直接部署。

Validation: `./scripts/ci/run-acceptance.sh --suite contract` -> exit 0。

### Milestone 6: 安全、资源和清理证据闭环

对 Git 文件、Tekton 资源、日志和证据执行凭据泄漏扫描；检查 ServiceAccount/RBAC、超时、request/limit、重试上限和 MemoryPressure。用可控的过期夹具证明 7 天/20 次清理规则，且结果采集先于 PVC 删除。

Validation: `./scripts/ci/run-acceptance.sh --suite safety-and-retention` -> exit 0。

### Milestone 7: CI Exit Gate 报告可审计

统一入口串行执行所有 suite，生成按 CI-AC-001～011、CI-SEC、CI-NFR 和 CI-DEC 编号索引的 Markdown/机器可读摘要。报告包含执行时间、目标版本和 Git baseline，不包含 Secret 值。任何缺失断言都使总入口失败。

Validation: `./scripts/ci/run-acceptance.sh --suite exit-gate` -> exit 0；证据索引无 missing/unknown。

## Acceptance matrix

| Requirement | Test intent | Required evidence |
|---|---|---|
| CI-AC-001 | Java 成功路径和 BuildResult | Succeeded、full SHA、report、image URI/digest/platform、template provenance |
| CI-AC-002 | 测试失败短路 | Failed、测试报告、image Task 未执行 |
| CI-AC-003 | Git 错误分类 | invalid URL、unauthorized、missing revision 三种可区分结果 |
| CI-AC-004 | Registry 错误分类 | push Failed，digest 为空且无伪造成功 |
| CI-AC-005 | 三次可重复运行 | 相同 full SHA、三个独立 run ID、可追踪制品 |
| CI-AC-006 | ARM64 可运行 | manifest platform 与启动健康结果 |
| CI-AC-007 | 凭据不泄漏 | Secret scan 报告零命中 |
| CI-AC-008 | 模板复用 | 两项目、同一 template ref/version、无项目硬编码 |
| CI-AC-009 | 语言无关契约 | 通用参数 Schema 与 Java profile Schema 对比 |
| CI-AC-010 | BuildResult 归一化 | runtime OCI artifact、digest、platforms、可选 package artifact |
| CI-AC-011 | CI/发布解耦 | 无 ReleaseCandidate、GitOps mutation 或直接部署行为 |
| CI-SEC-001～006 | Secret、RBAC、非特权、无 Docker Socket | 资源静态检查与运行时 PodSpec |
| CI-NFR-001～007 | 资源、超时、重试、清理、关联 | 节点状态、资源配置、清理与日志关联报告 |
| CI-DEC runtime gates | 版本组合与 BuildKit 安全运行 | compatibility suite 报告 |

## Landmines

- 当前没有 Git baseline 或远程 fixture URL；在测试夹具不能被完整 SHA 引用前，不得声称可重复。Observed at planning time。
- 错误 Registry 凭据、Git Token 和测试用 Secret 也属于敏感数据；不能因为它们是“故意错误的”就写入仓库或报告。
- 删除测试会真实影响运行和 PVC；只能针对带验收专用 label 且由当前 suite 创建的资源，禁止 namespace 级批量删除。
- 通过镜像 tag 拉取不能证明不可变性，必须使用 Registry 返回的 digest。
- PipelineRun Succeeded 不能单独证明 BuildResult 正确；字段级断言是必需的。

## Scope

In scope:

- `scripts/ci/**` 中的验收、断言、脱敏和清理入口；
- `examples/java-*/**` 中的确定性测试夹具；
- `docs/runbooks/ci-acceptance*.md` 和不含秘密的证据索引；
- `pipelines/**`、`profiles/**`、`infra/**` 的只读验收；
- `plans/README.md` 状态更新。

Out of scope:

- 修改 Plan 001 的产品实现以绕过失败；缺陷应回交实现阶段；
- 性能压测、并发扩容和生产容量评估；
- Sonar、Webhook、Python、Java 控制面、前端和 CD 验收；
- 访问未授权外部系统或创建真实生产凭据。

## Commands

| Purpose | Command | Expected result |
|---|---|---|
| Suite discovery | `./scripts/ci/run-acceptance.sh --list` | exit 0，suite 清单完整 |
| Script syntax | `find scripts/ci -type f -name '*.sh' -exec bash -n {} +` | exit 0 |
| Compatibility (acceptance) | `./scripts/ci/run-acceptance.sh --suite compatibility` | exit 0 |
| Source and tests (acceptance) | `./scripts/ci/run-acceptance.sh --suite source-and-test` | exit 0 |
| Artifact (acceptance) | `./scripts/ci/run-acceptance.sh --suite artifact` | exit 0 |
| Contract (acceptance) | `./scripts/ci/run-acceptance.sh --suite contract` | exit 0 |
| Safety and retention (acceptance) | `./scripts/ci/run-acceptance.sh --suite safety-and-retention` | exit 0 |
| Full gate (acceptance) | `./scripts/ci/run-acceptance.sh --suite exit-gate` | exit 0，无 missing/unknown |

## Done criteria

- [ ] 所有列出的命令通过。
- [ ] Acceptance matrix 每一行都关联至少一个自动断言和一份脱敏证据。
- [ ] 成功与预期失败路径均可独立重跑，并有明确退出码。
- [ ] CI-AC-001 至 CI-AC-011 全部通过，无跳过项。
- [ ] ADR-001 六项运行时门槛全部通过并支持状态更新为 Validated。
- [ ] 测试未访问或删除非验收资源。
- [ ] 实现遵循 Decisions & tradeoffs 中的全部决定。
- [ ] 未修改 out-of-scope 文件。
- [ ] `plans/README.md` 状态已更新。

## STOP conditions

- Plan 001 尚未完成实现审查，或其 STOP condition 尚未关闭。
- Git baseline、测试夹具 revision 或目标组件版本不确定。
- 验收需要把真实 Secret 值写入参数、日志或证据。
- 清理目标无法通过验收专用 label 和当前运行身份精确限定。
- 任一验收命令在一次合理测试修复后仍连续失败两次；应回交实现诊断。
- 发现规范与实际行为矛盾，或必须放宽安全基线才能通过。

## Maintenance notes

- 新增 CI 功能必须先增加需求编号，再增加 Acceptance matrix 行和自动断言。
- 版本升级后至少重跑 compatibility、artifact、contract 和 exit-gate。
- 失败证据与成功证据同等重要；不能只保存绿色运行。
- 证据保留策略变化时同步更新 ADR、清理测试与运行手册。
