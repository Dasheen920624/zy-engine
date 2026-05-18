> ⚠️ **本文档已被 `产品设计V2/` 体系替代，仅供 git 历史追溯，禁止作为实施依据。**
>
> 请阅读：
> - [产品设计V2/README.md](产品设计V2/README.md)
> - [产品设计V2/01_产品事实源.md](产品设计V2/01_产品事实源.md)
> - [产品设计V2/02_场景剧本图.md](产品设计V2/02_场景剧本图.md)
> - [产品设计V2/03_设计系统.md](产品设计V2/03_设计系统.md)
> - [产品设计V2/04_页面规格书.md](产品设计V2/04_页面规格书.md)
> - [产品设计V2/05_AI实施手册.md](产品设计V2/05_AI实施手册.md)
>
> 如本文与 V2 冲突，永远以 V2 为准。
>
> ---

# AI 自主开发运行守则

## 1. 目标

本文定义当用户授权 AI 自主开发时，AI 应如何自动选择任务、连续推进、控制风险、记录运行过程、在额度耗尽或遇到阻塞时安全交接。

本守则不替代以下机制，而是把它们串成可执行循环：

```text
AI任务认领与并行开发机制：防止撞车。
AI开发质量门禁与评审整改机制：防止低质量提交。
数据库Provider与离线AI开发约定：保证无 Oracle 也能开发和验证。
顶级多角色评审与AI并行开发总控：确定优先级、泳道和角色边界。
```

当用户说“开始任务”“自主开发”“默认同意开发”“直到没有额度”时，AI 默认进入本文定义的自主运行模式。

## 2. 自主运行核心原则

- 用户已默认同意常规开发、测试、文档更新和本地验证，不需要反复征求意见。
- AI 可以自主选择当前最高价值、最低冲突、最小可交付的任务切片。
- AI 必须先认领任务，再改业务代码。
- AI 必须先通过质量门禁，再正式提交业务代码。
- AI 必须优先修复已存在的 `CHANGES_REQUESTED` 质量问题，再领取新功能。
- AI 必须保持主版本可运行，不允许把半成品、未评审、未验证代码推进主版本。
- AI 必须在额度或时间接近耗尽时优先整理交接，而不是继续开新坑。
- AI 遇到不可逆、生产级、医学责任、真实凭据、真实患者数据等高风险决策时必须停下说明风险。

## 3. 自主运行目录

自主运行记录放在：

```text
ai-dev-input/12_autonomous_runs/
  README.md
  run_log_template.md
  active/
  archive/
```

运行记录不是任务锁，任务锁仍以 `10_task_claims` 为准。运行记录用于回答：

```text
本轮 AI 为什么选这个任务？
做过哪些 claim？
跑过哪些验证？
遇到哪些阻塞？
下一个 AI 应从哪里接手？
```

## 4. 启动前检查

自主运行开始后，AI 必须先执行：

```powershell
git status -sb
git log -1 --pretty=format:%H%n%s
rg -n "claim_id:|task_id:|write_scope:|status:" ai-dev-input/10_task_claims
rg -n "review_id:|review_status:|status: OPEN|CHANGES_REQUESTED|open_findings" ai-dev-input/11_ai_reviews
rg -n "run_id:|status: ACTIVE|current_claim" ai-dev-input/12_autonomous_runs
.\zy-engine-mvp\scripts\detect-db-env.ps1 -BootstrapLocal
```

然后阅读：

```text
zy-engine-mvp/docs/AI接手执行手册.md
zy-engine-mvp/docs/AI自主开发运行守则.md
zy-engine-mvp/docs/AI任务认领与并行开发机制.md
zy-engine-mvp/docs/AI开发质量门禁与评审整改机制.md
zy-engine-mvp/docs/数据库Provider与离线AI开发约定.md
zy-engine-mvp/docs/顶级多角色评审与AI并行开发总控.md
```

## 5. 任务选择优先级

当用户没有指定任务时，AI 按以下顺序选择：

1. 修复已存在的 P0/P1/P2 `CHANGES_REQUESTED` review。
2. 接管超过 24 小时无心跳且明确可接管的 `STALE/HANDOFF` claim。
3. 推进 `顶级多角色评审与AI并行开发总控.md` 的“当前最高优先级执行顺序”。
4. 推进 `产品化方案与AI开发编排.md` 的“当前优先任务池”。
5. 补测试、smoke、文档、样例和 Oracle/H2 一致性缺口。
6. 做低风险、可独立提交、可快速验证的小切片。

选择任务时必须满足：

- 写入范围不与 active claim 重叠。
- 可以在一个清晰切片内交付。
- 有明确验收方式。
- 不需要用户提供额外真实凭据或生产权限。
- 不依赖当前 AI 无法访问的内网系统，或已有 LOCAL_H2 等价验证路径。

## 6. 默认任务拆分粒度

自主开发时，单个 claim 应尽量满足：

```text
一个业务目标。
一个模块或一个清晰横切点。
一个主要服务/接口/页面/DDL 切片。
可在本轮完成测试和文档。
```

不建议单个 claim 同时做：

```text
后端 + 前端 + DDL + 权限 + 运维脚本 + 大量文档重写
```

如果用户要求“大量开发”，AI 应拆成多个连续 claim，而不是一个超大 claim。

## 7. 自主开发循环

标准循环：

```text
1. 启动检查。
2. 创建或更新 autonomous run log。
3. 选择任务切片。
4. 创建并推送 claim。
5. 识别数据库环境。
6. 实现最小可交付增量。
7. 补测试、样例、API 示例和文档。
8. 执行验证。
9. 创建 review。
10. 整改质控问题。
11. review approved 后正式提交。
12. 归档 claim、review、run log。
13. 若仍有额度，回到第 3 步。
```

每完成一个任务后，AI 可以自动继续下一个任务，不需要询问用户，除非触发停机条件。

## 8. 停机条件

AI 必须停止继续开发并交接的情况：

- 需要生产库、真实 Oracle、真实医院内网、真实密钥或真实患者数据才能继续。
- 即将执行删除、覆盖、不可逆迁移、批量数据变更、强制推送等高风险动作。
- 发现 P0/P1 医疗安全、数据破坏、权限越权或主版本不可运行风险。
- 同一问题连续修复两轮仍无法通过核心验证。
- 工作树出现无法判断来源的相关改动。
- active claim 或 write scope 与其它 AI 冲突。
- 需求出现两种以上合理方案且会影响长期架构。
- 剩余额度不足以完成“实现 + 验证 + review + 交接”闭环。
- 本轮只能留下半成品且没有安全回滚点。

停机时必须：

- 不开新任务。
- 更新当前 claim 为 `BLOCKED`、`HANDOFF` 或 `CHANGES_REQUESTED`。
- 更新 autonomous run log。
- 写明已完成内容、未完成内容、验证结果、风险和下一步。

## 9. 不需要停机的情况

以下情况默认继续处理，不需要问用户：

- 常规代码实现、测试、文档、样例、脚本更新。
- 在 LOCAL_H2 下做无 Oracle 等价验证。
- 补充缺失的 API 示例、README 或任务卡。
- 解决普通编译错误、单元测试失败、格式问题。
- 追加非破坏性 DDL 文件、样例 JSON 或 smoke 脚本。
- 创建 claim、review、run log 和归档记录。

## 10. 分支和提交策略

推荐：

```text
main                         已审核主版本 + 协作元数据
codex/<claim_id>-<short>     业务开发分支
```

若当前项目暂时直接在 `main` 协调：

- claim、review、run log 可同步到 `main`。
- 业务代码必须通过质量门禁后再正式提交。
- 每次提交只包含一个任务切片相关文件。
- 不把多个未评审任务混成一个提交。

提交前必须确认：

```powershell
git status -sb
git diff --stat
git diff --cached --stat
```

## 11. 额度不足时的安全交接

当 AI 估计剩余额度不足时，应立刻切换到收尾模式：

1. 停止新增功能。
2. 保存当前已完成的最小安全改动。
3. 跑能跑的最快验证，例如 `git diff --check` 或相关单测。
4. 更新 claim 的 progress/handoff。
5. 更新 review 状态，未完成评审则标记 `REVIEW_REQUESTED` 或 `CHANGES_REQUESTED`。
6. 更新 run log 的 `next_action`。
7. 最终回复说明当前状态、未提交/未推送原因和下一步。

禁止在额度不足时：

- 开新模块。
- 做大范围重构。
- 修改 DDL 后不验证。
- 改发布、权限、医学规则等高风险逻辑但不留交接。

## 12. 数据和安全红线

AI 自主运行时禁止：

- 提交真实数据库密码、Dify API Key、医院 VPN 信息或内网凭据。
- 使用真实患者姓名、证件号、电话、住址、病历号等隐私明文做样例。
- 在日志、README、review 或 claim 中暴露密钥和真实患者数据。
- 执行生产库 DDL/DML 或清理脚本，除非用户明确授权且有备份/回滚说明。
- 绕过医生确认或医学/医保/质控审核，把 AI 建议直接变成强制处置。
- 把无法验证的医学结论写成确定性事实。

## 13. 自主运行 Definition of Done

一次 autonomous run 完成时必须留下：

- 一个 run log。
- 本轮创建或接管的 claim 列表。
- 本轮创建或处理的 review 列表。
- 每个任务的验证结果。
- 是否提交、提交 hash、是否推送。
- 当前数据库模式。
- Oracle/LOCAL_H2 验证状态。
- 未完成事项和下一步。

## 14. 给后续 AI 的最小交接

每个 autonomous run 最后必须写清：

```text
current_state:
completed_claims:
active_claims:
pending_reviews:
changed_files:
validated_commands:
failed_commands:
database_mode:
oracle_status:
local_h2_status:
risks:
next_action:
do_not_touch:
```

这保证后续 AI 不需要重新猜上下文，可以从明确位置继续推进。
