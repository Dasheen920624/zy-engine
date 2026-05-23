# Active Task Locks

本目录是多 AI 并行认领的 Git 级硬锁目录。

每个正在开发的任务必须创建且只创建一个锁文件：

```text
ai-dev-input/10_task_claims/active_locks/<task_id>.lock
```

锁文件必须和 `active/<claim_id>.md` 在同一个提交中创建，并推送到 `develop` 后才算认领成功。两个 AI 同时认领同一 `task_id` 时会同时添加同名锁文件，Git 在 pull / merge / push 阶段产生冲突，从而阻止重复认领继续进入开发。

锁文件模板：

```text
task_id:
claim_id:
owner:
branch:
git_base_commit:
created_at:
last_heartbeat:
```

完成、放弃或接管归档 claim 时，必须在同一提交中删除对应锁文件。历史记录由 git 保留，不需要把锁文件复制到 archive。

## 强制对账

以下情况会被 `medkernel-mvp/scripts/check-ai-collaboration.ps1 -Strict` 阻断：

- `active_locks/*.lock` 没有对应 active claim。
- 多个 active claim 使用同一个 `task_id`。
- 多个 active claim 的 `write_scope` 指向同一文件或父子目录。
- claim 的 `task_lock_path` 与 `<task_id>.lock` 不一致。
- lock 文件中的 `claim_id` / `task_id` 与 claim 不一致。

AI 领任务前必须先跑：

```powershell
.\scripts\verify-task-prereq.ps1 -TaskId <TASK-ID> -Level <junior|middle|senior>
```

提交前必须跑：

```powershell
.\scripts\verify-pr.ps1 -TaskId <TASK-ID>
```
