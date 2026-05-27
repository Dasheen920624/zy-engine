# 推荐/CDSS API 设计

## 1. 决策结论

`GA-ENG-API-07` 采用“触发事实 + 推荐卡 + 来源解释 + 医师反馈 + 疲劳治理输入”的后端合同。首版建设推荐触发、推荐卡片、来源证据、反馈回流和疲劳治理信号的 API 与数据底座，为后续 `GA-ENG-CDSS-01` 综合推荐引擎、临床嵌入提醒和疲劳治理策略提供稳定基础。

首版不把 CDSS 做成自动诊断、自动医嘱或模型生成入口，而是实现一个可审计、可解释、可回放的最小闭环：

1. 规则、路径、知识或临床事件的上游结果通过受控触发 API 形成推荐触发事实。
2. 每张推荐卡必须包含风险级别、打扰级别、建议动作、来源摘要和可追溯来源。
3. 高风险或红线推荐必须要求医师确认，不能自动写入医嘱、诊断或病历。
4. 医师采纳、不采纳、稍后处理、忽略和查看依据都作为反馈事实保存。
5. 静默试运行、重复提醒、关闭、拒绝和延后等行为进入疲劳治理信号池，但首版不自动抑制临床提醒。

## 2. 目标

- 提供推荐触发、推荐卡查询、推荐详情、医师反馈、来源解释、疲劳治理信号和诊断 API。
- 建立五方言迁移表：`recommendation_trigger`、`recommendation_card`、`recommendation_source`、`recommendation_feedback`、`recommendation_fatigue_signal`。
- 复用统一 `ApiResult`、`PageResponse`、错误码、审计事件、租户数据范围和 traceId。
- 复用已有 `recommendation.read`、`recommendation.accept` 权限，并新增 `recommendation.write` 用于系统或治理侧写入推荐事实。
- 为规则、路径、知识、嵌入、评估质控、模型网关和证据链预留推荐运行事实合同。

## 3. 非目标

- 不实现完整综合推荐算法、规则/路径/知识自动编排或模型调用。
- 不实现前端临床嵌入卡片、iframe、SDK 或医生站工作流。
- 不自动生成医嘱、诊断、病历正文、护理计划或随访任务。
- 不把疲劳治理做成自动屏蔽策略；首版只记录治理输入和静默试运行事实。
- 不保存完整患者病历原文、自由文本病程、检查报告正文或大段上下文。
- 不新增绕过权限、租户隔离、审计和来源解释门禁的调试接口。

## 4. 业务边界

### 4.1 推荐触发

推荐触发表示一次 CDSS 运行入口，可来自临床事件、标准上下文、规则执行、路径推进、知识查询或外部嵌入场景。

| 状态 | 含义 |
|---|---|
| `RECEIVED` | 已接收触发，尚未完成卡片落库 |
| `EVALUATED` | 已生成至少一张推荐卡 |
| `NO_CARD` | 触发已记录，但本次无推荐卡 |
| `FAILED` | 触发处理失败，保留错误码和诊断 |

首版 API 允许上游把候选卡随触发请求一并提交。真正的综合生成逻辑留给 `GA-ENG-CDSS-01`，避免在 API 任务中伪造推荐引擎。

### 4.2 推荐卡

| 字段 | 规则 |
|---|---|
| 风险级别 | `LOW`、`MEDIUM`、`HIGH`、`CRITICAL` |
| 打扰级别 | `SILENT`、`INFO`、`WEAK_INTERRUPTIVE`、`STRONG_INTERRUPTIVE` |
| 状态 | `PENDING`、`VIEWED`、`ACCEPTED`、`REJECTED`、`DEFERRED`、`DISMISSED`、`SUPPRESSED`、`EXPIRED` |
| AI 标识 | `ai_generated` 必须显式保存，任何 AI 候选不能被当作人工规则结论展示 |
| 来源解释 | 每张卡至少一条来源，来源需包含类型、标题、版本或 hash 中的可追溯信息 |

高风险和红线推荐必须满足：

- `requires_physician_confirmation=true`。
- `source_summary` 不为空。
- 至少一条来源记录。
- 不允许通过反馈接口直接写病历或医嘱，只能保存医生处理事实。

### 4.3 医师反馈

反馈类型覆盖临床嵌入体验要求：

| 类型 | 含义 | 推荐卡状态 |
|---|---|---|
| `VIEW_SOURCE` | 查看依据 | `VIEWED` |
| `ACCEPT` | 采纳 | `ACCEPTED` |
| `REJECT` | 不采纳并说明 | `REJECTED` |
| `DEFER` | 稍后处理 | `DEFERRED` |
| `DISMISS` | 关闭或忽略 | `DISMISSED` |

反馈必须记录操作者、角色、原因代码、原因说明、traceId 和创建时间。原因说明可为空，但不采纳、关闭和稍后处理建议填写原因代码，便于后续治理。

### 4.4 疲劳治理输入

疲劳治理信号只做事实采集，不做自动屏蔽。首版信号包括：

| 信号 | 触发时机 |
|---|---|
| `SHOWN` | 推荐卡可展示给临床用户 |
| `SILENT_RECORDED` | 静默试运行，只记录不打扰 |
| `VIEWED` | 用户查看推荐或依据 |
| `ACCEPTED` | 用户采纳 |
| `REJECTED` | 用户不采纳 |
| `DEFERRED` | 用户稍后处理 |
| `DISMISSED` | 用户关闭或忽略 |

后续疲劳治理引擎可按 `fatigue_key`、患者、场景、操作者、时间窗和信号分布计算抑制策略。

### 4.5 医疗安全

- 推荐卡是辅助建议和风险提醒，不是医疗最终结论。
- 任意推荐写入医嘱、病历、诊断或随访任务前，必须由后续业务系统和医师确认完成。
- AI 候选必须被显式标记；无模型基线下，规则、路径和知识来源仍可产生确定性推荐事实。
- 所有推荐必须可追溯到规则、路径、知识、上下文或人工来源；推荐可追溯率目标为 100%。
- 静默试运行不能打扰医生，但必须能形成报告和证据。

## 5. API 合同

| API | 权限 | 用途 |
|---|---|---|
| `POST /api/v1/engine/recommendations/triggers` | `recommendation.write` | 接收一次推荐触发和候选卡 |
| `GET /api/v1/engine/recommendations/cards` | `recommendation.read` | 按状态、风险、场景、患者、时间分页查询推荐卡 |
| `GET /api/v1/engine/recommendations/cards/{cardId}` | `recommendation.read` | 查看推荐卡详情、来源、反馈和疲劳信号 |
| `GET /api/v1/engine/recommendations/cards/{cardId}/sources` | `recommendation.read` | 查看推荐来源解释 |
| `POST /api/v1/engine/recommendations/cards/{cardId}/feedback` | `recommendation.accept` | 回传医师反馈 |
| `GET /api/v1/engine/recommendations/fatigue-signals` | `recommendation.read` | 查询疲劳治理输入事实 |
| `GET /api/v1/engine/recommendations/triggers/{triggerId}/diagnose` | `recommendation.read` | 查看一次触发的诊断解释 |

## 6. 数据模型

### 6.1 `recommendation_trigger`

推荐触发主表，保存触发来源、患者和就诊引用、上下文引用、包版本、状态、输入摘要、错误和 traceId。

关键字段：`trigger_id`、`tenant_id`、`trigger_code`、`trigger_type`、`source_event_id`、`context_snapshot_id`、`patient_id`、`encounter_id`、`patient_pathway_id`、`scenario_code`、`package_version`、`input_digest`、`status`、`error_code`、`occurred_at`、`created_at`、`trace_id`。

### 6.2 `recommendation_card`

推荐卡主表，保存卡片编码、类型、标题、摘要、建议动作、风险、打扰级别、AI 标识、确认要求、状态、解释快照和疲劳键。

关键字段：`card_id`、`tenant_id`、`trigger_id`、`card_code`、`card_type`、`title`、`summary`、`suggested_action`、`risk_level`、`interrupt_level`、`status`、`requires_physician_confirmation`、`ai_generated`、`source_summary`、`explanation_json`、`fatigue_key`、`expires_at`、`trace_id`。

### 6.3 `recommendation_source`

推荐来源解释表，保存每张卡关联的规则、路径、知识、上下文、术语或人工来源。

关键字段：`source_id`、`tenant_id`、`card_id`、`source_type`、`source_ref_id`、`source_version`、`source_title`、`citation_locator`、`source_hash`、`summary`、`created_at`。

### 6.4 `recommendation_feedback`

医师反馈表，保存采纳、不采纳、稍后处理、忽略、查看依据等行为。

关键字段：`feedback_id`、`tenant_id`、`card_id`、`feedback_type`、`reason_code`、`reason_text`、`operator_id`、`operator_role`、`created_at`、`trace_id`。

### 6.5 `recommendation_fatigue_signal`

疲劳治理输入表，保存推荐展示、静默记录、查看、采纳、拒绝、延后和忽略信号。

关键字段：`signal_id`、`tenant_id`、`trigger_id`、`card_id`、`fatigue_key`、`patient_id`、`encounter_id`、`operator_id`、`signal_type`、`occurrence_count`、`window_started_at`、`created_at`、`trace_id`。

## 7. 首版服务规则

1. 触发请求必须包含 `triggerCode`、`triggerType`、`scenarioCode` 和 `inputDigest`。
2. 触发请求可不包含候选卡，此时触发状态为 `NO_CARD`。
3. 只要包含候选卡，每张卡必须至少一条来源。
4. `HIGH` 和 `CRITICAL` 风险卡必须要求医师确认。
5. `STRONG_INTERRUPTIVE` 只允许用于 `HIGH` 或 `CRITICAL` 风险。
6. `SILENT` 打扰级别写入 `SILENT_RECORDED` 疲劳信号，不强打扰医生。
7. 反馈只能作用于未过期且未关闭的卡；重复终态反馈返回状态冲突。
8. 反馈后同步写入一条疲劳治理信号。
9. 诊断接口返回触发状态、卡片数量、反馈数量、疲劳信号数量、风险分布、traceId 和安全提示。

## 8. 错误码

新增错误码：

| 错误码 | HTTP | 含义 |
|---|---|---|
| `ENG-REC-001` | 400 | 推荐触发请求校验失败 |
| `ENG-REC-002` | 404 | 推荐触发不存在 |
| `ENG-REC-003` | 404 | 推荐卡不存在 |
| `ENG-REC-004` | 409 | 推荐卡当前状态不允许反馈 |
| `ENG-REC-005` | 400 | 推荐来源解释不完整 |
| `ENG-REC-006` | 409 | 高风险推荐缺少医师确认门禁 |

## 9. 验收标准

- H2、PostgreSQL、Oracle、达梦、人大金仓迁移版本均增加到 V13。
- 后端单元测试覆盖迁移合同、触发落库、无卡触发、来源门禁、高风险确认门禁、反馈状态流转、疲劳信号和诊断解释。
- MockMvc 安全测试覆盖读、写、反馈权限。
- `mvn -f medkernel-backend/pom.xml test` 通过；无 Docker 环境下已有数据库烟测可按既有机制跳过。
- `docs/backlog.md` 将 `GA-ENG-API-07` 标记为完成，并保留 `GA-ENG-CDSS-01` 为后续综合推荐引擎任务。
