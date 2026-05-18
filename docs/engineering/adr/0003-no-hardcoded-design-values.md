# ADR-0003: 禁止硬编码颜色 / 字号 / 间距（必须用 Design Token）

- 状态：Accepted
- 日期：2026-05-18
- 决策者：前端 + 设计
- 涉及范围：前端

## 上下文

V2 [`03_设计系统.md`](../../03_设计系统.md) 定义了完整的 Design Tokens（颜色、字号、间距、动效）。但 AI 实现时常常：

- 顺手用 Ant Design 默认色（如蓝色按钮直接 `<Button type="primary">` 不改 ConfigProvider）
- 复制网上示例代码用 `#1890ff` 等硬编码颜色
- 用 `padding: 16px` 而不是 `padding: var(--mk-space-4)`
- 字号用 `font-size: 14px` 而不是 `var(--mk-text-base)`

后果：

- 视觉破碎（不同 AI 实现的页面色调不一致）
- 后续 token 调整困难（要扫描全代码改 hex 值）
- 三产品密度模式失效（密度依赖 token 切换）

## 决策

**所有视觉值必须使用 Design Token，不允许硬编码。**

- 颜色：必须用 `var(--mk-*)` token
- 字号：必须用 `var(--mk-text-*)`
- 间距：必须用 `var(--mk-space-*)`（4px 基准）
- 圆角：必须用 `var(--mk-radius-*)`
- 阴影：必须用 `var(--mk-shadow-*)`
- 动效：必须用 `var(--mk-duration-*)` + `var(--mk-ease-*)`
- z-index：必须用 `var(--mk-z-*)`

唯一例外：`frontend/src/styles/tokens.css` 是 token 定义文件，可以含 hex 值。

## 不变量

- I-1：除 `tokens.css` 外，任何 `.css` `.scss` `.tsx` `.ts` 文件**不允许**含 `#[0-9a-fA-F]{3,6}` `rgb(` `rgba(` `hsl(` 颜色值
- I-2：不允许直接用 px 写颜色相关属性的内联样式（如 `style={{ color: '#1890ff' }}`）
- I-3：Ant Design `<ConfigProvider>` 必须用 token 覆盖默认色

## 替代方案及拒绝原因

- **允许在 inline style 用 hex** → 拒绝：到处散落，统一改时遗漏
- **只规范主色不规范辅助色** → 拒绝：辅助色（如 disabled、border）也影响整体一致性
- **靠 code review 把关** → 拒绝：review 漏检率高，必须 ESLint 自动化

## 影响

正面：
- 视觉一致性 100% 保证
- Token 调整一处即可全局生效
- 三产品密度模式自动生效（`data-product` 切换）

负面：
- AI 第一次实现时需要查 token 表（PR-V2-01 完成后有完整 token）
- 写组件时多一步"找 token"

## 强制方式

- **ESLint 规则 `no-hardcoded-color.js`**：扫描所有非 token 文件的颜色值
- **Pre-commit hook**：lint-staged 自动跑
- **CI 检查**：PR 合入前必须 lint pass
- **verify-pr.ps1**：额外 grep 检测 inline style 的硬编码

## 相关参考

- 设计系统：[`docs/03_设计系统.md §2`](../../03_设计系统.md#2-design-tokens)
- 禁用清单：[`forbidden-patterns.md §1`](../forbidden-patterns.md)
- ESLint 规则：[`frontend/eslint-rules/no-hardcoded-color.js`](../../../frontend/eslint-rules/no-hardcoded-color.js)
- 相关 PR：PR-V2-01 设计 Token 落地
