/**
 * ESLint 规则：禁止 src/pages 与 src/features 中出现页面级 mock / 硬编码数据数组常量。
 *
 * 上下文：GA-ENG-BASE-09 净化把所有内联 MOCK / DEPTS / ITEMS / LINKS / PROVIDERS 等
 * 数组常量从业务页面与功能组件中清出；新代码必须改走 API hook，缺失时由
 * <PageState state="disabled" + RoadmapLink> 走六态占位。
 *
 * 触发位置：variable declarator
 *   形如 `const MOCK = [{ ... }]`、`const DEPTS = [...]` 这种顶层、SHOUTY-CASE
 *   且初始化为含对象元素的数组。
 *
 * 错误等级：error（与 medkernel/no-page-mock 一起锁住视觉债与假闭环不回潮）
 *
 * 范围：
 *   仅作用于 `src/pages/**` 与 `src/features/**` 下的 .ts / .tsx 文件。
 *   测试文件、shared/ui、shared/api、widgets 等不在控制范围内。
 */

const APPLICABLE_PATH = /\/(pages|features)\/.+\.(tsx|ts)$/;
const NAME_PATTERN = /^[A-Z][A-Z0-9_]*$/;

/** @type {import('eslint').Rule.RuleModule} */
export default {
  meta: {
    type: 'problem',
    docs: {
      description:
        '禁止 src/pages 与 src/features 中出现页面级 mock / 硬编码数据数组常量；改走 API hook + PageState 六态',
      recommended: true,
    },
    messages: {
      noPageMock:
        '业务页 / 功能组件禁止内联 mock 或硬编码数据数组常量（如 MOCK/DEPTS/ITEMS/LINKS/PROVIDERS）。' +
        '请改走 API hook，缺失时使用 <PageState state="disabled" + RoadmapLink> 走六态占位（GA-ENG-BASE-09）。',
    },
    schema: [],
  },

  create(context) {
    const filename = context.filename ?? context.getFilename?.() ?? '';
    if (!APPLICABLE_PATH.test(filename)) {
      return {};
    }

    return {
      VariableDeclarator(node) {
        if (node.id.type !== 'Identifier') return;
        if (!NAME_PATTERN.test(node.id.name)) return;
        if (!node.init || node.init.type !== 'ArrayExpression') return;
        if (node.init.elements.length === 0) return;
        const first = node.init.elements[0];
        if (!first || first.type !== 'ObjectExpression') return;
        context.report({ node, messageId: 'noPageMock' });
      },
    };
  },
};
