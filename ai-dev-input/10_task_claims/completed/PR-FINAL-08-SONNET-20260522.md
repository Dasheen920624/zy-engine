# AI Task Claim

claim_id: PR-FINAL-08-SONNET-20260522
task_id: PR-FINAL-08
task_lock_path: ai-dev-input/10_task_claims/completed/PR-FINAL-08-SONNET-20260522.md
slice: S01
title: /admin/users 用户管理页（含 PR-FINAL-08a 后端）
owner: Claude-Sonnet-4.6
role: 中级AI（含架构师部分：UserAdminController/Service + SecurityPersistenceService 扩展）
status: COMPLETED
branch: develop
target_base_branch: develop
git_base_commit: 151f1b0
git_status_at_claim: 无未提交改动
created_at: 2026-05-22T13:00+08:00
last_heartbeat: 2026-05-22T13:50+08:00
expected_finish: 2026-05-22T14:00+08:00
heartbeat_interval_minutes: 60
database_mode: LOCAL_H2_FILE
oracle_available: false
local_db_verified: true
oracle_verification_required: false
review_required: false
review_id: N/A_DIRECT_TO_DEVELOP
review_status: APPROVED
reviewer: self-verify
open_findings: 0
quality_gate: PASSED
feature_acceptance_required: false
feature_acceptance_id: N/A

## 完成摘要

### 后端（PR-FINAL-08a）
- `SecurityPersistenceService`：新增 10 个管理员专用方法（listUsers / countUsers /
  updateUserStatus / unlockUser / replaceUserRoles / resetPassword / listRoles /
  usernameExists / createUserWithPassword）
- `security/admin/UserAdminService.java`：业务层，含 BCrypt 密码哈希 + GB18030 CSV 解析
- `security/admin/UserAdminController.java`：8 个 REST 端点（/api/admin/users/*）

### 前端（PR-FINAL-08）
- `frontend/src/api/userAdmin.ts`：完整 API 契约 + 类型定义
- `UserList.tsx`：分页列表（筛选 + 锁定行红色高亮 + 行内操作）
- `UserDetail.tsx`：详情 Drawer（身份绑定 + 等保 2.0 三级：失败次数/锁定时间）
- `RoleAssignDialog.tsx`：角色分配 Modal（幂等替换 + 按 role_type 分组）
- `CsvImportDialog.tsx`：GB18030 CSV 批量导入
- `styles.module.css`：100% token，inline style 545→537（净减 8）
- `__tests__/UserList.test.tsx`：8 个单元测试
- `menuConfig.tsx`：解注释 admin-users 入口
- `routes.tsx`：PlaceholderPage → UserManagementPage

### 质量验证
- 后端：mvn compile PASS / 248 tests PASS
- 前端：typecheck PASS / 184 tests PASS / lint 0 errors / build PASS
- verify-pr：PASS=17 FAIL=0 WARN=3
- inline style 守门：537 < 545（PASS）
- git commit：`e93f3f2`（rebase 后 develop HEAD）
