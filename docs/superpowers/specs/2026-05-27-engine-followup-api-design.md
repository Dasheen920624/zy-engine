# 随访 API 设计规范 (GA-ENG-API-09)

> 日期：2026-05-27
> 作者：Antigravity
> 状态：APPROVED
> 归属场景：S12 智能随访

## 1. 业务目标

为中枢系统提供从专病路径自动生成随访计划的能力，并将异常回院及随访结果回流纳入 CDSS 及质控体系。本系统不负责提供完整的独立随访呼叫中心，只负责中枢资产的计划生成、任务调度与异常事件接入。

## 2. 核心领域模型

- **随访计划 (FollowupPlan)**：与患者、特定出院事件、风险分层或专病路径强绑定。
- **随访任务 (FollowupTask)**：基于计划生成的具体执行项，包含问卷填答、结果回访等类型，带有时限（due_date）。
- **随访问卷 (FollowupQuestionnaire)**：任务的扩展，记录机构内或跨机构收集的结构化填答结果。
- **异常事件 (FollowupEvent)**：由外围随访系统发现问题（如患者恶化），触发回调并生成异常回院事件，作为后续 CDSS 介入的触发器。

## 3. 数据库迁移 (V15)

我们将通过 `V15__followup_engine_api.sql` 向 Kingbase, Oracle, PostgreSQL 部署以下表：
- `followup_plan`
- `followup_task`
- `followup_questionnaire`
- `followup_event`

表中必须包含五方言标准的审计字段 (`created_by`, `updated_at` 等) 与 `tenant_id`, `trace_id`。

## 4. 接口规范

- `POST /api/v1/engine/followup/plans/generate`: 生成计划 (幂等)
- `GET /api/v1/engine/followup/plans`: 分页查询
- `GET /api/v1/engine/followup/plans/{planId}`: 详情
- `POST /api/v1/engine/followup/tasks/{taskId}/complete`: 任务完成状态上报
- `POST /api/v1/engine/followup/tasks/{taskId}/questionnaires`: 问卷结果提交
- `POST /api/v1/engine/followup/events/report-abnormal`: 异常事件（回院）上报

## 5. 安全与权限

- 受 `followup.read`, `followup.write` 权限控制。
- 所有数据变更必须记录 `trace_id` 保证全链路可追溯。
