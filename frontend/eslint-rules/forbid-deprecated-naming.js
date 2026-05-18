/**
 * ESLint 规则：禁用 zy-engine 时代的命名（已彻底重命名为 MedKernel）。
 *
 * 强制 ADR-0002 + 项目重命名决策。
 *
 * 检测：
 *   - 标识符含 ZyEngine
 *   - 字符串含 'zy-engine' 'zyengine' '/zy-engine/' 'ZyEngine' 'ZY_HOME' 'ZYENGINE_'
 *   - CSS 变量 --zy-*（已全部改为 --mk-*）
 *   - 注释里的旧命名
 *
 * 例外：
 *   - 本规则文件本身（要含 zy 关键字用于检测）
 *
 * 错误示例：
 *   const apiBase = '/zy-engine/api';
 *   import { ZyEngineConfig } from './config';
 *   color: var(--zy-primary);
 *   // TODO: 旧 zyengine 兼容
 *
 * 正确示例：
 *   const apiBase = '/medkernel/api';
 *   import { MedKernelConfig } from './config';
 *   color: var(--mk-primary);
 */

const DEPRECATED_PATTERNS = [
  // 标识符 / 路径
  { pattern: /\bZyEngine\w*\b/, hint: '改用 MedKernel*' },
  { pattern: /\bzyengine\w*\b/i, hint: '改用 medkernel*' },
  { pattern: /\bzy-engine\b/, hint: '改用 medkernel' },
  { pattern: /\/zy-engine\//, hint: '改用 /medkernel/' },

  // 环境变量
  { pattern: /\bZY_HOME\b/, hint: '改用 MK_HOME' },
  { pattern: /\bZY_BACKUP_DIR\b/, hint: '改用 MK_BACKUP_DIR' },
  { pattern: /\bZYENGINE_[A-Z]/, hint: '改用 MEDKERNEL_*' },

  // CSS 变量 / 类名（已彻底替换为 mk-）
  { pattern: /--zy-[a-z]/, hint: '改用 --mk-*（CSS Token 已统一为 mk- 命名空间）' },
  { pattern: /\.zy-[a-z]/, hint: '改用 .mk-*（CSS 类名已统一为 mk- 前缀）' },
];

// 例外文件：本规则文件本身要含 zy 关键字（用于模式检测）
const EXEMPT_FILES = [
  'forbid-deprecated-naming.js',
  'forbid-deprecated-naming.test.js',
];

function isExempt(filename) {
  return EXEMPT_FILES.some(f => filename.endsWith(f));
}

function checkString(value, node, context) {
  if (typeof value !== 'string') return;
  for (const { pattern, hint } of DEPRECATED_PATTERNS) {
    const m = value.match(pattern);
    if (m) {
      context.report({
        node,
        messageId: 'deprecatedNaming',
        data: { match: m[0], hint },
      });
      return;
    }
  }
}

export default {
  meta: {
    type: 'problem',
    docs: {
      description: '禁用 zy-engine 时代的命名（已重命名为 MedKernel）',
      recommended: true,
    },
    messages: {
      deprecatedNaming:
        '禁用已废弃的命名 "{{match}}"。{{hint}}。' +
        '项目已重命名为 MedKernel（2026-05-18），见 CHANGELOG.md。' +
        '历史保留例外：CSS Token --mk-*（仅 tokens.css）。',
    },
    schema: [],
  },

  create(context) {
    const filename = context.getFilename();
    if (isExempt(filename)) return {};

    return {
      // 标识符
      Identifier(node) {
        for (const { pattern, hint } of DEPRECATED_PATTERNS) {
          if (pattern.test(node.name)) {
            context.report({
              node,
              messageId: 'deprecatedNaming',
              data: { match: node.name, hint },
            });
            return;
          }
        }
      },

      // 字符串字面量
      Literal(node) {
        checkString(node.value, node, context);
      },

      // 模板字符串
      TemplateLiteral(node) {
        for (const q of node.quasis) {
          checkString(q.value.raw, node, context);
        }
      },

      // import 路径
      ImportDeclaration(node) {
        if (node.source && typeof node.source.value === 'string') {
          checkString(node.source.value, node.source, context);
        }
      },

      // 注释
      'Program:exit'() {
        const comments = context.getSourceCode().getAllComments();
        for (const c of comments) {
          checkString(c.value, c, context);
        }
      },
    };
  },
};
