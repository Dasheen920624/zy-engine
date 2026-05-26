---
name: openspec-propose
description: 为新变更一次性创建 OpenSpec 提案、设计、规格和任务。适用于用户已经描述想构建或修复的内容，并希望进入可实施状态。
license: MIT
compatibility: 需要 openspec CLI。
metadata:
  author: openspec
  version: "1.0"
  generatedBy: "1.3.1"
---

创建新的 OpenSpec 变更，并按依赖顺序生成实施前必须具备的产物。

## 输入

用户应提供 kebab-case 变更名，或描述想构建、修复、重构的内容。若只有描述，需要推导一个简短的 kebab-case 名称，例如“新增用户认证”对应 `add-user-auth`。

## 工作步骤

1. 如果意图不清楚，先用中文询问用户想构建或修复什么；不要在目标不明时继续。
2. 创建变更目录：

   ```bash
   openspec new change "<name>"
   ```

3. 查看产物依赖和实施准入要求：

   ```bash
   openspec status --change "<name>" --json
   ```

4. 按依赖顺序生成所有 `applyRequires` 需要的产物。每个产物生成前先运行：

   ```bash
   openspec instructions <artifact-id> --change "<name>" --json
   ```

5. 严格使用返回的 `template`、`instruction`、`outputPath` 和依赖文件；`context`、`rules` 是写作约束，不要原样复制进产物。
6. 每生成一个产物后重新检查 `openspec status --change "<name>" --json`，直到实施所需产物全部完成。
7. 最后运行：

   ```bash
   openspec status --change "<name>"
   ```

## 文档要求

- 产物主体必须使用简体中文。
- 代码标识符、命令、路径、接口名、第三方产品名可以保留英文，但要用中文解释。
- 不引用重启前历史归档作为当前事实源；需要背景时引用当前 README、产品宪法、OpenSpec 主规格和 Superpowers 当前设计。

## 输出

完成后用中文概述：

- 变更名称和目录。
- 已创建的产物清单。
- 当前是否已可实施。
- 下一步可以执行“开始实施此 OpenSpec 变更”。

## 约束

- 必须生成 schema 要求的全部实施准入产物。
- 生成新产物前必须读取已完成的依赖产物。
- 若同名变更已存在，先说明现状并询问是继续还是另建新名称。
- 每个产物写入后要确认文件存在。
