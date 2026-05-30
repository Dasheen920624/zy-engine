# API-09 · 随访 API

> 读卡前置：[核心 CONSTITUTION](../../CONSTITUTION.md) + [D3 域简报](_brief.md)。
> 迁移来源（覆盖矩阵锚点）：详规 S12 智能随访 · 详规 §1.4 随访入参 · 落地规划 §17。

## 身份
- 卡 ID：API-09（引擎/API 卡）
- 域：D3 临床运行
- 关联场景：S12 智能随访
- 依赖卡：[FOLLOW-01](FOLLOW-01.md) 随访引擎（单一归属）· [API-02](API-02.md) 事件 · [BASE-03](../D0/BASE-03.md) API 契约 · [API-13](../D0/API-13.md) 大列表
- 工作量：3d
- owner / reviewer：待派单（owner ≠ reviewer）

## 目标
把随访能力**契约化**：计划/任务/问卷下发、异常回院触发、结果回流，全部受控事实驱动、可追溯、可降级。

## 现状（搬迁时核查 2026-05-30，以 `medkernel-backend` 为准）
已有实质基础：`engine/followup/` 下 `FollowupEngineController` / `FollowupEngineService` + `FollowupEvent(+Repository)` + `FollowupAbnormalReportRequest` + 安全/契约测试。本卡＝把控制器契约化为统一 API（计划/任务/问卷/异常/回流），生成逻辑归 [FOLLOW-01](FOLLOW-01.md)。

## 功能要求（原子可测条目）
- [ ] FR-1 计划/任务：按患者取随访计划与任务列表（[API-13](../D0/API-13.md) 分页）。
- [ ] FR-2 问卷：下发问卷、回收作答，结构化存储。
- [ ] FR-3 异常回院：异常事件（`FollowupAbnormalReportRequest`）触发回院任务 + 通知。
- [ ] FR-4 结果回流：随访结果回流到患者上下文（[API-01](../D2/API-01.md)），可被后续命中消费。
- [ ] FR-5 幂等/降级：任务下发幂等；模型不可用时按确定性计划仍可跑。

## 接口契约 / 页面契约
### 接口契约（引擎/API 卡）
- 端点：`GET /api/v1/engine/followup/plans` · `GET .../followup/tasks` · `POST .../followup/questionnaires` · `POST .../followup/abnormal-reports` · `POST .../followup/results`
- DTO：`FollowupAbnormalReportRequest` + 计划/任务/问卷 Record DTO → `ApiResult`
- 响应信封：`ApiResult` / `ProblemDetail`；状态机：待办类（待执行→进行中→完成/异常回院）
- 幂等 / 错误码 / traceId：任务幂等键；trace（[OBS-01](../D0/OBS-01.md)）

## 数据与迁移
- 复用 `FollowupEvent` 表族（[FOLLOW-01](FOLLOW-01.md) 归属）+ 问卷/作答/结果表；五方言（[BASE-05](../D0/BASE-05.md)）

## 视角清单（11 视角逐条）
1. 产品架构：随访对外契约层（生成归引擎 [FOLLOW-01](FOLLOW-01.md)）。
2. 产品体验：N·A（页面在 [FUP-01](FUP-01.md)）。
3. 系统与数据架构：任务下发批量、问卷大表分页；P95 ≤1s。
4. 临床医疗安全：异常回院不漏派；结果回流不污染权威上下文。
5. 知识与数据治理：随访计划绑定路径/知识版本（[SYS-08](../D2/SYS-08.md)），结果可追溯。
6. 安全合规与监管：问卷/异常/回流留审计（[BASE-04](../D0/BASE-04.md)）；患者隐私最小化。
7. 集团化与多租户治理：随访按 `OrgContext`/随访团队作用域。
8. 集成与互操作：异常回院可经 [INTEG-01](../D2/INTEG-01.md) 通知外部；结果回流标准化。
9. 运维 / SRE / 国产化：任务队列可观测、可重试。
10. 质量与真实性审计：★随访人群受控事实驱动、不写死；无伪造作答。
11. AI / 模型治理与可降级：智能问卷/分层为挂点，关闭按确定性计划 `MODEL_DISABLED`。

## 适用不变量
- 命中核心约束：**铁律 #1 真实性** · **核心 §11 B0** · **§1.4 统一入参** · **§7 权威版本**。
- 本卡落点：随访计划/任务/问卷/异常/回流的真实契约，生成归 [FOLLOW-01](FOLLOW-01.md)。

## 验收 + 验证
- [ ] AC-1（FR-1/2）：计划/任务/问卷列表与作答正确。
- [ ] AC-2（FR-3/4）：异常回院触发任务 + 通知；结果回流可被消费。
- [ ] AC-3（FR-5）：任务幂等；关模型确定性计划可跑。
- 关联 A1–A9 剧本：A7 随访接续。
- T-GATE：后端真实性门禁全绿（无伪造作答 / 不写死人群）。
- B0 验收：关模型随访契约仍可用。

## 完工证据
- 代码 permalink：`engine/followup` 控制器契约化 + 问卷/异常/回流。
- 测试：异常回院 / 结果回流 / 降级 + 安全测试。
- 审计员签字：@<reviewer>（owner ≠ reviewer）。
