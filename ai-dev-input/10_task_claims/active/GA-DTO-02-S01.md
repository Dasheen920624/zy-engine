# AI Task Claim

claim_id: GA-DTO-02-S01
task_id: GA-DTO-02
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-DTO-02.lock
slice: S01
title: Knowledge / AI review Controller DTO 化
owner: TraeAI-1
role: senior
status: ACTIVE
branch: ai/GA-DTO-02/knowledge-dto
target_base_branch: develop
git_base_commit: 891c251
git_status_at_claim: clean
created_at: 2026-05-23T21:00+08:00
last_heartbeat: 2026-05-23T21:00+08:00
expected_finish: 2026-05-24T09:00+08:00
heartbeat_interval_minutes: 60
database_mode: local
oracle_available: false
local_db_verified: true
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
  - medkernel-mvp/src/main/java/com/medkernel/knowledge/**Controller.java
  - medkernel-mvp/src/main/java/com/medkernel/knowledge/dto/*.java (新建)
  - medkernel-mvp/src/test/java/com/medkernel/knowledge/** (测试更新)
read_scope:
  - medkernel-mvp/src/main/java/com/medkernel/knowledge/**Service.java
  - medkernel-mvp/src/main/java/com/medkernel/knowledge/*.java (实体)
  - medkernel-mvp/src/main/java/com/medkernel/common/**
  - medkernel-mvp/src/main/java/com/medkernel/organization/**
forbidden_scope:
  - frontend/**
  - medkernel-mvp/pom.xml
  - medkernel-mvp/src/main/resources/application.yml
  - docs/**

## Task Lock

必须与本 claim 同一提交创建同任务唯一锁，并推送到 develop 后才算认领成功：

```text
ai-dev-input/10_task_claims/active_locks/GA-DTO-02.lock
```

## Write Scope

```text
medkernel-mvp/src/main/java/com/medkernel/knowledge/**Controller.java
medkernel-mvp/src/main/java/com/medkernel/knowledge/dto/*.java (新建)
medkernel-mvp/src/test/java/com/medkernel/knowledge/** (测试更新)
```

## Read Scope

```text
medkernel-mvp/src/main/java/com/medkernel/knowledge/**Service.java
medkernel-mvp/src/main/java/com/medkernel/knowledge/*.java (实体)
medkernel-mvp/src/main/java/com/medkernel/common/**
medkernel-mvp/src/main/java/com/medkernel/organization/**
```

## Forbidden Scope

```text
frontend/**
medkernel-mvp/pom.xml
medkernel-mvp/src/main/resources/application.yml
docs/**
```

## Dependencies

```text
无外部依赖。spring-boot-starter-validation 已在 pom.xml 中。
GlobalExceptionHandler 已处理 MethodArgumentNotValidException。
```

## Acceptance

```text
1. 所有 knowledge 包下的 Controller 不再使用 Map<String, Object> 作为 @RequestBody 参数
2. 所有 @RequestBody 参数使用 DTO + @Valid
3. DTO 包含必要的 Bean Validation 注解（@NotBlank, @NotNull, @Pattern 等）
4. 原有手动验证逻辑由 DTO 验证替代或保留为补充
5. 编译通过：mvn -q compile
6. 测试通过：mvn test
7. 无新增 raw Map 入参
```

## Status Sync Checkpoints

```text
claim_pushed_before_code: true
task_ledger_in_progress: pending
git_status_checked_before_edit: true
last_heartbeat_pushed: pending
review_status_synced: pending
task_ledger_done_synced: pending
commit_hash_recorded: pending
post_push_git_status_clean: pending
task_lock_removed_on_archive: pending
```

## Verification

```text
grep -r "Map<String, Object> request" medkernel-mvp/src/main/java/com/medkernel/knowledge/*Controller.java → 0 匹配
grep -r "@Valid" medkernel-mvp/src/main/java/com/medkernel/knowledge/*Controller.java → ≥1 匹配
mvn -q compile → exit 0
mvn test → 0 failures
```

## Self Check

```text
task_card_satisfied: true
write_scope_matches_diff: pending
tests_updated: pending
samples_or_api_examples_updated: N/A
docs_updated: N/A
db_only_checked: N/A
oracle_dm_h2_schema_synced: N/A
production_development_schema_synced: N/A
table_and_column_comments_complete: N/A
required_code_comments_complete: pending
feature_acceptance_created: N/A
claim_status_synced: pending
security_privacy_checked: true
```

## Quality Review

```text
review_id:
review_file:
review_status:
highest_severity:
open_findings:
changes_requested:
approved_by:
approved_at:
submit_allowed:
```

## Progress

```text
- 读取 6 个 Controller 源码，识别 18 个 raw Map 入参端点
- 设计 16 个 Request DTO 类
- 开始实现
```

## Handoff

```text

```

## Completion

```text
commit:
push:
tests:
review:
risks:
```
