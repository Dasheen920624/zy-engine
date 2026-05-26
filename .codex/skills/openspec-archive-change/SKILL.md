---
name: openspec-archive-change
description: 完成 OpenSpec 变更后进行收尾：检查任务、同步主规格，并按当前项目策略归档或清理变更目录。
license: MIT
compatibility: 需要 openspec CLI。
metadata:
  author: openspec
  version: "1.0"
  generatedBy: "1.3.1"
---

用于 OpenSpec 变更完成后的收尾。当前项目不保留重启前历史包袱；已完成的新变更优先把结论同步到主规格和当前文档，再根据用户要求归档或移除变更目录，Git 历史负责追溯。

## 输入

用户可以指定变更名。若未指定，运行：

```bash
openspec list --json
```

只列出活跃变更，并用中文请用户选择；不要自动猜测。

## 工作步骤

1. 查看产物完成状态：

   ```bash
   openspec status --change "<name>" --json
   ```

2. 读取任务文件，统计 `- [ ]` 和 `- [x]`。如果仍有未完成任务，先向用户确认是否继续收尾。
3. 检查 `openspec/changes/<name>/specs/` 下是否存在 delta spec。
4. 若存在 delta spec，逐项对比 `openspec/specs/<capability>/spec.md`，用中文说明需要同步的新增、修改或删除。
5. 用户确认后，将已完成需求同步到主规格。同步后再次检查主规格为中文。
6. 按用户要求处理变更目录：
   - 若需要保留阶段记录，可移动到 `openspec/changes/archive/YYYY-MM-DD-<name>/`。
   - 若用户要求最干净模式，并且主规格与当前文档已同步，可删除已完成变更目录。

## 输出

用中文说明：

- 变更名称。
- 任务完成情况。
- 主规格是否已同步。
- 变更目录是归档、删除还是保留为活跃变更。
- 仍需人工确认的风险。

## 约束

- 不在任务未完成或规格未同步时无提示清理。
- 不把重启前历史归档重新引入当前工作树。
- 若用户明确要求“最干净”，优先清理已完成变更目录，保留 Git 历史用于追溯。
