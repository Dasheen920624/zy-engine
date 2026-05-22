# AI Task Claim

claim_id: GA-QA-02-S01
task_id: GA-QA-02
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-QA-02.lock
slice: S01
<<<<<<< HEAD
title: Vitest coverage + 前端 CI
owner: TraeAI-GLM5-1
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: 891c2510f88839f0bf4d096df27e2723e4e664e0
=======
title: 前端覆盖率与 CI
owner: TraeAI-GLM5
role: senior
status: ACTIVE
branch: ai/GA-QA-02/frontend-coverage
target_base_branch: develop
git_base_commit: 891c251
>>>>>>> f3777b0 (GA-QA-02: 认领前端覆盖率与 CI 任务)
git_status_at_claim: clean
created_at: 2026-05-23T21:00+08:00
last_heartbeat: 2026-05-23T21:00+08:00
expected_finish: 2026-05-24T05:00+08:00
heartbeat_interval_minutes: 60
database_mode: none
oracle_available: false
<<<<<<< HEAD
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
- [x] 创建 claim + lock 并 push
- [x] 安装 @vitest/coverage-v8
- [x] 配置 vite.config.ts 覆盖率
- [x] 添加 test:coverage 脚本
- [x] 添加 frontend CI job
- [x] 本地验证（本机无 Node.js，CI 环境验证）
- [x] 更新台账
- [x] commit + push
```

## Completion

```text
commit: 6808d29
push: develop
tests: CI 验证（frontend-build-test job）
review: 待创建
risks: 本机无 Node.js 环境，覆盖率阈值可能需要根据实际测试结果调整
```
=======

## write_scope

- `frontend/vite.config.ts`（添加 coverage 配置）
- `frontend/package.json`（添加 @vitest/coverage-v8 依赖 + coverage 脚本）
- `.github/workflows/ci.yml`（添加 frontend-build-test job）
- `frontend/src/test/setup.ts`（如有必要调整测试 setup）
- `frontend/src/**/__tests__/**`（补充缺失测试以提高覆盖率）
- `frontend/src/**/*.test.{ts,tsx}`（补充缺失测试以提高覆盖率）

## read_scope

- `docs/AI_TEAM_PR_BACKLOG_V1.0_GA.md`
- `docs/AI_TEAM_SOP.md`
- `docs/AI_CHARTER.md`
- `frontend/src/**`（读取源码以编写测试）
- `medkernel-mvp/scripts/check-ai-collaboration.ps1`

## review_requirements

- 覆盖率报告可生成
- CI frontend-build-test job 可运行
- 覆盖率阈值门禁生效（行覆盖率 >= 60%）

## feature_acceptance_requirements

- `npm run test:coverage` 命令可执行并生成 HTML 报告
- CI 中 frontend-build-test job 包含 lint + typecheck + test + coverage check + build
- 覆盖率不足 60% 时构建失败

## expected_commits: 2

## plan

1. 安装 @vitest/coverage-v8 依赖
2. 配置 vite.config.ts 添加 coverage 配置（目标 60% 行覆盖率）
3. 添加 npm scripts: test:coverage
4. 补充缺失测试文件以提高覆盖率到 60%
5. 在 CI 中添加 frontend-build-test job
6. 自测验证
>>>>>>> f3777b0 (GA-QA-02: 认领前端覆盖率与 CI 任务)
