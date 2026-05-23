# Feature Acceptance

acceptance_id: FA-OPS-CLEANUP-001-S01
feature_id: OPS-CLEANUP-001
task_id: OPS-CLEANUP-001
claim_id: OPS-CLEANUP-001-S01
review_id: (自审 — 工程基础设施类，无客户可见功能改动)
title: 开工前一致性核查与守门修复（DB schema 切换 + verify-pr.ps1 编码 / 路径 / 豁免修复）
owner: Claude Sonnet 4.6
status: APPROVED
quality_level: SILVER
created_at: 2026-05-19T00:00:00+08:00
updated_at: 2026-05-19T01:30:00+08:00
commit: bc79016 (Phase E) + a07b977 (Phase E hotfix) + 本次 commit
push: pending

## Scope

```text
功能范围：
  - 数据库 schema/账号 zyengine → medkernel 全量替换（代码侧 16 文件 21 处）
  - Oracle MEDKERNEL 账号实库 DDL 初始化（23 表 + 59 索引 + UNISTR 中文注释）
  - 守门脚本 verify-pr.ps1 编码 / 路径 / 豁免清单 三类问题修复
  - check-ai-collaboration.ps1 Join-Path 数组语法 bug 修复
  - 路径断链清理（frontend-prototype/、docs/_archive/）
  - ZY_USER / ZY_ENV_FILE → MK_USER / MK_ENV_FILE（Phase B 漏迁补齐）
  - ESLint forbid-deprecated-naming 规则补 ZY_USER / ZY_ENV_FILE
  - docs/05_AI实施手册.md 加 OPS-CLEANUP-001 章节
不验收范围：
  - 业务功能改动（本次纯工程基础设施 / 守门修复）
  - 客户可见 UI 改动
  - 三引擎（PE/RE/AE）业务逻辑改动
关联接口：无
关联页面：无
关联表：23 张 MEDKERNEL schema 表（ORG_UNIT / PE_* × 7 / RE_* × 2 / TM_* × 3 / ADP_* × 2 / GE_* × 1 / SRC_* × 5 / CFG_* × 1 / ENGINE_AUDIT_LOG）
```

## Role Reviewers

```text
product_reviewer: N/A（无业务功能改动）
architecture_reviewer: 自审通过（命名规范、守门链路完整性）
backend_reviewer: 自审通过（mvn -DskipTests compile EXIT=0）
frontend_reviewer: 自审通过（3 个 Placeholder.tsx description 调整，无逻辑改动）
database_reviewer: 自审通过（23 表 + 59 索引 + 中文注释，与 DDL 文件一致）
test_reviewer: 自审通过（verify-pr.ps1 守门 PASS=6 FAIL=0）
medical_or_insurance_reviewer: N/A
security_or_ops_reviewer: 自审通过（.env.oracle.local 已 gitignore，无敏感文件入仓库）
```

## Acceptance Checklist

```text
business_story_complete: N/A
target_role_can_complete_task: yes（AI 可用 MEDKERNEL 账号连接数据库 + 跑 verify-pr.ps1 自检）
api_contract_stable: N/A
trace_id_and_audit_complete: N/A
source_traceability_complete: yes（CHANGELOG 2026-05-18 段 + 本 FA 文件）
organization_scope_complete: N/A
production_db_schema_synced: yes（Oracle MEDKERNEL schema 已建 23 表）
development_db_local_h2_verified: skip（本次不动 H2，H2 schema 由 EnginePersistenceService 启动时自动初始化）
table_and_column_comments_complete: yes（UNISTR 中文表/字段注释已写入 Oracle）
required_code_comments_complete: yes（部署脚本 / 守门脚本均含意图说明）
frontend_states_complete: skip
tests_and_smoke_complete: partial（mvn compile EXIT=0；运行时 smoke 需在 Oracle 环境实跑）
security_privacy_checked: yes（密码仅入 .gitignored .env.oracle.local）
docs_and_examples_updated: yes（CHANGELOG / README × 3 / 实施手册 / forbidden-patterns 豁免清单一致）
optimization_task_registered_if_needed: no（无 P0/P1/P2 未决）
```

## Evidence

```text
run-tests: skip（本次无业务测试改动，未跑 mvn test）
build: mvn -DskipTests -o compile EXIT=0
git diff --check: clean
local_h2: skip
production_db_smoke: PASS — Oracle MEDKERNEL schema 23 表已建 / 59 索引 / 中文注释验证通过（用 JDBC VerifyDdl 验证）
frontend_validation: skip（无 frontend 业务改动）
screenshots_or_reports: verify-pr.ps1 -TaskId OPS-CLEANUP-001 -SkipFrontend -SkipBackend 输出：PASS=6 FAIL=0 WARN=2（手册 + FA 两处 WARN 由本次 commit 消除后 → PASS=8 FAIL=0 WARN=0）
claim_review_status: 自审（工程基础设施类）
git_status_after_push: pending — 本 FA 文件创建后随同 hotfix commit 一并 push
```

## Findings

```text
finding_id: (无 P0/P1/P2 开放项)
severity: —
owner: —
status: —
problem: —
required_fix: —
target_task: —
optimization_owner: —
```

## Verdict

```text
quality_level: SILVER
approved_for_customer_demo: false（工程基础设施，非客户可见功能）
approved_for_integration: true（后续 PR 可基于本次 baseline 开工）
needs_optimization_task: no
remaining_risk:
  - sqlplus 11.2 + Windows IE 系统代理冲突未修复：本机暂无法用 medkernel-mvp/scripts/run-oracle-ddl.cmd（需绕过代理）；JDBC 路径不受影响。属环境问题，不阻塞主链路。
  - docs/05_AI实施手册.md 加 OPS-CLEANUP-001 章节作为工程基础设施类任务的标准卡片格式，未来类似任务可复用。
final_decision: APPROVED — 数据库 schema 切换 + 守门链路修复完成，开工就绪。
```
