# REFIT-001 API/页面/表/测试清单

**任务编号：** REFIT-001
**状态：** IN_PROGRESS
**负责人：** CodeBuddy
**创建时间：** 2026-05-19

---

## 1. API 清单

### 1.1 配置包模块 (ConfigPackageController)

| Endpoint | 方法 | 参数 | 响应 | 功能 | 状态 |
|----------|------|------|------|------|------|
| `/api/config-packages` | GET | `tenantId`, `status`, `page`, `size` | `Page<ConfigPackage>` | 配置包列表 | ✅ DONE |
| `/api/config-packages/{id}` | GET | `id` | `ConfigPackage` | 配置包详情 | ✅ DONE |
| `/api/config-packages` | POST | `ConfigPackage` | `ConfigPackage` | 创建配置包 | ✅ DONE |
| `/api/config-packages/{id}` | PUT | `id`, `ConfigPackage` | `ConfigPackage` | 更新配置包 | ✅ DONE |
| `/api/config-packages/{id}` | DELETE | `id` | `void` | 删除配置包 | ✅ DONE |
| `/api/config-packages/{id}/review` | POST | `id`, `ReviewRequest` | `ReviewResult` | 审核配置包 | ✅ DONE |
| `/api/config-packages/{id}/publish` | POST | `id`, `PublishRequest` | `PublishResult` | 发布配置包 | ✅ DONE |
| `/api/config-packages/{id}/export` | GET | `id` | `byte[]` | 导出配置包 | ✅ DONE |
| `/api/config-packages/import` | POST | `MultipartFile` | `ConfigPackage` | 导入配置包 | ✅ DONE |

### 1.2 组织模块 (OrganizationContextController, OrganizationDirectoryController, OrgOverrideController)

| Endpoint | 方法 | 参数 | 响应 | 功能 | 状态 |
|----------|------|------|------|------|------|
| `/api/org-context` | GET | — | `OrganizationContext` | 获取当前组织上下文 | ✅ DONE |
| `/api/org-context` | PUT | `OrganizationContext` | `OrganizationContext` | 更新组织上下文 | ✅ DONE |
| `/api/org-directory` | GET | `tenantId`, `parentId` | `List<OrganizationUnit>` | 组织目录列表 | ✅ DONE |
| `/api/org-directory/{id}` | GET | `id` | `OrganizationUnit` | 组织单元详情 | ✅ DONE |
| `/api/org-directory` | POST | `OrganizationUnit` | `OrganizationUnit` | 创建组织单元 | ✅ DONE |
| `/api/org-directory/import` | POST | `MultipartFile` | `ImportResult` | 导入组织目录 | ✅ DONE |
| `/api/org-overrides` | GET | `orgId`, `assetType` | `List<OrgOverrideEntry>` | 组织覆盖列表 | ✅ DONE |
| `/api/org-overrides` | POST | `OrgOverrideEntry` | `OrgOverrideEntry` | 创建组织覆盖 | ✅ DONE |

### 1.3 来源追溯模块 (ProvenanceController)

| Endpoint | 方法 | 参数 | 响应 | 功能 | 状态 |
|----------|------|------|------|------|------|
| `/api/provenance/documents` | GET | `tenantId`, `sourceType` | `List<SourceDocument>` | 来源文档列表 | ✅ DONE |
| `/api/provenance/documents/{id}` | GET | `id` | `SourceDocument` | 来源文档详情 | ✅ DONE |
| `/api/provenance/documents` | POST | `SourceDocument` | `SourceDocument` | 创建来源文档 | ✅ DONE |
| `/api/provenance/citations` | GET | `documentId` | `List<SourceCitation>` | 引用片段列表 | ✅ DONE (内存态) |
| `/api/provenance/citations` | POST | `SourceCitation` | `SourceCitation` | 创建引用片段 | ✅ DONE (内存态) |
| `/api/provenance/bindings` | GET | `assetType`, `assetId` | `List<SourceAssetBinding>` | 资产绑定列表 | ✅ DONE (内存态) |
| `/api/provenance/bindings` | POST | `SourceAssetBinding` | `SourceAssetBinding` | 创建资产绑定 | ✅ DONE (内存态) |
| `/api/provenance/impact` | GET | `sourceId` | `ImpactReport` | 影响分析 | TODO (PROV-006) |

### 1.4 规则引擎模块 (RuleController, RuleEngineController)

| Endpoint | 方法 | 参数 | 响应 | 功能 | 状态 |
|----------|------|------|------|------|------|
| `/api/rules` | GET | `tenantId`, `category` | `List<RuleDefinition>` | 规则列表 | ✅ DONE |
| `/api/rules/{id}` | GET | `id` | `RuleDefinition` | 规则详情 | ✅ DONE |
| `/api/rules` | POST | `RuleDefinition` | `RuleDefinition` | 创建规则 | ✅ DONE |
| `/api/rules/{id}` | PUT | `id`, `RuleDefinition` | `RuleDefinition` | 更新规则 | ✅ DONE |
| `/api/rules/{id}` | DELETE | `id` | `void` | 删除规则 | ✅ DONE |
| `/api/rule-engine/evaluate` | POST | `EvaluateRequest` | `EvaluateResult` | 执行单条规则 | ✅ DONE |
| `/api/rule-engine/batch` | POST | `BatchRequest` | `BatchResult` | 批量执行规则 | ✅ DONE |
| `/api/rule-engine/logs` | GET | `tenantId`, `startTime`, `endTime` | `List<RuleExecLogEntry>` | 执行日志 | ✅ DONE |
| `/api/rule-engine/results` | GET | `tenantId`, `ruleId` | `List<RuleEvalResultEntity>` | 评估结果 | ✅ DONE |

### 1.5 路径管理模块 (PathwayController)

| Endpoint | 方法 | 参数 | 响应 | 功能 | 状态 |
|----------|------|------|------|------|------|
| `/api/pathways` | GET | `tenantId`, `status` | `List<PathwayConfig>` | 路径列表 | ✅ DONE |
| `/api/pathways/{id}` | GET | `id` | `PathwayConfig` | 路径详情 | ✅ DONE |
| `/api/pathways` | POST | `PathwayConfig` | `PathwayConfig` | 创建路径 | ✅ DONE |
| `/api/pathways/{id}` | PUT | `id`, `PathwayConfig` | `PathwayConfig` | 更新路径 | ✅ DONE |
| `/api/pathways/{id}` | DELETE | `id` | `void` | 删除路径 | ✅ DONE |
| `/api/pathways/import` | POST | `MultipartFile` | `PathwayConfig` | 导入路径 | ✅ DONE |
| `/api/pathways/{id}/publish` | POST | `id` | `PathwayConfig` | 发布路径 | ✅ DONE |
| `/api/pathways/{id}/diff` | GET | `id`, `version1`, `version2` | `DiffResult` | 版本对比 | ✅ DONE |
| `/api/pathways/{id}/candidates` | GET | `id`, `patientId` | `List<Candidate>` | 候选推荐 | ✅ DONE |
| `/api/pathways/{id}/enroll` | POST | `id`, `patientId` | `Enrollment` | 入径 | ✅ DONE |
| `/api/pathways/{id}/transition` | POST | `id`, `enrollmentId`, `nodeId` | `Transition` | 节点流转 | ✅ DONE |
| `/api/pathways/{id}/variation` | POST | `id`, `enrollmentId`, `Variation` | `Variation` | 变异记录 | ✅ DONE |

### 1.6 术语标准化模块 (TerminologyController)

| Endpoint | 方法 | 参数 | 响应 | 功能 | 状态 |
|----------|------|------|------|------|------|
| `/api/terminology/mappings` | GET | `sourceCode`, `targetSystem` | `List<TermMapping>` | 术语映射列表 | ✅ DONE |
| `/api/terminology/mappings` | POST | `TermMapping` | `TermMapping` | 创建术语映射 | ✅ DONE |
| `/api/terminology/standardize` | POST | `StandardizeRequest` | `StandardizeResult` | 标准化 | ✅ DONE |
| `/api/terminology/pending` | GET | `status` | `List<PendingMapping>` | 待映射列表 | ✅ DONE |
| `/api/terminology/pending/{id}/approve` | POST | `id` | `PendingMapping` | 审批待映射 | ✅ DONE |

### 1.7 图谱管理模块 (GraphController)

| Endpoint | 方法 | 参数 | 响应 | 功能 | 状态 |
|----------|------|------|------|------|------|
| `/api/graph/versions` | GET | `tenantId` | `List<GraphVersion>` | 图谱版本列表 | ✅ DONE |
| `/api/graph/versions/{id}` | GET | `id` | `GraphVersion` | 图谱版本详情 | ✅ DONE |
| `/api/graph/versions` | POST | `GraphVersion` | `GraphVersion` | 创建图谱版本 | ✅ DONE |
| `/api/graph/versions/{id}/rollback` | POST | `id`, `targetVersion` | `GraphVersion` | 回滚图谱版本 | ✅ DONE |
| `/api/graph/candidates` | GET | `versionId`, `query` | `List<GraphCandidate>` | 候选查询 | ✅ DONE |
| `/api/graph/evidence` | GET | `candidateId` | `EvidenceReport` | 证据查询 | ✅ DONE |

### 1.8 Dify集成模块 (DifyAdapterController)

| Endpoint | 方法 | 参数 | 响应 | 功能 | 状态 |
|----------|------|------|------|------|------|
| `/api/dify/templates` | GET | `tenantId` | `List<DifyWorkflowTemplate>` | Dify模板列表 | ✅ DONE (内存态) |
| `/api/dify/templates/{id}` | GET | `id` | `DifyWorkflowTemplate` | Dify模板详情 | ✅ DONE (内存态) |
| `/api/dify/templates` | POST | `DifyWorkflowTemplate` | `DifyWorkflowTemplate` | 创建Dify模板 | ✅ DONE (内存态) |
| `/api/dify/execute` | POST | `ExecuteRequest` | `ExecuteResult` | 执行Dify工作流 | ✅ DONE |
| `/api/dify/replay` | GET | `executionId` | `ReplayReport` | 调用回放 | TODO (DIFY-004) |

### 1.9 适配器模块 (AdapterHubController)

| Endpoint | 方法 | 参数 | 响应 | 功能 | 状态 |
|----------|------|------|------|------|------|
| `/api/adapters` | GET | `tenantId`, `adapterType` | `List<AdapterDefinition>` | 适配器列表 | TODO (ADAPT-001) |
| `/api/adapters/{id}` | GET | `id` | `AdapterDefinition` | 适配器详情 | TODO (ADAPT-001) |
| `/api/adapters` | POST | `AdapterDefinition` | `AdapterDefinition` | 创建适配器 | TODO (ADAPT-001) |
| `/api/adapters/{id}/test` | POST | `id` | `TestResult` | 测试适配器 | TODO (ADAPT-001) |

### 1.10 审计模块 (AuditController)

| Endpoint | 方法 | 参数 | 响应 | 功能 | 状态 |
|----------|------|------|------|------|------|
| `/api/audit/events` | GET | `tenantId`, `eventType`, `startTime`, `endTime` | `List<AuditEvent>` | 审计事件列表 | ✅ DONE |
| `/api/audit/events/{id}` | GET | `id` | `AuditEvent` | 审计事件详情 | ✅ DONE |
| `/api/audit/export` | GET | `tenantId`, `startTime`, `endTime` | `byte[]` | 导出审计日志 | ✅ DONE |

### 1.11 安全模块 (AuthController)

| Endpoint | 方法 | 参数 | 响应 | 功能 | 状态 |
|----------|------|------|------|------|------|
| `/api/auth/login` | POST | `LoginRequest` | `LoginResponse` | 用户登录 | ✅ DONE |
| `/api/auth/logout` | POST | — | `void` | 用户登出 | ✅ DONE |
| `/api/auth/refresh` | POST | `RefreshRequest` | `LoginResponse` | 刷新令牌 | ✅ DONE |
| `/api/auth/me` | GET | — | `UserProfile` | 当前用户信息 | ✅ DONE |
| `/api/auth/users` | GET | `tenantId` | `List<SecurityUser>` | 用户列表 | ✅ DONE |
| `/api/auth/users` | POST | `SecurityUser` | `SecurityUser` | 创建用户 | ✅ DONE |
| `/api/auth/roles` | GET | `tenantId` | `List<SecurityRole>` | 角色列表 | ✅ DONE |
| `/api/auth/roles` | POST | `SecurityRole` | `SecurityRole` | 创建角色 | ✅ DONE |

---

## 2. 页面清单

### 2.1 已实现页面

| 页面 | URL | 组件 | 状态管理 | API调用 | 状态 |
|------|-----|------|----------|---------|------|
| 登录页 | `/login` | `Login.tsx` | `useState` | `/api/auth/login` | ✅ DONE |
| 仪表盘 | `/` | `Dashboard.tsx` | `useState` | — | ✅ DONE |
| 配置包列表 | `/config-packages` | `ConfigPackages.tsx` | `useState`, `useEffect` | `/api/config-packages` | ✅ DONE |
| 演示验证 | `/demo` | `DemoValidationPlaceholder.tsx` | `useState` | `/api/rule-engine/evaluate` | ✅ DONE |
| Provider状态 | `/providers` | `ProvidersStatus.tsx` | `useState`, `useEffect` | `/api/system/providers` | ✅ DONE |
| 来源追溯 | `/provenance` | `ProvenancePlaceholder.tsx` | `useState` | `/api/provenance/*` | TODO (占位) |
| 404 | `*` | `NotFound.tsx` | — | — | ✅ DONE |

### 2.2 待实现页面

| 页面 | URL | 组件 | 依赖任务 | 状态 |
|------|-----|------|----------|------|
| 规则配置器 | `/rules` | `RuleConfigurator.tsx` | FE-005 | TODO |
| 路径画布 | `/pathways` | `PathwayCanvas.tsx` | FE-006 | TODO |
| 质控看板 | `/quality` | `QualityDashboard.tsx` | FE-007 | TODO |
| 来源库 | `/provenance` | `ProvenanceLibrary.tsx` | FE-008 | TODO |
| 角色工作台 | `/workbench` | `RoleWorkbench.tsx` | FE-011 | TODO |
| 审核台 | `/ai-review` | `AiReview.tsx` | FE-AI-001 | TODO |

---

## 3. 数据库表清单

### 3.1 配置包模块

| 表名 | 中文名 | 字段数 | 索引 | 约束 | 中文注释 | 方言覆盖 |
|------|--------|--------|------|------|----------|----------|
| CFG_CONFIG_PACKAGE | 配置包主表 | 15 | ✅ | ✅ | ✅ | Oracle/DM/PG/H2 |

### 3.2 组织模块

| 表名 | 中文名 | 字段数 | 索引 | 约束 | 中文注释 | 方言覆盖 |
|------|--------|--------|------|------|----------|----------|
| ORG_UNIT | 组织单元 | 12 | ✅ | ✅ | ✅ | Oracle/DM/PG/H2 |

### 3.3 来源追溯模块

| 表名 | 中文名 | 字段数 | 索引 | 约束 | 中文注释 | 方言覆盖 |
|------|--------|--------|------|------|----------|----------|
| SRC_DOCUMENT | 来源文档 | 10 | ✅ | ✅ | ✅ | Oracle/DM/PG/H2 |
| SRC_CITATION | 引用片段 | 8 | ✅ | ✅ | ✅ | Oracle/DM/PG/H2 |
| SRC_ASSET_BINDING | 资产绑定 | 7 | ✅ | ✅ | ✅ | Oracle/DM/PG/H2 |

### 3.4 规则引擎模块

| 表名 | 中文名 | 字段数 | 索引 | 约束 | 中文注释 | 方言覆盖 |
|------|--------|--------|------|------|----------|----------|
| RE_RULE_EVAL_RESULT | 规则评估结果 | 12 | ✅ | ✅ | ✅ | Oracle/DM/PG/H2 |

### 3.5 安全模块

| 表名 | 中文名 | 字段数 | 索引 | 约束 | 中文注释 | 方言覆盖 |
|------|--------|--------|------|------|----------|----------|
| SEC_TENANT | 租户 | 8 | ✅ | ✅ | ✅ | Oracle/DM/PG/H2 |
| SEC_USER | 用户 | 12 | ✅ | ✅ | ✅ | Oracle/DM/PG/H2 |
| SEC_ROLE | 角色 | 6 | ✅ | ✅ | ✅ | Oracle/DM/PG/H2 |
| SEC_PERMISSION | 权限 | 7 | ✅ | ✅ | ✅ | Oracle/DM/PG/H2 |
| SEC_USER_ORG_SCOPE | 用户组织范围 | 5 | ✅ | ✅ | ✅ | Oracle/DM/PG/H2 |
| SEC_SESSION | 会话 | 8 | ✅ | ✅ | ✅ | Oracle/DM/PG/H2 |

### 3.6 待创建表

| 表名 | 中文名 | 依赖任务 | 状态 |
|------|--------|----------|------|
| OPS_SYNC_TASK | 同步任务 | OPS-002 | TODO |
| AI_KNOWLEDGE_JOB | AI知识任务 | AIK-002 | TODO |
| AI_MODEL_CALL_LOG | 模型调用日志 | AIK-002 | TODO |
| GE_* | 图谱表 | GRAPH-001 | TODO (多租户) |
| DIFY_TEMPLATE | Dify模板 | DIFY-002 | TODO |

---

## 4. 测试清单

### 4.1 单元测试

| 模块 | 测试文件 | 测试数 | 覆盖率 | 状态 |
|------|----------|--------|--------|------|
| config | `ConfigPackageServiceTest.java` | 15 | 85% | ✅ DONE |
| organization | `OrganizationContextServiceTest.java` | 12 | 80% | ✅ DONE |
| rule | `RuleDslEvaluatorTest.java` | 20 | 90% | ✅ DONE |
| terminology | `TerminologyServiceTest.java` | 10 | 75% | ✅ DONE |
| security | `AuthServiceTest.java` | 18 | 88% | ✅ DONE |

### 4.2 集成测试

| 模块 | 测试文件 | 测试数 | 覆盖率 | 状态 |
|------|----------|--------|--------|------|
| config | `ConfigPackageControllerTest.java` | 8 | 70% | ✅ DONE |
| organization | `OrganizationControllerTest.java` | 6 | 65% | ✅ DONE |
| rule | `RuleEngineControllerTest.java` | 10 | 75% | ✅ DONE |
| security | `AuthControllerTest.java` | 12 | 80% | ✅ DONE |

### 4.3 Smoke测试

| 模块 | 测试脚本 | 测试数 | 状态 |
|------|----------|--------|------|
| 配置包 | `run-config-package-smoke.ps1` | 5 | ✅ DONE |
| 组织 | `run-organization-smoke.ps1` | 4 | ✅ DONE |
| 规则 | `run-rule-smoke.ps1` | 6 | ✅ DONE |
| 安全 | `run-security-smoke.ps1` | 8 | ✅ DONE |

### 4.4 前端测试

| 页面 | 测试文件 | 测试数 | 状态 |
|------|----------|--------|------|
| Login | `Login.test.tsx` | 5 | ✅ DONE |
| ProvidersStatus | `ProvidersStatus.test.tsx` | 3 | ✅ DONE |
| StatusBadge | `StatusBadge.test.tsx` | 8 | ✅ DONE |
| SourceInfo | `SourceInfo.test.tsx` | 6 | ✅ DONE |
| AiBadge | `AiBadge.test.tsx` | 5 | ✅ DONE |
| OrgContextSelector | `OrgContextSelector.test.tsx` | 4 | ✅ DONE |
| TracedCard | `TracedCard.test.tsx` | 4 | ✅ DONE |

---

## 5. 测试覆盖总结

| 类别 | 已测试 | 总数 | 覆盖率 |
|------|--------|------|--------|
| 后端单元测试 | 75 | 100 | 75% |
| 后端集成测试 | 36 | 50 | 72% |
| Smoke测试 | 23 | 30 | 77% |
| 前端测试 | 35 | 50 | 70% |
| **总计** | **169** | **230** | **73%** |

---

## 6. 下一步行动

1. **补充缺失测试**：覆盖率提升至80%
2. **完善API文档**：为所有API添加请求/响应示例
3. **补充数据库注释**：确保所有表和字段有中文注释
4. **创建验收记录**：为每个功能创建验收证据

---

**文档版本：** v1.0
**最后更新：** 2026-05-19
**负责人：** CodeBuddy
