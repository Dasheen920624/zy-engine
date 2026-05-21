# AI Task Claim

claim_id: PR-FINAL-06-S01
task_id: PR-FINAL-06
task_lock_path: ai-dev-input/10_task_claims/active_locks/PR-FINAL-06.lock
slice: S01
title: LoginPage 4 Tab 重写（国情合规 12 条）
owner: Codex-GPT5
role: senior
status: IN_PROGRESS
branch: codex/pr-final-06-login-tabs
target_base_branch: develop
git_base_commit: 67f271af50220778b9e572d2f6fd2a889bcfa1b3
git_status_at_claim: clean
created_at: 2026-05-21T22:21:25+08:00
last_heartbeat: 2026-05-21T22:21:25+08:00
expected_finish: 2026-05-23T22:21:25+08:00
heartbeat_interval_minutes: 60
database_mode: N/A for schema; frontend login page only
oracle_available: false
local_db_verified: N/A
oracle_verification_required: false
review_required: true
review_id: RV-PR-FINAL-06-S01-R01
review_status: PENDING
reviewer: pending
open_findings: pending
quality_gate: PENDING
feature_acceptance_required: true
feature_acceptance_id: FA-PR-FINAL-06-S01
write_scope: frontend/src/pages/auth/**, frontend/src/pages/Login.tsx, frontend/src/pages/SsoLogin.tsx, frontend/src/router/routes.tsx, frontend/src/styles/global.css, frontend/src/api/auth.ts, frontend/src/api/sso.ts, frontend/e2e/auth/**, docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md, ai-dev-input/10_task_claims/**, ai-dev-input/11_ai_reviews/**, ai-dev-input/13_feature_acceptance/**
read_scope: docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md, docs/v0.3-DEMO-REDESIGN.md, docs/PRODUCT_ARCHITECTURE_FINAL.md, frontend/src/**
forbidden_scope: backend DDL/schema; deploy/scripts; unrelated PR-FINAL pages

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/PR-FINAL-06.lock
```

## Acceptance

```text
1. /login is a single four-tab login surface: password, SMS, SSO, LDAP/domain.
2. /sso-login no longer duplicates a separate login page and routes into the unified surface.
3. Chinese compliance cues from docs/v0.3-DEMO-REDESIGN.md are visible without blocking the demo flow.
4. Existing account/password login still works against /api/auth/login.
5. SSO and LDAP flows keep their backend contracts and are represented as clear user actions.
6. Inline style count does not increase and frontend checks pass.
```

## Progress

```text
2026-05-21T22:21:25+08:00 Claimed PR-FINAL-06 from develop after confirming no remote PR-FINAL-06 branch.
```

## Completion

```text
commit: pending
push: pending
tests: pending
review: pending
risks: pending
```
