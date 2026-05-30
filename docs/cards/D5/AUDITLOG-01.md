# AUDITLOG-01 · 审计日志页

> 读卡前置：[核心 CONSTITUTION](../../CONSTITUTION.md) + [D5 域简报](_brief.md) + [体验契约](../../EXPERIENCE_CONTRACT.md)。
> 迁移来源（覆盖矩阵锚点）：详规 §3 S14 用户、权限与合规 · 详规 审计可查 · 体验规范 §3。
> 实化映射：占位 `D5-PAGE-审计日志` → 本卡 **AUDITLOG-01**。

## 身份
- 卡 ID：AUDITLOG-01（页面卡；= backlog `D5-PAGE-审计日志` 实化）
- 域：D5 合规运维
- 关联场景：S14 用户、权限与合规
- 依赖卡：[SVC-COMPLIANCE-02](SVC-COMPLIANCE-02.md)（审计后端）· [EVID-01](EVID-01.md)（证据）· [SYS-06](SYS-06.md)（导出审批）· [BASE-08](../D0/BASE-08.md)/[BASE-10](../D0/BASE-10.md) · [API-13](../D0/API-13.md) · [INFRA-09](../D1/INFRA-09.md)
- 工作量：3d
- owner / reviewer：待派单（owner ≠ reviewer）

## 目标
把审计日志页**真实化**：按条件查审计事件、查看详情、导出（带证据签名 + 审批），全部接 [SVC-COMPLIANCE-02](SVC-COMPLIANCE-02.md)，**不前端造日志、不漏关键操作**。

## 现状（搬迁时核查 2026-05-30，以 `frontend/src` 为准）
页面**已存在待真实化**：`pages/compliance/AdminAudit.tsx`（路由 `/admin/audit` 已注册 `app/router.tsx`）。本卡＝去占位/mock + 接审计查询/导出 API（[SVC-COMPLIANCE-02](SVC-COMPLIANCE-02.md) `AuditEvent`）+ 六态/五维 RBAC 齐全。

## 功能要求（原子可测条目）
- [ ] FR-1 审计查询：按时间/用户/操作/对象查审计事件（[API-13](../D0/API-13.md) 分页），真实。
- [ ] FR-2 详情：审计事件详情含操作者/对象/前后值/traceId。
- [ ] FR-3 导出：导出审计 + 证据签名（[EVID-01](EVID-01.md)），走导出审批（[SYS-06](SYS-06.md)）。
- [ ] FR-4 六态：加载/空/错误/无权限/部分成功/正常齐全（[BASE-08](../D0/BASE-08.md)）。
- [ ] FR-5 五维 RBAC：审计员/合规只读可查可导；数据按 `OrgContext` 隔离。

## 接口契约 / 页面契约
### 接口契约（引擎/API 卡）
N·A —— 消费 [SVC-COMPLIANCE-02](SVC-COMPLIANCE-02.md) 审计 API。
### 页面契约（页面卡）
- 路由元数据：sectionKey `compliance` / menuKey `admin-audit` / menuLabel `审计日志` / path `/admin/audit` / requiredPermissions 审计查询 / requiredRoles 审计员·合规·管理员。
- 结构：PageShell（[BASE-08](../D0/BASE-08.md)）+ 审计列表 + 详情抽屉（前后值/traceId）+ 导出 + 六态。
- 主按钮 ≤1（导出）/ 默认筛选 ≤3（今日/操作类型/用户）/ 默认角色视图（审计员）。
- 五维 RBAC：菜单 / 动作（导出申请）/ 数据（org）/ 资产 / 环境。
- 样式：仅引用 [BASE-10](../D0/BASE-10.md) token + [体验契约](../../EXPERIENCE_CONTRACT.md)；禁硬编码 hex/px。

## 数据与迁移
N·A —— 页面卡不落库；消费 [SVC-COMPLIANCE-02](SVC-COMPLIANCE-02.md) 后端。

## 视角清单（11 视角逐条）
1. 产品架构：合规留痕的"审计查询"页。
2. 产品体验：检索快、详情清晰；国产浏览器可读。
3. 系统与数据架构：审计大表检索分页 P95 ≤1s。
4. 临床医疗安全：N·A（审计）。
5. 知识与数据治理：审计可追溯 traceId。
6. 安全合规与监管：★审计只读不可改、导出带证据签名 + 审批。
7. 集团化与多租户治理：审计按租户隔离查询。
8. 集成与互操作：导出标准格式供监管。
9. 运维 / SRE / 国产化：内网慢场景骨架。
10. 质量与真实性审计：★无前端造日志、关键操作不漏、导出可验签；无演示路由（[INFRA-09](../D1/INFRA-09.md)）。
11. AI / 模型治理与可降级：N·A。

## 适用不变量
- 命中核心约束：**铁律 #1 真实性** · **核心 §6 审计** · **§9 隔离** · **依赖 [SVC-COMPLIANCE-02](SVC-COMPLIANCE-02.md)**。
- 本卡落点：把审计日志页变为接真实审计、详情可追溯、导出带证据签名的查询页。

## 验收 + 验证
- [ ] AC-1（FR-1/2）：审计查询/详情真实、含 traceId。
- [ ] AC-2（FR-3）：导出带证据签名 + 审批。
- [ ] AC-3（FR-4/5）：六态齐全；只读隔离。
- 关联 A1–A9 剧本：A9 审计查询。
- T-GATE：前端真实性门禁全绿（no-page-mock、无造日志）。
- B0 验收：N·A（确定性页面）。

## 完工证据
- 代码 permalink：`pages/compliance/AdminAudit` 真实化 + 接 [SVC-COMPLIANCE-02](SVC-COMPLIANCE-02.md) + 六态。
- 测试：查询/详情/导出验签 + 六态 + RBAC + no-page-mock 门禁。
- 审计员签字：@<reviewer>（owner ≠ reviewer）。
