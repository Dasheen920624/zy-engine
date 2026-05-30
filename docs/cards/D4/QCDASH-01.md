# QCDASH-01 · 院级质控驾驶舱页

> 读卡前置：[核心 CONSTITUTION](../../CONSTITUTION.md) + [D4 域简报](_brief.md) + [体验契约](../../EXPERIENCE_CONTRACT.md)。
> 迁移来源（覆盖矩阵锚点）：详规 §3 S11 智能评估与整改 · 详规 院级质控总览 · 体验规范 §3 角色体验标准。
> 实化映射：占位 `D4-PAGE-院级质控驾驶舱` → 本卡 **QCDASH-01**。

## 身份
- 卡 ID：QCDASH-01（页面卡；= backlog `D4-PAGE-院级质控驾驶舱` 实化）
- 域：D4 质控改进
- 关联场景：S11 智能评估与整改
- 依赖卡：[SVC-QUALITY-01](SVC-QUALITY-01.md)（驾驶舱后端）· [OPT-08](OPT-08.md)（价值口径）· [BASE-08](../D0/BASE-08.md)/[BASE-10](../D0/BASE-10.md) · [INFRA-09](../D1/INFRA-09.md)
- 工作量：3d
- owner / reviewer：待派单（owner ≠ reviewer）

## 目标
把院级质控驾驶舱页**真实化**：院级指标、风险热力、价值/ROI、问题分布**可逐级下钻**到科室/病例/证据，全部接 [SVC-QUALITY-01](SVC-QUALITY-01.md)，**不前端造数、不假指标**。

## 现状（搬迁时核查 2026-05-30，以 `frontend/src` 为准）
页面**已存在待真实化**：`pages/quality/QcDashboard.tsx`（路由 `/qc/dashboard` 已注册 `app/router.tsx`）。本卡＝去占位/mock + 接驾驶舱聚合/下钻 API + 六态/五维 RBAC/可下钻齐全。

## 功能要求（原子可测条目）
- [ ] FR-1 院级总览：指标/热力/价值/问题分布真实聚合（[SVC-QUALITY-01](SVC-QUALITY-01.md)）。
- [ ] FR-2 逐级下钻：院 → 科 → 病例 → 证据，每层真实可追溯。
- [ ] FR-3 价值/ROI：展示 [OPT-08](OPT-08.md) 口径价值指标，缺数据标 `NOT_AVAILABLE`。
- [ ] FR-4 六态：加载/空/错误/无权限/部分成功/正常齐全（[BASE-08](../D0/BASE-08.md)）。
- [ ] FR-5 五维 RBAC：院领导/质控办可见全院；科主任限本科；数据按 `OrgContext`。

## 接口契约 / 页面契约
### 接口契约（引擎/API 卡）
N·A —— 消费 [SVC-QUALITY-01](SVC-QUALITY-01.md)/[OPT-08](OPT-08.md) 聚合 API。
### 页面契约（页面卡）
- 路由元数据：sectionKey `quality` / menuKey `qc-dashboard` / menuLabel `院级质控驾驶舱` / path `/qc/dashboard` / requiredPermissions 质控总览 / requiredRoles 院领导·质控办·科主任（本科）。
- 结构：PageShell（[BASE-08](../D0/BASE-08.md)）+ 指标卡 + 风险热力 + 价值面板 + 下钻抽屉 + 六态。
- 主按钮 ≤1（导出证据）/ 默认筛选 ≤3（本月/本科/高风险）/ 默认角色视图（按角色范围）。
- 五维 RBAC：菜单 / 动作（导出）/ 数据（org 层级）/ 资产 / 环境。
- 样式：仅引用 [BASE-10](../D0/BASE-10.md) token + [体验契约](../../EXPERIENCE_CONTRACT.md)；禁硬编码 hex/px（热力色阶用 token）。

## 数据与迁移
N·A —— 页面卡不落库；消费 [SVC-QUALITY-01](SVC-QUALITY-01.md) 后端。

## 视角清单（11 视角逐条）
1. 产品架构：院级质控的"驾驶舱"页。
2. 产品体验：★可下钻、热力直观、价值清晰；老年/国产浏览器可读。
3. 系统与数据架构：下钻 P95 ≤1.5s；大数据量预聚合。
4. 临床医疗安全：风险热力真实反映安全质控、不掩盖。
5. 知识与数据治理：下钻到证据可追溯版本。
6. 安全合规与监管：证据导出留审计（[BASE-04](../D0/BASE-04.md)）。
7. 集团化与多租户治理：★集团/院/科逐级下钻按作用域。
8. 集成与互操作：N·A（页面）。
9. 运维 / SRE / 国产化：内网慢场景骨架。
10. 质量与真实性审计：★无前端造数、热力来自真实命中率、缺数据标记；无演示路由（[INFRA-09](../D1/INFRA-09.md) no-page-mock）。
11. AI / 模型治理与可降级：N·A（确定性聚合页）。

## 适用不变量
- 命中核心约束：**铁律 #1 真实性** · **§2 菜单 IA** · **§9 多租户下钻** · **依赖 [SVC-QUALITY-01](SVC-QUALITY-01.md)**。
- 本卡落点：把院级质控驾驶舱从占位页变为接真实聚合、可逐级下钻、价值真实的驾驶舱。

## 验收 + 验证
- [ ] AC-1（FR-1/2）：院级聚合真实；逐级下钻到证据。
- [ ] AC-2（FR-3）：价值指标来自 [OPT-08](OPT-08.md)；缺数据标记。
- [ ] AC-3（FR-4/5）：六态齐全；按角色作用域。
- 关联 A1–A9 剧本：A9 质控总览。
- T-GATE：前端真实性门禁全绿（no-page-mock、无造数、无假指标）。
- B0 验收：N·A（确定性页面）。

## 完工证据
- 代码 permalink：`pages/quality/QcDashboard` 真实化 + 接 [SVC-QUALITY-01](SVC-QUALITY-01.md) + 六态。
- 测试：下钻/热力/价值/缺数据 + 六态 + RBAC + no-page-mock 门禁。
- 审计员签字：@<reviewer>（owner ≠ reviewer）。
