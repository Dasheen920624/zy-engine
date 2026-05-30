# USERS-01 · 用户管理页

> 读卡前置：[核心 CONSTITUTION](../../CONSTITUTION.md) + [D5 域简报](_brief.md) + [体验契约](../../EXPERIENCE_CONTRACT.md)。
> 迁移来源（覆盖矩阵锚点）：详规 §3 S14 用户、权限与合规 · 体验规范 §3 角色体验标准。
> 实化映射：占位 `D5-PAGE-用户管理` → 本卡 **USERS-01**。

## 身份
- 卡 ID：USERS-01（页面卡；= backlog `D5-PAGE-用户管理` 实化）
- 域：D5 合规运维
- 关联场景：S14 用户、权限与合规
- 依赖卡：[SVC-COMPLIANCE-01](SVC-COMPLIANCE-01.md)（身份后端）· [BASE-02](../D0/BASE-02.md)/[INFRA-05](../D0/INFRA-05.md)（权限）· [BASE-08](../D0/BASE-08.md)/[BASE-10](../D0/BASE-10.md) · [API-13](../D0/API-13.md) · [INFRA-09](../D1/INFRA-09.md)
- 工作量：3d
- owner / reviewer：待派单（owner ≠ reviewer）

## 目标
把用户管理页**真实化**：管理员管用户/角色/权限/状态，按租户隔离、五维权限粒度，全部接 [SVC-COMPLIANCE-01](SVC-COMPLIANCE-01.md)，**不前端造用户、不兜底放行**。

## 现状（搬迁时核查 2026-05-30，以 `frontend/src` 为准）
页面**已存在待真实化**：`pages/compliance/AdminUsers.tsx`（路由 `/admin/users` 已注册 `app/router.tsx`，有 `AdminUsers.test.tsx`）。本卡＝去占位/mock + 接真实用户/角色/权限 API（[SVC-COMPLIANCE-01](SVC-COMPLIANCE-01.md)）+ 六态/五维 RBAC 齐全。

## 功能要求（原子可测条目）
- [ ] FR-1 用户列表：列用户/角色/状态（[API-13](../D0/API-13.md) 分页），按租户隔离、真实。
- [ ] FR-2 增改禁用：建/改/禁用用户、分配角色（五维 [INFRA-05](../D0/INFRA-05.md)），操作留审计。
- [ ] FR-3 权限可视：用户有效权限可查（[BASE-02](../D0/BASE-02.md)），不前端臆测。
- [ ] FR-4 六态：加载/空/错误/无权限/部分成功/正常齐全（[BASE-08](../D0/BASE-08.md)）。
- [ ] FR-5 五维 RBAC：仅平台/医院管理员可管；下级不可放大权限；数据按 `OrgContext`。

## 接口契约 / 页面契约
### 接口契约（引擎/API 卡）
N·A —— 消费 [SVC-COMPLIANCE-01](SVC-COMPLIANCE-01.md) 用户/权限 API。
### 页面契约（页面卡）
- 路由元数据：sectionKey `compliance` / menuKey `admin-users` / menuLabel `用户管理` / path `/admin/users` / requiredPermissions 用户管理 / requiredRoles 平台·医院管理员。
- 结构：PageShell（[BASE-08](../D0/BASE-08.md)）+ 用户列表 + 角色/权限编辑抽屉 + 六态。
- 主按钮 ≤1（新建用户）/ 默认筛选 ≤3（在职/角色/院区）/ 默认角色视图（管理员）。
- 五维 RBAC：菜单 / 动作（增改禁用/授权）/ 数据（org）/ 资产 / 环境。
- 样式：仅引用 [BASE-10](../D0/BASE-10.md) token + [体验契约](../../EXPERIENCE_CONTRACT.md)；禁硬编码 hex/px。

## 数据与迁移
N·A —— 页面卡不落库；消费 [SVC-COMPLIANCE-01](SVC-COMPLIANCE-01.md) 后端。

## 视角清单（11 视角逐条）
1. 产品架构：身份治理的"用户管理"页。
2. 产品体验：列表/授权清晰；国产浏览器/老年模式可读。
3. 系统与数据架构：用户 10万级分页；P95 ≤1s。
4. 临床医疗安全：N·A（身份管理）。
5. 知识与数据治理：N·A。
6. 安全合规与监管：增改禁用/授权留审计（[BASE-04](../D0/BASE-04.md)）。
7. 集团化与多租户治理：★租户隔离、下级不可放大权限。
8. 集成与互操作：身份可来自外部（[IDBIND-01](IDBIND-01.md)）。
9. 运维 / SRE / 国产化：内网慢场景骨架。
10. 质量与真实性审计：★无前端造用户、不兜底放行、权限真实；无演示路由（[INFRA-09](../D1/INFRA-09.md)）。
11. AI / 模型治理与可降级：N·A。

## 适用不变量
- 命中核心约束：**铁律 #1 真实性** · **§2 菜单 IA** · **§9 多租户隔离** · **依赖 [SVC-COMPLIANCE-01](SVC-COMPLIANCE-01.md)**。
- 本卡落点：把用户管理页变为接真实用户/权限、按租户隔离、不兜底放行的管理页。

## 验收 + 验证
- [ ] AC-1（FR-1/2）：用户列表/增改真实、留审计。
- [ ] AC-2（FR-3）：有效权限可查、不臆测。
- [ ] AC-3（FR-4/5）：六态齐全；越权不可管、不可放大。
- 关联 A1–A9 剧本：A1 用户管理。
- T-GATE：前端真实性门禁全绿（no-page-mock、无造用户、无兜底放行）。
- B0 验收：N·A（确定性页面）。

## 完工证据
- 代码 permalink：`pages/compliance/AdminUsers` 真实化 + 接 [SVC-COMPLIANCE-01](SVC-COMPLIANCE-01.md) + 六态。
- 测试：用户/角色/权限/隔离 + 六态 + RBAC + no-page-mock 门禁（含 `AdminUsers.test.tsx`）。
- 审计员签字：@<reviewer>（owner ≠ reviewer）。
