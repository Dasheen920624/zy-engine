/**
 * ESLint 规则：禁止 JSX 内联 style={{}}（v0.3-final 风格统一）。
 *
 * 上下文：v0.3-final 收口（docs/v0.3-DEMO-REDESIGN.md §4.3 / docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md）
 * 实测前端 582 处 `style={{ ... }}` 跨 64 个文件，是 UI 风格杂乱的核心来源。
 * 新代码必须把静态样式放进同名 `.module.css`（vite 默认支持 CSS Modules）。
 *
 * 触发位置：JSX 属性 style={...}
 * 错误等级：warn（存量太多，先 warn 让团队渐进式抽取；CI 跑 inline 数量统计监控收口）
 *
 * 豁免：
 *   1) 仅当 style 值是变量引用且名字含 dynamic / motion / transform，可以行尾 `// eslint-disable-next-line medkernel/no-inline-style`
 *   2) 测试 / mock 文件免（eslint.config.js overrides 已配）
 *   3) embed/ 嵌入器（受集成方约束，必要时允许内联）
 *
 * 正确做法：
 *   // 错误
 *   <div style={{ padding: 16, marginBottom: 24 }}>
 *
 *   // 正确（同名 module）
 *   import styles from "./MyPage.module.css";
 *   <div className={styles.container}>
 *
 *   // 正确（必须动态）
 *   // eslint-disable-next-line medkernel/no-inline-style
 *   <div style={{ transform: `rotate(${deg}deg)` }}>
 */

export default {
  meta: {
    type: 'suggestion',
    docs: {
      description: '禁止 JSX 内联 style={{}}，鼓励使用 CSS Modules / tokens',
      recommended: true,
    },
    messages: {
      noInlineStyle:
        'JSX 内联 style={{}} 被禁止。' +
        '请把静态样式抽取到同名 `.module.css`（CSS Modules）或使用 `var(--mk-*)` 类名。' +
        '若必须动态（transform / motion），请添加 `// eslint-disable-next-line medkernel/no-inline-style` 并说明理由。' +
        '详见 docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md §UI 统一风格。',
    },
    schema: [],
  },

  create(context) {
    return {
      JSXAttribute(node) {
        if (node.name && node.name.name === 'style' && node.value) {
          // 仅当值是对象字面量时报错（{{ ... }}）
          if (
            node.value.type === 'JSXExpressionContainer' &&
            node.value.expression &&
            node.value.expression.type === 'ObjectExpression'
          ) {
            context.report({
              node,
              messageId: 'noInlineStyle',
            });
          }
        }
      },
    };
  },
};
