# 会话接力交接（_HANDOFF）

> **用途**：跨会话、跨工具的中断续接神谕。**任何 AI 工具（Claude Code / Codex / Cursor / Copilot / Gemini 等）或人类协作者**开工第一件事读本文件，直接拿到所有在途工作线的「现在到哪、下一步做什么」，**不要翻历史会话或 git 考古**（贵且慢）。适用一切跨会话工作：施工卡迁移、软件开发、运维、审计……
> **这是中立的真相源**：纯 markdown、在版本库里、不依赖任何单一工具的私有记忆。配套规则见 [AGENTS.md](../AGENTS.md)「会话接力」节。
> **维护**：完成一个检查点/PR、或预感会话要中断时，立刻更新对应工作线的「状态」「下一步」；完成的线移入「已归档」。新领任务按末尾模板加一条工作线。

## 在途工作线

### 线 1 · D2 试点准备施工卡迁移 🚧
- **类型**：文档（施工卡迁移）
- **分支**：`claude/unruffled-satoshi-1105db`（基于含 #155 的新 main；B1 四卡 + 索引回填均在此分支，PR 待开/待合）
- **目标**：D2 域 30 卡（23 原 ID + 7 页面）搬成自包含施工卡，分 6 批 B1–B6、每批一 PR
- **全域进度**：D0 ✅(#152/#153) · D1 ✅(#154) · **D2 🚧 B1 卡已建+索引回填（PR 待合）** · D3/D4/D5/D6/wave2/ga ⬜
- **当前批 B1**（模型与版本框架基石）：API-01 · OPT-01 · SYS-04 · SYS-08
- **状态**：#155 已合并；**B1 _brief + 四卡（API-01/OPT-01/SYS-04/SYS-08）已建**、引用约定（已建卡→相对路径 / 未建卡→`../_index.md`）与 D0/D1 一致、**_index + _coverage-matrix + backlog D2 段已回填**（覆盖矩阵已迁 38→50，场景级锚点保持 `待迁` 待页面/服务卡）；待提交 → 开 PR → 合并
- **B1 后端现状（已核实 2026-05-30，建卡「现状」段照此写、勿凭空）**：
  - **API-01 标准上下文 API**：已有实质基础——`engine/context/` 下 ContextSnapshot(+Response/Filter)、PackageVersionPort、CanonicalResource(Type/Repository)、ContextValidator、ClinicalEventController 等；卡为"真实化/契约化/补全"非从零。
  - **OPT-01 FHIR R4/R5 门面**：基本待建——无 FHIR 资源门面（仅 `engine/integration` adapter DTO 提及 fhir）；现状是项目自有 `CanonicalResource` 抽象；卡＝新建 FHIR 门面并映射到 CanonicalResource。
  - **SYS-04 版本继承与发布框架**：待建——无统一 publish/inheritance 框架（散落各处）。
  - **SYS-08 权威知识版本解析/原子替换**：部分已有——`engine/knowledge/` 有 KnowledgeVersionService/Controller/Status、KnowledgeAssetVersion；卡聚焦"唯一有效约束/替代链/紧急失效/原子替换"框架化（与 B2·KNOW-02 划清单一归属）。
- **下一步**（精确动作）：
  1. ✅ _brief + 四卡已建、_index/_coverage-matrix/backlog D2 段已回填（本分支）
  2. 提交 → 推送 → 开 PR（`docs(cards): D2-B1 模型与版本框架基石`）→ 请用户合并
  3. 合并后更新本文件（B1 ✅）→ 从新 origin/main 起 `claude/d2-knowledge` 做 B2（知识+字典：API-03 / KNOW-01 / KNOW-02 / TERM-01 / API-04）
- **后续批**：B2 知识+字典 → B3 规则+路径 → B4 包发布+集成 → B5 试点服务包 → B6 七页面
- **专项流程**：填卡五步见 `docs/superpowers/specs/2026-05-30-doc-architecture-build-cards-design.md §7 P1`

## 已归档工作线（最近完成，供回溯）
- D0 登录域 28 卡 ✅（#152 + #153）
- D1 工作台 3 卡 ✅（#154：INFRA-09 + WORKBENCH-01/02）

## 通用约定（所有工作线 / 所有工具适用）
- **分支与 PR**：禁直推 main；分支 → 推送 → PR → 合并 → 确认 origin/main 含合并提交。**squash 合并后必须从新 origin/main 重拉分支**再做下一单元（否则基点回退、重复带入）。一个逻辑单元一个 PR；大任务拆批、每批独立分支基于当时最新 main。分支前缀现状用 `claude/`。
- **核现状别信单次 read**：建卡/改代码前用 `grep`/`find`/`git diff` 对照真实仓库（`frontend/src`、`medkernel-backend/src`），曾因伪造 read 整批返工。
- **软件开发**：遵循 TDD（先写失败测试再实现）；动手前跑现有测试建绿色基线；改动后跑测试 + 真实性门禁（T-GATE）再宣称完成——**证据优先，别空口说「已修复/已通过」**。
- **找散落改动**：中断后 `git worktree list` + `git status` + 各 worktree 查未跟踪文件（改动曾停在未提交的 worktree）。
- **语言**：文档/PR/注释简体中文（详见 AGENTS.md 语言要求）。

## 新开工作线模板（复制到「在途工作线」填写）
```
### 线 N · <一句话标题> 🚧
- 类型：文档 / 软件开发 / 运维 / 审计
- 分支：claude/<name>
- 目标：<这条线要交付的可验证结果>
- 状态：<现在到哪>
- 下一步（精确到动作/命令）：1. … 2. …
- 相关文件 / 测试 / 坑：<关键路径、待跑测试、已知陷阱>
```

---
> 末次更新：2026-05-30 · 机制通用化（跨工作类型 + 跨 AI 工具中立）；D2 B1 进行中
