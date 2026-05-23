# AI Autonomous Run Log

run_id:
owner:
status: ACTIVE
started_at:
ended_at:
mode: AUTONOMOUS
user_instruction:
database_mode:
oracle_available:
local_db_verified:

## Startup Checks

```text
git_status:
latest_commit:
active_claims_checked:
pending_reviews_checked:
db_env_checked:
```

## Task Selection

```text
selected_task_id:
selected_claim_id:
selection_reason:
priority_source:
conflict_check:
review_backlog_check:
```

## Claims

```text
current_claim:
completed_claims:
blocked_claims:
handoff_claims:
```

## Reviews

```text
current_review:
approved_reviews:
changes_requested_reviews:
pending_reviews:
```

## Work Summary

```text
changed_files:
implemented:
docs_updated:
samples_updated:
ddl_updated:
tests_updated:
```

## Verification

```text
run-tests:
build:
git diff --check:
local h2 smoke:
oracle ddl:
oracle smoke:
other:
```

## Stop Conditions Checked

```text
needs_user_decision:
needs_real_credentials:
production_or_destructive_risk:
medical_or_policy_risk:
claim_conflict:
review_blocker:
quota_low:
```

## Handoff

```text
current_state:
next_action:
risks:
do_not_touch:
notes_for_next_ai:
```
