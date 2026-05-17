// 前端 ESLint flat config（ESLint 9 风格）
// 与 zy-engine-mvp/docs/07_前端开发规范.md 对齐。

import js from "@eslint/js";
import tseslint from "typescript-eslint";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";
import globals from "globals";

export default tseslint.config(
  {
    ignores: ["dist", "node_modules", "coverage", "**/*.config.{js,ts}"],
  },
  {
    files: ["src/**/*.{ts,tsx}"],
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: "module",
      globals: { ...globals.browser, ...globals.es2022 },
      parserOptions: {
        ecmaFeatures: { jsx: true },
      },
    },
    plugins: {
      "react-hooks": reactHooks,
      "react-refresh": reactRefresh,
    },
    rules: {
      // === TypeScript ===
      "@typescript-eslint/no-explicit-any": ["warn", { ignoreRestArgs: true }],
      "@typescript-eslint/no-unused-vars": [
        "error",
        { argsIgnorePattern: "^_", varsIgnorePattern: "^_" },
      ],
      "@typescript-eslint/consistent-type-imports": [
        "warn",
        { prefer: "type-imports" },
      ],
      "@typescript-eslint/no-non-null-assertion": "warn",
      "@typescript-eslint/ban-ts-comment": [
        "error",
        {
          "ts-ignore": true,        // 完全禁止
          "ts-expect-error": "allow-with-description",
          minimumDescriptionLength: 5,
        },
      ],

      // === React Hooks ===
      ...reactHooks.configs.recommended.rules,
      "react-refresh/only-export-components": [
        "warn",
        { allowConstantExport: true },
      ],

      // === 反模式（07_前端开发规范.md §17）===
      "no-console": ["warn", { allow: ["warn", "error"] }],
      "no-debugger": "error",
      "no-alert": "error",
      "no-eval": "error",
      "no-implied-eval": "error",
      "no-restricted-syntax": [
        "error",
        {
          selector:
            "ImportDeclaration[source.value='axios']:not(:has(ImportSpecifier))",
          message:
            "禁止直接 import axios。请使用 @/api/client 中的 get/post 封装（见 07_前端开发规范.md §6）。",
        },
        {
          selector: "CallExpression[callee.object.name='localStorage'][callee.property.name='setItem']",
          message:
            "localStorage 写入需慎重。禁止存放 token / API Key / 患者完整隐私（见 07_前端开发规范.md §12）。",
        },
      ],
      "no-restricted-imports": [
        "error",
        {
          paths: [
            {
              name: "axios",
              importNames: ["default"],
              message:
                "禁止直接 import axios 默认导出。请使用 @/api/client。",
            },
          ],
        },
      ],

      // === 可读性 ===
      eqeqeq: ["error", "always"],
      "prefer-const": "error",
      "no-var": "error",
      "object-shorthand": "warn",
      "no-nested-ternary": "warn",
    },
  },
  {
    files: ["src/**/*.test.{ts,tsx}", "src/test/**/*.{ts,tsx}", "src/mocks/**/*.{ts,tsx}"],
    languageOptions: {
      globals: { ...globals.browser, ...globals.node, ...globals.es2022 },
    },
    rules: {
      "no-console": "off",
      "@typescript-eslint/no-explicit-any": "off",
    },
  },
);
