# 规则引擎 API 实施计划

> **给 AI 执行者：** 必须按测试先行执行。每个 Chunk 先写失败测试，再实现最小代码，再运行对应验证。步骤使用复选框跟踪。

**目标：** 完成 `GA-ENG-API-05` 后端闭环：规则定义、测试用例、发布门禁、仿真、执行和诊断解释。

**架构：** 新增 `com.medkernel.engine.rule` 边界包，内部包含规则资产、版本、测试用例、执行日志、DSL 执行器、服务和控制器。API 复用现有租户上下文、权限、审计、状态历史、统一响应和诊断响应。首版 DSL 为确定性 JSON 条件树，执行结果只返回提示动作和解释，不写入病历或医嘱。

**技术栈：** JDK 21、Spring Boot 3.3、Spring Security、Spring Data JDBC/JdbcTemplate、Flyway 五方言迁移、JUnit 5、MockMvc、AssertJ、Jackson。

**对应规格：** `docs/superpowers/specs/2026-05-27-engine-rule-api-design.md`

**本计划边界：**

| 包含 | 明确不包含 |
|---|---|
| 规则定义、版本、测试用例、发布门禁、执行日志、仿真、执行、解释、权限、审计、迁移测试 | 前端可视化编辑器、完整医学语义算子、模型生成规则、外部回调派发、规则包灰度、自动写病历或自动开医嘱 |

---

## Chunk 1：五方言规则资产迁移

### 任务 1：先扩展迁移合同测试

**文件：**
- 修改：`medkernel-backend/src/test/java/com/medkernel/migration/MigrationBaselineContractTest.java`
- 修改：`medkernel-backend/src/test/java/com/medkernel/migration/H2BaselineMigrationTest.java`
- 修改：`medkernel-backend/src/test/java/com/medkernel/migration/FlywayMultiDialectSmokeTest.java`

- [ ] 将已应用版本期望从 V10 扩展到 V11。
- [ ] 要求五方言都存在 `V11__rule_engine_api.sql`。
- [ ] 要求迁移包含四张表：`rule_definition`、`rule_version`、`rule_test_case`、`rule_execution_log`。
- [ ] 要求包含租户字段、业务唯一键、状态约束、风险级别约束、触发点索引、执行日志时间索引。
- [ ] 运行迁移测试并确认红灯。

### 任务 2：实现 V11 迁移

**文件：**
- 新增：`medkernel-backend/src/main/resources/db/migration/h2/V11__rule_engine_api.sql`
- 新增：`medkernel-backend/src/main/resources/db/migration/postgres/V11__rule_engine_api.sql`
- 新增：`medkernel-backend/src/main/resources/db/migration/oracle/V11__rule_engine_api.sql`
- 新增：`medkernel-backend/src/main/resources/db/migration/dm/V11__rule_engine_api.sql`
- 新增：`medkernel-backend/src/main/resources/db/migration/kingbase/V11__rule_engine_api.sql`

- [ ] 按现有迁移风格实现五方言 SQL。
- [ ] 使用 `rule_id`、`version_id`、`case_id`、`execution_id` 作为稳定业务 ID。
- [ ] 对 JSON 字段使用各方言现有可移植文本类型，不绑定数据库 JSON 特性。
- [ ] 运行迁移测试并确认绿灯。
- [ ] 提交迁移变更。

---

## Chunk 2：领域模型、仓储和 DSL 执行器

### 任务 1：先写领域与 DSL 测试

**文件：**
- 新增：`medkernel-backend/src/test/java/com/medkernel/engine/rule/RuleDslEvaluatorTest.java`
- 新增：`medkernel-backend/src/test/java/com/medkernel/engine/rule/RuleRepositoryTest.java`
- 修改：`medkernel-backend/src/test/java/com/medkernel/shared/api/error/ErrorCodeTest.java`

- [ ] DSL 测试覆盖 `all`、`any`、未命中、缺失字段、数值比较、集合包含和高风险动作确认。
- [ ] 仓储测试覆盖规则定义、版本、测试用例、执行日志保存和租户隔离查询。
- [ ] 错误码测试要求新增 `ENG-RULE-001` 至 `ENG-RULE-006`。
- [ ] 运行目标测试并确认红灯。

### 任务 2：实现最小领域层

**文件：**
- 新增：`medkernel-backend/src/main/java/com/medkernel/engine/rule/*`
- 修改：`medkernel-backend/src/main/java/com/medkernel/shared/api/error/ErrorCode.java`

- [ ] 增加枚举：规则状态、版本状态、测试用例类型、规则类型、创作模式、风险级别、执行状态。
- [ ] 增加记录类型：规则定义、规则版本、测试用例、执行日志。
- [ ] 增加 JDBC 仓储：按租户和业务 ID 查询，分页查询，保存测试结果和执行日志。
- [ ] 增加 `RuleDslEvaluator`，只支持规格列明的确定性算子。
- [ ] 运行领域层测试并确认绿灯。
- [ ] 提交领域层变更。

---

## Chunk 3：服务层、发布门禁和诊断解释

### 任务 1：先写服务测试

**文件：**
- 新增：`medkernel-backend/src/test/java/com/medkernel/engine/rule/RuleEngineServiceTest.java`

- [ ] 创建规则时生成定义和草稿版本。
- [ ] 新增测试用例后可被详情和发布门禁读取。
- [ ] 缺少测试类型时发布失败，返回 `ENG-RULE-004`。
- [ ] 测试用例期望不一致时发布失败并保存最近结果。
- [ ] 测试全部通过后发布成功，规则状态变为 `PUBLISHED`。
- [ ] 真实执行只选择已发布规则，保存执行日志，返回解释和动作。
- [ ] 诊断接口返回执行状态、traceId、payload 摘要和解释快照。
- [ ] 运行服务测试并确认红灯。

### 任务 2：实现服务层

**文件：**
- 新增或修改：`medkernel-backend/src/main/java/com/medkernel/engine/rule/*Service*`

- [ ] 服务层统一读取当前租户和 traceId。
- [ ] 发布、执行、仿真写入审计事件和状态历史。
- [ ] 发布门禁执行四类测试覆盖和期望比对。
- [ ] 执行日志只保存输入摘要、动作 JSON 和解释 JSON，不保存完整患者上下文。
- [ ] 运行服务测试并确认绿灯。
- [ ] 提交服务层变更。

---

## Chunk 4：REST API 与安全测试

### 任务 1：先写控制器安全测试

**文件：**
- 新增：`medkernel-backend/src/test/java/com/medkernel/engine/rule/RuleEngineControllerSecurityTest.java`

- [ ] `rule.read` 可以查询规则、执行规则、查看诊断。
- [ ] `rule.write` 可以创建规则、加测试用例、仿真。
- [ ] `rule.publish` 可以发布规则。
- [ ] 缺少对应权限时返回 403。
- [ ] 请求体校验失败时返回统一错误。
- [ ] 运行控制器测试并确认红灯。

### 任务 2：实现控制器和 DTO

**文件：**
- 新增：`medkernel-backend/src/main/java/com/medkernel/engine/rule/RuleEngineController.java`
- 新增或修改：`medkernel-backend/src/main/java/com/medkernel/engine/rule/*Request.java`
- 新增或修改：`medkernel-backend/src/main/java/com/medkernel/engine/rule/*Response.java`

- [ ] 实现规格中的八个 REST API。
- [ ] 所有入口加 `@DataScope(requireTenant = true)` 和精确权限。
- [ ] 所有写入请求加 Bean Validation。
- [ ] API 返回 `ApiResult`、分页返回 `PageResponse`。
- [ ] 运行控制器测试并确认绿灯。
- [ ] 提交 API 变更。

---

## Chunk 5：文档台账、全量验证和 PR

### 任务 1：同步权威台账

**文件：**
- 修改：`docs/backlog.md`

- [ ] 将 `GA-ENG-API-05` 状态改为 `done`。
- [ ] 保留 `GA-ENG-RULE-01` 为后续可视化/规则包能力。
- [ ] 说明本次只完成后端 API 闭环。

### 任务 2：全量验证

- [ ] 运行 `mvn -f medkernel-backend/pom.xml test`。
- [ ] 如未改前端，仅记录未运行前端验证的原因；如改动前端，运行 `npm run verify` 和必要浏览器验证。
- [ ] 检查 `git diff --check`。
- [ ] 检查工作区只包含本任务相关文件。

### 任务 3：提交、推送、PR、CI 与合并

- [ ] 提交最终台账与修正。
- [ ] 推送 `codex/ga-eng-api-05-rule-engine-api`。
- [ ] 创建中文 PR，说明变更范围、验证结果、未完成事项、医疗安全影响、部署和数据迁移影响。
- [ ] 等待远端检查通过。
- [ ] 合并 PR 到远程 `main`。
- [ ] 确认 `origin/main` 包含合并提交。

