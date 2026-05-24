# docs/design-system/ — 设计系统

> v1.0 GA 阶段：本目录承载「设计 Token / 13 组件 API / 7 全局模式 / 六态 / 中文文案 / i18n / A11Y / 性能基线」。
> 与 [frontend/src/shared/](../../frontend/src/shared/) 一一对应：
> - `tokens.md` ↔ [theme.ts](../../frontend/src/shared/config/theme.ts)
> - `components.md` ↔ [shared/ui/](../../frontend/src/shared/ui/)
> - `patterns.md` ↔ [shared/lib/](../../frontend/src/shared/lib/)
>
> 任何前端组件 / 页面 PR 必须遵守本目录规范，否则 PR 拒。

## 文件清单

| 文件 | 内容 | 阶段 |
|---|---|---|
| [components-checklist.md](components-checklist.md) | 13 组件清单 + 当前实装 / 未装状态 + 关联 GA-UI-* 任务 | R0 ✅ |
| tokens.md | 颜色 / 字号 / 间距 / 圆角 / 动效 / zIndex 6 套 token 矩阵 + 三密度模式 | R1 待填 |
| components.md | 13 组件完整 API（TypeScript 签名 + props 表 + 示例 + 关联文件位置） | R1 待填 |
| patterns.md | 7 全局模式（列表+详情/看板钻取/嵌入+抽屉/向导/双模式/审核流/时间轴）+ 六态强制 + 中文文案 + i18n + A11Y + 性能基线 | R1 待填 |

## 当前 frontend 实装

- `shared/config/theme.ts`：8 token（主色 #1565c0 + 6 状态色 + 1 字号），覆盖率约 30%
- `shared/ui/`：4 组件（PageShell / StatusBadge / StepFlow / ColumnManager），覆盖率 4/13
- 详见 [components-checklist.md](components-checklist.md)

## 历史源

完整 V2 时代版本：[docs/archive/v0.3/03_设计系统.md](../archive/v0.3/03_设计系统.md)（883 行，仅供查阅）

V2 三产品密度模式（A 信息密 / B 极简 / C 数据密）在 v1.0 GA 仍然适用，但落地方式从"独立 CSS class"改为"antd ConfigProvider componentSize + tokens".
