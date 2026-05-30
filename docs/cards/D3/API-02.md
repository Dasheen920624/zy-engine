# API-02 · 临床事件 API

> 读卡前置：[核心 CONSTITUTION](../../CONSTITUTION.md) + [D3 域简报](_brief.md)。
> 迁移来源（覆盖矩阵锚点）：详规 §1.4 统一入参与事件入口 · 详规 S8 临床嵌入运行 · 落地规划 §17 按域纵向推进。

## 身份
- 卡 ID：API-02（引擎/API 卡）
- 域：D3 临床运行
- 关联场景：S8 临床嵌入运行
- 依赖卡：[API-01](../D2/API-01.md) 标准上下文 · [SYS-01](../D0/SYS-01.md) 标准模型 · [SYS-05](../D0/SYS-05.md) 在线/异步/批量 · [BASE-03](../D0/BASE-03.md) API 契约 · [OBS-01](../D0/OBS-01.md) trace · [OPT-02](OPT-02.md) 触发点
- 工作量：3d
- owner / reviewer：待派单（owner ≠ reviewer）

## 目标
把临床事件入口**契约化/补全**：同步/异步/批量/回放接收临床事件 → 触发 CDSS/路径/随访，含重试、死信、回调，全程 traceId 可追、幂等不重复触发。

## 现状（搬迁时核查 2026-05-30，以 `medkernel-backend` 为准）
已有基础：`engine/context/` 下 `ClinicalEvent` / `ClinicalEventAcceptedResponse` + `ClinicalEventController`（事件受理骨架）。本卡＝补齐异步/批量/回放/重试/死信/回调 + 6 触发点（[OPT-02](OPT-02.md)）契约 + 幂等键，非从零。

## 功能要求（原子可测条目）
- [ ] FR-1 同步受理：`POST` 单事件 → 同步返回受理结果 + traceId。
- [ ] FR-2 异步/批量：异步入队与批量提交（[SYS-05](../D0/SYS-05.md)）；批量部分失败逐条可见。
- [ ] FR-3 幂等：相同 `ClinicalEventIdempotencyKey` 不重复触发，返回首次结果。
- [ ] FR-4 重试/死信：失败按策略重试，超限入死信可查可重放。
- [ ] FR-5 回放/回调：历史事件回放重建上下文；处理完回调通知（白名单 URL）。
- [ ] FR-6 触发点归类：6 类触发点（[OPT-02](OPT-02.md)）统一入参，按 §1.4 12 字段。

## 接口契约 / 页面契约
### 接口契约（引擎/API 卡）
- 端点：`POST /api/v1/engine/clinical-events`（同步）· `.../clinical-events:batch`（批量）· `.../clinical-events:replay`（回放）· `GET .../clinical-events/dead-letter`（死信）
- DTO：`ClinicalEventRequest`（Record + Bean Validation：触发点/患者/就诊/包版本/事件体）→ `ClinicalEventAcceptedResponse`
- 响应信封：`ApiResult` / `ProblemDetail`（[BASE-03](../D0/BASE-03.md)）
- 状态机：告警/待办类（受理→处理中→已触发/失败→死信）
- 幂等 / 错误码 / traceId：`ClinicalEventIdempotencyKey` 幂等；失败 `ProblemDetail` + traceId（[OBS-01](../D0/OBS-01.md)）

## 数据与迁移
- 表族：`clinical_event`（事件 + 幂等键 + 触发点 + 组织字段 + 状态 + traceId + 审计）+ 死信表；五方言（[BASE-05](../D0/BASE-05.md)）
- 唯一约束：幂等键唯一；索引：患者/就诊/状态/时间

## 视角清单（11 视角逐条）
1. 产品架构：临床主链路的"事件总入口"。
2. 产品体验：N·A（后端 API；消费页在 [REMIND-01](REMIND-01.md)/[TODO-01](TODO-01.md)）。
3. 系统与数据架构：异步/批量/回放，10万级事件/日；幂等 + 死信；P95 同步 ≤500ms。
4. 临床医疗安全：事件只触发引擎命中、不绕引擎直写医嘱；触发失败不静默丢。
5. 知识与数据治理：事件携包版本，命中按 `ACTIVE` 权威版本（[SYS-08](../D2/SYS-08.md)）。
6. 安全合规与监管：受理/触发/回调留审计（[BASE-04](../D0/BASE-04.md)）。
7. 集团化与多租户治理：事件按 `OrgContext` 作用域路由。
8. 集成与互操作：CDS Hooks 风格触发点（[OPT-02](OPT-02.md)）；外部经适配器（[INTEG-01](../D2/INTEG-01.md)）。
9. 运维 / SRE / 国产化：死信可观测、可重放；异步队列降级。
10. 质量与真实性审计：触发真实命中、无伪造受理；死信不丢。
11. AI / 模型治理与可降级：N·A（事件入口确定性；模型在下游 CDSS 可降级）。

## 适用不变量
- 命中核心约束：**铁律 #1 真实性** · **核心 §5 状态机** · **§10 集成边界** · **§1.4 统一入参**。
- 本卡落点：临床事件可靠受理 + 幂等 + 死信可重放，喂下游 CDSS/路径/随访。

## 验收 + 验证
- [ ] AC-1（FR-1/2/3）：同步/异步/批量受理正确；重复幂等键不重复触发。
- [ ] AC-2（FR-4/5）：失败入死信可重放；回放重建上下文；回调命白名单。
- [ ] AC-3（FR-6）：6 触发点统一入参契约测试通过。
- 关联 A1–A9 剧本：A4 临床事件触发。
- T-GATE：后端真实性门禁全绿（无伪造受理 / 死信不丢）。
- B0 验收：关模型事件受理与确定性触发仍可用。

## 完工证据
- 代码 permalink：`engine/context` ClinicalEvent 补齐 + 死信/回放 + 契约测试。
- 测试：幂等 / 批量 / 死信重放 / 回调白名单 + 安全测试。
- 审计员签字：@<reviewer>（owner ≠ reviewer）。
