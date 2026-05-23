# ADR — 架构决策记录（Architecture Decision Records）

> 记录"为什么这么设计"的不可违反决策。任何 AI 实施任务前必读对应 ADR。

## 现有 ADR

| 编号 | 标题 | 状态 | 涉及范围 |
|---|---|:---:|---|
| 0001 | 三产品分层架构（A 配置工厂 / B 临床嵌入器 / C 质控驾驶舱） | Accepted | 全平台 |
| 0002 | V2 PR 命名空间隔离（PR-V2-XX 不复用历史 FE-XXX） | Accepted | 任务管理 |
| 0003 | 禁止硬编码颜色 / 字号 / 间距（必须用 Design Token） | Accepted | 前端 |
| 0004 | 医学内容必须有来源（MISSING_SOURCE 阻断发布） | Accepted | 业务规则 |

## ADR 状态值

- **Proposed** — 提议中，待评审
- **Accepted** — 已接受，必须遵守
- **Deprecated** — 已废弃（被新 ADR 替代）
- **Superseded by ADR-XXXX** — 被某个新 ADR 替代

## 新增 ADR 流程

1. 在本目录新建 `NNNN-title-in-english.md`（编号顺延）
2. 用 [`template.md`](template.md) 模板填写
3. 提交 PR 让团队评审
4. 通过后状态改为 Accepted
5. 在本 README 表格中登记

## ADR 模板

见 [`template.md`](template.md)。
