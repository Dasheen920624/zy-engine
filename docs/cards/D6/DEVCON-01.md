# DEVCON-01 · 开发者控制台页

> 读卡前置：[核心 CONSTITUTION](../../CONSTITUTION.md) + [D6 域简报](_brief.md) + [体验契约](../../EXPERIENCE_CONTRACT.md)。
> 迁移来源（覆盖矩阵锚点）：详规 开发者工具 · 核心 §10 集成边界 · 体验规范 §3。
> 实化映射：占位 `D6-PAGE-开发者控制台` → 本卡 **DEVCON-01**。

## 身份
- 卡 ID：DEVCON-01（页面卡；= backlog `D6-PAGE-开发者控制台` 实化）
- 域：D6 高级工具
- 关联场景：生态扩展 / 开发者
- 依赖卡：[OPT-10](OPT-10.md)（插件边界）· [OBS-01](../D0/OBS-01.md)（可观测/traceId）· [BASE-03](../D0/BASE-03.md)（API 契约）· [BASE-08](../D0/BASE-08.md)/[BASE-10](../D0/BASE-10.md) · [INFRA-09](../D1/INFRA-09.md)
- 工作量：2d
- owner / reviewer：待派单（owner ≠ reviewer）

## 目标
把开发者控制台页**真实化**：API 浏览/调试（按权限）、traceId 追踪、插件管理（[OPT-10](OPT-10.md)）、Webhook/回调查看，给开发者用，**受控、不绕权限、不暴露敏感**。

## 现状（搬迁时核查 2026-05-30，以 `frontend/src` 为准）
页面**已存在待真实化**：`pages/advanced/DevConsole.tsx`（路由 `/advanced/dev-console` 已注册 `app/router.tsx`）。本卡＝去占位/mock + 接 API 目录/trace/插件管理 API（[OPT-10](OPT-10.md)/[OBS-01](../D0/OBS-01.md)）+ 六态/RBAC；**调试受权限约束、不暴露敏感**。

## 功能要求（原子可测条目）
- [ ] FR-1 API 浏览：列可用 API（按权限），契约（[BASE-03](../D0/BASE-03.md)）可查。
- [ ] FR-2 调试：在权限内调试 API，**不绕 RBAC**、不可越权造数据。
- [ ] FR-3 trace 追踪：按 traceId 查链路（[OBS-01](../D0/OBS-01.md)）。
- [ ] FR-4 插件管理：插件注册/授权/禁用（[OPT-10](OPT-10.md)），受控。
- [ ] FR-5 RBAC + 不暴露：开发者可见；敏感配置/密钥不暴露；数据按 `OrgContext`；不入客户主菜单。

## 接口契约 / 页面契约
### 接口契约（引擎/API 卡）
N·A —— 消费 API 目录 + [OBS-01](../D0/OBS-01.md) trace + [OPT-10](OPT-10.md) 插件 API。
### 页面契约（页面卡）
- 路由元数据：sectionKey `advanced` / menuKey `dev-console` / menuLabel `开发者控制台` / path `/advanced/dev-console` / requiredPermissions 开发者控制台 / requiredRoles 开发者·架构师·运维。
- 结构：PageShell（[BASE-08](../D0/BASE-08.md)）+ API 目录 + 调试器 + trace 查询 + 插件管理 + 六态。
- 主按钮 ≤1（调试/发送）/ 默认筛选 ≤3 / 默认角色视图（开发者）。
- 五维 RBAC：菜单 / 动作（调试/插件管理）/ 数据（org）/ 资产 / 环境。
- 样式：仅引用 [BASE-10](../D0/BASE-10.md) token + [体验契约](../../EXPERIENCE_CONTRACT.md)；禁硬编码 hex/px。

## 数据与迁移
N·A —— 页面卡不落库；消费各后端。

## 视角清单（11 视角逐条）
1. 产品架构：开发者/集成的"控制台"工具页。
2. 产品体验：API/调试/trace 清晰；国产浏览器可读。
3. 系统与数据架构：调试受限频率；trace 查询 P95 ≤1s。
4. 临床医疗安全：调试不可造临床数据、不绕引擎。
5. 知识与数据治理：N·A。
6. 安全合规与监管：★调试/插件操作留审计（[BASE-04](../D0/BASE-04.md)）；不暴露密钥。
7. 集团化与多租户治理：按 `OrgContext` 作用域；调试不可跨租户。
8. 集成与互操作：★插件管理（[OPT-10](OPT-10.md)）+ API 契约（[BASE-03](../D0/BASE-03.md)）。
9. 运维 / SRE / 国产化：trace/插件可观测。
10. 质量与真实性审计：★调试不绕 RBAC、不暴露敏感、不越权造数；无演示路由（[INFRA-09](../D1/INFRA-09.md)）。
11. AI / 模型治理与可降级：N·A。

## 适用不变量
- 命中核心约束：**核心 §10 集成边界** · **§6 安全（不暴露/不绕权限）** · **铁律 #1** · **技术对象不入主路径**。
- 本卡落点：把开发者控制台页变为接真实 API/trace/插件、受权限约束、不暴露敏感的工具台。

## 验收 + 验证
- [ ] AC-1（FR-1/2）：API 浏览/调试按权限、不绕 RBAC。
- [ ] AC-2（FR-3/4）：trace 可查；插件管理受控（[OPT-10](OPT-10.md)）。
- [ ] AC-3（FR-5）：六态齐全；不暴露敏感、不入客户主菜单。
- 关联 A1–A9 剧本：A9 开发者控制台。
- T-GATE：前端真实性门禁全绿（no-page-mock、不绕权限、不暴露敏感）。
- B0 验收：N·A（确定性页面）。

## 完工证据
- 代码 permalink：`pages/advanced/DevConsole` 真实化 + 接 API/trace/插件 + 六态。
- 测试：API 浏览/调试受限/trace/插件 + 六态 + RBAC + no-page-mock 门禁。
- 审计员签字：@<reviewer>（owner ≠ reviewer）。
