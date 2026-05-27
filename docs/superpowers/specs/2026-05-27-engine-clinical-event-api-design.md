# GA-ENG-API-02 临床事件 API 设计

> 版本：1.0 · 2026-05-27
> 状态：已确认，进入实施
> 适用任务：`GA-ENG-API-02 临床事件 API`

## 1. 目标

本任务交付临床事件接入闭环：院内系统可以同步、异步或批量提交诊断、医嘱、报告、入院、出院、随访等事件；平台将事件、payload、状态历史、审计、traceId 和死信信息全部落库，支持查询、诊断、payload 拉取和重放。

实现必须继续保持 DB-Only：不新增 Kafka、对象存储、消息队列或可观测性外部组件。后续如切换 OSS 或 Kafka，只替换 `PayloadStoragePort` 或 Outbox 分发实现，不改变 API 响应和诊断口径。

## 2. 范围

本轮包含：

| 能力 | 说明 |
|---|---|
| 同步接收 | `POST /api/v1/engine/events` 入库并尝试在超时窗口内完成处理 |
| 异步接收 | `POST /api/v1/engine/events/async` 入库即返回受理结果 |
| 批量接收 | `POST /api/v1/engine/events/batch` 一次最多 100 条，逐条返回受理结果 |
| 元数据查询 | `GET /api/v1/engine/events/{eventId}` 返回事件状态和错误摘要，不直接返回 payload |
| payload 查询 | `GET /api/v1/engine/events/{eventId}/payload` 返回原始事件内容和摘要 |
| 诊断查询 | `GET /api/v1/engine/events/{eventId}/diagnose` 复用 `DiagnoseResponse` 返回状态历史、payload 摘要、错误和 traceId |
| 列表查询 | `GET /api/v1/engine/events` 支持按 patientId、encounterId、status、type 查询 |
| 重放 | `POST /api/v1/engine/events/{eventId}/replay` 新建事件并把 `root_event_id` 指向源事件 |
| 可靠处理 | `clinical_event_outbox` 驱动后台处理，支持指数退避、重试和 DEAD 死信 |

本轮不包含：

| 不做 | 原因 |
|---|---|
| 外部 webhook 回调 | 后续 API-02b 单独实现，避免本轮引入签名、白名单和外部重试面 |
| Kafka / MQ | 当前吞吐目标先用数据库 outbox，降低部署复杂度 |
| OSS payload 外置 | 先保留 `payload_uri` 和 `storage_type` 扩展点，默认内联入旁路表 |
| 前端临床事件管理页 | 台账当前任务是引擎接口，前端只跑既有门禁，不新增页面假闭环 |

## 3. 数据模型

迁移版本使用 **V10**。`V9` 已被最新代码用于审计事件 outcome/error_code 字段，临床事件不得复用旧设计中的 V9 号。

| 表 | 变更 |
|---|---|
| `clinical_event_payload` | 新建 payload 旁路表，按 `event_id` 唯一保存原始事件内容、摘要、大小、存储类型 |
| `clinical_event_outbox` | 新建 outbox 表，记录待处理、已领取、已完成、死信状态和重试信息 |
| `clinical_event` | 增加 `patient_id`、`encounter_id`、`package_version`、`error_code`、`error_class`、`retry_count`、`root_event_id` |

`clinical_event` 主表保持轻量，只放检索、状态和诊断必要字段；原始 JSON 统一放入 `clinical_event_payload`，避免列表查询被大字段拖慢。

## 4. 服务分层

| 单元 | 职责 |
|---|---|
| `ClinicalEventController` | 处理 API 入参、权限、HTTP 状态和统一响应 |
| `ClinicalEventService` | 接收、幂等校验、payload 存储、outbox 写入、查询、诊断和重放编排 |
| `ClinicalEventProcessor` | 单事件处理链：状态从 RECEIVED 到 MAPPED 到 PROCESSED，失败时记录错误 |
| `ClinicalEventOutboxWorker` | 后台领取 PENDING 任务，调用 processor，处理重试和 DEAD |
| `ClinicalEventPayloadRepository` | payload 旁路表读写 |
| `ClinicalEventOutboxRepository` | outbox 状态流转和待处理任务查询 |
| `ClinicalEventProperties` | 事件大小、同步等待、worker 批量、最大重试、退避配置 |

处理链不直接调用模型或 Dify。当前最小处理只做结构校验、payload 摘要校验、字典映射端口检查和状态流转，为后续规则、路径、推荐引擎接入留出事件发布点。

## 5. 状态与错误

`ClinicalEventStatus` 增加 `SUPERSEDED`，用于标记被重放替代的源事件。

新增错误码：

| 错误码 | HTTP | 类型 | 可重试 | 含义 |
|---|---|---|---|---|
| `ENG-EVENT-001` | 400 | INPUT | 否 | 事件 schema 校验失败 |
| `ENG-EVENT-002` | 409 | INPUT | 否 | 事件 ID 已存在且请求内容不一致 |
| `ENG-EVENT-003` | 404 | DATA | 否 | 事件不存在 |
| `ENG-EVENT-004` | 503 | EXTERNAL | 是 | payload 存储不可用 |
| `ENG-EVENT-005` | 500 | INTERNAL | 否 | 事件处理失败并进入死信 |
| `ENG-EVENT-006` | 400 | INPUT | 否 | 当前状态不允许重放 |

失败处理必须同时写入：

| 位置 | 内容 |
|---|---|
| `clinical_event` | `processing_status`、`error_code`、`error_class`、`retry_count` |
| `clinical_event_outbox` | `claim_status`、`retry_count`、`last_error_code`、`next_attempt_at` |
| `state_transition_history` | 状态变迁、原因、错误摘要和 traceId |
| `audit_event` | CREATE、EXECUTE、REPLAY 或失败事件 |

## 6. 权限

新增权限码：

| 权限码 | 中文含义 | 默认角色 |
|---|---|---|
| `event.read` | 查看临床事件、payload 和诊断信息 | 医生、护士、专家、科主任、医务、质控、审计、运维、实施 |
| `event.write` | 创建、批量接收和重放临床事件 | 运维、实施、医院管理员、集团管理员、平台管理员 |

所有接口必须从 `RequestContext` 读取租户、用户和 traceId，不接受客户端伪造租户。

## 7. 验收

| 类别 | 验收项 |
|---|---|
| 迁移 | H2、PostgreSQL、Oracle、达梦、人大金仓 V10 迁移合同通过 |
| 服务 | 同步、异步、批量、查询、payload、diagnose、replay 单测通过 |
| Worker | PENDING → CLAIMED → PROCESSED；失败退避；超过最大重试进入 DEAD |
| 安全 | 未授权 403；缺租户 400；读写权限按角色生效 |
| 诊断 | event diagnose 返回状态历史、payload 摘要、错误、traceId |
| 回归 | 后端测试、前端 lint/typecheck/test/build 通过 |

## 8. 变更记录

| 日期 | 变更 |
|---|---|
| 2026-05-27 | 从综合设计中拆出 API-02 专项设计，并将迁移版本从旧草案 V9 修正为 V10 |

