---
task_id: GA-API-01
claim_seq: S01
ai: TraeAI-Main
level: senior
branch: ai/GA-API-01/openapi-types
started: "2026-05-23T19:00:00+08:00"
write_scope:
  - medkernel-mvp/src/main/java/com/medkernel/**
  - docs/engineering/api-examples.http
shared_files:
  - medkernel-mvp/pom.xml
  - medkernel-mvp/src/main/resources/application.yml
---

# GA-API-01 · OpenAPI 与前端类型生成

## Goal

OpenAPI 规范与 Controller 一致，前端 TypeScript 类型生成可复现。

## Plan

1. 引入 springdoc-openapi 依赖到 pom.xml
2. 配置 application.yml 暴露 OpenAPI 端点
3. 为所有 Controller 添加 @Tag/@Operation/@Schema 注解
4. 验证 /v3/api-docs 输出与 Controller 一致
5. 编写前端类型生成脚本（openapi-typescript）
6. 生成前端 TypeScript 类型并验证可复现

## Acceptance

````text
1. /v3/api-docs 输出完整 OpenAPI 3.0 spec
2. 每个 Controller 有 @Tag，每个端点有 @Operation
3. 前端 `npx openapi-typescript` 可复现生成类型文件
4. 生成的类型与 Controller 契约一致
5. 后端 compile/test 通过
6. 前端 lint/typecheck/build 通过
````

## Verification

````text
cd medkernel-mvp && mvn compile test -q
cd frontend && npm run lint && npm run typecheck && npm run build
````

## DoD

- [ ] springdoc-openapi 集成完毕
- [ ] 所有 Controller 注解完备
- [ ] 前端类型生成脚本可复现
- [ ] CI 通过
