# AI Task Claim

claim_id: DOC-BIZ-001-S01
task_id: DOC-BIZ-001
slice: S01
title: 产品功能业务最终核查与 AI 开工清单
owner: AI-Codex-20260516-product-governance-01
role: Product/Governance AI
status: DONE
branch: main-coordination
created_at: 2026-05-16
last_heartbeat: 2026-05-16
expected_finish: 2026-05-16
database_mode: N/A
oracle_available: N/A
local_db_verified: N/A
oracle_verification_required: false
review_required: true
review_id: RV-DOC-BIZ-001-S01-R01
review_status: APPROVED
reviewer: AI-Codex-20260516-bootstrap-self-review
open_findings: 0
quality_gate: APPROVED_BOOTSTRAP

## Write Scope

```text
README.md
medkernel-mvp/README.md
medkernel-mvp/docs/产品功能业务核查与开工清单.md
medkernel-mvp/docs/AI接手执行手册.md
medkernel-mvp/docs/产品化方案与AI开发编排.md
medkernel-mvp/docs/顶级多角色评审与AI并行开发总控.md
ai-dev-input/README.md
ai-dev-input/09_ai_task_cards/ai_system_prompt.md
ai-dev-input/09_ai_task_cards/backend_prompt_template.md
ai-dev-input/09_ai_task_cards/task_card_template.md
ai-dev-input/10_task_claims/archive/20260516/DOC-BIZ-001-S01.md
ai-dev-input/11_ai_reviews/archive/20260516/RV-DOC-BIZ-001-S01-R01.md
```

## Read Scope

```text
产品总纲、全功能蓝图、顶级总控、前端配置平台规划、AI 接手和任务模板。
```

## Forbidden Scope

```text
Java 业务代码、DDL、运行脚本、样例数据。
```

## Dependencies

```text
依赖既有产品总纲、全功能蓝图、AI 自主开发、任务认领和质量门禁机制。
```

## Acceptance

```text
1. 新增产品功能业务核查与开工清单。
2. 明确产品边界、用户角色、业务闭环、当前能力缺口和首批客户验收故事线。
3. 补充 AI 开工后的业务优先级和新增任务建议。
4. 接手手册、系统提示词、任务卡、后端提示词、总纲、README 均要求任务说明业务角色和验收故事线。
```

## Verification

```text
rg 检查 产品功能业务核查、客户验收故事线、目标角色、业务闭环 等入口引用。
git diff --check。
文档变更，无需运行后端构建。
```

## Self Check

```text
task_card_satisfied: true
write_scope_matches_diff: true
tests_updated: N/A
samples_or_api_examples_updated: N/A
docs_updated: true
db_only_checked: N/A
oracle_dm_h2_schema_synced: N/A
security_privacy_checked: true
```

## Quality Review

```text
review_id: RV-DOC-BIZ-001-S01-R01
review_file: ai-dev-input/11_ai_reviews/archive/20260516/RV-DOC-BIZ-001-S01-R01.md
review_status: APPROVED
highest_severity: NONE
open_findings: 0
changes_requested: none
approved_by: AI-Codex-20260516-bootstrap-self-review
approved_at: 2026-05-16
submit_allowed: true
```

## Progress

```text
DONE: Added final product/business readiness checklist.
DONE: Linked business checklist from AI startup and task execution entry points.
DONE: Added role, scenario, business loop, UAT and AI work-priority guidance.
```

## Handoff

```text
后续 AI 开始任务时，必须从产品功能业务核查与开工清单确认目标角色、业务闭环和客户验收故事线。
```

## Completion

```text
commit: pending
push: pending
tests: git diff --check passed
review: RV-DOC-BIZ-001-S01-R01
risks: 本轮为业务治理文档 bootstrap 自审；后续业务功能仍需独立质量评审。
```
