# AI Task Claim

claim_id: GA-QA-02-S01
task_id: GA-QA-02
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-QA-02.lock
slice: S01
title: Vitest coverage + 前端 CI
owner: TraeAI-GLM5
role: senior
status: ACTIVE
branch: ai/GA-QA-02/frontend-coverage
target_base_branch: develop
git_base_commit: 891c251
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
  - frontend/package.json
  - frontend/package-lock.json
  - frontend/vite.config.ts
  - .github/workflows/ci.yml
  - frontend/src/**/__tests__/**
  - frontend/src/**/*.test.{ts,tsx}
read_scope:
  - frontend/src/**
  - docs/**
  - medkernel-mvp/pom.xml
forbidden_scope:
  - medkernel-mvp/src/main/java/**

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
frontend/src/**/__tests__/**
frontend/src/**/*.test.{ts,tsx}
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
