# AI Task Claim
claim_id: AI-GOV-002-S01
task_id: AI-GOV-002
task_lock_path: ai-dev-input/10_task_claims/active_locks/AI-GOV-002.lock
title: AI 安全红队和幻觉防护
owner: TraeAI-Main
status: ACTIVE
claimed_at: 2026-05-20
last_heartbeat: 2026-05-22T13:37+08:00
git_base_commit: unknown_preexisting_claim
git_status_at_claim: unknown_preexisting_claim
review_required: true
feature_acceptance_required: true
dependencies: AI-GOV-001 (DONE), CDSS-004 (DONE)
write_scope: quality/**, llm/**, cdss/**
acceptance_criteria:
  - 红队测试场景定义和管理
  - 幻觉检测和标记
  - 幻觉防护策略（阻断/降级/人工审核）
  - 红队测试执行和结果记录
  - DDL 4 方言
