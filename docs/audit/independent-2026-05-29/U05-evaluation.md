# U05 评估质控引擎 · 独立逐行真实性审计

- 审计日期：2026-05-29
- 审计员：资深医疗系统审计专家（架构 + 临床双视角）
- 审计方式：从零、独立、逐行；不读取/不信任既有 `docs/audit/` 报告
- backlog 关联：GA-ENG-EVAL-01（评估质控引擎）、GA-ENG-API-08（评估质控 API）；旁及 GA-SVC-QUALITY-01（质控驾驶舱）

## 0. 单元概览

### 范围与证据来源
- 后端服务：`medkernel-backend/src/main/java/com/medkernel/engine/evaluation/`（39 个文件全读，核心 `EvaluationEngineService.java` 903 行、`EvaluationEngineController.java` 229 行）
- DSL 求值器（计算核心依赖）：`medkernel-backend/src/main/java/com/medkernel/engine/rule/RuleDslEvaluator.java`
- 迁移：`src/main/resources/db/migration/{postgres,oracle,dm,kingbase,h2}/V14__evaluation_quality_api.sql`
- 测试：`EvaluationEngineServiceTest` / `EvaluationEngineIntegrationTest` / `EvaluationEngineControllerSecurityTest` / `EvaluationRepositoryTest`
- 前端：`frontend/src/pages/quality/{QcEvalSets,QcEvalResults,QcAlerts,QcDashboard}.tsx` + `frontend/src/shared/api/hooks.ts`

### 结论速览
评估质控的**受控事实闭环**（指标版本状态机、运行记录、问题分级派单、整改提交、复核关闭/退回/豁免、幂等重放、P0 不可豁免、五方言迁移、分权）实现得**比较扎实且真实**——这是本单元的骨架，质量高于平均。

但 backlog 声称的两个"核心卖点"经不起验证：
1. **GA-ENG-EVAL-01 的"自动病例命中 + 指标计算"严重残缺**：单病例只产出 0/100 二值分，**没有任何分子/分母聚合、达标率计算**；DSL 求值异常被静默吞没导致医疗漏报；且该路径**完全没有真实测试覆盖**（唯一调用处把求值器 mock 成写死返回值）。
2. **院级质控驾驶舱（院长第一界面）整页伪造**：`QcDashboard.tsx` 全部指标/科室/下钻病例写死，扫描按钮用 `Math.random()` 伪造结果；`QcEvalResults.tsx` 顶部达标率 KPI 写死注释"Mock 以体现高设计感"。这直接违反宪法 §16 与体验规范"驾驶舱指标必须能下钻到行动对象""院长 10 秒看懂今天是否安全"。

---

## 1. 业务正确性（指标真算？）

### 1.1 受控 `run` 路径：真实 ✅
`run()`（service:451-516）对每条结果强制绑定当前租户 `ACTIVE` 指标（456-461），写运行 + 结果 + 问题 + 任务，计数真实回传。`EvaluationRepositoryTest` 与 `EvaluationEngineIntegrationTest` 用真 H2 + Flyway 验证持久化，非桩。此路径不造假。

### 1.2 DSL 求值器：真实确定性引擎 ✅
`RuleDslEvaluator`（rule/RuleDslEvaluator.java:42-214）是真正的条件树解释器：`all`/`any`/`leaf` 递归 + `exists/equals/not_equals/contains/gt/gte/lt/lte/in/not_in` 十算子，数值用 `BigDecimal.compareTo`（200-214），路径用 `a.b.c` 分段下钻（132-141）。**不是写死、不是随机**。`evaluateSnapshot` 的"分母→排除→分子"三步确实调用它求值（service:314/332/347）。这是判"真"的关键。

### 1.3 【Critical / 弱算法致误判】单病例只产 0/100 二值，无分子/分母/达标率聚合
指标实体定义了 `denominatorDefinition` / `numeratorDefinition`（语义是"分母人群 / 分子达标人群"，迁移注释 postgres V14:213/261 明示"达标率""完成率"），但 `evaluateSnapshot` 对**单个**病例求值后（service:361-406）：
- 命中分子或被排除 → `score = 100`、`PASS`（362-370）
- 否则 → `score = 0`、生成缺陷（371-406）

```
// service.java:362,367,372
score = BigDecimal.valueOf(100);   // 排除
score = BigDecimal.valueOf(100);   // 达标
score = BigDecimal.valueOf(0);     // 不达标
```
**全系统没有任何"分子数/分母数=达标率"的聚合计算**（`grep 达标率/complianceRate/passRate` 在整个 evaluation 包返回空）。"静脉血栓预防完成率 >= 95%"这类指标的本质是群体比率，而代码只会给单病例打 0 或 100 分，**指标语义被偷换成"单病例是否达标"**。院长/质控办需要的"全院达标率 92.8%"在后端根本算不出来——前端只能写死（见 §10）。这是 GA-ENG-EVAL-01 名实不符的核心。

### 1.4 【High / 弱算法致误判】严重度判级靠 `scoringDefinition` 字符串 `contains`
不达标时严重度由对自由文本 `scoringDefinition` 做 `contains("P0")/contains("CRITICAL")/contains("极危")` 决定（service:377-388）：

```
// service.java:376-388
String scoreDef = indicator.scoringDefinition() == null ? "" : indicator.scoringDefinition();
if (scoreDef.contains("P0") || scoreDef.contains("CRITICAL") || scoreDef.contains("极危")) {
    severity = QualityFindingSeverity.P0; ...
} else if (scoreDef.contains("P2") || scoreDef.contains("中危")) { ... }
... else { level = NON_COMPLIANT; }  // 兜底默认 P1
```
后果：
- `scoringDefinition` 可空（迁移 V14:14 `NULL`，前端 line 473 占位"默认扣 100 分，不达标生成 P1 缺陷"），**空则一律判 P1**。一个真正的 P0 安全红线指标若没在自由文本里写"P0"字样，会被降级为 P1，**P0 强制不可豁免的保护因此失效**。
- 子串匹配脆弱：文本含"非P0情形"会误判 P0；"达标率 >= 95%"不含任何关键字 → 全部缺陷无脑 P1。
- 医疗风险分级不应依赖运营人员在描述里碰巧写对关键字。应由结构化字段承载严重度。

### 1.5 【Medium】排除即判达标，证据摘要写死
被排除病例 `score=100 / PASS / hitFlag=true`（service:361-365），证据摘要是写死中文串"病例已入组，但已由排除条件自动排除，审计判定达标"。把"排除在外"等同"达标"在统计学上错误（应是"不计入分母"而非"达标"），会污染达标率口径。三处 evidenceSummary 均为常量串（365/370/390），非由实际命中事实生成。

### 1.6 【Medium】`evaluateSnapshot` 运行类型语义错误
自动扫描生成的运行被标为 `UPSTREAM_RESULT`（service:429）。该枚举语义是"上游系统送入的结果"（迁移注释 V14:226），而此处是**系统自身扫描**生成，应是独立类型（如系统自动）。导致运行来源统计失真，无法区分"系统自动扫描"与"第三方回传"。

---

## 2. 医疗安全合规（评级证据以可重算事实为准？）

### 2.1 【Critical】扫描 DSL 异常静默吞没 → 医疗漏报
`evaluateSnapshot` 的资源解析与三处 DSL 求值全部 `try { ... } catch (Exception e) { /* 注释 */ }` 静默吞错：

```
// service.java:278-280  资源解析
} catch (Exception e) {
    // 忽略异常行解析失败以确保流程高可用
}
// service.java:316-318  分母
} catch (Exception e) {
    // 解析或执行失败视为未入组
}
// service.java:334-336  排除   349-351  分子
} catch (Exception e) {
    // 默认不排除 / 解析失败视为未达标
}
```
医疗后果（双向漏报，均危险）：
- 分母 DSL 配错 → 异常 → `inDenominator=false` → 该病例**静默跳过**（service:320），本应被质控的病例不被质控，**漏报缺陷**，且无任何告警/日志/失败计数。
- 分子 DSL 配错 → 异常 → `hitNumerator=false` → 病例被判**不达标并自动派整改单**（service:371-406），把"系统配置错误"误报成"临床缺陷"，给科室派假整改任务。
- 资源 JSON 损坏 → 该类临床资源**整类丢失**（如所有 medications 丢失），分子分母在缺数据下求值，结论不可信却照常落库。

这违反"评级证据以可重算事实为准"与降级显式化原则：异常必须显式化为运行 `FAILED`（表里有 `status=FAILED` 和 `error_code` 列，迁移 V14:55-66，**但代码从不写入**），或单指标记为"无法评估"，绝不能静默当作"未命中"产出确定性结论。

### 2.2 P0 不可普通豁免：后端真门禁 ✅
`reviewRectification`（service:648-651）：`decision==WAIVED && severity==P0` → 抛 `ENG_EVAL_007`。被 `EvaluationEngineServiceTest.p0FindingCannotBeWaivedByOrdinaryReview`（test:238-249）覆盖。前端 `QcAlerts.tsx:137-155` 另有 `Modal.error` 拦截不发请求，是 UX 层双保险。**此红线真实有效**（但注意 §1.4 削弱了"哪些算 P0"的判定可靠性）。

### 2.3 P0/P1 强制责任科室 + 期限 + 证据 + 生成整改任务：真实 ✅
`validateResult`（service:759-762）对 `isHighRisk`（P0/P1）缺责任科室或期限抛 `ENG_EVAL_006`；`shouldAssign`（771-774）高风险必派单，`run` 据此建 `RectificationTask`（500-507）。`EvaluationEngineServiceTest`（test:165-179）验证 P0 缺信息被拒。**符合 backlog 承诺**。注意：evidenceSummary 在 `QualityFindingRequest` 为 `@NotBlank`（QualityFindingRequest:16），证据强制存在。

---

## 3. 多租户隔离

- 全部仓库查询带 `tenantId()`（service:148/295/457/...），`tenantId()` 缺租户抛 `tenantMissing`（887-893）。
- `EvaluationRepositoryTest.repositoryQueriesDoNotLeakAcrossTenants`（test:88-97）验证跨租户查不到。
- 控制器类级 `@DataScope(requireTenant = true)`（controller:31）兜底。
- 幂等键查询同样按租户 + 操作类型 + key（service:814-816）。
- 评价：**隔离真实，无明显跨租户缺口**。未发现 Critical/High。

---

## 4. 审计证据链（运行/整改/复核/豁免留痕）

### 4.1 留痕真实 ✅
每个写动作都 `auditPublisher.publish(...)` + `transitions.record(...)`：创建指标（129/127）、提交/发布/激活（177/196/221）、run（512-513）、整改提交（602-606）、复核（680-685）。复核记录**追加式**写 `rectification_review`（660-662），不覆写历史，迁移注释 V14:277 一致。

### 4.2 【Medium】幂等键写入不在唯一约束失败时优雅降级
`saveIdempotencyKey`（service:825-836）直接 `save`，DB 有唯一键 `uk_eval_idempotency_operation_key`（迁移 V14:197）。`findIdempotencyReplay`（805-823）先查后写，**查与写之间无锁/无 catch**：两个相同 key 的并发请求会同时查不到 → 同时执行业务 → 第二个 `save` 抛 `DataIntegrityViolationException`（非 `ENG_EVAL_008`），返回 500 而非幂等重放。竞态窗口虽小，但整改/复核是关键审计动作，应捕获唯一键冲突转为重放或明确冲突码。属降级体验缺陷，非假闭环。

### 4.3 【Medium】运行失败状态从不落库
表设计了 `RECEIVED/RECORDED/FAILED` 三态（迁移 V14:66）与 `error_code` 列，但代码只写 `RECORDED`（service:473）。配合 §2.1，所有失败被吞，**"失败审计"事实上丢失**——无法事后追溯哪些扫描真正失败。

---

## 5. 五方言一致性

- 五份 V14 迁移均存在（postgres/oracle/dm/kingbase/h2）。
- postgres 版逐行审：表结构、CHECK 约束（subject/status/severity/decision/level）、唯一键、索引、中文 COMMENT 完整。
- 复核意见列名用 `review_comment` 显式规避 Oracle 保留字 `comment`（V14:168/283 注释），实体 `@Column("review_comment")`（RectificationReview:22）对齐——**这点处理专业**。
- 抽查 oracle 版（见下）以确认非 postgres 方言无降级或缺约束。

> 注：本次对 postgres 全量逐行 + oracle 抽查；dm/kingbase/h2 未逐行比对，建议后续补五方言 schema diff 测试（当前测试仅跑 h2）。

---

## 6. 代码净化

- 后端 evaluation 包：无 `Math.random`、无 `TODO/FIXME`、无 `FORCE_*` 钩子、无写死 switch 充结论。代码整洁度高。
- DTO/枚举/实体职责清晰，注释规范（中文 Javadoc）。
- 唯一净化问题在前端（见 §10）：四个前端文件均 `/* eslint-disable medkernel/no-page-mock */` 顶置（QcEvalResults:1、QcAlerts:1、QcEvalSets:1）或函数包装绕过（QcDashboard:5 注释"规避 no-page-mock"），**主动关闭了 mock 门禁**。

---

## 7. 错误处理与降级（扫描异常是否显式化）

- **核心缺陷**见 §2.1（Critical）：扫描异常静默化，是本维度最严重问题。
- 错误码体系真实：`ENG_EVAL_001~008` 全部在 `ErrorCode.java:82-89` 定义，HTTP 状态合理（400/404/409）。
- `digestValues` 对 `NoSuchAlgorithmException` 抛 `IllegalStateException`（service:848-850）——合理，SHA-256 缺失属环境致命错。
- `evaluateSnapshot` 入参校验、快照不存在、无 ACTIVE 指标、无入组都有明确错误码（service:232/243/297/423）——这部分显式化是好的，**唯独 DSL 求值层吞错**。

---

## 8. 可观测性（达标率/整改率埋点）

### 8.1 【High】零业务指标埋点
`grep Metrics/Counter/Gauge/@Observed/meterRegistry` 在 evaluation 包**全空**。无达标率、无整改率、无 P0 闭环时长、无扫描成功/失败计数、无逾期任务量任何埋点。

- 体验规范 line 36/62 要求驾驶舱展示"风险、价值、进度"，宪法 §16 要求"指标能下钻"。没有后端埋点/聚合，驾驶舱只能造假（这正是 §10 QcDashboard 写死的根因）。
- 业务场景 S11（SPEC:589）流程末端是"趋势分析"——当前完全缺失。
- traceId 贯穿（service:899-902，每实体存 trace_id），这点可观测性是有的；但**业务度量维度为零**。

---

## 9. 测试覆盖与有效性

### 9.1 真实有效的测试 ✅
- `EvaluationEngineControllerSecurityTest`（test 全文）：5 角色（QA_MANAGER/MEDICAL_AFFAIRS/IT_OPS/DEPT_HEAD/DOCTOR）× 端点矩阵，验证 403 与租户兜底（ENG-BASE-001）。**分权测试扎实**，是亮点。
- `EvaluationRepositoryTest`：真 H2 + Flyway 持久化与跨租户隔离。
- `EvaluationEngineIntegrationTest.persistsIdempotentIndicatorRunRectificationAndReviewWorkflow`：真库跑通"指标→激活→run→整改→复核→关闭"+ 幂等重放 + 同键异文拒绝（ENG_EVAL_008）。覆盖受控闭环，**有效**。

### 9.2 【Critical / 关键测试缺失 + 误导命名】"指标计算"核心路径无真实测试
唯一测 `evaluateSnapshot` 的用例 `EvaluationEngineServiceTest.evaluateSnapshotCalculatesMetricsAndCreatesDefectFindings`（test:342-390）：
- 方法名含 "CalculatesMetrics"，但 `ruleEvaluator` 是 **mock**，`when(ruleEvaluator.evaluate(any(),any())).thenReturn(写死命中序列)`（test:373-376）——**DSL 求值逻辑完全没被执行**。
- 指标的分子分母定义写成空数组 `"{\"all\":[]}"`（test:363），不代表任何真实临床规则。
- 因此该测试只验证了 service 的编排（调了 save），**没有验证任何"计算"**。命名是误导。

更严重：`EvaluationEngineIntegrationTest` 把 `RuleDslEvaluator` 和 `ObjectMapper json` 都 `@MockBean`（test:59-60）。`evaluateSnapshot` 内部 `json.createObjectNode()` 在 mock 下返回 null，**该路径在集成测试里根本无法运行**——所以集成测试只覆盖了手工 `run`，**自动扫描端到端真实路径（资源组装 + 真实 DSL 求值 + 落库）从未被任何测试覆盖**。GA-ENG-EVAL-01 的核心声称无测试背书。

### 9.3 缺失的关键用例
- 分母/分子 DSL 真实求值（用真 `RuleDslEvaluator` + 真临床 JSON）。
- DSL 解析异常时的行为（当前静默吞，无测试，正好掩盖了 §2.1 缺陷）。
- 达标率聚合（功能本身缺失）。
- 幂等并发竞态（§4.2）。
- `scoringDefinition` 空/含误导子串的严重度判级（§1.4）。

---

## 10. 前后端契约一致 + 前端真实性

### 10.1 【Critical】`QcDashboard.tsx` 院级质控驾驶舱整页伪造
对应 GA-SVC-QUALITY-01、宪法"院长第一界面"。全页无任何真实 API：
- `getQcIndicators()` / `getDeptRisks()` / `getDrilldownCases()` 全是写死常量（QcDashboard:6-83），注释直言"规避 no-page-mock：通过函数动态提供初始指标"（line 5）——故意绕过 mock 门禁。
- "物理启动决策引擎重放自检"按钮 = `setTimeout(1500ms)` + `Math.random()` 伪造 AUD 指标回落（line 118-147，**line 137 `Math.random()`**），并弹"病例重放校验完毕"。
- "打包导出质控证据"= `alert("...导出成功！")`（line 367），假证据导出。
- 下钻病例（line 58-83）写死两个患者姓名/病案号/traceId，违反宪法 §16 与体验规范 line 245"驾驶舱指标不能下钻到行动对象"被列为**禁止项**。

院长看到的"综合安全合规达标率""科室风险热力"全是编造，**直接误导医疗安全管理决策**。

### 10.2 【Critical】`QcEvalResults.tsx` 顶部达标率 KPI 写死
表格本体走真 API（`useEvaluationResults`，line 24-34）✅；但院长第一眼的 4 张宏观卡：

```
// QcEvalResults.tsx:133-138
const totalCases = 485;       // 历史累积病例
const enrolledCases = 152;    // 入组病例
const complianceRate = 92.8;  // 指标质量达标率
const activeDefects = 6;      // 严重缺陷
```
注释 line 133 明写"计算宏观 KPI 指标（Mock 以体现高设计感）"。"临床综合质量达标率 92.8%"是写死常量当真展示（line 174-178）。根因正是 §8 后端无达标率聚合。属"前端写死数据当真展示"判伪铁律命中。

### 10.3 【Medium】复核历史前后端字段错配，Timeline 展示失真
后端 `RectificationReview` record 序列化字段为 `comment` / `reviewerId` / `reviewedAt`（RectificationReview:22/24/25）。前端 `hooks.ts:1202/1204` 定义为 `comments`（多 s）/ `reviewedBy`。`QcAlerts.tsx` 渲染复核 Timeline 用 `rev.reviewedBy`（line 651）、`rev.comments`（line 654）→ 永远 `undefined`，"专家 X 复核完毕"显示为"专家 undefined"、复核意见空白。质控办看到的复核历史失真。

### 10.4 【Medium】`QcAlerts.tsx` TraceId 造假 + 假"100% 留痕"标签
"可信审计 Trace" 面板（QcAlerts:604-610）把 TraceId 显示为 `selectedFinding.findingId + "_TRACE"`（用 findingId 拼字符串冒充 trace），右侧挂 `Tag "100% 留痕合规"`（line 610）。真实 traceId 后端有存（trace_id 列），但前端没取，反而用 ID 拼接伪造，标榜"100% 合规"——属"UUID/ID 充哈希 + 假证据展示"。

### 10.5 【Low】`DEMO_SNAPSHOTS` 演示数据混入生产构建
`hooks.ts:1442-1453` 导出写死的演示快照（"患者李建国""患者张淑芳"），作为 `QcEvalSets` 沙箱默认选项（QcEvalSets:34/621）。沙箱仿真本身调真 API（`useEvaluateSnapshot`）✅，但演示数据写在生产 `hooks.ts` 主文件并默认选中 `ctx-vte-demo-2`（line 58），混淆演示与真实。

### 10.6 真实对接的前端部分 ✅
- `QcEvalSets.tsx`：指标 CRUD + 状态流转（提交/发布/激活）+ 沙箱扫描全走真 hooks（line 76-80, 153-171），三大 DSL 表达式真实渲染（line 439-465）。
- `QcAlerts.tsx`：finding 列表、详情、整改提交、复核全走真 hooks（line 31-37, 114, 159）；P0 拦截真实。
- 这两页骨架真实，问题在上述局部造假/契约错配。

---

## 7 角色视角评估

| 角色 | 关注点 | 现状判定 |
|---|---|---|
| **院长** | 10 秒看懂今天是否安全/达标率/趋势 | **不通过**。质控驾驶舱（QcDashboard）整页假数据 + Math.random 假扫描；评估结果页达标率写死 92.8%。看到的安全结论是编造的，违反宪法 §16/体验规范。0 技术名词这点表面满足（页面无 DSL/trace 裸露），但代价是"0 真实数据"。|
| **医务处** | 30 分钟跑剧本：配指标→扫描→看缺陷→派单 | **部分通过**。指标库（QcEvalSets）配置→送审→发布→激活→沙箱扫描链路真实可跑；但"沙箱扫描"依赖被静默吞错的 DSL，配错规则不报错只漏报，30 分钟剧本会"看起来成功实则漏判"。无达标率/趋势看板。|
| **质控办** | 从发现到派单/整改/复核不重复录入 | **部分通过**。后端闭环真实、不重复录入；但复核历史前端字段错配（§10.3）展示失真，TraceId 造假（§10.4）。|
| **科主任/责任科室** | 收到整改任务、提交整改 | **通过**。P0/P1 强制派单、整改提交闭环真实（QcAlerts 整改 Tab）。|
| **质控专家（复核人）** | 复核通过/退回/豁免，P0 不可豁免 | **通过**。复核三决议 + P0 不可豁免后端真门禁，被测试覆盖。|
| **IT 运维** | 失败可观测、可追溯 | **不通过**。扫描失败静默吞、运行 FAILED 状态从不落库（§4.3）、零业务埋点（§8），故障无法定位。|
| **审计/监管** | 留痕、证据可重算 | **部分通过**。状态迁移 + 审计事件 + 追加式复核留痕真实；但"达标"证据不可重算（单病例 0/100 + 写死摘要 + 排除即达标），失败审计丢失。|

---

## Findings 汇总表

| # | 维度 | 严重度 | 摘要 | 证据 |
|---|---|---|---|---|
| F1 | ①业务正确性 | **Critical** | 无分子/分母/达标率聚合，单病例只产 0/100 二值，指标语义被偷换 | `EvaluationEngineService.java:362,367,372`；全包无 complianceRate |
| F2 | ②医疗安全/⑦降级 | **Critical** | 扫描 DSL 异常静默吞 → 漏报缺陷/误派整改，失败不显式化 | `EvaluationEngineService.java:278-280,316-318,334-336,349-351` |
| F3 | ⑨测试有效性 | **Critical** | 自动扫描"指标计算"核心无真实测试：唯一用例 mock 求值器，集成测试 MockBean 掉 evaluator+json | `EvaluationEngineServiceTest.java:373-376`；`EvaluationEngineIntegrationTest.java:59-60` |
| F4 | ⑩前端真实性 | **Critical** | 院级质控驾驶舱整页写死 + Math.random 假扫描 + alert 假导出，违反宪法 §16/驾驶舱下钻红线 | `QcDashboard.tsx:5-83,118-147,367` |
| F5 | ⑩前端真实性 | **Critical** | 评估结果页达标率等 4 KPI 写死，注释"Mock 以体现高设计感"当真展示 | `QcEvalResults.tsx:133-138,174-178` |
| F6 | ①②业务/安全 | **High** | 严重度靠 `scoringDefinition` 字符串 contains 判 P0/P1，空则一律 P1，削弱 P0 保护 | `EvaluationEngineService.java:376-388` |
| F7 | ⑧可观测性 | **High** | 零业务指标埋点（达标率/整改率/失败/逾期/P0 时长全无），趋势分析缺失 | evaluation 包 grep Metrics/Counter 全空 |
| F8 | ④审计证据链 | **Medium** | 幂等写入无唯一键冲突捕获，并发竞态返回 500 而非重放 | `EvaluationEngineService.java:805-836` |
| F9 | ④审计证据链 | **Medium** | 运行 FAILED 状态与 error_code 从不落库，失败审计丢失 | `EvaluationEngineService.java:473` vs 迁移 V14:55-66 |
| F10 | ⑩前后端契约 | **Medium** | 复核字段错配：后端 comment/reviewerId，前端 comments/reviewedBy，Timeline 显示 undefined | `RectificationReview.java:22,24` vs `hooks.ts:1202,1204`、`QcAlerts.tsx:651,654` |
| F11 | ⑩前端真实性 | **Medium** | QcAlerts TraceId 用 findingId 拼接伪造 + 假"100% 留痕合规"标签 | `QcAlerts.tsx:604-610` |
| F12 | ①业务正确性 | **Medium** | 排除病例判为"达标 PASS"污染达标率口径；三处 evidenceSummary 写死常量 | `EvaluationEngineService.java:361-365,365,370,390` |
| F13 | ①业务正确性 | **Medium** | 自动扫描运行误标 UPSTREAM_RESULT（应为系统自动），来源统计失真 | `EvaluationEngineService.java:429` |
| F14 | ⑥代码净化 | **Low** | DEMO_SNAPSHOTS 演示数据写在生产 hooks.ts 并默认选中 | `hooks.ts:1442-1453`、`QcEvalSets.tsx:58` |
| F15 | ⑤五方言 | **Low** | 测试仅跑 h2，无五方言 schema diff 校验（postgres/oracle 人工核对一致） | 三测试 `spring.flyway.locations=.../h2` |

合计：**Critical 5 · High 2 · Medium 6 · Low 2**

---

## 改造建议（每条 C/H）

### F1（Critical）补真正的指标聚合计算
- 新增"指标运行聚合"能力：按指标 + 组织范围 + 时间窗，统计分母人数、分子人数、排除人数，计算 `达标率 = 分子/(分母-排除)`，落 `evaluation_result` 之上的聚合表或视图。单病例评估保留为"明细命中"，但达标率必须群体聚合。
- 区分 `subjectType`：病历级（单病例 0/100）与科室/医生/疾病级（聚合率）走不同计算路径。

### F2（Critical）扫描异常显式化
- 三处 catch 改为：记录失败到运行 `FAILED` + `error_code`，或单指标产出 `resultLevel=ATTENTION/无法评估` 并附错误原因，绝不静默当"未命中"。
- 资源解析失败必须计数并在响应/诊断中暴露"N 条资源解析失败"，不得吞。
- 分母 DSL 解析错误应视为"指标配置错误"上抛或显式标记，与"病例未入组"区分开。

### F3（Critical）补自动扫描真实端到端测试
- 用**真** `RuleDslEvaluator` + 真临床资源 JSON，构造"达标/不达标/被排除/分母不入组/DSL 异常"五类用例，断言 score/level/severity/findingCount。
- 集成测试不要 MockBean `RuleDslEvaluator` 与 `ObjectMapper`；让 `evaluateSnapshot` 真实跑库。
- 把 `evaluateSnapshotCalculatesMetrics...` 改名为名实相符，或补真正的计算断言。

### F4（Critical）质控驾驶舱接真数据
- QcDashboard 删除所有写死函数与 Math.random 扫描；指标卡接 F1 的达标率聚合 API，下钻接 `useEvaluationResults`/`useQualityFindings` 真实病例，导出接真实证据包导出任务。
- 移除 `/* eslint-disable medkernel/no-page-mock */`，让门禁生效。

### F5（Critical）评估结果 KPI 接真聚合
- QcEvalResults 顶部 4 KPI 改为消费后端聚合接口（总数/入组/达标率/缺陷数），删除写死常量与"Mock 以体现高设计感"注释。

### F6（High）严重度结构化
- 指标新增结构化 `defaultSeverity` 字段（P0-P3 枚举），扫描不达标时直接取该字段；废弃对 `scoringDefinition` 自由文本 `contains` 判级。迁移加列 + CHECK 约束。

### F7（High）补业务埋点
- 注入 MeterRegistry，埋点：扫描成功/失败计数、各指标达标率 Gauge、P0/P1 缺陷生成数、整改提交/关闭/逾期 Counter、P0 闭环时长 Timer。为驾驶舱与趋势分析提供真实数据源。

---

## 总评

### done 是否名副其实？
- **GA-ENG-API-08（评估质控 API）**：受控事实闭环、状态机、分权、幂等、P0 门禁、五方言迁移**基本名副其实**，质量较高。扣分项为失败状态不落库（F9）、幂等并发（F8）、契约错配（F10）。可视为 **"接近 done，需修补"**。
- **GA-ENG-EVAL-01（评估质控引擎：病例命中 + 指标计算）**：**名不副实**。核心卖点"指标计算/达标率"根本不存在（F1），扫描异常静默漏报（F2），核心路径零真实测试（F3）。backlog 4.27 描述的"分母/排除/分子三步匹配算法"中，三步匹配（DSL 求值）是真的，但"指标计算"是假的——只会给单病例打 0/100。**应判未完成**。
- **GA-SVC-QUALITY-01（质控驾驶舱）**：院长第一界面整页伪造（F4/F5），**严重不达标**。

### 可否真实验收？
**不可**。阻塞项：
1. 院长无法验收——驾驶舱与达标率全假（F4/F5），违反宪法 §16 与体验规范驾驶舱红线。
2. 临床安全无法验收——扫描静默漏报（F2）、严重度判级脆弱削弱 P0 保护（F6），有医疗误判/漏判风险。
3. 工程无法验收——核心"计算"路径无真实测试（F3），"测试全绿"是假象（绿的是编排桩，不是计算）。

### 建议回退的 backlog
| backlog ID | 当前 | 建议 | 理由 |
|---|---|---|---|
| **GA-ENG-EVAL-01** | done | **回退 in-progress / reopen** | 指标计算/达标率缺失（F1）、扫描漏报（F2）、核心无测试（F3） |
| **GA-SVC-QUALITY-01** | done | **回退 in-progress / reopen** | 质控驾驶舱整页伪造（F4），院长界面不可用 |
| GA-ENG-API-08 | done | **保留 done，挂修补子项** | 骨架真实，F8/F9/F10 作为缺陷修补，不必整体回退 |

> 注：GA-SVC-QUALITY-01 的最终判定建议结合其服务包专项审计单元；本单元从"院长质控驾驶舱"视角已取得整页造假的确凿证据（QcDashboard.tsx），足以支撑回退建议。

### 阻塞项清单（按修复优先级）
1. F2 扫描异常显式化（医疗安全，最高优先）
2. F1 达标率真实聚合（核心功能）
3. F4 + F5 驾驶舱/结果页接真数据（院长验收前提）
4. F3 补自动扫描真实测试（防回归）
5. F6 严重度结构化（P0 保护可靠性）
6. F7 业务埋点（驾驶舱数据源 + 可观测）
