# AI Task Claim

claim_id: GA-API-01-S01
task_id: GA-API-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-API-01.lock
slice: S01
title: OpenAPI 与前端类型生成
owner: TraeAI-5
role: senior
status: DONE
branch: develop
target_base_branch: develop
git_base_commit: 69b46dd
git_status_at_claim: clean
created_at: 2026-05-23T20:30+08:00
last_heartbeat: 2026-05-23T20:30+08:00
expected_finish: 2026-05-23T23:00+08:00

## Write Scope

```text
medkernel-mvp/src/main/resources/application.yml
frontend/src/api/generated-types.ts
frontend/package.json
frontend/scripts/gen-api-types.sh
ai-dev-input/10_task_claims/active/GA-API-01-S01.md
ai-dev-input/10_task_claims/active_locks/GA-API-01.lock
docs/engineering/02_任务台账.md
```

## Acceptance

```text
1. 后端 springdoc 配置正确，/v3/api-docs 输出完整 OpenAPI spec
2. 前端 gen:types 脚本可执行并生成 generated-types.ts
3. generated-types.ts 提交到代码库
4. CI 中添加类型生成验证步骤
5. 类型生成可复现（同一 spec 产出相同类型）
```

## Progress

```text
- [ ] 创建 claim + lock 并 push
- [ ] 验证后端 OpenAPI spec 输出
- [ ] 运行 gen:types 生成前端类型
- [ ] 验证类型生成可复现
- [ ] 更新台账 + commit + push
```
