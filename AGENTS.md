# MedKernel AI 协作规则（AGENTS.md）

> 本文件＝任何 AI 工具（Claude Code / Codex / Cursor / Copilot / Gemini）与人类协作者在本仓库的**中立权威**，管「怎么协作」。产品「做什么 / 不变量」以 [产品宪法 CONSTITUTION](docs/CONSTITUTION.md) 为准。
> **开工第一件事**：读 [docs/_HANDOFF.md](docs/_HANDOFF.md) 续接在途工作线，别 git 考古。

## §0 总则

- 适用：所有 AI 工具与人类协作者；各工具入口（CLAUDE.md / GEMINI.md 等）仅转交至本文件，不重复维护。
- 分工：本文件管「怎么协作」，[CONSTITUTION](docs/CONSTITUTION.md) 管「做什么 / 产品不变量」。**冲突时 CONSTITUTION（质量红线）赢。**
- 优先级：**用户明确指令 ＞ 本文件 / 技能 ＞ 工具默认行为。**
- 活文档：本文件规则的增改也走 PR（同 §6）。

## §1 语言要求

- 所有新增 / 修改的当前有效文档用**简体中文**；不新增以英文为主的 README / 设计 / 计划 / OpenSpec / PR / Issue / 运行手册 / AI 协作说明。
- 代码标识符、接口路径、命令、配置键、库字段、标准英文缩写、第三方产品名、必要原文可保留英文，但须中文解释业务含义。
- 引擎层（`com.medkernel.engine.**` / `com.medkernel.shared.**`）公共类·方法 Javadoc，及迁移脚本新增表 / 枚举 / 状态 / 外键列 `COMMENT ON` 必须简体中文（CI `scripts/check-comment-zh.sh` 软门禁兜底）。
- 项目按未上线的新项目运行：不保留旧版本归档、旧任务锁、旧协作模板、旧分支口径；`openspec/archive/` 仅作审计追溯，非当前事实源。详 [DOCUMENTATION_LANGUAGE_POLICY](docs/DOCUMENTATION_LANGUAGE_POLICY.md)。

## §2 六条总纲（贯穿所有场景）

1. **质量优先** — 产品质量是一切前提；§9 红线绝不因赶工 / 省 token 妥协。
2. **真实诚实·证据优先** — 以真实仓库状态为准（grep 核实、不臆造端点 / 类 / 进度）；如实报告（失败说失败、跳过说跳过、完成才说完成）；宣称完成必附证据，不空口。
3. **安全与诚实降级优先** — 医疗安全高于功能；无模型 / 无连接 / 无图一律诚实状态 + 真实主链路（B0 先于模型）；红线见 §9。
4. **单一真相源·文档即合同·工具中立** — CONSTITUTION / 域简报 / 卡为权威，文档与代码同 PR；_HANDOFF 为跨会话跨工具接力真相源（开工读、收尾更新）；不依赖任一工具私有记忆。
5. **Token 经济（常态）** — 最少 token 做最全的事：定位不通读（grep / 索引）、批量少往返（独立操作并行 / 合并 PR / 不重读已写文件）、紧凑填全（套模板、N·A 不留空、表优于长段）、主线能做不起 subagent。**省冗余，不省质量与完整性。**
6. **重构优先·全新高标准** — 未完成任务按全新设计做，以卡（FR / AC / 11 视角）+ 11 铁律 + 最新标准为基准；旧低质量码不沿用不将就：达标的复用、不达标的重构到位（提到卡标准 + 过 T-GATE），不在烂地基加层；只重构实际低质处，不为重构而重构、不扩无关范围。

## §3 文档权威顺序与真相源

- 权威序：**核心 CONSTITUTION ＞ 域简报 `cards/<域>/_brief.md` ＞ 施工卡 `cards/<域>/<ID>.md`**；页面卡另读 [EXPERIENCE_CONTRACT](docs/EXPERIENCE_CONTRACT.md)。冲突裁决：核心赢；卡间冲突＝分区错误，修分区而非裁决。
- 真相源指针（按需查，不通读）：接力 [_HANDOFF](docs/_HANDOFF.md) · 任务 / 状态 [backlog](docs/backlog.md) · 找卡 [cards/_index](docs/cards/_index.md) · 覆盖 [_coverage-matrix](docs/cards/_coverage-matrix.md) · 验收 [质量基线](docs/audit/质量基线.md) · 名词 [glossary](docs/glossary.md)。
- 旧巨物（详规 / 落地规划 / FOUNDATION / 体验规范）P8 前物理保留但**不权威**，以核心 + 卡为准。

## §4 通用工作循环（所有场景骨架）

1. **读 _HANDOFF 续接** —— 按工作线拿「下一步」，不考古。
2. **现状核查** —— `grep` / `find` / `git diff` 对照 `frontend/src`、`medkernel-backend/src`，不信单次 read、不臆造端点 / 类（曾因伪造 read 整批返工）。
3. **设计先行** —— 创作类（新功能 / 组件 / 改行为）先出设计 / spec 再动手，别直接堆码。
4. **按权威序读最少去做**（§3）。
5. **自检 + 证据** —— 跑测试 + T-GATE，证据优先、不空口宣称「已修复 / 已通过」。
6. **收尾更新 _HANDOFF** —— 状态 / 下一步 / 归档。
- 中断后找散落改动：`git worktree list` + `git status` + `git log origin/main`。

## §5 分场景规约（一行一规则 + 指针，不复制细节）

| 场景 | 必守 | 详见 |
|---|---|---|
| 需求 / 设计 | 创作类先设计后码 · YAGNI · 单一职责 / 小而可测单元 | §4 |
| 开发 | TDD（先写失败测试 → 实现 → 绿，动手前建绿色基线）· B0 先于模型 · 单一归属（不重造）· 配置外置（不写死 yml）· 标准契约（ApiResult / ProblemDetail / Record DTO + Bean Validation / traceId / 幂等） | 核心 §1·§11 |
| 数据 / 迁移 | 5 方言（h2 / postgres / oracle / dm / kingbase）一致 + 中文 COMMENT + 索引约束 + 组织 / 版本 / 审计字段 | 核心 §12 |
| 产品体验 | 一页一目标 · 角色默认视图 · 六态 · 主按钮 ≤1 · 默认筛选 ≤3 · 低打扰嵌入 · 服务端分页 · 技术对象藏专家模式 | [EXPERIENCE_CONTRACT](docs/EXPERIENCE_CONTRACT.md) |
| 集成 / 互操作 | FHIR / CDS Hooks 风格门面 · 适配器 · Webhook 签名 · 字段映射 · 健康检查 · 重试死信 · 断连诚实降级（NOT_CONNECTED）+ 证据 | 核心 §10 |
| 性能 / NFR | 10 万级列表服务端分页 · P95 基线 · 并发幂等 · 可用性 · 5 方言一致 | [SYS-07](docs/cards/ga/SYS-07.md) |
| 测试 | 真实不造数 · 六态全覆盖 · 单测 / 契约 / E2E + 关键用例 · 不写死医学常量 | [质量基线](docs/audit/质量基线.md) |
| 真实性门禁 T-GATE | 前端 `no-page-mock`（阻断 mock / `eslint-disable` / 写死医学常量 / JSON 裸渲染）+ stylelint 阻断 hex；后端阻断 Math.random 造数 / catch 吞错返成功 / UUID 充 hash / 占位 Javadoc。前后端全绿才可 done | [INFRA-01](docs/cards/D0/INFRA-01.md) / 02 |
| 调试 | 系统化：先复现 + 定位根因再改，不瞎试 / 不吞错 / 最小可复现 | — |
| 代码评审 | 请求 / 接收都给证据 + 技术核实；不盲改、不表演式同意 | — |
| 验收 | 11 验收铁律 + 证据清单（代码 permalink / 测试 / T-GATE / 文档锚点 / A1–A9 剧本 / 审计员签字 owner≠reviewer）+ 域级验收 + GA 门禁 | [质量基线](docs/audit/质量基线.md) |
| 文档 / 施工卡 | 套 [_template](docs/cards/_template.md) · 11 视角填全或 N·A · FR↔AC 一一对应 · 单一归属 · 零死链 · 四索引回填（_index / _coverage-matrix / backlog / _HANDOFF）· commit 引锚点 · 整批一 PR | [_template](docs/cards/_template.md) |
| 运维 / SRE / 国产化 | 国产化 profile 自检 · 备份恢复 RPO-RTO · provider / 连接器无连接诚实（NOT_CONNECTED）· 监控健康检查 | 核心 §12 |
| 审计 / 核查 | 主线广度优先；逐单元独立 subagent 太费 token，慎用 | — |
| 安全（研发行为） | 仅授权的防御性 / 本仓研发，不产破坏性手法；密钥 / 凭证不落明文 / 不入日志 / 不进库；患者数据最小化、脱敏后外调 | §8 |

## §6 Git / 分支 / PR / 合并

- **禁直推 `main`**；路径：分支 → 推送 → PR → CI 通过 → 合并 → 确认 `origin/main` 含合并提交。
- 分支前缀按工具：Claude Code `claude/`、Codex `codex/`……除非用户指定其他名。
- **一逻辑单元一 PR**；大任务分批，每批基于当时最新 `main`；**squash 合并后必须从新 `origin/main` 重拉**再做下一单元（否则基点回退、重复带入）。
- 远程长期分支只留 `main`（禁 develop / dev 第二主干）；本地清理已合并分支 / worktree。
- commit / PR 用中文，写清变更范围、验证结果、未完成事项、是否影响医疗安全 / 部署 / 数据迁移。文档与代码改动**同 PR**（核心硬约束 #12）。

## §7 会话接力（中断续接）

- **真相源**：[docs/_HANDOFF.md](docs/_HANDOFF.md) —— 所有在途工作线的活跃分支、状态、下一步精确动作；纯 markdown 在版本库，任何工具或人都能读写，不依赖某工具私有记忆。
- 开工先读它续接；收尾或预感中断时立刻更新对应工作线「状态」「下一步」，完成的线移入「已归档」。
- 开新工作线按 _HANDOFF 末尾模板加一条。
- 中断后找散落改动：`git worktree list` + 各 worktree `git status` 查未跟踪文件（改动曾停在未提交的 worktree）。

## §8 协作边界与安全（AI 行为）

- 难逆 / 外向操作（发布、删除、外部服务调用）先确认再做。
- 删除 / 覆盖前看清目标；与描述矛盾、或非自己所建的改动，上报不蛮干。
- 如实报告：失败说失败、跳过说跳过、完成才说完成；不伪造证据、不假装通过。
- 密钥 / 凭证不落明文、不入日志、不进版本库（走密钥管理）；患者数据最小化、脱敏后外调。
- 本仓库为授权医疗软件研发，全程恪守医疗安全（红线见 §9）。

## §9 质量红线（所有场景不可妥协）

指向 [CONSTITUTION](docs/CONSTITUTION.md) 11 验收铁律 / 20 硬约束，**token 经济绝不以牺牲它们为代价**：真实性 · 医疗安全（AI 内容标识 / 医师确认 / 高危双签 / 禁自动开嘱）· 诚实降级 · 关系库权威 · 唯一权威知识 · 五维权限 · 六态完整 · 中文优先 · 文档同步 · 配置外置 · 无模型可运行。
