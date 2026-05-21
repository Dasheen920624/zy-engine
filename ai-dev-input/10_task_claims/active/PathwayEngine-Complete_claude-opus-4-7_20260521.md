# AI Task Claim

claim_id: PATHWAY-ENGINE-COMPLETE-S01
task_id: PATHWAY-ENGINE-COMPLETE
task_lock_path: ai-dev-input/10_task_claims/active_locks/PATHWAY-ENGINE-COMPLETE.lock
slice: S01
title: 路径引擎前端完整功能（详情重写 + 版本对比 + 患者路径管理 + 变异）
owner: claude-opus-4-7@pathway-engine-complete
role: senior
status: ACTIVE
target_base_branch: develop
branch: claude/pathway-engine-followup
git_base_commit: 046ed72
git_status_at_claim: clean
created_at: 2026-05-21T22:50+08:00
last_heartbeat: 2026-05-22T06:15+08:00
expected_finish: 2026-05-22T04:00+08:00
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

- 锁文件：`ai-dev-input/10_task_claims/active_locks/PATHWAY-ENGINE-COMPLETE.lock`

## 任务背景

领单卡 PR-FINAL-* 矩阵未覆盖路径引擎前端完整化（仅 PR-FINAL-18 涉及拆后端 1950 行 PathwayService）。当前路径模块：
- 模板列表 ✅ 已实装（PR-V2-06）
- 模板详情 ⚠️ 仅 96 行简陋实现，含 7+ inline style 违反
- 模板编辑器 ✅ 已实装（PR-V2-08）
- 版本对比 ❌ PlaceholderPage（PR-V2-07 占位）
- 患者路径管理 ❌ PlaceholderPage（PR-V2-09 占位）
- 变异记录管理 ❌ 完全缺失

后端 PathwayController 已提供 26 个端点（含 diff / 候选 / 入径 / 任务管理 / 实例查询 / 变异 / 聚合统计）。本 PR 把前端补齐到与后端对齐。

## Write Scope

```text
frontend/src/api/pathway.ts                                    # 扩展现有 client：+14 端点
frontend/src/pages/Pathway/PathwayDetail.tsx                   # 重写（去 inline + 时间轴 + 引用警告 + JSON CodeMirror 只读）
frontend/src/pages/Pathway/PathwayDiff.tsx                     # 新建：版本对比页
frontend/src/pages/Pathway/PatientPathway/**                   # 新建：患者路径管理整包
frontend/src/pages/Pathway/components/PathwayTimeline.tsx      # 新建：版本时间轴
frontend/src/pages/Pathway/components/ReferenceWarnings.tsx    # 新建：引用警告卡
frontend/src/pages/Pathway/helpers/**                          # 新建：纯函数 helpers
frontend/src/pages/Pathway/styles.module.css                   # 新建：路径模块整体样式（PathwayDetail/Diff/PatientPathway 共用）
frontend/src/pages/Pathway/__tests__/**                        # 新建：单测
frontend/src/router/menuConfig.tsx                              # 加患者路径入口到 M1 知识工厂（领单卡 §2 已授权风格）
frontend/src/router/routes.tsx                                  # PlaceholderPage 改真实组件（领单卡 §2 已授权风格）
docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md                          # §2 新增实施记录
ai-dev-input/10_task_claims/active/PathwayEngine-Complete_*.md # 本认领卡
ai-dev-input/10_task_claims/active_locks/PATHWAY-ENGINE-COMPLETE.lock # 任务锁
ai-dev-input/13_feature_acceptance/pending/FA-PATHWAY-ENGINE-COMPLETE-S01.md # acceptance
```

## Read Scope

```text
medkernel-mvp/src/main/java/com/medkernel/pathway/**          # 只读：参考契约
medkernel-mvp/src/main/java/com/medkernel/dto/Patient*.java   # 只读：DTO 字段
medkernel-mvp/src/main/java/com/medkernel/dto/Pathway*.java   # 只读：DTO 字段
medkernel-mvp/src/main/java/com/medkernel/dto/RecommendationCard.java # 只读
frontend/src/api/types.ts                                       # 只读：复用 PathwaySummary/PathwayDetail/PathwayListResult/ListPathwaysParams
frontend/src/components/**                                      # 只读：复用 SourceInfo/StatusBadge/PathwayCanvas/DryRunResultPanel
frontend/src/pages/Pathway/PathwayList.tsx PathwayEditor/**     # 只读：保持骨架不变
docs/AI_CHARTER.md docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md       # 只读
```

## Forbidden Scope

```text
frontend/src/api/types.ts                                       # 架构师专属
frontend/src/styles/tokens*.{css,ts}                            # 架构师专属
frontend/src/theme/**                                           # 架构师专属
frontend/src/App.tsx frontend/src/main.tsx                      # 架构师专属
frontend/eslint-rules/** frontend/eslint.config.js              # 架构师专属
medkernel-mvp/**                                                # 本 PR 纯前端
frontend/src/pages/Pathway/PathwayList.tsx                      # 已实装（V2），本 PR 不动
frontend/src/pages/Pathway/PathwayEditor/**                     # 已实装（V2），本 PR 不动
frontend/src/components/PathwayCanvas/**                        # 共享画布，本 PR 不动
docs/01_产品事实源.md … 05_AI实施手册.md                       # 金本位 V2 5 份
```

## Dependencies

```text
PR-V2-06 ✅ DONE   路径模板 CRUD（依赖现有 PathwayList/Editor）
PR-FINAL-11 🟢 IN REVIEW  规则模块（独立，本 PR 不依赖；不冲突）
后端：PathwayController 26 端点已就绪（develop）
后端：PathwayService 1950 行不动（属 PR-FINAL-18 范围）
```

## Acceptance

```text
1. /pathway/templates/:code 详情页：
   - 元信息 + 版本时间轴（draft + 已发布 + 激活）
   - 引用警告卡（reference_warnings 缺失项可见）
   - 草稿/已发布 JSON：CodeMirror 6 只读视图（替代旧 pre 标签）
   - 实例统计（调 /pathway-instances/summary）
   - 变异统计（调 /pathway-variations/summary）
2. /pathway/templates/:code/diff?from=&to= 版本对比：
   - 顶栏选 from / to 两个版本
   - 调 /api/pathways/{code}/diff 拿后端 diff 结果
   - 节点/边/任务三段式：增/删/改高亮
   - 并排 JSON CodeMirror 视图
3. /pathway/patients 患者路径管理：
   - 列表（按路径/状态/患者/科室筛选）+ 分页
   - 入径对话框：拿 candidates 推荐 + 选模板 + 确认
   - 实例详情：节点进度时间轴 + 当前节点 + 任务列表（完成/跳过按钮）
   - 变异对话框：记录 SKIP/DEFER/EXTEND_TIME/SUBSTITUTE 等
4. 菜单：M1 知识工厂 新增「患者路径管理」入口
5. ADR-0004：路径详情 SourceInfo（reference_sources）合规渲染
6. 国情合规：
   - 患者列表脱敏（身份证 4+4 / 手机号 3+4）
   - 中文错误提示
   - 候选样本走国内场景（AMI/卒中/COPD）
7. UI：100% CSS Modules + var(--mk-*) token；PathwayDetail 重写后 inline 数下降（原有 7+ → 0）
8. 测试：每个核心组件 ≥1 render + 1 交互
9. 验证：CI guard-rules + 后端 PASS；前端 verify 由开发者本地兜底（node 18+）
```

## Status Sync Checkpoints

```text
claim_pushed_before_code: pending
git_status_checked_before_edit: yes
last_heartbeat_pushed: 2026-05-21T22:50+08:00
review_status_synced: pending
commit_hash_recorded: pending
post_push_git_status_clean: pending
task_lock_removed_on_archive: pending
```

## Verification

```text
[本地]
./scripts/check-inline-style-count.ps1               # 不上涨（PathwayDetail 重写后应下降 7+ → 守门 baseline 待手工更新）
[CI]
backend-build-test + guard-rules（本 PR 无后端改动，仅前端，仍需 guard-rules PASS）
```

## Self Check

```text
task_card_satisfied: pending
write_scope_matches_diff: pending
tests_updated: pending
docs_updated: docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md §2 实施记录
required_code_comments_complete: pending
feature_acceptance_created: pending
security_privacy_checked: yes（患者列表脱敏；不存储完整身份证/手机号到 localStorage）
```

## Quality Review

```text
review_status: NOT_REQUESTED
submit_allowed: false
```

## Progress

```text
[2026-05-21T22:50] 认领 + 锁定 PathwayEngine-Complete 完整范围
```

## Completion

```text
commit:
push:
tests:
review:
risks: 单 PR diff 预计 ~2500 行（PathwayDetail 重写 + PathwayDiff 新建 + PatientPathway 整包）。
  本 PR 不属于领单卡 PR-FINAL-* 矩阵，是 PR-V2-07 + PR-V2-09 占位实装的合并补刀。
  与 PR-FINAL-* 阶段 2 实装节奏一致，沿用相同质量标准。
```
