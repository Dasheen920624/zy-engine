# AI 质量评审目录

本目录用于记录多 AI 并行开发时的评审、质控问题、整改和放行结果。

正式机制见：

```text
../../zy-engine-mvp/docs/AI开发质量门禁与评审整改机制.md
```

目录含义：

```text
pending/             等待评审或复评。
changes_requested/   有必须整改的问题，阻断正式提交。
approved/            已通过评审，可进入 READY_TO_SUBMIT。
archive/             已正式提交并归档的评审记录。
```

基本规则：

- 每个完成开发的 claim 至少对应一个 `review_id`。
- 未通过评审的业务代码不得正式提交或合入主版本。
- P0/P1/P2 开放问题必须修复或由 Reviewer 明确降级后才允许放行。
- 高风险任务不得由 Builder AI 自己批准。

常用查询：

```powershell
rg -n "review_id:|claim_id:|review_status:|severity:|status: OPEN" ai-dev-input/11_ai_reviews
```
