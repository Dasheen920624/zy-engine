# AI Task Claim

claim_id: DOC-AUTO-001-S01
task_id: DOC-AUTO-001
slice: S01
title: 建立 AI 自主开发运行守则和运行记录机制
owner: AI-Codex-20260516-autonomy-governance-01
role: Documentation/Governance AI
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
review_id: RV-DOC-AUTO-001-S01-R01
review_status: APPROVED
reviewer: AI-Codex-20260516-bootstrap-self-review
open_findings: 0
quality_gate: APPROVED_BOOTSTRAP

## Write Scope

```text
README.md
medkernel-mvp/README.md
medkernel-mvp/docs/AI自主开发运行守则.md
medkernel-mvp/docs/AI接手执行手册.md
medkernel-mvp/docs/产品化方案与AI开发编排.md
medkernel-mvp/docs/顶级多角色评审与AI并行开发总控.md
ai-dev-input/README.md
ai-dev-input/09_ai_task_cards/ai_system_prompt.md
ai-dev-input/09_ai_task_cards/backend_prompt_template.md
ai-dev-input/09_ai_task_cards/task_card_template.md
ai-dev-input/10_task_claims/archive/20260516/DOC-AUTO-001-S01.md
ai-dev-input/11_ai_reviews/archive/20260516/RV-DOC-AUTO-001-S01-R01.md
ai-dev-input/12_autonomous_runs/**
```

## Read Scope

```text
现有 AI 接手、任务认领、质量门禁、产品总纲、顶级总控、任务卡和系统提示词。
```

## Forbidden Scope

```text
Java 业务代码、DDL、运行脚本、样例数据。
```

## Dependencies

```text
依赖既有任务认领和质量门禁机制。
```

## Acceptance

```text
1. 新增 AI 自主开发运行守则。
2. 新增 ai-dev-input/12_autonomous_runs 目录和模板。
3. 接手手册、系统提示词、任务卡、后端提示词、总纲、总控、README 均接入自主运行规则。
4. 明确自主选题优先级、停机条件、额度不足交接、数据安全红线和 run log 交接格式。
```

## Verification

```text
rg 检查 AI自主开发、12_autonomous_runs、run_id、next_action 等入口引用。
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
review_id: RV-DOC-AUTO-001-S01-R01
review_file: ai-dev-input/11_ai_reviews/archive/20260516/RV-DOC-AUTO-001-S01-R01.md
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
DONE: Added autonomous development runbook.
DONE: Added autonomous run log directory and template.
DONE: Linked autonomy rules from all AI startup and task execution entry points.
```

## Handoff

```text
后续 AI 开始自主任务时，先复制 run_log_template.md 到 active/<run_id>.md，再按守则选择任务。
```

## Completion

```text
commit: pending
push: pending
tests: git diff --check passed
review: RV-DOC-AUTO-001-S01-R01
run: RUN-DOC-AUTO-001-20260516
risks: 本轮为治理文档 bootstrap 自审，后续高风险业务任务仍必须独立评审。
```
