# 参考实现样板（Reference Implementations）

> 提供可直接复制的样板代码，避免 AI 自由发挥导致风格不一致。

## 现有样板

| 文件 | 适用场景 | 关联 PR |
|---|---|---|
| [status-badge-component.md](status-badge-component.md) | 实现公共组件 | PR-V2-02 |
| [rest-controller-pattern.md](rest-controller-pattern.md) | 实现后端 REST Controller | PR-V2-04 等所有后端 PR |
| [page-with-six-states.md](page-with-six-states.md) | 实现前端页面（6 状态） | PR-V2-05 ~ PR-V2-12 |
| [api-call-pattern.md](api-call-pattern.md) | 前端调用后端 API | 所有前端 PR |

## 使用规则

1. 实现 X 类组件/页面前**必须**先读 X 类的样板
2. 复制样板代码到目标位置，按业务调整
3. **不允许**自创新模式（如有必要先新增样板）

## 新增样板流程

1. 在本目录新建 `xxx-pattern.md`
2. 包含：使用场景、完整代码、关键约束、不允许变体
3. 在本 README 表格登记
4. 关联到对应 PR
