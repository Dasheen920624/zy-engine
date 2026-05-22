# ADR-0014：Flyway 零 baseline 设计 — schema 全面接管 + UNISTR 清理 + 5 方言备注同步

- 状态：Accepted
- 日期：2026-05-22
- 决策人：架构组（Claude Opus 4.7 + 用户）
- 关联：[AI_TEAM_PR_BACKLOG_V0.3_FINAL.md §1 PR-FINAL-25](../../AI_TEAM_PR_BACKLOG_V0.3_FINAL.md) + [PRODUCT_ARCHITECTURE_FINAL.md §6 D5 跨数据库](../../PRODUCT_ARCHITECTURE_FINAL.md) + [ADR-0013 LLM Gateway 去 Dify 化](0013-llm-gateway-go-domestic.md)

---

## 1. 背景

### 1.1 v0.3-final RC 之前 schema 管理的 5 类问题

| 问题 | 现状 |
|---|---|
| **散落 5 套** | DDL 文件分布在 `medkernel-mvp/db/{dm,oracle,postgres}/`（项目根 DBA 参考）+ `medkernel-mvp/src/main/resources/db/{dm,local,oracle,postgres}/`（类路径），共 58 个 SQL 文件、9708 行，5 套方言互不同步 |
| **init-schema 反模式** | 3 个 PersistenceService（Mpi/Security/Engine）通过 `@PostConstruct` 用 raw-JDBC + `loadSchemaStatements()` 读 classpath SQL 执行；仅服务 H2 本地开发，生产 Oracle/DM/PG 靠 DBA 手动执行项目根 DDL —— 双轨制 |
| **无版本号、无回滚** | 没有 schema 版本管理；手工补丁靠 `clean_init_all.sql` "DROP ALL + 重建"，不可线性升级 |
| **UNISTR Unicode 转义反模式** | `db/oracle/medkernel_comments_unistr.sql` 用 `EXECUTE IMMEDIATE 'COMMENT ON \|\| UNISTR('\\8DEF\\5F84...')` 把中文备注编成 `\xxxx` Unicode 转义。这是 Oracle 9i/10g 时代规避 ASCII-only 编辑器的 hack；现代 Oracle 19c / DM 8 / KingbaseES 全部原生支持 UTF-8 直写中文，UNISTR 是技术债 |
| **生产方言 schema 不完整** | 项目根 `db/{oracle,dm,postgres}/` 仅有 6-7 文件，覆盖 ~10 模块；而 H2 `local/` 有 30 模块。意味着生产部署时 21 模块的 Oracle/DM/PG DDL **缺失**，要么 DBA 手翻、要么完全跑不通 |

### 1.2 现状量化

```
DDL 文件总数:          58
DDL 总行数:          9708
方言覆盖:        oracle 9 / dm 9 / postgres 9 / h2 30
UNISTR 行数:          ~165（oracle/medkernel_comments_unistr.sql 125 + 部分残留）
PL/SQL 包裹:           4 文件（oracle 三个 + dm core_ddl 一个）
init-schema 调用点:    3 @PostConstruct 方法 + 4 私有辅助方法（共 ~120 行 Java）
```

---

## 2. 决策

### 2.1 核心原则

**v0.3-final RC 阶段（项目未上线）一次性接管：用 Flyway 取代所有自定义 schema 管理路径。零 baseline——不承认任何"历史 schema 已存在"。**

### 2.2 实施细节

| 项 | 决策 |
|---|---|
| **Flyway 版本** | `spring-boot-starter-flyway`（默认随 Spring Boot 2.7.18 拉 Flyway 8.5.13；保持 JDK 8 兼容）|
| **接管时机** | **显式 `MedKernelFlywayConfig` @Bean(initMethod="migrate")**，**不**走 Spring Boot `FlywayAutoConfiguration`。原因：项目 `EngineDataSourceConfig` 用 `@ConditionalOnMissingBean(DataSource.class)` 提供自定义 DataSource Bean（命名空间 `medkernel.database.*` 而非 `spring.datasource.*`），不触发 `DataSourceAutoConfiguration` → `FlywayAutoConfiguration` 的 `@AutoConfigureAfter` 链断；autoconfig 不会被激活。显式 Bean 同时支持把 V*.sql location 跟项目自有的 `EnginePersistenceProperties.providerName()` 绑定，无需再用 `${...}` 占位符 |
| **配置命名空间** | 标准 `spring.flyway.*`（不再用项目自定义 `medkernel.database.flyway.*`）|
| **方言切换** | `MedKernelFlywayConfig.dialectFolder()` 把 `EnginePersistenceProperties.providerName()` (POSTGRESQL/DM/KINGBASE/LOCAL_H2_FILE/默认 oracle) 映射为 `classpath:db/migration/<dialect>` |
| **Bean 初始化顺序** | `EnginePersistenceService` 标 `@DependsOn("flyway")`，让其本身以及所有通过构造器依赖它的 14+ 个 `@PostConstruct rebuildFromPersistence()` Service（SourceCitationService/PathwayService/MpiPersistenceService 等）排在 Flyway migrate 完成之后初始化，避免缓存预热阶段查询不存在的表 |
| **baseline-on-migrate** | `false` —— 项目未上线，没有"既有 schema"需要承认；任何启用 Flyway 的部署目标库必须为空 |
| **history 表** | `medkernel_schema_history`（带 `medkernel_` 前缀避免污染共享 schema 命名空间）|
| **validate-on-migrate** | `true` —— 每次启动校验 V*.sql checksum，被改过的脚本拒绝执行，强制走新 V 版本号 |
| **enabled 联动** | `spring.flyway.enabled` 跟 `medkernel.database.enabled` 同步（数据库未启用时跳过 Flyway，DataSource 是 NoOp）|

### 2.3 V*.sql 目录结构

```
src/main/resources/db/migration/
├── h2/               (30 个 V1-V30 完整本地开发覆盖)
│   ├── V1__init_engine_core.sql            (h2_core_ddl.sql, 620 行)
│   ├── V2__init_security_core.sql          (sec_ddl.sql, 178 行)
│   ├── ...
│   └── V30__init_re_rule_eval_result.sql
├── oracle/           (8 个 V1-V8)
│   ├── V1__init_engine_core.sql            (medkernel_core_ddl_with_comments.sql, 含 PL/SQL create_ignore 包裹)
│   ├── V2__init_data_governance.sql
│   ├── V3__init_mpi.sql
│   ├── V4__init_re_rule_eval_result.sql
│   ├── V5__init_engine_org_context.sql
│   ├── V6__init_security_audit_chain.sql
│   ├── V7__init_security_multi_identity.sql
│   └── V8__init_security_sso.sql
├── dm/               (8 个 V1-V8，结构与 oracle 镜像)
├── postgres/         (8 个 V1-V8，812 行 V1 合并 comments_unistr 备注)
└── kingbase/         (8 个，从 postgres 复制 + 标记注释)
```

### 2.4 中文备注规范

| 项 | 规则 |
|---|---|
| **UNISTR 禁用** | 不允许 `UNISTR('\\xxxx')` Unicode 转义；新建表的 `COMMENT ON` 必须直接 UTF-8 中文 |
| **权威备注源** | PostgreSQL `medkernel_comments_unistr.sql`（315 行 27 表 222 列 UTF-8 直写）— 最详细、最新 |
| **同步策略** | PG/Kingbase V1 直接合并；DM V1 追加合并；Oracle V1 保留现有混合（line 481+ UTF-8 已直写）|
| **新增表 / 字段** | 所有 5 方言的 V*.sql 必须有对应 `COMMENT ON TABLE` 和 `COMMENT ON COLUMN`，覆盖率 100%（CI 守门待补，详见 §4.2）|

### 2.5 跨方言 V 编号统一规范（D8 follow-up 的命名约定）

**问题**：D3 整理时仅按 H2 的依赖顺序排号（30 个 V1-V30），oracle/dm/postgres/kingbase 各只有 8 个文件且使用了**不同的**编号方案。意味着同一个逻辑模块（如 `init_security_sso`）在 H2 是 V4、在 oracle 是 V8 —— 跨方言运行结果不一致，且新增 V31 时不知道该映射到 oracle 的哪个号。

**决策**：以 H2 为基准建立**逻辑模块 → V 编号的全局映射表**，每个模块的 V 编号在所有 5 方言保持一致。

| V | 逻辑模块 | h2 | oracle | dm | postgres | kingbase |
|---|---|---|---|---|---|---|
| V1 | init_engine_core | ✅ | ✅ | ✅ | ✅ | ✅ |
| V2 | init_security_core | ✅ | ❌ | ❌ | ❌ | ❌ |
| V3 | init_security_user_sync | ✅ | ❌ | ❌ | ❌ | ❌ |
| V4 | init_security_sso | ✅ | ❌ | ❌ | ❌ | ❌ |
| V5 | init_security_audit_chain | ✅ | ❌ | ❌ | ❌ | ❌ |
| V6 | init_security_multi_identity | ✅ | ❌ | ❌ | ❌ | ❌ |
| V7 | init_security_data_permission | ✅ | ❌ | ❌ | ❌ | ❌ |
| V8 | init_security_menu_permission | ✅ | ❌ | ❌ | ❌ | ❌ |
| V9 | init_mpi | ✅ | ❌ | ❌ | ❌ | ❌ |
| V10 | init_data_governance | ✅ | ❌ | ❌ | ❌ | ❌ |
| V11-V30 | （workflow/notify/ops/cdss/ai_*/quality_finding/…）| ✅ | ❌ | ❌ | ❌ | ❌ |

❌ = 该方言下该 V 还未补齐，**D8 (#13) 范围**。

**约束**：
- **编号一致性**：所有方言的 V<n> **必须**对应同一个逻辑模块；不一致会让医院在不同 DB 上看到不同的 schema_history
- **依赖顺序**：V<n> 的 SQL 不能依赖 V<n+k> 创建的表 / 列（D7 修复了 H2 V4↔V5 这种 audit_chain 先于 sso 的反例）
- **缺位策略**：方言 X 暂未实现的 V<n>，**允许**跳号（不放空文件，但下一次新增时仍续 V<max>+1）。例：oracle 当前缺 V2/V3，D8 翻译时直接补 `oracle/V2__init_security_core.sql`，不与 H2 V2 抢号
- **新增表 / 字段**：必须在所有 5 方言同步加 V<max+1>__<同名>.sql；不允许只加 H2

### 2.6 KingbaseES 集成

| 项 | 决策 |
|---|---|
| **驱动** | `cn.com.kingbase:kingbase8:8.6.0`，pom.xml 用 `<profile id="kingbase">` 包裹，CI 不激活（Maven Central 无此 artifact）|
| **DBA 装包** | 文档化指引：`mvn install:install-file` 装本地 `kingbase8-8.6.0.jar`（从人大金仓官网下载）|
| **Dialect 识别** | 复用 `EnginePersistenceProperties.providerName()` 已有 `KINGBASE` 分支（line 144）|
| **DDL** | `db/migration/kingbase/V*.sql` 从 postgres 复制；KingbaseES V8 协议兼容 PG 8.x，仅当实测发现差异时单独覆盖 |

---

## 3. 后果

### 3.1 好处

- **schema 是单一权威源**：所有方言的 V*.sql 在 `db/migration/<dialect>/`，DBA / 开发者只看一个地方
- **版本可追溯**：`medkernel_schema_history` 表记录每次 migrate 的版本、时间、checksum、操作人
- **删除 ~120 行 init-schema Java 代码**：3 个 PersistenceService 不再持有 schema 加载责任，回归单一职责
- **删除 4 个目录**：`medkernel-mvp/db/{dm,oracle,postgres}/` + `src/main/resources/db/{dm,local,oracle,postgres}/` 全删除
- **修 1 个 schema bug**：PG `medkernel_core_ddl_with_comments.sql` line 510 起 `src_dify_template` 表混入 Oracle 类型（`NUMBER(20)/VARCHAR2/CLOB`）→ 修正为 `BIGINT/VARCHAR/TEXT`
- **删除 UNISTR 反模式**：3 个 `medkernel_comments_unistr.sql` 删除，内容已合并到对应 V1
- **未来 schema 变更走 V31+.sql**：医院升级、字段补充、索引调整全部版本化

### 3.2 风险与缓解

| 风险 | 缓解 |
|---|---|
| 生产方言 V*.sql 覆盖度不完整（仅 9 模块 vs H2 30 模块），上线时 21 个模块的 Oracle/DM/PG DDL 缺失 | 留 **PR-FINAL-25 Phase 2**：DBA + 架构师 AI 翻译 H2 → 生产方言；本 PR 范围内 ADR 标 TODO，未上线项目可分批补 |
| 备注语义校验未做（C+D 未完成）| 留 **PR-FINAL-25 Phase 2**：逐表逐字段对照 Java entity / Repository SQL 检查备注是否与字段语义匹配 |
| Oracle V1 含 PL/SQL `create_ignore` 包裹 | Flyway Oracle 方言识别 `END; /` 终止符，原文件可直接被 Flyway 执行；如果未来要拆解，独立 PR |
| 现有测试可能依赖 init-schema 路径 | mvn compile 已验证编译通过；mvn test 在 D7 验证；若测试失败，必要时把测试改用 Flyway 在 H2 上 migrate |
| Flyway 8.5.13 + Java 8 兼容性 | Spring Boot 2.7 BOM 默认拉这版本，已生产验证；不强行升级 Flyway 9.x（需 Java 11+）|

### 3.3 迁移路径（医院真实部署）

1. **新建库**：建好空库 → 启动应用 → Flyway 自动跑 V1-V30（H2）/V1-V8（生产方言）→ schema 就位
2. **升级老库**（v0.2-demo 已部署的医院）：先 DBA `BACKUP`，然后用 `clean_init_all.sql` 等价 SQL 全清，再走新建库流程
3. **故障回滚**：Flyway 不支持自动回滚 V*.sql（社区版限制），故障时 DBA 手动 DROP 表 + 删 `medkernel_schema_history` 对应行

---

## 4. 不变量

### 4.1 V*.sql 写作规范

- **版本号**：单调递增 `V<n>__<description>.sql`，n 不重用、不删除
- **文件名**：snake_case，`__` 双下划线分隔版本号和描述
- **不要修改**已 merge 的 V*.sql 内容（checksum 校验会失败）；变更走 V+1
- **跨方言编号一致**（v0.3-final 起）：同一逻辑模块在所有方言共用 V 编号（详见 §2.5）；允许跳号但不允许抢号

### 4.2 待办

**D8 (#13) — 跨方言 V 编号统一**（紧随 PR-FINAL-25 合入后开 follow-up PR）：
- [ ] 以 H2 V1-V30 为基准，把缺位的 22 个 migration 翻译到 oracle / dm / postgres / kingbase（详见 §2.5 映射表的 ❌ 项）
- [ ] 每个翻译后的 V*.sql 与 H2 同名同号；保证 `medkernel_schema_history` 跨方言一致

**PR-FINAL-25 Phase 2 / 后续 PR**：
- [ ] CI 守门：脚本 `scripts/check-flyway-comment-coverage.ps1` 检查每方言 V*.sql 的 `COMMENT ON TABLE / COLUMN` 覆盖度 ≥ 95%
- [ ] CI 守门：跨方言 V 编号一致性校验（同一 V<n> 在所有方言下应同名同语义）
- [ ] 5 方言 222 列备注的语义校验（对照 Java entity / Repository SQL）
- [ ] KingbaseES 真机 smoke 验证（人大金仓 V8 测试库 + `mvn -P kingbase` 编译 + Flyway migrate）
- [ ] 生产 schema 升级 runbook（`docs/engineering/runbook/flyway-upgrade-runbook.md`）
- [ ] H2 完整测试套件回归（D7 仅验证了 `UserSyncApiContractTests#listSourcesReturnsSeededData`；需要扩展到 mvn test 全量）
