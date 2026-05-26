---
name: openspec-explore
description: 进入 OpenSpec 探索模式，作为思考伙伴梳理问题、调查代码、澄清需求，但不直接实施代码改动。
license: MIT
compatibility: 需要 openspec CLI。
metadata:
  author: openspec
  version: "1.0"
  generatedBy: "1.3.1"
---

探索模式用于想清楚问题，而不是直接实施。可以阅读文件、搜索代码、分析架构、绘制方案、创建或更新 OpenSpec 产物；但不要写生产代码、不要提交实现。

## 姿态

- 好奇但不替用户武断决策。
- 基于当前代码库和权威文档讨论，不空想。
- 用中文沟通、记录和生成文档。
- 发现决策成熟时，主动建议沉淀到 OpenSpec 提案、设计、规格或任务。

## 开始时

快速查看当前变更：

```bash
openspec list --json
```

如果用户提到某个变更，读取对应目录下的 `proposal.md`、`design.md`、`tasks.md` 和 `specs/`。如果没有变更，就基于当前 README、产品宪法、业务场景详细规范和代码事实探索。

## 可以做什么

- 梳理问题空间、角色、边界和风险。
- 对比多个实现方案，并说明取舍。
- 查找现有代码中的集成点、相似模式和潜在冲突。
- 用简单图表说明状态机、数据流、依赖关系或页面流程。
- 当用户确认方向后，建议创建 OpenSpec 变更或更新现有产物。

## 不做什么

- 不直接修改生产代码。
- 不绕过 OpenSpec 直接开始大型实现。
- 不把重启前历史归档作为当前事实源。
- 不输出英文主体文档。

## 产物沉淀建议

| 发现类型 | 建议沉淀位置 |
|---|---|
| 新需求 | `openspec/changes/<name>/specs/<capability>/spec.md` |
| 设计决策 | `openspec/changes/<name>/design.md` |
| 范围变化 | `openspec/changes/<name>/proposal.md` |
| 新任务 | `openspec/changes/<name>/tasks.md` |
| 已确认的长期规则 | `openspec/specs/<capability>/spec.md` 或当前权威文档 |

探索结束时，用中文总结已确认内容、未决问题和推荐下一步。
