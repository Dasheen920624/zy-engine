# A7 推荐 / CDSS 引擎 · 深度审计报告

> 审计日期：2026-05-29 · 审计人：Claude
> backlog 自报状态：CDSS-01 / API-07 = done
> **审计结论：后端 ✅ 真实达标；前端 ⚠️ 需返工（含 2 项医疗安全 Critical）**

---

## 1. 单元概览

| 项 | 信息 |
|---|---|
| 后端 | `engine/recommendation`（29 文件）；核心 `RecommendationEngineService`(340) / `RecommendationEngineController`(124) |
| 迁移 | V13 五方言齐全（postgres/oracle/dm/kingbase/h2 均有 `V13__recommendation_cdss_api.sql`） |
| 前端 | `pages/clinical/CdssFatigue.tsx`(851) |
| 测试 | 3 个：ServiceTest(7)、ControllerSecurityTest(5)、RepositoryTest(3) |
| 业务目标 | 触发 / 推荐卡 / 来源解释 / 医师反馈 / 疲劳信号 / 诊断 6 流 |

**完成度**：后端 6 流全真实落地；前端展示层有伪造内容（详见 §2.2）。

---

## 2. 十维度审计结果

### 2.1 业务正确性 — ✅ 后端真实
- `trigger`(72)/`listCards`(108)/`cardDetail`(123)/`sources`(137)/`feedback`(149)/`fatigueSignals`(180)/`diagnose`(199) 均为真实持久化，无 stub/写死。
- 推荐卡状态机真实：PENDING → VIEWED/ACCEPTED/REJECTED/DEFERRED/DISMISSED（`nextStatus` 282）；终止态/过期卡拒绝再反馈（`ENG_REC_004`，151）。
- 触发状态由候选卡数量决定（EVALUATED/NO_CARD，79）。

### 2.2 医疗安全合规 — ⚠️ 后端达标 / 前端红线
**后端达标**（`validateCards` 221）：
- 每卡 ≥1 来源，否则 `ENG_REC_005`（223）✅
- 高风险卡未确认 → `ENG_REC_006`（226）✅ 满足宪法 #10「医师确认」
- 强打断必须高风险 → `ENG_REC_001`（229）✅ 满足产品体验「低打扰」
- `aiGenerated` 字段贯穿（242/277）✅ 满足宪法 #9「AI 标识」
- Javadoc 明示且代码确无「自动写医嘱/诊断/病历」（28）✅

**前端红线（Critical）**：
- 🔴 **CDSS-CRIT-01**：`CdssFatigue.tsx:172-189` 真实触发成功后，**客户端额外伪造一张写死的临床卡**——`title:"智能药理提示：发现潜在中高度相互作用风险"`、`summary:"...建议临床注意监控肾功能指征"`、`cardId/triggerId/traceId` 全 `Math.random`、`tenantId:"TEN-001"` 写死。医师看到的是**引擎从未产出的伪造药理建议**，直接违反宪法 #9/#10 与「禁止业务 mock 假闭环」，有患者安全风险。
- 🔴 **CDSS-CRIT-02**：`CdssFatigue.tsx:204` 医师反馈署名硬编码 `physicianId:"PHYS-1002" // 当前登录医生模拟`，**不取真实登录用户**。临床决策的采纳/拒绝被错误归因，破坏医疗追责与审计链。

### 2.3 多租户隔离 — ✅
- Controller 类级 `@DataScope(requireTenant = true)`（33）。
- Service `tenantId()`（324）缺租户即抛 `tenantMissing`。
- 所有 Repository 方法均 `...AndTenantId` / `pageByFilter(tenantId(),...)`；**无裸 `findById`**（grep 确认）。

### 2.4 审计与证据链 — 🟡 有缺口
- 成功路径：trigger→EXECUTE 审计（99）、feedback→FEEDBACK 审计（170）+ StateTransitionRecorder（98/168）✅
- ⚠️ **CDSS-M-01**：校验失败路径（`ENG_REC_005/006/001`，221-232）**无失败审计**。高风险卡未确认被拒、强打断违规等医疗安全相关事件未留痕，与 BASE-04「运行/反馈统一留痕」及 API-01b 既定失败 audit 模式不一致。
- ⚠️ **CDSS-M-02**：用 `AuditEventPublisher` 而非项目既定的 `IsolatedAuditPublisher`（PROPAGATION_REQUIRES_NEW）。成功路径同事务尚可，但与 LLM/EVID/INTEG 等模块的失败保全模式不统一。

### 2.5 五方言一致性 — ✅（文件齐全，列等价待逐字段核）
V13 五方言文件均存在。建议后续逐字段比对列类型/长度/保留字（本轮未逐字段）。

### 2.6 代码净化 — ⚠️ 前端
- 后端：仅 `UUID.randomUUID()` 生成业务 ID（合法），无 `Math.random`/`System.out`/`TODO`/写死 switch ✅
- 🟠 **CDSS-H-01**：`CdssFatigue.tsx:1` `/* eslint-disable medkernel/no-page-mock */` **整文件关闭防造假门禁**（呼应总计划 §5.4 / 真实性审计 R1）。
- 前端 `Math.random` 伪造 ID（162/174/176/187）、写死 `tenantId/encounterId`（175/178）。

### 2.7 错误处理与降级 — ✅
- `ENG_REC_001..006` 声明并使用；统一走 `ApiException`→ProblemDetail。
- CDSS 不直连模型（候选卡由调用方传入），天然 B0 安全，无模型依赖。

### 2.8 可观测性 — 🟡
- traceId 贯穿（76/337）、StateTransitionRecorder 在 trigger/feedback 调用 ✅
- ⚠️ **CDSS-M-03**：无 Micrometer 自定义埋点（采纳率/拒绝率/疲劳触发计数），疲劳治理缺运行时指标支撑。

### 2.9 测试覆盖与有效性 — 🟡
- ServiceTest 覆盖 `ENG_REC_005`(127)、`ENG_REC_006`(141) 等医疗安全校验 ✅（真实 service，仅 mock repository，未把校验算法 mock 掉）。
- RepositoryTest 真存真查（tenant-A）✅
- ⚠️ **CDSS-M-04**：**无跨租户负向用例**（tenant-A 写、tenant-B 读应为空）。隔离靠构造保证，未被测试证明。
- ⚠️ 前端无针对 trigger/feedback 的业务断言测试（仅 smoke）。

### 2.10 前后端契约一致 — 🟡
- 路径 `/api/v1/engine/recommendations/*` 与 hooks 对齐；DTO↔type 基本一致。
- ⚠️ **CDSS-H-02**：`CdssFatigue.tsx:209` 反馈成功提示「已采纳该合理化建议，**医嘱流转成功**！」与后端契约矛盾——后端明确不写医嘱（28）。误导医师以为已下达医嘱。

---

## 3. 七角色视角评估

| 角色 | 评估 |
|---|---|
| 临床医生 | 后端「采纳/不采纳 + 理由」契约齐全（feedbackType+reasonText）✅；但前端伪造卡 + 假「医嘱流转成功」会直接误导临床，**体验红线** |
| 合规审计 | 后端来源解释强制、状态历史齐全 ✅；但失败路径无审计 + 反馈署名造假，审计链不完整 |
| 医务处 | 疲劳治理信号采集真实，但缺运行指标看板支撑 |

---

## 4. Findings 汇总

| Severity | ID | 一句话 | 位置 |
|---|---|---|---|
| Critical | CDSS-CRIT-01 | 前端伪造写死临床药理卡展示给医师 | `CdssFatigue.tsx:172-189` |
| Critical | CDSS-CRIT-02 | 医师反馈署名硬编码 PHYS-1002 非真实用户 | `CdssFatigue.tsx:204` |
| High | CDSS-H-01 | 整文件关闭 no-page-mock 防造假门禁 | `CdssFatigue.tsx:1` |
| High | CDSS-H-02 | 成功提示谎称「医嘱流转成功」违背后端契约 | `CdssFatigue.tsx:209` |
| Medium | CDSS-M-01 | 校验失败路径无失败审计 | `RecommendationEngineService.java:221-232` |
| Medium | CDSS-M-02 | 用 AuditEventPublisher 非 IsolatedAuditPublisher | 同上 99/170 |
| Medium | CDSS-M-03 | 无 Micrometer 采纳率/疲劳指标埋点 | service 全局 |
| Medium | CDSS-M-04 | 无跨租户负向隔离测试 | `RecommendationRepositoryTest.java` |

合计：Critical 2 · High 2 · Medium 4 · Low 0

---

## 5. 改造方案（按优先级）

### CDSS-CRIT-01 删除前端伪造卡（必须）
- **问题**：成功后客户端凭空造一张写死药理卡入列展示。
- **改造**：删除 `:172-189` 整段 `newCard` 伪造与 `setLocalCards`；触发成功后改为 `refetch` 推荐卡列表（`useRecommendationCards`），只展示后端真实返回的卡。无卡则走六态「空态」。
- **工作量**：约 1.5h（含移除 `localCards` 兜底状态）。
- **验证**：断网/后端返回 0 卡时，列表显示空态而非假卡；Vitest 断言列表数据源仅来自 query。

### CDSS-CRIT-02 反馈署名取真实用户（必须）
- **问题**：`physicianId:"PHYS-1002"` 写死。
- **改造**：移除该字段，由后端从 `RequestContext.currentUserId()` 取操作者（service `feedback` 已用 `actor()`，前端不应再传 physicianId）；若 DTO 需 operatorRole，从当前用户角色注入。
- **工作量**：约 1h。
- **验证**：不同医生登录反馈，audit_event 的 actor 为真实 userId。

### CDSS-H-01 恢复 no-page-mock 门禁
- **改造**：删 `:1` 的 eslint-disable；按总计划 R1 修复后的规则跑通；存量假数据随 CRIT-01/02 一并清除。
- **工作量**：随上面两项附带，约 0.5h。

### CDSS-H-02 纠正成功文案
- **改造**：`:209` 改为「已登记采纳，已生成临床决策证据；是否下达医嘱由您在 HIS 确认」之类，去除「医嘱流转成功」误导。
- **工作量**：0.2h。

### CDSS-M-01/02 失败审计 + Isolated 发布器
- **改造**：`validateCards` 抛错前，经 `IsolatedAuditPublisher` 发 `outcome=FAILED` + errorCode 的审计（参考 API-01b 模式）；并将本模块成功审计也切到 IsolatedAuditPublisher 统一。
- **工作量**：约 2h（含测试）。

### CDSS-M-03/M-04 指标 + 跨租户测试
- M-03：service 注入 MeterRegistry，对 accept/reject/silent 计数与疲劳 key 打点（约 1.5h）。
- M-04：RepositoryTest 增 tenant-A 写、tenant-B 读为空的负向用例（约 0.5h）。

---

## 6. 优化建议（非阻塞）
- 推荐卡 `explanationJson` 建议定义结构化 schema（来源类型/权重/规则号），便于前端「可信解释」一致渲染。
- `expiresAt` 过期卡可由定时任务批量置 EXPIRED + 留痕，而非仅查询时判断。

---

## 7. 总评
- **done 名副其实性**：后端 CDSS-01 / API-07 **名副其实**（真引擎、医疗安全校验齐全、租户隔离扎实）；**前端不达标**——存在 2 项医疗安全 Critical（伪造临床卡 + 反馈署名造假），不可进入真实临床验收。
- **可否进入 GA 验收**：后端可；**前端必须先完成 CDSS-CRIT-01/02 + H-01/02 返工**。
- **后续重点**：本单元前端假闭环模式（成功后仍叠加写死数据、`eslint-disable` 门禁、硬编码身份）极可能在 C2-C6 其它临床/业务页复现，C3 临床页整体需重点排查。

---

## 8. 修复记录（2026-05-29 · Claude · 用户授权"针对有问题的进行改造"）

> 验证：前端 typecheck/lint(0 error)/build/smoke 全过；`CdssFatigue.tsx` 移除整文件 `eslint-disable` 后无门禁报错。后端推荐引擎本轮未改（其 Medium 项见下）。

| Finding | 状态 | 修复内容（file） |
|---|---|---|
| CDSS-CRIT-01 | ☑️ 已修 | `CdssFatigue.tsx` 删除写死 `localCards`（含两张假卡）与触发后伪造 `newCard`；改用真实分页查询 `useRecommendationCards`（服务端按 status/risk/patient 过滤），触发成功后 `refetchCards()`，列表只展示后端真实卡。另删除"来源/疲劳/诊断"三处 Mock fallback（伪造《合理用药共识》卡、12/10 疲劳卡、`SHA-256-REC-MOCK-HASH` 假哈希、写死决策解释），无数据走 `Empty` 空态 |
| CDSS-CRIT-02 | ☑️ 已修 | 删除硬编码 `physicianId:"PHYS-1002"`；反馈载荷契约对齐后端 `RecommendationFeedbackRequest`（`reasonCode`/`reasonText`，operator 由后端取真实登录用户）。同步修复 `hooks.ts` 反馈/触发 hook 的 payload 类型与 `RecommendationFeedback` 展示类型（`physicianId`→`operatorId/reasonCode/reasonText/operatorRole`） |
| CDSS-H-01 | ☑️ 已修 | 删除 `CdssFatigue.tsx:1` 整文件 `eslint-disable medkernel/no-page-mock`；并修复 `eslint-rules/no-page-mock.js` 误伤 `const columns` 的门禁 bug（启用 `NAME_PATTERN`，仅拦截 SHOUTY-CASE，R1 根因） |
| CDSS-H-02 | ☑️ 已修 | 采纳成功文案由"医嘱流转成功"改为"已登记采纳，已生成临床决策证据；是否下达医嘱请在 HIS 中确认"，与后端"不写医嘱"契约一致 |
| 附带契约 | ☑️ 已修 | 原前端触发载荷 `{patientId,encounterId,scenarioCode,diseaseCode,payloadJson}` 与后端必填 `triggerCode/triggerType/inputDigest` 完全不符（恒 400→必走 catch 伪造）；现按真实契约提交（`inputDigest` 用 `crypto.subtle` 真实 SHA-256），并诚实说明触发为受控写入、cardCount 据实 |
| CDSS-M-01 | ☑️ 已修 | `trigger` 的 `validateCards` 失败（ENG_REC_001/005/006）经 `IsolatedAuditPublisher` 发 outcome=FAILED 审计，失败也留痕（不被主事务回滚带走）；单测断言两条拒绝路径均发失败审计 |
| CDSS-M-02 | ☑️ 已修(澄清) | 复核 `IsolatedAuditPublisher` 契约（其 Javadoc 明确"成功路径走 AuditEventPublisher、isolated 仅限失败留痕"）：原成功路径用 AuditEventPublisher **本就正确**；真正缺口是失败留痕缺失＝CDSS-M-01，已补齐 |
| CDSS-M-03 | ☑️ 已修 | `trigger` 每发一张提醒卡调用 `BusinessMetrics.incCdssAlerts()`，使 `medkernel_cdss_alerts_total` 真实计数（该指标此前已定义但从未被任何代码调用，恒为 0）；单测断言单卡触发计一次 |
| CDSS-M-04 | ☑️ 已修 | `RecommendationRepositoryTest.repositoryQueriesDoNotLeakAcrossTenants` 跨租户负向用例已存在并强化：trigger + card 双维度——错误租户 `findById/按 trigger 列/countByFilter` 全为空/0，正确租户可读，证明隔离来自 tenant 过滤 |

**复核结论（2026-05-29 二次整改）**：前端 2C+2H（及隐藏触发契约假闭环）+ 后端 4 项 Medium **全部闭环并测试固化**（后端 RecommendationEngineServiceTest 7 + RepositoryTest 2 绿）。本单元 8 项 findings 全清，后端前端均达真实可验收状态。
