/**
 * no-placeholder-page
 *
 * GA-UX-01: 禁止引入或使用 PlaceholderPage 组件。
 * v1.0 GA 要求所有客户可见路由指向真实页面，不允许占位页。
 * PlaceholderPage 组件已被删除，此规则防止未来重新引入。
 */

export default {
  meta: {
    type: "problem",
    docs: {
      description: "禁止引入或使用 PlaceholderPage 占位页组件（GA-UX-01）",
      category: "MedKernel",
      recommended: true,
    },
    messages: {
      noPlaceholderImport:
        "禁止引入 PlaceholderPage。v1.0 GA 要求所有路由指向真实页面组件，不允许占位页。请实现实际页面或使用 NotFound 组件。",
      noPlaceholderUsage:
        "禁止使用 PlaceholderPage 组件。v1.0 GA 要求所有路由指向真实页面组件，不允许占位页。请实现实际页面。",
    },
    schema: [],
  },

  create(context) {
    return {
      ImportDeclaration(node) {
        if (
          node.source &&
          typeof node.source.value === "string" &&
          node.source.value.includes("PlaceholderPage")
        ) {
          context.report({
            node,
            messageId: "noPlaceholderImport",
          });
        }
      },
      JSXIdentifier(node) {
        if (
          node.name === "PlaceholderPage" &&
          node.parent &&
          node.parent.type === "JSXOpeningElement"
        ) {
          context.report({
            node,
            messageId: "noPlaceholderUsage",
          });
        }
      },
    };
  },
};
