# AIFLOW-01 · AI 工作流页

> 读卡前置：[核心 CONSTITUTION](../../CONSTITUTION.md) + [D6 域简报](_brief.md) + [体验契约](../../EXPERIENCE_CONTRACT.md)。
> 迁移来源（覆盖矩阵锚点）：详规 AI 工作流 · 核心 §11 B0 先于模型 · 体验规范 §3。
> 实化映射：占位 `D6-PAGE-AI 工作流` → 本卡 **AIFLOW-01**。

## 身份
- 卡 ID：AIFLOW-01（页面卡；= backlog `D6-PAGE-AI 工作流` 实化）
- 域：D6 高级工具
- 关联场景：S15 AI 验证与验收（技术侧）
- 依赖卡：[BASE-08](../D0/BASE-08.md)/[BASE-10](../D0/BASE-10.md) · [INFRA-09](../D1/INFRA-09.md) · wave2 LLM-*/AIK-*（编排，后置）
- 工作量：2d
- owner / reviewer：待派单（owner ≠ reviewer）

## 目标
把 AI 工作流页**做成壳/查看**：呈现已配置 AI 工作流定义（只读查看 + 状态），**本期不做编排/执行**（编排留第二波 wave2），关模型诚实显示，**不前端伪造编排能力**。

## 现状（搬迁时核查 2026-05-30，以 `frontend/src` 为准）
页面**已存在待真实化**：`pages/advanced/AiWorkflows.tsx`（路由 `/advanced/ai-workflows` 已注册 `app/router.tsx`）。本卡＝去占位/mock + 接工作流**查看**API（壳）+ 六态/RBAC；**编排/执行不在本期**（wave2）。

## 功能要求（原子可测条目）
- [ ] FR-1 工作流列表：列已定义 AI 工作流（只读查看），真实或诚实空态。
- [ ] FR-2 查看详情：查看工作流定义/节点/状态（只读）。
- [ ] FR-3 编排后置：**本期不提供编排/执行**；编排入口诚实标"第二波"，不前端假执行。
- [ ] FR-4 降级：模型/编排引擎不可用诚实显示 `MODEL_DISABLED`/`NOT_AVAILABLE`。
- [ ] FR-5 六态 + RBAC：齐全；开发者/架构师可见；数据按 `OrgContext`；不入客户主菜单。

## 接口契约 / 页面契约
### 接口契约（引擎/API 卡）
N·A —— 消费 AI 工作流查看 API（壳）；编排/执行 API 在 wave2。
### 页面契约（页面卡）
- 路由元数据：sectionKey `advanced` / menuKey `ai-workflows` / menuLabel `AI 工作流` / path `/advanced/ai-workflows` / requiredPermissions AI 工作流 / requiredRoles 开发者·架构师。
- 结构：PageShell（[BASE-08](../D0/BASE-08.md)）+ 工作流列表/查看 + "编排留第二波"提示 + 六态。
- 主按钮 ≤1（查看）/ 默认筛选 ≤3 / 默认角色视图（开发者）。
- 五维 RBAC：菜单 / 动作（查看）/ 数据（org）/ 资产 / 环境。
- 样式：仅引用 [BASE-10](../D0/BASE-10.md) token + [体验契约](../../EXPERIENCE_CONTRACT.md)；禁硬编码 hex/px。

## 数据与迁移
N·A —— 页面卡不落库；查看后端（壳）。

## 视角清单（11 视角逐条）
1. 产品架构：AI 工作流的"查看壳"页（编排 wave2）。
2. 产品体验：诚实空态/查看；国产浏览器可读。
3. 系统与数据架构：查看列表 P95 ≤1s。
4. 临床医疗安全：N·A（技术工具）。
5. 知识与数据治理：N·A。
6. 安全合规与监管：查看留审计（[BASE-04](../D0/BASE-04.md)）。
7. 集团化与多租户治理：按 `OrgContext` 作用域。
8. 集成与互操作：编排引擎（Dify 等）对接留 wave2。
9. 运维 / SRE / 国产化：内网慢场景骨架。
10. 质量与真实性审计：★**本期只壳/查看、不前端假编排/假执行**；无演示路由（[INFRA-09](../D1/INFRA-09.md)）。
11. AI / 模型治理与可降级：★B0 先于模型——编排/执行留 wave2，关模型诚实降级。

## 适用不变量
- 命中核心约束：**核心 §11 B0 先于模型（编排后置）** · **铁律 #1 真实性** · **技术对象不入主路径**。
- 本卡落点：把 AI 工作流页做成只读查看壳，编排/执行诚实留第二波、不前端伪造。

## 验收 + 验证
- [ ] AC-1（FR-1/2）：工作流可查看（或诚实空态）。
- [ ] AC-2（FR-3/4）：无前端假编排/执行；不可用诚实降级。
- [ ] AC-3（FR-5）：六态齐全；开发者可见、不入客户主菜单。
- 关联 A1–A9 剧本：A9 AI 工作流查看。
- T-GATE：前端真实性门禁全绿（no-page-mock、无假编排/执行）。
- B0 验收：★关模型壳可查看、不伪造能力。

## 完工证据
- 代码 permalink：`pages/advanced/AiWorkflows` 真实化（查看壳）+ 六态。
- 测试：查看/诚实空态/无假执行 + 六态 + RBAC + no-page-mock 门禁。
- 审计员签字：@<reviewer>（owner ≠ reviewer）。
