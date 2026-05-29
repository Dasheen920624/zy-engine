# U08 临床路径引擎 · 独立深度真实性审计

- 审计日期：2026-05-29
- 审计单元：临床路径引擎（专病包 / 路径模板 / 节点 / 边 / 患者入径 / 推进 / 变异 / 临床时钟 / 专病包同步）
- 关联 backlog：GA-ENG-API-06（路径引擎 API）、GA-ENG-PATH-01（路径引擎：专病包、分型分支、节点推进、变异、关键时钟、仿真）
- 审计方式：从零、独立、逐行；只读真实代码；**未读取/未信任** `docs/audit/` 下任何既有报告
- 取证范围：
  - 后端 `medkernel-backend/src/main/java/com/medkernel/engine/pathway/`（全部 49 个文件已读，核心逐行）
  - 迁移 `medkernel-backend/src/main/resources/db/migration/{postgres,oracle,dm,kingbase,h2}/V12__pathway_engine_api.sql`
  - 测试 `medkernel-backend/src/test/java/com/medkernel/engine/pathway/`（4 个文件全读）
  - 前端 `frontend/src/pages/clinical/PatientPathways.tsx`、`frontend/src/pages/tenant/PathwayTemplates.tsx`、`frontend/src/shared/api/hooks.ts`（路径段）
  - 权威规范：README / CONSTITUTION / 体验规范 / 落地方案 / `MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md` / `backlog.md`

---

## 一、单元概览

### 1.1 真实做到了什么（不否认的部分）

| 能力 | 真实性 | 证据 |
|---|---|---|
| 9 张关系表建模（专病包/画像/模板/节点/边/患者路径/变异/时钟/指标绑定） | 真 | `postgres/V12__pathway_engine_api.sql:3-231`，含 CHECK 约束、租户唯一键、租户索引 |
| 五方言迁移结构一致 | 真 | postgres/oracle/kingbase 各 395 行；dm/h2 各 231 行（仅缺 COMMENT，方言正常差异）；CREATE TABLE/INDEX 各方言均 25 条 |
| 草稿→发布门禁（起始/终止节点、节点编码唯一、边端点存在、非终止节点须有出边、时间窗非负） | 真 | `PathwayEngineService.java:419-450` |
| 推进器按图计算（出边过滤 + 优先级排序 + 目标可达校验） | 真（但弱，见下） | `PathwayProgressor.java:43-68` |
| 患者入径只允许 PUBLISHED 模板、创建首个时钟、dueAt 真实计算 | 真 | `PathwayEngineService.java:264-290`、`492-500`（`now.plusSeconds(timeWindow*60)`） |
| 多租户隔离（service 强制 tenant、repo 方法带 tenantId、controller `@DataScope(requireTenant=true)`） | 真 | `PathwayEngineService.java:553-559`；`PathwayEngineController.java:31`；repo 全部 `...AndTenantId(...)` |
| 审计事件 + 状态迁移留痕（创建/发布/入径/推进） | 真 | `PathwayEngineService.java:124-126,174-176,200-202,285-287,392-394` |
| 权限三分（read/write/publish）与角色映射 | 真 | `PathwayEngineController.java:49,62,119,167`；`PathwayEngineControllerSecurityTest.java:89-153` |
| 仓库层真连 H2 跑 Flyway 验证持久化与跨租户隔离 | 真 | `PathwayRepositoryTest.java:20-130` |

### 1.2 核心判定（先给结论）

后端推进器**本身不是写死下一步**（它真的读图、过滤出边、按优先级排序），这一点优于"硬编码 switch"。但本单元在**临床语义层面存在系统性的"真闭环假装"**：

1. **边条件 `condition_json` 从不被求值** → 所谓"分型分支 / 条件分支 / 风险分层"全是装饰，引擎对所有分支边一律取 priority 最小的那条；真正的"按患者上下文选分支"只能靠调用方手动把目标节点码塞进 `requestedNextNodeCode`。
2. **临床时钟只生不灭**：`TIMEOUT` / `MISSING_DATA` 状态定义了却永不写入，无任何超时巡检；时钟是"记了 dueAt 的死数据"。
3. **变异 100% 靠人工传 `eventType=VARIANCE`**，引擎不自动判定（偏离图、节点超时都不会自动记变异）。
4. **治疗节点（MEDICATION/SURGERY）无任何专家确认门禁**。
5. **前端两页大量假数据、假兜底、假合规宣称，且前后端枚举/字段契约系统性断裂**，真实数据下功能会瘫痪或显示编造内容。

→ 详见维度⑩与 Critical 列表。**两条 backlog 标记的 done 名不副实。**

---

## 二、十维度审计（findings 带 file:line）

### ① 业务正确性

- **[High] U08-B1 边条件不参与推进，分型/条件分支名存实亡。** `PathwayProgressor.advance` 选边逻辑只做 `filter(fromNodeCode==current)` + `sort(priority, edgeId)` + `getFirst()`，从不读取 `edge.conditionJson` 也不区分 `edge.edgeType`。`PathwayProgressor.java:43-68`。`condition_json` 在全代码仅出现于 record 定义（`PathwayEdge.java:24`）和写入（`PathwayEngineService.java:164`），无任何读取/求值点（grep 全 `engine/pathway` 仅这两处）。后果：规范 `MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md:1188`（"分型分支：同一病种按分型、风险、治疗条件进入不同分支，支持互斥/并行分支和再合流"）无法实现；`backlog.md:80` GA-ENG-PATH-01 标题里的"分型分支"未交付。
- **[High] U08-B2 节点超时不会自动记变异，违反明确验收用例。** 规范 `MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md:1060` 把"路径节点超时未记录变异"列为**反例（不允许出现的情况）**，`:1245` 重申"路径节点超时需记录合理变异"。代码无任何超时检测路径（见维度②/⑦），节点超时既不改时钟状态也不生成变异。
- **[Medium] U08-B3 仿真循环边界依赖节点数而非真实终止，且不反映条件。** `PathwayEngineService.simulate` 用 `for (i=0; i<=graphNodes.size(); i++)` 兜底防环（`:337`），但每步都走 `PathwayAdvanceEventType.COMPLETE` 且条件不求值，仿真结果对"阳性/阴性/边界/转诊/变异样例"（规范 `:1194` 要求发布前强制通过高风险用例）毫无表达力。
- **[Low] U08-B4 节点 `dependencyJson`、`configJson`、`responsibleRole` 写入后从不使用。** `PathwayEngineService.java:156-157` 写入；全代码无读取。节点"依赖前置完成""责任角色把关"均未落地，属残留字段。

### ② 医疗安全合规

- **[Critical] U08-S1 治疗节点无专家确认 / 高风险门禁。** `PathwayNodeType` 含 `MEDICATION`（用药）、`SURGERY`（手术）等高危类型（`PathwayNodeType.java`），但 `PathwayProgressor.advance` 与 `PathwayEngineService.advance` 对所有节点类型一视同仁，推进时不读 `nodeType`、不要求二次/专家确认。全 `engine/pathway` 无 `responsibleRole` 读取、无 signoff/二次确认逻辑（二次确认机制只存在于 `engine/security/PermissionCode.java:127-129`、`engine/knowledge`，路径 advance 从不调用）。对照规范 `CONSTITUTION.md:283`（专科路径安全边界）、`MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md:714`（"会诊结论由专家确认"）。本单元判伪重点之一命中。
- **[Critical] U08-S2 临床时钟无超时/缺数判定，"关键时钟"是死数据。** `ClinicalClockStatus` 定义 `TIMEOUT`、`MISSING_DATA`（`ClinicalClockStatus.java:11-12`），但这两个值**全代码仅出现于枚举定义**，无任何写入。时钟只在 `newClock` 设 `RUNNING`（`PathwayEngineService.java:498`）、在 `closeCurrentClocks` 设 `COMPLETED`/`VARIANCE`（`:455-462`）。无 `@Scheduled` 巡检（全后端仅 `ClinicalEventOutboxWorker` 一个定时任务，与路径无关）、`ClinicalClockRepository` 无任何按 `due_at`/`status` 查到期时钟的方法（`ClinicalClockRepository.java` 仅 2 个查询方法），即便迁移特意建了 `idx_clinical_clock_due`（`postgres/V12__pathway_engine_api.sql:210`）也无人使用。对照规范 `MEDKERNEL_BUSINESS_SCENARIO_DETAIL_SPEC.md:197`（clocks 接口须返回"超时/缺数"）、`:1189`（"临床关键时钟…缺数/超时/变异原因…支撑急症质量指标而非仅节点完成"）。本单元判伪重点"临床时钟是否真"命中——**不真**。
- **[Medium] U08-S3 advance 接口诚实声明"不自动诊断、不自动开医嘱"。** `PathwayEngineController.java:164`、`PathwayEngineService.java:38-40`。这一点是合规正向（推进器明确只做流程判断，不生成诊断/医嘱），予以记录。

### ③ 多租户隔离

- 未发现跨租户漏洞。service 每个公共方法首行 `requireCurrentTenant()`（`PathwayEngineService.java:553-559`，无 tenant 抛 `tenantMissing`）；所有 repo 查询均 `...AndTenantId(...)`；写入实体均带 `tenantId`；controller 类级 `@DataScope(requireTenant=true)`（`PathwayEngineController.java:31`）。
- 测试佐证：`PathwayRepositoryTest.repositoryQueriesDoNotLeakAcrossTenants`（`:107-115`）验证 tenant-B 查不到 tenant-A 模板；security test 三角色用例均在缺租户时返回 `ENG-BASE-001`（`PathwayEngineControllerSecurityTest.java:91-138`）。
- **[Low] U08-T1** `enterPatientPathway` 未校验 `request.startNodeCode` 之外的入参与租户的从属一致性以外的内容（如 patientId 是否属本租户患者主索引），但该校验属患者主索引单元职责，此处仅记录边界。

### ④ 审计证据链（入径/推进/变异留痕）

- 入径、推进、创建、发布均有 `auditPublisher.publish` + `transitions.record`（见 1.1）。变异事实落 `pathway_variance` 表（`PathwayEngineService.java:376-382`）。
- **[Medium] U08-A1 推进审计事件粒度过粗，不含事件语义。** `advance` 无论 COMPLETE/VARIANCE/EXIT 都发同一句 `AuditAction.EXECUTE` + "推进患者路径 {id}"（`PathwayEngineService.java:394`），未带 eventType、from/to 节点、变异类型。审计虽"有留痕"，但对"合理/不合理变异是否进入质控"（规范 `:1139`/`:1165`）的追溯支撑不足。
- **[Medium] U08-A2 失败路径不留审计。** advance/publish 抛 `ApiException` 时（如门禁失败、目标不可达）只抛异常，无失败审计事件。对照严重度定义"失败审计丢失"。属功能性缺口（非跨租户级），记 Medium。

### ⑤ 五方言一致性

- 9 张表、CHECK 约束（status/level/node_type/edge_type/variance_type/clock_status）、唯一键、索引在 postgres/oracle/kingbase/dm/h2 五方言齐备且一致（CREATE 语句各 25 条）。
- **[Low] U08-D1** dm/h2 不含 `COMMENT ON`（231 vs 395 行），属方言能力差异，非缺陷；记录备查。
- 未发现方言间表名/列名/约束值集不一致。

### ⑥ 代码净化

- **[Critical] U08-N1 前端两页头部主动禁用反 mock lint。** `frontend/src/pages/clinical/PatientPathways.tsx:1` 与 `frontend/src/pages/tenant/PathwayTemplates.tsx:1` 均为 `/* eslint-disable medkernel/no-page-mock */`——直接关闭了仓库自带的"页面禁止 mock"护栏，为下方假数据放行。
- **[Critical] U08-N2 PatientPathways 写死假患者台账当真展示。** `PatientPathways.tsx:71-92` 硬编码 PP-1001/PP-1002 两条患者路径（`status:"ACTIVE"`、`traceId:"TRACE-RULE-3312918"`/`"TRACE-RULE-8891023"`、`currentNodeCode:"TREAT_PLAN"/"CHECK_LEVEL"`），作为列表 `dataSource` 默认展示（`:381`）。注释自承认（`:67-70`：“后端…没有提供 listPatients API…我们模拟一些台账…在 table 事实里展示示例”）。
- **[Critical] U08-N3 Math.random 造业务标识 + Mock fallback。** `PatientPathways.tsx:134,150-157` 用 `Math.floor(Math.random()*…)` 生成 encounterId/patientPathwayId/traceId；`:147` 注释 “Mock fallback”，当后端不返回 body 时塞入随机假实例。
- **[Critical] U08-N4 诊断抽屉写死编造的临床归因与"哈希"。** `PatientPathways.tsx:806` `"SHA-256-MOCK-HASH"`；`:827` 写死 “依据《社区获得性肺炎指南 §3.2》自动激活抗感染时钟，且检测到血常规白细胞偏高，流转归因成立”；`:811` `riskLevel||"LOW"`。这些都是后端不返回时的"展示性兜底"，等于把编造内容当真实诊断展示。
- **[Low] U08-N5 注释口语化、夹带营销话术。** 如 `PatientPathways.tsx:425`“极宽抽屉”、`:748`“物理脱靶退出”、`PathwayTemplates.tsx:262`“时窗门禁”。属命名/注释问题。

### ⑦ 错误处理与降级

- 推进器入参校验完整（命令不完整/当前节点不存在/目标不可达均抛 `ENG_PATHWAY_006`，`PathwayProgressor.java:30-82`）；JSON 序列化失败抛 `ENG_PATHWAY_001`（`PathwayEngineService.java:573-582`）；状态机门禁（已完成/退出不可推进 `:538-542`，非草稿不可发布 `:532-536`）。
- **[High] U08-E1 无时钟超时降级链，违反"节点超时"语义。** 规范要求"节点超时"为一等公民（`:542`/`:1060`/`:1175`/`:1245`），但既无超时检测也无超时后的降级/变异/质控触发（承 U08-S2/B2）。
- **[Low] U08-E2** `closeCurrentClocks` 的 switch 只覆盖 VARIANCE/COMPLETE/EXIT（`:455-458`），新增事件类型会编译期暴露（switch over enum 无 default），属安全写法，记录为正向。

### ⑧ 可观测性

- 写动作均带 traceId（`RequestContext.currentTraceId()`），状态迁移经 `StateTransitionRecorder`，diagnose 端点经统一 `DiagnoseResponseAssembler`（`PathwayEngineService.java:406-417`）。
- **[High] U08-O1 前端 diagnose 展示与后端响应契约断裂（见维度⑩ U08-X3）**，导致诊断页观测信息要么空、要么走假兜底，可观测性"看起来有、实际假"。
- **[Low] U08-O2** diagnose 的 `PayloadRef` 用 `sizeBytes=0` + inline digest（`:410-412`），payload 大小恒 0，监控侧无法据此判断负载，属轻度失真。

### ⑨ 测试覆盖与有效性

- 有效部分：`PathwayProgressorTest`（7 用例覆盖默认边/显式目标/完成/变异停留/变异续走/不可达拒绝）、`PathwayEngineServiceTest`（create/publish 门禁/入径/COMPLETE/VARIANCE/EXIT/diagnose，mock 仓库 + 真 progressor）、`PathwayRepositoryTest`（真 H2 + Flyway + 跨租户）、security test（权限三分 + 租户兜底）。
- **[High] U08-Q1 关键临床语义零覆盖——且测试反而自证分支是假的。** `PathwayProgressorTest.respectsExplicitTargetNodeWhenItIsReachable`（`:28-35`）让 ASSESS 有两条出边（→LAB DEFAULT prio10、→SURGERY CONDITION prio20），断言只有"**显式传 SURGERY**"才走 CONDITION 边；而 `followsDefaultEdgeWhenNodeCompletes`（`:18-26`）证明不传目标永远走 prio 最小的 LAB。即测试本身证明"条件边靠人手选、引擎不按条件走"。所有用例 `condition=null`（`:114`），从未测试条件求值——因为没有该逻辑。
- **[High] U08-Q2 无任何测试覆盖：时钟 TIMEOUT/MISSING_DATA、节点超时自动变异、治疗节点专家确认、并行/合流分支。** 这些都是规范要求的核心能力，测试缺位与功能缺位互相印证（不是"测试漏了"，是"功能没有"）。
- **[Medium] U08-Q3 全绿不等于真。** 现有测试全部围绕"已实现的薄逻辑"构造，绿色仅证明 CRUD + 优先级选边 + 状态机迁移正确，不能支撑 backlog 的 done。

### ⑩ 前后端契约一致

> 本维度问题最严重，且多为 Critical/High，因为契约断裂使"真实数据下功能不可用"。

- **[Critical] U08-X1 患者路径状态枚举前后端互不相交，真实数据下推进面板瘫痪。** 后端 `PatientPathwayStatus = ENTERED/NODE_EXECUTING/VARIANCE/COMPLETED/EXITED`（`PatientPathwayStatus.java`），入径后真实返回 `NODE_EXECUTING`（`PathwayEngineService.java:281`）。前端 `hooks.ts:463` 定义 `PatientPathwayStatus = "ACTIVE" | "COMPLETED" | "EXITED"`，UI 多处判断 `status === "ACTIVE"`（`PatientPathways.tsx:466,576,631,716`）。真实入径返回 `NODE_EXECUTING` 时，三个推进 Tab（标准流转/变异/退径）的 `disabled={status!=="ACTIVE"}` 全部命中 → **医生拿到真实患者路径后无法做任何推进操作**；状态 Badge 也会显示原始字符串（`config[status]` 取不到）。
- **[Critical] U08-X2 节点/边/层级枚举前后端不同集合，按前端默认模板创建必被 DB CHECK 拒绝（假闭环）。**
  - 节点类型：前端默认模板用 `"START"/"PROCESS"/"BRANCH"/"STOP"`（`PathwayTemplates.tsx:47-50`），`hooks.ts:461` 同；后端枚举 `SCREENING/ASSESSMENT/EXAM/LAB/MEDICATION/SURGERY/NURSING/REHAB/DISCHARGE/FOLLOWUP/QUALITY`，DB `ck_pathway_node_type` 硬卡这 11 值（`postgres/V12__pathway_engine_api.sql:101-104`）。
  - 边类型：前端 `"STANDARD"/"CONDITIONAL"/"VARIANCE"/"EXCEPTION"`（`PathwayTemplates.tsx:55-58`，`hooks.ts:462`）；后端 `DEFAULT/CONDITION/RISK_STRATIFICATION/PATIENT_CHOICE/RESOURCE_UNAVAILABLE/PHYSICIAN_DECISION/ROLLBACK`，DB `ck_pathway_edge_type` 硬卡（`:127-130`）。
  - 模板层级：前端 `"CLINICAL"/"BUSINESS"`（`PathwayTemplates.tsx:317,476-477`，`hooks.ts:460`）；后端 `STANDARD/GROUP/HOSPITAL/DEPARTMENT/SPECIALTY`，DB `ck_pathway_template_level` 硬卡（`:71-73`）。
  → 用户用页面提供的默认 JSON 创建路径模板，后端枚举反序列化或 DB CHECK 必然失败。这是一个**演示能跑通的假象、真实创建必崩**的闭环。
- **[Critical] U08-X3 仿真与诊断响应字段不匹配，前端永远拿不到真数据。**
  - 仿真：后端 `PathwaySimulationResponse` 字段为 `nodeTrajectory` + `finalStatus`（`PathwaySimulationResponse.java:13-16`）；前端 hooks 声明字段为 `simulatedPath`（`hooks.ts:572`），页面读 `result.simulatedPath`（`PathwayTemplates.tsx:189`）。后端 JSON 实际键是 `nodeTrajectory` → 前端 `simulatedPath` 恒 `undefined` → 沙箱仿真轨迹**永远空白**；`finalStatus` 被丢弃。
  - 诊断：后端 `DiagnoseResponse` 字段为 `entityType/entityId/currentStatus/entity/stateHistory/auditEvents/relatedEntities/payloadSummary/traceId/links`（`shared/observability/DiagnoseResponse.java`）；前端期望 `executionId/inputPayloadSummary/explanationSnapshot/riskLevel/statusHistory`（`hooks.ts:311-335`，`PatientPathways.tsx:798-857`）。字段全不对 → 诊断抽屉全部走假兜底（承 U08-N4），把"SHA-256-MOCK-HASH"和编造指南文本当真展示，并配文"100% 透明及不可篡改审计"（`PatientPathways.tsx:791`）。
- **[Medium] U08-X4 模板状态前端缺 ARCHIVED，渲染会崩。** 后端/DB 含 `ARCHIVED`（`:74`），前端 `hooks.ts:459` 与 `PathwayTemplates.tsx:233-237` 的 status config 仅 DRAFT/PUBLISHED/OFFLINE；遇 ARCHIVED 模板 `config[status].color` 抛 `Cannot read properties of undefined`。
- **[Medium] U08-X5 前端 UI 文案宣称引擎"自动评估出边条件"，与后端不符。** `PatientPathways.tsx:585`“会自动触发节点出边（Edge）的评估”、`PathwayTemplates.tsx:57-58` 默认边内置 `conditionJson` DSL（`{"fact":"patient.condition","operator":"equals",...}`）并在边表展示"流转条件 (DSL)"（`:636-641`），给用户"引擎按条件分支"的错误预期，而后端 progressor 不解析条件（承 U08-B1）。

---

## 三、七角色评估

| 角色 | 关注 | 真实可用？ | 证据 |
|---|---|---|---|
| **临床医生（患者进路径）** | 入径 → 看当前节点/时钟 → 标准推进/登记变异/退径 | **否（瘫痪）** | 入径 API 真（`Service.java:264-290`）；但前端拿到真实 `NODE_EXECUTING` 后三推进 Tab 全 disabled（U08-X1），医生无法推进；列表是写死假台账（U08-N2），看不到真实患者 |
| **路径专家（专家模式画 6 节点）** | 画节点/边/分支条件/时间窗 → 仿真 → 发布 | **否（创建必崩 + 分支假 + 仿真空）** | 用页面默认 JSON 创建因 node_type/edge_type/level 三处枚举不符被 DB CHECK 拒（U08-X2）；即便手填正确枚举，条件分支不被引擎执行（U08-B1）；仿真轨迹因字段不符永远空白（U08-X3） |
| **科主任 / 医务处** | 变异是否进质控、节点超时治理 | **否** | 节点超时不自动记变异（U08-B2/S2，违反规范反例 `:1060`）；变异审计粒度粗（U08-A1） |
| **信息科 / 实施工程师** | 五方言部署、迁移可重复 | **基本可** | 五方言迁移结构一致、约束齐全（维度⑤） |
| **质控 / 急症质量** | 关键时钟支撑急症质量指标 | **否** | 时钟 TIMEOUT/MISSING_DATA 永不写入，无巡检（U08-S2），无法支撑"急症质量指标"（规范 `:1189`） |
| **安全 / 合规官** | 治疗节点专家确认、审计不可篡改 | **否** | 治疗节点无专家确认门禁（U08-S1）；前端伪称"不可篡改审计"实为假兜底（U08-N4/X3） |
| **平台 / 架构** | 多租户隔离、可观测、降级 | **部分** | 租户隔离真（维度③）；但无超时降级（U08-E1）、前端契约断裂致可观测失真（U08-O1） |

---

## 四、Findings 汇总表

| ID | 维度 | 严重度 | 一句话 | 关键 file:line |
|---|---|---|---|---|
| U08-S1 | ② | Critical | 治疗节点(MEDICATION/SURGERY)推进无专家确认门禁 | `PathwayProgressor.java:43-68`；`PathwayEngineService.java:358-399` |
| U08-S2 | ② | Critical | 临床时钟 TIMEOUT/MISSING_DATA 永不写入、无超时巡检，时钟是死数据 | `ClinicalClockStatus.java:11-12`；`PathwayEngineService.java:455-500` |
| U08-N1 | ⑥ | Critical | 前端两页头部主动禁用反 mock lint | `PatientPathways.tsx:1`；`PathwayTemplates.tsx:1` |
| U08-N2 | ⑥ | Critical | 写死两条假患者路径当真列表展示 | `PatientPathways.tsx:71-92,381` |
| U08-N3 | ⑥ | Critical | Math.random 造业务 ID + Mock fallback | `PatientPathways.tsx:134,147,150-157` |
| U08-N4 | ⑥ | Critical | 诊断抽屉写死编造临床归因 + "SHA-256-MOCK-HASH" | `PatientPathways.tsx:806,811,827` |
| U08-X1 | ⑩ | Critical | 患者状态枚举前后端不相交，真实数据下推进面板全瘫 | 后端`PatientPathwayStatus.java`；前端`hooks.ts:463`；`PatientPathways.tsx:576,631,716` |
| U08-X2 | ⑩ | Critical | 节点/边/层级枚举不同集合，按默认模板创建必被 DB CHECK 拒（假闭环） | `PathwayTemplates.tsx:47-58,317`；`hooks.ts:460-462`；`postgres/V12__pathway_engine_api.sql:71-73,101-104,127-130` |
| U08-X3 | ⑩ | Critical | 仿真(nodeTrajectory vs simulatedPath)/诊断响应字段全不匹配，前端拿不到真数据 | `PathwaySimulationResponse.java:13-16` vs `hooks.ts:572`；`DiagnoseResponse.java` vs `hooks.ts:311-335` |
| U08-B1 | ① | High | 边 condition_json 从不求值，分型/条件分支名存实亡 | `PathwayProgressor.java:43-68`；`PathwayEngineService.java:164` |
| U08-B2 | ① | High | 节点超时不自动记变异，违反规范明确反例 | 规范`:1060,1245`；代码无对应实现 |
| U08-E1 | ⑦ | High | 无时钟超时检测与降级链 | `ClinicalClockRepository.java`（无 due 查询）；无 @Scheduled |
| U08-O1 | ⑧ | High | 前端 diagnose 与后端响应契约断裂致观测信息假 | 见 U08-X3 |
| U08-Q1 | ⑨ | High | 关键临床语义零测试，且测试反证条件边靠人手选 | `PathwayProgressorTest.java:28-35,114` |
| U08-Q2 | ⑨ | High | 时钟超时/专家确认/并行合流分支无测试 | 测试目录缺位 |
| U08-A1 | ④ | Medium | 推进审计粒度过粗，不含 eventType/节点/变异类型 | `PathwayEngineService.java:394` |
| U08-A2 | ④ | Medium | 失败路径(门禁/不可达)不留审计事件 | `PathwayProgressor.java:50-65`；`Service.java:419-450` |
| U08-B3 | ① | Medium | 仿真按节点数兜底循环、不反映条件，高风险用例无表达力 | `PathwayEngineService.java:337-348` |
| U08-Q3 | ⑨ | Medium | 全绿测试仅覆盖薄逻辑，不支撑 done | 测试整体 |
| U08-X4 | ⑩ | Medium | 前端模板状态缺 ARCHIVED，渲染会崩 | `hooks.ts:459`；`PathwayTemplates.tsx:233-237` |
| U08-X5 | ⑩ | Medium | UI 宣称引擎自动评估出边条件，与后端不符 | `PatientPathways.tsx:585`；`PathwayTemplates.tsx:57-58,636-641` |
| U08-O2 | ⑧ | Low | diagnose payload sizeBytes 恒 0，监控失真 | `PathwayEngineService.java:410-412` |
| U08-B4 | ① | Low | dependency/config/responsibleRole 写入后从不使用 | `PathwayEngineService.java:156-157` |
| U08-T1 | ③ | Low | 入径未校验 patientId 与租户患者主索引从属（属他单元职责，记边界） | `PathwayEngineService.java:264-290` |
| U08-D1 | ⑤ | Low | dm/h2 不含 COMMENT（方言差异，非缺陷） | `dm/V12__...sql`、`h2/V12__...sql` |
| U08-N5 | ⑥ | Low | 注释口语化/营销话术 | `PatientPathways.tsx:425,748` 等 |

**计数：Critical 9 · High 6 · Medium 6 · Low 5（合计 26）。**

---

## 五、改造建议（每条 C/H 一策）

### Critical

- **U08-S1（治疗节点专家确认）**：在 `PathwayProgressor`/`PathwayEngineService.advance` 增加节点类型门禁：对 `MEDICATION/SURGERY`（及可配置的高危类型）要求 advance 请求携带专家确认凭据（确认人、确认时间、可选第二确认人），缺失则抛专用错误码并留失败审计；门禁规则建议落到节点 `config_json` 的 schema 化字段（如 `requireExpertConfirm`、`signoffRole`），由引擎读取执行。
- **U08-S2 / U08-E1（时钟超时）**：新增时钟巡检（`@Scheduled` worker，复用 `ClinicalEventOutboxWorker` 的 traceId 传播范式），按租户扫描 `status=RUNNING AND due_at < now` 的时钟，标 `TIMEOUT` 并发审计；为 `ClinicalClockRepository` 增 `findByTenantIdAndStatusAndDueAtBefore`（利用既有 `idx_clinical_clock_due`）；MISSING_DATA 在指标绑定 required 且到期未采集时写入。
- **U08-B2（超时自动变异）**：时钟标 TIMEOUT 时，按规范 `:1060` 自动生成一条 `SYSTEM_REASON`/`MEDICAL` 待确认变异（合理性留人工/质控判定），保证"节点超时必记变异"。
- **U08-N1~N4（前端假数据/假兜底）**：删除 `eslint-disable medkernel/no-page-mock`；移除写死台账与 Math.random/Mock fallback；列表改为后端真实数据来源（需后端补 `GET /patients` 列表 API，见下）；诊断抽屉的字段一律由后端真实响应渲染，无数据时显示"暂无"，**禁止**任何编造文本与"不可篡改"宣称。
- **U08-X1（状态枚举）**：前端 `PatientPathwayStatus` 改为与后端一致的 `ENTERED/NODE_EXECUTING/VARIANCE/COMPLETED/EXITED`，推进 Tab 的可用条件改为 `status===NODE_EXECUTING || status===ENTERED || status===VARIANCE`。
- **U08-X2（创建枚举）**：前端 `PathwayNodeType/PathwayEdgeType/PathwayTemplateLevel` 及默认 JSON 模板全部对齐后端枚举值集；建议由后端暴露枚举字典接口或共享类型，避免再次漂移。
- **U08-X3（响应字段）**：统一仿真字段（`nodeTrajectory`/`finalStatus`）与诊断字段（按 `DiagnoseResponse` 真实结构）；前端按真实字段渲染轨迹与诊断；补端到端契约测试。

### High

- **U08-B1（条件分支）**：实现确定性条件求值器——`advance`/`simulate` 注入患者上下文（事件/检验/分型事实），按 `edge.conditionJson` 的确定性 DSL（fact/operator/value，禁 Math.random、禁吞错）筛选可行出边，再按 priority 选；无模型、纯确定性，符合 `CONSTITUTION.md:279/283`。同时支持互斥/并行分支与再合流（规范 `:1188`）。
- **U08-O1**：随 U08-X3 一并修复。
- **U08-Q1/Q2**：补测试——条件分支按上下文选边、时钟超时→TIMEOUT→自动变异、治疗节点缺确认被拒、并行分支合流、仿真高风险用例集（阳性/阴性/边界/转诊/变异，规范 `:1194`）。

---

## 六、总评

- **done 名副其实？** 否。`GA-ENG-API-06` 与 `GA-ENG-PATH-01` 均标 `done`（`backlog.md:61,80`），但 GA-ENG-PATH-01 标题列举的"**分型分支**""**关键时钟**""**仿真**"三项核心能力实质未交付：分支条件不求值（U08-B1）、关键时钟无超时/缺数（U08-S2）、仿真前端拿不到结果且不反映条件（U08-X3/B3）。GA-ENG-API-06 的 advance API 在后端层薄可用，但前端契约系统性断裂使整条用户旅程不可用。
- **可否真实验收？** 否。临床医生无法推进真实患者路径（U08-X1）、路径专家用页面默认模板创建必崩（U08-X2）、诊断展示为编造内容（U08-N4/X3）、关键时钟与超时变异缺失（U08-S2/B2，且违反规范明确反例 `:1060`）、治疗节点无专家确认（U08-S1）。
- **阻塞项（必须先清零）**：9 条 Critical 全部为验收阻塞，尤以 U08-S1/S2（医疗安全）、U08-X1/X2/X3（真实数据下功能不可用 + 假闭环）、U08-N2/N3/N4（假数据/假兜底/假合规宣称）为甚。
- **后端 vs 前端定性**：后端"骨架真、临床语义空"（推进器真按图算，但条件/时钟/确认/超时全缺）；前端"假数据 + 契约断裂 + 假兜底"，是本单元欺骗性最强的部分。

### 建议回退的 backlog

| backlog ID | 当前 | 建议 | 理由 |
|---|---|---|---|
| `GA-ENG-PATH-01` 路径引擎：专病包、分型分支、节点推进、变异、关键时钟、仿真 | done | **回退 → in-progress / reopened** | 分型分支(U08-B1)、关键时钟超时缺数(U08-S2)、仿真(U08-X3/B3)未交付；节点超时变异违反规范反例(U08-B2) |
| `GA-ENG-API-06` 路径引擎 API：模板、专病包、患者路径、节点推进、变异、关键时钟 | done | **回退 → in-progress / reopened** | 治疗节点专家确认缺失(U08-S1)；患者路径列表 API 缺失致前端造假(U08-N2)；前后端枚举/字段契约系统性断裂致真实数据不可用(U08-X1/X2/X3) |

> 注：本报告仅依据真实代码、迁移、测试与权威规范取证，未参考任何既有审计结论。所有 finding 均可按所列 file:line 复核。
