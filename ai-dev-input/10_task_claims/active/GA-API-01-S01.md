# GA-API-01 认领文档

## 任务信息
- 任务编号: GA-API-01
- 任务名称: OpenAPI 与前端类型生成
- 认领ID: GA-API-01-S01
- 持有者: TraeAI-GLM5
- 认领时间: 2026-05-23T19:00:00+08:00
- 基准提交: 3f69e96

## 核心验收标准
1. OpenAPI 规范与 Controller 一致
2. 前端类型生成可复现
3. springdoc-openapi 接入并配置
4. 所有 Controller 添加 OpenAPI 注解

## 独占范围
- medkernel-mvp/src/main/java/**
- docs/engineering/api-examples.http

## 执行计划
1. 添加 springdoc-openapi 依赖到 pom.xml
2. 配置 application.yml 中 OpenAPI 相关设置
3. 创建 OpenAPI 配置类（OpenApiConfig）
4. 为所有 Controller 添加 @Tag、@Operation 等注解
5. 配置前端类型生成脚本（openapi-typescript）
6. 验证 OpenAPI 规范生成正确
7. 验证前端类型生成可复现

## 关联 GA 硬指标
- D1: 编译与测试
- D6: API OpenAPI 文档齐备
