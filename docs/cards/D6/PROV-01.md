# PROV-01 · 来源追溯页

> 读卡前置：[核心 CONSTITUTION](../../CONSTITUTION.md) + [D6 域简报](_brief.md) + [体验契约](../../EXPERIENCE_CONTRACT.md)。
> 迁移来源（覆盖矩阵锚点）：详规 §3 S7 图谱与来源追溯 · 核心 §7 来源链 · 体验规范 §3。
> 实化映射：占位 `D6-PAGE-来源追溯` → 本卡 **PROV-01**。

## 身份
- 卡 ID：PROV-01（页面卡；= backlog `D6-PAGE-来源追溯` 实化）
- 域：D6 高级工具
- 关联场景：S7 图谱与来源追溯
- 依赖卡：[KNOW-01](../D2/KNOW-01.md)/[OPT-07](../D2/OPT-07.md)（来源链/分级）· [SYS-08](../D2/SYS-08.md)（版本）· [BASE-08](../D0/BASE-08.md)/[BASE-10](../D0/BASE-10.md) · [API-13](../D0/API-13.md) · [INFRA-09](../D1/INFRA-09.md)
- 工作量：3d
- owner / reviewer：待派单（owner ≠ reviewer）

## 目标
把来源追溯页**真实化**：对一条结论/推荐/知识/规则命中，**追溯到来源条目与版本**（来源链 `KnowledgeLineage`），全部真实，**不前端编造来源**。

## 现状（搬迁时核查 2026-05-30，以 `frontend/src` 为准）
页面**已存在待真实化**：`pages/advanced/Provenance.tsx`（路由 `/advanced/provenance` 已注册 `app/router.tsx`）。本卡＝去占位/mock + 接来源链 API（`engine/knowledge` `KnowledgeLineage` + [OPT-07](../D2/OPT-07.md)）+ 六态/RBAC 齐全。

## 功能要求（原子可测条目）
- [ ] FR-1 追溯入口：从结论/推荐/规则命中追溯来源（[API-13](../D0/API-13.md) 分页）。
- [ ] FR-2 来源链：展示来源条目 + 版本 + 分级（[OPT-07](../D2/OPT-07.md)）+ 冲突仲裁，到条。
- [ ] FR-3 版本一致：追溯到的版本与命中所用版本一致（[SYS-08](../D2/SYS-08.md)），旧版标"历史"。
- [ ] FR-4 六态：加载/空/错误/无权限/部分成功/正常齐全（[BASE-08](../D0/BASE-08.md)）。
- [ ] FR-5 RBAC：专家/架构师可见；数据按 `OrgContext`；技术对象不入客户主菜单。

## 接口契约 / 页面契约
### 接口契约（引擎/API 卡）
N·A —— 消费 `engine/knowledge` 来源链 + [OPT-07](../D2/OPT-07.md) API。
### 页面契约（页面卡）
- 路由元数据：sectionKey `advanced` / menuKey `provenance` / menuLabel `来源追溯` / path `/advanced/provenance` / requiredPermissions 来源追溯 / requiredRoles 专科专家·架构师。
- 结构：PageShell（[BASE-08](../D0/BASE-08.md)）+ 追溯入口 + 来源链时间轴/树 + 版本标识 + 六态。
- 主按钮 ≤1（导出来源）/ 默认筛选 ≤3 / 默认角色视图（专家）。
- 五维 RBAC：菜单 / 动作（导出）/ 数据（org）/ 资产（知识版本）/ 环境。
- 样式：仅引用 [BASE-10](../D0/BASE-10.md) token + [体验契约](../../EXPERIENCE_CONTRACT.md)；禁硬编码 hex/px。

## 数据与迁移
N·A —— 页面卡不落库；消费 `engine/knowledge` 后端。

## 视角清单（11 视角逐条）
1. 产品架构：可解释性的"来源追溯"工具页。
2. 产品体验：来源链清晰、到条可追；国产浏览器可读。
3. 系统与数据架构：来源链查询 P95 ≤1.5s。
4. 临床医疗安全：临床结论可追溯来源、增强可信。
5. 知识与数据治理：★来源到条 + 版本 + 分级（[OPT-07](../D2/OPT-07.md)）可追溯。
6. 安全合规与监管：追溯导出留审计（[BASE-04](../D0/BASE-04.md)）。
7. 集团化与多租户治理：按 `OrgContext` 作用域。
8. 集成与互操作：N·A（页面）。
9. 运维 / SRE / 国产化：内网慢场景骨架。
10. 质量与真实性审计：★无前端编造来源、版本一致、到条真实；无演示路由（[INFRA-09](../D1/INFRA-09.md)）。
11. AI / 模型治理与可降级：N·A（确定性追溯）。

## 适用不变量
- 命中核心约束：**核心 §7 来源链** · **铁律 #1 真实性** · **§13 可追溯** · **技术对象不入主路径**。
- 本卡落点：把来源追溯页变为接真实来源链、到条到版本、不编造的可解释工具。

## 验收 + 验证
- [ ] AC-1（FR-1/2）：追溯到来源条目 + 版本 + 分级。
- [ ] AC-2（FR-3）：版本与命中一致、旧版标历史。
- [ ] AC-3（FR-4/5）：六态齐全；专家可见、不入客户主菜单。
- 关联 A1–A9 剧本：A9 来源追溯。
- T-GATE：前端真实性门禁全绿（no-page-mock、无编造来源）。
- B0 验收：N·A（确定性页面）。

## 完工证据
- 代码 permalink：`pages/advanced/Provenance` 真实化 + 接来源链 API + 六态。
- 测试：追溯/来源链/版本一致 + 六态 + RBAC + no-page-mock 门禁。
- 审计员签字：@<reviewer>（owner ≠ reviewer）。
