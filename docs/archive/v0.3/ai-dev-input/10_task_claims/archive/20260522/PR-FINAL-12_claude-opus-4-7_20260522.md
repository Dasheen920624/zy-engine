# AI Task Claim

claim_id: PR-FINAL-12-S01
task_id: PR-FINAL-12
task_lock_path: ai-dev-input/10_task_claims/active_locks/PR-FINAL-12.lock
slice: S01
title: /adapter/hub 适配器中心（业务适配器 + 互联互通 + CDSS 触发点）
owner: claude-opus-4-7@stoic-kirch-da158e
role: senior
status: ACTIVE
target_base_branch: develop
branch: claude/pr-final-12-adapter-hub
git_base_commit: f766353
git_status_at_claim: clean
created_at: 2026-05-22T17:00+08:00
last_heartbeat: 2026-05-22T17:00+08:00
expected_finish: 2026-05-22T20:00+08:00
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
quality_gate: PASS_ON_GREEN_CI
feature_acceptance_required: false
feature_acceptance_id:
write_scope: see "Write Scope" section
read_scope: see "Read Scope" section
forbidden_scope: see "Forbidden Scope" section

## Task Lock

- 锁文件：`ai-dev-input/10_task_claims/active_locks/PR-FINAL-12.lock`

## 任务背景

阶段 2 占位入口实装第 6 个（继 PR-FINAL-10/11/13/14/09 之后），把 `/adapter/hub` PlaceholderPage 换成真实适配器中心页。

后端就绪（3 个 Controller 15 个端点，inspect 已确认）：
- `adapter/AdapterHubController` `/api/adapters` — 业务适配器（HIS/EMR/LIS 业务查询编排）
- `adapter/InteropController` `/api/interop` — 互联互通标准适配器（HL7 v2 / FHIR / CDA / IHE / CDS Hooks / SMART on FHIR / DICOM）
- `adapter/TriggerPointController` `/api/cdss/triggers` — CDSS 触发点（业务场景 + 接入策略 + 规则/路径绑定）

实体 `CdssTriggerPoint` 字段：triggerCode / triggerName / triggerType / businessScenario / accessStrategy / adapterCode / endpointUrl / ruleCodes / pathwayCodes / priority / riskLevel / timeoutMs / enabled / description。

后端不暴露的：批量启用/禁用（PR-FINAL-08a 后端补；本 PR 用 register/update 单条做）；"测试连接"端点（前端调 query POST 加 sample payload 当测试）。

## Write Scope

```text
frontend/src/api/adapterHub.ts                                     # 新建 API client
frontend/src/pages/Adapter/AdapterHubPage.tsx                      # 主页（3 Tab）
frontend/src/pages/Adapter/components/AdapterDefinitionList.tsx    # 业务适配器列表
frontend/src/pages/Adapter/components/InteropAdapterList.tsx       # 互联互通列表（3 类合：adapters/cds-hooks/smart-apps）
frontend/src/pages/Adapter/components/TriggerPointList.tsx         # CDSS 触发点列表
frontend/src/pages/Adapter/components/AdapterDetailDrawer.tsx      # 单 adapter 详情 Drawer
frontend/src/pages/Adapter/components/TriggerEditModal.tsx         # 触发点注册/更新 Modal
frontend/src/pages/Adapter/styles.module.css                       # 100% var(--mk-*) token
frontend/src/pages/Adapter/index.ts                                # 命名导出
frontend/src/pages/Adapter/__tests__/AdapterHubPage.test.tsx
frontend/src/pages/Adapter/components/__tests__/TriggerPointList.test.tsx
frontend/src/router/menuConfig.tsx                                 # 加 adapter-hub 回 M1 知识工厂（ClusterOutlined）
frontend/src/router/routes.tsx                                     # /adapter/hub PlaceholderPage 改 AdapterHubPage
docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md                              # §1 状态表 PR-FINAL-12 → DONE
docs/engineering/02_任务台账.md                                     # §2.5.2 补 PR-FINAL-12 行
ai-dev-input/10_task_claims/active/PR-FINAL-12_*.md                # 本认领卡
ai-dev-input/10_task_claims/active_locks/PR-FINAL-12.lock          # 任务锁
```

## Read Scope

```text
medkernel-mvp/src/main/java/com/medkernel/adapter/**               # 只读：3 Controller + 实体 + Service
frontend/src/api/client.ts                                          # 只读
frontend/src/pages/AiWorkflows/**                                   # 只读：参考 Tab + react-query 模式
frontend/src/pages/Admin/AuditLog/**                                # 只读：参考 Drawer + filter 模式
```

## Forbidden Scope

```text
frontend/src/api/types.ts                                           # 架构师专属
frontend/src/styles/tokens*.{css,ts}                                # 架构师专属
frontend/src/theme/** App.tsx main.tsx                              # 架构师专属
frontend/eslint-rules/** eslint.config.js                           # 架构师专属
medkernel-mvp/**                                                    # 后端 0 改动（已就绪）
docs/01-05_*.md                                                     # 金本位 V2
```

## Dependencies

```text
后端：3 Controller 已合 develop（AdapterHub / Interop / TriggerPoint）
前端：参考 PR-FINAL-13 AiWorkflows 的 3 Tab 模式 + PR-FINAL-09 AuditLog 的 Drawer 模式
```

## Acceptance

```text
1. /adapter/hub 路由可访问，渲染 3 Tab 主页
2. Tab 1「业务适配器」：listDefinitions 列表，点击行打开 AdapterDetailDrawer（getDefinition）
3. Tab 2「互联互通」：3 子分组（adapters / cds-hooks / smart-apps）合并展示
4. Tab 3「CDSS 触发点」：listTriggers 列表 + business_scenario / access_strategy filter；行操作：编辑（updateTrigger）+ 执行（executeTrigger，弹 payload 输入）；顶部「新建触发点」按钮（registerTrigger）
5. 菜单：M1 知识工厂新增「适配器中心」入口（ClusterOutlined）
6. 路由：/adapter/hub 改真实 AdapterHubPage
7. UI：100% CSS Modules + var(--mk-*) token，0 新增 inline
8. CI 全绿
```

## Status Sync Checkpoints

```text
claim_pushed_before_code: yes
git_status_checked_before_edit: yes
last_heartbeat_pushed: 2026-05-22T17:00+08:00
review_status_synced: pending
commit_hash_recorded: pending
post_push_git_status_clean: pending
task_lock_removed_on_archive: pending
```

## Verification

```text
[本地]
./scripts/check-inline-style-count.ps1   # 不上涨（本 PR 0 新增 inline）
[CI]
backend-build-test + guard-rules
```

## Self Check

```text
task_card_satisfied: pending
write_scope_matches_diff: pending
tests_updated: yes
docs_updated: yes
required_code_comments_complete: yes
feature_acceptance_created: not_required（管理后台只读 + 触发点编辑，非高风险病人数据）
security_privacy_checked: yes（不展示密钥/token；endpoint_url 截断显示长串）
```

## Quality Review

```text
review_status: NOT_REQUESTED
submit_allowed: true_after_local_ci_pass
```

## Progress

```text
[2026-05-22T17:00] 认领 + 锁定 PR-FINAL-12；inspect 后端 3 Controller 15 端点确认就绪。
```

## Completion

```text
commit:
push:
tests: 含 vitest 测试
review:
risks: 后端 raw Map<String,Object> 返回，前端用本地 view 类型；future PR-FINAL-16 SNAKE_CASE 全局后契约不变。
```
