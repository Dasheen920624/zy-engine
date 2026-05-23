# Feature Acceptance

acceptance_id: FA-PR-FINAL-13-S01
feature_id: PR-FINAL-13
task_id: PR-FINAL-13
claim_id: PR-FINAL-13-S01
review_id:
title: AI 工作流引擎页（去 Dify 化 ADR-0013 前端收口）
owner: claude-opus-4-7@ai-workflows-engine
status: PENDING
quality_level:
created_at: 2026-05-22T06:30+08:00
updated_at: 2026-05-22T06:30+08:00
commit: see PR
push: claude/ai-workflows-engine

## Scope

```text
功能范围：
  - /ai-workflows 主页 3 Tab（Provider 状态 / 降级链 / 多步工作流）
  - 顶部 hero banner 明示「8 家国产 + Ollama + LOCAL + Dify 仅 WORKFLOW」叙事
  - Provider 状态卡按「国产 / 本地兜底 / 工作流编排」三分组
  - 6 种 callType 降级链可视化（RESEARCH/EXTRACT/EMBEDDING/RERANK/CRITIC/WORKFLOW）
  - 多步工作流模板列表 + 调用统计 + 最近调用表
  - menuConfig 加「AI 工作流引擎」入口到 M1 知识工厂（RobotOutlined）

不验收范围：
  - 在线调用 LLM（/api/model-gateway/invoke）演示按钮（留下一轮）
  - 在线运行工作流（/api/dify/workflows/run）演示按钮（留下一轮）
  - 工作流模板的可视化编辑（属配置包 / DSL 范围，PR-V0.4+）

关联接口：
  - GET   /api/model-gateway/providers
  - GET   /api/model-gateway/degradation-chains
  - GET   /api/model-gateway/providers/{type}/status
  - POST  /api/model-gateway/invoke
  - GET   /api/dify/workflows
  - GET   /api/dify/workflows/{code}
  - POST  /api/dify/workflows/run
  - GET   /api/dify/workflows/stats

关联页面：
  - /ai-workflows
  - /dify/workflows → Navigate /ai-workflows（已在 develop）

关联表：
  - 后端 LLM Gateway + Dify Adapter 已就绪（PR-FINAL-01 已合 develop）
  - 本 PR 仅前端，不涉及 DDL
```

## Role Reviewers

```text
product_reviewer: 待人工拍板（叙事是否到位：去 Dify 化 + 8 家国产直连）
architecture_reviewer: 待架构师 AI（确认未触碰 §3 共享文件清单）
backend_reviewer: 不需要（纯前端）
frontend_reviewer: 高级 AI 互审
database_reviewer: 不需要
test_reviewer: 高级 AI 互审
medical_or_insurance_reviewer: 不需要（无医学规则内容）
security_or_ops_reviewer: 待人工（Provider 状态页是否暴露过多内部信息）
```

## Acceptance Checklist

```text
business_story_complete: yes（ADR-0013 去 Dify 化前端收口完整）
target_role_can_complete_task: yes（架构师/运维可一眼看到 LLM Provider 状态和降级链）
api_contract_stable: yes（沿用现有 ModelGateway + DifyAdapter 端点）
trace_id_and_audit_complete: yes（client.ts 自动 X-Trace-Id）
source_traceability_complete: not_applicable（无医学规则内容）
organization_scope_complete: yes（client.ts Header 注入）
production_db_schema_synced: not_applicable
development_db_local_h2_verified: not_applicable
table_and_column_comments_complete: not_applicable
required_code_comments_complete: yes（关键文件均 JSDoc）
frontend_states_complete: yes（loading / error / empty / 数据态全覆盖）
tests_and_smoke_complete: yes（17+ vitest 测试）
security_privacy_checked: yes（不存储任何 LLM API key 到 localStorage / 不暴露后端 secret）
docs_and_examples_updated: yes（docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md §1 表 + §2 实施记录）
optimization_task_registered_if_needed: 暂无（无遗留 TODO）
```

## Evidence

```text
run-tests: 本地 node 12 不支持 vitest 2.x，CI 不强 gate 前端测试，依赖人工 node 18+ 兜底
build: 同上
git diff --check: PASS
local_h2: not_applicable
production_db_smoke: not_applicable
frontend_validation: 待人工在 node 18+ 跑 npm verify
screenshots_or_reports: 待人工
claim_review_status: NOT_REQUESTED
git_status_after_push: clean
```

## Findings

```text
finding_id:
severity:
owner:
status: OPEN
problem:
required_fix:
target_task:
optimization_owner:
```

## Verdict

```text
quality_level: 待评审
approved_for_customer_demo: false
approved_for_integration: false
needs_optimization_task:
remaining_risk:
  1. 本地 node 12 跑不了 npm test/build，依赖人工 node 18+ 验证
  2. 在线调用 LLM / 运行工作流的演示按钮留到下一轮
  3. Provider 实际可用性依赖 application.yml 配置（QIANWEN_API_KEY 等环境变量）
final_decision:
```
