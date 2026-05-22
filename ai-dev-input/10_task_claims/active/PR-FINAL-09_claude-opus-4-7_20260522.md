# AI Task Claim

claim_id: PR-FINAL-09-S01
task_id: PR-FINAL-09
task_lock_path: ai-dev-input/10_task_claims/active_locks/PR-FINAL-09.lock
slice: S01
title: /admin/audit 审计日志查询页（等保 2.0 三级合规收口）
owner: claude-opus-4-7@stoic-kirch-da158e
role: senior
status: ACTIVE
target_base_branch: develop
branch: claude/pr-final-09-admin-audit
git_base_commit: 3be1ba2
git_status_at_claim: clean
created_at: 2026-05-22T16:00+08:00
last_heartbeat: 2026-05-22T16:00+08:00
expected_finish: 2026-05-22T19:00+08:00
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

- 锁文件：`ai-dev-input/10_task_claims/active_locks/PR-FINAL-09.lock`

## 任务背景

阶段 2 占位入口实装第 5 个（继 PR-FINAL-10/11/13/14 之后），把 `/admin/audit` PlaceholderPage 换成真实审计日志查询页。

后端就绪：
- `audit/AuditController` — `/api/audit-logs` 列表（8 个 filter）+ `/api/audit-logs/summary` 聚合
- `security/audit/SecurityAdminController` — `/api/security/admin/audit-chain/verify` POST 触发链校验 + `/api/security/admin/audit-chain/status` GET 状态

等保 2.0 三级要求落地：
- 审计日志只读，不允许删除 / 修改（前端不暴露删除按钮，列表仅查看）
- 验签失败的记录用红色显著标注
- 支持导出（前端 CSV 导出 + 后续可加加密 ZIP）

**附带顺手**：
- backlog §1：PR-FINAL-04 → ✅ DONE（commit `d6b5bf0` 实际已完成 Login + Dashboard 抽取 -27 inline，stale TODO 收口）
- backlog §1：PR-FINAL-08 → 🔴 BLOCKED（仓库扫描发现 0 个 `/api/users` 或 `/api/admin/users` 端点；后端 `security/SEC-001..007` 在 backlog 中标"已就绪"但实际仅有 SSO/audit/usersync/baseline 4 大类，缺 UserController/UserService。需要先开一个 PR-FINAL-08-BACKEND 架构师任务，再做前端）
- backlog §1：PR-FINAL-09 → ✅ DONE
- 02_任务台账 §2.5.2 同步补行

## Write Scope

```text
frontend/src/api/auditLog.ts                                       # 新建 API client（不污染 architects api/types.ts）
frontend/src/pages/Admin/AuditLog/AuditLogList.tsx                 # 列表 + 8 filter + 聚合卡 + CSV 导出
frontend/src/pages/Admin/AuditLog/AuditLogDetail.tsx               # 详情 Drawer + 验签结果
frontend/src/pages/Admin/AuditLog/SignatureVerifyBanner.tsx        # 审计链验签状态 + 触发校验
frontend/src/pages/Admin/AuditLog/styles.module.css                # 100% var(--mk-*) token
frontend/src/pages/Admin/AuditLog/index.ts                         # 命名导出
frontend/src/pages/Admin/AuditLog/__tests__/AuditLogList.test.tsx
frontend/src/pages/Admin/AuditLog/components/__tests__/SignatureVerifyBanner.test.tsx
frontend/src/router/menuConfig.tsx                                 # 加 admin-audit 回 M4 平台监控（DatabaseOutlined）
frontend/src/router/routes.tsx                                     # /admin/audit PlaceholderPage 改 AuditLogList
docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md                              # §1 状态表 PR-FINAL-04/09 → DONE, PR-FINAL-08 → BLOCKED
docs/engineering/02_任务台账.md                                     # §2.5.2 补 PR-FINAL-04/09 行
ai-dev-input/10_task_claims/active/PR-FINAL-09_*.md                # 本认领卡
ai-dev-input/10_task_claims/active_locks/PR-FINAL-09.lock          # 任务锁
```

## Read Scope

```text
medkernel-mvp/src/main/java/com/medkernel/audit/AuditController.java                # 只读：端点契约
medkernel-mvp/src/main/java/com/medkernel/security/audit/SecurityAdminController.java # 只读：audit-chain 端点
frontend/src/api/client.ts                                          # 只读：get/post wrapper
frontend/src/pages/AiWorkflows/**                                   # 只读：参考 PR-FINAL-13 模式
frontend/src/pages/Tenant/Onboarding/**                             # 只读：参考 PR-FINAL-10 模式
```

## Forbidden Scope

```text
frontend/src/api/types.ts                                           # 架构师专属，本地 view 类型直接在 auditLog.ts 定义
frontend/src/styles/tokens*.{css,ts}                                # 架构师专属
frontend/src/theme/** App.tsx main.tsx                              # 架构师专属
frontend/eslint-rules/** eslint.config.js                           # 架构师专属
medkernel-mvp/**                                                    # 后端 0 改动（已就绪）
docs/01-05_*.md                                                     # 金本位 V2（DOC-V2-* 流程）
```

## Dependencies

```text
后端：audit/AuditController + security/audit/SecurityAdminController 已合 develop（v0.3-final-rc1 之前）
前端：参考 PR-FINAL-13 AiWorkflows 的 react-query + Tab + view 类型模式
```

## Acceptance

```text
1. /admin/audit 路由可访问，渲染审计日志列表（默认 limit=20）
2. 8 个 filter（traceId / engineType / actionType / targetType / targetCode / patientId / encounterId / operatorId）+ limit 自由组合
3. 聚合卡：总数 / 按 actionType 分布 / 按 engineType 分布
4. SignatureVerifyBanner：默认显示 3 个审计表（engine_audit_log / sec_auth_audit_log / sec_sso_audit_log）状态；点「立即校验」触发 verifyAuditChain
5. 验签失败记录在列表中红色高亮显示（按 broken_records / first_broken_id 标识）
6. 详情 Drawer：单条记录所有字段（脱敏后）+ traceId 链路 + 验签结果
7. CSV 导出：客户端导出当前筛选结果（不含未脱敏字段）
8. 菜单：M4 平台监控新增「审计日志」入口（DatabaseOutlined）
9. 路由：/admin/audit 改真实组件（替换 PlaceholderPage）
10. UI：100% CSS Modules + var(--mk-*) token，0 新增 inline
11. 等保 2.0 三级：审计只读、无删除按钮、验签失败显著标注
12. CI 全绿（backend-build-test + guard-rules）
```

## Status Sync Checkpoints

```text
claim_pushed_before_code: yes
git_status_checked_before_edit: yes
last_heartbeat_pushed: 2026-05-22T16:00+08:00
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
tests_updated: yes（AuditLogList render + filter + SignatureVerifyBanner verify trigger）
docs_updated: yes（backlog + 任务台账同步）
required_code_comments_complete: yes
feature_acceptance_created: not_required（非高风险，只读查询页）
security_privacy_checked: yes（前端 4+4 脱敏 patientId、3+4 脱敏手机号；不显示密钥材料）
```

## Quality Review

```text
review_status: NOT_REQUESTED
submit_allowed: true_after_local_ci_pass
```

## Progress

```text
[2026-05-22T16:00] 认领 + 锁定 PR-FINAL-09；inspect 后端确认 AuditController 8 filter + SecurityAdminController 链验签就绪；
                  原计划做 PR-FINAL-08 但发现后端 UserController 缺失（backlog 表"已就绪"是 stale），改做 PR-FINAL-09。
```

## Completion

```text
commit:
push:
tests: 含 vitest 测试
review:
risks: 后端 AuditController 返回 raw Map<String, Object>，前端用本地 view 类型；future 若后端补 SNAKE_CASE DTO（PR-FINAL-16）字段对得上但需要重测。
```
