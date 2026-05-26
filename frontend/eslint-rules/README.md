# MedKernel 自定义 ESLint 规则

本目录是项目自定义的 ESLint 规则，配合 `frontend/eslint.config.js` 使用。

## 规则清单

| 规则名 | 类别 | 严重度 | 说明 |
|---|---|:---:|---|
| `no-hardcoded-color.js` | 视觉 | error | 禁止硬编码颜色（必须用 token） |
| `no-inline-style.js` | 视觉 | warn | 存量内联样式只减不增，后续逐步抽取到 CSS Modules |
| `require-source-info-for-medical.js` | 业务 | warn | 含医学语义的组件必须有 `<SourceInfo>` |
| `forbid-deprecated-naming.js` | 命名 | error | 禁用重启前品牌、路径和环境变量标识 |

内置 ESLint 规则还会阻断生产代码中的直接 `console.*`、直接 `localStorage/sessionStorage` 访问，以及组件内 axios 直连。

## 集成方式

在 `frontend/eslint.config.js`：

```js
import noHardcodedColor from './eslint-rules/no-hardcoded-color.js';
import requireSourceInfo from './eslint-rules/require-source-info-for-medical.js';
import forbidDeprecatedNaming from './eslint-rules/forbid-deprecated-naming.js';
import noInlineStyle from './eslint-rules/no-inline-style.js';

export default [
  // ... 其它配置
  {
    plugins: {
      medkernel: {
        rules: {
          'no-hardcoded-color': noHardcodedColor,
          'require-source-info-for-medical': requireSourceInfo,
          'forbid-deprecated-naming': forbidDeprecatedNaming,
          'no-inline-style': noInlineStyle,
        },
      },
    },
    rules: {
      'medkernel/no-hardcoded-color': 'error',
      'medkernel/require-source-info-for-medical': 'warn',
      'medkernel/forbid-deprecated-naming': 'error',
      'medkernel/no-inline-style': 'warn',
    },
  },
];
```

## 自检命令

```bash
cd frontend
npm run lint                    # 跑全部规则
npm run lint -- --rule medkernel/no-hardcoded-color  # 跑单条
```

## 新增规则流程

1. 在本目录新建 `规则名.js`
2. 实现 ESLint Rule API（`create(context) { return { ... } }`）
3. 在 `eslint.config.js` 注册
4. 在本 README 表格登记
5. 配套写单元测试 `tests/规则名.test.js`
