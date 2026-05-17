# AI 开发质量门禁与评审整改机制

## 1. 目标

本文定义多 AI 并行开发时的质量审查、问题提出、整改闭环和正式提交门禁。它和 `AI任务认领与并行开发机制.md` 配套使用：

```text
任务认领机制解决：谁在做、做哪块、哪些文件不能撞。
质量门禁机制解决：做得是否达标、有问题谁改、什么时候允许进入主版本。
```

任何 AI 完成功能后，不能只因为自己测试通过就直接视为可正式提交。必须经过本文定义的自检、评审、整改和放行流程。

## 2. 核心原则

- `main` 可以同步 claim、review、heartbeat 等协作元数据，但业务代码进入主版本必须有质量门禁记录。
- 开发 AI 不能自行批准高风险任务；医疗、医保、质控、权限、安全、数据库、发布链路、持久化链路必须由独立 Reviewer AI 或集成 AI 审查。
- 评审发现问题后，任务状态必须进入 `CHANGES_REQUESTED` 或 `FIXING`，不得标记 `DONE`。
- 原开发 AI 仍在线时，默认由原开发 AI 自行整改；原开发 AI 超时或不可用时，必须创建新的修复 claim 接管。
- 所有质控问题必须可追踪到 `review_id`、`claim_id`、文件位置、严重级别、整改验证和最终结论。
- 未关闭 P0/P1/P2 问题的任务不得正式提交、合并、发布或归档为完成。
- P3 建议项可以带风险说明延期，但必须记录 owner、原因和后续任务编号。

## 3. 状态机

任务 claim 的推荐状态流转：

```text
ACTIVE
-> IN_PROGRESS
-> SELF_CHECK
-> REVIEW_REQUESTED
-> CHANGES_REQUESTED
-> FIXING
-> REVIEW_REQUESTED
-> APPROVED
-> READY_TO_SUBMIT
-> DONE
```

异常状态：

```text
BLOCKED      外部依赖阻塞。
HANDOFF      需要交接。
ABANDONED    放弃任务。
STALE        超时无心跳，等待接管。
REJECTED     评审认为方向错误，需要重新拆任务或重做。
```

状态规则：

- `SELF_CHECK`：开发 AI 已完成自检，但尚未提交评审。
- `REVIEW_REQUESTED`：已创建 `ai-dev-input/11_ai_reviews/pending/<review_id>.md`。
- `CHANGES_REQUESTED`：Reviewer AI 已提出必须整改的问题。
- `FIXING`：开发 AI 正在按评审意见整改。
- `APPROVED`：评审通过，开放问题数为 0。
- `READY_TO_SUBMIT`：通过评审后，准备正式提交、合并或推送业务代码。
- `DONE`：正式提交完成，claim 和 review 均已归档。

## 4. 目录约定

质量评审记录统一放在：

```text
ai-dev-input/11_ai_reviews/
  README.md
  review_template.md
  pending/
  changes_requested/
  approved/
  archive/
```

目录含义：

```text
pending/             等待评审或复评的记录。
changes_requested/   已提出必须整改问题，阻断正式提交。
approved/            已通过评审，可进入 READY_TO_SUBMIT。
archive/             已完成提交后的历史记录。
```

评审文件命名：

```text
<review_id>.md
```

推荐 `review_id`：

```text
RV-<claim_id>-R<两位序号>
```

示例：

```text
RV-PROV-001-S03-R01
RV-FE-003-S01-R02
RV-DB-ORG-001-S01-R01
```

## 5. 角色分工

### 5.1 Builder AI

负责任务实现。

必须做到：

- 创建并维护 claim。
- 按任务卡实现代码、测试、样例和文档。
- 执行自检。
- 创建或请求创建 review 记录。
- 对评审问题逐项整改。
- 在 claim 中更新 `review_id`、`review_status`、`open_findings`。

不能做：

- 自行批准高风险任务。
- 删除 Reviewer AI 的问题记录。
- 用“已测试通过”替代评审批准。

### 5.2 Reviewer AI

负责审查质量和提出质控问题。

必须做到：

- 只审查 claim 声明的范围和实际 diff。
- 先找问题，再给结论。
- 每条问题给出严重级别、位置、影响、整改要求和验证要求。
- 不直接大改 Builder AI 的代码，除非认领修复任务或用户明确要求。
- 对整改后的版本进行复评。

### 5.3 Integrator AI

负责合并多个已通过评审的任务。

必须做到：

- 核对所有相关 claim 和 review 是否 `APPROVED`。
- 解决合并冲突。
- 跑完整回归。
- 把集成风险写入最终 claim 和 review。

### 5.4 Domain Reviewer AI

医学、医保、质控、安全、数据库任务需要领域审查。

典型审查点：

- 医学建议是否有来源、适用人群、排除条件。
- 医保规则是否有政策版本和适用组织。
- 质控结论是否可复核、可追责。
- 数据库 DDL 是否保持生产库（Oracle/达梦/PostgreSQL-Kingbase）与开发库（LOCAL_H2_FILE）一致。
- 安全变更是否覆盖鉴权、越权、日志脱敏和密钥泄漏。

## 6. 评审触发条件

所有任务都需要至少一次自检。以下任务必须有独立评审：

- 修改 Java 业务代码、持久化代码、接口契约或测试基座。
- 修改 Oracle/达梦/PostgreSQL-Kingbase/LOCAL_H2_FILE DDL、迁移脚本、索引、约束或表备注。
- 修改配置发布、review、publish、rollback、active 指针或来源阻断逻辑。
- 修改医学、医保、病历质控、医嘱安全、路径推荐、Dify 解释等高风险逻辑。
- 修改认证、授权、审计、日志、脱敏、密钥、签名或租户隔离。
- 修改前端核心流程、配置发布页面、规则校验工作台、质控看板或客户演示路径。
- 修改 AI 协作机制、开发总纲、任务卡模板和质量门禁文档本身。

低风险纯文档或样例修改可以由同一 AI 做自检评审，但必须保留 review 记录。

## 7. 自检清单

Builder AI 提交评审前必须完成：

```text
任务卡是否满足：
claim write_scope 是否与实际 diff 一致：
是否误改无关文件：
是否更新测试：
是否更新样例/API 示例：
是否更新 README/docs：
是否支持组织上下文：
是否支持来源追溯：
是否写审计：
是否返回 traceId：
是否保持 DB-only 可运行：
是否避免 Neo4j/Dify 强依赖：
是否避免硬编码医院逻辑：
是否同步 Oracle/达梦/PostgreSQL-Kingbase/LOCAL_H2_FILE DDL：
是否区分生产库与开发库：
是否执行 run-tests：
是否执行 build：
是否执行 git diff --check：
无 Oracle 时是否执行 LOCAL_H2 等价 smoke：
有 Oracle 时是否执行 Oracle DDL 和 smoke：
```

## 8. 评审维度

Reviewer AI 必须按以下维度审查，不适用项写 `N/A`。

### 8.1 需求符合度

- 功能是否满足任务卡。
- 是否扩大范围。
- 是否遗漏用户明确要求。
- 是否破坏已有能力。

### 8.2 架构一致性

- 是否遵守 Provider 边界。
- 是否保持 Oracle 主数据权威。
- 是否支持 LOCAL_H2 离线开发。
- 是否保持组织隔离和配置包生命周期。
- 是否把临时演示逻辑写入核心路径。

### 8.3 医疗安全和来源可信

- 医学/医保/质控结论是否有来源。
- 来源缺失、过期、未审核是否能阻断发布。
- 高风险建议是否仍由医生确认。
- Dify/AI 解释是否没有脱离规则和来源。

### 8.4 数据库一致性

- 生产库（Oracle/达梦/PostgreSQL-Kingbase）和开发库（LOCAL_H2_FILE）结构是否同步。
- 表、字段、索引、约束、备注是否完整。
- 迁移脚本是否可重复执行。
- 无 Oracle 环境是否有 LOCAL_H2 等价验证。
- Oracle smoke 未跑时是否明确标记待补验证。

### 8.5 代码质量

- 接口是否统一 `ApiResult`。
- 错误码和校验是否一致。
- 关键操作是否审计。
- 是否存在 NPE、并发、状态机、回滚或幂等风险。
- 是否有敏感信息泄漏。

### 8.6 测试和验证

- 契约测试是否覆盖新能力。
- smoke 是否覆盖真实运行链路。
- 异常、降级、边界条件是否覆盖。
- 前端是否有关键交互、截图或 E2E 验证计划。
- `git diff --check` 是否通过。

### 8.7 可运维性

- Provider 状态、日志、traceId 是否可定位。
- 失败是否有可读错误和修复建议。
- 是否能回滚或重试。
- 是否影响部署脚本或环境变量。

## 9. 问题严重级别

```text
P0 阻断：会导致主版本不可用、数据破坏、医疗/安全高风险、发布绕过审核。
P1 严重：核心功能不符合要求、重要测试缺失、Oracle/本地库严重不一致、越权风险。
P2 必改：局部行为错误、边界遗漏、文档/样例/测试不完整、可维护性明显问题。
P3 建议：体验、命名、注释、非阻断优化，可带任务编号延期。
```

放行规则：

- 存在任意 P0/P1：禁止正式提交。
- 存在 P2：默认禁止正式提交，除非 Reviewer 明确降级并记录原因。
- 存在 P3：可以提交，但必须记录后续任务或接受原因。
- `open_findings` 必须为 0 才能进入 `APPROVED`。

## 10. 质控问题格式

每条问题必须包含：

```text
finding_id:
severity: P0/P1/P2/P3
status: OPEN/FIXED/ACCEPTED_RISK/REJECTED
file:
line:
title:
problem:
impact:
required_fix:
verification_required:
owner:
fixed_in:
reviewer_verdict:
```

要求：

- `file` 尽量精确到文件路径。
- `line` 不确定时写 `N/A`，不能编造行号。
- `required_fix` 要具体到可执行动作。
- `verification_required` 写必须跑的命令或接口。
- `fixed_in` 写提交 hash；未提交时写本地 diff 或待提交。

## 11. 标准流程

### 11.1 开发完成后

Builder AI：

```powershell
git status -sb
git diff --stat
.\zy-engine-mvp\scripts\run-tests.ps1
.\zy-engine-mvp\scripts\build.ps1
git diff --check
```

然后：

1. 更新 claim 状态为 `SELF_CHECK`。
2. 填写自检结果。
3. 创建 `ai-dev-input/11_ai_reviews/pending/<review_id>.md`。
4. 更新 claim 的 `review_id`、`review_status=REVIEW_REQUESTED`、`open_findings=unknown`。
5. 将 claim 状态改为 `REVIEW_REQUESTED`。

### 11.2 Reviewer 审查

Reviewer AI：

```powershell
git status -sb
git diff --stat
rg -n "<claim_id>|<review_id>" ai-dev-input/10_task_claims ai-dev-input/11_ai_reviews
```

如果发现问题：

1. 在 review 文件写入 findings。
2. 将 review 文件移入 `changes_requested/`。
3. 将 claim 状态改为 `CHANGES_REQUESTED`。
4. 写明开放问题数和最高严重级别。

如果没有阻断问题：

1. 将 review 状态改为 `APPROVED`。
2. 将 review 文件移入 `approved/`。
3. 将 claim 状态改为 `APPROVED`。
4. 将 `open_findings` 写为 `0`。

### 11.3 Builder 整改

Builder AI：

1. 把 claim 状态改为 `FIXING`。
2. 逐条处理 `OPEN` findings。
3. 在 review 文件中填写 `fixed_in`、验证命令和结果。
4. 重新执行必要验证。
5. 将 review 文件移回 `pending/` 并标记 `REVIEW_REQUESTED`。

### 11.4 复评和放行

Reviewer AI 复评：

- 已修复：将对应 finding 改为 `FIXED`。
- 接受风险：仅 P3 或经过说明的低风险 P2 可写 `ACCEPTED_RISK`。
- 未修复：保持 `OPEN`，任务继续阻断。

所有阻断问题关闭后：

```text
review_status: APPROVED
open_findings: 0
claim.status: APPROVED
```

Builder AI 再把 claim 改为 `READY_TO_SUBMIT`，才允许正式提交、合并或推送业务代码。

## 12. 正式提交门禁

业务代码正式提交前必须满足：

```text
claim 文件存在：
claim.status 为 APPROVED 或 READY_TO_SUBMIT：
review_id 已填写：
review 文件状态为 APPROVED：
open_findings = 0：
run-tests 通过：
build 通过：
git diff --check 通过：
数据库模式已说明：
Oracle/LOCAL_H2 验证结果已说明：
```

推荐分支策略：

```text
main                         协作元数据、已审核主版本
codex/<claim_id>-<short>     业务开发分支
```

若当前项目临时只允许直接推 `main`：

- claim 和 review 元数据仍先推 `main`。
- 业务代码必须先通过评审记录，再提交或推送。
- 若已经直接推入但评审发现 P0/P1，必须立即创建修复 claim，主版本不得发布或交付。

## 13. 主版本保护口径

主版本必须始终可运行。任何 AI 不得因为“认领成功”就把未完成或未评审的业务代码同步到主版本。

允许进入 `main` 的内容：

- 任务认领文件。
- 心跳更新。
- 评审记录。
- 已通过质量门禁的业务代码。
- 紧急修复且带用户明确授权的热修代码。

不允许进入 `main` 的内容：

- 未完成、未自检的业务代码。
- 被 `CHANGES_REQUESTED` 阻断的代码。
- 缺测试、缺文档、缺 DDL 同步的功能。
- 影响 Oracle 生产结构但只在内存或单库验证过的改动。

## 14. 特殊场景

### 14.1 没有独立 Reviewer AI

可以使用自审，但限制如下：

- 只适用于纯文档、样例、小范围低风险任务。
- review 文件必须写 `review_type: SELF_REVIEW`。
- 高风险任务只能进入 `REVIEW_REQUESTED`，不能自批 `APPROVED`。

### 14.2 Reviewer 直接修复问题

Reviewer 如果要直接修改代码，必须：

- 创建新的修复 claim，例如 `<claim_id>-FIX01`。
- 在原 review 中记录接管原因。
- 不覆盖 Builder 未提交的无关改动。

### 14.3 发现旧代码问题

若问题不属于当前 claim：

- 不在当前任务里顺手大改。
- 创建独立 finding 并标记 `out_of_scope: true`。
- 建议新增后续 claim。
- 只有 P0/P1 且影响当前主版本可用性时，才升级为紧急修复。

### 14.4 Oracle 不可用

无 Oracle 的 AI 必须：

- 执行 `detect-db-env.ps1 -BootstrapLocal`。
- 使用 `LOCAL_H2_FILE` 完成等价验证。
- 同步维护 Oracle、达梦、PostgreSQL-Kingbase 和 LOCAL_H2_FILE DDL。
- 在 review 中写明 `oracle_smoke_status: PENDING_IN_INTRANET_ENV`。

有 Oracle 的集成 AI 后续必须补：

- `run-oracle-ddl.ps1`
- 对应 Oracle smoke
- review 复评结论

## 15. Definition of Done

一个任务只有同时满足以下条件，才算完成：

- claim 已归档。
- review 已归档。
- review 状态为 `APPROVED`。
- `open_findings=0`。
- 正式提交 hash 已写入 claim 和 review。
- 验证命令和结果已写入 claim 和 review。
- 数据库模式和 Oracle/LOCAL_H2 验证状态已写明。
- 剩余风险只有已接受的 P3 或明确后续任务。

## 16. 后续 AI 最小执行顺序

每个 AI 接手时必须按此顺序：

```text
1. 阅读 AI接手执行手册。
2. 阅读 AI任务认领与并行开发机制。
3. 阅读 AI开发质量门禁与评审整改机制。
4. 查看 active/blocked claim。
5. 查看 pending/changes_requested review。
6. 无冲突后创建 claim。
7. 开发、自检、提交评审。
8. 根据质控问题整改。
9. 通过评审后正式提交。
10. 归档 claim 和 review。
```

这套机制保证不同能力的 AI 不是靠“自觉”交付，而是被同一套质量门、同一套问题格式和同一套放行条件约束。
