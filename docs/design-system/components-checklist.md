# 13 组件清单与实装状态

> R0 产物：把 [docs/archive/v0.3/03_设计系统.md §4](../archive/v0.3/03_设计系统.md) 的 13 个核心组件清单与当前 `frontend/src/shared/ui/` 实装情况对齐，便于 AI 协同时知道「哪些已实装」「哪些待 GA-UI-* 装」。
> 完整 API（props 表 + 示例）见 `components.md`（R1 阶段填充）。

## 总览

| # | 组件 | 用途 | 当前实装 | 关联文件 | 实装任务 |
|---|---|---|---|---|---|
| C01 | `<StatusBadge>` | 统一状态标签（图标 + 文字 + 颜色三重）| ✅ 已装 | [shared/ui/StatusBadge.tsx](../../frontend/src/shared/ui/StatusBadge.tsx) | — |
| C02 | `<SourceInfo>` | 来源信息条（指南/文献/规则版本）| ❌ 未装 | — | `GA-UI-01` |
| C03 | `<AiBadge>` | AI 候选标识（🤖 含置信度 + 来源）| ❌ 未装 | — | `GA-UI-02`（[CONSTITUTION §不变量 9](../CONSTITUTION.md) 硬约束） |
| C04 | `<OrgContextSelector>` | 组织上下文选择器（租户 × 院区 × 科室）| ❌ 未装 | — | `GA-UI-03` |
| C05 | `<TracedCard>` | 带 traceId 卡片（任意业务卡片基类）| ❌ 未装 | — | `GA-UI-04` |
| C06 | `<DangerConfirm>` | 三级危险操作确认（删除/回滚/全量发布）| ❌ 未装 | — | `GA-UI-05` |
| C07 | `<StepWizard>` | 多步向导（带草稿保存）| ⚠️ 部分（仅 [StepFlow.tsx](../../frontend/src/shared/ui/StepFlow.tsx) 视觉版，无草稿）| `GA-UI-06`（扩展为 StepWizard）|
| C08 | `<EmptyState>` | 空状态（icon + title + action）| ❌ 未装（antd `<Empty>` 可暂代）| `GA-UI-07`（六态强制中必装）|
| C09 | `<ErrorState>` | 错误状态（含 retry + 错误码）| ❌ 未装 | — | `GA-UI-08`（与 C08 同 PR）|
| C10 | `<AuditTrail>` | 审计时间轴 | ❌ 未装 | — | `GA-EXT-12` 审计相关任务 |
| C11 | `<RuleDslEditor>` | 规则 DSL 双模式编辑器 | ❌ 未装 | — | `GA-QUALITY-01` 域内实装 |
| C12 | `<PathwayCanvas>` | 路径画布（X6 封装）| ❌ 未装（X6 已从 package.json 删除，需重加）| `GA-CLINICAL-01` 域内实装 |
| C13 | `<EmbeddedAlert>` | 嵌入式预警条（B 临床嵌入器）| ❌ 未装 | — | `GA-UI-09` |
| — | `<PageShell>` | 通用页骨架（v1.0 GA 新增）| ✅ 已装 | [shared/ui/PageShell.tsx](../../frontend/src/shared/ui/PageShell.tsx) | — |
| — | `<StepFlow>` | 7 步流程视觉条 | ✅ 已装 | [shared/ui/StepFlow.tsx](../../frontend/src/shared/ui/StepFlow.tsx) | — |
| — | `<ColumnManager>` | 列管理 + 视图保存 | ✅ 已装 | [shared/ui/ColumnManager.tsx](../../frontend/src/shared/ui/ColumnManager.tsx) | — |

## 统计

- v0.3 13 组件实装率：**1/13 = 7.7%**（仅 StatusBadge），扩展 C07 算 0.5 计 1.5/13 = 11.5%
- v1.0 GA 自有 3 个：PageShell / StepFlow / ColumnManager（不在 v0.3 13 组件清单内）
- **9 个需要 GA-UI-01~09 拆单实装**（见 [backlog.md §Phase-2.5](../backlog.md)）

## 优先级

按 CONSTITUTION 硬约束推 P0 / P1 / P2：

| 优先级 | 组件 | 触发硬约束 |
|---|---|---|
| **P0** | C03 `<AiBadge>` | §1.9 AI 内容必须标识 |
| **P0** | C08 `<EmptyState>` + C09 `<ErrorState>` | §不变量 22 六态强制 |
| **P0** | C04 `<OrgContextSelector>` | §不变量 21 组织上下文必传 |
| **P0** | C02 `<SourceInfo>` | §不变量 8 来源必须 |
| **P1** | C05 `<TracedCard>` | §不变量 12 trace_id 全链路 |
| **P1** | C06 `<DangerConfirm>` | §1.6 默认 1 主按钮 + §4 7 步流 |
| **P1** | C07 `<StepWizard>` | §4 7 步流 |
| **P2** | C10 `<AuditTrail>` | §不变量 11 写操作必审计 |
| **P2** | C13 `<EmbeddedAlert>` | B 临床嵌入器 4 个 EM 用 |

C11 / C12 归各业务域 PR，不单拆。

## R1 阶段产物

`components.md` 完整 API（每组件 TypeScript 签名 + props 表 + 4 个使用示例 + 关联代码位置）。

## 历史源

完整 V2 API：[docs/archive/v0.3/03_设计系统.md §4](../archive/v0.3/03_设计系统.md)（行 285-676）
