# MedKernel AI 协作规则（AGENTS.md）

> 任何 AI 工具（Claude Code / Codex / Cursor / Copilot / Gemini）与人类协作者的**中立权威**，管「怎么协作」；产品「做什么 / 不变量」见 [CONSTITUTION](docs/CONSTITUTION.md)。**开工先读 [docs/_HANDOFF.md](docs/_HANDOFF.md) 续接，别考古。**

## §0 总则
- 适用所有工具与人类；各入口（CLAUDE.md / GEMINI.md…）仅转交至此，不重复维护。
- 分工：本文件管「怎么协作」，CONSTITUTION 管「不变量」，**冲突宪法（§8 红线）赢**；优先级 **用户指令 ＞ 本文件 ＞ 工具默认**。
- 本文件是活文档，改规则也走 PR（§6）。

## §1 语言
当前有效文档 / PR / 注释用**简体中文**；代码标识符·接口·配置键·字段等可保留英文但须中文释义；引擎层（`com.medkernel.engine/shared.**`）公共 Javadoc 与迁移 `COMMENT ON` 必须中文。详 [语言规范](docs/DOCUMENTATION_LANGUAGE_POLICY.md) / 核心 §14。

## §2 六条总纲（贯穿所有场景）
1. **质量优先** — §8 红线绝不因赶工 / 省 token 妥协。
2. **真实诚实·证据优先** — grep 核实不臆造（端点 / 类 / 进度）；如实报告（失败说失败、跳过说跳过、完成才说完成）；宣称完成必附证据。
3. **安全与诚实降级优先** — 医疗安全高于功能；无模型 / 连接 / 图一律诚实状态 + 真实主链路（B0 先于模型）。
4. **单一真相源·工具中立** — CONSTITUTION / 域简报 / 卡为权威，文档与代码同 PR；_HANDOFF 跨工具接力，不依赖任一工具私有记忆。
5. **Token 经济（常态）** — 最少 token 做最全：grep 定位不通读、批量少往返（并行 / 合并 PR / 不重读已写）、套模板填全（N·A 不留空）、主线能做不起 subagent。**省冗余，不省质量。**
6. **重构优先·全新高标准** — 未完成任务按全新设计做（卡 + 11 铁律 + 最新标准）；旧低质码不将就：达标复用、不达标重构到位（过 T-GATE），不在烂地基加层、不扩无关范围。

## §3 权威序与真相源
- 权威序：**核心 CONSTITUTION ＞ 域简报 `cards/<域>/_brief.md` ＞ 卡 `cards/<域>/<ID>.md`**（页面卡另读 [体验契约](docs/EXPERIENCE_CONTRACT.md)）；冲突核心赢，卡间冲突＝修分区。旧巨物 P8 前保留但不权威。
- 指针（按需查，不通读）：[_HANDOFF](docs/_HANDOFF.md) 接力 · [backlog](docs/backlog.md) 任务 · [_index](docs/cards/_index.md) 找卡 · [覆盖矩阵](docs/cards/_coverage-matrix.md) · [质量基线](docs/audit/质量基线.md) 验收 · [glossary](docs/glossary.md) 名词。

## §4 工作循环（含会话接力）
读 [_HANDOFF](docs/_HANDOFF.md) 续接 → **现状核查**（grep / find / git diff 对照 `frontend/src`·`medkernel-backend/src`，不信单次 read、不臆造）→ 创作类**先设计后码** → 按 §3 权威序读最少去做 → **自检 + 证据**（测试 + T-GATE，不空口宣称）→ **收尾更新 _HANDOFF**（状态 / 下一步 / 归档，新线套其末尾模板）。中断后 `git worktree list` + `git status` + `git log origin/main` 找散落改动。

## §5 分场景规约（一行一规则 + 指针）

| 场景 | 必守 | 详见 |
|---|---|---|
| 需求 / 设计 | 创作类先设计后码 · YAGNI · 小而可测单元 | — |
| 开发 | TDD（先失败测试 → 实现 → 绿，动手前建绿基线）· B0 先于模型 · 单一归属 · 配置外置 · 标准契约（Record DTO + 校验 / ApiResult / traceId / 幂等）| 核心 §1·§11 |
| 数据 / 迁移 | 5 方言一致 + 中文 COMMENT + 索引约束 + 组织 / 版本 / 审计字段 | 核心 §12 |
| 产品体验 | 一页一目标 · 六态 · 主按钮 ≤1 · 默认筛选 ≤3 · 低打扰 · 服务端分页 · 技术对象藏专家模式 | [体验契约](docs/EXPERIENCE_CONTRACT.md) |
| 集成 / 互操作 | FHIR / CDS Hooks 门面 · 适配器 · Webhook 签名 · 健康检查 · 重试死信 · 断连诚实降级 | 核心 §10 |
| 性能 / NFR | 10 万级服务端分页 · P95 · 并发幂等 · 可用性 · 5 方言一致 | [SYS-07](docs/cards/ga/SYS-07.md) |
| 测试 | 真实不造数 · 六态全 · 单测 / 契约 / E2E · 不写死医学常量 | [质量基线](docs/audit/质量基线.md) |
| 真实性门禁 T-GATE | 前端 no-page-mock + stylelint 拦 hex；后端拦 Math.random / 吞错返成功 / UUID 充 hash / 占位 Javadoc；前后端全绿才 done | [INFRA-01](docs/cards/D0/INFRA-01.md) |
| 调试 | 先复现 + 定根因再改，不瞎试 / 不吞错 / 最小可复现 | — |
| 代码评审 | 给 / 收都附证据 + 技术核实，不盲改 / 不表演同意 | — |
| 验收 | 11 铁律 + 证据清单（permalink / 测试 / T-GATE / 锚点 / A1–A9 / owner≠reviewer）+ 域级 + GA 门禁 | [质量基线](docs/audit/质量基线.md) |
| 文档 / 施工卡 | 套 _template · 11 视角填全或 N·A · FR↔AC · 单一归属 · 零死链 · 四索引回填 · commit 引锚点 · 整批一 PR | [_template](docs/cards/_template.md) |
| 运维 / 国产化 | 国产化 profile 自检 · 备份恢复 · 无连接诚实（NOT_CONNECTED）· 监控健康检查 | 核心 §12 |
| 审计 / 核查 | 主线广度优先；逐单元 subagent 太费 token，慎用 | — |
| 安全 | 仅授权防御性研发；密钥 / 凭证不落明文 · 不入日志 · 不进库；患者数据脱敏 | §7 |

## §6 Git / PR
禁直推 `main`；分支 → 推送 → PR → CI → 合并 → 确认 `origin/main` 含合并提交。分支前缀按工具（`claude/` / `codex/`）。一逻辑单元一 PR；分批基于最新 `main`；**squash 后从新 `origin/main` 重拉**。远程只留 `main`，清理已合并分支 / worktree。commit / PR 中文写清范围 / 验证 / 未完成 / 医疗安全·部署·迁移影响；**文档与代码同 PR**。

## §7 协作边界与安全
难逆 / 外向操作（发布 / 删除 / 外调）先确认；删改前看清目标，矛盾或非己建则上报不蛮干；不伪造证据、不假装通过。密钥 / 凭证不落明文 · 不入日志 · 不进库；患者数据最小化、脱敏后外调。授权医疗研发，全程恪守医疗安全。

## §8 质量红线（不可妥协）
[CONSTITUTION](docs/CONSTITUTION.md) 11 铁律 / 20 硬约束，**token 经济绝不牺牲**：真实性 · 医疗安全（AI 标识 / 医师确认 / 高危双签 / 禁自动开嘱）· 诚实降级 · 关系库权威 · 唯一权威知识 · 五维权限 · 六态 · 中文优先 · 文档同步 · 配置外置 · 无模型可运行。
