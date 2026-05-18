# AI 任务认领目录

本目录用于多 AI 并行开发时记录任务锁、阻塞和归档状态。

正式机制见：

- [`../../docs/engineering/00_总入口与AI接手导航.md §2.1`](../../docs/engineering/00_总入口与AI接手导航.md) — 并发开发硬机制
- [`../../docs/05_AI实施手册.md §16`](../../docs/05_AI实施手册.md) — PR 协作与冲突处理

目录含义：

```text
active/   正在开发或短时等待的任务认领文件。
blocked/  被外部依赖阻塞的任务认领文件。
archive/  已完成、放弃或接管完成的任务记录。
```

质量评审记录放在：

```text
../11_ai_reviews/
```

认领前必须：

```powershell
git pull --ff-only origin main
git status -sb
rg -n "claim_id:|task_id:|write_scope:" ai-dev-input/10_task_claims
.\medkernel-mvp\scripts\check-ai-collaboration.ps1
```

没有成功提交并推送 `active/<claim_id>.md` 前，不允许修改业务代码。claim 中必须记录 `git_base_commit`、`git_status_at_claim`、写入范围、review 需求、功能验收需求和预计心跳间隔。

状态同步要求：

- 开发超过 60 分钟必须更新 `last_heartbeat` 并推送。
- 进入自检、评审、整改、放行、提交、归档时，claim、任务台账、review、功能验收记录必须同步。
- `git status -sb` 出现无法解释的本地改动时，不允许继续改业务文件。
- 发现写入范围重叠的 active claim 时，必须拆分、等待或创建集成任务，不得覆盖别人改动。

认领成功不代表可以把业务代码正式提交到主版本。完成开发后必须创建 review，开放问题为 0 且 `review_status=APPROVED` 后，claim 才能进入 `READY_TO_SUBMIT` 和 `DONE`。客户可见、医学/医保/质控、数据库持久化、前端页面、发布同步、安全权限功能还必须创建 `../13_feature_acceptance/` 验收记录。
