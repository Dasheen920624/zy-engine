# U04 规则引擎单元 · 独立深度真实性审计

- 审计日期：2026-05-29
- 审计单元：规则引擎（DSL 执行 / 规则版本 / 测试病例 / 仿真）
- backlog 关联：GA-ENG-API-05（done）、GA-ENG-RULE-01（done）
- 审计方式：从零、独立、逐行；**未参考** `docs/audit/` 下任何既有报告；只读不改 src
- 取证基线：README.md → docs/CONSTITUTION.md → docs/MEDKERNEL_PRODUCT_EXPERIENCE_RULES.md → docs/MEDKERNEL_IMPLEMENTATION_LANDING_PLAN.md → docs/MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md → docs/backlog.md

---

## 一、单元概览

### 范围内文件（已全部逐行通读）

后端 `medkernel-backend/src/main/java/com/medkernel/engine/rule/`（31 个文件）：
- 核心：`RuleDslEvaluator.java`、`RuleEngineService.java`、`RuleEngineController.java`
- 实体：`RuleDefinition` / `RuleVersion` / `RuleTestCase` / `RuleExecutionLog`
- 枚举：`RuleDefinitionStatus` / `RuleVersionStatus` / `RuleRiskLevel` / `RuleType` / `RuleAuthoringMode` / `RuleTestCaseType` / `RuleTestCaseStatus` / `RuleExecutionStatus`
- 仓库：`RuleDefinitionRepository` / `RuleVersionRepository` / `RuleTestCaseRepository` / `RuleExecutionLogRepository`
- DTO：`Rule*Request` / `Rule*Response` / `RuleDslEvaluation` / `RuleActionResult` / `RuleEvaluationItem` / `RuleTestCaseResult` / `RuleFilter`

迁移 `medkernel-backend/src/main/resources/db/migration/{postgres,oracle,dm,kingbase,h2}/V11__rule_engine_api.sql`（5 方言）

测试 `medkernel-backend/src/test/java/com/medkernel/engine/rule/`：
- `RuleDslEvaluatorTest.java`、`RuleEngineServiceTest.java`、`RuleEngineControllerSecurityTest.java`、`RuleRepositoryTest.java`

前端：
- `frontend/src/pages/clinical/RuleValidate.tsx`
- `frontend/src/pages/tenant/RuleDefinitions.tsx`
- `frontend/src/shared/api/hooks.ts`（契约层，连带审）

### 一句话定性

**后端 DSL 执行器与服务层是“真活”——条件树确定性求值真实、发布门禁真跑测试用例、审计/状态迁移/输入摘要真实落库、五方言迁移高度一致、租户隔离到位。但本单元在三个层面“名不副实”：(1) 前后端契约系统性错位，create/simulate/evaluate 三大写接口前端发的报文后端必然 400，响应 DTO 字段为前端虚构 → 端到端假闭环；(2) DSL 算子集无法表达详细规范自己点名的临床规则（单位换算/时序/连续 N 次/区间/算术派生全缺）；(3) 阻断（BLOCK）动作无强制、双审缺失、医师确认标志全程不落地 UI，违反 CONSTITUTION 与详细规范的医疗安全硬约束。**

---

## 二、10 维度逐项核查（findings 带 file:line）

### ① 业务正确性

**真实/正向：**
- DSL 求值是**真按条件树执行**，非贴标签：`RuleDslEvaluator.evaluateConditionNode` 对 `all`/`any` 递归短路（`RuleDslEvaluator.java:62-90`），叶子按 `fact` 路径取上下文、按 `operator` 求值（`:92-111`）。10 种算子（exists/equals/not_equals/contains/gt/gte/lt/lte/in/not_in）均有真实实现（`:143-214`），无 `switch 写死结果`、无 `Math.random`、无写死常量。
- 严重度真实归并：`RuleRiskLevel.max` 按 `ordinal` 取高（`RuleRiskLevel.java:18-26`），`evaluate` 对 `then` 动作 reduce 取最高（`RuleDslEvaluator.java:56-58`）——真用上下文求值结果。
- 命中才解析 `then`、未命中返回空动作（`RuleDslEvaluator.java:50-59`）；缺失字段产生“未命中”而非抛错（`:135-141` + `exists` `:143-154`），便于仿真定位失败——设计合理。

**问题：**
- **[B-1 / Medium] 创建期 DSL 校验避开了 `then` 校验。** `RuleEngineService.validateDsl` 用**空上下文**调 `evaluate`（`RuleEngineService.java:354`）。因为空上下文几乎不命中，`evaluate` 走 `if(!hit) return`（`RuleDslEvaluator.java:51-53`），`parseActions` 根本不执行。结果：`then` 里非法的 `actionCode` 缺失、非法 `severity` 在**创建时不会被发现**，要拖到 publish 跑用例或真实 evaluate 命中时才抛 `ENG-RULE-001`。创建态“校验通过”名不副实。
- **[B-2 / Medium] ruleCode 同租户唯一仅靠 DB 兜底，且报错不友好。** `RuleDefinition.java:13` 与迁移 `uk_rule_definition_tenant_code`（`postgres/V11:21`）声明唯一；仓库备有 `findByTenantIdAndRuleCode`（`RuleDefinitionRepository.java:26`）**但 `createRule` 从不调用**（`RuleEngineService.java:95-119`）。重复 ruleCode 会以 `DataIntegrityViolationException`（HTTP 500）抛出，而非干净的 `ENG-RULE-*` 业务错误。前端创建表单还把“同租户不可重复”写进 placeholder（`RuleDefinitions.tsx:658`），但无任何前置校验。

### ② 医疗安全合规（阻断双审 / 医师确认 / 高风险）

这是本单元最薄弱的维度，多处违反硬约束。

- **[S-1 / Critical] “阻断规则必须双审”未实现。** 详细规范 `MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md:1516`：“阻断规则必须测试和**双审**”。实际 publish 仅需单一 `rule.publish` 权限（`RuleEngineController.java:120`），`RuleEngineService.publish`（`:200-231`）无第二复核人、无双签状态机。阻断/红线规则可被单人直接发布上线。
- **[S-2 / Critical] BLOCK 动作无任何强制效果。** 详细规范 `:1043`/`:1069` 要求“阻断”为动作类型，`:1122` 要求“阻断类规则必须有豁免和应急流程”。代码里 `BLOCK` 只是 `requiresConfirmation` 里的一个字符串判断（`RuleDslEvaluator.java:238-240`），`evaluate` 命中后仅把 flag 放进返回值（`RuleEngineService.java:240-258`），**没有任何机制阻断医嘱/病历**，也无豁免（exemption）/应急（override）流程与记录。所谓“阻断”是纯展示语义。
- **[S-3 / Critical] `requiresPhysicianConfirmation` 服务端算出却全程不落地 UI。** CONSTITUTION `:57`：“任何医疗决策必须有医师确认才进病历”。后端确为 HIGH/CRITICAL/BLOCK/STRONG_REMINDER/RECOMMEND_NEXT 强制 `requiresPhysicianConfirmation=true`（`RuleDslEvaluator.java:124-127, 235-241`），并随 `RuleActionResult` 返回（`RuleActionResult.java:13`）。但 **`RuleValidate.tsx` 的命中表（`:92-144`）与仿真面板（`RuleDefinitions.tsx:609-619`）均无任何“需医师确认”列/控件**，更无确认动作回写。安全闭环在“现场提醒”这一端断裂。
- **[S-4 / High] 无 CRITICAL 严重度的任何测试，确认强制逻辑无回归保护。** `RuleDslEvaluatorTest`/`RuleEngineServiceTest` 全程只测到 HIGH（`RuleEngineServiceTest.java:233` 等），无一例断言“DSL 未写 requiresPhysicianConfirmation 时 HIGH/CRITICAL 仍被强制为 true”——医疗安全最关键的不变量裸奔。

### ③ 多租户隔离

**整体真实、到位。**
- 类级 `@DataScope(requireTenant=true)`（`RuleEngineController.java:31`）兜底。
- 服务层每个入口先 `requireCurrentTenant()`（`RuleEngineService.java:96,129,143,...`），无租户抛 `tenantMissing`（`:376-382`）。
- 所有仓库查询强制带 `tenant_id`：`findByRuleIdAndTenantId`、`findPublishedByTenantId`（`RuleDefinitionRepository.java:21,31-36`）、`pageByFilter`/`countByFilter` 均 `WHERE tenant_id=:tenantId`（`:41-63`）；版本/用例/执行日志同理。
- 迁移所有唯一键/索引以 `tenant_id` 打头（`postgres/V11:21,31-32,53,57,84,111-113`）。
- 有跨租户隔离测试 `repositoryQueriesDoNotLeakAcrossTenants`（`RuleRepositoryTest.java:71-79`）与分页隔离断言（`:82-93`）。

**小问题：**
- **[T-1 / Low] `rule_test_case`/`rule_execution_log` 唯一键未含 tenant。** `uk_rule_test_case_id UNIQUE(case_id)`、`uk_rule_execution_id UNIQUE(execution_id)`（`postgres/V11:79,106`）为全局唯一。因业务键是 UUID 不会碰撞，风险低，但与 `rule_definition`/`rule_version` 的“tenant+键”范式不一致。

### ④ 审计证据链

**真实。**
- 写操作均发审计事件：CREATE（`RuleEngineService.java:118`）、UPDATE 新增用例（`:175`）、PUBLISH（`:227`）、EXECUTE（`:309`）。
- 状态迁移真记录：`transitions.record` 在 create（`:117`）、publish（`:225`）、execute（`:308`）。
- 执行日志真实落库且**只存输入 SHA-256 摘要而非完整患者上下文**（`RuleEngineService.java:407-415` + `RuleExecutionLog.java:25` + 注释 `RuleExecutionLog.java:11-13`，迁移 `postgres/V11:95,167`）——隐私设计正确，且摘要为真实 SHA-256（`HexFormat`，非 UUID 充哈希）。
- diagnose 真按 `execution_id` 装配 `DiagnoseResponse`（`:266-277`），`PayloadRef` 用真实 digest（`:271-273`）。

**问题：**
- **[A-1 / Low] 仿真也产生 EXECUTE 审计 + 状态迁移，未与真实执行区分。** `simulate` 复用 `evaluateAndLog`（`:191`），与 `evaluate` 走同一条 `AuditAction.EXECUTE`/`EXECUTE_RULE`（`:308-309`）。仿真与生产执行在审计流里无法区分，长期会污染“真实命中”统计。`trigger` 缺省为 `SIMULATE`（`:190`）有一定区分度，但审计动作语义未分。

### ⑤ 五方言一致性

**强一致，本维度基本通过。**
- 5 方言 13 个约束**完全一致**（实测 `grep -oE "CONSTRAINT ..."` 对 postgres/oracle/dm/kingbase/h2 输出完全相同的 13 项：4 个 uk + 9 个 ck）。
- 方言类型正确：postgres/kingbase 用 `BIGSERIAL`+`TEXT`+`TIMESTAMPTZ`+`BOOLEAN`+`NOW()`；oracle 用 `GENERATED ALWAYS AS IDENTITY`+`CLOB`+`TIMESTAMP WITH TIME ZONE`+`NUMBER(1)`+`SYSTIMESTAMP`；dm 用 `IDENTITY`+`CLOB`+`CURRENT_TIMESTAMP`；h2 用 `GENERATED ALWAYS AS IDENTITY`+`CLOB`，`MODE=PostgreSQL`。

**问题：**
- **[D-1 / Low] dm 与 h2 缺 COMMENT 块。** postgres/oracle/kingbase 各 175 行（含完整 `COMMENT ON`），dm/h2 各 113 行（无注释）。功能无影响，文档平价缺口。
- **[D-2 / Low] `expected_hit` 在 Oracle/dm 为 `NUMBER(1) NOT NULL`，实体映射为 `Boolean`（可空）。** （`oracle/V11:67` vs `RuleTestCase.java:25`）。请求层 `expectedHit` 为原始 `boolean`（`RuleTestCaseRequest.java:16`）兜底，实际不致 NPE，但类型语义不齐。

### ⑥ 代码净化

**后端干净；存在“声明了却没接线”的死代码与半成品状态机。**
- 无 `Math.random`、无 catch 吞错伪造成功、无写死结果、无 FORCE_* 钩子、无硬编码署名。`runTestCase` 的 catch 是把异常**如实**记为 `ERROR` 状态并回写信息（`RuleEngineService.java:289-294`），不是伪造成功——正确。
- **[C-1 / High] 状态机半成品：OFFLINE/ARCHIVED/回滚全是“声明无实现”。** 枚举与注释宣称 `DRAFT→PUBLISHED→OFFLINE→ARCHIVED 由 RuleEngineService 推进`（`RuleDefinition.java:14`、`RuleDefinitionStatus.java:6-7`、`RuleVersionStatus.java:6-7`），迁移亦写入 CHECK 含 4 态（`postgres/V11:28,54`）。但全 grep 确认**无任何代码路径**能进入 OFFLINE/ARCHIVED——服务层只有 DRAFT→PUBLISHED（`RuleEngineService.java:200-231`），无 offline/archive/rollback 接口。`rollback_version_id` 列（`RuleVersion.java:29`、`postgres/V11:47`）从不写值（copyVersion 原样透传 `:433`）。
- **[C-2 / Medium] 6 个仓库查询方法是死代码（版本历史/回滚/执行历史/覆盖度全没接线）。** 经 grep 确认仅在各自接口声明、无调用方：`findByTenantIdAndRuleCode`、`findCoveredTypes`、`findLatestByRuleIdTenantAndStatus`、`pageByRule`、`findByRuleIdAndTenantIdOrderByVersionNoDesc`、`findByRuleIdAndTenantIdAndVersionNo`、`findByCaseIdAndTenantId`（`RuleDefinitionRepository.java:26`、`RuleTestCaseRepository.java:40,21`、`RuleVersionRepository.java:26,36,47`、`RuleExecutionLogRepository.java:32`）。其中 `findCoveredTypes` 与 `ensureCoverage`（`RuleEngineService.java:333-340`，改在 Java 内存里重算覆盖度）功能重复——说明覆盖度查询接口是“写了备用从未启用”。
- **[C-3 / Medium] “版本化”是空壳。** `versionNo` 永远是 1：创建时硬写 `1`（`RuleEngineService.java:112`），publish 只是把同一版本 copy 一份改状态（`copyVersion :427-435`），不新增版本号、不归档旧版。`uk_rule_version_rule_no(tenant,rule,version_no)` 的多版本设计、`findByRuleIdAndTenantIdAndVersionNo` 等全部闲置。GA-ENG-RULE-01 标题里的“规则版本”实为伪能力。

### ⑦ 错误处理与降级

**后端真实、规范。**
- 统一 `ApiException + ErrorCode.ENG_RULE_*`：DSL 非法 001（`RuleDslEvaluator.java:243-245`）、规则不存在 002、版本不存在 003、门禁未过 004、序列化失败 005、非草稿态 006（`RuleEngineService.java` 多处）。
- JSON 解析失败如实抛 001（`:384-390`），不静默吞。
- 不支持的算子真抛 001（`RuleDslEvaluator.java:109`），有测试覆盖（`RuleDslEvaluatorTest.java:117-132`）。

**问题：**
- **[E-1 / Medium] 详细规范要求的“超时不阻断医生主流程 + 降级日志”在规则引擎内无体现。** 规范 `:178`/`:302`/`:563`/`:1446`：在线提示/同步 REST “超时不阻断、记录降级日志”。`evaluate` 是同步、串行遍历全部已发布规则（`RuleEngineService.java:246-253`），无超时控制、无 P95/熔断、无“引擎不可用→提示暂不可用”降级路径。规则数量增长后会拖慢医生主流程，违反 `:1478` 性能原则。

### ⑧ 可观测性

**基本到位。** traceId 贯穿（创建/版本/执行日志/各响应均带 `RequestContext.currentTraceId()`）；执行日志含 `trigger_point`、`actor_user_id`、`hit`、`severity`、`status`、`error_code`/`error_class` 字段（`RuleExecutionLog.java`，迁移 `postgres/V11:86-113`）；执行日志索引按租户+时间/规则/触发点（`postgres/V11:111-113`）。
- **[O-1 / Low] `error_code`/`error_class` 列在真实执行路径从不写入。** `evaluateAndLog` 里 status 只会是 SUCCESS/MISS（`RuleEngineService.java:301`），异常未被捕获写 FAILED——一旦 `evaluator.evaluate` 抛错，整个事务回滚，`rule_execution_log` 不会留下 FAILED 行。`RuleExecutionStatus.FAILED`（`RuleExecutionStatus.java:11`）与两列基本是死字段。

### ⑨ 测试覆盖与有效性（阳/阴/边界/缺失/冲突）

**后端测试是“真测”，非凑绿；但有关键缺口。**

真实有效：
- `RuleDslEvaluatorTest`：阳性 all 命中取最高严重度（`:18-56`）、any+in 命中（`:58-80`）、缺失字段→MISS 不抛错（`:82-98`）、缺失数值→lt 不命中（`:100-115`）、不支持算子→抛 001（`:117-132`）。断言落到 hit/severity/actions/explanation 真实字段。
- `RuleEngineServiceTest`：create 用 ArgumentCaptor 验证落库 tenant/versionNo/sourceRef（`:73-95`）；publish 缺类型→004（`:115-128`）；publish 用例期望不符→004 且回写 FAIL（`:130-155`）；publish 全通过→PUBLISHED 且四例全 PASS（`:157-185`）；evaluate 真写执行日志、digest 以 sha256: 开头、actions 含 STRONG_REMINDER（`:187-208`）。**boundaryContext age=18 正好压在 gte 18 边界**（`:288-291`）——真边界。
- `RuleEngineControllerSecurityTest`：doctor 不能 publish→403（`:133-138`）、guest 不能 read→403（`:140-145`）、各角色到达入口但缺租户→ENG-BASE-001（多处）。RBAC 真验证。
- `RuleRepositoryTest`：真 Flyway 迁 H2、真持久化四表、跨租户不泄漏（`:46-93`）。

缺口：
- **[Q-1 / High] CONFLICT 用例形同虚设——发布门禁的“冲突”只查类型标签存在，不验证真冲突。** `ensureCoverage` 仅校验四类**枚举标签齐备**（`RuleEngineService.java:333-340`），不校验 CONFLICT 用例是否真表达“两条规则相互排斥/条件重叠”。测试里 CONFLICT 用例直接复用 `missContext`（age=12 的普通阴性，`RuleEngineServiceTest.java:168`），与 NEGATIVE 无本质差异。把任意阴性贴上 CONFLICT 标签即可过门禁，“四类齐备”是装饰性门槛。
- **[Q-2 / High] 缺“医师确认强制”不变量测试**（见 S-4）：无一例验证 DSL 省略 `requiresPhysicianConfirmation` 时 HIGH/CRITICAL 被强制为 true。
- **[Q-3 / Medium] 无前后端契约测试**：没有任何测试覆盖前端实际发送的报文形状（payloadJson/inputPayload/dslJson），故 ⑩ 的契约错位全程无网拦截。
- **[Q-4 / Low] 无 OFFLINE/ARCHIVED/rollback/多版本测试**——因功能本就不存在（C-1/C-3）。

### ⑩ 前后端契约一致

**系统性错位，本单元判定“端到端假闭环”的主要依据。** 经核对后端记录与 `frontend/src/shared/api/hooks.ts`：

- **[X-1 / Critical] create 接口前端字段名+类型双错，必然 400。** 后端 `RuleCreateRequest` 需 `dsl`/`explanation` 为 `@NotNull JsonNode`（`RuleCreateRequest.java:24-25`）；前端 `useCreateRule` 发 `dslJson`/`explanationJson` **字符串**（`hooks.ts:368-379`，页面 `RuleDefinitions.tsx:143-153`）。字段名不符 → `dsl` 缺失 → `@NotNull` 校验 400。创建规则功能**不可用**。
- **[X-2 / Critical] simulate 接口字段名错，必然 400。** 后端 `RuleSimulateRequest` 需 `context`（`@NotNull JsonNode`，`RuleSimulateRequest.java:11`）；前端发 `{inputPayload}`（`hooks.ts:405`，页面 `:202-204`）→ `context` 缺失 → 400。仿真功能**不可用**。
- **[X-3 / Critical] evaluate 接口字段错，必然 400。** 后端 `RuleEvaluateRequest` 需 `triggerPoint`+`context`(@NotNull JsonNode)+`eventId`+`ruleIds`（`RuleEvaluateRequest.java:16-21`）；前端发 `{triggerPoint, patientId, payloadJson}`（`hooks.ts:428-432`，页面 `RuleValidate.tsx:78-82`）→ `context` 缺失 → 400。规则沙箱评估功能**不可用**。
- **[X-4 / High] 响应 DTO 为前端虚构字段。** 后端 `RuleEvaluationItem = {executionId, ruleId, versionId, hit, severity, actions[], explanation:JsonNode}`（`RuleEvaluationItem.java:10-18`）；前端 `RuleEvaluationItem` 声明为 `{ruleId, ruleCode, ruleName, hit, severity, actionCode, explanation:string}`（`hooks.ts:299-307`）。`ruleCode`/`ruleName`/`actionCode` 后端从不返回，`explanation` 实为对象非字符串，`actions` 数组被前端塌缩成 `actionCode` 标量。命中表三列（`RuleValidate.tsx:95,102,121`）运行时恒为空。
- **[X-5 / High] `RuleEvaluateResponse.executionId` 不存在，诊断回溯按钮是死路。** 后端响应是 `{requestId, items, highestSeverity, traceId}`（`RuleEvaluateResponse.java:8-13`），无 `executionId`；前端声明含 `executionId`（`hooks.ts:309-314`）并据此渲染“追溯解释诊断”（`RuleValidate.tsx:129-134`）。该字段恒 undefined → 永远走“无可追溯快照”分支；真正的 `executionId` 在 `items[].executionId` 里却没被用。
- **[X-6 / High] ruleType 枚举前后端完全对不上。** 后端 `RuleType` + DB CHECK 仅允许 `DIAGNOSIS/ORDER/LAB/REPORT/DISCHARGE/FOLLOWUP/INSURANCE/QUALITY/RECORD/PATHWAY`（`RuleType.java:10-21`、`postgres/V11:22-25`）；前端到处用 `DRUG_SAFETY`/`INSURANCE_AUDIT`/`CLINICAL_QUALITY`（`RuleDefinitions.tsx:250-256,335-337,677-679`）。即便修好 X-1，创建仍会被 Jackson 枚举绑定/DB CHECK 拒绝。
- **[X-7 / Medium] simulate 输出渲染基于不存在的标量字段。** 页面读 `simulateResult.actionCode`/`simulateResult.severity`/`simulateResult.explanation`(string)（`RuleDefinitions.tsx:612-624`），后端返回的是 `actions[]` 与 `explanation`(JsonNode)。即便接口连通，输出面板也会显示空动作与 `[object Object]`/空解释。
- **[X-8 / Low] 两页头部 `/* eslint-disable medkernel/no-page-mock */`（`RuleValidate.tsx:1`、`RuleDefinitions.tsx:1`）。** 该 lint 规则只拦 SHOUTY-CASE 数组（`eslint-rules/no-page-mock.js:48-56`），页内 DEFAULT_* 为 camelCase 本不会触发，整文件 disable 属防御性冗余；真正的债是 X-1~X-7 的虚构契约，而非数组 mock。`RuleValidate.tsx:81` 的 `patientId:"P-1001" // 模拟传入` 是硬编码占位。

---

## 三、临床表达力专项（资深临床视角）

**判定：DSL 算子集无法表达详细规范自己点名的临床规则，构成实质性临床能力缺失（High）。**

详细规范 `MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md §4 规则引擎详细规范` 明确要求的算子能力 vs 实现：

| 规范要求 | 出处 | 实现状态 |
|---|---|---|
| 数值：区间 / between | `:1053` | **缺**（只有 gt/gte/lt/lte 单边） |
| 数值：变化率 / 趋势 | `:1053` | **缺** |
| 数值：单位换算后比较（mg/g、mmol/L） | `:1053,1248` | **缺**（compare 仅裸 BigDecimal 比较，`RuleDslEvaluator.java:200-214`） |
| 时间：窗口内/外、距今、之前/之后 | `:1054` | **缺**（无任何日期/时间算子） |
| 时间：连续 N 次 | `:1054` | **缺** |
| 时序：先后顺序、持续时长、延迟、重复 | `:1059` | **缺** |
| 集合/聚合（count/sum/window） | `:1032,1059` | **缺**（contains/in 仅成员判断，无聚合） |
| 算术派生（CrCl/eGFR 等公式） | 隐含于 `:1053` 肌酐示例 | **缺**（无算术表达式求值） |

**临床后果（具体到规范自带示例）：**
- 规范 `:1053` 示例“**肌酐超过阈值**”——真实场景需把不同单位（μmol/L vs mg/dL）换算后比较，当前 `gt` 无单位概念，直接比较原始数值会**误判**。
- 规范 `:1054` 示例“**入院后 N 小时内需完成某文书**”——需“事件时间 + 窗口”算子，当前无法表达，这类时限质控规则**根本配不出来**。
- 规范 `:1059`/`:1290` 示例“**抗菌药物使用早于手术切口时间**”“**术前知情同意时序**”——需时序先后比较，当前缺失，围手术期（S26）核心时序规则**无法落地**。
- 肾功能调整剂量（按 CrCl/eGFR 分档）——需算术派生 + 区间，**双缺**。

当前 10 算子只能表达“静态字段等值/包含/单边阈值”类规则（如“年龄≥65 且药品=X”），覆盖的是最初级的合理用药提醒，撑不起规范定义的 L3 专家 DSL（`:1032`）与 §4 全量临床规则。

---

## 四、7 角色可用性评估

| 角色 | 能否真实使用本单元 | 结论 |
|---|---|---|
| 实施工程师 | ✗ | 创建/仿真接口前端 400（X-1/X-2），导入规则走不通；且本就“不进规则模块”（CONSTITUTION `:127`） |
| **路径专家 / 临床专家** | ✗ | 需求是“不碰 JSON/DSL，用可视化配置”（规范 `:1019-1032` L1/L2 模板与可视化模式）。实际 `RuleAuthoringMode.TEMPLATE/VISUAL` 仅枚举占位（`RuleAuthoringMode.java:6-7`），前端只有裸 JSON DSL 文本框（`RuleDefinitions.tsx:706-725`）。专家**无可视化入口**，完全无法自助配置。 |
| **医务处 / 质控办** | ✗ | 同上无可视化；且“阻断规则双审”缺失（S-1），医务处的复核职责（规范 `:387` S5“医务处、质控办、专家”）在系统里无承载。 |
| 临床医生（现场被提醒方） | ✗ | evaluate 接口 400（X-3）；即便修通，命中表关键列空（X-4）、医师确认标志不显示（S-3）、诊断回溯死路（X-5）。现场提醒不可信。 |
| 信息科 | △ | 后端 API/迁移可独立部署、可观测，但无 offline/回滚（C-1）运维手段。 |
| AI 团队 | △ | 后端 DSL 执行器可作为确定性基线被调用；但算子表达力不足（专项），生成的复杂规则无法执行。 |
| 患者 | N/A | 不直接触达。 |

**结论：面向“非技术配置者”（路径专家/医务处）的核心诉求——可视化/模板配置 + 双审——完全未实现；面向临床医生的现场提醒因契约错位与安全标志缺失不可用。**

---

## 五、Findings 汇总表

| ID | 维度 | 严重度 | 一句话 | file:line |
|---|---|---|---|---|
| X-1 | ⑩契约 | Critical | create 发 dslJson/explanationJson 字符串，后端要 dsl/explanation JsonNode → 恒 400 | hooks.ts:368-379 / RuleCreateRequest.java:24-25 |
| X-2 | ⑩契约 | Critical | simulate 发 inputPayload，后端要 context → 恒 400 | hooks.ts:405 / RuleSimulateRequest.java:11 |
| X-3 | ⑩契约 | Critical | evaluate 发 patientId/payloadJson，后端要 context(JsonNode) → 恒 400 | hooks.ts:428-432 / RuleEvaluateRequest.java:16-21 |
| S-1 | ②安全 | Critical | 阻断规则必须双审，实际单权限单人发布 | SPEC:1516 / RuleEngineController.java:120 |
| S-2 | ②安全 | Critical | BLOCK 动作无强制效果、无豁免/应急流程 | RuleDslEvaluator.java:238-240 / RuleEngineService.java:240-258 |
| S-3 | ②安全 | Critical | requiresPhysicianConfirmation 算出却全程不落地 UI | RuleActionResult.java:13 / RuleValidate.tsx:92-144 |
| C-1 | ⑥净化 | High | 状态机 OFFLINE/ARCHIVED/rollback 声明无实现 | RuleDefinition.java:14 / RuleEngineService.java:200-231 |
| Q-1 | ⑨测试 | High | CONFLICT 用例只查标签不验真冲突，门禁装饰化 | RuleEngineService.java:333-340 / RuleEngineServiceTest.java:168 |
| 临床 | 专项 | High | DSL 缺单位换算/时序/连续N次/区间/算术派生，规范示例无法表达 | RuleDslEvaluator.java:98-111 / SPEC:1053-1059 |
| X-4 | ⑩契约 | High | 响应 DTO 虚构 ruleCode/ruleName/actionCode，命中表恒空 | hooks.ts:299-307 / RuleEvaluationItem.java:10-18 |
| X-5 | ⑩契约 | High | RuleEvaluateResponse.executionId 不存在，诊断回溯死路 | hooks.ts:309-314 / RuleEvaluateResponse.java:8-13 |
| X-6 | ⑩契约 | High | ruleType 前端 DRUG_SAFETY 等后端不认 | RuleDefinitions.tsx:677-679 / RuleType.java:10-21 |
| S-4 | ②安全 | High | 无 CRITICAL/强制确认不变量测试 | RuleEngineServiceTest.java（全文件无 CRITICAL 断言） |
| C-3 | ⑥净化 | Medium | versionNo 恒为 1，多版本/回滚是空壳 | RuleEngineService.java:112,427-435 |
| C-2 | ⑥净化 | Medium | 6 个仓库查询方法死代码 | RuleVersionRepository.java:26,36,47 等 |
| B-1 | ①正确 | Medium | 创建期用空上下文校验，then 非法值漏检 | RuleEngineService.java:354 / RuleDslEvaluator.java:51-53 |
| B-2 | ①正确 | Medium | ruleCode 唯一仅 DB 兜底，重复报 500 非业务错 | RuleEngineService.java:95-119 / RuleDefinitionRepository.java:26 |
| E-1 | ⑦降级 | Medium | evaluate 同步串行无超时/降级，违反“超时不阻断”原则 | RuleEngineService.java:246-253 / SPEC:1478 |
| X-7 | ⑩契约 | Medium | simulate 输出读不存在的标量字段 | RuleDefinitions.tsx:612-624 |
| Q-3 | ⑨测试 | Medium | 无前后端契约测试，X-* 全程无网 | （范围内无契约测试） |
| A-1 | ④审计 | Low | 仿真与真实执行共用 EXECUTE 审计动作，无法区分 | RuleEngineService.java:191,308-309 |
| O-1 | ⑧可观测 | Low | error_code/error_class/FAILED 真实路径从不写入 | RuleEngineService.java:301 |
| T-1 | ③隔离 | Low | test_case/execution 唯一键未含 tenant | postgres/V11:79,106 |
| D-1 | ⑤方言 | Low | dm/h2 缺 COMMENT 块 | dm/V11、h2/V11（113 行 vs 175 行） |
| D-2 | ⑤方言 | Low | expected_hit NUMBER(1) NOT NULL 与实体 Boolean 不齐 | oracle/V11:67 / RuleTestCase.java:25 |
| X-8 | ⑩契约 | Low | 两页整文件 eslint-disable no-page-mock + 硬编码 patientId | RuleValidate.tsx:1,81 / RuleDefinitions.tsx:1 |

合计：**Critical 6 / High 7 / Medium 6 / Low 6**。

---

## 六、改造建议（每条 C/H）

**Critical**
- **X-1/X-2/X-3（契约 400）**：统一前后端 DTO。前端 `useCreateRule` 改发 `dsl`/`explanation`（解析为对象再提交，非字符串）；`useSimulateRule`/`useEvaluateRules` 改发 `context`（JsonNode），evaluate 增 `eventId`/`ruleIds`、去掉 `patientId/payloadJson/inputPayload`。落一组契约测试（Q-3）锁死报文形状，禁止再漂移。
- **S-1（双审）**：为发布引入双人复核状态机（如 DRAFT→PENDING_REVIEW→PUBLISHED），第二复核人独立权限（`rule.review`≠`rule.publish`），阻断/CRITICAL 规则强制走双审；状态迁移与审计落库。
- **S-2（BLOCK 无强制）**：定义 BLOCK 在 evaluate 返回中的硬语义（调用方据此真实拦截），并补豁免（exemption with reason+actor）与应急 override 流程及独立审计记录；迁移加 `rule_exemption`/`rule_override` 证据表。
- **S-3（确认不落地）**：`RuleValidate` 命中表与 simulate 面板增“需医师确认”列，命中 requiresPhysicianConfirmation=true 时强制确认控件，确认动作回写执行日志/审计；与“进病历”动作联动（CONSTITUTION `:57`）。

**High**
- **C-1（状态机半成品）**：补 offline/archive/rollback 接口与状态迁移，写 `rollback_version_id`；或若 GA 不交付该能力，则从枚举/迁移/注释中删除 OFFLINE/ARCHIVED/rollback，避免“声明即谎言”。
- **Q-1（CONFLICT 装饰化）**：发布门禁对 CONFLICT 用例增加实质校验——要求其覆盖与其他规则条件重叠/互斥的场景，或至少校验同 trigger 下多规则同时命中的冲突裁决；否则该门禁应降级为“建议”而非“必过”。
- **临床表达力**：扩 DSL 算子集——between/区间、单位换算（带单位元数据的数值比较）、时间窗口（window_in/before/after/within_hours）、连续 N 次（sequence/count_over_window）、算术派生（支持 CrCl/eGFR 公式表达式）。每新增算子配阳/阴/边界用例。
- **X-4/X-5（虚构响应字段）**：前端类型严格对齐后端 record；命中表改读 `actions[]`（展示多动作）、`explanation` 按对象渲染；诊断回溯改用 `items[].executionId`。
- **X-6（ruleType 不符）**：前端枚举改为后端 10 类，或后端补 DRUG_SAFETY 等业务类目（需同步 DB CHECK 五方言）——二选一并统一。
- **S-4（缺确认不变量测试）**：补单测：DSL 省略 requiresPhysicianConfirmation 时 HIGH/CRITICAL/BLOCK 仍被强制为 true；并覆盖 CRITICAL 严重度归并。

---

## 七、总评

### done 是否名副其实？
**否。** `docs/backlog.md:60`（GA-ENG-API-05）与 `:79`（GA-ENG-RULE-01）均标 done，但：
- **后端**（API-05 的执行/解释/门禁/迁移/审计/租户）确为真实实现，质量较高，单独看接近可验收；
- **整体特性**（含 RULE-01 的“规则 DSL/模板、风险动作、解释”面向用户落地）**不成立**：前端三大写接口与后端契约系统性错位（必然 400），响应字段虚构，可视化/模板配置完全缺位，医疗安全三要素（双审/阻断强制/医师确认落地）缺失，DSL 表达力撑不起规范 §4。这不是“小瑕疵”，而是端到端假闭环 + 宪法/规范医疗安全红线未达标。
- 注：2026-05-28 既有 backlog 记录（`:149`）称“RULE-01 确认真实”——该结论仅基于后端，未覆盖前后端契约与临床表达力，**本独立审计不予采信其 done 判定**。

### 可否真实验收？
**不可。** 阻塞项（必须先解）：
1. X-1/X-2/X-3 契约修复 + Q-3 契约测试（否则任何用户操作都失败）；
2. S-1/S-2/S-3 医疗安全三要素（CONSTITUTION `:57`、SPEC `:1516,1122` 硬约束）；
3. 临床表达力最小集（至少单位换算 + 时间窗口 + 连续 N 次 + 区间，否则规范示例规则配不出）；
4. C-1 状态机：要么补全 offline/rollback，要么如实删除虚假声明；
5. 路径专家/医务处可视化或模板配置入口（规范 `:1019-1032` L1/L2 必达，否则目标用户无法使用）。

### 建议回退的 backlog ID
- **GA-ENG-RULE-01 → 回退 in_progress**：其“规则 DSL/模板、风险动作、解释”要求面向用户的可视化/模板配置、阻断风险动作强制、医师确认与解释落地，均未达标（保留后端确定性执行真实部分，非全盘推倒）。
- **GA-ENG-API-05 → 标注“后端 done、端到端未通过”，建议回退 in_progress**：API 后端真实，但 backlog 标题含“执行、解释”的对外闭环因契约错位不成立，且“发布/版本”状态机为半成品（C-1/C-3）。
- 关联建议核查 **GA-ENG-QA-03（医疗安全：医师确认/禁忌红线/高风险审核，`docs/backlog.md:108` done）**：本单元 S-1/S-2/S-3 显示“医师确认/高风险审核”在规则引擎侧并未真正接线，QA-03 的 done 可能同样名不副实，建议纳入下一轮独立复核。
