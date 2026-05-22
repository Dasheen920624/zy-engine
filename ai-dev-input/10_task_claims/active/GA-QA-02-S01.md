# AI Task Claim

claim_id: GA-QA-02-S01
task_id: GA-QA-02
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-QA-02.lock
slice: S01
title: Vitest coverage + 前端 CI
owner: TraeAI-GLM5-1
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: 891c2510f88839f0bf4d096df27e2723e4e664e0
git_status_at_claim: clean
created_at: 2026-05-23T21:00+08:00
last_heartbeat: 2026-05-23T21:00+08:00
expected_finish: 2026-05-24T05:00+08:00
heartbeat_interval_minutes: 60
database_mode: none
oracle_available: false
local_db_verified: false
oracle_verification_required: false
review_required: true
review_id:
review_status: NOT_REQUESTED
reviewer:
open_findings: 0
quality_gate: BLOCKED_UNTIL_APPROVED
feature_acceptance_required: false
feature_acceptance_id:
write_scope:
read_scope:
forbidden_scope:

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/GA-QA-02.lock
```

## Write Scope

```text
frontend/package.json
frontend/package-lock.json
frontend/vite.config.ts
.github/workflows/ci.yml
ai-dev-input/10_task_claims/active/GA-QA-02-S01.md
ai-dev-input/10_task_claims/active_locks/GA-QA-02.lock
docs/engineering/02_任务台账.md
```

## Read Scope

```text
frontend/src/**
docs/**
medkernel-mvp/pom.xml
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/**
frontend/src/**（不修改业务代码，仅配置和 CI）
ai-dev-input/10_task_claims/active/GA-GOV-02-S01.md
ai-dev-input/10_task_claims/active/GA-QA-01-S01.md
ai-dev-input/10_task_claims/active/GA-DTO-01-S01.md
ai-dev-input/10_task_claims/active/NOTIFY-001-S01.md
```

## Dependencies

```text
无显式依赖
```

## Acceptance

```text
1. @vitest/coverage-v8 依赖安装
2. vite.config.ts 配置覆盖率目标和输出
3. package.json 添加 test:coverage 脚本
4. .github/workflows/ci.yml 添加 frontend-build-test job
5. 前端覆盖率目标 60%（行覆盖率）
6. CI 中覆盖率不足时构建失败
7. npm run test:coverage 本地可运行
```

## Verification

```text
cd frontend && npm run test:coverage
cd frontend && npm run lint && npm run typecheck && npm run build
```

## Self Check

```text
task_card_satisfied: pending
write_scope_matches_diff: pending
tests_updated: N/A
samples_or_api_examples_updated: N/A
docs_updated: pending
db_only_checked: N/A
oracle_dm_h2_schema_synced: N/A
production_development_schema_synced: N/A
table_and_column_comments_complete: N/A
required_code_comments_complete: pending
feature_acceptance_created: N/A
claim_status_synced: pending
security_privacy_checked: N/A
```

## Progress

```text
- [ ] 创建 claim + lock 并 push
- [ ] 安装 @vitest/coverage-v8
- [ ] 配置 vite.config.ts 覆盖率
- [ ] 添加 test:coverage 脚本
- [ ] 添加 frontend CI job
- [ ] 本地验证
- [ ] 更新台账
- [ ] commit + push
```

## Completion

```text
commit:
push:
tests:
review:
risks:
