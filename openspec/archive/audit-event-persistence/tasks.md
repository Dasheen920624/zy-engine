## 1. Schema 与持久化契约

- [x] 1.1 仓储测试写入两个租户的事件，并验证签名链、`findPage` 和 `findByEventId` 的租户隔离。
- [x] 1.2 为 H2、PostgreSQL、Oracle、达梦、人大金仓新增 V5 迁移；`audit_event` 增加 `prev_event_id`、`prev_signature`，新增 `audit_chain_head`。
- [x] 1.3 实现基于 JDBC 的 `AuditEventRepository`，对 `audit_chain_head` 使用行级锁推进链头。

## 2. 持久化 Sink 与签名链

- [x] 2.1 `AuditChainWriterTest` 覆盖首事件锚定 `GENESIS`、后续事件链接、租户隔离、缺租户回退 `__SYSTEM__` 和篡改校验失败。
- [x] 2.2 `AuditPersistenceSink` 在 `AFTER_COMMIT` 与无事务路径监听事件，委托 `AuditChainWriter` 使用 `REQUIRES_NEW` 写入。
- [x] 2.3 `AuditPersistenceSinkTest` 验证失败不抛到业务调用方、失败指标递增、成功计数只在写入成功后递增。

## 3. 查询服务与合规 API

- [x] 3.1 `AuditQueryServiceTest` 覆盖租户上下文必需、过滤条件、时间范围、游标截断和非法游标拒绝。
- [x] 3.2 实现 `AuditQueryService`，并让合规审计 DTO 从持久化记录投影。
- [x] 3.3 `AuditController` 的 `/events` 返回 `ApiResult<CursorResponse<AuditEvent>>`，`/snapshot` 发布真实 `EXPORT` 事件并回读持久化视图。
- [x] 3.4 `AuditControllerTest` 覆盖 `audit.read`、`audit.export`、无权限和无租户矩阵。

## 4. 验证与推出

- [x] 4.1 `mvn -pl medkernel-backend test` 已覆盖 27 个审计相关测试。完整套件中 Docker 依赖测试由远端 CI 环境确认。
- [x] 4.2 `docs/backlog.md` 将 GA-ENG-BASE-04 标记为完成。
- [x] 4.3 相关分支已推送并向 `main` 开 PR。
