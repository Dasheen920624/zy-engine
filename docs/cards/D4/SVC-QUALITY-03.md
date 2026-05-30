# SVC-QUALITY-03 · 整改闭环服务包

> 读卡前置：[核心 CONSTITUTION](../../CONSTITUTION.md) + [D4 域简报](_brief.md)。
> 迁移来源（覆盖矩阵锚点）：详规 S11 智能评估与整改 · 详规 整改闭环 · 落地规划 §服务包。

## 身份
- 卡 ID：SVC-QUALITY-03（服务包卡；`RectificationTask`/`RectificationReview` 单一归属）
- 域：D4 质控改进
- 关联场景：S11 智能评估与整改
- 依赖卡：[EVAL-01](EVAL-01.md) 评估（问题源）· [MED-C3](../D3/MED-C3.md) 安全复核 · [SVC-CLINICAL-03](../D3/SVC-CLINICAL-03.md) 待办 · 页 [EVALRES-01](EVALRES-01.md)
- 工作量：3d
- owner / reviewer：待派单（owner ≠ reviewer）

## 目标
把**问题 → 责任科室 → 整改 → 复核 → 豁免 → 报告**做成整改闭环服务包：质控问题可派、可整改、可复核、可豁免（带理由），闭环可审计、不可静默关闭。

## 现状（搬迁时核查 2026-05-30，以 `medkernel-backend` 为准）
已有实质基础：`engine/evaluation/` 下 `RectificationTask{+Repository/Status}` + `RectificationReview{+Decision/Repository/Request/Response}` + `RectificationSubmitRequest` + `RectificationResponse`。本卡＝把"派发→整改提交→复核决策→豁免→报告"框架化为服务包，问题源来自 [EVAL-01](EVAL-01.md)。

## 功能要求（原子可测条目）
- [ ] FR-1 派发：问题派到责任科室/责任人，带截止（`RectificationTask`）。
- [ ] FR-2 整改提交：科室提交整改证据（`RectificationSubmitRequest`）。
- [ ] FR-3 复核决策：质控复核通过/驳回（`RectificationReviewDecision`），驳回带原因。
- [ ] FR-4 豁免：可豁免（带理由 + 审批），豁免留痕不可静默。
- [ ] FR-5 报告：整改闭环率/超期/豁免报告，真实统计。
- [ ] FR-6 安全联动：安全复核任务（[MED-C3](../D3/MED-C3.md)）汇入、高优先。

## 接口契约 / 页面契约
### 接口契约（引擎/API 卡）
- 端点：`POST /api/v1/engine/rectifications`（派发）· `POST .../rectifications/{id}/submit` · `POST .../rectifications/{id}/review` · `POST .../rectifications/{id}/waive`
- DTO：`RectificationSubmitRequest` / `RectificationReviewRequest` → `RectificationResponse`；信封 `ApiResult`
- 状态机：待办类（待整改→整改中→待复核→已闭环/已豁免/驳回重整改）
- 幂等 / traceId：派发/复核幂等；trace（[OBS-01](../D0/OBS-01.md)）

## 数据与迁移
- 复用 `rectification_task` / `rectification_review` 表族（本卡归属）+ 豁免/报告；五方言（[BASE-05](../D0/BASE-05.md)）

## 视角清单（11 视角逐条）
1. 产品架构：质控"改"的闭环服务枢纽。
2. 产品体验：整改任务进待办（[SVC-CLINICAL-03](../D3/SVC-CLINICAL-03.md)）；状态清晰。
3. 系统与数据架构：闭环状态机；报告聚合；P95 ≤1s。
4. 临床医疗安全：安全复核任务高优先不漏（[MED-C3](../D3/MED-C3.md)）。
5. 知识与数据治理：整改证据可追溯问题与指标版本。
6. 安全合规与监管：★整改/复核/豁免全留审计（[BASE-04](../D0/BASE-04.md)）；豁免不可静默。
7. 集团化与多租户治理：按责任科室 + `OrgContext` 作用域。
8. 集成与互操作：报告可外发（[INTEG-01](../D2/INTEG-01.md)）。
9. 运维 / SRE / 国产化：超期提醒可观测。
10. 质量与真实性审计：★闭环率真实、豁免有据、无伪造整改。
11. AI / 模型治理与可降级：N·A（闭环确定性）。

## 适用不变量
- 命中核心约束：**铁律 #1 真实性** · **核心 §5 状态机闭环** · **§9 多租户作用域** · **§6 安全（豁免不可静默）**。
- 本卡落点：问题→整改→复核→豁免→报告闭环，问题源归 [EVAL-01](EVAL-01.md)。

## 验收 + 验证
- [ ] AC-1（FR-1/2/3）：派发/提交/复核闭环正确，驳回带原因。
- [ ] AC-2（FR-4/5）：豁免有据留痕；报告真实。
- [ ] AC-3（FR-6）：安全复核任务高优先汇入。
- 关联 A1–A9 剧本：A9 整改闭环 · A8 安全复核。
- T-GATE：后端真实性门禁全绿（闭环率真实/豁免有据）。
- B0 验收：整改闭环确定性可用。

## 完工证据
- 代码 permalink：`engine/evaluation` 整改闭环服务包。
- 测试：派发/整改/复核/豁免/报告/安全汇入。
- 审计员签字：@<reviewer>（owner ≠ reviewer）。
