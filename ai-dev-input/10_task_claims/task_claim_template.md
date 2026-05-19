# AI Task Claim

claim_id:
task_id:
task_lock_path:
slice:
title:
owner:
role:
status: ACTIVE
branch:
target_base_branch: develop
git_base_commit:
git_status_at_claim:
created_at:
last_heartbeat:
expected_finish:
heartbeat_interval_minutes: 60
database_mode:
oracle_available:
local_db_verified:
oracle_verification_required:
review_required: true
review_id:
review_status: NOT_REQUESTED
reviewer:
open_findings:
quality_gate: BLOCKED_UNTIL_APPROVED
feature_acceptance_required:
feature_acceptance_id:
write_scope:
read_scope:
forbidden_scope:

## Task Lock

必须与本 claim 同一提交创建同任务唯一锁，并推送到 `develop` 后才算认领成功：

```text
ai-dev-input/10_task_claims/active_locks/<task_id>.lock
```

锁文件内容模板：

```text
task_id:
claim_id:
owner:
branch:
git_base_commit:
created_at:
last_heartbeat:
```

## Write Scope

```text

```

## Read Scope

```text

```

## Forbidden Scope

```text

```

## Dependencies

```text

```

## Acceptance

```text

```

## Status Sync Checkpoints

```text
claim_pushed_before_code:
task_ledger_in_progress:
git_status_checked_before_edit:
last_heartbeat_pushed:
review_status_synced:
task_ledger_done_synced:
commit_hash_recorded:
post_push_git_status_clean:
task_lock_removed_on_archive:
```

## Verification

```text

```

## Self Check

```text
task_card_satisfied:
write_scope_matches_diff:
tests_updated:
samples_or_api_examples_updated:
docs_updated:
db_only_checked:
oracle_dm_h2_schema_synced:
production_development_schema_synced:
table_and_column_comments_complete:
required_code_comments_complete:
feature_acceptance_created:
claim_status_synced:
security_privacy_checked:
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
