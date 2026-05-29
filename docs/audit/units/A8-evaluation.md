# A8 评估质控引擎 · 深度审计报告

> 审计日期：2026-05-29 · 审计人：Claude
> backlog 自报状态：EVAL-01 / API-08 = done
> **审计结论：✅ 基本通过（首个前后端均达标的单元）。真 DSL 病例命中引擎、P0 豁免门禁真实且被测、跨租户测试齐全、前端诚实报错。仅 3 Medium + 1 Low，无 Critical/High。**

---

## 1. 单元概览

| 项 | 信息 |
|---|---|
| 后端 | `engine/evaluation`（40 文件）；核心 `EvaluationEngineService`(903) / `EvaluationEngineController`(229) |
| 迁移 | V14 五方言齐全（evaluation_indicator/run/result、quality_finding、rectification_task/review、evaluation_idempotency_key） |
| 前端 | `quality/QcEvalSets.tsx`(772) / `QcAlerts.tsx`(675) / `QcEvalResults.tsx`(260) |
| 测试 | ServiceTest(10) / ControllerSecurityTest(5) / RepositoryTest(3) / IntegrationTest(2) = 20 |
| 业务目标 | 指标配置 / 病例命中扫描 / 问题生成 / 整改 / 复核闭环 |

---

## 2. 十维度审计结果

### 2.1 业务正确性 — ✅ 真实
- 自动病例扫描（`EvaluationEngineService.java:295-420`）**真三步算法**：分母入组（314）→ 排除（332）→ 分子达标（347），均通过**真实的 `RuleDslEvaluator.evaluate`**（A5 已确认真引擎）求值 `eval.hit()`，非写死。
- 不达标自动生成 `QualityFinding` + 整改派单（396），带责任科室、7 天期限（403）。
- P0 严重度由 `scoringDefinition` 识别（377）。

### 2.2 医疗安全合规 — ✅ 核心红线达标
- ✅ **P0 豁免门禁真实**：`reviewRectification:648-651` WAIVED + severity==P0 → 抛 `ENG_EVAL_007`「P0 质控问题不得通过普通复核豁免」，**且被 `EvaluationEngineServiceTest:245-248` 测试覆盖**。满足宪法「P0 不允许普通豁免」。
- 复核状态守卫：仅 SUBMITTED 任务 + REMEDIATING 问题可复核（644）；WAIVED 必带说明（654）。

### 2.3 多租户隔离 — ✅
- Controller 类级 `@DataScope(requireTenant=true)`（31）+ 细粒度权限（write/read/publish/execute/review，分工清晰）。
- 所有 Repository `...AndTenantId`；`RepositoryTest:94` 含 tenant-B 跨租户负向断言。

### 2.4 审计与证据链 — 🟡
- 复核走 `AuditAction.REVIEW` 审计（684）+ StateTransitionRecorder（680/682）；幂等键持久化（687）。
- ⚠️ **A8-M-02**：用 `AuditEventPublisher` 而非 `IsolatedAuditPublisher`，与 A12/EVID 等失败保全模式不统一；且校验失败路径未见失败审计。

### 2.5 五方言一致性 — ✅
V14 五方言齐全（含 evaluation_idempotency_key 幂等唯一键）。

### 2.6 代码净化 — 🟡
- 后端无 `Math.random`/写死推理/`System.out`。
- ⚠️ **A8-M-01**：扫描中 DSL 解析/求值的三处 `catch (Exception e)`（316/334/349）**静默吞异常**——畸形指标 DSL 被默默当作"未入组/未达标"跳过，无日志无审计。质控场景下可能**漏报质量缺陷**，且难以排障。
- ⚠️ **A8-M-03**：`QcEvalSets/QcAlerts/QcEvalResults` 三页首行均 `/* eslint-disable medkernel/no-page-mock */`，虽当前无实质假闭环（见 2.10），但属门禁削弱，应去除或收窄。

### 2.7 错误处理与降级 — ✅
`ENG-EVAL-001..007` 声明并使用；未激活指标/无入组/无来源均拒收（297/423）；不依赖模型，天然 B0 安全。

### 2.8 可观测性 — 🟡
traceId 贯穿、状态历史齐全；缺 Micrometer 指标（达标率/P0 计数/整改时效）。

### 2.9 测试覆盖与有效性 — ✅（本批最佳）
- ServiceTest 10 例含 P0 豁免拒绝（245-248，断言真实业务规则而非 mock 掉）；
- RepositoryTest 跨租户（94）；IntegrationTest 2 例真 DB + 幂等复核重放（137）。
- 建议补：畸形 DSL 指标扫描的显式用例（配合 A8-M-01）。

### 2.10 前后端契约一致 — ✅ 前端诚实
- `QcEvalSets.tsx:115-170` 所有 catch 均 `message.error` 真实报错，扫描真调后端（160）并渲染真实 `res`；"沙箱扫描仿真"是**显式标注**的指标试跑功能（用户选快照→真调 scan 接口），非伪造闭环。
- `QcAlerts.tsx:124/177` catch 同样真实报错。
- 与 A7/A12 前端「catch 伪造成功」形成对照——本单元前端**未造假**。

---

## 3. 七角色视角评估

| 角色 | 评估 |
|---|---|
| 医务处 | 指标库 7 步流 + 沙箱试跑 + 整改派单闭环真实可用 ✅ |
| 院长 | 评估结果/问题严重度分级真实；缺达标率趋势指标看板（A8-M-?） |
| 合规审计 | P0 不可豁免硬门禁 + 幂等复核留痕，合规性强 ✅ |

---

## 4. Findings 汇总

| Severity | ID | 一句话 | 位置 |
|---|---|---|---|
| Medium | A8-M-01 | 扫描 DSL 求值静默吞异常，畸形指标被默默跳过，可能漏报 | `EvaluationEngineService.java:316,334,349` |
| Medium | A8-M-02 | 用 AuditEventPublisher 非 Isolated，失败路径无审计 | `:684` 等 |
| Medium | A8-M-03 | 3 质控页 eslint-disable no-page-mock（门禁削弱） | `QcEvalSets/QcAlerts/QcEvalResults:1` |
| Low | A8-L-01 | 缺 Micrometer 质控指标埋点 | service 全局 |

合计：Critical 0 · High 0 · Medium 3 · Low 1

---

## 5. 改造方案

### A8-M-01 DSL 求值异常显式化（建议优先）
- **改造**：三处 `catch` 改为记录 WARN 日志 + 发审计（指标 ID + 错误），并对"指标 DSL 不可解析"返回可诊断结论而非静默跳过；或在指标激活前就校验 DSL 合法性（前移到 publish/activate 门禁）。
- **工作量**：约 2h。验证：畸形 DSL 指标扫描时产生告警日志/审计，不被静默吞。

### A8-M-02 统一 Isolated 审计 + 失败留痕
- 切到 `IsolatedAuditPublisher`，校验失败路径补 `outcome=FAILED` 审计。约 1.5h。

### A8-M-03 收回 eslint-disable
- 删三页首行 disable，若 lint 报错则将沙箱样本数据改为常量明确标注或移到 fixtures。约 1h。

### A8-L-01 指标埋点
- 注入 MeterRegistry，对达标率/P0 计数/整改关闭时效打点。约 1.5h。

---

## 6. 优化建议
- 病例扫描可批量化（当前按单快照），为质控驾驶舱（C4）提供聚合数据源。
- `scoringDefinition` 用 `contains("P0")` 文本判断严重度偏脆，建议结构化字段。

---

## 7. 总评
- **done 名副其实性**：EVAL-01 / API-08 **名副其实**。这是本批**首个前后端均达标**的单元：真 DSL 引擎、P0 红线门禁真实且被测、跨租户测试、前端诚实。
- **可否进入 GA 验收**：可。建议先处理 A8-M-01（静默吞异常可能漏报质量缺陷，对质控引擎是实质风险）。
- **参考价值**：A8 前端是 C2–C6 应对照的"诚实样板"——catch 报错、真调接口、沙箱显式标注，不伪造成功。
