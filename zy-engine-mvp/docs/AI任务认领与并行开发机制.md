# AI 任务认领与并行开发机制

## 1. 目标

本文定义多 AI 同时开发时的任务认领、锁定、心跳、交接、冲突处理和归档机制。所有 AI 开始开发前必须先完成任务认领，避免多个 AI 重复执行同一任务或同时修改同一文件。

本机制使用 Git 仓库作为协调源，不依赖额外服务。认领文件一旦成功提交并推送，即视为任务锁生效。

认领机制只解决“谁在做”和“避免撞车”，不代表实现质量已通过。业务代码正式提交、合并或进入主版本前，还必须执行 `AI开发质量门禁与评审整改机制.md`。

## 2. 核心原则

- 一个 AI 同一时间只认领一个明确任务切片。
- 认领粒度必须小于总任务，不能只写 `PROV-001`、`RULE-001` 这类大任务。
- 任务切片必须有唯一 `claim_id`，例如 `PROV-001-S03`、`FE-003-S01`。
- 认领成功以 claim 文件推送到远端为准，本地创建但未推送不算锁定。
- 每个 claim 必须声明写入范围，未声明的文件默认不能改。
- 两个 active claim 的写入范围不得重叠；确需重叠时必须先拆分或由一个 AI 做集成。
- 完成、放弃、阻塞、交接都必须更新 claim 文件，不能只在聊天里说明。
- 开发完成后必须进入质量评审；未获得 `APPROVED` review 的任务不得归档为 `DONE`。
- claim、heartbeat、review 等协作元数据可以同步到 `main`，但业务代码进入主版本必须先通过质量门禁。
- 发现别人的 active claim 后，不抢占、不覆盖、不回滚。

## 3. 目录约定

任务认领文件统一放在：

```text
ai-dev-input/10_task_claims/
  README.md
  task_claim_template.md
  active/
  blocked/
  archive/
ai-dev-input/11_ai_reviews/
  README.md
  review_template.md
  pending/
  changes_requested/
  approved/
  archive/
```

状态含义：

```text
active/    正在开发或短时等待的任务锁。
blocked/   被外部依赖阻塞、暂时不继续写代码的任务。
archive/   已完成、已放弃或已被接管完成的任务记录。
```

active 目录下每个任务切片只能有一个文件：

```text
ai-dev-input/10_task_claims/active/<claim_id>.md
```

示例：

```text
ai-dev-input/10_task_claims/active/PROV-001-S03.md
```

## 4. claim_id 规则

格式：

```text
<任务编号>-S<两位序号>
```

示例：

```text
PKG-004-S01
PROV-001-S03
RULE-008-S02
FE-003-S01
DOC-001-S01
```

切片序号含义由任务认领文件描述。禁止多个 AI 自行使用同一个 `claim_id`。

如果总任务还没有拆分，先由认领 AI 创建第一个清晰切片，例如：

```text
PROV-001-S01 来源表 DDL 草案
PROV-001-S02 来源样例与 API 契约
PROV-001-S03 来源 review 阻断测试
```

## 5. 认领前检查

每个 AI 开始前必须执行：

```powershell
git pull --ff-only origin main
git status -sb
rg -n "claim_id:|task_id:|write_scope:" ai-dev-input/10_task_claims
.\zy-engine-mvp\scripts\check-ai-collaboration.ps1
```

然后检查：

- 是否已有相同 `claim_id`。
- 是否已有相同 `task_id` 且状态为 `ACTIVE`。
- 是否已有 active claim 的 `write_scope` 与本任务重叠。
- 当前工作树是否已有别人未提交或当前线程遗留改动。
- 当前任务是否属于总控文档中的某条并行泳道。

如果发现冲突：

- 相同 `claim_id`：不能继续，必须换任务或拆新切片。
- 相同 `task_id` 但写入范围不同：可以继续，但必须使用新的 `claim_id`，并在 claim 中写清依赖关系。
- 写入范围重叠：不能继续，需要拆分、等待、或认领集成任务。
- 本地已有无关改动：忽略但不回滚。
- 本地已有相关改动：兼容现有改动继续，claim 中记录“接续已有本地改动”。
- `git status -sb` 不是干净状态且无法说明来源：不能开始修改，必须先交接或请用户确认。

## 6. 认领流程

认领必须先提交并推送 claim 文件。

步骤：

```powershell
git pull --ff-only origin main
Copy-Item ai-dev-input/10_task_claims/task_claim_template.md ai-dev-input/10_task_claims/active/<claim_id>.md
# 编辑 claim 文件，填完整 task_id、owner、write_scope、DoD
git add ai-dev-input/10_task_claims/active/<claim_id>.md
git commit -m "认领<claim_id>任务"
git push origin main
```

如果 `git push` 被拒绝：

```powershell
git pull --ff-only origin main
rg -n "<claim_id>|<task_id>|<关键写入文件>" ai-dev-input/10_task_claims/active
```

重新检查是否已有其它 AI 抢先认领。若冲突存在，必须停止当前任务，换任务或重新拆分；若无冲突，再重新提交推送。

## 7. claim 文件必填字段

每个 claim 必须包含：

```text
claim_id:
task_id:
slice:
title:
owner:
role:
status:
branch:
created_at:
last_heartbeat:
expected_finish:
git_base_commit:
git_status_at_claim:
feature_acceptance_required:
feature_acceptance_id:
write_scope:
read_scope:
forbidden_scope:
dependencies:
acceptance:
verification:
database_mode:
oracle_available:
local_db_verified:
oracle_verification_required:
review_required:
review_id:
review_status:
reviewer:
open_findings:
quality_gate:
handoff:
```

字段要求：

- `owner` 使用可读的 AI 标识，例如 `AI-Codex-20260516-frontend-01`。
- `status` 只能是 `ACTIVE`、`IN_PROGRESS`、`SELF_CHECK`、`REVIEW_REQUESTED`、`CHANGES_REQUESTED`、`FIXING`、`APPROVED`、`READY_TO_SUBMIT`、`BLOCKED`、`HANDOFF`、`DONE`、`ABANDONED`、`STALE`。
- `write_scope` 必须精确到目录或文件，尽量精确到文件。
- `forbidden_scope` 写明不能碰的模块，防止顺手扩大范围。
- `verification` 写本任务必须跑的脚本。
- `database_mode` 写 `ORACLE`、`LOCAL_H2` 或 `IN_MEMORY`。
- `oracle_verification_required` 写清是否需要后续 Oracle smoke。
- `review_required` 写 `true/false`；高风险任务必须为 `true`。
- `review_id` 必须引用 `ai-dev-input/11_ai_reviews` 下的评审记录。
- `review_status` 写 `NOT_REQUESTED`、`REVIEW_REQUESTED`、`CHANGES_REQUESTED`、`APPROVED` 或 `REJECTED`。
- `open_findings` 写开放问题数量；通过评审必须为 `0`。
- `quality_gate` 写明是否允许正式提交。
- `feature_acceptance_required` 写明是否需要逐功能验收；客户可见、医学/医保/质控、数据库、前端、发布同步、安全权限任务必须为 `true`。
- `feature_acceptance_id` 引用 `ai-dev-input/13_feature_acceptance/` 下的验收记录。
- `handoff` 记录未完成事项、风险和下一步。

## 8. 心跳机制

长任务必须维护心跳。

规则：

- 开发超过 60 分钟仍未完成，应更新 `last_heartbeat` 并推送。
- 每次切换大方向、发现阻塞、开始验证前，应更新 claim。
- 心跳只更新当前 claim 文件，不混入业务代码。

心跳提交示例：

```powershell
git add ai-dev-input/10_task_claims/active/<claim_id>.md
git commit -m "更新<claim_id>任务心跳"
git push origin main
```

超时规则：

```text
超过 4 小时无心跳：视为可能 STALE，后续 AI 不得直接改同一文件范围，应先认领接管/协调任务。
超过 24 小时无心跳且无对应提交：可由新的 AI 创建接管 claim，并在 handoff 中记录接管原因。
```

## 8.1 Git 状态同步机制

为避免“任务做了但台账没更新”或“git 状态滞后”：

- 开发前：claim 必须已经推送到远端；`git status -sb` 结果写入 claim。
- 开发中：超过 60 分钟必须更新 `last_heartbeat`；状态变化必须同步到 claim。
- 自检前：claim 状态改为 `SELF_CHECK`，记录实际 diff 范围。
- 发起评审前：claim 状态改为 `REVIEW_REQUESTED`，review 文件写入 `pending/`。
- 提交前：claim 必须是 `READY_TO_SUBMIT`，review 必须 `APPROVED` 且 `open_findings=0`。
- 推送后：claim、任务台账、功能验收记录必须写入 commit hash，再归档。

任何一个状态对象缺失或不同步，都视为并发风险，不允许口头宣布完成。

## 9. 阻塞与交接

任务暂时不能继续时：

1. 将 claim 文件从 `active/` 移到 `blocked/`。
2. 状态改为 `BLOCKED`。
3. 写清阻塞原因、已完成内容、下一步和风险。
4. 提交并推送。

命令示例：

```powershell
git mv ai-dev-input/10_task_claims/active/<claim_id>.md ai-dev-input/10_task_claims/blocked/<claim_id>.md
git commit -m "阻塞<claim_id>任务"
git push origin main
```

需要另一个 AI 接手时：

- 原 owner 将状态改为 `HANDOFF`，写清 handoff。
- 新 AI 创建新的 `claim_id`，例如 `PROV-001-S03B`。
- 新 claim 的 `dependencies` 必须引用原 claim。
- 新 AI 不删除原 claim，除非完成归档时一起说明。

## 10. 质量评审与整改

开发完成后不能直接标记 `DONE`。必须先进入质量门禁：

1. Builder AI 将 claim 状态改为 `SELF_CHECK`，补齐自检和验证结果。
2. 创建 `ai-dev-input/11_ai_reviews/pending/<review_id>.md`。
3. 将 claim 状态改为 `REVIEW_REQUESTED`。
4. Reviewer AI 审查并给出 `APPROVED` 或 `CHANGES_REQUESTED`。
5. 若有问题，Builder AI 将 claim 改为 `FIXING` 并逐条整改。
6. 复评通过后，claim 改为 `APPROVED`，再改为 `READY_TO_SUBMIT`。

质量门禁要求以 `AI开发质量门禁与评审整改机制.md` 为准。未通过评审的业务代码不得正式提交、合并、发布或归档为完成。

## 11. 完成与归档

任务完成后必须：

1. 跑完 claim 中写明的验证。
2. 通过质量评审，且 `review_status=APPROVED`、`open_findings=0`。
3. 更新 claim 状态为 `READY_TO_SUBMIT`。
4. 正式提交或合并本任务业务代码。
5. 更新 claim 状态为 `DONE`。
6. 填写完成内容、验证结果、review_id、提交 hash、剩余风险。
7. 将 claim 文件移到 `archive/YYYYMMDD/`。
8. 将对应 review 文件移到 `ai-dev-input/11_ai_reviews/archive/YYYYMMDD/`。
9. 提交并推送归档记录。

示例：

```powershell
New-Item -ItemType Directory -Force ai-dev-input/10_task_claims/archive/20260516
git mv ai-dev-input/10_task_claims/active/<claim_id>.md ai-dev-input/10_task_claims/archive/20260516/<claim_id>.md
git commit -m "归档<claim_id>任务"
git push origin main
```

如果任务只完成了一部分，不能标记 `DONE`；应标记 `HANDOFF` 或拆出新的后续 claim。

## 12. 冲突处理

常见冲突和处理：

```text
两个 AI 同时认领同一 claim_id
-> 先 push 成功者有效，后 push 失败者必须换任务或拆新切片。

两个 AI 认领不同 claim_id 但 write_scope 重叠
-> 后认领者暂停，改为等待、拆分或认领集成任务。

业务代码冲突但 claim 不冲突
-> 说明 write_scope 写得不够精确，必须更新 claim 并由一个 AI 做集成。

某 AI 长时间无心跳
-> 超过 4 小时标记风险，超过 24 小时可接管，但必须保留原记录。

用户明确要求覆盖某个任务
-> 新 AI 创建接管 claim，记录用户指令和接管范围，不删除历史。
```

冲突解决优先级：

1. 远端已推送 claim 优先。
2. 更小写入范围优先。
3. 有明确用户指令的接管 claim 优先。
4. 已通过测试并推送的实现优先。
5. 不确定时由一个 AI 认领 `INT-xxx` 集成任务统一处理。

## 13. 共享文件规则

以下文件容易被多个 AI 同时修改，必须在 claim 中精确声明修改段落：

```text
README.md
zy-engine-mvp/README.md
zy-engine-mvp/docs/api-examples.http
zy-engine-mvp/docs/AI接手执行手册.md
zy-engine-mvp/docs/产品化方案与AI开发编排.md
zy-engine-mvp/docs/顶级多角色评审与AI并行开发总控.md
zy-engine-mvp/src/test/java/com/zyengine/EngineApiContractTests.java
zy-engine-mvp/src/main/java/com/zyengine/persistence/EnginePersistenceService.java
zy-engine-mvp/db/oracle/zyengine_core_ddl_with_comments.sql
ai-dev-input/04_database/oracle/core_ddl.sql
ai-dev-input/04_database/dm/core_ddl.sql
ai-dev-input/11_ai_reviews/*
```

共享文件修改约定：

- API 示例只追加当前任务相关区块。
- README 只改当前能力和验证说明。
- 测试文件只新增当前任务测试，不重排旧测试。
- DDL 只追加当前任务表、字段、索引和备注，不格式化全文件。
- 总控文档只更新当前任务状态，不重写已定路线图。

## 14. 分支策略

默认协调分支是 `main`，claim 和 review 元数据必须推送到 `main`，用于让所有 AI 看见任务锁和质量状态。

业务开发可以使用短分支：

```text
codex/<claim_id>-<short-title>
```

但必须满足：

- claim 仍先进入 `main`。
- review 记录进入 `main`。
- 开发分支定期从 `main` 同步。
- 合并前确认 active claim 中没有写入范围冲突。
- 合并前确认 review 已 `APPROVED` 且 `open_findings=0`。
- 合并后归档 claim 和 review。

如果项目暂时只允许直接推 `main`，仍必须先推 claim 和 review 元数据；业务代码正式提交前必须先通过质量门禁。认领成功不等于业务代码可以直接进入主版本。

## 15. 集成任务

当多个任务需要合并同一服务、同一 DDL 或同一前端路由时，应创建集成任务：

```text
INT-001-S01 合并来源追溯 DDL 与服务入口
INT-002-S01 合并前端路由和导航
```

集成任务只能由一个 AI 认领。集成 AI 的职责：

- 拉取所有相关提交。
- 解决冲突。
- 跑完整验证。
- 更新相关 claim 的 handoff 或 archive。
- 核对所有相关 review 已通过；未通过的任务不得合并。
- 在最终回复中列出合并来源和风险。

## 16. 接手 AI 最小流程

每个 AI 开始开发前必须按这个顺序执行：

```text
1. git pull --ff-only origin main
2. 阅读 00_总入口与AI接手导航
3. 查看任务台账目标行
4. 运行 check-ai-collaboration.ps1 和 git status -sb
5. 查看 active/blocked claim、pending review、feature acceptance 队列
6. 选择一个无冲突任务切片
7. 创建并推送 claim
8. 开发代码、测试、文档
9. 创建 review，完成质量评审和整改
10. 需要时创建功能验收记录
11. 评审和必要功能验收通过后正式提交
12. 完成后归档 claim、review 和功能验收记录
```

没有成功推送 claim 前，只允许阅读和规划，不允许修改业务代码。

## 17. 本机制的 Definition of Done

多 AI 并行时，每个任务最终必须留下：

- 一个 active 期间可查的 claim。
- 一个归档后的 claim。
- 一个对应的 review 记录。
- `review_status=APPROVED`。
- `open_findings=0`。
- 明确的写入范围。
- 通过的验证命令。
- 提交 hash 或无法提交原因。
- 需要功能验收时，有功能验收记录和质量等级。
- 剩余风险和下一步。

这套记录是后续 AI 判断“哪些任务已做、哪些任务正在做、哪些任务不能碰”的第一依据。
