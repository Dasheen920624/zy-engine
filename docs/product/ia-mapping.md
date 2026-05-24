# 旧 v0.3 ↔ 新 v1.0 GA IA 映射表

> **R0 核心产物** — 把 v0.3 三产品分层（A 配置工厂 / B 临床嵌入器 / C 质控驾驶舱）下的 18 客户页 + 4 嵌入组件，逐项映射到 v1.0 GA 5 菜单 IA 下的 31 客户业务页 + 4 嵌入组件 + 若干新增页。
>
> **用途**：任何 AI 实施页面前先查本表 — 确认「该页旧规格是否仍适用」「是否需要新写」「是否已废」，避免重复或缺失。
>
> **维护规则**：menu.ts / routes.ts 任何新增 / 删除页面的 PR 必须同步本表，否则 PR 拒。

---

## 1. 四态标记规则

| 标记 | 含义 | AI 处理 |
|---|---|---|
| ✅ | 已实装 MOCK，旧规格基本适用 | R4 直接按旧规格 12 字段化即可 |
| 🟡 | 需扩规格：旧有规格但与新 IA / 新组件库不一致 | R4 必须重写，注明差异 |
| 🆕 | v1.0 GA 新增页，旧 v0.3 无对应 | R4 从零写规格（用 _template.md） |
| 🚮 | 旧有但 v1.0 GA 已废 / 已合并 | 仅在 archive 查阅，不写新规格 |

---

## 2. 旧 18 页 + 4 嵌入 → 新映射

### 2.1 工作台（1 页）

| 旧 ID | 旧名 | 旧 URL | 新菜单 | 新 URL | 新文件 | 状态 |
|---|---|---|---|---|---|---|
| P01 | 工作台首页 | `/dashboard` | 工作台 | `/dashboard` | [Dashboard.tsx](../../frontend/src/pages/Dashboard.tsx) + [WorkbenchPanel.tsx](../../frontend/src/widgets/WorkbenchPanel.tsx) | ✅ |

> 旧 P01 含「我的待办 / 引擎健康 / 今日质控 / 快捷入口 / 最近活动」5 卡。新增 CONSTITUTION §6.4「租户生命周期面板」(6 阶段进度 + 本周建议动作 3 项)，R4 时合并。

### 2.2 试点准备（旧 0 页 → 新 7 页）

> ⚠️ v1.0 GA 把 v0.3 散在 A/C 产品里的「实施类」功能全部收口到「试点准备」一级菜单，是最大的 IA 重构区。

| 旧 ID | 旧名 | 新菜单 | 新 URL | 新文件 | 状态 |
|---|---|---|---|---|---|
| — | — | 试点准备 | `/onboarding/guide` | [tenant/ImplementationGuide.tsx](../../frontend/src/pages/tenant/ImplementationGuide.tsx) | 🆕 客户实施向导 |
| — | — | 试点准备 | `/tenant/onboarding` | [tenant/TenantOnboarding.tsx](../../frontend/src/pages/tenant/TenantOnboarding.tsx) | 🆕 租户开通 |
| P15 | 配置包列表 | 试点准备 | `/config/packages` | [tenant/ConfigPackages.tsx](../../frontend/src/pages/tenant/ConfigPackages.tsx) | ✅ |
| P16 | 配置包发布向导 | 试点准备 | `/config/packages/import` | (R4 待建) | 🟡 旧规格适用，但需补「7 步流」+ 灰度 |
| P02 | 路径模板列表 | 试点准备 | `/pathway/templates` | [tenant/PathwayTemplates.tsx](../../frontend/src/pages/tenant/PathwayTemplates.tsx) | ✅ |
| P03 | 路径模板编辑器 | 试点准备（专家模式入口）| `/pathway/templates/:code/edit` | (R4 待建) | 🟡 X6 已从 package.json 删，需重加 |
| P04 | 路径版本对比 | 试点准备（专家模式）| `/pathway/templates/:code/diff` | (R4 待建) | 🟡 |
| P06 | 规则库 | 试点准备 | `/rule/definitions` | [tenant/RuleDefinitions.tsx](../../frontend/src/pages/tenant/RuleDefinitions.tsx) | ✅ |
| P07 | 规则 DSL 编辑器 | 试点准备（专家模式）| `/rule/definitions/:code/edit` | (R4 待建) | 🟡 |
| P12 | 字典映射工作台 | 试点准备 | `/terminology/mapping` | [tenant/TerminologyMapping.tsx](../../frontend/src/pages/tenant/TerminologyMapping.tsx) | ✅ |
| — | — | 试点准备 | `/adapter/hub` | [tenant/AdapterHub.tsx](../../frontend/src/pages/tenant/AdapterHub.tsx) | 🆕 适配器中心（HIS/EMR/LIS 接入）|

### 2.3 临床运行（旧 1 页 → 新 6 页）

| 旧 ID | 旧名 | 新菜单 | 新 URL | 新文件 | 状态 |
|---|---|---|---|---|---|
| — | — | 临床运行 | `/mpi` | [clinical/Mpi.tsx](../../frontend/src/pages/clinical/Mpi.tsx) | 🆕 患者主索引 |
| P05 | 患者路径管理 | 临床运行 | `/pathway/patients` | [clinical/PatientPathways.tsx](../../frontend/src/pages/clinical/PatientPathways.tsx) | ✅ |
| — | — | 临床运行 | `/cdss/fatigue` | [clinical/CdssFatigue.tsx](../../frontend/src/pages/clinical/CdssFatigue.tsx) | 🆕 临床提醒治理 |
| P08 | 规则校验工作台 | 临床运行 | `/rule/validate` | [clinical/RuleValidate.tsx](../../frontend/src/pages/clinical/RuleValidate.tsx) | ✅ |
| — | — | 临床运行 | `/workflow/todos` | [clinical/WorkflowTodos.tsx](../../frontend/src/pages/clinical/WorkflowTodos.tsx) | 🆕 待办中心 |
| — | — | 临床运行 | `/notifications` | [clinical/Notifications.tsx](../../frontend/src/pages/clinical/Notifications.tsx) | 🆕 通知中心 |

### 2.4 质控改进（旧 4 页 → 新 6 页）

| 旧 ID | 旧名 | 新菜单 | 新 URL | 新文件 | 状态 |
|---|---|---|---|---|---|
| P10 | 院级质控驾驶舱 | 质控改进 | `/qc/dashboard` | [quality/QcDashboard.tsx](../../frontend/src/pages/quality/QcDashboard.tsx) | ✅ |
| P09 | 质控预警列表 | 质控改进 | `/qc/alerts` | [quality/QcAlerts.tsx](../../frontend/src/pages/quality/QcAlerts.tsx) | ✅ |
| P11 | 医保智能审核 | 质控改进 | `/qc/insurance` | [quality/InsuranceAudit.tsx](../../frontend/src/pages/quality/InsuranceAudit.tsx) | ✅ |
| — | — | 质控改进 | `/qc/eval/sets` | [quality/QcEvalSets.tsx](../../frontend/src/pages/quality/QcEvalSets.tsx) | 🆕 评估指标库 |
| — | — | 质控改进 | `/qc/eval/results` | [quality/QcEvalResults.tsx](../../frontend/src/pages/quality/QcEvalResults.tsx) | 🆕 评估结果 |
| P14 | 知识审核台 | 质控改进 | `/aik/review` | [quality/AiReview.tsx](../../frontend/src/pages/quality/AiReview.tsx) | ✅（CONSTITUTION §2.2 合并去重） |

### 2.5 合规运维（旧 2 页 → 新 6 页）

| 旧 ID | 旧名 | 新菜单 | 新 URL | 新文件 | 状态 |
|---|---|---|---|---|---|
| P17 | 用户管理 | 合规运维 | `/admin/users` | [compliance/AdminUsers.tsx](../../frontend/src/pages/compliance/AdminUsers.tsx) | ✅ |
| — | — | 合规运维 | `/security/identity-binding` | [compliance/IdentityBinding.tsx](../../frontend/src/pages/compliance/IdentityBinding.tsx) | 🆕 身份绑定（HIS 工号 ↔ 平台账号） |
| P18 | 审计日志 | 合规运维 | `/admin/audit` | [compliance/AdminAudit.tsx](../../frontend/src/pages/compliance/AdminAudit.tsx) | ✅ |
| — | — | 合规运维 | `/security/baseline` | [compliance/SecurityBaseline.tsx](../../frontend/src/pages/compliance/SecurityBaseline.tsx) | 🆕 安全基线（等保 2.0 自检） |
| — | — | 合规运维 | `/system/providers` | [compliance/SystemProviders.tsx](../../frontend/src/pages/compliance/SystemProviders.tsx) | 🆕 Provider 状态（含降级链）|
| — | — | 合规运维 | `/notifications/settings` | [compliance/NotificationSettings.tsx](../../frontend/src/pages/compliance/NotificationSettings.tsx) | 🆕 通知设置 |

### 2.6 高级工具（旧 1 页 + 1 部分 → 新 5 页）

> ⚠️ v1.0 GA 把 v0.3 散在主菜单的「专家功能」全部下放到隐藏式「高级工具 ⊕」，配合 CONSTITUTION §1.3「主路径 ≤ 5 菜单」+ §2.1 隐藏式入口。

| 旧 ID | 旧名 | 新菜单 | 新 URL | 新文件 | 状态 |
|---|---|---|---|---|---|
| — | (旧 SourceInfo 散落) | 高级工具 | `/advanced/provenance` | [advanced/Provenance.tsx](../../frontend/src/pages/advanced/Provenance.tsx) | 🆕 集中式来源追溯 |
| P13 | 图谱查询工作台 | 高级工具（下放专家区）| `/advanced/graph` | [advanced/GraphExplore.tsx](../../frontend/src/pages/advanced/GraphExplore.tsx) | ✅ 旧规格仍适用 |
| — | — | 高级工具 | `/advanced/ai-workflows` | [advanced/AiWorkflows.tsx](../../frontend/src/pages/advanced/AiWorkflows.tsx) | 🆕 AI 工作流编排 |
| — | — | 高级工具 | `/advanced/domestic` | [advanced/DomesticCheck.tsx](../../frontend/src/pages/advanced/DomesticCheck.tsx) | 🆕 国产化自检（GA-EXT-21）|
| — | — | 高级工具 | `/advanced/dev-console` | [advanced/DevConsole.tsx](../../frontend/src/pages/advanced/DevConsole.tsx) | 🆕 开发者控制台 |

### 2.7 嵌入组件（B 临床嵌入器，旧 4 ↔ 新 4）

> 旧版 `frontend/embed-build.config.ts + embed.html` 已迁到 `docs/archive/v0.3/legacy-embed/`。v1.0 GA 重建 B 端嵌入打包链路在 `GA-CORE-08` 内规划。

| 旧 ID | 旧名 | 新路径（v1.0 GA） | 状态 |
|---|---|---|---|
| EM01 | AMI 路径推荐嵌入条 | `specs/embed/EM01-ami-pathway.md` | ✅ 旧规格仍适用 |
| EM02 | 质控整改嵌入条 | `specs/embed/EM02-qc-rectify.md` | ✅ 旧规格仍适用 |
| EM03 | 医保合规嵌入条 | `specs/embed/EM03-insurance.md` | ✅ 旧规格仍适用 |
| EM04 | 医嘱安全拦截弹窗 | `specs/embed/EM04-order-intercept.md` | ✅ 旧规格仍适用 |

---

## 3. 产品分层 vs 菜单 IA 的映射规则

v0.3 用「三产品分层」（A/B/C）描述功能归属，v1.0 GA 改用「5 菜单 IA + 隐藏式高级」表达。两者**不一一对应**：

| v0.3 三产品 | 主要落到 v1.0 GA 哪个菜单 | 备注 |
|---|---|---|
| **A 配置工厂** | 试点准备（主）+ 高级工具（专家功能下放）| 配置类工作流走 7 步流，专家 DSL/X6 进 ⊕ |
| **B 临床嵌入器** | 嵌入组件 EM01-04（独立打包，不入主菜单）+ 临床运行 | B 端是 HIS/EMR 内嵌的浮条/弹窗，不算主应用页 |
| **C 质控驾驶舱** | 质控改进 | 几乎 1:1 |
| **共用底座** | 合规运维 + 工作台 | C7 来源追溯 / C8 适配器 / C9 身份组织 |

→ **AI 协同提示**：写代码时**不要再引用 "A 配置工厂 / B 临床嵌入器 / C 质控驾驶舱"** 这些 v0.3 时代命名。直接用 5 菜单一级名（工作台 / 试点准备 / 临床运行 / 质控改进 / 合规运维 / 高级工具）。

---

## 4. 统计

### 4.1 总览

| 维度 | v0.3 | v1.0 GA | 变化 |
|---|---|---|---|
| 客户业务页 | 18 | 31 | +13 🆕 |
| 嵌入组件 | 4 | 4 | 0 |
| 一级菜单 | "三产品分层"（A/B/C）| 5 菜单 + 1 隐藏（⊕）| IA 重构 |
| 主菜单出现技术功能 | 是（图谱 / 字典 等都在主菜单）| 否（下放 ⊕）| §1.3 硬约束 |

### 4.2 v1.0 GA 31 客户业务页四态分布

| 状态 | 数量 | 占比 |
|---|---|---|
| ✅ 已实装 MOCK + 旧规格适用 | 14 | 45% |
| 🟡 需扩规格（路径子页 / 专家模式）| 4 | 13% |
| 🆕 v1.0 GA 新增 | 13 | 42% |
| 🚮 v0.3 有但 v1.0 GA 废 | 0 | 0% |

→ **R4 工作量预估**：14 项 ✅ × 30 行迁移 + 4 项 🟡 × 80 行扩写 + 13 项 🆕 × 80 行新写 + 4 嵌入 × 50 行 = **~2000 行**。5 个域目录可并行（5 AI × 2 天）。

---

## 5. 关联文档

- [docs/CONSTITUTION.md §2 5 菜单 + 30 二级](../CONSTITUTION.md) — 菜单 IA 锁定
- [docs/specs/_template.md](../specs/_template.md) — R4 写规格用 12 字段模板
- [docs/design-system/components-checklist.md](../design-system/components-checklist.md) — 写规格时填「组件树」字段查这里
- [frontend/src/shared/config/menu.ts](../../frontend/src/shared/config/menu.ts) — 菜单代码权威
- [frontend/src/shared/config/routes.ts](../../frontend/src/shared/config/routes.ts) — 路由代码权威

---

**End of IA mapping.**
