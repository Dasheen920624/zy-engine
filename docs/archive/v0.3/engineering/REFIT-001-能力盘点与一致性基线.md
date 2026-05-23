# REFIT-001 已实现能力全量盘点与一致性基线

**任务编号：** REFIT-001
**状态：** IN_PROGRESS
**负责人：** CodeBuddy
**创建时间：** 2026-05-19
**依赖：** DOC-012 ✅ DONE

---

## 1. 已实现能力矩阵

### 1.1 后端能力矩阵

| 模块 | 包名 | 核心类 | 任务编号 | 状态 | 备注 |
|------|------|--------|----------|------|------|
| 配置包 | `config` | ConfigPackage, ConfigPackageController, ConfigPackageService, ConfigPackageRepository | PKG-001~004 | DONE | 内存态+持久化 |
| 组织上下文 | `organization` | OrganizationContext, OrganizationContextService, OrganizationDirectoryService, OrgOverrideService | ORG-001~004 | DONE | 含继承覆盖计算 |
| 来源追溯 | `provenance` | SourceDocument, SourceCitation, SourceAssetBinding, ProvenanceService | PROV-001~003 | DONE | 内存态（PROV-002F/003F TODO） |
| 规则引擎 | `rule` | RuleDefinition, RuleDslEvaluator, RuleService, RuleEvalResultRepository | RULE-001~005,008 | DONE | 含来源字段和持久化 |
| 路径管理 | `pathway` | PathwayConfigSupport, PathwayService, PathwayController | PATH-001~008 | DONE | 含来源字段 |
| 术语标准化 | `terminology` | TerminologyService, TerminologyController | TERM-001~002 | DONE | 含未映射治理 |
| 图谱管理 | `graph` | GraphService, GraphController, GraphCandidate | GRAPH-001,003,004 | DONE | 含来源绑定和回滚 |
| Dify集成 | `dify` | DifyService, DifyAdapterController, DifyWorkflowTemplate | DIFY-001 | DONE | 内存态（DIFY-002 TODO） |
| 适配器 | `adapter` | AdapterHubService, AdapterHubController | ADAPT-001 | TODO | 框架已存在 |
| 审计 | `audit` | AuditController | AUDIT-001 | DONE | 统一审计事件 |
| 安全 | `security` | AuthService, JwtTokenProvider, SecurityFilter, SecurityPersistenceService | SEC-001, PR-V2-04 | DONE | 用户体系+JWT |
| 持久化 | `persistence` | EnginePersistenceService, OrganizationPersistenceService | ARCH-004 | DONE | 多数据库分层 |

### 1.2 前端能力矩阵

| 页面 | 文件 | 功能 | 任务编号 | 状态 | 备注 |
|------|------|------|----------|------|------|
| 配置包列表 | `ConfigPackages.tsx` | 配置包管理 | FE-004 | DONE | 列表/筛选/详情/Review/Diff/发布/导出 |
| 仪表盘 | `Dashboard.tsx` | 系统概览 | FE-002 | DONE | 基础仪表盘 |
| 演示验证 | `DemoValidationPlaceholder.tsx` | 规则校验工作台 | FE-003 | DONE | 4类剧本dry-run |
| 登录 | `Login.tsx` | 用户登录 | SEC-001 | DONE | JWT认证 |
| 404 | `NotFound.tsx` | 页面未找到 | FE-002 | DONE | 路由兜底 |
| 来源追溯 | `ProvenancePlaceholder.tsx` | 来源管理 | PROV-007 | TODO | 占位页面 |
| Provider状态 | `ProvidersStatus.tsx` | 数据库状态 | ARCH-004 | DONE | 含测试 |

### 1.3 数据库表清单

| 表名 | 模块 | DDL方言 | 任务编号 | 中文注释 | 备注 |
|------|------|---------|----------|----------|------|
| CFG_CONFIG_PACKAGE | 配置包 | Oracle/DM/PG/H2 | PKG-004 | ✅ | 配置包主表 |
| ORG_UNIT | 组织 | Oracle/DM/PG/H2 | ORG-002 | ✅ | 组织目录 |
| SRC_DOCUMENT | 来源 | Oracle/DM/PG/H2 | PROV-001 | ✅ | 来源文档 |
| SRC_CITATION | 来源 | Oracle/DM/PG/H2 | PROV-002 | ✅ | 引用片段 |
| SRC_ASSET_BINDING | 来源 | Oracle/DM/PG/H2 | PROV-003 | ✅ | 资产绑定 |
| RE_RULE_EVAL_RESULT | 规则 | Oracle/DM/PG/H2 | RULE-008 | ✅ | 评估结果 |
| SEC_TENANT | 安全 | Oracle/DM/PG/H2 | SEC-001 | ✅ | 租户 |
| SEC_USER | 安全 | Oracle/DM/PG/H2 | SEC-001 | ✅ | 用户 |
| SEC_ROLE | 安全 | Oracle/DM/PG/H2 | SEC-001 | ✅ | 角色 |
| SEC_PERMISSION | 安全 | Oracle/DM/PG/H2 | SEC-001 | ✅ | 权限 |
| SEC_USER_ORG_SCOPE | 安全 | Oracle/DM/PG/H2 | SEC-001 | ✅ | 用户组织范围 |
| SEC_SESSION | 安全 | Oracle/DM/PG/H2 | SEC-001 | ✅ | 会话 |

### 1.4 API清单

| Controller | Endpoint | 方法 | 功能 | 任务编号 |
|------------|----------|------|------|----------|
| ConfigPackageController | `/api/config-packages` | GET/POST/PUT/DELETE | 配置包CRUD | PKG-001 |
| OrganizationContextController | `/api/org-context` | GET | 组织上下文 | ORG-001 |
| OrganizationDirectoryController | `/api/org-directory` | GET/POST | 组织目录 | ORG-002 |
| OrgOverrideController | `/api/org-overrides` | GET/POST | 组织覆盖 | ORG-004 |
| ProvenanceController | `/api/provenance` | GET/POST | 来源管理 | PROV-001 |
| RuleController | `/api/rules` | GET/POST | 规则配置 | RULE-001 |
| RuleEngineController | `/api/rule-engine` | POST | 规则执行 | RULE-001 |
| PathwayController | `/api/pathways` | GET/POST | 路径管理 | PATH-001 |
| TerminologyController | `/api/terminology` | GET/POST | 术语管理 | TERM-001 |
| GraphController | `/api/graph` | GET/POST | 图谱管理 | GRAPH-001 |
| DifyAdapterController | `/api/dify` | GET/POST | Dify集成 | DIFY-001 |
| AdapterHubController | `/api/adapters` | GET/POST | 适配器管理 | ADAPT-001 |
| AuditController | `/api/audit` | GET | 审计查询 | AUDIT-001 |
| AuthController | `/api/auth` | POST | 认证 | SEC-001 |

---

## 2. P0 改造 Finding（安全、数据一致性、多租户隔离）

### 2.1 安全类

| ID | Finding | 影响范围 | 严重程度 | 对应任务 |
|----|---------|----------|----------|----------|
| P0-S01 | 来源追溯（PROV-002/003）仅内存态，重启丢失 | provenance | 高 | PROV-002F, PROV-003F |
| P0-S02 | Dify模板（DIFY-001）仅内存态，DDL缺失 | dify | 高 | DIFY-002 |
| P0-S03 | 规则引擎未接入租户隔离 | rule | 高 | REFIT-002 |
| P0-S04 | 路径管理未接入租户隔离 | pathway | 高 | REFIT-002 |
| P0-S05 | 图谱管理未接入租户隔离 | graph | 高 | GRAPH-006 |

### 2.2 数据一致性类

| ID | Finding | 影响范围 | 严重程度 | 对应任务 |
|----|---------|----------|----------|----------|
| P0-D01 | 多数据库DDL字段名不一致（citation_code vs citationId） | provenance | 高 | REFIT-004 |
| P0-D02 | 持久化服务未统一加载/写入路径 | persistence | 高 | REFIT-004 |
| P0-D03 | @PostConstruct重建逻辑缺失 | persistence | 高 | REFIT-004 |

### 2.3 多租户隔离类

| ID | Finding | 影响范围 | 严重程度 | 对应任务 |
|----|---------|----------|----------|----------|
| P0-T01 | 内存Map跨租户共享 | graph | 高 | GRAPH-006 |
| P0-T02 | 规则执行结果未按租户过滤 | rule | 高 | REFIT-002 |
| P0-T03 | 路径配置未按租户过滤 | pathway | 高 | REFIT-002 |

---

## 3. P1 改造 Finding（代码规范、文档补全、性能优化）

### 3.1 代码规范类

| ID | Finding | 影响范围 | 严重程度 | 对应任务 |
|----|---------|----------|----------|----------|
| P1-C01 | 硬编码SQL未统一 | persistence | 中 | REVIEW-FIX-001 |
| P1-C02 | 异常处理不统一 | common | 中 | REVIEW-FIX-001 |
| P1-C03 | 中文注释缺失 | 多模块 | 中 | REFIT-004 |

### 3.2 文档补全类

| ID | Finding | 影响范围 | 严重程度 | 对应任务 |
|----|---------|----------|----------|----------|
| P1-D01 | API文档缺失 | 多模块 | 中 | REFIT-005 |
| P1-D02 | 部署文档不完整 | deploy | 中 | OPS-004 |
| P1-D03 | 用户手册缺失 | docs | 中 | DOC-002 |

### 3.3 性能优化类

| ID | Finding | 影响范围 | 严重程度 | 对应任务 |
|----|---------|----------|----------|----------|
| P1-P01 | 规则引擎无缓存 | rule | 中 | OPS-007 |
| P1-P02 | 图谱查询无索引 | graph | 中 | REFIT-004 |
| P1-P03 | 配置包列表无分页 | config | 中 | REFIT-005 |

---

## 4. P2 改造 Finding（可维护性、可观测性、国际化）

### 4.1 可维护性类

| ID | Finding | 影响范围 | 严重程度 | 对应任务 |
|----|---------|----------|----------|----------|
| P2-M01 | 组件库未统一 | frontend | 低 | PR-V2-02 |
| P2-M02 | 状态管理分散 | frontend | 低 | FE-013 |
| P2-M03 | 测试覆盖不足 | 多模块 | 低 | TEST-003 |

### 4.2 可观测性类

| ID | Finding | 影响范围 | 严重程度 | 对应任务 |
|----|---------|----------|----------|----------|
| P2-O01 | 缺乏统一指标 | ops | 低 | OPS-003 |
| P2-O02 | 日志格式不统一 | audit | 低 | OPS-008 |
| P2-O03 | 链路追踪缺失 | 多模块 | 低 | OPS-010 |

### 4.3 国际化类

| ID | Finding | 影响范围 | 严重程度 | 对应任务 |
|----|---------|----------|----------|----------|
| P2-I01 | 中英文混杂 | frontend | 低 | I18N-001 |
| P2-I02 | 错误消息未统一 | backend | 低 | I18N-001 |
| P2-I03 | 资源键缺失 | frontend | 低 | I18N-002 |

---

## 5. 验收基线

### 5.1 GOLD 标准（完全达标）

- 所有API有完整的请求/响应示例
- 所有数据库表有中文注释
- 所有页面有完整的交互流程
- 测试覆盖率 ≥ 80%
- 无P0级finding
- 文档完整且与代码一致

### 5.2 SILVER 标准（基本达标）

- 核心API有请求/响应示例
- 主要数据库表有中文注释
- 核心页面有基本交互
- 测试覆盖率 ≥ 60%
- 无P0级finding
- 核心文档完整

### 5.3 BRONZE 标准（最低达标）

- API可调用
- 数据库表可创建
- 页面可访问
- 基本测试通过
- 无阻塞性问题

### 5.4 REJECTED 标准（不达标）

- 存在P0级finding
- 核心功能不可用
- 数据丢失风险
- 安全漏洞

---

## 6. 改造任务映射

| Finding | 对应任务 | 优先级 | 依赖 |
|---------|----------|--------|------|
| P0-S01 | PROV-002F, PROV-003F | P0 | PROV-002, PROV-003 |
| P0-S02 | DIFY-002 | P0 | DIFY-001 |
| P0-S03~T03 | REFIT-002 | P0 | SEC-001, ORG-003 |
| P0-D01~D03 | REFIT-004 | P0 | ARCH-004 |
| P0-T01 | GRAPH-006 | P0 | GRAPH-001 |
| P1-C01~C03 | REFIT-004 | P1 | ARCH-004 |
| P1-D01 | REFIT-005 | P1 | REFIT-001 |
| P1-D02 | OPS-004 | P1 | OPS-001 |
| P1-D03 | DOC-002 | P1 | OPS-001, SEC-001 |
| P1-P01~P03 | OPS-007 | P1 | OPS-003 |
| P2-M01 | PR-V2-02 | P2 | PR-V2-01 |
| P2-M02 | FE-013 | P2 | FE-002 |
| P2-M03 | TEST-003 | P2 | DOC-004 |
| P2-O01~O03 | OPS-003, OPS-008, OPS-010 | P2 | OPS-001 |
| P2-I01~I03 | I18N-001, I18N-002 | P2 | FE-002 |

---

## 7. 当前状态总结

### 7.1 已完成能力（DONE）

- ✅ 配置包管理（PKG-001~004）
- ✅ 组织上下文（ORG-001~004）
- ✅ 来源追溯（PROV-001~003，内存态）
- ✅ 规则引擎（RULE-001~005,008）
- ✅ 路径管理（PATH-001~008）
- ✅ 术语标准化（TERM-001~002）
- ✅ 图谱管理（GRAPH-001,003,004）
- ✅ Dify集成（DIFY-001，内存态）
- ✅ 审计（AUDIT-001）
- ✅ 安全（SEC-001, PR-V2-04）
- ✅ 持久化（ARCH-004）

### 7.2 进行中能力（IN_PROGRESS）

- 🔄 FE-013：业务组件和页面状态库
- 🔄 PR-V2-02：公共组件库 v1
- 🔄 PR-V2-05：配置包列表重做 + 发布向导

### 7.3 待开发能力（TODO）

- ⏳ REFIT-002：租户/组织/身份贯通改造
- ⏳ REFIT-003：来源/审计/traceId/发布门禁统一改造
- ⏳ REFIT-004：多数据库持久化和中文注释统一补齐
- ⏳ REFIT-005：API契约、错误码和幂等统一改造
- ⏳ REFIT-006：已有前端页面一致性和组件替换
- ⏳ REFIT-007：已实现功能验收证据补齐和专业优化池

---

## 8. 下一步行动

1. **完成当前任务**：REFIT-001 全量盘点
2. **优先处理P0**：PROV-002F/003F、DIFY-002、GRAPH-006
3. **推进REFIT系列**：REFIT-002~007
4. **完善测试**：TEST-003、TEST-004
5. **文档补全**：DOC-002、REFIT-005

---

**文档版本：** v1.0
**最后更新：** 2026-05-19
**负责人：** CodeBuddy
