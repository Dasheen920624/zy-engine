# 评估质控 API 设计

## 1. 决策结论

`GA-ENG-API-08` 采用“指标配置版本 + 运行事实 + 结果证据 + 质控问题 + 整改任务 + 复核结论”的后端合同。首版建设可审计、可解释、可闭环的评估质控 API 与关系库存储，为后续 `GA-ENG-EVAL-01` 确定性指标计算、病例扫描和质控执行引擎提供稳定事实入口。

本任务只接收已经由授权上游或人工质控确认的评估事实，不在接口层伪造自动计算能力：

1. 评估指标按版本配置、提交审核、发布和激活，只有生效指标可用于新运行事实。
2. 一次运行可携带多条结果与质控问题，每条结果必须保留指标版本、来源证据摘要和上下文引用。
3. `P0` 安全红线与 `P1` 高风险问题必须在入库时同时生成有责任科室、有时限的整改任务。
4. 整改提交后只能由有复核权限的人员关闭或豁免；复核结论追加留痕，不覆写历史意见。
5. 所有写动作记录审计事件与状态历史，并携带租户和 `traceId`。

## 2. 目标

- 提供评估指标、运行结果、质控问题、整改和复核的 API 最小闭环。
- 建立五方言 `V14` 表族：`evaluation_indicator`、`evaluation_run`、`evaluation_result`、`quality_finding`、`rectification_task`、`rectification_review`。
- 复用统一 `ApiResult`、`PageResponse`、错误码、租户数据范围、审计、状态历史和诊断响应。
- 复用已有 `evaluation.read`、`evaluation.write`、`evaluation.publish` 权限，并追加运行、整改和复核的职责分离权限。
- 使问题、整改和复核事实可被后续证据链、业务包装和电子病历评级验收复用。

## 3. 非目标

- 不实现病例自动扫描、指标表达式执行、规则/路径联动计算或批量质控任务。
- 不调用大模型、Dify 或图数据库，不生成语义质控候选。
- 不建设质控驾驶舱、整改页面或临床嵌入交互。
- 不自动阻断病历归档、医嘱执行或临床流程。
- 不保存完整病历正文、报告原文或非必要个人信息。
- 不允许通过本 API 绕过知识、规则、路径或推荐的既有发布与审计边界。

## 4. 业务边界

### 4.1 指标配置版本

指标是可发布的配置资产，每个 `evaluation_indicator` 记录表达一个不可变的指标版本。新增口径必须创建新版本，不原地改写已发布口径。表名按实施方案中的既有合同采用 `evaluation_indicator`，用于明确其属于评估引擎而非通用运营指标。

| 状态 | 含义 | 允许动作 |
|---|---|---|
| `DRAFT` | 草稿口径 | 查看、提交审核 |
| `PENDING_REVIEW` | 等待质控审核 | 查看、发布或退回（退回延后实现） |
| `PUBLISHED` | 已通过审核但未用于新运行 | 查看、激活 |
| `ACTIVE` | 当前适用范围内可用于新评估事实 | 查看、运行入库、由新版本激活替换 |
| `OFFLINE` | 不再参与新运行 | 只读和历史解释 |
| `ARCHIVED` | 已归档历史版本 | 只读 |

首版发布门禁：

- 指标必须填写名称、对象类型、分母、分子、时间窗、组织范围、责任口径和来源引用。
- 同一租户、同一 `indicator_code`、同一版本号不得重复。
- 激活新版本时，同编码的旧 `ACTIVE` 版本转为 `OFFLINE`，历史结果仍绑定旧版本。

### 4.2 评估运行与结果

`evaluation_run` 表示一次外部计算或人工复核后的受控事实入库。首版接口不计算是否命中，只校验事实是否可安全留存。

| 运行类型 | 含义 |
|---|---|
| `MANUAL_SAMPLE` | 质控人员完成抽检后提交的事实 |
| `UPSTREAM_RESULT` | 已授权规则、路径或外部质控适配器提交的事实 |
| `BATCH_IMPORT` | 经授权导入的历史或离线评估事实，不表示系统已具备自动扫描 |

| 运行状态 | 含义 |
|---|---|
| `RECEIVED` | 已接收请求，等待事务内完成记录 |
| `RECORDED` | 结果和问题事实已完整持久化 |
| `FAILED` | 入库失败，保留错误码和诊断线索 |

每条 `evaluation_result` 必须包含：

- 一个处于 `ACTIVE` 的 `indicator_id` 与其版本快照。
- 评估对象类型和对象引用，例如患者、病历、科室、路径或医保病例。
- 标准上下文快照、临床事件、路径或推荐触发中的至少一种关联线索，或者明确的人工抽检引用。
- 得分或判定结果、证据摘要、包版本、发生时间和 `traceId`。

结果判定级别采用 `PASS`、`ATTENTION`、`NON_COMPLIANT`、`CRITICAL`，分别表达通过、需关注、不符合和安全红线；是否生成问题由请求事实显式提供，不由首版接口推断。

### 4.3 质控问题

问题记录是结果中需要处置的发现事实。首版问题级别与默认动作遵循详细规范：

| 级别 | 说明 | API 门禁 |
|---|---|---|
| `P0` | 安全红线，可能影响患者安全或违法违规 | 必须有责任科室、整改期限和证据摘要，立即生成整改任务 |
| `P1` | 高风险，影响核心制度、病历质量或医保病案 | 必须有责任科室、整改期限和证据摘要，生成整改任务 |
| `P2` | 中风险，证据不足或时限偏差 | 可生成整改任务，也可只保留待派单问题 |
| `P3` | 低风险，轻微缺项或格式问题 | 默认保留问题事实，不自动派单 |

`P2/P3` 若请求同时给出责任科室和整改期限，则在同一事务内生成整改任务；否则保留 `NEW` 问题供后续治理策略派单，本轮不新增独立派单算法。

| 问题状态 | 含义 |
|---|---|
| `NEW` | 已发现，尚未派单 |
| `ASSIGNED` | 已分配整改任务 |
| `REMEDIATING` | 责任方已提交整改内容 |
| `CLOSED` | 质控复核通过并闭环 |
| `WAIVED` | 有理由豁免且已留痕 |

### 4.4 整改与复核

`rectification_task` 保存整改责任和处理提交；`rectification_review` 保存不可覆写的复核结论。

| 整改状态 | 含义 |
|---|---|
| `ASSIGNED` | 已派发责任科室 |
| `SUBMITTED` | 责任方已提交整改说明与证据 |
| `RETURNED` | 复核未通过，退回继续整改 |
| `CLOSED` | 复核通过 |
| `WAIVED` | 经授权豁免 |

| 复核结论 | 问题状态 | 整改状态 |
|---|---|---|
| `APPROVED` | `CLOSED` | `CLOSED` |
| `RETURNED` | `REMEDIATING` | `RETURNED` |
| `WAIVED` | `WAIVED` | `WAIVED` |

复核约束：

- `APPROVED` 必须包含复核说明或证据引用。
- `WAIVED` 必须包含豁免理由，`P0` 问题不得经普通复核接口豁免。
- 关闭或豁免后的任务不允许再次提交整改；历史复核记录始终可读。

### 4.5 医疗安全

- 评估结果与质控问题是质量管理事实，不是诊断或治疗指令。
- `P0/P1` 问题不能因缺少责任人、期限或证据而静默落库。
- 未发布或已下线指标不得参与新运行事实，避免旧口径混入现行质控。
- 模型关闭、Dify 不存在或图投影失效时，指标配置、事实接收、整改和复核仍完整可用。
- 运行结果绑定当时的指标版本和上下文引用，确保历史解释与证据导出可复现。

## 5. API 合同

### 5.1 指标

| API | 权限 | 用途 |
|---|---|---|
| `POST /api/v1/engine/evaluations/indicators` | `evaluation.write` | 创建指标草稿版本 |
| `GET /api/v1/engine/evaluations/indicators` | `evaluation.read` | 按状态、对象类型和编码分页查询 |
| `GET /api/v1/engine/evaluations/indicators/{indicatorId}` | `evaluation.read` | 查看指标详情 |
| `POST /api/v1/engine/evaluations/indicators/{indicatorId}/submit` | `evaluation.write` | 提交审核 |
| `POST /api/v1/engine/evaluations/indicators/{indicatorId}/publish` | `evaluation.publish` | 发布审核通过的指标 |
| `POST /api/v1/engine/evaluations/indicators/{indicatorId}/activate` | `evaluation.publish` | 激活当前版本并下线旧版 |

### 5.2 运行与查询

| API | 权限 | 用途 |
|---|---|---|
| `POST /api/v1/engine/evaluations/run` | `evaluation.execute` | 接收一次评估结果与质控问题事实 |
| `GET /api/v1/engine/evaluations/results` | `evaluation.read` | 分页查询结果 |
| `GET /api/v1/engine/evaluations/findings` | `evaluation.read` | 分页查询质控问题 |
| `GET /api/v1/engine/evaluations/findings/{findingId}` | `evaluation.read` | 查看问题、整改和复核详情 |
| `GET /api/v1/engine/evaluations/runs/{runId}/diagnose` | `evaluation.read` | 查看运行状态、结果、问题和状态历史 |

### 5.3 闭环

| API | 权限 | 用途 |
|---|---|---|
| `POST /api/v1/engine/evaluations/findings/{findingId}/rectification` | `evaluation.remediate` | 提交整改说明和整改证据 |
| `POST /api/v1/engine/evaluations/findings/{findingId}/review` | `evaluation.review` | 提交复核结论，关闭、退回或豁免 |

## 6. 数据模型

### 6.1 `evaluation_indicator`

指标版本主表，保存口径、来源、适用范围和发布状态。

关键字段：`indicator_id`、`tenant_id`、`indicator_code`、`version_no`、`name`、`subject_type`、`denominator_definition`、`numerator_definition`、`exclusion_definition`、`scoring_definition`、`time_window`、`organization_scope`、`responsible_department_id`、`source_ref`、`package_version`、`status`、`published_at`、`published_by`、`activated_at`、`trace_id`。

### 6.2 `evaluation_run`

运行事实主表，保存外部运行标识、来源、状态和诊断入口。

关键字段：`run_id`、`tenant_id`、`run_code`、`run_type`、`source_event_id`、`context_snapshot_id`、`patient_id`、`encounter_id`、`scenario_code`、`package_version`、`input_digest`、`status`、`error_code`、`occurred_at`、`trace_id`。

### 6.3 `evaluation_result`

评估结果表，保存绑定指标版本的一次对象判定与证据摘要。

关键字段：`result_id`、`tenant_id`、`run_id`、`indicator_id`、`indicator_code`、`indicator_version`、`subject_type`、`subject_ref_id`、`score_value`、`result_level`、`hit_flag`、`evidence_summary`、`source_ref`、`responsible_department_id`、`trace_id`。

### 6.4 `quality_finding`

质控问题表，保存风险分级、解释、责任和闭环状态。

关键字段：`finding_id`、`tenant_id`、`run_id`、`result_id`、`indicator_id`、`finding_code`、`title`、`description`、`severity`、`status`、`evidence_summary`、`responsible_department_id`、`due_at`、`trace_id`。

### 6.5 `rectification_task`

整改任务表，保存派单、提交与关闭状态。

关键字段：`task_id`、`tenant_id`、`finding_id`、`responsible_department_id`、`assignee_user_id`、`status`、`due_at`、`rectification_summary`、`evidence_ref`、`submitted_at`、`submitted_by`、`closed_at`、`trace_id`。

### 6.6 `rectification_review`

复核记录表，保存追加式复核结论。

关键字段：`review_id`、`tenant_id`、`finding_id`、`task_id`、`decision`、`comment`、`evidence_ref`、`reviewer_id`、`reviewed_at`、`trace_id`。

## 7. 首版服务规则

1. 创建指标只产生 `DRAFT` 版本；提交、发布和激活按状态顺序流转。
2. 同租户同指标编码仅允许一个 `ACTIVE` 版本；激活新版本时下线旧版本。
3. 创建运行请求必须包含 `runCode`、`runType`、`scenarioCode`、`inputDigest` 和至少一条结果。
4. 结果引用的指标必须属于当前租户且状态为 `ACTIVE`。
5. 每条结果必须有证据摘要；事实入库不等同于自动计算正确性认证。
6. 创建 `P0/P1` 问题必须携带责任科室、整改期限和证据摘要，并在同事务生成整改任务；`P2/P3` 仅在请求显式携带派单信息时生成任务。
7. 整改只能提交给处于 `ASSIGNED` 或 `RETURNED` 的任务；提交后问题状态进入 `REMEDIATING`。
8. 复核只能处理已提交整改；`P0` 问题不得通过常规 `WAIVED` 结论关闭。
9. 运行诊断返回结果、问题和整改任务关联标识，并保留该运行的 `traceId`。
10. 所有读写均按租户隔离，写动作均发布审计事件并记录状态变化。

## 8. 权限与角色

| 权限 | 业务含义 | 默认角色 |
|---|---|---|
| `evaluation.read` | 查看指标、结果、问题与复核历史 | 质控办、医务处、科主任、医保办 |
| `evaluation.write` | 创建和提交指标草稿 | 质控办 |
| `evaluation.publish` | 发布、激活指标版本 | 质控办、医院管理员 |
| `evaluation.execute` | 受控写入运行、结果和问题事实 | 质控办、信息科 |
| `evaluation.remediate` | 为责任科室提交整改事实 | 科主任、质控办 |
| `evaluation.review` | 复核整改和形成闭环结论 | 质控办 |

首版不为医生角色开放整改或复核接口，避免在未具备责任对象级数据范围校验前扩大医疗责任操作面。

## 9. 错误码

| 错误码 | HTTP | 含义 |
|---|---:|---|
| `ENG-EVAL-001` | 400 | 指标或运行请求校验失败 |
| `ENG-EVAL-002` | 404 | 指标不存在 |
| `ENG-EVAL-003` | 409 | 指标状态不允许当前操作 |
| `ENG-EVAL-004` | 409 | 运行引用了非生效指标 |
| `ENG-EVAL-005` | 404 | 质控问题或整改任务不存在 |
| `ENG-EVAL-006` | 400 | 高风险问题缺少责任、期限或证据 |
| `ENG-EVAL-007` | 409 | 整改或复核状态不允许当前操作 |

## 10. 验收标准

- H2、PostgreSQL、Oracle、达梦、人大金仓迁移均增加至 `V14`，`evaluation_indicator` 等六张表及约束合同一致。
- 单元和集成测试覆盖指标状态流转、生效版本门禁、运行事实入库、`P0/P1` 自动派单、整改提交、复核关闭/退回/豁免限制、诊断和租户隔离。
- MockMvc 安全测试覆盖读取、配置、发布、运行、整改和复核权限分离。
- 服务层仅持久化授权事实，不存在病例自动计算、模型调用或自动临床处置代码。
- 完整后端测试通过；Docker 不可用时，既有容器依赖多方言冒烟按现行机制跳过。
- `docs/backlog.md` 在完成验证后将 `GA-ENG-API-08` 标记为完成，并保留 `GA-ENG-EVAL-01` 为后续执行引擎任务。
