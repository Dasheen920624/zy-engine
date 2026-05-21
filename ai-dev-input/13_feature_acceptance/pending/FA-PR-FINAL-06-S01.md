# Feature Acceptance: FA-PR-FINAL-06-S01

acceptance_id: FA-PR-FINAL-06-S01
feature_id: unified-login-four-tabs
task_id: PR-FINAL-06
claim_id: PR-FINAL-06-S01
review_id: RV-PR-FINAL-06-S01-R01
title: LoginPage 4 Tab 重写（国情合规 12 条）
owner: Codex-GPT5
status: PENDING_PRODUCT_ACCEPTANCE
quality_level: GOLD
created_at: 2026-05-21T22:41:50+08:00
updated_at: 2026-05-21T22:41:50+08:00
commit: pending
push: pending

## Scope

```text
功能范围：
- /login 统一为四 Tab：手机短信、账号密码、SSO、域账号。
- /sso-login 进入同一个 LoginPage，并默认选中 SSO Tab。
- 删除旧 Login.tsx / SsoLogin.tsx 双副本页面。
- 登录合规页脚显示用户协议、隐私政策、忘记密码、ICP、公网安备、版本与会话保护提示。
- 账号密码 Tab 保留 /api/auth/login，增加协议勾选、SM2/MFA、密码强度、失败次数与验证码触发提示。
- SSO/LDAP 保留现有 /api/security/sso/* 前端契约。
- MSW 与 E2E 覆盖统一登录页。

不验收范围：
- 不新增后端 SMS API。
- 不改 DDL/schema。
- 不改主菜单和其它业务页面。
```

## Acceptance Checklist

```text
business_story_complete: yes
target_role_can_complete_task: yes
api_contract_stable: yes
frontend_states_complete: yes
responsive_checked: yes
security_privacy_checked: yes
tests_and_smoke_complete: yes
docs_and_records_updated: yes
legacy_baggage_removed: yes
optimization_task_registered_if_needed: N/A
```

## Evidence

```text
eslint_changed_files:
  PASS — eslint on auth/router/mocks/vite-env changed scope

eslint_full:
  PASS — eslint . via bundled Node 24

typecheck:
  PASS — tsc -b --noEmit

build:
  PASS — vite build

unit_tests:
  PASS — vitest run, 39 tests

inline_style_guard:
  PASS — scripts/check-inline-style-count.ps1 reports 561 current / 582 baseline

playwright_smoke:
  PASS — /login desktop, password login redirect, /sso-login SSO tab, /login mobile

e2e_login:
  PASS — e2e/auth/login.spec.ts, 4 tests

git_status_after_push:
  pending
```

## Findings

```text
finding_id: none
severity: none
owner: none
status: CLOSED
```

## Verdict

```text
quality_level: GOLD
approved_for_customer_demo: true
approved_for_integration: true
needs_optimization_task: false
remaining_risk: SMS verification uses demo bridge until PR-V3-COMPLIANCE-BACKEND implements /api/auth/sms/*.
final_decision: Ready for PR review to develop.
```
