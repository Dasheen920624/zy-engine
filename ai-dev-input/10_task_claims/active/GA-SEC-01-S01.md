# AI Task Claim

claim_id: GA-SEC-01-S01
task_id: GA-SEC-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-SEC-01.lock
slice: S01
title: 等保 2.0 三级控制点自查闭环
owner: TraeAI-GLM5
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: bd9fc64
git_status_at_claim: clean
created_at: 2026-05-23T23:30+08:00
last_heartbeat: 2026-05-23T23:30+08:00
expected_finish: 2026-05-24T11:30+08:00
heartbeat_interval_minutes: 60
database_mode: local
oracle_available: false
local_db_verified: true
oracle_verification_required: false
review_required: true

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/GA-SEC-01.lock
```

## Write Scope

```text
medkernel-mvp/src/main/java/com/medkernel/security/**
docs/engineering/COMP-001_合规基线与证据包.md
ai-dev-input/10_task_claims/active/GA-SEC-01-S01.md
ai-dev-input/10_task_claims/active_locks/GA-SEC-01.lock
docs/engineering/02_任务台账.md
```

## Read Scope

```text
medkernel-mvp/src/main/java/com/medkernel/**
docs/AI_TEAM_PR_BACKLOG_V1.0_GA.md
```

## Forbidden Scope

```text
frontend/src/**
medkernel-mvp/pom.xml
medkernel-mvp/src/main/resources/application.yml
medkernel-mvp/src/main/java/com/medkernel/persistence/**
```

## Acceptance

```text
1. 等保 2.0 三级控制点自查表完成
2. 安全控制点对应代码实现
3. COMP-001 合规基线与证据包文档齐备
4. 后端编译通过
```

## Verification

```text
mvn compile -pl medkernel-mvp -q
```

## Progress

```text
- [ ] 创建 claim + lock 并 push
- [ ] 分析等保 2.0 三级控制点要求
- [ ] 实现安全控制点代码
- [ ] 编写 COMP-001 合规基线与证据包
- [ ] 后端编译验证
- [ ] 更新台账
- [ ] commit + push
```
