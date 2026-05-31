# 会话接力交接（_HANDOFF）

> **用途**：跨会话、跨工具的中断续接神谕。**任何 AI 工具（Claude Code / Codex / Cursor / Copilot / Gemini 等）或人类协作者**开工第一件事读本文件，直接拿到所有在途工作线的「现在到哪、下一步做什么」，**不要翻历史会话或 git 考古**（贵且慢）。适用一切跨会话工作：施工卡迁移、软件开发、运维、审计……
> **这是中立的真相源**：纯 markdown、在版本库里、不依赖任何单一工具的私有记忆。配套规则见 [AGENTS.md](../AGENTS.md)「会话接力」节。
> **维护**：完成一个检查点/PR、或预感会话要中断时，立刻更新对应工作线的「状态」「下一步」；完成的线移入「已归档」。新领任务按末尾模板加一条工作线。

## 在途工作线

### 线 1 · wave2 第二波 AI 施工卡（55 项 · 4 子块多 PR）🚧 —— 批 1 X-LLM 待合
- **背景**：核心域 D0–D6 + GA 12 卡 + 覆盖矩阵卡级锚点 + 根文档对齐 **全部已合**（#152–#167）。卡体系迁移进入最后一域 **wave2**（AI 加深）；覆盖矩阵已迁 **127** 锚点。
- **批次（4 子块 = 4 PR，子块内整批一个 PR）**：
  1. **X-LLM 模型网关 11 卡 🚧（`claude/wave2-llm`，PR 待合）** —— API-12 · LLM-01~08 · OPT-06/09 + wave2 域简报 + 四索引回填。现状：后端 `engine/llm/` 已有网关 MVP（`ModelGatewayService`/`Controller` + 五方言 `V18` + 8 能力码 + B0 诚实回退），卡照实写「MVP 已建 / 缺口待建」。
  2. X-AIK AI 工厂 12 卡（AIK-STD-01~12）—— 待起（`claude/wave2-aik`）。
  3. X-KNOWGEN 首发资产 15 卡（KNOWGEN-01~15）—— 待起（`claude/wave2-knowgen`）。
  4. X-DOMAIN 15 领域门面 + 服务包 17 卡（NURSING…SVC-DOMAIN-02）—— 待起（`claude/wave2-domain`），同批回填场景 S17–S40。
- **下一步（精确到动作）**：1. 推 `claude/wave2-llm` → 开 PR（标题 `docs(cards): wave2 X-LLM 模型网关 11 卡 + 域简报 + 四索引回填`）。2. 用户合并后从**新 origin/main 重拉**，起批 2 `claude/wave2-aik`。3. 四批全合 → 过 wave2 域级验收 → 点亮 GA 门禁 3/8/10。
- **GA pass 前置**：门禁 3（AIK）/ 8（15 领域门面）/ 10（KNOWGEN-15）依赖 wave2 建成。
- **起新子块口径（沿用，已跑通 7 单元 + wave2 批1）**：从新 origin/main 起 `claude/<name>` → 套 [`_template.md`](cards/_template.md) + backlog 段卡清单 + `grep` 真实后端/前端现状（**不深调研**，差异留执行开发）→ 域简报（首批已建可复用）+ 全卡 → 回填四索引（`_index`/`_coverage-matrix`/`backlog`/`_HANDOFF`）→ **整批一个 PR** → 用户合并 → 从新 main 重拉。
- **专项流程**：填卡五步见 `docs/superpowers/specs/2026-05-30-doc-architecture-build-cards-design.md §7 P1`

## 已归档工作线（最近完成，供回溯）
- D0 登录域 28 卡 ✅（#152 + #153）
- D1 工作台 3 卡 ✅（#154：INFRA-09 + WORKBENCH-01/02）
- D2 试点准备 30 卡 ✅（#156 B1 + #157 B2-B4 + #158 B5-B6 收官）
- D3 临床运行 21 卡 ✅（#159：14 ID + 7 页面，整域一个 PR）
- D4 质控改进 14 卡 ✅（#160：8 ID + 6 页面，整域一个 PR）
- D5 合规运维 11 卡 ✅（#161：5 ID + 6 页面，整域一个 PR）
- D6 高级工具 6 卡 ✅（#162：1 ID + 5 页面）—— 核心域收官
- 间接引用→直链 sweep ✅（#163：32 处 / D2 五卡 + D3/_brief，零死链）
- GA 总验收 12 卡 ✅（#164：QA-01~08 + DEGRADE-01 + SYS-07 + INFRA-07 + INFRA-10，验收规格，pass 待 wave2）
- 组织树七层一致性修复 ✅（#165：D2 SVC-PILOT-01/TENANT-01）
- 覆盖矩阵卡级 §-锚点细化 ✅（#166：§3 已迁场景 18→68 卡级行；原线 2）
- 根文档对齐 ✅（#167：README 卡为中心 + AGENTS 分支前缀工具中立）

## 通用约定（所有工作线 / 所有工具适用）
- **分支与 PR**：禁直推 main；分支 → 推送 → PR → 合并 → 确认 origin/main 含合并提交。**squash 合并后必须从新 origin/main 重拉分支**再做下一单元（否则基点回退、重复带入）。一个逻辑单元一个 PR；大任务拆批、每批独立分支基于当时最新 main。分支前缀现状用 `claude/`。
- **核现状别信单次 read**：建卡/改代码前用 `grep`/`find`/`git diff` 对照真实仓库（`frontend/src`、`medkernel-backend/src`），曾因伪造 read 整批返工。
- **软件开发**：遵循 TDD（先写失败测试再实现）；动手前跑现有测试建绿色基线；改动后跑测试 + 真实性门禁（T-GATE）再宣称完成——**证据优先，别空口说「已修复/已通过」**。
- **找散落改动**：中断后 `git worktree list` + `git status` + `git log origin/main` 查未提交/是否真合（改动曾停在未提交的 worktree；曾发生「写完卡+回填但截断在提交前、分支未提交」）。
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
> 末次更新：2026-05-31 · 核心域 D0–D6 + GA 12 卡 + 覆盖矩阵卡级锚点 + 根文档对齐 **全部已合**（#152–#167）；wave2 启动——**批 1 X-LLM 模型网关 11 卡 + 域简报已建（`claude/wave2-llm`，PR 待合）**；剩 wave2 X-AIK/KNOWGEN/DOMAIN（44 项，批 2-4）
