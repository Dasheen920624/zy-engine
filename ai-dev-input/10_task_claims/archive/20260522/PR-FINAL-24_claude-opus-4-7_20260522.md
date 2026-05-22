# AI Task Claim

claim_id: PR-FINAL-24-S02
task_id: PR-FINAL-24
task_lock_path: ai-dev-input/10_task_claims/active_locks/PR-FINAL-24.lock
slice: S02
title: actuator + Prometheus + Grafana 5 看板（Codex 实装 cherry-pick 重整）
owner: claude-opus-4-7@pr-final-24-cherry-pick
role: senior
status: ACTIVE
target_base_branch: develop
branch: claude/pr-final-24-cherry-pick
git_base_commit: 7b41fe6
git_status_at_claim: clean
created_at: 2026-05-22T07:15+08:00
last_heartbeat: 2026-05-22T07:15+08:00
expected_finish: 2026-05-22T08:30+08:00
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

- 锁文件：`ai-dev-input/10_task_claims/active_locks/PR-FINAL-24.lock`

## 任务背景

PR-FINAL-24 原由 Codex 实装在 `origin/codex/pr-final-24-prometheus-grafana` 分支（commit 665478b），基于 v0.3-beta 前的 main，**会回滚 PR-FINAL-15 HikariCP**（codex 的 pom.xml 删了 spring-boot-starter-jdbc 换成 actuator；application.yml 删了 hikari 段）。

按用户拍板（2026-05-22）：保留 Codex 的监控基础设施（Grafana 5 看板 + Prometheus 配置 + Actuator + 业务指标 + 测试），但**手工合并**到 develop 的 pom.xml / application.yml（追加 actuator+micrometer 依赖，不删 jdbc；追加 management 段，不删 hikari）。

**保留备份**：原 codex 分支已 bundle 到 `D:/vibeCoding/claudeCode/medkernel-codex-archive/codex-pr-final-24-prometheus-grafana-20260522.bundle`（永久可恢复）+ 2 个 patch。

## Write Scope

```text
deploy/monitoring/**                                            # checkout from codex（10 文件：5 Grafana 看板 + 配置 + 告警规则）
medkernel-mvp/src/main/java/com/medkernel/system/MedKernelBusinessMetrics.java  # checkout from codex（业务指标）
medkernel-mvp/src/test/java/com/medkernel/system/MonitoringConfigurationTests.java  # checkout from codex（监控测试）
medkernel-mvp/pom.xml                                           # 手工合并：追加 actuator + micrometer，保留 PR-FINAL-15 的 spring-boot-starter-jdbc
medkernel-mvp/src/main/resources/application.yml                # 手工合并：追加 management + info 段，保留 PR-FINAL-15 的 hikari 段
docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md                          # §1 表 PR-FINAL-24 → IN REVIEW
ai-dev-input/10_task_claims/active/PR-FINAL-24_*.md            # 本认领卡（claim_id S02）
ai-dev-input/10_task_claims/active_locks/PR-FINAL-24.lock      # 任务锁
ai-dev-input/13_feature_acceptance/pending/FA-PR-FINAL-24-S02.md  # acceptance
```

## Read Scope

```text
origin/codex/pr-final-24-prometheus-grafana                     # 只读：参考 Codex 实装
origin/develop:medkernel-mvp/pom.xml application.yml            # 只读：当前 PR-FINAL-15 配置
docs/PRODUCT_ARCHITECTURE_FINAL.md DEPLOYMENT_DUAL_MODE.md       # 只读
```

## Forbidden Scope

```text
frontend/src/api/types.ts                                       # 架构师专属（本 PR 纯后端，本来就不动）
frontend/src/**                                                 # 本 PR 纯后端 + 部署配置
docs/01-05_*.md                                                 # 金本位 V2
```

## Dependencies

```text
PR-FINAL-15 ✅ DONE   HikariCP 接入（develop 4f82f81）；本 PR 必须保留 jdbc + hikari 配置不被回滚
```

## Acceptance

```text
1. pom.xml：保留 spring-boot-starter-jdbc（PR-FINAL-15）+ 新增 spring-boot-starter-actuator + micrometer-registry-prometheus
2. application.yml：保留 medkernel.database.hikari.*（PR-FINAL-15）+ 新增 management.* 段（actuator 端口 18081 + endpoints + Prometheus 启用）+ info.app
3. deploy/monitoring/：完整复制 Codex 实装（10 文件：5 Grafana JSON 看板 + provisioning + alert-rules + prometheus 配置 + README）
4. MedKernelBusinessMetrics.java：业务指标 Bean
5. MonitoringConfigurationTests.java：Spring Boot context 加载测试
6. 后端启动后：
   - GET http://127.0.0.1:18081/medkernel/actuator/health 返回 UP
   - GET http://127.0.0.1:18081/medkernel/actuator/prometheus 返回 Prometheus 文本指标
7. 备份保留：原 codex 远端分支在本 PR 合并后才删
```

## Status Sync Checkpoints

```text
claim_pushed_before_code: in_progress
git_status_checked_before_edit: yes
last_heartbeat_pushed: 2026-05-22T07:15+08:00
review_status_synced: pending
commit_hash_recorded: pending
post_push_git_status_clean: pending
task_lock_removed_on_archive: pending
```

## Verification

```text
[本地]
mvn -q -f medkernel-mvp/pom.xml compile     # 验证 actuator + micrometer 依赖可用
[CI]
backend-build-test PASS（含 MonitoringConfigurationTests）
guard-rules PASS
```

## Self Check

```text
task_card_satisfied: pending
write_scope_matches_diff: pending
tests_updated: yes（Codex 已提供 MonitoringConfigurationTests）
docs_updated: yes
required_code_comments_complete: yes
feature_acceptance_created: pending
security_privacy_checked: yes（management 端口 18081 默认绑 127.0.0.1，外网不可达；health details 默认 when_authorized）
```

## Quality Review

```text
review_status: NOT_REQUESTED
submit_allowed: false
```

## Progress

```text
[2026-05-22T07:15] 认领 + 锁定 PR-FINAL-24（cherry-pick from codex/pr-final-24-prometheus-grafana）
```

## Completion

```text
commit:
push:
tests:
review:
risks:
  1. Codex 实装 base 较老，pom.xml / application.yml 直接 checkout 会回滚 PR-FINAL-15 HikariCP。本 PR 改为手工合并（追加，不替换）。
  2. PR-FINAL-24 是阶段 4 合规上线第 1/4，单独合 develop 不影响 v0.3-final-rc1（监控基础设施可独立部署）。
  3. Grafana 看板需要在演示前导入 Grafana 实例验证；本 PR 仅提交配置文件。
```
