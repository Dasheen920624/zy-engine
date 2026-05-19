# REFIT-001 已实现能力全量盘点与一致性基线

> 产出日期：2026-05-19
> 负责人：TraeAI-Main
> 依据：`docs/engineering/02_任务台账.md` DONE 任务清单 + 源码扫描

---

## 1. 已实现能力矩阵

### 1.1 后端 API（16 Controller / 83 端点）

| 模块 | Controller | 端点数 | 组织上下文 | 审计写入 | 持久化 |
|---|---|---|---|---|---|
| 配置包 | ConfigPackageController | 9 | ✅ | Service层 | ✅ |
| 规则引擎 | RuleEngineController | 4 | ✅ | Service层 | ✅ |
| 规则配置 | RuleController | 11 | ✅ | Service层 | ✅ |
| 路径引擎 | PathwayController | 20 | ✅ | Service层 | ✅ |
| 图谱引擎 | GraphController | 14 | ❌ | ❌ | 内存态 |
| Dify工作流 | DifyAdapterController | 5 | ❌ | ❌ | 内存态 |
| 来源追溯 | ProvenanceController | 12 | ❌ | ❌ | 内存态 |
| 术语标准化 | TerminologyController | 7 | ❌ | ❌ | 内存态 |
| 适配器 | AdapterHubController | 4 | ❌ | ❌ | 内存态 |
| 组织目录 | OrganizationDirectoryController | 4 | ❌ | ❌ | ✅ |
| 组织上下文 | OrganizationContextController | 1 | ✅ | — | — |
| 组织覆盖 | OrgOverrideController | 5 | ✅ | ❌ | ✅ |
| 质控 | QualityController | 1 | ✅ | ❌ | ❌ |
| 审计 | AuditController | 2 | ✅ | —(只读) | ✅ |
| 安全认证 | AuthController | 4 | ❌ | Service层 | ✅ |
| 系统 | HealthController | 2 | ❌ | ❌ | — |

### 1.2 前端页面（7 实际页面 / 18 占位页面）

| 页面 | 路由 | 状态 | 对接API |
|---|---|---|---|
| 登录 | /login | ✅ 可用 | auth.login/logout/me |
| 工作台 | /dashboard | ✅ 可用 | — |
| Provider状态 | /system/providers | ✅ 可用 | system.providers |
| 配置包中心 | /config/packages | ✅ 可用 | configPackage.* |
| 演示校验(占位) | /demo-validation | 占位 | — |
| 来源追溯(占位) | /provenance | 占位 | — |
| 404 | * | ✅ 可用 | — |
| 其余11个 | /pathway/*, /rule/*, /graph/*, /terminology/*, /qc/*, /aik/*, /admin/* | 占位 | — |

### 1.3 前端公共组件（5 个，PR-V2-02 交付）

| 组件 | 变体 | 单测 | Story |
|---|---|---|---|
| StatusBadge | 14种状态 / sm/md / dotOnly | 9 | ✅ |
| SourceInfo | inline/compact/card | 7 | ✅ |
| AiBadge | badge/card | 10 | ✅ |
| OrgContextSelector | dropdown/inline | 4 | ✅ |
| TracedCard | default/highlight | 5 | ✅ |

### 1.4 数据库表（31 表）

| 分类 | 表数 | 表名 |
|---|---|---|
| 组织 | 1 | org_unit |
| 路径 | 5 | pe_pathway_def, pe_pathway_version, pe_patient_instance, pe_patient_node_state, pe_patient_task_state |
| 规则 | 3 | re_rule_def, re_rule_exec_log, re_rule_eval_result |
| 术语 | 3 | tm_standard_concept, tm_concept_mapping, tm_unmapped_queue |
| 适配器 | 2 | adp_adapter_def, adp_query_def |
| 图谱 | 1 | ge_graph_version |
| 审计 | 1 | engine_audit_log |
| 来源 | 5 | src_document, src_citation, src_asset_binding, src_review_record, src_runtime_evidence |
| 配置包 | 1 | cfg_config_package |
| 安全 | 8 | sec_tenant, sec_user, sec_role, sec_permission, sec_user_role, sec_user_org_scope, sec_role_permission, sec_auth_audit_log |
| 推荐 | 1 | pe_recommendation_record（仅H2） |

### 1.5 测试覆盖

| 类别 | 数量 | 说明 |
|---|---|---|
| 后端契约测试 | 95 | EngineApiContractTests 覆盖全部 Controller |
| 前端组件测试 | 38 | 5组件 + traceId + ProvidersStatus |
| Smoke脚本 | 7 | terminology/rule/pathway/org/oracle/graph-dify/config-import |
| 样例JSON | 19 | 覆盖全部业务场景 |
| 测试矩阵 | 38 | 8维度回归用例 |

---

## 2. P0 改造 Finding（阻断级，必须修复）

### P0-01 SEC 表生产部署脚本缺失

- **问题**：SEC 8 张表 DDL 仅存在于 `ai-dev-input/04_database/{oracle,dm,postgres}/sec_ddl.sql` 和 `src/main/resources/db/local/sec_ddl.sql`，`medkernel-mvp/db/{oracle,postgres,dm}/` 下无 sec_ddl.sql。
- **影响**：生产环境部署时 SEC 表无法自动创建，用户体系不可用。
- **对应任务**：REFIT-004
- **修复方案**：将 sec_ddl.sql 同步到 `medkernel-mvp/db/{oracle,postgres,dm}/`，含中文注释。

### P0-02 pe_recommendation_record 仅 H2 存在

- **问题**：该表仅在 H2 开发库 DDL 中定义，Oracle/PG/DM 生产 DDL 均缺失。Oracle 注释文件有该表注释定义，说明设计过但未同步。
- **影响**：推荐记录无法在生产库持久化。
- **对应任务**：REFIT-004
- **修复方案**：补充 Oracle/PG/DM DDL 或确认该表已废弃并从 H2 移除。

### P0-03 cfg_config_package 索引不一致

- **问题**：H2 的 `idx_cfg_pkg_tenant` 索引列为 `(tenant_id, asset_type, status)`，Oracle/PG/DM 为 `(tenant_id, package_code, status)`。H2 第二索引 `idx_cfg_pkg_code` 与生产库 `idx_cfg_pkg_asset` 也不同。
- **影响**：开发库查询行为与生产库不一致，可能导致开发时无法发现性能问题。
- **对应任务**：REFIT-004
- **修复方案**：统一 H2 索引定义与生产库一致。

### P0-04 8 个 Controller 未接入组织上下文

- **问题**：AdapterHubController、DifyAdapterController、GraphController、ProvenanceController、OrganizationDirectoryController、TerminologyController、AuthController、HealthController 未注入 OrganizationContextService。
- **影响**：这些模块的数据无法按租户/组织隔离，多租户部署时数据越权风险。
- **对应任务**：REFIT-002
- **修复方案**：所有业务 Controller 接入 OrganizationContextService，列表/写入接口按组织过滤。

### P0-05 图谱/来源/术语/适配器/Dify 仅内存态

- **问题**：GraphController、ProvenanceController、TerminologyController、AdapterHubController、DifyAdapterController 的数据仅存内存，重启丢失。
- **影响**：生产环境不可用，无法持久化业务数据。
- **对应任务**：REFIT-004 + PROV-002F/003F + DIFY-002 + GRAPH-006
- **修复方案**：逐模块接通 EnginePersistenceService 持久化路径。

---

## 3. P1 改造 Finding（重要，应修复）

### P1-01 H2 DDL 无中文注释

- **问题**：`src/main/resources/db/local/` 下所有 DDL 文件无表/列中文注释。
- **影响**：开发库与生产库注释不同步，开发人员无法从 DDL 理解字段含义。
- **对应任务**：REFIT-004
- **修复方案**：H2 DDL 加 COMMENT 语句（H2 支持 COMMENT ON）。

### P1-02 pe_patient_task_state 索引 PG/DM 缺失

- **问题**：`idx_pe_task_instance(instance_id, node_code)` 在 H2 和 Oracle 中存在，PG 和 DM 缺失。
- **影响**：PG/DM 部署时该表查询性能可能不达标。
- **对应任务**：REFIT-004
- **修复方案**：补充 PG/DM DDL 中的索引定义。

### P1-03 tm_unmapped_queue DM 类型不一致

- **问题**：DM DDL 中该表使用 Oracle 风格类型（NUMBER/VARCHAR2），而同文件其他表使用 DM 原生类型（BIGINT/VARCHAR）。
- **影响**：达梦部署时可能出现类型兼容问题。
- **对应任务**：REFIT-004
- **修复方案**：统一 DM DDL 类型为 DM 原生风格。

### P1-04 审计写入不完整

- **问题**：OrgOverrideController、QualityController 等写操作 Controller 无审计写入。ConfigPackageController 的审计在 Service 层但未覆盖所有操作。
- **影响**：关键操作无法追溯，违反不变量 #8（发布/回滚/同步必须写审计）。
- **对应任务**：REFIT-003
- **修复方案**：统一通过 AuditService 封装审计写入，所有写操作 Controller 必须调用。

### P1-05 前端 18 个页面为占位

- **问题**：仅 7 个页面实际可用，18 个路由指向 PlaceholderPage。
- **影响**：客户验收时大部分功能不可见。
- **对应任务**：PR-V2-05~12
- **修复方案**：按 V2 PR 顺序逐个实现。

### P1-06 无 Playwright E2E 测试

- **问题**：前端无任何 E2E 测试，仅组件单测。
- **影响**：页面交互无法自动验证，演示时翻车风险高。
- **对应任务**：FE-010
- **修复方案**：PR-V2-05 起每个 PR 必须附 Playwright 脚本。

### P1-07 来源/审计/traceId 发布门禁不统一

- **问题**：ConfigPackage 发布有来源检查，但 Rule/Pathway/Graph/Dify 发布无统一来源门禁。
- **影响**：缺来源的规则/路径可能被发布，违反不变量 #8。
- **对应任务**：REFIT-003
- **修复方案**：所有发布接口统一调用 ProvenanceService.review() 阻断检查。

---

## 4. P2 改造 Finding（优化，建议修复）

### P2-01 ai-dev-input DDL 无中文注释

- **问题**：`ai-dev-input/04_database/` 中 core_ddl.sql 均无注释，sec_ddl.sql 仅 PG/Oracle 有注释。
- **影响**：设计输入文档可读性差。
- **对应任务**：REFIT-004
- **修复方案**：同步注释到 ai-dev-input DDL。

### P2-02 前端 mock 用户系统

- **问题**：`currentUser.ts` 使用 8 个 mock 用户，PR-V2-04 已完成后端认证但前端未完全接通。
- **影响**：开发模式下角色切换仍依赖 mock。
- **对应任务**：PR-V2-03 后续接通
- **修复方案**：前端 Auth 流程完全接通后端 /api/auth/me。

### P2-03 QualityController 仅 1 个端点

- **问题**：质控模块仅有 `/api/quality/metrics`，无预警列表、派单、驾驶舱 API。
- **影响**：质控功能不完整。
- **对应任务**：PR-V2-11、PR-V2-12
- **修复方案**：按 V2 PR 扩展。

### P2-04 无 API 版本管理

- **问题**：所有 API 无版本前缀（如 /api/v1/），未来不兼容变更困难。
- **对应任务**：REL-001
- **修复方案**：规划 API 版本策略。

---

## 5. 验收基线

### 5.1 后端 API 验收基线

| 模块 | 端点数 | 契约测试 | 组织上下文 | 审计 | 持久化 | 基线等级 |
|---|---|---|---|---|---|---|
| 配置包 | 9 | ✅ | ✅ | ✅ | ✅ | GOLD |
| 规则引擎 | 4 | ✅ | ✅ | ✅ | ✅ | GOLD |
| 规则配置 | 11 | ✅ | ✅ | ✅ | ✅ | GOLD |
| 路径引擎 | 20 | ✅ | ✅ | ✅ | ✅ | GOLD |
| 安全认证 | 4 | ✅ | ❌ | ✅ | ✅ | SILVER |
| 组织目录 | 4 | ✅ | ❌ | ❌ | ✅ | SILVER |
| 组织覆盖 | 5 | ✅ | ✅ | ❌ | ✅ | SILVER |
| 审计 | 2 | ✅ | ✅ | — | ✅ | SILVER |
| 质控 | 1 | ✅ | ✅ | ❌ | ❌ | BRONZE |
| 图谱 | 14 | ✅ | ❌ | ❌ | ❌ | BRONZE |
| 来源 | 12 | ✅ | ❌ | ❌ | ❌ | BRONZE |
| 术语 | 7 | ✅ | ❌ | ❌ | ❌ | BRONZE |
| 适配器 | 4 | ✅ | ❌ | ❌ | ❌ | BRONZE |
| Dify | 5 | ✅ | ❌ | ❌ | ❌ | BRONZE |
| 系统 | 2 | ✅ | ❌ | ❌ | — | SILVER |

### 5.2 前端页面验收基线

| 页面 | 基线等级 | 说明 |
|---|---|---|
| 登录 | GOLD | 接通后端认证，JWT sessionStorage |
| 工作台 | SILVER | 信息展示可用，无实时数据 |
| Provider状态 | GOLD | 接通真实 API，TanStack Query 轮询 |
| 配置包中心 | GOLD | 完整 CRUD + 发布 + 导出 |
| 其余页面 | BRONZE | 占位页 |

### 5.3 数据库验收基线

| 分类 | 表数 | 四库同步 | 中文注释 | 基线等级 |
|---|---|---|---|---|
| 核心业务表 | 23 | ✅ | 生产库✅/H2❌ | SILVER |
| 安全表 | 8 | ❌(生产部署脚本缺) | 部分 | BRONZE |
| 推荐记录 | 1 | ❌(仅H2) | ❌ | BRONZE |

---

## 6. 改造优先级路线图

| 阶段 | 任务 | 解锁 | 预计影响 |
|---|---|---|---|
| 阶段1 | REFIT-002（组织/租户贯通） | 多租户安全 | P0-04 |
| 阶段2 | REFIT-003（来源/审计/门禁统一） | 发布安全 | P1-04, P1-07 |
| 阶段3 | REFIT-004（多库持久化+注释补齐） | 生产可用 | P0-01~03, P1-01~03, P2-01 |
| 阶段4 | PROV-002F/003F + DIFY-002 + GRAPH-006 | 全模块持久化 | P0-05 |
| 阶段5 | PR-V2-05~12（前端页面实现） | 客户可演示 | P1-05~06 |

---

## 7. 统计摘要

| 指标 | 数值 |
|---|---|
| 后端 Controller | 16 |
| 后端 API 端点 | 83 |
| 前端实际页面 | 7 |
| 前端占位页面 | 18 |
| 前端公共组件 | 5 |
| 数据库表 | 31 |
| 四库同步表 | 23/31 (74%) |
| 后端契约测试 | 95 |
| 前端组件测试 | 38 |
| Smoke 脚本 | 7 |
| 样例 JSON | 19 |
| P0 Finding | 5 |
| P1 Finding | 7 |
| P2 Finding | 4 |
