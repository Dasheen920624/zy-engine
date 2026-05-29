# U02 独立深度审计 · 推荐 / CDSS 临床决策支持单元

- 审计日期：2026-05-29
- 审计员视角：资深医疗系统审计专家（架构师 + 临床从业者）
- 审计方式：从零、独立、逐行取证；**未阅读** `docs/audit/` 下任何既有报告，不采信任何既有 AI 结论
- 取证范围（实际逐行读完）：
  - 后端：`medkernel-backend/src/main/java/com/medkernel/engine/recommendation/`（28 个文件全读）
  - 迁移：`db/migration/{postgres,oracle,dm,kingbase,h2}/V13__recommendation_cdss_api.sql`（五方言对账）
  - 测试：`recommendation/` 下 3 个测试文件全读
  - 前端：`frontend/src/pages/clinical/CdssFatigue.tsx`（全读）+ `frontend/src/shared/api/hooks.ts` 推荐契约段（848–1083 行）+ `DiagnoseResponse` 类型（316–330 行）
  - 旁证：`shared/observability/{DiagnoseResponse,DiagnoseResponseAssembler,BusinessMetrics}.java`、`shared/api/error/ErrorCode.java`、`docs/backlog.md`
- 对应 backlog：`GA-ENG-API-07`（后端，本单元核心代码）、`GA-ENG-CDSS-01`（前端控制台，done）、`GA-ENG-API-13`（大列表，仅范围列入，非本单元代码）

---

## 一、单元概览

本单元实现 CDSS（临床决策支持）的"事实读写 + 治理"层，**刻意不做规则推理本身**（候选卡由上游引擎/适配器提交，首版允许随触发请求携带 `candidateCards`）。

后端能力（`RecommendationEngineController` + `RecommendationEngineService`）：
1. `POST /triggers` 受控写入（`recommendation.write`）：登记触发，落库候选卡 / 来源 / 初始疲劳信号；
2. `GET /cards`、`GET /cards/{id}`、`GET /cards/{id}/sources` 读（`recommendation.read`）；
3. `POST /cards/{id}/feedback` 医师反馈（`recommendation.accept`）：推进卡状态机 + 采集疲劳信号；
4. `GET /fatigue-signals` 疲劳信号分页读；
5. `GET /triggers/{id}/diagnose` 诊断聚合。

**核心判断：后端 + 迁移 + 后端测试三层是真实、扎实、可独立验收的**——医疗安全校验真实有效、多租户隔离严密、不写医嘱、失败留痕、五方言一致、测试为真实行为断言（非 mock 固化假绿）。

**问题全部集中在前端契约层**：`hooks.ts` 系统性虚构了后端**根本不存在**的字段，`CdssFatigue.tsx` 用这些恒为 `undefined` 的字段叠加硬编码兜底值（`90 分`、`Class I`、`LOW`），把虚构数据当真实临床证据 / 风险定级展示给医师；整个"可信诊断审计" Drawer 因字段名全错而**全空/全兜底**，却挂着"100% 透明及非假 MOCK 审计线索"的文案——构成医疗安全层面的**假数据展示**与**假审计闭环**。后端真实触发后，前端在展示侧"二次造假"，正是本单元的判伪重点命中。

> 关键事实：后端 `RecommendationCard`（实体 + `recommendation_card` 表）**没有** `patient_id` / `encounter_id` / `scenario_code` 列（这些在 `recommendation_trigger` 上，列表过滤靠 JOIN trigger 取——见 `RecommendationCardRepository.java:24,28-29`）；`RecommendationSource` **没有** `authority_score` / `evidence_level` / `content` / `source_ref`（实际是 `source_title` / `summary` / `source_ref_id`）；`recommendation_fatigue_signal` **没有** `governance_threshold`（实际是 `occurrence_count`，恒为 1）。这些已由"DTO + 五方言迁移 + grep 全库为空"三重确认。

---

## 二、十维度逐条结论（带 file:line）

### ① 业务正确性

- **[真] 状态机映射正确且自洽**。`nextStatus()`（`RecommendationEngineService.java:302-310`）与 `feedbackSignal()`（:318-326）对 5 种反馈类型一一映射；`isClosed()`（:332-338）终止态集合（ACCEPTED/REJECTED/DISMISSED/SUPPRESSED/EXPIRED）与枚举注释（`RecommendationCardStatus.java:7`）一致，迁移 CHECK 也吻合（`postgres/V13:68-70`）。
- **[真] 触发状态判定正确**：候选卡空 → `NO_CARD`，非空 → `EVALUATED`（:97-99）。
- **[High] 前端 `interruptLevel` 取值体系与后端完全不符，导致拦截级别全部错误展示**。后端枚举为 `SILENT/INFO/WEAK_INTERRUPTIVE/STRONG_INTERRUPTIVE`（`RecommendationInterruptLevel.java:10-13`、迁移 `postgres/V13:65-67`），前端类型却定义为 `"NONE"|"SOFT"|"HARD"`（`hooks.ts:859`），并据此上色与判定（`CdssFatigue.tsx:223` `{HARD,SOFT,NONE}`、:459 `interruptLevel === "HARD" ? "purple" : "volcano"`）。后端真实返回 `WEAK_INTERRUPTIVE` 等值时，前端永不匹配 → 表格走默认蓝色、详情错误地把所有非空值渲染为"volcano（疑似强打断色）"。**临床误导**：医师无法从 UI 正确区分强打断/弱打断/静默。
- **[Medium] `triggerStatus` 前端取值错误**：前端 `"SUCCESS"|"FAILED"`（`hooks.ts:861`），后端为 `RECEIVED/EVALUATED/NO_CARD/FAILED`（`RecommendationTriggerStatus.java:7-10`）。当前前端未直接渲染该字段，影响有限，仍属契约错误。

### ② 医疗安全合规（AI 标识 / 医师确认 / 不自动开医嘱 / 不编造）

- **[真·亮点] 后端不自动写医嘱 / 诊断 / 病历**。`trigger()` 仅写 trigger/card/source/fatigue 四类表；`feedback()` 仅写 feedback 表 + 推进卡状态 + 写疲劳信号（`RecommendationEngineService.java:80-122,168-193`）。全单元无任何写医嘱/病历/随访的代码路径。类注释（:30-32）与迁移注释（`postgres/V13:206` "只记录医师处理动作，不写病历或医嘱"）一致。
- **[真·亮点] 医师确认门禁真实存在且强制**。`validateCards()`（:241-254）：高风险/红线未 `requiresPhysicianConfirmation` → `ENG_REC_006`；强打断非高风险 → `ENG_REC_001`；每卡至少一来源 → `ENG_REC_005`。迁移层亦有 CHECK 兜底。
- **[真] AI 标识落库**：`aiGenerated` 入参直存（:262），迁移有列与 CHECK（`postgres/V13:49,72`），实体注释明确"AI 候选必须显式标识，不能伪装为人工规则结论"。
- **[Critical] 前端把硬编码常量当"真实临床证据级别 / 权威度评分"展示给医师**。`CdssFatigue.tsx:489` `权威度评分: {source.authorityScore || 90}分`、:506 `{source.evidenceLevel || "Class I"}`。后端 `RecommendationSource` **完全没有** `authorityScore` / `evidenceLevel`（`RecommendationSource.java:17-34`；迁移 `postgres/V13:80-101` 无对应列；全库 grep `authority_score|evidence_level` 为空）。因此两表达式**永远取兜底值**，向医师稳定展示伪造的"权威度 90 分""证据等级 Class I"。**命中判伪铁律：前端写死常量当真实数据展示**，且发生在最敏感的"循证证据强度"维度，可直接误导临床采纳决策。
- **[Critical] 前端把硬编码 `LOW` 当推荐风险定级展示给医师（诊断面板）**。`CdssFatigue.tsx:721,723` `diagnoseData.riskLevel === "HIGH" ? "red" : "orange"` 与 `{diagnoseData.riskLevel || "LOW"}`。后端 `DiagnoseResponse`（`DiagnoseResponse.java:13-25`）**没有** `riskLevel` 字段，故恒走兜底 `"LOW"`。一张后端真实风险为 `CRITICAL/HIGH` 的卡，在诊断追溯面板会被错误地标注为 `LOW`。**临床安全风险：风险降级误导**。
- **[真] 医师反馈署名取真实登录用户，前端不伪造**。操作者 `actor = RequestContext.currentUserId()`（:184,352-354），前端反馈入参仅 `feedbackType/reasonCode/reasonText`（`hooks.ts:1035-1039`、`CdssFatigue.tsx:164-168`），未传任何 `physicianId`。无 `PHYS-xxxx` 类硬编码署名。测试 `feedbackCap.getValue().operatorId()).isEqualTo("doctor-1")`（`RecommendationEngineServiceTest.java:177`）佐证。**此项符合红线要求**。
- **[真] "采纳不等于下达医嘱"红线在文案上守住**。`CdssFatigue.tsx:172` "已登记采纳，已生成临床决策证据；是否下达医嘱请在 HIS 中确认。"、:539 "是否下达/撤销医嘱由医师在 HIS 中确认"。未谎称"医嘱流转成功"。**符合**（仅 "已生成临床决策证据" 略有夸大，见 ⑩）。

### ③ 多租户隔离

- **[真·亮点] 隔离严密**。所有 Repository 方法均双键（`...AndTenantId`）；`countByFilter`/`pageByFilter` 的 JOIN 显式带 `t.tenant_id = c.tenant_id`（`RecommendationCardRepository.java:24,37`），不存在仅靠 card.tenant_id 而 trigger 维度泄露的缝隙；疲劳/来源/反馈查询同理（`RecommendationFatigueSignalRepository.java:23`）。
- `tenantId()` 缺租户即抛（`RecommendationEngineService.java:344-350`），控制器类级 `@DataScope(requireTenant = true)`（`RecommendationEngineController.java:33`）。
- 真实库测试覆盖跨租户零泄露（`RecommendationRepositoryTest.java:80-97`，含"正确租户能读到，证明隔离来自过滤而非数据缺失"的反证）。

### ④ 审计证据链（成功 + 失败留痕 / trace_id / outcome）

- **[真·亮点] 失败也留痕**。`validateCards` 抛错前用 `IsolatedAuditPublisher.publishInNewTx(AuditEvent.failure(...))` 发 `outcome=FAILED` 审计（`RecommendationEngineService.java:84-91`），独立子事务不被主事务回滚带走；测试 `verify(isolatedAudit).publishInNewTx(...)`（`...ServiceTest.java:138,154`）佐证。
- 成功路径 `EXECUTE`/`FEEDBACK` 审计 + `StateTransitionRecorder.record` 同事务写状态历史（:118-120,188-191）。
- `traceId` 全链贯穿（:94,356-359；各 save 携带 trace_id；表均有 `trace_id` 列）。
- **[Medium] 前端诊断面板谎称展示底座审计线索，实则全空**——见 ⑧/⑩，审计数据本身在后端是真的，但前端取不到，对外形成"审计可视化已闭环"的假象。

### ⑤ 五方言一致性

- **[真] 五方言齐整**。postgres/oracle/dm/kingbase/h2 各 5 张表（`grep -c CREATE TABLE` 均为 5），三项关键 CHECK（card_status / fatigue_signal / card_interrupt）各方言均在；全方言均无 `governance_threshold|authority_score|evidence_level` 列（grep 全空）。
- Oracle 用 `NUMBER(19) GENERATED ALWAYS AS IDENTITY` / `VARCHAR2`，与 postgres `BIGSERIAL` 等价映射，符合预期方言差异。

### ⑥ 代码净化

- **[真] 后端无判伪信号**：无 `Math.random`、无写死 switch 充结果、无 `mock/模拟/占位` 注释当业务、无 `FORCE_*` 生产钩子、无 UUID 充哈希（哈希来自前端 `crypto.subtle.digest` 真 SHA-256，`CdssFatigue.tsx:59-65`）。业务 id 用 `UUID.randomUUID()` 作主键前缀（`rt-/rc-/rs-/rf-/rfs-`）是正当用法，非充哈希。
- **[Low] 前端死代码 / 残留虚构扩展字段**：`hooks.ts:883-891` `severity/recommendations/evidenceSummary`、:879 `changeSummary` 等"嵌入与全屏决策终端可选扩展属性"在后端无对应，且本页未使用，属类型层噪音，易诱导后续误用。

### ⑦ 错误处理与降级

- **[真] 错误码完整且语义正确**：`ENG_REC_001..006`（`ErrorCode.java:75-80`）状态码合理（404/409/400 区分得当），与 Service 抛点一致。
- **[Medium] 前端 catch 不伪造成功（合格）但错误信息可能误导**。`handleFeedback` catch 统一提示"反馈提交失败，卡片可能已过期或已处于终止态"（`CdssFatigue.tsx:185`）——把所有错误（含网络/权限/校验）归因为"过期/终止态"，对医师而言可能掩盖真实失败原因。属降级文案不精确，非伪造（未谎称成功）。

### ⑧ 可观测性（业务埋点）

- **[真] CDSS 提醒计数真实埋点**。每发一张卡 `businessMetrics.incCdssAlerts()`（`RecommendationEngineService.java:115`），底层 `Counter.builder("medkernel_cdss_alerts_total").increment()`（`BusinessMetrics.java:48,70`），测试 `verify(businessMetrics).incCdssAlerts()`（`...ServiceTest.java:110`）佐证。
- **[High] 缺关键业务埋点：采纳率 / 不采纳率 / 疲劳抑制计数无任何指标**。`feedback()` 路径未对 ACCEPT/REJECT/DISMISS 做任何计数（:168-193 全程无 `businessMetrics` 调用）；疲劳治理本身未实现（见 ⑨/总评），故无抑制告警计数。任务明确要求"采纳率 / 告警计数等业务埋点"，目前仅有"告警发出数"，**采纳侧观测为零**。
- **[High] 前端"疲劳治理可视化"是基于虚构字段的假仪表**。进度条 `Math.floor((signal.triggerCount / signal.governanceThreshold) * 100)`（`CdssFatigue.tsx:647`）、超阈值判定 `signal.triggerCount >= signal.governanceThreshold`（:650）、文案 `{signal.triggerCount} / {signal.governanceThreshold} 次`（:641）。后端 `RecommendationFatigueSignal` 无 `triggerCount`（实际 `occurrenceCount`，恒为 1）、无 `governanceThreshold`（`RecommendationFatigueSignal.java:17-35`；迁移无列）。运行时 `undefined / undefined = NaN`，进度条与"静音阈值"纯属虚构 UI。

### ⑨ 测试覆盖与有效性（是否 mock 固化假绿）

- **[真·亮点] 后端测试为真实行为断言，非假绿**：
  - `...ServiceTest`：用真实 `validateCards` 逻辑触发 `ENG_REC_005/006`，用 `ArgumentCaptor` 断言落库内容（卡确认位、来源 cardId 关联、信号类型、operatorId=真实用户），`verify(cards,never()).save()` 断言 NO_CARD 不存卡。这些是对真实代码路径的断言。
  - `...RepositoryTest`：真实 H2 + Flyway 跑迁移（`:19-29`），断言主键回填 + 跨租户零泄露（`:80-97`）。
  - `...ControllerSecurityTest`：真实 MockMvc + Spring Security，验证权限矩阵（医师不能触发 :95-100、guest 不能读 :120-125、IT_OPS 能触发但缺租户被拒 :102-110）。
- **[High] 测试盲区：无任何前后端契约对齐测试**。后端字段名 / DTO 结构与前端 `hooks.ts` 类型、`CdssFatigue.tsx` 取值无对照测试，导致本报告所列全部 Critical/High 前端契约缺陷**在"全量 lint/TSC 通过"下完全隐身**（TS 因 `... | string` 宽松联合与可选字段而不报错）。这正是"测试全绿 ≠ 真实"的活样本。
- **[Medium] 疲劳治理无测试**：因后端未实现治理逻辑（仅采集 occurrence_count=1），无抑制 / 阈值 / 静音相关测试，与 `GA-ENG-CDSS-01` 声称"超频疲劳治理"不符。

### ⑩ 前后端契约一致

这是本单元的**重灾区**。逐条（左 = 前端用法，右 = 后端真实）：

| 前端字段 / 用法（file:line） | 后端真实情况 | 后果 |
|---|---|---|
| `card.patientId`（`CdssFatigue.tsx:445`；表格列 :207 经 dataIndex `patientId`；类型 `hooks.ts:870`） | `RecommendationCard` 无该字段（在 trigger 上）；`recommendation_card` 表无 `patient_id` 列 | 患者 ID 详情/列表恒空 |
| `card.encounterId`（:448；`hooks.ts:871`） | 同上，卡无此字段 | 就诊编码恒空 |
| `card.scenarioCode`（:451，且 :108 用作疲劳查询 `fatigueKey`；`hooks.ts:872`） | 卡无此字段（在 trigger 上） | 决策场景恒空；**疲劳 Tab 用 `undefined` 查询 → 取不到信号** |
| `source.title`（:487；`hooks.ts:899`） | 实际 `sourceTitle`（`RecommendationSource.java:25`） | 来源标题恒空 |
| `source.content`（:495；`hooks.ts:900`） | 实际 `summary`（:28） | 证据正文恒空 |
| `source.sourceRef`（:503；`hooks.ts:903`） | 实际 `sourceRefId`（:23） | 出处恒空 |
| `source.authorityScore`（:489；`hooks.ts:902`） | **无此字段** | 恒显伪造"90 分"（Critical，见 ②） |
| `source.evidenceLevel`（:506；`hooks.ts:901`） | **无此字段** | 恒显伪造"Class I"（Critical，见 ②） |
| `signal.triggerCount` / `signal.governanceThreshold`（:641,647,650；`hooks.ts:926-927`） | 实际 `occurrenceCount`（恒 1）；**无 governanceThreshold** | 进度条 NaN，假仪表（High，见 ⑧） |
| `signal.signalType === "MUTE"`（:632；类型 `hooks.ts:863` `MUTE/WARNING/BLOCK`） | 实际 `SHOWN/VIEWED/ACCEPTED/...`（`RecommendationFatigueSignalType.java:10-17`） | 判定恒 false，颜色错误 |
| `diagnoseData.executionId`（:712；`hooks.ts:317`） | `DiagnoseResponse` 实际 `entityId`（`DiagnoseResponse.java:14`） | Trigger ID 恒空 |
| `diagnoseData.inputPayloadSummary`（:718） | 实际 `payloadSummary.digest`，且 recommendation.diagnose 传 `payloadRef=null`（Service:237） | 恒显 "—" |
| `diagnoseData.riskLevel`（:721,723） | `DiagnoseResponse` **无** riskLevel | 恒兜底 `LOW`（Critical，见 ②） |
| `diagnoseData.explanationSnapshot`（:737） | **无**此字段 | 恒显"暂无决策解释快照" |
| `diagnoseData.statusHistory[].{status,changedAt,changedBy,summary}`（:751-765） | 实际字段名 `stateHistory[].{toStatus,occurredAt,actor,reason}`（`DiagnoseResponse.java:19,27-35`） | `statusHistory` 整个 `undefined`，`?.map` 短路，**审计 Timeline 永远空白** |
| `RecommendationCardStatus`（`hooks.ts:852`，仅 4 态） | 后端 8 态（缺 VIEWED/DEFERRED/DISMISSED/SUPPRESSED） | 这些状态在表格走裸枚举名兜底（`CdssFatigue.tsx:238-245`） |
| `RecommendationFeedbackResponse.status`（`hooks.ts:948`） | 后端字段名 `cardStatus`（`RecommendationFeedbackResponse.java:9`） | 前端未用返回值，影响小 |

**结论**：前端展示的"提醒卡主数据 Facts"、"循证证据"、"疲劳治理仪表"、"可信诊断审计"四大区块，绝大多数字段在运行时为 `undefined` 或硬编码兜底。`DiagnoseResponse` 前端类型整体是为"规则引擎"写的（含 `ruleId`），被 CdssFatigue 直接复用且未适配，是断裂根因之一。

---

## 三、七角色评估

| 角色 | 关注点 | 评估 |
|---|---|---|
| **临床医生（0 培训用 CDSS，采纳 / 不采纳两主按钮）** | 能否零培训看懂并安全采纳 | **不及格**。采纳/不采纳两主按钮交互齐全（`CdssFatigue.tsx:537,555`），文案守住"采纳≠下医嘱"（:172,539）；**但**患者 ID / 就诊 / 决策场景全空，"证据级别 Class I / 权威度 90 分"是伪造，诊断面板风险恒显 LOW、审计历史空白——医师据此判断循证强度与风险，会被系统性误导。零培训医生无从识破，**临床安全不可接受**。 |
| **信息科 / 运维** | 可观测、可排障 | 部分。后端 trace_id / 审计 / metrics 齐全可排障；但前端"诊断追溯"面板对外失效（字段全错），运维若依赖它定位问题会被空数据误导。 |
| **医务 / 质控** | 采纳率、疲劳治理、留痕 | **不及格**。采纳率无埋点（⑧）；疲劳治理后端未实现、前端是假仪表；失败留痕（后端）合格。 |
| **安全 / 合规** | 多租户、权限、AI 标识 | **良好**（后端）。隔离严密、权限矩阵清晰、AI 标识落库、医师署名取真实用户。 |
| **架构师** | 契约、分层、可维护 | **不及格**。前后端契约系统性断裂，TS 宽松类型掩盖错误；`DiagnoseResponse` 跨引擎复用未适配。后端分层与隔离设计优秀。 |
| **测试 / QA** | 覆盖有效性 | 后端**良好**（真实断言）；整体**有缺口**：无契约对齐测试，致前端缺陷在全绿下隐身。 |
| **产品 / 交付** | done 是否名副其实 | `GA-ENG-API-07`（后端）名副其实；`GA-ENG-CDSS-01`（前端）**名不副实**，4.26 日志所述"打通通道 / 治理可视化 / 可信审计 Drawer"与实际（空数据 / 假仪表 / 假审计）不符。 |

---

## 四、Findings 汇总表

| ID | 维度 | 严重度 | 摘要 | 位置（file:line） |
|---|---|---|---|---|
| C-01 | ②⑩ | **Critical** | 来源"权威度评分"硬编码 90、"证据级别"硬编码 Class I 当真实循证强度展示给医师；后端无此字段 | `CdssFatigue.tsx:489,506`；`hooks.ts:901-902`；`RecommendationSource.java:17-34` |
| C-02 | ②④⑩ | **Critical** | 诊断面板风险定级恒兜底 LOW（后端 DiagnoseResponse 无 riskLevel），可致 CRITICAL/HIGH 卡被降级误导 | `CdssFatigue.tsx:721,723`；`DiagnoseResponse.java:13-25` |
| C-03 | ④⑧⑩ | **Critical** | "可信诊断审计" Drawer 字段名全错（executionId/explanationSnapshot/statusHistory 等），数据全空/全兜底，却挂"100% 透明非假 MOCK"文案 → 假审计闭环 | `CdssFatigue.tsx:704,712,718,737,751-765`；`DiagnoseResponse.java:13-58` |
| H-01 | ①⑩ | High | interruptLevel 前端取值 NONE/SOFT/HARD 与后端 SILENT/INFO/WEAK_/STRONG_ 完全不符，拦截级别全错展示 | `CdssFatigue.tsx:223,459`；`hooks.ts:859`；`RecommendationInterruptLevel.java:10-13` |
| H-02 | ⑧⑩ | High | 疲劳治理进度条/阈值用虚构字段 triggerCount/governanceThreshold（后端 occurrenceCount 恒 1、无阈值）→ NaN 假仪表 | `CdssFatigue.tsx:641,647,650`；`hooks.ts:926-927`；`RecommendationFatigueSignal.java:17-35` |
| H-03 | ⑩ | High | card.patientId/encounterId/scenarioCode 前端当卡字段展示，后端卡无此列（在 trigger 上）；致主数据空 + 疲劳查询用 undefined | `CdssFatigue.tsx:108,445,448,451`；`hooks.ts:870-872`；`RecommendationCard.java:20-44` |
| H-04 | ⑩ | High | source.title/content/sourceRef 字段名错（应 sourceTitle/summary/sourceRefId），证据正文/标题/出处恒空 | `CdssFatigue.tsx:487,495,503`；`hooks.ts:899-903`；`RecommendationSource.java:23-28` |
| H-05 | ⑧ | High | 采纳/不采纳率等业务埋点缺失，feedback 路径无任何 metrics | `RecommendationEngineService.java:168-193` |
| H-06 | ⑨ | High | 无前后端契约对齐测试，致 C-01..C-03/H-01..H-04 在全绿下隐身 | 整单元（缺失） |
| M-01 | ①⑩ | Medium | triggerStatus 前端 SUCCESS/FAILED 与后端 RECEIVED/EVALUATED/NO_CARD/FAILED 不符 | `hooks.ts:861`；`RecommendationTriggerStatus.java:7-10` |
| M-02 | ① | Medium | RecommendationCardStatus 前端仅 4 态，缺 VIEWED/DEFERRED/DISMISSED/SUPPRESSED，状态走裸枚举名 | `hooks.ts:852`；`CdssFatigue.tsx:238-245` |
| M-03 | ⑩ | Medium | 成功文案"已生成临床决策证据"略夸大（实仅登记反馈事实，未生成独立证据制品） | `CdssFatigue.tsx:172` |
| M-04 | ⑦ | Medium | feedback catch 把所有错误归因"卡片可能已过期或已处于终止态"，掩盖真实失败原因 | `CdssFatigue.tsx:185` |
| M-05 | ⑨ | Medium | 疲劳治理无实现亦无测试，与 GA-ENG-CDSS-01"超频疲劳治理"声称不符 | `RecommendationEngineService.java:275-285` |
| L-01 | ⑥ | Low | hooks 残留后端无对应的虚构扩展字段（severity/recommendations/evidenceSummary/changeSummary），类型噪音 | `hooks.ts:879-891` |
| L-02 | ⑩ | Low | RecommendationFeedbackResponse 前端字段名 status vs 后端 cardStatus（未使用，影响小） | `hooks.ts:948`；`RecommendationFeedbackResponse.java:9` |

合计：**Critical 3 · High 6 · Medium 5 · Low 2**。

---

## 五、改造建议（每条 C/H：问题 / 位置 / 影响 / 改动 / 工作量 / 验证）

> 总原则：后端 + 迁移 + 后端测试**保留**（已真实达标）；改造集中在前端契约对齐 + 后端补埋点 + 补契约测试。**严禁**为迁就前端而在后端"造字段"（如硬编码 authorityScore）——若产品确需循证强度/治理阈值，应作为新需求在后端真实建模。

### C-01 来源伪造"权威度 / 证据级别"
- 问题：前端硬编码 90 分 / Class I 当真实循证强度展示。
- 位置：`CdssFatigue.tsx:489,506`；`hooks.ts:901-902`。
- 影响：医疗安全（误导采纳决策），命中判伪铁律。
- 改动：删除 `authorityScore`/`evidenceLevel` 字段与兜底渲染；改为展示后端真实存在的 `sourceType` / `sourceVersion` / `citationLocator` / `sourceHash`（可追溯三要素）。若产品确需证据分级，另立后端需求建模 `evidence_level` 列后再展示。
- 工作量：3h（前端 2h + 产品确认 1h）。
- 验证：详情来源卡不再出现"90 分 / Class I"；展示 citationLocator/sourceHash；新增契约测试断言渲染字段全部来自后端 DTO 真实键。

### C-02 诊断面板风险恒兜底 LOW
- 问题：`DiagnoseResponse` 无 riskLevel，前端恒显 LOW。
- 位置：`CdssFatigue.tsx:721,723`。
- 影响：医疗安全（风险降级误导）。
- 改动：诊断面板的风险定级改从该卡详情（`detailData.card.riskLevel`，后端真有，且支持 CRITICAL）取，或在 diagnose 聚合时由后端把卡风险纳入 `relatedEntities`/扩展返回；删除 `|| "LOW"` 兜底，缺失时显式显示"未知/—"。
- 工作量：3h。
- 验证：CRITICAL 卡诊断面板显示 CRITICAL（红）；无数据时不伪造 LOW。

### C-03 诊断审计 Drawer 全空假闭环
- 问题：前端 `DiagnoseResponse` 类型为规则引擎所写，字段名（executionId/explanationSnapshot/statusHistory）与后端（entityId/无解释字段/stateHistory）全错，Drawer 全空却称"100% 透明非假 MOCK"。
- 位置：`CdssFatigue.tsx:704,712,718,737,751-765`；`hooks.ts:316-330`；`DiagnoseResponse.java`。
- 影响：审计证据链假闭环 + 虚假宣称。
- 改动：(a) 为 recommendation 单独定义对齐后端的 `DiagnoseResponse` 类型（`entityId/currentStatus/stateHistory[fromStatus,toStatus,reason,actor,occurredAt]/payloadSummary{digest}/links`）；(b) Timeline 改读 `stateHistory` 与正确子字段；(c) 删除/改写"暂无解释快照"等不存在维度，改展示真实存在的状态流转 + traceId + payloadSummary.digest；(d) 修正/删除"100% 非假 MOCK"绝对化文案。
- 工作量：5h。
- 验证：触发后 diagnose Drawer 显示真实状态流转（fromStatus→toStatus、actor、时间）；executionId 显示真实 triggerId；契约测试断言字段映射。

### H-01 interruptLevel 取值不符
- 问题：前端 NONE/SOFT/HARD vs 后端 SILENT/INFO/WEAK_INTERRUPTIVE/STRONG_INTERRUPTIVE。
- 位置：`CdssFatigue.tsx:223,459`；`hooks.ts:859`。
- 影响：拦截级别全错展示（功能 + 临床误导）。
- 改动：前端类型与上色映射改为后端四值，区分强/弱打断/静默。
- 工作量：1.5h。
- 验证：四种 interruptLevel 各自正确上色与文案。

### H-02 疲劳治理假仪表
- 问题：进度条用虚构 triggerCount/governanceThreshold → NaN。
- 位置：`CdssFatigue.tsx:641,647,650`；`hooks.ts:926-927`。
- 影响：可观测性造假（假治理仪表）。
- 改动：诚实化——后端当前仅 `occurrenceCount`（恒 1）且无阈值，前端应改为展示真实信号类型分布 / occurrenceCount，移除"静音阈值进度条"，或明确标注"疲劳治理引擎未启用（GA-ENG-CDSS-01 待实现）"。真正的阈值/抑制须等后端实现后再可视化。
- 工作量：3h（前端 2h + 文案 1h）。
- 验证：Fatigue Tab 无 NaN；不出现未实现的"阈值/静音"伪进度。

### H-03 card 主数据字段越位
- 问题：patientId/encounterId/scenarioCode 在卡上不存在（在 trigger）。
- 位置：`CdssFatigue.tsx:108,445,448,451`；`hooks.ts:870-872`。
- 影响：主数据空 + 疲劳查询用 undefined。
- 改动：两条路线择一——(a) 后端在 `cardDetail`/`listCards` 返回里补 JOIN trigger 的 patientId/encounterId/scenarioCode（推荐，列表已 JOIN，扩展投影即可）；(b) 前端改从 trigger/diagnose 取。疲劳查询 `fatigueKey` 改用 `card.fatigueKey`（后端真有）。
- 工作量：4h（后端投影 2.5h + 前端 1.5h）。
- 验证：详情显示真实患者/就诊/场景；疲劳查询用真实 fatigueKey 命中信号。

### H-04 source 字段名错
- 问题：title/content/sourceRef vs sourceTitle/summary/sourceRefId。
- 位置：`CdssFatigue.tsx:487,495,503`；`hooks.ts:899-903`。
- 影响：证据标题/正文/出处恒空。
- 改动：前端类型与渲染改用后端真实键。
- 工作量：1.5h。
- 验证：来源卡显示真实 sourceTitle/summary/sourceRefId。

### H-05 采纳率埋点缺失
- 问题：feedback 路径无 metrics。
- 位置：`RecommendationEngineService.java:168-193`。
- 影响：医务质控无法观测采纳/不采纳率。
- 改动：在 feedback() 按 feedbackType 增计数指标（如 `medkernel_cdss_feedback_total{type=ACCEPT|REJECT|...}`），参照 `BusinessMetrics` 既有模式。
- 工作量：2.5h。
- 验证：单测 verify 计数；指标端点可见分类计数。

### H-06 缺契约对齐测试
- 问题：无前后端契约对照，缺陷在全绿下隐身。
- 位置：整单元缺失。
- 影响：测试有效性根本性缺口。
- 改动：新增契约测试——可用后端 `@WebMvcTest` 快照真实 JSON key 集合，与前端类型/页面取值字段做断言对照（或引入 OpenAPI 生成前端类型，根治字段漂移）。
- 工作量：6h。
- 验证：故意改一处后端字段名应使契约测试红。

---

## 六、总评

### done 是否名副其实？
- **`GA-ENG-API-07`（后端推荐/CDSS API）：名副其实**。后端代码、五方言迁移、后端测试三层真实达标——医疗安全校验真实强制、多租户隔离严密、不写医嘱、成功+失败均留痕、metrics 真埋点、测试为真实行为断言。这是高质量的后端单元。
- **`GA-ENG-CDSS-01`（推荐引擎前端控制台 + "规则/路径/知识综合、解释追溯、疲劳治理"）：名不副实**。前端 `CdssFatigue.tsx` + `hooks.ts` 契约系统性断裂，向医师展示伪造证据级别/权威度（C-01）、伪造风险定级（C-02）、空壳假审计追溯（C-03）、假疲劳仪表（H-02）、空主数据（H-03/H-04）。4.26 完成日志所述"打通通道、治理可视化、可信审计 Drawer、通过全量 lint/TSC"与运行实际严重不符（TSC 通过恰因 TS 宽松类型掩盖了字段漂移）。此外 backlog 标题承诺的"规则/路径/知识综合"推理在本单元后端**本就未实现**（首版仅受控接收上游候选卡），治理引擎亦未实现。

### 可否真实验收？
- 后端：**可验收**。
- 前端 + 端到端：**不可验收**。在 0 培训临床医生场景下，UI 会稳定展示伪造的循证强度、错误的风险定级与空白审计，存在直接临床误导，触及医疗安全红线。

### 阻塞项（必须先解决方可整单元交付）
1. C-01 / C-02：杜绝前端伪造循证证据与风险定级（医疗安全红线）。
2. C-03：修复或诚实化"可信诊断审计"面板，并删除"100% 非假 MOCK"绝对化宣称。
3. H-03 / H-04：恢复患者/就诊/场景/来源等主数据真实展示。
4. H-06：补前后端契约对齐测试，防回归。

### 建议回退的 backlog
- **`GA-ENG-CDSS-01` 应从 done 回退为 in-progress / reopen**：其交付主体（前端控制台 + 解释追溯 + 疲劳治理）经实测为假数据展示 + 假审计闭环 + 未实现治理；完成日志与实际不符。回退后须按本报告 C-01..C-03、H-01..H-06 整改并补契约测试。
- **`GA-ENG-API-07` 维持 done**（后端达标），但建议附注：前端契约消费方（CdssFatigue）尚未与本 API 真实对齐，待 CDSS-01 整改闭环。
- `GA-ENG-API-13`：不在本单元代码取证范围内（本单元仅复用其 PageResponse 工具），本次不就其单独结论；如需对 V19 大列表/导出做真实性判定，应单列单元审计。

---

*取证完毕。本报告所有结论均来自上述 file:line 真实代码，未引用任何既有审计文档。*
