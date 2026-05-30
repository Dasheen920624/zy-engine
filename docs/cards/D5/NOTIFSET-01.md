# NOTIFSET-01 · 通知设置页

> 读卡前置：[核心 CONSTITUTION](../../CONSTITUTION.md) + [D5 域简报](_brief.md) + [体验契约](../../EXPERIENCE_CONTRACT.md)。
> 迁移来源（覆盖矩阵锚点）：详规 §3 S14 用户、权限与合规 · 详规 通知设置 · 体验规范 §3 低打扰。
> 实化映射：占位 `D5-PAGE-通知设置` → 本卡 **NOTIFSET-01**。

## 身份
- 卡 ID：NOTIFSET-01（页面卡；= backlog `D5-PAGE-通知设置` 实化）
- 域：D5 合规运维
- 关联场景：S14 用户、权限与合规
- 依赖卡：[SVC-CLINICAL-03](../D3/SVC-CLINICAL-03.md)（通知后端）· [CONFIG-01](../D0/CONFIG-01.md)（系统通知配置）· [NOTIFY-01](../D3/NOTIFY-01.md)（通知中心）· [BASE-08](../D0/BASE-08.md)/[BASE-10](../D0/BASE-10.md) · [INFRA-09](../D1/INFRA-09.md)
- 工作量：2d
- owner / reviewer：待派单（owner ≠ reviewer）

## 目标
把通知设置页**真实化**：用户/管理员配置通知渠道、订阅类型、免打扰时段，低打扰，全部接真实通知配置，**不前端假保存**。

## 现状（搬迁时核查 2026-05-30，以 `frontend/src` 为准）
页面**已存在待真实化**：`pages/compliance/NotificationSettings.tsx`（路由 `/notifications/settings` 已注册 `app/router.tsx`；通知中心 [NOTIFY-01](../D3/NOTIFY-01.md) 在 `/notifications`）。本卡＝去占位/mock + 接通知偏好/渠道/免打扰 API + 六态/五维 RBAC 齐全。

## 功能要求（原子可测条目）
- [ ] FR-1 渠道配置：配置通知渠道（站内/短信/院内消息），真实保存。
- [ ] FR-2 订阅类型：按通知类型订阅/退订（安全/危急类不可全退订）。
- [ ] FR-3 免打扰：免打扰时段配置；安全/危急通知不受免打扰静默。
- [ ] FR-4 系统级 vs 个人级：管理员配系统默认（[CONFIG-01](../D0/CONFIG-01.md)），用户配个人偏好。
- [ ] FR-5 六态 + 五维 RBAC：齐全；个人偏好本人可改、系统级管理员可改；按 `OrgContext`。

## 接口契约 / 页面契约
### 接口契约（引擎/API 卡）
N·A —— 消费 [SVC-CLINICAL-03](../D3/SVC-CLINICAL-03.md) 通知 + [CONFIG-01](../D0/CONFIG-01.md) 系统通知配置 API。
### 页面契约（页面卡）
- 路由元数据：sectionKey `compliance` / menuKey `notification-settings` / menuLabel `通知设置` / path `/notifications/settings` / requiredPermissions 通知设置 / requiredRoles 全角色（个人）+ 管理员（系统级）。
- 结构：PageShell（[BASE-08](../D0/BASE-08.md)）+ 渠道/订阅/免打扰分组 + 六态。
- 主按钮 ≤1（保存设置）/ 默认筛选 ≤3 / 默认角色视图（个人）。
- 五维 RBAC：菜单 / 动作（改设置）/ 数据（本人/org）/ 资产 / 环境。
- 样式：仅引用 [BASE-10](../D0/BASE-10.md) token + [体验契约](../../EXPERIENCE_CONTRACT.md)；禁硬编码 hex/px。

## 数据与迁移
N·A —— 页面卡不落库；消费 [SVC-CLINICAL-03](../D3/SVC-CLINICAL-03.md)/[CONFIG-01](../D0/CONFIG-01.md) 后端。

## 视角清单（11 视角逐条）
1. 产品架构：通知的"偏好与渠道配置"页。
2. 产品体验：★低打扰、分组清晰、免打扰可设；国产浏览器/老年模式可读。
3. 系统与数据架构：设置保存即时；P95 ≤1s。
4. 临床医疗安全：★安全/危急通知不可被免打扰/退订静默。
5. 知识与数据治理：N·A。
6. 安全合规与监管：设置变更留审计（[BASE-04](../D0/BASE-04.md)）。
7. 集团化与多租户治理：系统级配置按 `OrgContext`；个人偏好本人。
8. 集成与互操作：渠道对接短信/院内消息（[INTEG-01](../D2/INTEG-01.md)）。
9. 运维 / SRE / 国产化：内网慢场景骨架。
10. 质量与真实性审计：★无前端假保存、安全通知不可静默；无演示路由（[INFRA-09](../D1/INFRA-09.md)）。
11. AI / 模型治理与可降级：N·A（确定性页面）。

## 适用不变量
- 命中核心约束：**铁律 #1 真实性** · **核心 §13 低打扰** · **§9 多租户作用域**。
- 本卡落点：把通知设置页变为接真实渠道/订阅/免打扰、安全通知不可静默的设置页。

## 验收 + 验证
- [ ] AC-1（FR-1/2）：渠道/订阅真实保存；安全类不可全退订。
- [ ] AC-2（FR-3/4）：免打扰生效；安全通知不被静默；系统/个人级正确。
- [ ] AC-3（FR-5）：六态齐全；按作用域。
- 关联 A1–A9 剧本：A6 通知设置。
- T-GATE：前端真实性门禁全绿（no-page-mock、无假保存）。
- B0 验收：N·A（确定性页面）。

## 完工证据
- 代码 permalink：`pages/compliance/NotificationSettings` 真实化 + 接通知/配置 API + 六态。
- 测试：渠道/订阅/免打扰/安全不静默 + 六态 + RBAC + no-page-mock 门禁。
- 审计员签字：@<reviewer>（owner ≠ reviewer）。
