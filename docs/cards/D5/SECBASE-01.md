# SECBASE-01 · 安全基线与系统配置页

> 读卡前置：[核心 CONSTITUTION](../../CONSTITUTION.md) + [D5 域简报](_brief.md) + [体验契约](../../EXPERIENCE_CONTRACT.md)。
> 迁移来源（覆盖矩阵锚点）：详规 §3 S14 用户、权限与合规 · 核心 §配置外置（#19）· 详规 系统配置中心。
> 实化映射：占位 `D5-PAGE-安全基线与系统配置` → 本卡 **SECBASE-01**（承载系统配置中心前台）。

## 身份
- 卡 ID：SECBASE-01（页面卡；= backlog `D5-PAGE-安全基线与系统配置` 实化）
- 域：D5 合规运维
- 关联场景：S14 用户、权限与合规
- 依赖卡：[CONFIG-01](../D0/CONFIG-01.md)（系统配置中心引擎，单一归属）· [SVC-COMPLIANCE-01](SVC-COMPLIANCE-01.md)（安全基线）· [BASE-08](../D0/BASE-08.md)/[BASE-10](../D0/BASE-10.md) · [INFRA-09](../D1/INFRA-09.md)
- 工作量：3d
- owner / reviewer：待派单（owner ≠ reviewer）

## 目标
把安全基线与系统配置页**真实化**：作为**系统配置中心前台**（[CONFIG-01](../D0/CONFIG-01.md)）——功能开关/认证/备份/国产化/Provider/日志级别 + 安全基线，**高危项护栏置灰不可关、配置外置不写死、不净增二级菜单**（27 槽不变）。

## 现状（搬迁时核查 2026-05-30，以 `frontend/src` 为准）
页面**已存在待真实化**：`pages/compliance/SecurityBaseline.tsx`（路由 `/security/baseline` 已注册 `app/router.tsx`）。本卡＝去占位/mock + 接配置中心（[CONFIG-01](../D0/CONFIG-01.md)）+ 安全基线（[SVC-COMPLIANCE-01](SVC-COMPLIANCE-01.md)）API + 六态/五维 RBAC 齐全；**配置中心挂本页槽、不另起二级菜单**。

## 功能要求（原子可测条目）
- [ ] FR-1 系统配置：功能开关/认证/备份/国产化/Provider/日志级别可配（[CONFIG-01](../D0/CONFIG-01.md)），配置外置不写死。
- [ ] FR-2 安全基线：口令策略/会话/IP 白名单等安全基线可配（[SVC-COMPLIANCE-01](SVC-COMPLIANCE-01.md)）。
- [ ] FR-3 高危护栏：高危项（如关安全红线/审计）**置灰不可关**，有护栏说明。
- [ ] FR-4 不净增菜单：配置中心挂本页，**二级菜单仍 27**、不净增。
- [ ] FR-5 六态 + 五维 RBAC：齐全；仅平台/安全管理员可改；变更留审计。

## 接口契约 / 页面契约
### 接口契约（引擎/API 卡）
N·A —— 消费 [CONFIG-01](../D0/CONFIG-01.md) 配置 + [SVC-COMPLIANCE-01](SVC-COMPLIANCE-01.md) 安全基线 API。
### 页面契约（页面卡）
- 路由元数据：sectionKey `compliance` / menuKey `security-baseline` / menuLabel `安全基线与系统配置` / path `/security/baseline` / requiredPermissions 系统配置 / requiredRoles 平台·安全管理员。
- 结构：PageShell（[BASE-08](../D0/BASE-08.md)）+ 分组配置面板（系统配置 / 安全基线）+ 高危置灰护栏 + 六态。
- 主按钮 ≤1（保存配置）/ 默认筛选 ≤3 / 默认角色视图（管理员）；**不净增二级菜单**。
- 五维 RBAC：菜单 / 动作（改配置）/ 数据（org）/ 资产 / 环境。
- 样式：仅引用 [BASE-10](../D0/BASE-10.md) token + [体验契约](../../EXPERIENCE_CONTRACT.md)；禁硬编码 hex/px。

## 数据与迁移
N·A —— 页面卡不落库；消费 [CONFIG-01](../D0/CONFIG-01.md) 后端。

## 视角清单（11 视角逐条）
1. 产品架构：系统配置中心 + 安全基线的统一前台（[CONFIG-01](../D0/CONFIG-01.md)）。
2. 产品体验：分组清晰、高危置灰有说明；国产浏览器可读。
3. 系统与数据架构：配置即时生效/灰度；P95 ≤1s。
4. 临床医疗安全：高危安全配置不可被静默关。
5. 知识与数据治理：配置版本化可追溯。
6. 安全合规与监管：★配置变更留审计（[BASE-04](../D0/BASE-04.md)）；高危护栏。
7. 集团化与多租户治理：配置按层级继承；下级不可关安全红线。
8. 集成与互操作：Provider/国产化配置联动（[PROVIDER-01](PROVIDER-01.md)）。
9. 运维 / SRE / 国产化：★国产化/备份/日志级别配置真实。
10. 质量与真实性审计：★配置外置不写死、高危不可关、**不净增二级菜单**；无演示路由（[INFRA-09](../D1/INFRA-09.md)）。
11. AI / 模型治理与可降级：Provider/模型开关配置；关闭诚实降级。

## 适用不变量
- 命中核心约束：**铁律 #19 配置外置 / #11** · **核心 §2 菜单 IA（27 槽不净增）** · **§9 继承** · **依赖 [CONFIG-01](../D0/CONFIG-01.md)**。
- 本卡落点：把安全基线页变为系统配置中心前台，高危护栏置灰、配置外置、不净增菜单。

## 验收 + 验证
- [ ] AC-1（FR-1/2）：系统配置/安全基线真实可配、外置不写死。
- [ ] AC-2（FR-3/4）：高危置灰不可关；二级菜单仍 27、不净增。
- [ ] AC-3（FR-5）：六态齐全；变更留审计。
- 关联 A1–A9 剧本：A1 系统配置。
- T-GATE：前端真实性门禁全绿（no-page-mock、配置外置、不净增菜单）。
- B0 验收：N·A（确定性配置页）。

## 完工证据
- 代码 permalink：`pages/compliance/SecurityBaseline` 真实化 + 接 [CONFIG-01](../D0/CONFIG-01.md) + 六态。
- 测试：配置/安全基线/高危护栏/不净增菜单 + 六态 + RBAC + no-page-mock 门禁。
- 审计员签字：@<reviewer>（owner ≠ reviewer）。
