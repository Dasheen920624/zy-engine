# INSAUDIT-01 · 医保智能审核页

> 读卡前置：[核心 CONSTITUTION](../../CONSTITUTION.md) + [D4 域简报](_brief.md) + [体验契约](../../EXPERIENCE_CONTRACT.md)。
> 迁移来源（覆盖矩阵锚点）：详规 §3 S10 医保与病案质控 · 详规 医保审核 · 体验规范 §3。
> 实化映射：占位 `D4-PAGE-医保智能审核` → 本卡 **INSAUDIT-01**。

## 身份
- 卡 ID：INSAUDIT-01（页面卡；= backlog `D4-PAGE-医保智能审核` 实化）
- 域：D4 质控改进
- 关联场景：S10 医保与病案质控
- 依赖卡：[SVC-QUALITY-02](SVC-QUALITY-02.md)（病案医保后端）· [SVC-QUALITY-03](SVC-QUALITY-03.md)（整改）· [BASE-08](../D0/BASE-08.md)/[BASE-10](../D0/BASE-10.md) · [API-13](../D0/API-13.md) · [INFRA-09](../D1/INFRA-09.md)
- 工作量：3d
- owner / reviewer：待派单（owner ≠ reviewer）

## 目标
把医保智能审核页**真实化**：呈现医保违规/DRG 入组/编码费用问题，可追溯病历证据、派整改，全部接 [SVC-QUALITY-02](SVC-QUALITY-02.md)，**不前端造违规、不臆造**。

## 现状（搬迁时核查 2026-05-30，以 `frontend/src` 为准）
页面**已存在待真实化**：`pages/quality/InsuranceAudit.tsx`（路由 `/qc/insurance` 已注册 `app/router.tsx`）。本卡＝去占位/mock + 接医保审核/DRG/编码 API + 六态/五维 RBAC 齐全。

## 功能要求（原子可测条目）
- [ ] FR-1 违规列表：医保违规/编码/费用问题真实（[SVC-QUALITY-02](SVC-QUALITY-02.md)），含规则依据。
- [ ] FR-2 证据追溯：每条违规可追溯到病历证据，不臆造。
- [ ] FR-3 DRG/DIP：入组结果与异常可见、可解释。
- [ ] FR-4 派整改：违规一键派整改（[SVC-QUALITY-03](SVC-QUALITY-03.md)）。
- [ ] FR-5 六态 + 五维 RBAC：齐全；医保办/病案室按作用域；数据按 `OrgContext`。

## 接口契约 / 页面契约
### 接口契约（引擎/API 卡）
N·A —— 消费 [SVC-QUALITY-02](SVC-QUALITY-02.md) 医保/病案 API。
### 页面契约（页面卡）
- 路由元数据：sectionKey `quality` / menuKey `qc-insurance` / menuLabel `医保智能审核` / path `/qc/insurance` / requiredPermissions 医保审核 / requiredRoles 医保办·病案室。
- 结构：PageShell（[BASE-08](../D0/BASE-08.md)）+ 违规列表 + 证据追溯抽屉 + DRG 入组面板 + 六态。
- 主按钮 ≤1（派整改）/ 默认筛选 ≤3（未处理/本月/高金额）/ 默认角色视图（医保办）。
- 五维 RBAC：菜单 / 动作（派整改）/ 数据（org）/ 资产 / 环境。
- 样式：仅引用 [BASE-10](../D0/BASE-10.md) token + [体验契约](../../EXPERIENCE_CONTRACT.md)；禁硬编码 hex/px。

## 数据与迁移
N·A —— 页面卡不落库；消费 [SVC-QUALITY-02](SVC-QUALITY-02.md) 后端。

## 视角清单（11 视角逐条）
1. 产品架构：医保合规的"审核工作台"。
2. 产品体验：违规可追溯病历、一键派整改；国产浏览器可读。
3. 系统与数据架构：违规列表分页 P95 ≤1s。
4. 临床医疗安全：医保审核不干预临床诊疗决策。
5. 知识与数据治理：违规规则版本化可追溯（[SYS-08](../D2/SYS-08.md)）。
6. 安全合规与监管：★违规作监管证据须有病历依据、留审计（[BASE-04](../D0/BASE-04.md)）。
7. 集团化与多租户治理：按院/病案室作用域。
8. 集成与互操作：医保数据经适配器（[INTEG-01](../D2/INTEG-01.md)）入。
9. 运维 / SRE / 国产化：内网慢场景骨架。
10. 质量与真实性审计：★无前端造违规、违规追溯病历、不臆造；无演示路由（[INFRA-09](../D1/INFRA-09.md)）。
11. AI / 模型治理与可降级：编码辅助为挂点，关模型确定性规则审核。

## 适用不变量
- 命中核心约束：**铁律 #1 真实性（不臆造违规）** · **§2 菜单 IA** · **§9 多租户作用域** · **合规监管**。
- 本卡落点：把医保审核页变为接真实违规、追溯病历证据、可派整改的审核台。

## 验收 + 验证
- [ ] AC-1（FR-1/2）：违规真实、追溯病历证据。
- [ ] AC-2（FR-3/4）：DRG 入组可解释；一键派整改。
- [ ] AC-3（FR-5）：六态齐全；按作用域。
- 关联 A1–A9 剧本：A9 医保病案审核。
- T-GATE：前端真实性门禁全绿（no-page-mock、无造违规）。
- B0 验收：N·A（确定性页面）。

## 完工证据
- 代码 permalink：`pages/quality/InsuranceAudit` 真实化 + 接 [SVC-QUALITY-02](SVC-QUALITY-02.md) + 六态。
- 测试：违规/证据追溯/DRG/派整改 + 六态 + RBAC + no-page-mock 门禁。
- 审计员签字：@<reviewer>（owner ≠ reviewer）。
