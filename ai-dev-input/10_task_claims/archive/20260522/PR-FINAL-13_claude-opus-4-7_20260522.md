# AI Task Claim

claim_id: PR-FINAL-13-S01
task_id: PR-FINAL-13
task_lock_path: ai-dev-input/10_task_claims/active_locks/PR-FINAL-13.lock
slice: S01
title: AI 工作流引擎页（去 Dify 化 ADR-0013 前端收口）
owner: claude-opus-4-7@ai-workflows-engine
role: senior
status: ACTIVE
target_base_branch: develop
branch: claude/ai-workflows-engine
git_base_commit: 7b41fe6
git_status_at_claim: clean
created_at: 2026-05-22T06:30+08:00
last_heartbeat: 2026-05-22T06:30+08:00
expected_finish: 2026-05-22T10:00+08:00
heartbeat_interval_minutes: 60
database_mode: not_required
oracle_available: false
local_db_verified: not_required
oracle_verification_required: false
review_required: true
review_id:
review_status: NOT_REQUESTED
reviewer:
open_findings: 0
quality_gate: BLOCKED_UNTIL_APPROVED
feature_acceptance_required: true
feature_acceptance_id:
write_scope: see "Write Scope" section
read_scope: see "Read Scope" section
forbidden_scope: see "Forbidden Scope" section

## Task Lock

- 锁文件：`ai-dev-input/10_task_claims/active_locks/PR-FINAL-13.lock`

## 任务背景

领单卡 PR-FINAL-13（阶段 2 占位入口实装第 6 个）：把 `/ai-workflows` PlaceholderPage 换成真实组件，完成 ADR-0013「去 Dify 化」策略的前端收口。

ADR-0013 核心：
- 国产大模型（QIANWEN/DEEPSEEK/Kimi/智谱/豆包/Yi/百川/阶跃）通过 OpenAI 兼容协议直连，不依赖 Dify
- Ollama 本地兜底（医院内网部署可选）
- LOCAL 规则降级（永远可用）
- **仅 WORKFLOW 调用类型走 Dify**（复杂多步流程场景）

页面主流叙事：「8 家国产大模型直连 + Ollama 本地 + LOCAL 规则兜底」。Dify 仅作为 Provider 状态卡中的一项可见，不作为主品牌。

## Write Scope

```text
frontend/src/api/aiWorkflows.ts                                # 新建：封装 /api/model-gateway/* + /api/dify/workflows/*
frontend/src/pages/AiWorkflows/**                              # 新建整包：4 个核心组件
frontend/src/router/menuConfig.tsx                              # 加 ai-workflows 回 M1 知识工厂（RobotOutlined）
frontend/src/router/routes.tsx                                  # /ai-workflows 改真实组件
docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md                          # §1 表 + §2 实施记录
ai-dev-input/10_task_claims/active/PR-FINAL-13_*.md            # 本认领卡
ai-dev-input/10_task_claims/active_locks/PR-FINAL-13.lock      # 任务锁
ai-dev-input/13_feature_acceptance/pending/FA-PR-FINAL-13-S01.md  # acceptance
```

## Read Scope

```text
medkernel-mvp/src/main/java/com/medkernel/llm/**               # 只读：参考 ModelGateway 契约
medkernel-mvp/src/main/java/com/medkernel/dify/workflow/**     # 只读：参考 DifyAdapterController 契约
frontend/src/api/types.ts                                       # 只读：复用 OrgContext / ProviderStatus 等
frontend/src/components/**                                      # 只读：复用 SourceInfo / StatusBadge / TracedCard
frontend/src/api/system.ts ProvidersStatus.tsx                  # 只读：参考现有 Provider 状态页风格
frontend/src/pages/Rule/styles.module.css                       # 只读：参考 PR-FINAL-11 风格基线
```

## Forbidden Scope

```text
frontend/src/api/types.ts                                       # 架构师专属
frontend/src/styles/tokens*.{css,ts}                            # 架构师专属
frontend/src/theme/**                                           # 架构师专属
frontend/src/App.tsx frontend/src/main.tsx                      # 架构师专属
frontend/eslint-rules/** frontend/eslint.config.js              # 架构师专属
medkernel-mvp/**                                                # 本 PR 纯前端
docs/01_产品事实源.md … 05_AI实施手册.md                       # 金本位 V2 5 份
```

## Dependencies

```text
PR-FINAL-01 ✅ DONE   LLM Gateway 迁包 + 收敛（develop f2a1716 + c952726）
后端：ModelGatewayController + DifyAdapterController 已就绪（main 60c1f02）
```

## Acceptance

```text
1. /ai-workflows 路由可访问，渲染 3 Tab 主页：
   Tab 1 Provider 状态：8 家国产大模型 + Dify + LOCAL，每个卡含 ready/status/降级原因
   Tab 2 降级链：6 种 callType（RESEARCH/EXTRACT/EMBEDDING/RERANK/CRITIC/WORKFLOW）的链路可视化
   Tab 3 工作流模板：DifyAdapterController 提供的模板列表 + 调用统计
2. 整页面**不出现 "Dify" 作为主品牌**（仅在 Provider 状态卡和 WORKFLOW 降级链尾部出现）
3. 国情合规叙事：顶栏明示「8 家国产大模型直连 + Ollama 本地 + LOCAL 规则兜底」
4. 菜单：M1 知识工厂分组新增「AI 工作流引擎」入口（RobotOutlined 图标）
5. 路由：/ai-workflows 改真实组件；/dify/workflows 保留 Navigate 重定向
6. UI：100% CSS Modules + var(--mk-*) token，0 新增 inline style
7. 测试：每个核心组件 ≥1 render 测试 + 1 交互测试
8. 验证：CI guard-rules + backend-build-test PASS；前端 verify 由开发者本地兜底
```

## Status Sync Checkpoints

```text
claim_pushed_before_code: pending
git_status_checked_before_edit: yes
last_heartbeat_pushed: 2026-05-22T06:30+08:00
review_status_synced: pending
commit_hash_recorded: pending
post_push_git_status_clean: pending
task_lock_removed_on_archive: pending
```

## Verification

```text
[本地]
./scripts/check-inline-style-count.ps1   # 不上涨
[CI]
backend-build-test + guard-rules（纯前端 PR，仍需 guard-rules PASS）
```

## Self Check

```text
task_card_satisfied: pending
write_scope_matches_diff: pending
tests_updated: pending
docs_updated: docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md §1 表 + §2 实施记录
required_code_comments_complete: pending
feature_acceptance_created: pending
security_privacy_checked: yes（不存储任何 LLM API key 到 localStorage；Provider 配置由后端管理）
```

## Quality Review

```text
review_status: NOT_REQUESTED
submit_allowed: false
```

## Progress

```text
[2026-05-22T06:30] 认领 + 锁定 PR-FINAL-13
```

## Completion

```text
commit:
push:
tests:
review:
risks: 单 PR diff 预计 ~1800 行（4 个核心组件 + helpers + 测试）。
  与 PR-FINAL-11 规则模块同等复杂度，沿用相同质量标准与 CSS Modules 风格。
```
