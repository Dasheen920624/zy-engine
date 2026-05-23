# AI Task Claim

claim_id: GA-DOC-03-S01
task_id: GA-DOC-03
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-DOC-03.lock
slice: S01
title: 医院 IT、医生、实施工程师培训材料齐备
owner: TraeAI-1
role: senior
status: ACTIVE
branch: ai/GA-DOC-03/training-materials
target_base_branch: develop
git_base_commit: b74f78c
git_status_at_claim: clean
created_at: 2026-05-23T23:00+08:00
last_heartbeat: 2026-05-23T23:00+08:00
expected_finish: 2026-05-24T12:00+08:00
write_scope:
  - docs/training/**
read_scope:
  - docs/manuals/**
  - docs/ops/**
  - docs/user-guide/**
forbidden_scope:
  - medkernel-mvp/src/main/java/**
  - frontend/src/**

## Acceptance

```text
1. docs/training/it-staff-training.md — 医院信息科IT人员培训材料
2. docs/training/clinical-user-training.md — 临床医生培训材料
3. docs/training/implementation-engineer-training.md — 实施工程师培训材料
4. 所有培训材料面向不同角色，可直接交付
```
