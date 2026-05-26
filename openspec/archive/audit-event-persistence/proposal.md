## Why

GA-ENG-BASE-04 要求所有写入、审核、发布、执行、反馈、导出和回滚都留下可防篡改证据。当前已具备进程内审计事件契约，包括 `AuditEvent`、`AuditEventPublisher` 和 `AuditAction`，也在迁移基线中创建了 `audit_event` 表，但还缺少把事件可靠写入数据库的监听器，合规审计 API 也不能继续返回示例数据。

没有真实审计底座，后续证据链、导出、回滚和合规追踪都无法落地。

## What Changes

- 新增 `AuditPersistenceSink`，在业务事务提交后监听 `AuditEvent` 并写入 `audit_event`。
- 为 `audit_event` 增加 `prev_event_id`、`prev_signature`，并新增 `audit_chain_head` 维护每个租户的最新签名头。
- 新增 `AuditEventRepository` 和 `AuditQueryService`，按 `RequestContext` 组织范围强制租户隔离，并提供游标分页查询。
- 改造 `com.medkernel.compliance.audit.AuditController`，让 `/events` 返回真实持久化数据，让 `/snapshot` 发布真实 `EXPORT` 审计事件。
- 审计持久化失败时记录结构化错误和指标，但不回滚已经成功的业务事务。

## Capabilities

### New Capabilities

- `audit-event-persistence`：基于 `audit_event` 和租户级签名链的持久化审计轨迹，并通过合规审计 API 查询。

### Modified Capabilities

- 不改变现有 `medkernel` 主能力形状；未来业务模块通过已有 `AuditEventPublisher` 接入审计底座。

## Impact

- 文档：`docs/MEDKERNEL_FOUNDATION_AND_SERVICES.md`、`docs/backlog.md` 和本 OpenSpec 变更目录。
- 后端：新增 `com.medkernel.shared.audit.persistence`，改造合规审计控制器和 DTO。
- 数据：5 方言新增 `V5__audit_chain_baseline.sql`，扩展 `audit_event` 并新增 `audit_chain_head`。
- 测试：覆盖持久化、签名链、租户隔离、分页过滤、权限和数据范围。
- 非目标：不在本任务中给所有业务控制器补审计调用，不实现 TSA 时间戳、异步导出或前端页面。
