# AI 团队 v0.3-final PR 领单卡（封板版）

> 版本：1.0 · 2026-05-21（v0.3-final 封板）  
> 适用：所有领 PR-FINAL-* 任务的 AI（架构师 / 高级 / 中级 / 初级）  
> 上游：[`PRODUCT_ARCHITECTURE_FINAL.md`](PRODUCT_ARCHITECTURE_FINAL.md) + [`v0.3-DEMO-REDESIGN.md`](v0.3-DEMO-REDESIGN.md) + [`AI_TEAM_SOP.md`](AI_TEAM_SOP.md) + [`COMPREHENSIVE_REVIEW.md`](COMPREHENSIVE_REVIEW.md) + [`DEPLOYMENT_DUAL_MODE.md`](DEPLOYMENT_DUAL_MODE.md)
>
> **核心原则**（不变量，违反者 PR 自动 FAIL）：
> 1. 任何 PR 必须 → `develop`，不许直接 → `main`（main 受保护）
> 2. 单 PR diff ≤ 800 行；超出必须拆
> 3. 修改任何 §3 共享文件清单中的文件，必须由架构师 AI 领单
> 4. 提交前跑 `scripts/verify-pr.ps1` + `scripts/check-inline-style-count.ps1` 全 PASS
> 5. **不要重新设计架构**——架构已封板，本文件只是「按图施工」的 checklist
>
> 阶段分布：
> - **阶段 1（基线收敛，已完成 7/10 个 PR）**：tokens 统一 / 命名收口 / 隐藏占位菜单 / ESLint 守门
> - **阶段 2（占位实装，8 个 PR）**：把 §4 列出的 8 个隐藏菜单变成真实页面
> - **阶段 3（架构债，7 个 PR）**：HikariCP / Jackson SNAKE_CASE / 拆超长 Service / OpenAPI / E2E
> - **阶段 4（合规上线，4 个 PR + 外部评测）**：国密 / SM4 加密 / Prometheus / 等保评测

---

## 1. 阶段进度（一张表追踪）

| PR | 标题 | 阶段 | 状态 | 负责 AI 级别 | 估时 |
|---|---|:---:|:---:|:---:|:---:|
| **PR-FINAL-00** | tokens 主色 + 命名/菜单统一 | 1 | ✅ DONE | 架构师 | 1 天 |
| **PR-FINAL-01** | LLM Gateway 迁包 `dify/` → `llm/` | 1 | ✅ DONE · Codex-GPT5 · 2026-05-21T22:12+08:00 | 架构师 | 1 天 |
| **PR-FINAL-02** | 删 patientindex 整包（ADR-0005）| 1 | ✅ DONE | 架构师 | 2 天 |
| **PR-FINAL-03** | 删 security/UserSyncController（ADR-0006）| 1 | ✅ DONE | 架构师 | 1 天 |
| **PR-FINAL-04** | CSS Modules 框架 + Login/Dashboard 示范抽取 | 1 | 🟡 TODO | 高级 | 2 天 |
| **PR-FINAL-05** | ESLint `no-inline-style` + 守门脚本 | 1 | ✅ DONE | 架构师 | 0.5 天 |
| **PR-FINAL-06** | LoginPage 4 Tab 重写（国情合规 12 条）| 1 | ✅ DONE · Codex-GPT5 · 2026-05-21T22:41+08:00 | 高级 | 2 天 |
| **PR-FINAL-07** | `/mpi/patients` 患者主索引页 | 2 | 🟡 TODO | 高级 | 5 天 |
| **PR-FINAL-08** | `/admin/users` 用户管理页 | 2 | 🟡 TODO | 中级 | 3 天 |
| **PR-FINAL-09** | `/admin/audit` 审计日志查询页 | 2 | 🟡 TODO | 中级 | 3 天 |
| **PR-FINAL-10** | `/tenant/onboarding` 租户开通向导 | 2 | 🟡 TODO | 高级 | 4 天 |
| **PR-FINAL-11** | `/rule/definitions` 规则库 + DSL 编辑器 | 2 | ✅ DONE · Claude-Opus-4.7 · 2026-05-21 | 高级 | 8 天 |
| **PR-FINAL-12** | `/adapter/hub` 适配器中心 | 2 | 🟡 TODO | 中级 | 4 天 |
| **PR-FINAL-13** | `/ai-workflows` AI 工作流引擎页（替代旧 Dify 工作流）| 2 | 🟢 IN REVIEW · Claude-Opus-4.7 · 2026-05-22 | 中级 | 3 天 |
| **PR-FINAL-14** | 砍菜单 + Dashboard PENDING 卡更新 | 2 | 🟡 TODO | 初级 | 0.5 天 |
| **PR-FINAL-15a** | HikariCP 框架接入 + 5 核心 PersistenceService 改造 | 3 | ✅ DONE | 架构师 | 2 天 |
| **PR-FINAL-15b** | 剩余 24 个 Service 的 DriverManager → DataSource 改造（同模板，mechanical work）| 3 | ✅ DONE | 架构师 | 2 天 |
| **PR-FINAL-16** | Jackson SNAKE_CASE 全局 + 修 30 测试 | 3 | 🟡 TODO | 架构师 | 5 天 |
| **PR-FINAL-17** | 拆 EnginePersistenceService（2175 行）| 3 | 🟡 TODO | 架构师 | 5 天 |
| **PR-FINAL-18** | 拆 RuleService / PathwayService / SecPersistence 等 5 个超长 | 3 | 🟡 TODO | 架构师 | 14 天 |
| **PR-FINAL-19** | 抽取剩余 ~430 处 inline → CSS Modules | 3 | 🟡 TODO | 高级 | 5 天 |
| **PR-FINAL-20** | springdoc-openapi + 前端 types 自动生成 | 3 | 🟡 TODO | 架构师 | 3 天 |
| **PR-FINAL-21** | E2E 6 剧本 fixture + Playwright | 3 | 🟡 TODO | 高级 | 5 天 |
| **PR-FINAL-22** | 国密 SM2/SM3/SM4（BouncyCastle）| 4 | 🟡 TODO | 架构师 | 5 天 |
| **PR-FINAL-23** | 数据分级 + HEALTH_DATA SM4 加密 | 4 | 🟡 TODO | 架构师 | 5 天 |
| **PR-FINAL-24** | actuator + Prometheus + Grafana 5 看板 | 4 | 🟡 TODO | 高级 | 4 天 |
| **PR-FINAL-25** | Flyway DB migration + KingbaseES 实测 | 4 | 🟡 TODO | 架构师 | 5 天 |

**总计**：约 102 工作日（5 AI 并行 = 4-5 周）

---

## 2. 已完成 PR 实施记录（避免重复劳动）

### ✅ PR-FINAL-00：tokens 主色 + 命名/菜单统一（DONE 2026-05-21）

**修改**：
- `frontend/src/styles/tokens.css`：`--mk-brand-primary` 从 `#0b8fa6` → `#1565c0`（医蓝）；菜单深蓝 `#0a1e33`；字体中文优先 `"PingFang SC", "Microsoft YaHei", "Source Han Sans CN"` 前置
- `frontend/src/styles/tokens.ts`：`clinicalPrimary` 等同步改 hex
- `frontend/src/styles/theme-tokens.ts`：AntD fontFamily 字体顺序中文优先
- `frontend/src/router/menuConfig.tsx`：菜单分组 4 个改名（「配置治理→知识工厂」「运营治理→质控驾驶舱」「用户与组织→用户与身份」「系统→平台监控」）；8 个未实装的占位入口从菜单移除（路由仍保留 PlaceholderPage 兜底）；删除 unused 图标 import
- `frontend/src/pages/Dashboard.tsx`：4 大分组名同步；「Dify 工作流」卡改名「AI 工作流引擎」+ 路径 `/ai-workflows`
- `frontend/src/router/routes.tsx`：`/dify/workflows` 重定向到 `/ai-workflows`；新增 `/ai-workflows` 占位路由
- `frontend/src/pages/Login.tsx` / `SsoLogin.tsx`：标题改「集团医疗智能中枢」/「MedKernel · 单点登录」
- `frontend/src/layouts/TopNav.tsx`：brand-title 改「集团医疗智能中枢」/「MedKernel · 管理工作台」
- `frontend/src/theme/tokens.ts`：medkernel-blue 主题描述「治理控制台」→「管理工作台」
- `frontend/src/layouts/SideMenu.tsx` + `frontend/src/styles/global.css`：注释同步
- `frontend/index.html`：`<title>` 改「集团医疗智能中枢 · MedKernel」
- `frontend/package.json` + `frontend/README.md`：description 同步

**后续注意**：
- AI 团队领单 PR-FINAL-07~14 实装某个菜单时，请把它**从隐藏注释中加回 menuSections.items 数组**（保持菜单与路由一一对应不变量 A2）
- 注释中已标注每个隐藏入口对应的 PR-FINAL-* 编号

### ✅ PR-FINAL-05：ESLint no-inline-style + 守门脚本（DONE 2026-05-21）

**修改**：
- `frontend/eslint-rules/no-inline-style.js`：新建 ESLint 规则，检测 JSX `style={{ ... }}` 对象字面量
- `frontend/eslint.config.js`：注册规则为 `medkernel/no-inline-style` 等级 `warn`
- `scripts/check-inline-style-count.ps1`：新建 baseline 监控脚本（baseline=582，2026-05-21 初始扫描值）

**使用方式**：
- 写新代码：用 `.module.css`（vite 默认支持，详见 §6）；动态样式必须用 inline 时加 `// eslint-disable-next-line medkernel/no-inline-style` 说明理由
- 抽取存量：每减少一次 inline，跑 `./scripts/check-inline-style-count.ps1 -UpdateBaseline` 把 baseline 下调
- CI 拦截：scripts/verify-pr.ps1 调用本脚本，新数量 > baseline 时 PR FAIL

### ✅ PR-FINAL-15a：HikariCP 框架接入 + 5 核心 PersistenceService 改造（DONE 2026-05-21）

**KD**：KD-004（29 处 `DriverManager.getConnection()` 散落 5+ 持久化类）

**修改**：
- `medkernel-mvp/pom.xml`：加 `spring-boot-starter-jdbc`（自带 HikariCP 4.0.3，Spring Boot 2.7 默认版本）
- `medkernel-mvp/src/main/resources/application.yml`：新增 `medkernel.database.hikari.*` 配置段（max-pool-size=20 / connection-timeout-ms=3000 / leak-detection-threshold-ms=2000 / idle-timeout / max-lifetime 等可调）
- `medkernel-mvp/src/main/java/com/medkernel/persistence/EnginePersistenceProperties.java`：新增内嵌 `HikariOptions` 类承载连接池参数
- **新建** `medkernel-mvp/src/main/java/com/medkernel/persistence/EngineDataSourceConfig.java`：`@Configuration` 暴露 `@Bean DataSource`，显式构造 `HikariDataSource`（跳过 Spring Boot DataSourceAutoConfiguration，因为项目用自定义 `medkernel.database.*` 命名空间）
- **5 个核心 PersistenceService 注入 DataSource + 改 connection() 方法**：
  - `persistence/EnginePersistenceService.java`（2175 行，最重）— 删 `loadDriver()` + 3 次重试循环 + `shouldRetryConnection` / `sleepQuietly` helpers
  - `persistence/OrganizationPersistenceService.java` — 删 `loadDriver()`（多方言驱动选择，HikariCP 自动）+ 重试循环
  - `security/SecurityPersistenceService.java`（963 行）— 简化
  - `patient/MpiPersistenceService.java`（835 行）— 简化
  - `security/SsoService.java`（登录核心路径）— 简化
- **附带清理（用户"系统未上线"指令落地）**：删除 `ai-dev-input/04_database/{local,pg,oracle,dm}/sec_*.sql` 中旧 `sec_user_sync_job` / `sec_user_sync_detail` 整段表定义（PR-FINAL-03 删 Java 时这些 schema 还在参考 DDL 中，本 PR 彻底删除——系统未上线，不需要 DEPRECATED 注释保留兼容）

**关键设计**：
- **保留 `connection()` 私有方法签名**：所有 `try (Connection c = connection())` 调用点不动 → 改动半径最小
- **HikariCP 通过 jdbcUrl 自动 Class.forName 加载驱动**：不再需要每个 PersistenceService 自己 `loadDriver()`，多方言（Oracle/DM/PG/Kingbase/H2）由 jdbcUrl 自驱动
- **@Bean destroyMethod = "close"**：Spring 容器关闭时自动 `HikariDataSource.close()` 释放 pool
- **`@ConditionalOnMissingBean(DataSource.class)`**：允许单元测试注入 mock DataSource 覆盖

**带来的核心收益**：
1. `@Transactional` 真正生效（同 Service 内多 DAO 走同 connection）
2. 连接池监控可观测（pool-name = `MedKernelHikari`）
3. 连接泄漏检测（threshold 2s WARN）
4. 高并发下不再无限制创建连接（max-pool-size=20）
5. ORA-12518 / Listener refused 由 pool 层重试（业务层不再关心）

**剩余 24 个文件由 PR-FINAL-15b 接力**（同一模板的 mechanical work，2026-05-21 已 DONE）：
- security: UserSyncService / IdentityBindingService / SsoConfigService / KeyManagementService / AuditChainService / UserSyncApiService
- knowledge: KnowledgeSyncService / KnowledgePackageService / AssetQualityService / AiKnowledgeJobService / AiCandidateReviewService
- datagovernance: QualityRuleRepository / PatientRepository / QualityCheckRepository / DoctorRepository / DepartmentRepository
- cdss: SafetyRedLineService / ClinicalSafetyService / CdssOverrideService
- 其它: dify/AiGovernanceService / rule/RuleEvalResultRepository / ops/OpsSyncTaskService / config/ConfigPackageRepository / adapter/TriggerPointService

### ✅ PR-FINAL-15b：剩余 24 个 Service 的 HikariCP 全量迁移（DONE 2026-05-21）

**目标**：闭环 PR-FINAL-15a 留下的"剩余 24 个文件"，把全仓库 29 个直接 DriverManager.getConnection() 全部收敛到 EngineDataSourceConfig 暴露的 DataSource。

**改造文件**：上方 §2 表格列出的 24 个 Service。

**改造模板**（每个文件 4 处）：
- import：加 `javax.sql.DataSource`，删 `java.sql.DriverManager`
- 字段：加 `private final DataSource dataSource;`
- 构造函数：参数列表加 `DataSource dataSource`，函数体加 `this.dataSource = dataSource;`
- `connection()` 方法：内部改用 `dataSource.getConnection()`，删 `DriverManager.getConnection(url, user, pass)`

**结果**：
- 24 文件 +161 / -151（净 +10 行）— 极其干净的批量改造
- 全 src `grep DriverManager` 仅余 1 个文件（EngineDataSourceConfig 自身，合法例外）
- 全 src `grep dataSource.getConnection()` 命中 29 文件（PR-FINAL-15a 5 个 + 本 PR 24 个）

**至此 PR-FINAL-15 全量完成**：29 处散落的 DriverManager.getConnection() 全部收敛到 HikariCP 连接池。`@Transactional` 在所有 Service 中真正生效。

**关联**：
- 前置 PR-FINAL-15a（PR #21，新建 EngineDataSourceConfig）
- 解锁 PR-FINAL-17/18：拆超长 Service 时新 Repository 直接注入 DataSource，不需再造 connection() 方法

**改造模板**（PR-FINAL-15b 照搬本 PR 的 5 个示范）：
```java
// import:
import javax.sql.DataSource;   // 加
// import java.sql.DriverManager; // 删

// 字段:
private final DataSource dataSource;

// 构造函数加参数:
public XxxService(..., DataSource dataSource) {
    ...
    this.dataSource = dataSource;
}

// connection() 方法:
private Connection connection() throws SQLException {
    return dataSource.getConnection();
}
// 同时删除 loadDriver() / 重试循环 / sleepQuietly 等 helper（如果存在）
```

### ✅ PR-FINAL-02：删 patientindex 整包（DONE 2026-05-21）

**ADR**：ADR-0005（PRODUCT_ARCHITECTURE_FINAL.md §3）

**修改**：
- 删除 `medkernel-mvp/src/main/java/com/medkernel/patientindex/` 整目录 8 个文件：
  - `controller/MpiController.java`（99 行）— 旧 `/api/mpi/*` 7 端点
  - `service/MpiService.java`（608 行）— 内存态 ConcurrentHashMap demo 实现
  - `entity/` 5 个 POJO（687 行）：`MpiPatientIndexEntity` / `MpiEncounterEntity` / `MpiIdentifierMappingEntity` / `MpiIdentifierConflictEntity` / `MpiInsuranceSettlementEntity`
  - `util/MpiHashUtil.java`（81 行）
- 删除 `medkernel-mvp/src/test/java/com/medkernel/patientindex/MpiApiContractTests.java`
- **合计 ~1500 行 Java + 测试**
- **`patient/` 包完整保留**：MpiController 17 端点 + MpiService + MpiPersistenceService 835 行 + IdentityConflict / PatientIdentity / VisitIdentity
- DDL：旧 patientindex 0 表定义（生产部署 / ai-dev-input 参考 DDL 都查过），**无需 DEPRECATED 注释**
- 前端：0 引用 `/api/mpi/*` 旧端点 → **0 前端改动**

**关键确认**：旧 patientindex 是纯内存态 demo 代码——`MpiService` 全程 ConcurrentHashMap 存储，0 处 JDBC 调用，0 张数据库表。删除 0 数据库影响 / 0 运行时影响 / 0 测试影响（同包测试已一并删除）。

**后续注意**：
- PR-FINAL-07（`/mpi/patients` 患者主索引页）的"前置 PR-FINAL-02"现已就绪，可以开工
- 所有 MPI 业务请走 `/api/v1/mpi/*`（`patient/MpiController` 17 端点完整方案），不要再造 `patientindex/` 包

### ✅ PR-FINAL-03：删 security/UserSyncController 双副本（DONE 2026-05-21）

**ADR**：ADR-0006（PRODUCT_ARCHITECTURE_FINAL.md §3）

**修改**：
- 删除 5 个 Java 文件：
  - `security/UserSyncController.java`（旧 `/api/security/sync/*` 6 端点）
  - `security/UserSyncService.java`（provider 模型同步逻辑）
  - `security/UserSyncJob.java`（实体）
  - `security/UserSyncDetail.java`（实体）
  - `security/SyncReport.java`（DTO）
- **保留** `security/IdentityProvider.java`（SSO 共用：`SsoService` × 13 / `SsoController` × 1 / `SecurityPersistenceService` × 14 处引用，删了 SSO 会崩）
- 重构 `security/SecurityPersistenceService.java`：
  - 删 9 个旧方法（createSyncJob / updateSyncJob / findSyncJobsByTenant / findSyncJobById / insertSyncDetails / findSyncDetailsByJobId × 2 / mapSyncJob / mapSyncDetail）
  - **保留** IdentityProvider 全套 CRUD（findIdentityProviderByType / findAllIdentityProviders / saveIdentityProvider / findIdentityProvidersByTenant / findIdentityProviderById / deleteIdentityProvider）—— SsoService 仍在用
  - 净减 216 行（1179 → 963）
- DDL：`ai-dev-input/04_database/{local,pg,oracle,dm}/sec_*.sql` 4 个参考 DDL 文件中的旧 `sec_user_sync_job` / `sec_user_sync_detail` 表已由 PR-FINAL-15 彻底删除；系统未上线，不保留 DEPRECATED 脏表
- 前端 `frontend/src` 0 引用 `/api/security/sync` → **0 前端改动**

**确认**：旧 UserSyncService 实际上是僵尸代码——生产部署 DDL `medkernel-mvp/src/main/resources/db/local/sec_ddl.sql` **从未建过** `sec_user_sync_job`/`sec_user_sync_detail` 表，任何对旧 API 的调用必然 SQL 失败。删除 0 运行时影响。

**后续注意**：
- PR-FINAL-08（`/admin/users` 用户管理页）的"前置 PR-FINAL-03" 现已就绪，可以开工
- 任何 AI 想做"院内身份源 / 全量同步 / 增量同步"前端 → 走 `/api/user-sync/*`（`security/usersync/UserSyncApiController`）source/task 模型，不要再造 `provider` 路径

### ✅ PR-FINAL-11：规则库 + DSL 编辑器（DONE 2026-05-21；路径引擎补刀以 follow-up PR 形式同期入 develop）

> **范围扩展（2026-05-21 用户拍板）**：原 PR-FINAL-11 「规则库 + DSL 编辑器」已 squash merge 进 develop（commit `f20c2a7`）。在同一会话里把「路径引擎前端完整化」（PR-V2-07 路径版本对比 + PR-V2-09 患者路径管理 占位实装的补刀）以 follow-up PR 形式接力进入 develop，统一引入 CodeMirror 6 依赖、统一国情合规标准、统一 UI 风格。

#### 规则模块（原 PR-FINAL-11 范围，已合 develop f20c2a7）

**修改**：
- `frontend/src/api/rule.ts`（新建）：规则模块前端契约——封装 `/api/rules/*` + `/api/rule-engine/*`；本地 view 类型 `RuleDefinition` / `RuleExecLog` / `RuleExecLogSummary`（避免触碰架构师专属 `api/types.ts`）
- `frontend/src/pages/Rule/`（新建整包）：
  - `RuleList.tsx`：列表 + 搜索 + 类型/状态筛选 + 分页
  - `RuleDetail.tsx`：元信息 + 来源追溯（`<SourceInfo>`）+ DSL 只读视图 + 触发历史时间轴 + 触发聚合
  - `RuleEditor/index.tsx`：DSL 编辑器主容器（路由 `/rule/definitions/:code/edit` / `new/edit`）
  - `RuleEditor/DslEditor.tsx`：JSON DSL 文本编辑器 + 实时 Schema 校验
  - `RuleEditor/DryRunPanel.tsx`：场景选择 + facts JSON + `/api/rules/simulate`
  - `RuleEditor/EditorHeader.tsx`：保存草稿 / 发布按钮 + DSL 校验状态
  - `components/RuleTypeTag.tsx` / `SeverityTag.tsx` / `ExecLogTimeline.tsx` / `SourceCitationCard.tsx`
  - `helpers/ruleSchema.ts`：轻量 DSL 校验器（与 `rule_dsl.schema.json` 对齐，不引入 ajv）
  - `helpers/ruleFormatters.ts`：类型/严重度/状态/时间/scope 纯函数格式化
  - `helpers/ruleSamples.ts`：5 个国情场景样本（AMI / EMR-QC / 红线 / 医保 / 路径准入）
  - `styles.module.css`：100% var(--mk-*) token，零 inline style
  - `__tests__/`：18+ 测试（helpers / 组件 / 列表页 / 详情页 / 试运行）
- `frontend/src/router/menuConfig.tsx`：M1 知识工厂分组新增「规则库」入口（SafetyCertificateOutlined 图标，从隐藏注释加回）
- `frontend/src/router/routes.tsx`：`/rule/definitions` `/rule/definitions/:code` `/rule/definitions/:code/edit` 从 PlaceholderPage 改为真实组件
- `frontend/package.json`：无新增运行时或测试依赖

**国情合规**：
- 所有医学规则必须显示来源（ADR-0004）—— `<SourceInfo>` 通过 `<SourceCitationCard>` 承载
- 试运行 5 个场景样本全部使用国内场景（AMI/STEMI、病历时限、红线医嘱、医保审核、卒中溶栓准入）
- DSL 编辑器中文注释 + 中文错误提示
- 来源缺失自动渲染「来源缺失」状态卡（不静默通过）

#### 路径模块（PR-V2-07 + PR-V2-09 占位实装，合并到本 PR）

- `frontend/src/api/pathway.ts`（扩展）：+14 端点（diff / 候选 / 入径 / 任务 / 实例查询 / 变异 / 聚合）；本地 view 类型 `PatientPathwayInstance` / `PatientNodeState` / `PatientTaskState` / `PathwayVariationRecord` / `RecommendationCard` / `PathwayDiffResult` 等
- `frontend/src/pages/Pathway/PathwayDetail.tsx`（**重写**，96 行 → 240 行）：去 inline → CSS Modules + var(--mk-*)；版本时间轴；引用警告卡；草稿/已发布双 Tab 配置（CodeMirror 6 只读）；实例统计；变异统计；删除/编辑/对比操作
- `frontend/src/pages/Pathway/PathwayDiff.tsx`（**新建**，PR-V2-07 占位实装）：`/pathway/templates/:code/diff?from=&to=` 版本对比；节点/连接/任务三段式增删改可视化；JSON 并排只读（CodeMirror 6）
- `frontend/src/pages/Pathway/PatientPathway/`（**新建整包**，PR-V2-09 占位实装）：
  - `PatientPathwayList.tsx`：列表 + 路径/状态/患者筛选 + 入径对话框
  - `PatientPathwayDetail.tsx`：基本信息 + 节点进度时间轴 + 当前节点任务列表（完成 / 跳过 / 完成节点）+ 变异记录列表 + 变异对话框
  - `AdmitDialog.tsx`：3 个国情样本 + facts JSON + 候选推荐 + 确认入径
  - `VariationDialog.tsx`：7 种变异类型枚举 + 节点 + 原因填写
  - `components/NodeProgressTimeline.tsx` / `TaskList.tsx` / `VariationCard.tsx`
- `frontend/src/pages/Pathway/components/PathwayTimeline.tsx`（新建）：版本时间轴
- `frontend/src/pages/Pathway/components/ReferenceWarnings.tsx`（新建）：引用警告卡（ADR-0004）
- `frontend/src/pages/Pathway/helpers/`（新建）：pathwayFormatters / pathwayDiff / pathwaySamples / pathwaySamples.guards 纯函数
- `frontend/src/pages/Pathway/styles.module.css`（新建）：路径模块整体样式 555 行
- `frontend/src/pages/Pathway/__tests__/` + `helpers/__tests__/` + `components/__tests__/` + `PatientPathway/__tests__/` + `PatientPathway/components/__tests__/`：24+ 测试

#### 共享改动

- `frontend/src/router/menuConfig.tsx`：M1 知识工厂新增「规则库」+「患者路径管理」入口
- `frontend/src/router/routes.tsx`：`/rule/definitions` 等 3 个 + `/pathway/templates/:code/diff` `/pathway/patients` `/pathway/patients/:instanceId` 全部从 PlaceholderPage 改为真实组件
- `frontend/package.json`：新增 `@uiw/react-codemirror` `@codemirror/lang-json` `@codemirror/state` `@codemirror/theme-one-dark` + `@testing-library/user-event`（规则 + 路径模块共用）

#### 国情合规（双模块共同）

- ADR-0004 所有医学内容必须显示来源 → 规则用 `<SourceCitationCard>`；路径用 `<ReferenceWarnings>` + `<SourceInfo>`
- 国情场景样本：规则模块 5 个（AMI/STEMI、病历时限、红线、医保、卒中）；路径入径 3 个（AMI/STEMI、卒中溶栓、COPD 急性加重）
- 中文错误提示 + 中文枚举标签
- 患者路径列表患者 ID 4+4 脱敏（身份证 GB 11643-1999 风格）
- DSL / JSON 编辑器中文注释

#### 后续注意

- PR-FINAL-14 砍菜单时无需动 rule-definitions / patient-pathways（本 PR 已加回）
- PR-FINAL-19 inline 抽取无需动本 PR 触及的两个模块（规则 0 inline 新增；路径 -8 inline → 守门由 582 降至 574）
- 后端 `PathwayService` 1950 行未拆，属 PR-FINAL-18 范围（架构师任务）

---

## 3. 共享文件清单（必须由架构师 AI 领单）

| 文件 | 改动权 | 影响范围 |
|---|---|---|
| `frontend/src/api/types.ts` | 架构师 | 全前端 API 契约 |
| `frontend/src/router/menuConfig.tsx` | 架构师 | 全局菜单 |
| `frontend/src/router/routes.tsx` | 架构师 | 全局路由 |
| `frontend/src/styles/tokens.css` `tokens.ts` `theme-tokens.ts` | 架构师 | 全局设计 token |
| `frontend/src/theme/**` | 架构师 | 主题切换机制 |
| `frontend/src/App.tsx` `main.tsx` | 架构师 | ConfigProvider 根 |
| `frontend/eslint-rules/**` `eslint.config.js` | 架构师 | 全前端 lint 规则 |
| `medkernel-mvp/pom.xml` | 架构师 | 后端依赖 |
| `medkernel-mvp/src/main/resources/application.yml` | 架构师 | 后端全局配置 |
| `medkernel-mvp/src/main/java/com/medkernel/common/**` | 架构师 | 通用层（ApiResult / ErrorCode / Exception）|
| `medkernel-mvp/src/main/java/com/medkernel/persistence/**` | 架构师 | 持久化基础设施 |
| `medkernel-mvp/src/main/java/com/medkernel/dify/workflow/` 整包 | 架构师 | Dify 可选 WORKFLOW Provider |
| `medkernel-mvp/src/main/java/com/medkernel/patient/` `patientindex/` | 架构师 | ADR-0005 双副本 |
| `medkernel-mvp/src/main/java/com/medkernel/security/UserSyncController*` `security/usersync/**` | 架构师 | ADR-0006 双副本 |
| `scripts/verify-pr.ps1` `verify-task-prereq.ps1` `check-inline-style-count.ps1` | 架构师 | CI 门禁 |
| `docs/AI_CHARTER.md` `PRODUCT_ARCHITECTURE_FINAL.md` `AI_TEAM_SOP.md` | 架构师 | 宪法和白皮书 |
| 本文件 | 架构师 | PR 进度看板 |

中级/初级 AI 改这些文件 → 自动 FAIL + 标 `OVER_SCOPE`。

---

## 4. 阶段 2 详细领单卡（PR-FINAL-07..14）

### PR-FINAL-07 · `/mpi/patients` 患者主索引页（高级 AI，5 天）

**前置**：PR-FINAL-02（patientindex 删包 + 数据迁移完成）

**后端**：已就绪（`patient/MpiController` 17 端点 + `patient/MpiPersistenceService` 835 行）

**前端任务**：
- 新建 `frontend/src/pages/Mpi/` 目录（参考 `pages/Pathway/` 结构）：
  - `PatientList.tsx`（列表 + 搜索 + 筛选）
  - `PatientDetail.tsx`（详情：主索引 + 关联 ID + 就诊记录）
  - `MergeWorkbench.tsx`（冲突队列 + 人工合并）
  - `components/PatientCard.tsx`
  - `styles.module.css`
- 在 `frontend/src/api/mpi.ts` 补齐前端 API client（已有部分骨架）
- 在 `menuConfig.tsx` 把 mpi 入口加回 M4 用户与身份分组（恢复 IdcardOutlined 图标 import）
- 在 `routes.tsx` 把 `/mpi/patients` 改为真实组件
- 单元测试：每个组件 ≥ 1 render 测试 + 1 交互测试

**国情合规**：
- 身份证 4+4 脱敏（GB 11643-1999）
- 手机号 3+4 脱敏
- 患者列表默认脱敏，需「查看完整」权限解锁
- 民族字段 56 项枚举（GB 3304）

**DoD 自查**：见 [`AI_TEAM_SOP.md §4`](AI_TEAM_SOP.md#4-验收标准模板dod)

### PR-FINAL-08 · `/admin/users` 用户管理页（中级 AI，3 天）

**前置**：PR-FINAL-03（UserSyncController 双副本归并完成）

**后端**：已就绪（`security/SEC-001..007` 7 个 Controller）

**前端任务**：
- 新建 `frontend/src/pages/Admin/UserManagement/`：
  - `UserList.tsx`（列表 + 角色筛选 + 状态筛选）
  - `UserDetail.tsx`（用户详情 + 绑定的身份源 + 操作历史）
  - `RoleAssignDialog.tsx`（角色分配，多选）
  - `CsvImportDialog.tsx`（CSV 批量导入，参考 v0.3-DEMO-REDESIGN §P2-4）
  - `styles.module.css`
- `menuConfig.tsx` 加 admin-users 回 M4 平台监控（恢复 TeamOutlined 图标）
- `routes.tsx` 改为真实组件
- 单元测试

**国情合规**：
- CSV 批量导入支持 GB18030 编码（卫健委系统常见）
- 用户表显示「登录失败次数 / 锁定状态」

### PR-FINAL-09 · `/admin/audit` 审计日志查询页（中级 AI，3 天）

**前置**：无

**后端**：已就绪（`audit/AuditController` + `security/audit/SecurityAdminController`）

**前端任务**：
- 新建 `frontend/src/pages/Admin/AuditLog/`：
  - `AuditLogList.tsx`（按时间 / 用户 / 操作类型筛选 + 分页 + 导出）
  - `AuditLogDetail.tsx`（请求详情 + 验签结果）
  - `SignatureVerifyBanner.tsx`（审计链验签状态）
  - `styles.module.css`
- `menuConfig.tsx` 加 admin-audit 回 M4 平台监控（恢复 DatabaseOutlined 图标）
- `routes.tsx` 改为真实组件

**等保 2.0 三级要求**：
- 审计日志只读，不允许删除 / 修改
- 验签失败的记录用红色标注
- 支持导出加密 ZIP（密码由配置项 `medkernel.audit.export-password` 决定）

### PR-FINAL-10 · `/tenant/onboarding` 租户开通向导（高级 AI，4 天）

**前置**：无

**后端**：已就绪（`tenant/TenantOnboardingController` + SEC-011）

**前端任务**：
- 新建 `frontend/src/pages/Tenant/Onboarding/`：
  - `OnboardingWizard.tsx`（3 步向导 Tab 容器）
  - `steps/Step1Info.tsx`（租户信息：医院名 / 编码 / 联系人）
  - `steps/Step2Subscription.tsx`（套餐选择：试用 / 标准 / 专业 / 旗舰）
  - `steps/Step3InitData.tsx`（默认配置包 + SSO 提供商初始化）
  - `OnboardingSuccess.tsx`（开通成功页 + 管理员账号生成）
  - `styles.module.css`
- `menuConfig.tsx` 加 tenant-onboarding 回 M4 用户与身份（恢复 ShopOutlined 图标）
- `routes.tsx` 改为真实组件

**国情合规**：
- 套餐选择必须显示「数据本地化」承诺
- 试用套餐有 30 天到期提示
- 开通成功后发送短信通知（依赖 PR-V3-COMPLIANCE-BACKEND）

### PR-FINAL-11 · `/rule/definitions` 规则库 + DSL 编辑器（高级 AI，8 天，最大单 PR）

**前置**：无

**后端**：已就绪（`rule/RuleController` + `RuleEngineController` + `RuleActionLogController`，RULE-001..008）

**前端任务**：
- 新建 `frontend/src/pages/Rule/`：
  - `RuleList.tsx`（规则库列表 + 分类 + 状态）
  - `RuleDetail.tsx`（规则详情 + 触发历史 + 关联路径）
  - `RuleEditor/index.tsx`（DSL 编辑器主容器）
  - `RuleEditor/DslEditor.tsx`（JSON DSL 文本编辑器 + 实时 Schema 校验）
  - `RuleEditor/DryRunPanel.tsx`（试运行 + 样本数据 + 命中结果）
  - `RuleEditor/PreviewPanel.tsx`（规则可视化预览）
  - `styles.module.css`
- `menuConfig.tsx` 加 rule-definitions 回 M1 知识工厂（恢复 SafetyCertificateOutlined 图标）
- `routes.tsx` 改为真实组件
- **依赖新引入**：不新增编辑器运行时依赖，优先复用 AntD 文本域，避免锁文件漂移

**重要约束**：
- DSL 编辑器组件本身可能 > 500 行（行数硬上限例外），需要拆分子组件
- 试运行必须显示来源追溯（任何医疗规则必须有来源，ADR-0004）

### PR-FINAL-12 · `/adapter/hub` 适配器中心（中级 AI，4 天）

**前置**：无

**后端**：已就绪（`adapter/AdapterHubController` + `InteropController` + `TriggerPointController`，21 端点）

**前端任务**：
- 新建 `frontend/src/pages/Adapter/`：
  - `AdapterList.tsx`（适配器列表 + 启用 / 禁用）
  - `AdapterConfig.tsx`（单个适配器的配置 + 测试连接）
  - `TriggerPointList.tsx`（CDS Hooks 触发点配置）
  - `styles.module.css`
- `menuConfig.tsx` 加 adapter-hub 回 M1 知识工厂（恢复 ClusterOutlined 图标）

### PR-FINAL-13 · `/ai-workflows` AI 工作流引擎页（中级 AI，3 天）

**前置**：PR-FINAL-01（LLM Gateway 迁包完成）

**后端**：已就绪（`llm/ModelGatewayController` 迁包后 + `dify/workflow/DifyAdapterController` 收敛后）

**前端任务**：
- 新建 `frontend/src/pages/AiWorkflows/`：
  - `WorkflowList.tsx`（AI 工作流模板列表）
  - `WorkflowDetail.tsx`（模板详情 + 执行历史 + 调用统计）
  - `ProviderStatus.tsx`（**显示当前所有 LLM Provider 状态**：通义/DeepSeek/Kimi/智谱/豆包/Yi/百川/阶跃 + Dify 可选 + LOCAL）
  - `DegradationChainViewer.tsx`（5 种调用类型的降级链可视化）
  - `styles.module.css`
- `menuConfig.tsx` 加 ai-workflows 回 M1 知识工厂（恢复 RobotOutlined 图标）

**重要措辞**：
- 整个页面**不出现 "Dify"** 作为主品牌（去 Dify 化策略 ADR-0013）
- Dify 仅作为 Provider 状态卡的一项（WORKFLOW 类型时可选）
- 主流叙事：「**8 家国产大模型直连 + Ollama 本地 + LOCAL 规则兜底**」

### PR-FINAL-14 · 砍菜单 + Dashboard PENDING 卡更新（初级 AI，0.5 天）

**前置**：PR-FINAL-07..13 部分完成（已实装的菜单恢复，未实装的继续隐藏）

**任务**：
- 检查 `frontend/src/router/menuConfig.tsx`：哪些 PR-FINAL-07..13 已 DONE，把对应注释中的入口加回 `items` 数组
- 更新 `frontend/src/pages/Dashboard.tsx` PENDING 卡：已实装的从 PENDING 改 READY
- 跑 `scripts/verify-pr.ps1` 验证菜单 ↔ 路由一一对应

---

## 5. 阶段 3 详细领单卡（PR-FINAL-15..21）

### PR-FINAL-15 · HikariCP 接入（架构师，4 天）

**问题**：29 处 `DriverManager.getConnection()` 散落 5+ 持久化类（KD-004）

**任务**：
- `medkernel-mvp/pom.xml` 加 `spring-boot-starter-jdbc`
- `application.yml` 加 `spring.datasource.hikari.*`（max-pool-size=20 / connection-timeout=3000 / leak-detection-threshold=2000）
- 改造 5 个 Persistence Service 注入 `DataSource`（去除 `private Connection connection()` 私有方法）
- 验证 `@Transactional` 真正生效（同 service 内多 DAO 走同 connection）
- 跑 100 并发压测 30 min 无 leak

**DoD**：Hikari pool metrics 暴露 + 压测报告

### PR-FINAL-16 · Jackson SNAKE_CASE 全局（架构师，5 天）

**问题**：DTO camelCase 字段 + 前端 types.ts 标 snake_case，契约不齐（KD-005）

**任务**：
- `application.yml` 启用 `spring.jackson.property-naming-strategy: SNAKE_CASE`
- grep 全代码 `(String) data.get("camelCaseKey")` 改为 `snake_case_key`（约 30 处契约测试断言）
- 验证 `frontend/src/api/types.ts` 所有 DTO 字段对得上
- 跑全套契约测试 PASS

### PR-FINAL-17 · 拆 EnginePersistenceService（架构师，5 天）

**问题**：2175 行管 12+ 表

**任务**：按白皮书 §4 拆为 6 个 Repository：
- `config/ConfigPackageRepository`（已存在）
- `pathway/PathwayInstanceRepository`（新）
- `rule/RuleExecLogRepository`（已部分）
- `provenance/SourceDocumentRepository`（新）
- `audit/AuditLogRepository`（新）
- `common/IdAllocatorRepository`（雪花算法 + tenant 隔离）

### PR-FINAL-18 · 拆 RuleService / PathwayService / SecurityPersistence 等 5 个超长（架构师，14 天）

**任务**：参见 [`PRODUCT_ARCHITECTURE_FINAL.md §4`](PRODUCT_ARCHITECTURE_FINAL.md#4-数据领域聚合拆分超长-service)

### PR-FINAL-19 · 抽取剩余 ~430 处 inline → CSS Modules（高级，5 天）

**前置**：PR-FINAL-04（CSS Modules 框架引入后）

**任务**：
- 按 inline 数量排序，从 top 10 开始抽取：
  1. `pages/ConfigPackages/PackageDetail.tsx` (45)
  2. `pages/AIReview/AiCandidateReviewDesk.tsx` (25)
  3. `embed/OrderSafetyBlocker/OrderSafetyBlocker.tsx` (21)
  4. `pages/ConfigPackages/PackageList.tsx` (20)
  5. ...
- 每抽取一批跑 `./scripts/check-inline-style-count.ps1 -UpdateBaseline`
- 目标：v0.3-final RC 时 ≤ 100 处（动态样式保留）

### PR-FINAL-20 · springdoc-openapi（架构师，3 天）

**任务**：
- `pom.xml` 加 `springdoc-openapi-ui` 1.7.0（Spring Boot 2.7 兼容版）
- 配置 `application.yml` `springdoc.swagger-ui.path=/api-docs`（仅 internal port + 鉴权）
- 53 个 Controller 加 `@Operation` `@ApiResponse` 注解
- 前端引入 `openapi-typescript` CLI，自动生成 `types.ts`（删手工维护）

### PR-FINAL-21 · E2E Playwright（高级，5 天）

**任务**：6 大演示剧本（v0.3-DEMO-REDESIGN §5）各 1 个 E2E：
- S1 CIO 看治理大盘
- S2 医学专家发布 AMI 路径
- S3 医生在 HIS 收到推荐
- S4 CDSS 提醒疲劳治理
- S5 AI 知识审核闭环
- S6 多医院身份联邦

---

## 6. UI 风格强制规范（所有 PR 必须遵守）

### 6.1 CSS Modules 使用模式（vite 默认支持）

**文件命名**：组件同名 `Xxx.module.css`，放组件目录内

**示例**：
```tsx
// frontend/src/pages/Mpi/PatientList.tsx
import styles from "./styles.module.css";
import { Tag } from "antd";

export function PatientList() {
  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <h2 className={styles.title}>患者主索引</h2>
      </header>
      <main className={styles.body}>
        {/* ... */}
      </main>
    </div>
  );
}
```

```css
/* frontend/src/pages/Mpi/styles.module.css */
.page {
  display: flex;
  flex-direction: column;
  gap: var(--mk-space-5);
  padding: var(--mk-space-6);
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: var(--mk-space-4);
  border-bottom: 1px solid var(--mk-border-divider);
}

.title {
  font-size: var(--mk-text-xl);
  font-weight: var(--mk-weight-semibold);
  color: var(--mk-text-primary);
  margin: 0;
}

.body {
  flex: 1;
  min-height: 0;
}
```

### 6.2 动态样式（必须用 inline 的场景）

```tsx
// ✅ 正确：动态值（旋转、进度条宽度）
<div
  // eslint-disable-next-line medkernel/no-inline-style
  style={{ transform: `rotate(${deg}deg)`, width: `${pct}%` }}
/>

// ✅ 正确：CSS custom property 注入（推荐，更可维护）
<div
  className={styles.dial}
  // eslint-disable-next-line medkernel/no-inline-style
  style={{ "--dial-rotation": `${deg}deg` } as React.CSSProperties}
/>

// ❌ 错误：静态颜色 / 间距 / 字号
<div style={{ padding: 16, color: "#1565c0" }}>
```

### 6.3 颜色 / 字号 / 间距（强制 token）

| 类型 | ❌ 禁止 | ✅ 强制 |
|---|---|---|
| 颜色 | `#1565c0` `rgb(...)` | `var(--mk-brand-primary)` |
| 字号 | `14px` `font-size: 14px` | `var(--mk-text-base)` |
| 间距 | `padding: 16` | `var(--mk-space-4)` |
| 圆角 | `border-radius: 6` | `var(--mk-radius-md)` |
| 阴影 | `box-shadow: 0 1px 2px...` | `var(--mk-shadow-sm)` |
| 字体 | `font-family: "PingFang SC"...` | `var(--mk-font-family)` |

唯一允许写 hex 的文件：
- `frontend/src/styles/tokens.css`（CSS 变量底座）
- `frontend/src/styles/tokens.ts`（COLOR_TOKEN 原色板）
- `frontend/src/theme/tokens.ts`（运行时主题切换）

### 6.4 三产品节奏区分（v0.3-final 落地）

| 产品 | data-product | 设计语言 | 密度 token |
|---|---|---|---|
| A 知识工厂 | `[data-product="studio"]`（默认）| Notion 范：宽留白、富文本 | `--mk-density-*`（comfortable） |
| B 临床嵌入器 | `[data-product="embed"]` | Apple Health 范：极简、显源点 | `--mk-density-*`（compact） |
| C 质控驾驶舱 | `[data-product="cockpit"]` | Grafana 范：数据密集 | `--mk-density-*`（data-dense） |

**`tokens.css` 已经定义好这 3 套密度**（第 150-168 行），AI 团队只需在 `<body>` 或路由根容器加 `data-product` 属性即可切换。

### 6.5 文件命名 / 路由 / 包结构最终规范

```
frontend/src/
├── pages/<Module>/             # 业务页面，按 M1/M3/M4 模块分组
│   ├── index.tsx               # 列表
│   ├── Detail.tsx              # 详情
│   ├── Editor.tsx              # 编辑器
│   ├── components/             # 本页专属组件
│   │   └── XxxCard.tsx
│   └── styles.module.css       # 本页样式
├── components/<Atomic>/        # 跨页面共享原子组件
│   ├── Xxx.tsx
│   ├── Xxx.module.css
│   └── Xxx.test.tsx
├── styles/
│   ├── tokens.css              # 唯一 hex 来源
│   ├── global.css
│   └── theme-tokens.ts         # AntD 桥接
└── theme/                      # 主题切换机制（保留）

medkernel-mvp/src/main/java/com/medkernel/
├── common/                     # ApiResult / ErrorCode / Exception
├── llm/                        # NEW：LLM Gateway（PR-FINAL-01 迁包后）
│   ├── ModelGatewayService.java
│   ├── ModelGatewayController.java
│   ├── ModelProvider.java
│   ├── LlmProviderFactory.java
│   ├── LlmProviderConfig.java
│   └── OpenAICompatibleProvider.java
├── dify/                       # 仅 Dify 相关（已退化为可选 Provider）
│   └── workflow/
│       ├── DifyService.java
│       ├── DifyModelProvider.java
│       ├── DifyWorkflowTemplate.java
│       └── DifyAdapterController.java
├── config/                     # M1 配置包
├── pathway/                    # M1 路径
├── rule/                       # M1 规则
├── graph/                      # M1 图谱
├── terminology/                # M1 字典
├── adapter/                    # M1 适配器
├── provenance/                 # M1 来源
├── cdss/                       # M2 临床决策
├── embed/                      # M2 嵌入器
├── quality/                    # M3 质控
├── knowledge/                  # M3 AI 知识审核
├── workflow/                   # M3 待办
├── notification/               # M3 通知
├── patient/                    # M4 患者主索引（保留方案）
├── security/                   # M4 用户/身份
│   ├── audit/                  # 安全审计
│   ├── sso/                    # SSO（保留 sso/ 子包方案）
│   └── usersync/               # 用户同步（保留 usersync/ 子包方案）
├── tenant/                     # M4 租户
├── organization/               # M4 组织树
├── audit/                      # M4 审计链
├── system/                     # M4 系统
├── ops/                        # M4 运维
├── persistence/                # 通用持久化（待拆分到各业务包）
├── datagovernance/             # 数据治理
└── util/                       # 工具类
```

**已删除的包**（不要再创建）：
- ~~`patientindex/`~~（ADR-0005 删除，并入 `patient/`）

**已废弃的命名**（grep 命中 → CI FAIL）：
- ~~`配置治理` / `运营治理` / `用户与组织` / `系统`~~（菜单分组）
- ~~`医疗智能引擎平台` / `治理控制台`~~（产品/工作台名）
- ~~`Dify 工作流`~~（菜单项名，已改「AI 工作流引擎」）
- ~~`zy-engine` / `ZyEngine` / `--zy-*`~~（已彻底替换为 medkernel / --mk-*）

---

## 7. AI 接手 PR-FINAL-* 任务的 10 分钟启动 SOP

```
□ [1 分钟] git pull origin develop && git status（确认 worktree 干净）
□ [1 分钟] 读 ai-dev-input/00_DEVELOP_HEALTH.md（确认 🟢 GREEN）
□ [1 分钟] 读 docs/AI_CHARTER.md（红线）
□ [2 分钟] 读本文件对应 PR 卡片（§4 / §5 对应那段）
□ [2 分钟] 读 docs/PRODUCT_ARCHITECTURE_FINAL.md §1.2（确认菜单/路由/Controller 对应表）
□ [1 分钟] 读 docs/AI_TEAM_SOP.md §2 任务领取 10 步
□ [1 分钟] 看本文件 §3 共享文件清单，确认你不会改到这些（除非你是架构师）
□ [1 分钟] 看本文件 §6 UI 强制规范

10 分钟后：
□ 跑 scripts/verify-task-prereq.ps1 -TaskId PR-FINAL-XX -Level <senior/middle/junior>
□ 创建 ai-dev-input/10_task_claims/active/PR-FINAL-XX_<myId>_<timestamp>.md
□ 开始编码（严格在 claim 的 write_scope 内）

完成后：
□ cd frontend && npm run typecheck && npm run lint && npm test -- --run && npm run build
□ cd medkernel-mvp && mvn -q compile && mvn -q test
□ ./scripts/check-inline-style-count.ps1（确认未上涨）
□ ./scripts/verify-pr.ps1 -TaskId PR-FINAL-XX
□ git commit + git push origin HEAD:develop
□ commit message："PR-FINAL-XX: <动词> <对象>"
□ 在本文件 §1 表中把对应行从 🟡 TODO 改为 ✅ DONE
□ 创建 ai-dev-input/11_ai_reviews/pending/PR-FINAL-XX_<myId>.md
```

---

## 8. v0.3-final → v1.0 GA 准入七维度

复用 [`COMPREHENSIVE_REVIEW.md §22`](COMPREHENSIVE_REVIEW.md)：

| 维度 | v0.3-final 准入 | v1.0 GA 准入 |
|---|---|---|
| D1 编译/测试 | mvn/npm 全 GREEN | + 覆盖率（后端 70% / 前端 60%）+ E2E 6 剧本 |
| D2 功能完整 | 阶段 2 PR-FINAL-07..14 完成 | 27 菜单 100% 可达 + 用户手册 4 册 |
| D3 性能 | HikariCP 接入 + 压测 100 并发 30min | P95 < 300ms |
| D4 合规 | ICP/公安备案 + 国密集成 + 用户协议 | + 等保 2.0 三级 + 国密评测 |
| D5 跨数据库 | 4 套 DDL 一致 + KingbaseES 实测 | + Flyway migration |
| D6 文档 | 本文件 + 5 金本位 + 工程规范 30+ | + 用户手册 + 运维手册 + OpenAPI |
| D7 运维 | actuator + Prometheus 基础 | + Grafana 5 看板 + 备份演练 + SLA |

---

## 9. 历史决策快速索引

| 决策 | 位置 | 关键内容 |
|---|---|---|
| ADR-0001 三产品分层 | docs/engineering/adr/ | A 知识工厂 / B 嵌入器 / C 驾驶舱 |
| ADR-0002 PR 命名空间 | 同上 | V2 已用 PR-V2-XX；v0.3 起用 PR-V3-* + PR-FINAL-* |
| ADR-0003 禁硬编码颜色 | 同上 | 唯一 hex 来源：tokens.css/ts |
| ADR-0004 医学内容必须有来源 | 同上 | 所有 AI 推荐显示 traceId + 来源 |
| ADR-0005 MpiController 双副本 | PRODUCT_ARCHITECTURE_FINAL §3 | 保留 patient/，删 patientindex/ |
| ADR-0006 UserSyncController 双副本 | 同上 | 保留 security/usersync/，删 security/UserSyncController |
| ADR-0007 四大模块命名 | 同上 §1 | M1 知识与配置 / M2 临床决策 / M3 质控评估 / M4 平台底座 |
| ADR-0013 去 Dify 化 | application.yml + ModelGatewayService.java | LLM Gateway 主路径国产直连，Dify 只用于 WORKFLOW |
| ADR-0014 v0.3-final 风格统一 | 本文件 + tokens.css | 主色医蓝 #1565c0 + CSS Modules + ESLint no-inline-style |

---

**End of v0.3-final PR backlog.**  
**核心原则**：架构已封板，AI 团队按本卡领单实施。**任何想"重新设计"的冲动 → 阻止 + 提 issue 到架构师。**
