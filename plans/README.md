# CI Foundation Plans

| Plan | Outcome | Depends on | Status | Execution order |
|---|---|---|---|---|
| [001](001-ci-foundation-implementation.md) | K3s 上可复用的 Java CI 基础闭环 | none | review | 1 |
| [002](002-ci-foundation-acceptance.md) | CI-AC-001～CI-AC-011 的可重复验收证据 | Plan 001 | review | 2（Plan 001 各里程碑完成时同步准备，完整验收最后执行） |

这两个计划串行交付，不构成可并行执行组。Plan 002 会验证 Plan 001 产生的同一套基础设施、模板和示例仓库，强行并行会造成共享集群状态与测试夹具冲突。

执行前置条件：

- `docs/specs/002-ci-foundation.md` 已从 Draft 变为 Approved；
- 工作目录已纳入 Git 版本控制；
- 用户已授权进入实施阶段；
- 不存在正在修改同一 K3s 虚拟机或同一 Pipeline 模板目录的其他任务。
