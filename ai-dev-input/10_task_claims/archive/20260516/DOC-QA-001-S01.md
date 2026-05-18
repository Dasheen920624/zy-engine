# AI Task Claim

claim_id: DOC-QA-001-S01
task_id: DOC-QA-001
slice: S01
title: 建立多 AI 开发质量门禁与评审整改机制
owner: AI-Codex-20260516-doc-governance-01
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
review_id: RV-DOC-QA-001-S01-R01
review_status: APPROVED
reviewer: AI-Codex-20260516-bootstrap-self-review
open_findings: 0
quality_gate: APPROVED_BOOTSTRAP

## Write Scope

```text
README.md
medkernel-mvp/README.md
medkernel-mvp/docs/AI任务认领与并行开发机制.md
medkernel-mvp/docs/AI开发质量门禁与评审整改机制.md
medkernel-mvp/docs/AI接手执行手册.md
medkernel-mvp/docs/产品化方案与AI开发编排.md
medkernel-mvp/docs/顶级多角色评审与AI并行开发总控.md
medkernel-mvp/docs/全功能蓝图与并行开发计划.md
ai-dev-input/README.md
ai-dev-input/09_ai_task_cards/ai_system_prompt.md
ai-dev-input/09_ai_task_cards/backend_prompt_template.md
ai-dev-input/09_ai_task_cards/task_card_template.md
ai-dev-input/10_task_claims/README.md
ai-dev-input/10_task_claims/task_claim_template.md
ai-dev-input/11_ai_reviews/**
```

## Read Scope

```text
现有 AI 接手、任务认领、总纲、总控、任务卡模板和输入包 README。
```

## Forbidden Scope

```text
Java 业务代码、DDL、脚本、样例数据、运行配置。
```

## Dependencies

```text
依赖已建立的任务认领机制和本地数据库/Oracle 开发约定。
```

## Acceptance

```text
1. 新增质量门禁总文档。
2. 新增 ai-dev-input/11_ai_reviews 目录和模板。
3. claim 模板增加 review 字段。
4. 接手手册、系统提示词、任务卡、后端提示词、总纲、蓝图、README 均要求评审通过后才能正式提交。
5. 明确认领状态同步 main 不等于业务代码自动进入主版本。
```

## Verification

```text
rg 检查质量门禁关键字段和入口引用。
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
review_id: RV-DOC-QA-001-S01-R01
review_file: ai-dev-input/11_ai_reviews/archive/20260516/RV-DOC-QA-001-S01-R01.md
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
已补齐质量门禁、评审目录、模板、执行入口和总纲引用。
```

## Handoff

```text
后续若有独立 Reviewer AI，可对本 bootstrap 机制再做一次复核并按新机制提出修订。
```

## Completion

```text
commit: pending
push: pending
tests: git diff --check passed
review: RV-DOC-QA-001-S01-R01
risks: 本轮为治理文档 bootstrap 自审，后续高风险代码任务必须独立评审。
```
