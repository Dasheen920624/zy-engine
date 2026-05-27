# 路径引擎 API 实施计划

> **给 AI 执行者：** 必须按测试先行执行。每个 Chunk 先写失败测试，再实现最小代码，再运行对应验证。步骤使用复选框跟踪。

**目标：** 完成 `GA-ENG-API-06` 后端闭环：专病包、路径模板、发布门禁、仿真、患者入径、节点推进、变异、关键时钟和诊断解释。

**架构：** 新增 `com.medkernel.engine.pathway` 边界包，内部包含专病包、专病画像、模板、节点、边、患者路径、变异、关键时钟、指标绑定、服务和控制器。API 复用现有租户上下文、权限、审计、状态历史、统一响应和诊断响应。首版推进器为确定性节点图，只返回流程建议和执行事实，不写入病历、医嘱或诊断。

**技术栈：** JDK 21、Spring Boot 3.3、Spring Security、Spring Data JDBC/JdbcTemplate、Flyway 五方言迁移、JUnit 5、MockMvc、AssertJ、Jackson。

**对应规格：** `docs/superpowers/specs/2026-05-27-engine-pathway-api-design.md`

**本计划边界：**

| 包含 | 明确不包含 |
|---|---|
| 专病包、专病画像、路径模板、节点、边、患者路径、变异、关键时钟、指标绑定、发布门禁、仿真、推进、权限、审计、迁移测试 | 前端路径画布、复杂医学分型推理、自动入径推荐、自动医嘱生成、外部随访派发、跨版本灰度和患者批量迁移 |

---

## Chunk 1：五方言路径资产迁移

### 任务 1：先扩展迁移合同测试

**文件：**
- 修改：`medkernel-backend/src/test/java/com/medkernel/migration/MigrationBaselineContractTest.java`
- 修改：`medkernel-backend/src/test/java/com/medkernel/migration/H2BaselineMigrationTest.java`
- 修改：`medkernel-backend/src/test/java/com/medkernel/migration/FlywayMultiDialectSmokeTest.java`

- [ ] 将已应用版本期望从 V11 扩展到 V12。
- [ ] 要求五方言都存在 `V12__pathway_engine_api.sql`。
- [ ] 要求迁移包含九张表：`specialty_package`、`specialty_profile`、`pathway_template`、`pathway_node`、`pathway_edge`、`patient_pathway`、`pathway_variance`、`clinical_clock`、`specialty_metric_binding`。
- [ ] 要求包含租户字段、业务唯一键、状态约束、节点类型约束、边类型约束、变异类型约束、关键时钟索引。
- [ ] 运行迁移测试并确认红灯。

### 任务 2：实现 V12 迁移

**文件：**
- 新增：`medkernel-backend/src/main/resources/db/migration/h2/V12__pathway_engine_api.sql`
- 新增：`medkernel-backend/src/main/resources/db/migration/postgres/V12__pathway_engine_api.sql`
- 新增：`medkernel-backend/src/main/resources/db/migration/oracle/V12__pathway_engine_api.sql`
- 新增：`medkernel-backend/src/main/resources/db/migration/dm/V12__pathway_engine_api.sql`
- 新增：`medkernel-backend/src/main/resources/db/migration/kingbase/V12__pathway_engine_api.sql`

- [ ] 按现有迁移风格实现五方言 SQL。
- [ ] 使用稳定业务 ID：`package_id`、`profile_id`、`template_id`、`node_id`、`edge_id`、`patient_pathway_id`、`variance_id`、`clock_id`、`binding_id`。
- [ ] JSON 字段使用各方言现有可移植文本类型，不绑定数据库 JSON 特性。
- [ ] 运行迁移测试并确认绿灯。
- [ ] 提交迁移变更。

---

## Chunk 2：领域模型、仓储和路径推进器

### 任务 1：先写领域与仓储测试

**文件：**
- 新增：`medkernel-backend/src/test/java/com/medkernel/engine/pathway/PathwayRepositoryTest.java`
- 新增：`medkernel-backend/src/test/java/com/medkernel/engine/pathway/PathwayProgressorTest.java`
- 修改：`medkernel-backend/src/test/java/com/medkernel/shared/api/error/ErrorCodeTest.java`

- [ ] 仓储测试覆盖专病包、画像、模板、节点、边、患者路径、变异、关键时钟保存和租户隔离查询。
- [ ] 推进器测试覆盖起始节点、默认边、指定目标节点、无出边完成、变异继续、变异停留和非法目标节点。
- [ ] 错误码测试要求新增 `ENG-PATHWAY-001` 至 `ENG-PATHWAY-007`。
- [ ] 运行目标测试并确认红灯。

### 任务 2：实现最小领域层

**文件：**
- 新增：`medkernel-backend/src/main/java/com/medkernel/engine/pathway/*`
- 修改：`medkernel-backend/src/main/java/com/medkernel/shared/api/error/ErrorCode.java`

- [ ] 增加枚举：专病包状态、模板状态、模板层级、节点类型、边类型、患者路径状态、变异类型、关键时钟状态、推进事件类型。
- [ ] 增加记录类型：专病包、专病画像、模板、节点、边、患者路径、变异、关键时钟、指标绑定。
- [ ] 增加 JDBC 仓储：按租户和业务 ID 查询、分页查询、节点图查询、实例查询。
- [ ] 增加 `PathwayProgressor`，只处理规格列明的确定性节点图推进。
- [ ] 运行领域层测试并确认绿灯。
- [ ] 提交领域层变更。

---

## Chunk 3：服务层、发布门禁、入径和关键时钟

### 任务 1：先写服务测试

**文件：**
- 新增：`medkernel-backend/src/test/java/com/medkernel/engine/pathway/PathwayEngineServiceTest.java`

- [ ] 创建专病包后可创建路径模板。
- [ ] 创建模板时保存节点和边。
- [ ] 缺少起始节点或终止路径时发布失败，返回 `ENG-PATHWAY-004`。
- [ ] 发布成功后模板状态变为 `PUBLISHED`。
- [ ] 入径只能选择已发布模板，并创建起始节点关键时钟。
- [ ] 完成节点后进入下一节点并关闭上一关键时钟。
- [ ] 变异事件写入 `pathway_variance`，可选择继续推进或停留在变异状态。
- [ ] 退出事件关闭当前时钟并将路径状态设为 `EXITED`。
- [ ] 诊断接口返回路径状态、traceId、当前节点、payload 摘要和变异摘要。
- [ ] 运行服务测试并确认红灯。

### 任务 2：实现服务层

**文件：**
- 新增或修改：`medkernel-backend/src/main/java/com/medkernel/engine/pathway/*Service*`

- [ ] 服务层统一读取当前租户、用户和 traceId。
- [ ] 发布、入径、推进、变异、退出和仿真写入审计事件和状态历史。
- [ ] 发布门禁校验模板、节点、边和时间窗。
- [ ] 关键时钟只保存节点级时间事实，不推断医疗质量结论。
- [ ] 运行服务测试并确认绿灯。
- [ ] 提交服务层变更。

---

## Chunk 4：REST API 与安全测试

### 任务 1：先写控制器安全测试

**文件：**
- 新增：`medkernel-backend/src/test/java/com/medkernel/engine/pathway/PathwayEngineControllerSecurityTest.java`

- [ ] `pathway.read` 可以查询专病包、模板、患者路径、关键时钟和诊断。
- [ ] `pathway.write` 可以创建专病包、创建模板、仿真、入径和推进。
- [ ] `pathway.publish` 可以发布路径模板。
- [ ] 缺少对应权限时返回 403。
- [ ] 请求体校验失败时返回统一错误。
- [ ] 运行控制器测试并确认红灯。

### 任务 2：实现控制器和 DTO

**文件：**
- 新增：`medkernel-backend/src/main/java/com/medkernel/engine/pathway/PathwayEngineController.java`
- 新增或修改：`medkernel-backend/src/main/java/com/medkernel/engine/pathway/*Request.java`
- 新增或修改：`medkernel-backend/src/main/java/com/medkernel/engine/pathway/*Response.java`

- [ ] 实现规格中的十二个 REST API。
- [ ] 所有入口加 `@DataScope(requireTenant = true)` 和精确权限。
- [ ] 所有写入请求加 Bean Validation。
- [ ] API 返回 `ApiResult`，分页返回 `PageResponse`。
- [ ] 运行控制器测试并确认绿灯。
- [ ] 提交 API 变更。

---

## Chunk 5：文档台账、全量验证和 PR

### 任务 1：同步权威台账

**文件：**
- 修改：`docs/backlog.md`

- [ ] 将 `GA-ENG-API-06` 状态改为 `done`。
- [ ] 保留 `GA-ENG-PATH-01` 为后续路径画布、分型分支和可视化仿真能力。
- [ ] 说明本次只完成后端 API 闭环。

### 任务 2：全量验证

- [ ] 运行 `mvn -f medkernel-backend/pom.xml test`。
- [ ] 如未改前端，仅记录未运行前端验证的原因；如改动前端，运行 `npm run verify` 和必要浏览器验证。
- [ ] 检查 `git diff --check`。
- [ ] 检查工作区只包含本任务相关文件。

### 任务 3：提交、推送、PR、CI 与合并

- [ ] 提交最终台账与修正。
- [ ] 推送 `codex/ga-eng-api-06-pathway-engine-api`。
- [ ] 创建中文 PR，说明变更范围、验证结果、未完成事项、医疗安全影响、部署和数据迁移影响。
- [ ] 等待远端检查通过。
- [ ] 合并 PR 到远程 `main`。
- [ ] 确认 `origin/main` 包含合并提交。
