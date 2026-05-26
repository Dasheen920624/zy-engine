# 医疗智能引擎数据服务层首批实施计划

> **给 AI 执行者：** 必须使用 `superpowers:subagent-driven-development`（可用子代理时）或 `superpowers:executing-plans` 实施本计划。步骤使用复选框（`- [ ]`）跟踪。

**目标：** 建立医疗智能引擎数据服务的第一条真实闭环：同步权威设计与台账，记录不含患者隐私的已发布知识访问事实，提供脱敏日汇总与受控权威知识工具 API，为后续 MCP、产品级 CLI 和临床端安全接入建立可验证后端合同。

**架构：** 首批仅处理 D0-D2 数据，不接收患者上下文或自由文本工具查询，不实现规则命中统计或临床结论。新增 `engine.dataservice` 边界包，由它负责数据级别、工具合同、已发布知识访问事件与日汇总查询；现有 `engine.knowledge` 仅在成功读取当前权威版本后调用记录器，工具入口只接受稳定知识标识并复用权威版本判定。凡返回当前权威知识依据、脱敏聚合结果或工具结果的入口，必须在返回业务数据前同步写入共享审计链，并保留不含敏感正文的结构化输出摘要；审计落位失败时以服务不可用拒绝返回结果，不能仅生成事件编号。

**技术栈：** JDK 21、Spring Boot 3.3、Spring Security、Spring Data JDBC/JdbcTemplate、Flyway 五方言迁移、JUnit 5、MockMvc、AssertJ。

**对应规格：** `docs/superpowers/specs/2026-05-26-engine-data-service-mcp-cli-clinical-design.md`

**本计划边界：**

| 包含 | 明确不包含 |
|---|---|
| 权威文档和任务台账同步；D0-D2 数据合同；当前权威知识版本访问事实；按日抑制的知识使用汇总；受控权威知识查询工具 API；权限、审计、降级和迁移测试 | 自由文本/语义工具搜索；D3/D4 患者上下文；规则命中或采纳率统计；正式 MCP Server；可安装 CLI；临床嵌入卡片；模型调用；任何样例医疗统计 |

**后续独立计划门禁：**

| 后续计划 | 必须先具备 |
|---|---|
| 规则使用统计 | `GA-ENG-API-05` 与 `GA-ENG-RULE-01` 提供真实规则执行事件合同 |
| MCP Server + 产品级 CLI | 本计划的受控工具 API 通过验收，且模型工具路由/鉴权策略确认 |
| 临床端只读解释 | `GA-ENG-API-11`、标准患者上下文与 CDSS/规则解释具备真实上游 |

---

## Chunk 1：D0-D2 真实知识数据服务底座

### 任务 1：将已确认设计同步到权威方案与任务台账

**文件：**
- 修改：`docs/MEDKERNEL_FOUNDATION_AND_SERVICES.md`
- 修改：`docs/MEDKERNEL_IMPLEMENTATION_LANDING_PLAN.md`
- 修改：`docs/MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md`
- 修改：`docs/backlog.md`

- [ ] **步骤 1：在权威文档中加入统一能力边界**

在总览和实施方案中增加“医疗智能引擎数据服务层”，明确它位于知识/规则/路径/推荐/质控等执行引擎与临床嵌入、管理统计、MCP、CLI 之间；所有入口共用权限、脱敏、审计和 B0 降级，不新增一级菜单。

- [ ] **步骤 2：在唯一详细规范中落位合同**

将已批准规格的以下内容整合到 `MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md` 的系统设计、API/嵌入及 AI 治理相关章节：

```text
数据等级：D0 系统元数据 / D1 已发布资产元数据 / D2 脱敏聚合 /
          D3 去标识化样本 / D4 授权临床上下文 / D5 重要个人信息
首批可实现：D0-D2
首批数据服务与工具禁止：自由文本查询、D3-D5 输入、原始病历、患者标识、自动决策、伪造统计
工具入口：统一受控工具 API，MCP/CLI 只能作为其适配器
```

- [ ] **步骤 3：在单一台账登记实施任务与依赖**

在 `docs/backlog.md` 追加以下任务，保持 `id / owner / status` 三字段口径：

```markdown
| GA-ENG-API-14 引擎数据服务 API：数据分级、受控权威知识工具、知识使用日汇总、审计与降级 | codex | in_progress |
| GA-ENG-DATA-01 知识使用事实与脱敏聚合：D0-D2 事件、统计、五方言迁移和证据 | codex | in_progress |
| GA-ENG-DATA-02 规则与临床信号统计：依赖真实规则/CDSS/质控执行事件，不得伪造 | - | pending |
| GA-ENG-MCP-01 MCP 服务：授权 AI Agent 受控查询知识、规则解释和统计 | - | pending |
| GA-ENG-CLI-01 产品级 CLI：实施、信息科与交付侧安全查询和诊断 | - | pending |
```

其中 `GA-ENG-API-14` 放入 E2，`GA-ENG-DATA-01/02` 放入 E3，`GA-ENG-MCP-01/CLI-01` 放入 E4；临床嵌入仍复用现有 `GA-ENG-EMBED-01` 与后续业务服务包，不新造并行主链。

- [ ] **步骤 4：核对中文、边界和单一事实源**

运行：

```bash
rg -n "医疗智能引擎数据服务层|GA-ENG-API-14|GA-ENG-DATA-01|GA-ENG-MCP-01|GA-ENG-CLI-01|D0|D5|MCP|CLI" docs/MEDKERNEL_FOUNDATION_AND_SERVICES.md docs/MEDKERNEL_IMPLEMENTATION_LANDING_PLAN.md docs/MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md docs/backlog.md
```

预期：四份权威文档均可定位新增能力；详细规范明确首批只处理 D0-D2，D5 禁止进入工具和模型输入。

- [ ] **步骤 5：提交文档治理变更**

```bash
git add docs/MEDKERNEL_FOUNDATION_AND_SERVICES.md docs/MEDKERNEL_IMPLEMENTATION_LANDING_PLAN.md docs/MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md docs/backlog.md
git commit -m "docs: 纳入引擎数据服务与工具入口主线"
```

### 任务 2：定义数据等级、受控工具与隐私拒绝合同

**文件：**
- 新增：`medkernel-backend/src/main/java/com/medkernel/engine/dataservice/EngineDataLevel.java`
- 新增：`medkernel-backend/src/main/java/com/medkernel/engine/dataservice/EngineToolCode.java`
- 新增：`medkernel-backend/src/main/java/com/medkernel/engine/dataservice/EngineToolChannel.java`
- 新增：`medkernel-backend/src/main/java/com/medkernel/engine/dataservice/EngineDataAccessPolicy.java`
- 新增：`medkernel-backend/src/main/java/com/medkernel/engine/dataservice/EngineDataPolicyDecision.java`
- 新增：`medkernel-backend/src/test/java/com/medkernel/engine/dataservice/EngineDataAccessPolicyTest.java`

- [ ] **步骤 1：编写失败的隐私策略测试**

创建 `EngineDataAccessPolicyTest`，覆盖：

```java
@Test
void firstBatchAllowsOnlySystemMetadataPublishedAssetsAndAggregates() {
    assertThat(policy.authorize(EngineDataLevel.D0)).extracting(EngineDataPolicyDecision::allowed).isEqualTo(true);
    assertThat(policy.authorize(EngineDataLevel.D1)).extracting(EngineDataPolicyDecision::allowed).isEqualTo(true);
    assertThat(policy.authorize(EngineDataLevel.D2)).extracting(EngineDataPolicyDecision::allowed).isEqualTo(true);
}

@Test
void firstBatchRejectsAnyPatientContextOrImportantPersonalInformation() {
    assertThat(policy.authorize(EngineDataLevel.D3).allowed()).isFalse();
    assertThat(policy.authorize(EngineDataLevel.D4).allowed()).isFalse();
    assertThat(policy.authorize(EngineDataLevel.D5).allowed()).isFalse();
}
```

同时断言拒绝结果包含中文理由与稳定原因代码，例如 `PATIENT_CONTEXT_NOT_ENABLED`、`IMPORTANT_PERSONAL_INFORMATION_FORBIDDEN`。

- [ ] **步骤 2：运行红灯测试**

运行：

```bash
cd medkernel-backend
mvn -B -Dtest=EngineDataAccessPolicyTest test
```

预期：失败，原因是 `engine.dataservice` 合同类型尚不存在。

- [ ] **步骤 3：实现最小合同类型**

实现稳定枚举：

```java
public enum EngineDataLevel { D0, D1, D2, D3, D4, D5 }

public enum EngineToolChannel { REST }

public enum EngineToolCode {
    GET_ACTIVE_KNOWLEDGE_BY_CODE,
    CHECK_ACTIVE_KNOWLEDGE_EXISTENCE_BY_CODE
}
```

`EngineDataAccessPolicy` 首批只允许 D0-D2，并对 D3-D5 返回结构化拒绝决策；受控工具只通过路径参数接收满足知识资产编码格式的 `identityCode`，不提供请求体、`keyword`、自由文本、患者标识或病例上下文字段；禁止创建绕过策略的 boolean 参数或配置开关。`MCP` 与 `CLI` 渠道枚举只能在对应适配器真正实施时追加。

首批 D2 日汇总是管理端 REST 查询，不纳入 `EngineToolCode`，也不作为 MCP/CLI 工具预置；后续如需让 MCP/CLI 查询汇总，必须先完成独立适配器计划、权限说明和抗差分推断复审。

- [ ] **步骤 4：运行绿灯测试**

运行：`mvn -B -Dtest=EngineDataAccessPolicyTest test`

预期：通过，且测试证明首批无法授权患者上下文进入数据服务。

- [ ] **步骤 5：提交合同变更**

```bash
git add medkernel-backend/src/main/java/com/medkernel/engine/dataservice medkernel-backend/src/test/java/com/medkernel/engine/dataservice
git commit -m "feat: 定义引擎数据分级与工具访问合同"
```

### 任务 3：增加知识使用事实五方言迁移合同

**文件：**
- 新增：`medkernel-backend/src/main/resources/db/migration/h2/V7__engine_data_service_baseline.sql`
- 新增：`medkernel-backend/src/main/resources/db/migration/postgres/V7__engine_data_service_baseline.sql`
- 新增：`medkernel-backend/src/main/resources/db/migration/oracle/V7__engine_data_service_baseline.sql`
- 新增：`medkernel-backend/src/main/resources/db/migration/dm/V7__engine_data_service_baseline.sql`
- 新增：`medkernel-backend/src/main/resources/db/migration/kingbase/V7__engine_data_service_baseline.sql`
- 修改：`medkernel-backend/src/test/java/com/medkernel/migration/MigrationBaselineContractTest.java`
- 修改：`medkernel-backend/src/test/java/com/medkernel/migration/H2BaselineMigrationTest.java`
- 修改：`medkernel-backend/src/test/java/com/medkernel/migration/FlywayMultiDialectSmokeTest.java`

- [ ] **步骤 1：先扩展迁移合同测试**

在 `MigrationBaselineContractTest` 中先要求 `V7__engine_data_service_baseline.sql`、`engine_knowledge_usage_event` 表、索引、租户字段、技术审计字段与约束存在；在 `H2BaselineMigrationTest` 和 `FlywayMultiDialectSmokeTest` 中把已应用版本期望从 `V1` 至 `V6` 改为 `V1` 至 `V7`。

新表最低合同：

```sql
engine_knowledge_usage_event (
    id, tenant_id, event_code, trace_id, actor_user_id,
    hospital_id, department_id, tool_channel, tool_code,
    usage_action, knowledge_identity_id, knowledge_domain,
    result_count, outcome, data_level, masking_policy_code,
    occurred_at, created_at
)
```

约束必须限制：

```text
tool_channel ∈ REST
usage_action ∈ ACTIVE_VERSION_VIEW, AUTHORITATIVE_LOOKUP, EXISTENCE_CHECK
outcome ∈ HIT, NO_RESULT, DEGRADED
data_level ∈ D0, D1, D2
```

不得新增 `keyword`、`query_digest`、`patient_id`、`encounter_id`、`patient_name`、`id_card`、`mobile` 或病历原文列。首批只统计结构化权威知识访问行为，不保存自由文本的明文或可比对摘要。

- [ ] **步骤 2：运行红灯迁移测试**

运行：

```bash
mvn -B -Dtest=MigrationBaselineContractTest,H2BaselineMigrationTest,FlywayMultiDialectSmokeTest test
```

预期：失败，原因是 V7 表族、索引和版本尚不存在。

- [ ] **步骤 3：实现五方言 V7 迁移**

五份 SQL 创建相同语义的 `engine_knowledge_usage_event` 表，增加：

```text
uk_engine_knowledge_usage_event_code
ck_engine_knowledge_usage_channel
ck_engine_knowledge_usage_action
ck_engine_knowledge_usage_outcome
ck_engine_knowledge_usage_level
idx_engine_knowledge_usage_tenant_time
idx_engine_knowledge_usage_tenant_action
idx_engine_knowledge_usage_tenant_identity
```

表中仅保留 D0-D2 使用事实；患者相关字段一律不建列，因此本批次不存在需要落库的 D3/D4 字段加密清单。后续正式接入 MCP/CLI 时，必须以新的迁移扩展 `tool_channel` 约束并同步更新五方言合同，不得提前把尚未上线的渠道伪装成运行事实。

- [ ] **步骤 4：运行绿灯迁移验证**

运行：

```bash
mvn -B -Dtest=MigrationBaselineContractTest,H2BaselineMigrationTest,FlywayMultiDialectSmokeTest test
```

预期：通过；Docker 可用环境由远端继续执行 PostgreSQL/Oracle smoke。

- [ ] **步骤 5：提交迁移变更**

```bash
git add medkernel-backend/src/main/resources/db/migration medkernel-backend/src/test/java/com/medkernel/migration
git commit -m "feat: 增加知识使用事实迁移合同"
```

### 任务 4：记录当前权威知识访问事实，不采集查询内容或患者数据

**文件：**
- 新增：`medkernel-backend/src/main/java/com/medkernel/engine/dataservice/KnowledgeUsageAction.java`
- 新增：`medkernel-backend/src/main/java/com/medkernel/engine/dataservice/KnowledgeUsageOutcome.java`
- 新增：`medkernel-backend/src/main/java/com/medkernel/engine/dataservice/KnowledgeUsageEvent.java`
- 新增：`medkernel-backend/src/main/java/com/medkernel/engine/dataservice/KnowledgeUsageEventRepository.java`
- 新增：`medkernel-backend/src/main/java/com/medkernel/engine/dataservice/KnowledgeUsageRecorder.java`
- 新增：`medkernel-backend/src/test/java/com/medkernel/engine/dataservice/KnowledgeUsageRecorderTest.java`
- 新增：`medkernel-backend/src/main/java/com/medkernel/shared/audit/persistence/RequiredAuditService.java`
- 新增：`medkernel-backend/src/test/java/com/medkernel/shared/audit/persistence/RequiredAuditServiceTest.java`
- 修改：`medkernel-backend/src/main/java/com/medkernel/engine/knowledge/KnowledgeIdentityController.java`
- 修改：`medkernel-backend/src/test/java/com/medkernel/engine/knowledge/KnowledgeIdentityControllerSecurityTest.java`
- 修改：`medkernel-backend/src/main/java/com/medkernel/shared/api/error/ErrorCode.java`

- [ ] **步骤 1：编写失败的记录器测试**

`KnowledgeUsageRecorderTest` 使用 mock repository 与 `RequestContext`，断言：

- `recordActiveVersionView(identityId, REST)` 只保存权威版本访问动作和知识身份标识，不保存任何查询文本或摘要。
- 使用事件从 `RequestContext` 获取 tenant、医院、科室、actor 与 `traceId`。
- 事件数据级别固定为 D1，脱敏策略固定为 `NO_PATIENT_DATA_V1`。
- 未设置租户时拒绝写入，不生成孤立统计事件。
- `RequiredAuditService` 接收已最小化的摘要与可选 `payloadDigest`，调用 `AuditChainWriter.persist` 成功后才返回真实审计事件 ID，并增加已签名指标。
- `AuditChainWriter.persist` 失败时，`RequiredAuditService` 增加失败指标并抛出 `ErrorCode.AUDIT_PERSISTENCE_REQUIRED`（HTTP `503`）；不得吞掉异常或向调用方返回未落链的事件 ID。

- [ ] **步骤 2：运行红灯测试**

运行：`mvn -B -Dtest=KnowledgeUsageRecorderTest,RequiredAuditServiceTest test`

预期：失败，原因是事件记录器、强制审计服务与错误码不存在。

- [ ] **步骤 3：实现追加型事件仓储与强制审计路径**

`KnowledgeUsageEventRepository` 参照 `AuditEventRepository` 使用 `JdbcTemplate` 追加写入，不提供更新或删除操作。记录器方法只接受结构化动作、知识身份 ID、结果和渠道，不接受关键词、病历摘要或患者标识参数。

新增 `ErrorCode.AUDIT_PERSISTENCE_REQUIRED`，HTTP 状态为 `503`，表示“审计证据未能可靠落位，本次访问结果不可返回”。新增 `RequiredAuditService`：它使用 `AuditEvent.of(...)` 建立最小摘要事件，必要时调用 `withPayloadDigest(...)` 附上结构化输出摘要，并直接同步调用现有 `AuditChainWriter.persist(...)`；持久化成功后才返回事件 ID，失败时增加失败指标并抛出上述 `ApiException`。该路径不再把同一事件交给 `AuditEventPublisher`，避免同步落链后被异步监听器重复写入；原有非本计划入口的 `AuditPersistenceSink` 失败兜底语义不在此任务中擅自改变。

普通 `GET .../{id}/active` 与任务 6 的工具入口均调用 `RequiredAuditService` 写入 `AuditAction.EXECUTE` 审计，摘要只描述入口、知识身份标识、渠道、数据等级与结果，不包含请求内容；`payloadDigest` 只对 `toolCode|outcome|knowledgeIdentityId|activeVersionId|dataLevel|maskingPolicyCode` 等结构化引用做摘要，不包含知识正文或入参原文。只有强制审计成功后，才追加成功的知识使用事件并返回业务数据；审计失败返回 `503`，不能返回权威知识内容、工具结果或虚构的成功统计事件。

- [ ] **步骤 4：让现有真实读取路径调用记录器**

在 `KnowledgeIdentityController` 中注入 `KnowledgeUsageRecorder`，仅对可证明返回当前权威版本的成功路径记录：

| 现有接口 | 记录动作 |
|---|---|
| `GET /api/v1/engine/knowledge/identities/{id}/active` | 强制审计成功后记录 `ACTIVE_VERSION_VIEW` / `HIT` |

`identities` 列表、`by-code/{identityCode}`、按 id 详情和 `lineage` 可包含草稿、候选或审核上下文，首批不计为已发布知识使用；新工具 API 在任务 6 中补充结构化权威版本查找事件。权限被拒绝和租户缺失继续由现有安全边界处理，不能记成业务命中。

- [ ] **步骤 5：运行记录与安全测试**

运行：

```bash
mvn -B -Dtest=KnowledgeUsageRecorderTest,RequiredAuditServiceTest,KnowledgeIdentityControllerSecurityTest test
```

预期：通过；测试验证读取权限与租户门禁未被记录逻辑绕过，且当前权威内容在审计链写入失败时以 `503` 拒绝返回、不产生成功使用事实。

- [ ] **步骤 6：提交事实采集变更**

```bash
git add medkernel-backend/src/main/java/com/medkernel/engine/dataservice medkernel-backend/src/main/java/com/medkernel/engine/knowledge/KnowledgeIdentityController.java medkernel-backend/src/main/java/com/medkernel/shared/audit/persistence/RequiredAuditService.java medkernel-backend/src/main/java/com/medkernel/shared/api/error/ErrorCode.java medkernel-backend/src/test/java/com/medkernel/engine medkernel-backend/src/test/java/com/medkernel/shared/audit/persistence/RequiredAuditServiceTest.java
git commit -m "feat: 记录脱敏知识使用事实"
```

### 任务 5：提供抗差分推断的知识使用日汇总 API

**文件：**
- 新增：`medkernel-backend/src/main/java/com/medkernel/engine/dataservice/KnowledgeUsageDailyQuery.java`
- 新增：`medkernel-backend/src/main/java/com/medkernel/engine/dataservice/KnowledgeUsageSummary.java`
- 新增：`medkernel-backend/src/main/java/com/medkernel/engine/dataservice/KnowledgeUsageService.java`
- 新增：`medkernel-backend/src/main/java/com/medkernel/engine/dataservice/KnowledgeUsageController.java`
- 新增：`medkernel-backend/src/test/java/com/medkernel/engine/dataservice/KnowledgeUsageServiceTest.java`
- 新增：`medkernel-backend/src/test/java/com/medkernel/engine/dataservice/KnowledgeUsageControllerSecurityTest.java`
- 修改：`medkernel-backend/src/main/java/com/medkernel/engine/security/PermissionCode.java`
- 修改：`medkernel-backend/src/main/java/com/medkernel/engine/security/DefaultPermissionPolicy.java`
- 修改：`medkernel-backend/src/test/java/com/medkernel/engine/security/DefaultPermissionPolicyTest.java`

- [ ] **步骤 1：先写服务与权限红灯测试**

增加权限 `engine-data.read`，仅管理、质控、审计与实施相关角色默认持有，临床医生不因拥有 `knowledge.read` 而能查看科室聚合趋势。

`KnowledgeUsageServiceTest` 必须断言：

- 请求只能选择按 `Asia/Shanghai`（中国标准时间）解释且已经结束的自然日 `date`，不接收任意 `from/to` 时间范围、渠道筛选或请求方提供的组织筛选。
- UTC 时刻跨越中国标准时间日界时仍按 `Asia/Shanghai` 判断“已结束自然日”，不依赖 JVM 或部署主机默认时区。
- 汇总只返回当前租户整体在该自然日的权威知识访问次数、命中数和无结果数，不开放院区、科室或渠道切片。
- 当日当前租户的总事件数低于 10，或任一拟公开计数字段为 1 至 9 时，结果返回 `suppressed=true` 与中文说明，所有计数为空，不返回维度细分。
- 返回任何未抑制或已抑制的 D2 汇总前，必须通过 `RequiredAuditService` 写入审计链并返回真实 `auditEventId`；审计失败时返回 `503` 且不返回汇总结果。

`KnowledgeUsageControllerSecurityTest` 必须断言：

- `GET /api/v1/engine-data/knowledge-usage/daily` 要求 `engine-data.read` 和租户上下文。
- 医生被拒绝，合规审计或医务处可在有租户上下文时查询。

- [ ] **步骤 2：运行红灯测试**

运行：

```bash
mvn -B -Dtest=KnowledgeUsageServiceTest,KnowledgeUsageControllerSecurityTest,DefaultPermissionPolicyTest test
```

预期：失败，原因是聚合服务、权限和控制器尚不存在。

- [ ] **步骤 3：实现服务端聚合与抑制策略**

实现接口：

```http
GET /api/v1/engine-data/knowledge-usage/daily?date=<yyyy-MM-dd>
```

返回 `ApiResult<KnowledgeUsageSummary>`，结构至少包含：

```java
public record KnowledgeUsageSummary(
    LocalDate date,
    Long authoritativeViews,
    Long hits,
    Long noResults,
    boolean suppressed,
    String suppressionReason,
    String dataLevel,
    String maskingPolicyCode,
    String auditEventId
) {}
```

SQL 聚合始终添加当前 tenant 范围，首批不开放医院、院区、科室、渠道或知识身份切片，也不接受组织筛选参数。首批以服务端固定 `ZoneId.of("Asia/Shanghai")` 计算已结束自然日；未来支持跨时区医院前，必须另立配置及迁移方案，不能依赖部署节点默认时区。接口只允许查询已经结束的单个自然日，不能通过重叠区间相减反推小样本；小样本阈值常量为 `10`。若当日总事件数低于阈值，或 `authoritativeViews`、`hits`、`noResults` 中任一拟公开字段为 1 至 9，则整体抑制，所有 `Long` 计数统一为 `null` 且不返回细分。计数为 0 可保留为 0，前提是总事件数和其他公开正计数字段均满足阈值。控制器仅在聚合结果确定后调用 `RequiredAuditService`：摘要记录入口、查询自然日、D2、脱敏策略和是否抑制，`payloadDigest` 对结构化返回字段做摘要但不把计数写入可阅读摘要；审计成功才填入真实 `auditEventId` 并返回，失败统一为 `503`。

统计口径固定如下，避免把有限事实解释为全量检索效果：

| 字段 | 首批真实来源 |
|---|---|
| `authoritativeViews` | `ACTIVE_VERSION_VIEW/HIT` 与 `AUTHORITATIVE_LOOKUP/HIT`，即确实返回过当前权威版本的次数 |
| `hits` | `AUTHORITATIVE_LOOKUP/HIT` 与 `EXISTENCE_CHECK/HIT`，即受控工具确认存在当前权威版本的次数 |
| `noResults` | `AUTHORITATIVE_LOOKUP/NO_RESULT` 与 `EXISTENCE_CHECK/NO_RESULT`，即受控工具确认不存在当前权威版本的次数 |

组织下钻、跨日趋势与更多维度统计留待单独设计防推断策略后实施。

- [ ] **步骤 4：运行绿灯测试**

运行：

```bash
mvn -B -Dtest=KnowledgeUsageServiceTest,KnowledgeUsageControllerSecurityTest,DefaultPermissionPolicyTest test
```

预期：通过；D2 汇总在 UTC/中国标准时间边界保持稳定，在任一公开正计数字段低于阈值时整体抑制，并在审计落链失败时不泄露聚合结果。

- [ ] **步骤 5：提交统计 API 变更**

```bash
git add medkernel-backend/src/main/java/com/medkernel/engine/dataservice medkernel-backend/src/main/java/com/medkernel/engine/security medkernel-backend/src/test/java/com/medkernel/engine
git commit -m "feat: 提供脱敏知识使用统计 API"
```

### 任务 6：提供后续 MCP 与 CLI 可适配的受控权威知识 API

**文件：**
- 新增：`medkernel-backend/src/main/java/com/medkernel/engine/dataservice/EngineToolResult.java`
- 新增：`medkernel-backend/src/main/java/com/medkernel/engine/dataservice/KnowledgeToolService.java`
- 新增：`medkernel-backend/src/main/java/com/medkernel/engine/dataservice/EngineToolController.java`
- 新增：`medkernel-backend/src/test/java/com/medkernel/engine/dataservice/KnowledgeToolServiceTest.java`
- 新增：`medkernel-backend/src/test/java/com/medkernel/engine/dataservice/EngineToolControllerSecurityTest.java`

- [ ] **步骤 1：编写受控工具红灯测试**

覆盖两个首批工具：

| 工具 | 行为 | 数据级别 |
|---|---|---|
| `GET_ACTIVE_KNOWLEDGE_BY_CODE` | 按稳定 `identityCode` 获取当前权威版本、来源和版本摘要 | D1 |
| `CHECK_ACTIVE_KNOWLEDGE_EXISTENCE_BY_CODE` | 按稳定 `identityCode` 判断是否存在当前权威版本 | D1 |

测试必须覆盖：

- 首批工具渠道固定为 `REST`；不得由请求伪装为 `MCP` 或 `CLI`，对应渠道留待真实适配器实施时由服务端赋值。
- 请求只通过路径参数接受符合资产编码格式的 `identityCode`；接口不存在请求体、`keyword`、自由文本或 D3/D4/D5 上下文输入。
- 结果包含 `traceId`、`dataLevel`、`maskingPolicyCode=NO_PATIENT_DATA_V1`、来源版本、`auditEventId` 和 `fallbackUsed=false`。
- 身份编码不存在或身份存在但无当前权威版本时，两个工具都返回结构化 `outcome=NO_RESULT`（存在性结果为 `exists=false`，版本字段为空），强制审计成功后记录相应 `NO_RESULT` 知识使用事实；不得把现有 `NOT_FOUND` 直接冒泡为未审计的退出路径。
- 调用在返回业务结果前通过 `RequiredAuditService` 形成已签名的 `AuditAction.EXECUTE` 审计，并产生知识使用事件，不保存任意查询文本或摘要。
- 审计链写入失败时返回 `ErrorCode.AUDIT_PERSISTENCE_REQUIRED`（HTTP `503`），响应中不存在工具业务结果或 `auditEventId`，也不能生成标记为成功的知识使用事件。
- 审计 `payloadDigest` 覆盖结构化工具代码、结果、知识身份/当前版本引用、数据级别和脱敏策略，不包含知识正文、自由文本或患者字段。

- [ ] **步骤 2：运行红灯测试**

运行：

```bash
mvn -B -Dtest=KnowledgeToolServiceTest,EngineToolControllerSecurityTest test
```

预期：失败，原因是受控工具 API 尚不存在。

- [ ] **步骤 3：实现工具 API，不引入协议适配器**

实现接口：

```http
GET /api/v1/engine-data/tools/knowledge/active/by-code/{identityCode}
GET /api/v1/engine-data/tools/knowledge/active-existence/by-code/{identityCode}
```

路径参数使用 Bean Validation 验证稳定资产编码格式；接口无请求体且渠道由控制器固定为 `REST`。工具服务按 `getByCode(identityCode)` 定位身份后必须调用现有 `getActiveVersion(identityId)` 判定当前权威版本；仅捕获这两个受控查找步骤产生的 `ErrorCode.NOT_FOUND` 并转为 `NO_RESULT`，权限、租户、校验、审计或数据库故障不得伪装成无结果。工具不得使用返回草稿/候选身份的列表或详情结果充当已发布知识。控制器使用 `@DataScope(requireTenant = true)` 和 `@PreAuthorize("@perm.has('knowledge.read')")`。

工具服务确定 `HIT` 或 `NO_RESULT` 结构化响应后，使用任务 4 的 `RequiredAuditService` 同步落入共享审计链，只有持久化成功才记录相同结果的知识使用事实、把该事件的真实 ID 写入 `auditEventId` 并返回结果；审计失败统一映射为 `503`，不退化为无审计成功响应。

首批只交付 REST 形式的统一工具合同：不依赖 MCP SDK、不创建 CLI 命令、不暴露患者上下文、不支持自由文本或语义搜索。后续 MCP/CLI 适配器只能调用这里定义的工具，不重新实现权限或脱敏；自然语言查询必须等模型网关和输入最小化方案另行设计。

- [ ] **步骤 4：运行绿灯和关联安全测试**

运行：

```bash
mvn -B -Dtest=KnowledgeToolServiceTest,EngineToolControllerSecurityTest,KnowledgeIdentityControllerSecurityTest test
```

预期：全部通过；工具仅查询真实知识资产，保留现有权限边界，能把受控不存在结果留痕计入 `noResults`，且在强制审计写链失败时不返回工具结果或伪造的 `auditEventId`。

- [ ] **步骤 5：提交受控工具入口**

```bash
git add medkernel-backend/src/main/java/com/medkernel/engine/dataservice medkernel-backend/src/test/java/com/medkernel/engine/dataservice
git commit -m "feat: 增加受控知识工具 API"
```

### 任务 7：验证、证据与后续适配器交接

**文件：**
- 修改：`docs/backlog.md`
- 修改：`docs/superpowers/plans/2026-05-26-engine-data-service-foundation.md`

- [ ] **步骤 1：更新任务状态和后续依赖**

实现验收通过后，将 `GA-ENG-API-14` 与 `GA-ENG-DATA-01` 更新为 `done`。`GA-ENG-DATA-02`、`GA-ENG-MCP-01` 与 `GA-ENG-CLI-01` 保持 `pending`，并在修订记录中说明：

```text
首批只上线 D0-D2 当前权威知识访问事实、按日抑制汇总和受控权威知识工具 API；
规则统计须等待真实规则执行合同；
MCP 与 CLI 须作为受控工具 API 的独立适配器实施；
未处理患者上下文或临床决策。
```

- [ ] **步骤 2：运行后端完整验证**

运行：

```bash
cd medkernel-backend
mvn -B clean test
```

预期：所有无需 Docker 的测试通过；如本地无 Docker，PostgreSQL/Oracle Testcontainers smoke 可跳过并在 PR 中如实声明，由远端检查兜底。

- [ ] **步骤 3：运行文档与安全边界检查**

运行：

```bash
cd ..
git diff --check
rg -n "keyword|query_digest|patient_id|encounter_id|patient_name|id_card|mobile|病历原文|患者姓名|身份证号|手机号" medkernel-backend/src/main/java/com/medkernel/engine/dataservice medkernel-backend/src/main/resources/db/migration/*/V7__engine_data_service_baseline.sql
```

预期：`git diff --check` 无错误；第二条检查除输入拒绝测试或中文说明外，不出现数据表列、请求 DTO 或输出 DTO 承载自由文本和患者敏感字段。

同时确认 `KnowledgeUsageControllerSecurityTest` 与 `EngineToolControllerSecurityTest` 覆盖：

- 受控工具接口没有请求体，不接受 `keyword`、自由文本、患者标识或 D3/D4/D5 字段。
- D2 日汇总接口只接受 `date`，不接受 `from/to`、组织筛选、渠道筛选或工具通道伪装参数。
- MCP/CLI 字样只出现在后续计划或注释边界中，不作为首批可调用渠道枚举、请求参数或路由。

- [ ] **步骤 4：提交完成状态**

```bash
git add docs/backlog.md docs/superpowers/plans/2026-05-26-engine-data-service-foundation.md
git commit -m "docs: 完成引擎数据服务首批验收记录"
```

- [ ] **步骤 5：按远程主干门禁交付**

推送 `codex/*` 短分支，创建中文 PR 到 `main`。PR 必须说明医疗安全边界、D0-D2 数据范围、五方言迁移、验证结果与未完成的 MCP/CLI/临床端工作；等待 `backend-build-test`、`guard-rules` 等远端检查通过后使用 squash 合并，并确认 `origin/main` 包含合并提交。
