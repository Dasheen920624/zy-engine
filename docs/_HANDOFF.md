# 会话接力交接（_HANDOFF）

> **用途**：跨会话、跨工具的中断续接神谕。**任何 AI 工具（Claude Code / Codex / Cursor / Copilot / Gemini 等）或人类协作者**开工第一件事读本文件，直接拿到所有在途工作线的「现在到哪、下一步做什么」，**不要翻历史会话或 git 考古**（贵且慢）。适用一切跨会话工作：施工卡迁移、软件开发、运维、审计……
> **这是中立的真相源**：纯 markdown、在版本库里、不依赖任何单一工具的私有记忆。配套规则见 [AGENTS.md](../AGENTS.md)「会话接力」节。
> **维护**：完成一个检查点/PR、或预感会话要中断时，立刻更新对应工作线的「状态」「下一步」；完成的线移入「已归档」。新领任务按末尾模板加一条工作线。

## 在途工作线

### 线 1 · D3 临床运行施工卡迁移 🚧
- **类型**：文档（施工卡迁移）
- **分支**：`claude/d3-clinical`（基于含 #158 的新 main；D3 全 21 卡 + 简报 + 四索引回填在此，PR 待开/待合）
- **目标**：D3 域 21 卡（14 ID + 7 页面）搬成自包含施工卡 —— **全部已建**；**一次提交一个收官 PR**（用户要求减少提交/省 token）
- **全域进度**：D0 ✅(#152/#153) · D1 ✅(#154) · D2 ✅(#156/#157/#158) · **D3 🚧 全 21 卡已建（PR 待合）** · D4/D5/D6/wave2/ga ⬜
- **当前批**（14 ID + 7 页面）：API-02/07/09/11 · CDSS-01 · FOLLOW-01 · EMBED-01 · MED-C3 · OPT-02/03/04 · SVC-CLINICAL-01/02/03 / 页 PMI-01 · PPATH-01 · REMIND-01 · RULECHK-01 · TODO-01 · NOTIFY-01 · FUP-01
- **状态**：#158 已合（D2 收官）；**D3 共 21 卡已建**（现状段基于真实后端类名 + 前端真实路由）、**_index（D3 目录 ✅ 21 卡 + 场景行 S8/S12/S16）+ _coverage-matrix（已迁 101→108，D3 场景锚点已迁，D3 标 ✅）+ backlog（🗂️ 全 21 卡 + D3-PAGE 实化）已回填**；待提交 → 开 PR → 合并
- **D3 后端/前端现状（已核实 2026-05-30，建卡照此、勿凭空）**：CDSS=`engine/recommendation`（RecommendationCard+EngineController，含反馈/疲劳）· 随访=`engine/followup`（FollowupEngineService/Event/AbnormalReport）· 嵌入=`engine/embed`（EmbedLaunchToken/Service/FeedbackRequest）· 患者主索引=`engine/mpi`（MpiService/Patient/Merge/Stats）· 临床事件=`engine/context`（ClinicalEvent）· 规则执行=`engine/rule`（RuleDslEvaluator）· 关键时钟=`engine/pathway`（ClinicalClock）；7 前端页 `frontend/src/pages/clinical/*.tsx` 已存在待真实化（路由 `/mpi`、`/pathway/patients`、`/cdss/fatigue`、`/rule/validate`、`/workflow/todos`、`/notifications`、`/clinical/followup`）。缺口（框架化非从零）：CDS Hooks 契约(OPT-02)/风险分级矩阵(OPT-03)/红线库+静默试运行(OPT-04)/安全撤回端到端(MED-C3)。
- **下一步**（精确动作）：
  1. ✅ D3 全 21 卡已建、_index/_coverage-matrix/backlog 已回填（本分支 `claude/d3-clinical`）
  2. 提交 → 推送 → 开 **PR**（`docs(cards): D3 临床运行 21 卡 + 索引回填（D3 收官）`）→ 请用户合并
  3. 合并后 **D3 卡全建** → 走 D3 域级验收（[质量基线 §2.3]）→ 从新 main 起 D4 质控改进迁移
- **后续批**：D4 质控（API-08/EVAL-01/OPT-08/EMR-LEVEL-01/02/SVC-QUALITY-01/02/03 + 6 页）→ D5 合规运维 → D6 高级工具 → wave2 → ga
- **遗留 sweep（不急）**：D2/D3 卡对同域已建卡仍有 `../_index.md` 间接引用（可达、不算错），全域搬完做一次「间接→直链」升级 pass；_coverage-matrix D3 当前为场景级锚点，卡级 §-锚点细化可后补。
- **专项流程**：填卡五步见 `docs/superpowers/specs/2026-05-30-doc-architecture-build-cards-design.md §7 P1`

## 已归档工作线（最近完成，供回溯）
- D0 登录域 28 卡 ✅（#152 + #153）
- D1 工作台 3 卡 ✅（#154：INFRA-09 + WORKBENCH-01/02）
- D2 试点准备 30 卡 ✅（#156 B1 框架 + #157 B2-B4 引擎/API + #158 B5-B6 服务包+7页面 收官）

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
> 末次更新：2026-05-30 · D2 收官合并（#158）；D3 临床运行 21 卡已建（`claude/d3-clinical`，收官 PR 待合）
