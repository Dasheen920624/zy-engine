# AI Task Claim

claim_id: PR-FINAL-11-S01
task_id: PR-FINAL-11
status: COMPLETED
task_lock_path: ai-dev-input/10_task_claims/active_locks/PR-FINAL-11.lock
slice: S01
title: 规则库 + DSL 编辑器（完整版：列表/详情/编辑器/试运行）
owner: claude-opus-4-7@angry-rhodes-1b24a7
role: senior
target_base_branch: develop
branch: claude/angry-rhodes-1b24a7
git_base_commit: 67f271a
git_status_at_claim: clean
created_at: 2026-05-21T22:00+08:00
last_heartbeat: 2026-05-21T22:00+08:00
expected_finish: 2026-05-22T02:00+08:00
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

- 锁文件：`ai-dev-input/10_task_claims/active_locks/PR-FINAL-11.lock`
- 同 commit 创建，push 后才算认领成功

## Write Scope

```text
frontend/src/pages/Rule/**                                # 新建整包
frontend/src/api/rule.ts                                  # 新建：RuleController + RuleEngineController API client
frontend/src/router/menuConfig.tsx                        # 仅把 rule-definitions 入口从隐藏注释加回 items 数组（领单卡 §2 已授权）
frontend/src/router/routes.tsx                            # 仅把 /rule/definitions /rule/definitions/:code/edit 从 PlaceholderPage 改为真实组件（领单卡 §4 PR-FINAL-11 明确授权）
frontend/package.json                                     # 新增依赖：@codemirror/state @codemirror/view @codemirror/lang-json @codemirror/theme-one-dark @uiw/react-codemirror
frontend/package-lock.json                                # npm install 自动生成
docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md                     # §1 表 PR-FINAL-11 行：🟡 TODO → ✅ DONE
ai-dev-input/10_task_claims/active/PR-FINAL-11_*.md       # 本认领卡
ai-dev-input/10_task_claims/active_locks/PR-FINAL-11.lock # 任务锁
```

## Read Scope

```text
medkernel-mvp/src/main/java/com/medkernel/rule/**         # 只读：参考后端契约
frontend/src/api/types.ts                                 # 只读：复用 EvaluateRequest/EvaluateResponse/HitItem/Severity/ScenarioCode/RuleEngineResultSummary
frontend/src/components/**                                # 只读：复用 SourceInfo/DryRunResultPanel/OrgContextSelector/StatusBadge/AiBadge
frontend/src/api/ruleActionLog.ts                         # 只读：参考决策日志模式
frontend/src/pages/Pathway/**                             # 只读：参考列表/详情页结构
ai-dev-input/03_data_models/rule_dsl.schema.json          # 只读：DSL Schema
ai-dev-input/06_samples/sample_ami_rules.json             # 只读：示例规则
docs/AI_CHARTER.md docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md  # 只读：宪法 + 领单卡
```

## Forbidden Scope

```text
frontend/src/api/types.ts                                 # 架构师专属（共享类型）
frontend/src/styles/tokens*.{css,ts}                      # 架构师专属（设计 token）
frontend/src/theme/**                                     # 架构师专属
frontend/src/App.tsx frontend/src/main.tsx                # 架构师专属
frontend/eslint-rules/** frontend/eslint.config.js        # 架构师专属
medkernel-mvp/**                                          # 本 PR 纯前端
docs/01_产品事实源.md docs/02_*.md docs/03_*.md
docs/04_*.md docs/05_*.md                                 # 金本位 V2 5 份，需 DOC-V2-* 单独 PR
```

## Dependencies

```text
PR-FINAL-00 ✅ DONE   tokens/menu 收口（依赖 menuConfig 中已埋的隐藏入口注释、tokens.css 颜色基线）
PR-FINAL-05 ✅ DONE   ESLint no-inline-style + check-inline-style-count.ps1（依赖 lint 规则验证零 inline）
后端：RuleController + RuleEngineController + RuleActionLogController + RuleDslEvaluator 已就绪（develop）
```

## Acceptance

```text
1. /rule/definitions 路由可访问，渲染规则库列表（搜索/类型筛选/状态筛选/分页）
2. 点击规则进入 /rule/definitions/:code 详情，显示：
   - 元信息（编码/名称/版本/类型/严重度/组织范围）
   - 来源追溯卡（reference_document_code/citation_id 走 <SourceInfo>）
   - DSL JSON 只读视图（语法高亮）
   - 触发历史表（最近 50 条 exec-logs：traceId/命中/耗时/时间）
3. /rule/definitions/:code/edit 进入 DSL 编辑器：
   - CodeMirror 6 JSON 语法高亮 + 折叠 + 行号
   - 实时校验（按 rule_dsl.schema.json，错误高亮）
   - 试运行面板：选场景 + 注入 facts JSON → 调 /api/rules/simulate → <DryRunResultPanel>
   - 保存/发布按钮（调用 /api/rules POST + /publish）
4. 菜单：M1 知识工厂分组下新增「规则库」入口（图标 SafetyCertificateOutlined）
5. 任何点击/搜索/打开详情/试运行均自动带 X-Trace-Id（client.ts 已注入）
6. ADR-0004 合规：详情页若 reference_document_code 存在，必须用 <SourceInfo> 展示
7. 国情合规：DSL 编辑器中文注释 + 试运行示例使用国内场景（AMI/QC 等）
8. UI：100% CSS Modules + var(--mk-*) token，零 inline style（动态值除外，加 // eslint-disable 注释）
9. 测试：每个核心组件 ≥1 render 测试 + 1 交互测试（vitest + @testing-library）
10. 验证全过：frontend npm run typecheck && lint && test && build；check-inline-style-count.ps1 不上涨
```

## Status Sync Checkpoints

```text
claim_pushed_before_code: pending
task_ledger_in_progress: not_applicable (PR-FINAL-* 不走 02_任务台账，走领单卡 §1)
git_status_checked_before_edit: yes
last_heartbeat_pushed: 2026-05-21T22:00+08:00
review_status_synced: pending
task_ledger_done_synced: pending
commit_hash_recorded: pending
post_push_git_status_clean: pending
task_lock_removed_on_archive: pending
```

## Verification

```text
[本地]
cd frontend && npm install && npm run typecheck && npm run lint && npm test -- --run && npm run build
./scripts/check-inline-style-count.ps1                # 不上涨
[CI]
backend-build-test + guard-rules（本 PR 无后端改动，仅前端，仍需 guard-rules PASS）
```

## Self Check

```text
task_card_satisfied: pending
write_scope_matches_diff: pending
tests_updated: pending
samples_or_api_examples_updated: not_required
docs_updated: 仅 docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md §1 表 状态更新
db_only_checked: not_required
oracle_dm_h2_schema_synced: not_required
production_development_schema_synced: not_required
table_and_column_comments_complete: not_required
required_code_comments_complete: pending
feature_acceptance_created: pending
claim_status_synced: pending
security_privacy_checked: yes（规则不含 PHI；详情默认不渲染患者样本，仅在试运行用户输入时使用）
```

## Quality Review

```text
review_id:
review_file:
review_status: NOT_REQUESTED
highest_severity:
open_findings:
changes_requested:
approved_by:
approved_at:
submit_allowed: false
```

## Progress

```text
[2026-05-21T22:00] 认领 + 锁定 PR-FINAL-11 完整范围
```

## Handoff

```text
n/a — single-AI claim
```

## Completion

```text
commit:
push:
tests:
review:
risks: 单 PR diff 预计 ~1500 行（含 CodeMirror 编辑器子组件 + DSL Schema 校验 + 试运行联调），属领单卡 §4 PR-FINAL-11 已声明的"行数硬上限例外"。
```
