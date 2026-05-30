# MedKernel AI 协作规则

## 语言要求

- 本仓库面向中文医疗、实施、研发和交付团队，所有新增或修改的文档必须使用简体中文书写。
- 代码标识符、接口路径、命令、配置键、数据库字段、标准英文缩写、第三方产品名和必要原文引用可以保留英文，但必须用中文解释其业务含义。
- `medkernel-backend` 引擎层（`com.medkernel.engine.**` 与 `com.medkernel.shared.**`）公共类、公共方法的 Javadoc，以及 Oracle/PostgreSQL/Kingbase 迁移脚本中新增表与枚举/状态/外键列的 `COMMENT ON`，必须使用简体中文；CI 由 `scripts/check-comment-zh.sh` 软门禁兜底。
- 不允许新增以英文为主的 README、设计文档、计划、OpenSpec、PR 说明、Issue 模板、运行手册或 AI 协作说明。
- 不保留旧版本历史归档、旧任务锁、旧协作模板或旧分支口径；项目按未上线的新项目方式运行。`openspec/archive/` 仅作已完成变更的审计追溯，不作为当前事实源。
- 详细规则见 [docs/DOCUMENTATION_LANGUAGE_POLICY.md](docs/DOCUMENTATION_LANGUAGE_POLICY.md)。

## 开发与合并要求

- 任何功能、修复或文档治理任务完成后，必须创建 PR，等待远端检查通过，再合并到 GitHub 远程 `main`。
- 禁止直接推送到远程 `main`；开发分支合入远程 `main` 的标准路径是：分支提交 → 推送 → PR → CI 通过 → 合并 PR → 确认 `origin/main` 包含合并提交。
- 远程长期分支只保留 `main`；禁止创建或保留 `develop`、`dev` 等第二主干分支。
- 分支默认使用 `codex/` 前缀，除非用户明确指定其他分支名。
- PR 描述必须使用中文，写清变更范围、验证结果、未完成事项和是否影响医疗安全、部署或数据迁移。

## 文档权威顺序

构建任一任务，按下序读（读最少、拿最全）：

1. [docs/CONSTITUTION.md](docs/CONSTITUTION.md) —— 核心（恒读，11 视角不变量）
2. `docs/cards/<域>/_brief.md` —— 所领卡所在域的域简报
3. `docs/cards/<域>/<TASK-ID>.md` —— 所领的施工卡

页面卡额外读 [docs/EXPERIENCE_CONTRACT.md](docs/EXPERIENCE_CONTRACT.md)（共享体验与组件契约）。

辅助（按需查，不通读）：
- 找卡：[docs/cards/_index.md](docs/cards/_index.md)（场景 S0–S40 → 卡）
- 验收方法论：[docs/audit/质量基线.md](docs/audit/质量基线.md)
- 名词：[docs/glossary.md](docs/glossary.md)
- 任务状态 / 派单：[docs/backlog.md](docs/backlog.md)

冲突裁决：核心 > 域简报 > 卡。卡与核心冲突 → 核心赢；卡之间本不应重叠，若冲突＝分区错误，修分区而非裁决。

> 迁移过渡期（P0–P7）：旧巨物（详规/落地规划/FOUNDATION/体验规范）在对应域搬迁完成前物理保留但**不再作为权威**；以本序为准。全部域搬迁完成后（P8）删除。
