# AI 任务认领目录

本目录用于多 AI 并行开发时记录任务锁、阻塞和归档状态。

正式机制见：

```text
../../zy-engine-mvp/docs/AI任务认领与并行开发机制.md
../../zy-engine-mvp/docs/AI开发质量门禁与评审整改机制.md
```

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
rg -n "claim_id:|task_id:|write_scope:" ai-dev-input/10_task_claims
```

没有成功提交并推送 `active/<claim_id>.md` 前，不允许修改业务代码。

认领成功不代表可以把业务代码正式提交到主版本。完成开发后必须创建 review，开放问题为 0 且 `review_status=APPROVED` 后，claim 才能进入 `READY_TO_SUBMIT` 和 `DONE`。
