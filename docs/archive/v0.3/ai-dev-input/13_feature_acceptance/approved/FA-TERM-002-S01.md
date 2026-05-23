# Feature Acceptance

acceptance_id: FA-TERM-002-S01
feature_id: TERM-002
task_id: TERM-002
claim_id: TERM-002-S01
review_id: REV-TERM-002-S01
title: 未映射治理队列 - PENDING_MAPPING 持久化 + 查询 + 审批
owner: CodeBuddy-20260517-term-governance-01
status: APPROVED
quality_level: SILVER
created_at: 2026-05-17T22:00:00+08:00
updated_at: 2026-05-18T10:40:00+08:00
commit: closeout commit reported in final response
push: closeout push pending

## Scope

```text
功能范围：
  - 标准化未命中时自动进入 PENDING_MAPPING 治理队列
  - 治理队列持久化（TM_UNMAPPED_QUEUE 表）
  - 查询治理队列 API（GET /api/terminology/pending）
  - 审批映射 API（POST /api/terminology/pending/{queueId}/approve）
  - 驳回映射 API（POST /api/terminology/pending/{queueId}/reject）
  - 审批后自动写入映射缓存
  - DDL 同步 Oracle/DM/PG-Kingbase/LOCAL_H2_FILE
不验收范围：
  - 前端治理队列界面（前端导航已预留但未启用）
  - 自动映射建议算法（当前仅记录 proposed_* 字段）
  - Oracle 生产库实际验证（需内网环境）
关联接口：
  - POST /api/terminology/normalize
  - GET /api/terminology/pending
  - POST /api/terminology/pending/{queueId}/approve
  - POST /api/terminology/pending/{queueId}/reject
关联页面：无
关联表：tm_unmapped_queue
```

## Role Reviewers

```text
product_reviewer:
medical_reviewer:
compliance_reviewer:
```

## Acceptance Criteria

```text
1. 未映射术语标准化时 governance_status=PENDING_MAPPING，返回 queue_id
2. 治理队列可按 governance_status/source_system/concept_type 过滤查询
3. 审批后映射状态改为 APPROVED，映射缓存同步更新
4. 审批后再次标准化同一术语应命中
5. 驳回后映射状态改为 REJECTED
6. 重复出现的未映射术语 occurrence_count 递增
7. LOCAL_H2_FILE 模式下治理队列可正常读写
8. Oracle/DM/PG DDL 与 H2 DDL 结构一致
```

## Test Evidence

```text
- 契约测试：terminologyUnmappedEntersGovernanceQueue
- 契约测试：terminologyApprovePendingMapping
- 契约测试：terminologyRejectPendingMapping
- 契约测试：terminologyPendingListFilters
- DDL 同步：8 个 DDL 文件均包含 tm_unmapped_queue 建表语句
```

## Risks

```text
1. Oracle 生产库未实际验证（需内网环境执行 run-oracle-ddl.ps1）
2. 前端治理队列页面尚未实现，目前只能通过 API 操作
3. 自动映射建议（proposed_standard_code）暂未实现智能推荐
```

## Closeout Evidence

```text
closeout_owner: Codex-closeout-20260518
reason: 原 active claim 无 AI 继续工作，用户确认可处理；REVIEW-FIX-002 已修复原 review findings
run-tests.ps1: PASS
build.ps1: PASS
git diff --check: PASS
quality_level: SILVER（API/后端/DDL 已验收；Oracle 生产库和前端页面仍为明确非本任务范围/待后续）
```
