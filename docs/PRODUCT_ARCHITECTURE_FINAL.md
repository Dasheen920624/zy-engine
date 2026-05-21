# MedKernel · 产品架构最终白皮书

> 版本：1.0 · 2026-05-21（产品架构师视角，最终归口）  
> 起草人：架构组（Claude Opus 切换产品+架构角色）  
> 适用：本文是**全项目落地的最高约束**，任何后续决策与本文冲突时以本文为准（除非新开 ADR 推翻）。  
> 阅读对象：产品 / 架构 / 开发 AI / 实施工程师 / 客户售前（按需读对应章节）。

---

## 0. 这份文档解决什么问题

实地核查发现 6 类系统性问题，这份白皮书逐一定调：

| 问题 | 现象 | 本文裁决 |
|---|---|---|
| **命名混乱** | 4 治理域同时有 2 套叫法（「配置治理 / 知识工厂」「运营治理 / 质控驾驶舱」），菜单和文档不一致 | §1：统一「四大模块」命名，禁止双命名 |
| **路径不全** | 4 个菜单项点击 404（`/mpi/patients`、`/adapter/hub`、`/dify/workflows`、`/tenant/onboarding`），8 个入口是占位页 | §2：菜单与路由一一对应表，砍菜单 or 补路由 |
| **Controller 双副本** | MpiController（17+7 端点）/ UserSyncController（2 实现）方向不一致 | §3：ADR-0005/0006 决策 → 表内裁决 |
| **数据模型分散** | EnginePersistenceService 2175 行管 12+ 表 / SecurityPersistenceService 1178 行 / KnowledgePackageService 1334 行 | §4：按 6 个领域聚合上下文（Bounded Context）拆分 |
| **API 风格不统一** | 35 个 Controller 用 raw Map，URL 有的用 kebab-case 有的 snake_case | §5：API 设计规范统一约束 |
| **完成定义模糊** | 「v0.2-demo 发布」≠「v1.0 GA」，但没人定义 GA 标准 | §6：v1.0 GA 七维度准入 |

---

## 1. 产品架构（终态）

### 1.1 三产品 × 四大模块

**三产品** = 三种用户节奏（不变量，对应 ADR-0001）：

| 产品 | 用户 | 节奏 | 入口域名（建议） | 终态愿景 |
|---|---|---|---|---|
| **A · 知识工厂** | 医学专家 / 信息科 / 临床路径主任 | 周-月 | `studio.medkernel.com` | 像 Figma 一样管医学知识：路径 / 规则 / 图谱 / 字典 / 适配器 / Dify 工作流，可版本化 / 灰度 / 回滚 / 来源追溯 |
| **B · 临床嵌入器** | 医生 / 护士 / 药师 / 检验技师 | 秒-分钟 | 嵌入 HIS / EMR / 移动端 | 像 Apple Health 一样无感：在医生既有工作流里推合理建议，永远显式医生确认，永远显源 |
| **C · 质控驾驶舱** | 集团 CIO / CMO / 质控科 / 医保科 / 院领导 | 日-周 | `cockpit.medkernel.com` | 像 Grafana 一样看治理：合规率 / 路径执行率 / CDSS 命中率 / 提醒疲劳 / AI 知识 ROI |

**四大模块** = 服务端能力的最终归类（**统一命名，禁止双命名**）：

| 模块 | 内容 | 服务 A 用 | 服务 B 用 | 服务 C 用 | 责任团队 |
|---|---|:---:|:---:|:---:|---|
| **M1 · 知识与配置（KnowledgeOps）** | 配置包 / 路径 / 规则 / 图谱 / 字典 / 适配器 / Dify / 来源 | ✅ 主用 | 读取 | 读取 | 知识团队 |
| **M2 · 临床决策（ClinicalDecision）** | CDSS 触发点 / 推荐 / 提醒疲劳 / 医疗安全红线 / 路径推荐 | 写规则 | ✅ 主用 | 监控 | 临床团队 |
| **M3 · 质控与评估（QualityOps）** | 院级驾驶舱 / 质控预警 / 评估指标 / 评估结果 / 模型调用日志 | 读取 | 读取 | ✅ 主用 | 质控团队 |
| **M4 · 平台底座（Platform）** | 组织 / 用户 / 身份 / SSO / MPI / 租户 / 审计 / 通知 / 待办 / Provider 状态 / 安全基线 | 共用 | 共用 | 共用 | 平台团队 |

**禁止再使用的命名**（违者改回）：

| 旧叫法 | 新叫法 | 出现位置（必改） |
|---|---|---|
| 「配置治理」 | **「M1 · 知识与配置」**（前端菜单分组用 **「知识工厂」**） | `menuConfig.tsx` `MenuSection.label` |
| 「运营治理」 | **「M3 · 质控与评估」**（前端菜单分组用 **「质控驾驶舱」**） | 同上 |
| 「用户与组织」 | 拆为 **「M4 · 用户与身份」**（菜单分组「用户与身份」） | 同上 |
| 「系统」 | **「M4 · 平台监控」**（菜单分组「平台监控」） | 同上 |
| 「医疗智能引擎平台」 | **「集团医疗智能中枢 MedKernel」** | 所有页面 Title / Login / 文档 |
| 「治理控制台」 | **「管理工作台」** | Login 副标题 / 全局 header |

### 1.2 模块 ↔ 后端包 ↔ 前端路由 最终对照表

> 这张表是**唯一权威**。任何 Controller / 路由 / 菜单 变更必须先改本表，再改代码。

| 模块 | 后端包 | 前端路由前缀 | 菜单分组 | 状态 |
|---|---|---|---|---|
| **M1 · 知识与配置** | | | **知识工厂** | |
| 配置包 | `config` | `/config/packages` | 配置包中心 | ✅ READY |
| 路径模板 | `pathway` | `/pathway/templates` | 路径配置 | ✅ READY |
| 规则定义 | `rule` | `/rule/definitions` | 规则配置 | 🟡 后端 ✅ 前端占位 → **PR-V3-07** |
| 图谱配置 | `graph` | `/graph/explore` | 图谱配置 | 🟡 后端 ✅ 前端占位 → **PR-V3-08** |
| 字典映射 | `terminology` | `/terminology/mapping` | 字典映射 | ✅ READY |
| 适配器中心 | `adapter` | `/adapter/hub` | 适配器中心 | 🔵 后端 ✅ 前端占位 → **PR-V3-01** |
| Dify 工作流 | `dify` | `/dify/workflows` | Dify 工作流 | 🔵 后端 ✅ 前端占位 → **PR-V3-02** |
| 来源追溯 | `provenance` | `/provenance` | 来源追溯 | 🟡 前端占位 → **PR-V3-12** |
| **M2 · 临床决策** | | | **（嵌入 HIS）** | |
| CDSS 触发 | `cdss` | `/cdss/*` + `/embed/*` | CDSS 提醒疲劳 | ✅ READY |
| 临床安全 | `cdss.ClinicalSafety` | `/embed/order-safety` | （嵌入） | ✅ READY |
| 医疗红线 | `cdss.SafetyRedLine` | （API only） | （嵌入） | ✅ READY |
| **M3 · 质控与评估** | | | **质控驾驶舱** | |
| 质控看板 | `quality` | `/qc/dashboard` | 院级质控驾驶舱 | ✅ READY |
| 质控预警 | `quality.Alert` | `/qc/alerts` | 质控预警 | ✅ READY |
| 评估指标 | `quality.Eval` | `/qc/eval/sets` | 评估指标库 | ✅ READY |
| 评估结果 | `quality.EvalResult` | `/qc/eval/results` | 评估结果 | ✅ READY |
| 评估报告 | `quality.EvalReport` | `/qc/eval/reports` | （从评估结果跳）| ✅ READY |
| AI 知识审核 | `knowledge` | `/aik/sources` | AI 知识审核 | ✅ READY |
| 知识包 | `knowledge.Package` | （未独立路由） | （AI 知识审核内）| ✅ READY |
| AI 候选审核 | `knowledge.AiCandidateReview` | `/aik/review` | （占位）| 🟡 前端占位 → **PR-V3-13** |
| 模型调用日志 | `dify.ModelGateway` | （未独立路由） | （AI 知识审核内）| ✅ READY |
| 医保智能审核 | （未实装） | `/qc/insurance` | （占位）| 🔵 PLANNED → **PR-V4-01** |
| **M4 · 平台底座** | | | **用户与身份 / 平台监控** | |
| 组织树 / 上下文 | `organization` | （header selector） | （全局） | ✅ READY |
| 用户管理 | `security` (SEC-001..007) | `/admin/users` | 用户管理 | 🔵 后端就绪 前端占位 → **PR-V3-05** |
| 身份绑定 | `security.IdentityBinding` | `/security/identity-binding` | 身份绑定管理 | ✅ READY |
| 用户同步 | `security.UserSync` + `security.usersync` 双副本 | （未独立路由） | （隐藏） | 🔴 **ADR-0006** 决策 + 归并 → **PR-V3-USERSYNC** |
| SSO | `security.sso` | `/sso-login` | （登录页 Tab）| ✅ READY |
| 安全基线 | `security.audit.SecurityAdmin` | `/security/baseline` | 安全基线 | ✅ READY |
| 患者主索引 | `patient` + `patientindex` 双副本 | `/mpi/patients` | 患者主索引 | 🔴 **ADR-0005** 决策 + 归并 + 实装 → **PR-V3-03** |
| 租户开通 | `tenant` | `/tenant/onboarding` | 租户开通 | 🔵 后端就绪 前端占位 → **PR-V3-04** |
| 审计日志 | `audit` + `security.audit` | `/admin/audit` | 审计日志 | 🔵 后端就绪 前端占位 → **PR-V3-06** |
| Provider 状态 | `system` | `/system/providers` | Provider 状态 | ✅ READY |
| 待办中心 | `workflow` | `/workflow/todos` | 待办中心 | 🔴 **全 Mock** → **PR-V3-WF**（KD-001） |
| 通知中心 | `notification` | `/notifications` | 通知中心 | ✅ READY |
| 通知设置 | `notification` | `/notifications/settings` | 通知设置 | ✅ READY |
| 数据治理 | `datagovernance` | `/api/data-governance` (无前端) | （AI 知识审核内）| 🟡 后端 ✅ 前端无独立入口 |
| 运维同步 | `ops.OpsSyncTask` | （未独立路由） | （隐藏 admin） | 🟡 后端 ✅ |
| 嵌入器配置 | `embed.EmbedConfig` | （集成方拿） | （无菜单）| ✅ READY |

**状态图例：** ✅ READY = 前后端均接通；🟡 = 后端就绪+前端占位（v0.3 必修）；🔵 = 后端就绪+前端无入口（v0.3 必修）；🔴 = 有重大缺陷（双副本 / 全 Mock）

### 1.3 立即清理：菜单 vs 路由 一致性 bug 4 个

| Bug | 当前现象 | v0.3 修复 |
|---|---|---|
| #1 | `/mpi/patients` 在 menuConfig 但 routes.tsx 无注册 → 点击 404 | **已修复**（本 PR commit）— 加 PlaceholderPage 占位，PR-V3-03 实装 |
| #2 | `/adapter/hub` 同上 | **已修复** — PR-V3-01 实装 |
| #3 | `/dify/workflows` 同上 | **已修复** — PR-V3-02 实装 |
| #4 | `/tenant/onboarding` 同上 | **已修复** — PR-V3-04 实装 |

---

## 2. 后端 Controller 最终归档（55 个 → 49 个目标）

实测后端有 **55 个 `@RestController`**（高于 V3 功能矩阵说的 50/53）。下面是裁决：

### 2.1 必须归并（6 个 → 3 个）

| 归并目标 | 当前文件 | 决策 |
|---|---|---|
| **保留 `patient/MpiController`**（17 端点完整方案） | `patient/MpiController` ✅ + `patientindex/controller/MpiController` ❌ | ADR-0005 决策保留 `patient/` 方案，删除 `patientindex/`；数据迁移见 ADR |
| **保留 `security/usersync/UserSyncApiController`**（source/task 模型，更新） | `security/UserSyncController` ❌ + `security/usersync/UserSyncApiController` ✅ | ADR-0006 决策保留 usersync/ 子包；删除 `security/UserSyncController` |
| **CDSS 4 Controller → 1 个** | `CdssController` + `ClinicalSafetyController` + `SafetyRedLineController` + `AlertFatigueController` + `CdssOverrideController` | 5 个 Controller 合并到 `cdss/` 一个 facade Controller + 内部 4 个 Service；URL 不变 → **PR-V3-CDSS-REFACTOR** |

### 2.2 必须重命名（4 个）

| 当前名 | 目标名 | 理由 |
|---|---|---|
| `system/HealthController` | `system/SystemController`（合并 health + providers + org-context） | 拆 3 个端点到 3 个 Controller 没必要 |
| `security/SsoController` | （并入 `security/sso/SsoConfigController`） | `security/SsoController` 与 `security/sso/SsoAuthController` 职责重叠 |
| `quality/QualityController` | `quality/QualityFacadeController` | 与 `QualityDashboardController` 区分（前者是综合/列表，后者是看板专用） |
| `dify/AiGovernanceController` | `quality/AiGovernanceController` | "AI 治理"属于 M3 质控范畴，不是 M1 Dify 范畴 |

### 2.3 保留但补 DTO（35 个 Controller 用 raw Map）

不能立即拆，但**禁止新增 raw Map**（verify-pr.ps1 §2.7 已拦），存量分批改：

| 优先级 | Controller 名单 | 理由 |
|---|---|---|
| **P0**（v0.3 必改） | `MpiController` `UserSyncApiController` `TenantOnboardingController` | 客户演示路径 + ADR 归并时一起做 |
| **P1**（v0.3 可改） | `CdssController` `IdentityBindingController` `SecurityBaselineController` `EvalController` `NotificationController` | 4 治理域核心 Controller |
| **P2**（v1.0 GA 前改） | 剩余 27 个 | 收口债务 |

### 2.4 内部域（不出现在 SideMenu）— 保留但需添加 README

| 包 | 用途 | 缺什么 |
|---|---|---|
| `audit` | 审计链服务，被其他模块写入 | 包级 `package-info.java` 描述职责 |
| `embed` | 临床嵌入器 WebSocket + 安全推送 | 同上 |
| `ops` | 运维同步任务（OPS-001/002）| 同上 |
| `organization` | 组织树 / 上下文 / Override | 同上 |
| `persistence` | 持久化基础设施（EnginePersistenceService 2175 行） | **拆分**到各领域，见 §4 |
| `common` / `util` | 通用层 | 增加 README 列举可复用工具 |

---

## 3. 双副本 ADR 决策（ADR-0005 / ADR-0006）

### ADR-0005 · MpiController 双副本归并 → 保留 `patient/MpiController` ✅ 已执行

**决策**：保留 `patient/MpiController`（`/api/v1/mpi/*` 17 端点完整方案），删除 `patientindex/controller/MpiController`（`/api/mpi/*` 7 端点不完整方案）。

**理由**：
1. `patient/` 实现是 MPI-001 第一手设计，覆盖入院登记 / 主索引归并 / 身份核验 / 就诊登记 / 冲突队列 / 冲突处置 6 个完整闭环
2. `patientindex/` 实现是后续重写，但只覆盖 7 个端点，缺业务深度
3. URL 风格 `/api/v1/mpi/*` 带版本号，更符合 RESTful 演进
4. `patient/MpiPersistenceService` 已在本 PR 修复 3 处 ON DUPLICATE KEY UPDATE，技术债清零

**实施**：✅ 由 PR-FINAL-02 完成（2026-05-21）
- 删除 `medkernel-mvp/src/main/java/com/medkernel/patientindex/` 整目录 8 个文件：
  - `controller/MpiController.java`（99 行）— 旧 `/api/mpi/*` 7 端点
  - `service/MpiService.java`（608 行）— 内存态 ConcurrentHashMap 实现
  - `entity/` 5 个实体：`MpiPatientIndexEntity` / `MpiEncounterEntity` / `MpiIdentifierMappingEntity` / `MpiIdentifierConflictEntity` / `MpiInsuranceSettlementEntity`（共 687 行 POJO）
  - `util/MpiHashUtil.java`（81 行）— SHA-256 + 盐字符串工具
  - **合计 1475 行**
- 删除测试 `medkernel-mvp/src/test/java/com/medkernel/patientindex/MpiApiContractTests.java`（与被删 API 一一对应）
- **`patient/` 包完整保留**：`MpiController` 17 端点 / `MpiService` / `MpiPersistenceService` 835 行 / `IdentityConflict` / `PatientIdentity` / `VisitIdentity`
- 前端 `frontend/src/api/mpi.ts` 0 引用 `/api/mpi/*`（仅前端路由 `/mpi/patients` 是占位，目标 URL，不是 API）→ **0 前端改动**
- DDL：旧 `patientindex` 0 张表定义在任何 DDL 中（grep 验证），无需 DEPRECATED 注释——比 ADR-0006 删得还干净

**关键确认**：旧 `patientindex/MpiService` 全程 ConcurrentHashMap 内存态，无任何 JDBC 调用（`grep PreparedStatement\|Connection\|DriverManager patientindex/` = 0），属于纯内存 demo 代码——删除 0 数据库影响 / 0 运行时影响。

**owner**：架构师 AI（Claude-Opus-4.7）  
**实际工时**：1 PR / 0.5 天（领单卡估时 2 天）

### ADR-0006 · UserSyncController 双副本归并 → 保留 `security/usersync/UserSyncApiController` ✅ 已执行

**决策**：保留 `security/usersync/UserSyncApiController`（source/task 模型，新设计），删除 `security/UserSyncController`（provider 模型，旧设计）。

**理由**：
1. `source/task` 模型与外部身份源（AD / LDAP / OIDC IdP / 第三方 HR）的实际同步语义更对齐 — 「数据源」+「同步任务」是双坐标
2. 旧 `provider` 模型与 SSO 配置的 provider 概念重名，造成 API 混淆
3. `usersync` 子包独立后，与 `security.sso` `security.audit` `security.IdentityBinding` 横向并列，包结构更清晰

**实施**：✅ 由 PR-FINAL-03 完成（2026-05-21）
- 删除 5 个 Java 文件：`security/UserSyncController.java` `UserSyncService.java` `UserSyncJob.java` `UserSyncDetail.java` `SyncReport.java`
- `security/IdentityProvider.java` 保留（SSO 共用，被 `SsoService` / `SsoController` 大量引用）
- `SecurityPersistenceService.java` 删 9 个旧方法：`createSyncJob` / `updateSyncJob` / `findSyncJobsByTenant` / `findSyncJobById` / `insertSyncDetails` / `findSyncDetailsByJobId`（×2 重载） / 私有 `mapSyncJob` / `mapSyncDetail`（净减 216 行）；IdentityProvider 相关 CRUD 完整保留
- 前端 `frontend/src/api` 0 引用 `/api/security/sync` → **0 前端改动**（旧 endpoint 从未被前端用过）
- DDL：`ai-dev-input/04_database/{local,pg,oracle,dm}/sec_*.sql` 共 4 个文件，`sec_user_sync_job` / `sec_user_sync_detail` 表定义保留并加 DEPRECATED 标记；真正 DROP 留给 PR-FINAL-25 Flyway 迁移
- 生产部署 `medkernel-mvp/src/main/resources/db/local/sec_user_sync_ddl.sql` 是**新** source/task/log 三表 DDL，本就独立——0 改动

**确认**：旧 UserSyncService 实际上是僵尸代码——`medkernel-mvp/src/main/resources/db/local/sec_ddl.sql`（生产部署 DDL）**从未建过** `sec_user_sync_job`/`sec_user_sync_detail` 表，任何对旧 API 的调用一定会 SQL 失败。删除 0 运行时影响。

**owner**：架构师 AI（Claude-Opus-4.7）  
**实际工时**：1 PR / 0.5 天（领单卡估时 1 天）

---

## 4. 数据领域聚合（拆分超长 Service）

**当前问题**：6 个 Service 超 1000 行（最重 EnginePersistenceService 2175 行管 12+ 表），单文件无法人脑覆盖，事故温床。

**目标**：按**领域驱动设计（DDD）的 Bounded Context** 拆分到 6 个独立的领域聚合根（aggregate root）。

| 当前文件 | 拆分目标 | 估时 |
|---|---|---|
| `persistence/EnginePersistenceService` (2175) | 拆为 6 个领域 Repository：<br>- `config/ConfigPackageRepository`（已存在）<br>- `pathway/PathwayInstanceRepository`<br>- `rule/RuleExecLogRepository`（已部分）<br>- `provenance/SourceDocumentRepository`<br>- `audit/AuditLogRepository`<br>- `common/IdAllocatorRepository`（雪花算法 + tenant 隔离） | 5 天 |
| `pathway/PathwayService` (1950) | 拆为：<br>- `pathway/PathwayTemplateService`（模板 CRUD）<br>- `pathway/PathwayInstanceService`（病人入径 / 节点流转）<br>- `pathway/PathwayValidator`（已存在，扩展）<br>- `pathway/PathwayConfigSupport`（已存在） | 3 天 |
| `rule/RuleService` (1923) | 拆为：<br>- `rule/RuleDefinitionService`（DSL 定义）<br>- `rule/RuleEngineService`（已部分，evaluate）<br>- `rule/RulePackageService`（包管理 + 发布）<br>- `rule/RuleActionLogService`（已存在，扩展） | 3 天 |
| `knowledge/KnowledgePackageService` (1334) | 拆为：<br>- `knowledge/KnowledgeSubscriptionService`<br>- `knowledge/KnowledgeSyncService`（已存在）<br>- `knowledge/KnowledgePackageBuilder`<br>- `knowledge/KnowledgePackageRepository` | 3 天 |
| `security/SecurityPersistenceService` (1178) | 拆为：<br>- `security/UserRepository`<br>- `security/RoleRepository`<br>- `security/PlatformUserRepository`<br>- `security/SecurityAuditRepository` | 3 天 |
| `graph/GraphService` (1030) | 拆为：<br>- `graph/GraphQueryService`（节点 / 边 / 候选）<br>- `graph/GraphSyncService`（已存在，Neo4j 同步）<br>- `graph/GraphVersionService`（版本管理） | 2 天 |

**总估时**：19 天（5 个并行 AI 可压缩到 1 周）

**禁止**：新文件 > 500 行（verify-pr 2.x 加规则）；新 Service 写在 `*PersistenceService` 集合文件里。

---

## 5. API 设计统一规范

### 5.1 URL 命名

| 部分 | 规则 | 示例 | 反例 |
|---|---|---|---|
| 前缀 | `/medkernel/api`（context-path 已设） | `/medkernel/api/...` | 不要 `/api/medkernel/...` |
| 版本 | 涉及业务模型变更的核心域用 `/v1`，平台底座不用 | `/api/v1/mpi/patients` | `/api/mpi/v1/patients` ❌ |
| 资源命名 | 名词复数 + kebab-case | `/api/config-packages` `/api/identity-bindings` | `/api/configPackage` ❌ `/api/config_packages` ❌ |
| 路径参数 | 业务编码 + 版本号（路径资源型）| `/api/pathways/templates/{code}/{version}` | 不要全用 UUID `id` |
| 查询参数 | snake_case（与 JSON 字段一致） | `?tenant_id=...&package_code=...` | `?tenantId=...` ❌ |

### 5.2 请求/响应

- **请求体**：DTO + `@Valid`，**禁止 `Map<String, Object>`**（verify-pr §2.7）
- **响应体**：`ApiResult<T>` 包装；**T 字段强制 snake_case**（Jackson `PropertyNamingStrategies.SNAKE_CASE` 必须启用，KD-005 v0.3 修）
- **错误码**：`ErrorCode` 枚举集中定义；HTTP status code 用 4xx/5xx 标准，业务错误码放 body
- **traceId**：所有响应必须有 `meta.trace_id`；全链路追踪用

### 5.3 标准响应模板

```json
{
  "success": true,
  "code": 0,
  "message": "ok",
  "data": { ... },
  "meta": {
    "trace_id": "trace-abc123",
    "request_id": "req-xyz456",
    "timestamp": "2026-05-21T15:00:00+08:00"
  }
}
```

错误响应：

```json
{
  "success": false,
  "code": "MISSING_SOURCE",
  "message": "资产缺少来源文档绑定（reference_document_code），不允许发布",
  "errors": [{"field": "reference_document_code", "severity": "ERROR"}],
  "meta": {"trace_id": "..."}
}
```

---

## 6. v1.0 GA 完成定义（七维度准入）

**v1.0 GA = General Availability**，意味着「可以签正式合同 + 商业化部署 + SLA 担保」。当前 v0.2-demo → v0.3-pilot → v1.0 GA 需要满足以下 7 个维度的全部硬指标：

| 维度 | 准入硬指标 |
|---|---|
| **D1 · 编译与测试** | ✅ mvn compile / mvn test / npm lint / npm test / npm build 全 PASS；覆盖率：后端 ≥ 70% / 前端核心路径 ≥ 60% |
| **D2 · 功能完整** | ✅ 4 治理模块 ≥ 27 个菜单项 100% 可达完整页面（无 PlaceholderPage 入口）；6 大演示剧本一键 fixture 加载后能完整跑完 |
| **D3 · 性能与并发** | ✅ HikariCP 接入（KD-004 关闭）；100 并发持续 30 min 无 connection leak；P95 < 300ms（核心 API） |
| **D4 · 合规与安全** | ✅ 等保 2.0 三级评测过；信通院国密评测过；ICP/公安备案；登录页 12 条国情合规清单 100%；JWT 不允许默认密钥；审计链验签 100% |
| **D5 · 跨数据库** | ✅ Oracle / 达梦 / KingbaseES / PostgreSQL 4 套 DDL 一致；smoke 在 4 套库各跑一次通过；H2 仅开发本地 |
| **D6 · 文档完备** | ✅ AI_CHARTER + PRODUCT_ARCHITECTURE_FINAL + 4 治理模块用户手册 + 部署运维手册 + API OpenAPI 3.0 文档（自动生成） |
| **D7 · 运维就绪** | ✅ 部署脚本（Linux .sh + Windows .ps1）跑通；监控（Prometheus + Grafana 看板）；备份恢复演练；升级回滚演练 |

**当前距离 GA 的缺口估算**：

| 维度 | 当前 | 缺口 | 工时 |
|---|---|---|---|
| D1 | 🟡 mvn ✅ npm 前端在本机跑不动 | 升 Node + CI 跑通 + 覆盖率达标 | 5 天 |
| D2 | 🟡 8 占位 + 11 路由 - 菜单不一致 | PR-V3-01..13 + 6 剧本 fixture | 25 天 |
| D3 | 🔴 DriverManager 29 处 / 无 HikariCP | 接 HikariCP + 拆 6 Service + 压测 | 25 天 |
| D4 | 🔴 国情合规 12+7 条几乎都没做 | PR-V3-COMPLIANCE + 等保评测 | 30 天（含评测周期） |
| D5 | 🟡 Oracle 主 / 4 套 DDL 在 / smoke 部分 | 跨库 smoke 全跑 | 10 天 |
| D6 | 🟡 已有 5 金本位 + 工程规范 30+ | 用户手册 + OpenAPI 自动化 | 10 天 |
| D7 | 🟡 部署脚本有 / 监控无 | 监控 + 备份/升级演练 | 15 天 |

**总估时**：约 **120 个人日**（含评测周期）。**5 AI 并行 + 3 人工评测协同 → 约 8-10 周交付 v1.0 GA**。

---

## 7. 重大决策汇总（ADR 已起草 / 待起草）

| ADR # | 主题 | 状态 |
|---|---|---|
| 0001 | 三产品分层架构 A/B/C | ✅ Accepted |
| 0002 | V2 PR 命名空间隔离（PR-V2-XX）| ✅ Accepted |
| 0003 | 禁止硬编码颜色 / 字号 / 间距 | ✅ Accepted |
| 0004 | 医学内容必须有来源 | ✅ Accepted |
| **0005** | **MpiController 双副本归并** → 保留 `patient/MpiController` | 🆕 本文 §3 决策，需起草正式 ADR 文件 |
| **0006** | **UserSyncController 双副本归并** → 保留 `security/usersync/` | 🆕 本文 §3 决策，需起草正式 ADR 文件 |
| **0007** | **四大模块命名最终统一**（禁止双命名） | 🆕 本文 §1 决策 |
| **0008** | **HikariCP 接入 + DataSource 注入** | 🆕 KD-004 关闭 |
| **0009** | **Jackson SNAKE_CASE 全局启用** | 🆕 KD-005 关闭 |
| **0010** | **单文件 / 单函数行数硬上限** + 持续拆分计划 | 🆕 与本文 §4 联动 |

---

## 8. 与既有文档的关系

| 文档 | 与本文关系 |
|---|---|
| [`docs/01_产品事实源.md`](01_产品事实源.md) | 产品**愿景**定义 — 本文不替代，本文是产品愿景**落地**的最终架构 |
| [`docs/04_页面规格书.md`](04_页面规格书.md) | 18 个目标页面规格 — 本文 §1.2 表是 27 菜单 ↔ 路由的最终对照，是 04 的工程化补充 |
| [`docs/05_AI实施手册.md`](05_AI实施手册.md) | 12 个 PR-V2-* 任务 — 已交付 v0.2-demo，本文之后用 PR-V3-* 命名空间继续 |
| [`docs/AI_CHARTER.md`](AI_CHARTER.md) | AI 必读 1 页纸 — 本文是宪法的**解释**和**具体指引**，宪法不变 |
| [`docs/v0.3-DEMO-REDESIGN.md`](v0.3-DEMO-REDESIGN.md) | v0.3 演示版重设计（用户体验 + 国情合规） — 本文与之**横向互补**，本文偏架构，那份偏体验 |
| [`docs/engineering/2026-05-21-roadmap-v0.3.md`](engineering/2026-05-21-roadmap-v0.3.md) | v0.3 工程债时间表（KD-001~007） — 本文 §6 GA 准入引用之 |
| [`docs/engineering/2026-05-21-功能矩阵-V3.md`](engineering/2026-05-21-功能矩阵-V3.md) | 当前能力快照 — 本文 §1.2 是「目标态」，矩阵是「现状」，对照看缺口 |
| [`docs/engineering/AUDIT-20260521-V3-基线核查.md`](engineering/AUDIT-20260521-V3-基线核查.md) | 本次核查报告 — 本文是核查后的最终白皮书 |
| [`docs/AI_TEAM_SOP.md`](AI_TEAM_SOP.md) | AI 团队执行 SOP — 本文是 What/Why，那份是 How |

---

## 9. 不变量（任何 AI 不得违反）

补充 22 不变量之外，本文新增 5 条架构不变量：

| # | 不变量 | 检测方式 |
|---|---|---|
| **A1** | **四大模块命名不许双命名**（§1.1 表） | `verify-pr.ps1` 加 grep 「配置治理 \| 运营治理 \| 治理控制台 \| 医疗智能引擎平台」（除 CHANGELOG / 本文 / 历史 ADR 外） |
| **A2** | **菜单与路由必须一一对应** | `verify-pr.ps1` 加脚本比对 `menuConfig.tsx items[].path` 全部在 `routes.tsx` 中能找到对应 element |
| **A3** | **新 Controller / 新 Service 必须先在 §1.2 表登记** | code review 检查；CI 跑 `scripts/check-controller-registry.ps1` |
| **A4** | **单文件 ≤ 500 行（新增）/ ≤ 800 行（存量豁免清单）** | `verify-pr.ps1` 加 wc -l 检测 |
| **A5** | **双副本不许扩散** — `MpiController` `UserSyncController` 任何一份被修改即提示「待 ADR-0005/0006 归并」 | `verify-pr.ps1` 加文件路径 grep |

---

**End of product architecture final whitepaper.**
**核心结论**：v0.2-demo 是「能力完整、演示可用」基线，v0.3-pilot 通过 13 个 PR-V3-* + 2 个 ADR 归并把架构债务收口，v1.0 GA 通过 D1-D7 七维度评测达成「可商业化」标准。**整个项目落地时间表**：v0.3 (8 周) → v1.0 GA (再 4 周) = **12 周到正式商业化**。
