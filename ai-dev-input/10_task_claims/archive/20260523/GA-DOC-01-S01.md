# AI Task Claim

claim_id: GA-DOC-01-S01
task_id: GA-DOC-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-DOC-01.lock
slice: S01
title: 4 治理模块用户手册可直接给医院信息科
owner: TraeAI-1
role: senior
status: ACTIVE
branch: ai/GA-DOC-01/user-manuals
target_base_branch: develop
git_base_commit: d37238b
git_status_at_claim: clean
created_at: 2026-05-23T22:30+08:00
last_heartbeat: 2026-05-23T22:30+08:00
expected_finish: 2026-05-24T12:00+08:00
write_scope:
  - docs/manuals/**
read_scope:
  - medkernel-mvp/src/main/java/com/medkernel/**
  - frontend/src/pages/**
  - docs/04_页面规格书.md
forbidden_scope:
  - medkernel-mvp/src/main/java/**
  - frontend/src/**

## Acceptance

```text
1. docs/manuals/rule-management.md — 规则治理模块用户手册
2. docs/manuals/pathway-management.md — 路径治理模块用户手册
3. docs/manuals/knowledge-management.md — 知识治理模块用户手册
4. docs/manuals/quality-management.md — 质控治理模块用户手册
5. 所有手册面向医院信息科用户，可直接交付
```
