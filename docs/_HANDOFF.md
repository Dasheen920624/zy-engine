# 会话接力交接（_HANDOFF）

> **用途**：跨会话、跨工具的中断续接神谕。**任何 AI 工具（Claude Code / Codex / Cursor / Copilot / Gemini 等）或人类协作者**开工第一件事读本文件，直接拿到所有在途工作线的「现在到哪、下一步做什么」，**不要翻历史会话或 git 考古**（贵且慢）。适用一切跨会话工作：施工卡迁移、软件开发、运维、审计……
> **这是中立的真相源**：纯 markdown、在版本库里、不依赖任何单一工具的私有记忆。配套规则见 [AGENTS.md](../AGENTS.md)「会话接力」节。
> **维护**：完成一个检查点/PR、或预感会话要中断时，立刻更新对应工作线的「状态」「下一步」；完成的线移入「已归档」。新领任务按末尾模板加一条工作线。

## 在途工作线

### 线 1 · D2 试点准备施工卡迁移 🚧
- **类型**：文档（施工卡迁移）
- **分支**：`claude/d2-pages`（基于含 #157 的新 main；B5 服务包 + B6 页面 9 卡 + 索引回填在此，PR-B 待开/待合）。B1→#156、B2-B4→#157
- **目标**：D2 域 30 卡（23 原 ID + 7 页面）搬成自包含施工卡 —— **全部已建**（B1 #156 / B2-B4 #157 / B5-B6 PR-B）。合批省推送共 3 个 PR
- **全域进度**：D0 ✅(#152/#153) · D1 ✅(#154) · **D2 🚧 全 30 卡已建（B1✅#156 + B2-B4✅#157 + B5-B6 PR-B 待合）** · D3/D4/D5/D6/wave2/ga ⬜
- **当前批 PR-B**（B5 服务包 + B6 页面）：SVC-PILOT-01/02/03 · SVC-INTEGRATION-01 / IMPL-01 · TENANT-01 · CFGPKG-01 · DICTMAP-01 · ADAPTER-01（规则库=RULE-01、路径配置=PATH-01 已于 #157 含页）
- **状态**：#156/#157 已合并；**B5+B6 共 9 卡已建**（SVC 现状基于 `engine/{tenant,org,mpi,integration,pkg}` 真实类名；页面卡路由元数据用 `frontend/src` 真实 path/menuKey/组件 `pages/tenant/*`，现状＝页面已存在待真实化，含清 `/config/packages/demo` 演示路由）、**_index（D2 目录 ✅ 30 卡 + 场景行）+ _coverage-matrix（已迁 87→101，D2 完域场景锚点已迁，D2 标 ✅）+ backlog（🗂️ 全 30 卡 + D2-PAGE 实化）已回填**；待提交 → 开 PR-B → 合并
- **B1 后端现状（已核实 2026-05-30，建卡「现状」段照此写、勿凭空）**：
  - **API-01 标准上下文 API**：已有实质基础——`engine/context/` 下 ContextSnapshot(+Response/Filter)、PackageVersionPort、CanonicalResource(Type/Repository)、ContextValidator、ClinicalEventController 等；卡为"真实化/契约化/补全"非从零。
  - **OPT-01 FHIR R4/R5 门面**：基本待建——无 FHIR 资源门面（仅 `engine/integration` adapter DTO 提及 fhir）；现状是项目自有 `CanonicalResource` 抽象；卡＝新建 FHIR 门面并映射到 CanonicalResource。
  - **SYS-04 版本继承与发布框架**：待建——无统一 publish/inheritance 框架（散落各处）。
  - **SYS-08 权威知识版本解析/原子替换**：部分已有——`engine/knowledge/` 有 KnowledgeVersionService/Controller/Status、KnowledgeAssetVersion；卡聚焦"唯一有效约束/替代链/紧急失效/原子替换"框架化（与 B2·KNOW-02 划清单一归属）。
- **下一步**（精确动作）：
  1. ✅ B5+B6 共 9 卡已建、_index/_coverage-matrix/backlog 已回填（本分支 `claude/d2-pages`）
  2. 提交 → 推送 → 开 **PR-B**（`docs(cards): D2 B5~B6 服务包+7页面 9 卡 + 索引回填（D2 收官）`）→ 请用户合并
  3. 合并后 **D2 卡全建** → 走 D2 域级验收（[质量基线 §2.3]）→ 从新 main 起 D3 临床运行迁移
- **后续批**：D3 临床运行（API-02/API-07/CDSS-01/EMBED-01/FOLLOW-01/患者主索引等）→ D4 → D5 → D6 → wave2 → ga
- **遗留 sweep（不急）**：B1-B4 卡对现已建同域卡仍有 `../_index.md` 间接引用（可达、不算错）；D2 全合并后做一次「间接→直链」升级 pass
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
