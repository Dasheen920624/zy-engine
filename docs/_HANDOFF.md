# 会话接力交接（_HANDOFF）

> **用途**：跨会话、跨工具的中断续接神谕。**任何 AI 工具（Claude Code / Codex / Cursor / Copilot / Gemini 等）或人类协作者**开工第一件事读本文件，直接拿到所有在途工作线的「现在到哪、下一步做什么」，**不要翻历史会话或 git 考古**（贵且慢）。适用一切跨会话工作：施工卡迁移、软件开发、运维、审计……
> **这是中立的真相源**：纯 markdown、在版本库里、不依赖任何单一工具的私有记忆。配套规则见 [AGENTS.md](../AGENTS.md)「会话接力」节。
> **维护**：完成一个检查点/PR、或预感会话要中断时，立刻更新对应工作线的「状态」「下一步」；完成的线移入「已归档」。新领任务按末尾模板加一条工作线。

## 在途工作线

### 线 1 · D4 质控改进施工卡迁移 🚧
- **类型**：文档（施工卡迁移）
- **分支**：`claude/d4-quality`（基于含 #159 的新 main；D4 全 14 卡 + 简报 + 四索引回填在此，PR 待开/待合）
- **目标**：D4 域 14 卡（8 ID + 6 页面）搬成自包含施工卡 —— **全部已建**；**一次提交一个收官 PR**（用户要求减少提交/省 token）
- **全域进度**：D0 ✅(#152/#153) · D1 ✅(#154) · D2 ✅(#156/#157/#158) · D3 ✅(#159) · **D4 🚧 全 14 卡已建（PR 待合）** · D5/D6/wave2/ga ⬜
- **当前批**（8 ID + 6 页面）：API-08 · EVAL-01 · OPT-08 · EMR-LEVEL-01/02 · SVC-QUALITY-01/02/03 / 页 QCDASH-01 · QCALERT-01 · INSAUDIT-01 · EVALSET-01 · EVALRES-01 · AIREVIEW-01
- **状态**：#159 已合（D3 收官）；**D4 共 14 卡已建**（现状段基于真实后端类名 + 前端真实路由）、**_index（D4 目录 ✅ 14 卡 + 场景行 S9/S10/S11/S15/S23）+ _coverage-matrix（已迁 108→118，D4 场景锚点 10 已迁，D4 标 ✅）+ backlog（🗂️ 全 14 卡 + D4-PAGE 实化）已回填**；待提交 → 开 PR → 合并
- **D4 后端/前端现状（已核实 2026-05-30，建卡照此、勿凭空）**：评估/整改＝`engine/evaluation`（EvaluationEngineService/Controller · EvaluationIndicator{+CreateRequest/Filter/Status} · EvaluationResult{+Level/Filter} · RectificationTask{+Status}/RectificationReview{+Decision} · EvaluationIdempotencyKey）——EVAL-01/API-08/SVC-QUALITY-03 多已实质成型；6 前端页 `frontend/src/pages/quality/*.tsx` 已存在待真实化（`/qc/dashboard`、`/qc/alerts`、`/qc/insurance`、`/qc/eval/sets`、`/qc/eval/results`、`/aik/review`）。缺口（新建/框架化）：价值/ROI(OPT-08)、电子病历评级(EMR-LEVEL-01/02)、DRG/DIP/医保审核(SVC-QUALITY-02)无专用后端；AI 知识审核(AIREVIEW-01)只做审/发、AI 生成留 wave2。
- **下一步**（精确动作）：
  1. ✅ D4 全 14 卡已建、_index/_coverage-matrix/backlog 已回填（本分支 `claude/d4-quality`）
  2. 提交 → 推送 → 开 **PR**（`docs(cards): D4 质控改进 14 卡 + 索引回填（D4 收官）`）→ 请用户合并
  3. 合并后 **D4 卡全建** → 走 D4 域级验收（[质量基线 §2.3]）→ 从新 main 起 D5 合规运维迁移
- **后续批**：D5 合规运维（EVID-01/SYS-06/OPT-05/SVC-COMPLIANCE-01/02 + 6 页）→ D6 高级工具（OPT-10 + 5 页）→ wave2 → ga
- **遗留 sweep（不急）**：各域卡对同域已建卡仍有 `../_index.md` 间接引用（可达、不算错），全域搬完做一次「间接→直链」升级 pass；_coverage-matrix 当前为场景级锚点，卡级 §-锚点细化可后补。
- **专项流程**：填卡五步见 `docs/superpowers/specs/2026-05-30-doc-architecture-build-cards-design.md §7 P1`

## 已归档工作线（最近完成，供回溯）
- D0 登录域 28 卡 ✅（#152 + #153）
- D1 工作台 3 卡 ✅（#154：INFRA-09 + WORKBENCH-01/02）
- D2 试点准备 30 卡 ✅（#156 B1 框架 + #157 B2-B4 引擎/API + #158 B5-B6 服务包+7页面 收官）
- D3 临床运行 21 卡 ✅（#159：14 ID + 7 页面，整域一个 PR）

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
> 末次更新：2026-05-30 · D3 收官合并（#159）；D4 质控改进 14 卡已建（`claude/d4-quality`，收官 PR 待合）
