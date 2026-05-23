# Feature Acceptance

acceptance_id: FA-PR-FINAL-07-S01
feature_id: PR-FINAL-07
task_id: PR-FINAL-07
claim_id: PR-FINAL-07-S01
review_id: RV-PR-FINAL-07-S01-R01
title: /mpi/patients 患者主索引页
owner: Codex-GPT5
status: PENDING
quality_level:
created_at: 2026-05-22T08:07+08:00
updated_at: 2026-05-22T08:07+08:00
commit:
push:

## Scope

```text
功能范围：
  - /mpi/patients 真实页面：患者搜索、当前工作列表、状态/民族筛选
  - 患者详情：主索引、关联 ID、就诊记录
  - 冲突合并工作台：待处理冲突、重新检测、合并/保留/拆分/人工关联处理
  - 国情合规：身份证 4+4 脱敏、手机号 3+4 脱敏、默认脱敏、完整信息权限开关、56 民族枚举
  - 前端 API client：patient identities、visit identities、conflicts、verify、merge、resolve
  - 菜单 / 路由：M4 用户与身份恢复患者主索引入口，/mpi/patients 不再是占位页
  - DoD 构建/测试阻断最小修复：Rule query 类型、Pathway JSON 只读视图去未入包依赖、AntD Select/jsdom 测试兼容、脆弱测试断言

不验收范围：
  - 后端新增全量患者列表端点
  - 后端 DDL / 数据迁移
  - 真实 RBAC 审批流（本 PR 仅提供完整信息授权开关）
  - Pathway / Rule / AiWorkflows 业务行为重构

关联接口：
  - GET  /api/v1/mpi/patient-identities/{tenantId}/{platformPatientId}
  - GET  /api/v1/mpi/patient-identities/external
  - POST /api/v1/mpi/patient-identities/{identityId}/verify
  - POST /api/v1/mpi/patient-identities/merge
  - GET  /api/v1/mpi/visit-identities/patient/{tenantId}/{platformPatientId}
  - POST /api/v1/mpi/conflicts/detect/{tenantId}
  - GET  /api/v1/mpi/conflicts/pending/{tenantId}
  - POST /api/v1/mpi/conflicts/{conflictId}/resolve

关联页面：
  - /mpi/patients

关联表：
  - mpi_patient_identity（只读/核验/合并，经后端服务）
  - mpi_visit_identity（只读，经后端服务）
  - mpi_identity_conflict（检测/处理，经后端服务）
```

## Role Reviewers

```text
product_reviewer: 待人工指派（确认搜索式工作列表是否满足 RC 演示）
architecture_reviewer: 待架构师 AI（确认不新增后端端点的取舍）
backend_reviewer: not_required（后端已就绪，本 PR 不改 Java）
frontend_reviewer: 高级 AI 互审
database_reviewer: not_required
test_reviewer: 高级 AI 互审
medical_or_insurance_reviewer: 待人工（隐私展示、民族枚举、医保号呈现是否符合国内场景）
security_or_ops_reviewer: 待人工（完整信息权限开关后续需接 RBAC）
```

## Acceptance Checklist

```text
business_story_complete: yes
target_role_can_complete_task: yes（管理员可搜索患者、查看主索引、核验标识、处理冲突）
api_contract_stable: yes（对齐现有 MpiController，不新增后端接口）
trace_id_and_audit_complete: yes（client.ts 注入 trace；核验/合并/解决冲突带经办人）
source_traceability_complete: yes（API client 对齐 patient/MpiController）
organization_scope_complete: yes（tenant_id 来自 orgContext，Header 自动透传）
production_db_schema_synced: not_applicable
development_db_local_h2_verified: not_applicable
table_and_column_comments_complete: not_applicable
required_code_comments_complete: yes
frontend_states_complete: yes（loading / empty / selected / masked / reveal / pending conflicts）
tests_and_smoke_complete: yes（verify-pr 全绿；前端 160 测试全过）
security_privacy_checked: yes（默认脱敏；完整信息需显式开关；不在前端持久化 PHI）
docs_and_examples_updated: yes（claim / review / FA / backlog）
optimization_task_registered_if_needed: 暂无
```

## Evidence

```text
run-tests:
  PASS - bundled Node vitest run src/pages/Mpi --reporter=verbose
  Result: 4 files passed, 8 tests passed
  PASS - bundled Node vitest run --reporter=dot
  Result: 35 files passed, 160 tests passed

build:
  PASS - npm run build via bundled Node PATH/function npm

git diff --check: PASS
local_h2: not_applicable
production_db_smoke: not_applicable
frontend_validation:
  PASS - frontend lint via verify-pr
  PASS - scripts/check-inline-style-count.ps1
  PASS - scripts/verify-pr.ps1 -TaskId PR-FINAL-07 (17 PASS / 0 FAIL / 2 WARN)
screenshots_or_reports: not_run（本 PR 未启动 dev server；由 PR reviewer 可在 /mpi/patients 交互验收）
claim_review_status: REVIEW_REQUESTED
git_status_after_push: pending
```

## Findings

```text
finding_id:
severity:
owner:
status:
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
needs_optimization_task: 视 review 结果
remaining_risk:
  1. 当前后端没有全量患者列表查询，本 PR 使用“搜索载入当前工作列表”避免伪造接口；
  2. 完整信息权限开关是前端显式授权动作，后续需接入真实 RBAC；
  3. DoD 仍提示健康哨兵文件声明 RED，但本地 mvn compile、frontend lint/test/build 均通过；本 PR 不修改健康哨兵。
final_decision:
```
