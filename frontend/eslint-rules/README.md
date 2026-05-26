# MedKernel 自定义 ESLint 规则

本目录是项目自定义的 ESLint 规则，配合 `frontend/eslint.config.js` 使用。

## 规则清单

| 规则名 | 类别 | 严重度 | 说明 |
|---|---|:---:|---|
| `no-hardcoded-color.js` | 视觉 | error | 禁止硬编码颜色（必须用 token） |
| `require-source-info-for-medical.js` | 业务 | warn | 含医学语义的组件必须有 `<SourceInfo>` |
| `forbid-deprecated-naming.js` | 命名 | error | 禁用重启前品牌、路径和环境变量标识 |

## 集成方式

在 `frontend/eslint.config.js`：

```js
import noHardcodedColor from './eslint-rules/no-hardcoded-color.js';
import requireSourceInfo from './eslint-rules/require-source-info-for-medical.js';
import forbidDeprecatedNaming from './eslint-rules/forbid-deprecated-naming.js';

export default [
  // ... 其它配置
  {
    plugins: {
      medkernel: {
        rules: {
          'no-hardcoded-color': noHardcodedColor,
          'require-source-info-for-medical': requireSourceInfo,
          'forbid-deprecated-naming': forbidDeprecatedNaming,
        },
      },
    },
    rules: {
      'medkernel/no-hardcoded-color': 'error',
      'medkernel/require-source-info-for-medical': 'warn',
      'medkernel/forbid-deprecated-naming': 'error',
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
