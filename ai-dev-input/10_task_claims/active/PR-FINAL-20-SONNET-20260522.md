# AI Task Claim

claim_id: PR-FINAL-20-SONNET-20260522
task_id: PR-FINAL-20
task_lock_path: ai-dev-input/10_task_claims/active_locks/PR-FINAL-20.lock
slice: S01
title: springdoc-openapi + 前端 types 自动生成
owner: Claude-Sonnet-4.6
role: 架构师 AI
status: IN_PROGRESS
branch: develop
target_base_branch: develop
git_base_commit: e93f3f2
git_status_at_claim: 无未提交改动
created_at: 2026-05-22T15:00+08:00
last_heartbeat: 2026-05-22T16:00+08:00
expected_finish: 2026-05-22T17:00+08:00
heartbeat_interval_minutes: 60
database_mode: LOCAL_H2_FILE
oracle_available: false
local_db_verified: true
oracle_verification_required: false
review_required: false
review_id: N/A_DIRECT_TO_DEVELOP
review_status: PENDING
reviewer: self-verify
open_findings: 0
quality_gate: PENDING
feature_acceptance_required: false
feature_acceptance_id: N/A
write_scope: medkernel-mvp/pom.xml; medkernel-mvp/src/main/resources/application.yml; medkernel-mvp/src/main/java/com/medkernel/config/OpenApiConfig.java; medkernel-mvp/src/main/java/com/medkernel/**/*Controller.java (53 files); frontend/package.json; frontend/package-lock.json
read_scope: docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md; ai-dev-input/00_DEVELOP_HEALTH.md
forbidden_scope: medkernel-mvp/src/main/java/com/medkernel/security/SecurityPersistenceService.java

## 实施范围

- pom.xml：springdoc-openapi-ui 1.7.0（Spring Boot 2.7 兼容）
- application.yml：springdoc.swagger-ui.path=/api-docs + 仅内网端口
- config/OpenApiConfig.java：全局 API Info（JWT Bearer 安全方案）
- 53 Controller：@Tag（类级分组）+ @Operation(summary)（403 个端点全覆盖）
- frontend/package.json：openapi-typescript devDep + gen:types 脚本
