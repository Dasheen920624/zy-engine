# AI 自主运行记录目录

本目录用于记录 AI 自主开发时的一轮运行过程、任务选择依据、claim/review 列表、验证结果和交接信息。

正式机制见：

```text
../../zy-engine-mvp/docs/AI自主开发运行守则.md
```

目录含义：

```text
active/   当前正在运行或尚未完成交接的自主开发记录。
archive/  已结束并完成交接的自主开发记录。
```

运行记录不是任务锁。任务锁仍以：

```text
../10_task_claims/
```

质量门禁仍以：

```text
../11_ai_reviews/
```

为准。

常用查询：

```powershell
rg -n "run_id:|status: ACTIVE|current_claim|next_action" ai-dev-input/12_autonomous_runs
```
