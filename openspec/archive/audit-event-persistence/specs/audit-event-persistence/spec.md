## ADDED Requirements

### Requirement: 持久化审计事件

系统 SHALL 将已发布的审计事件写入数据库，并保存行为、操作者、组织范围、资源标识、traceId、载荷摘要和时间戳。

#### Scenario: 业务事务提交后写入审计

- **WHEN** 业务代码通过 `AuditEventPublisher` 发布审计事件且外层事务成功提交
- **THEN** 系统在新的事务中写入 `audit_event`
- **AND** 审计写入不会改变原业务事务的提交结果

### Requirement: 租户级防篡改签名链

系统 SHALL 为每个租户维护独立签名链，使用前一事件签名连接当前事件，形成可验证的顺序证据。

#### Scenario: 首个事件

- **WHEN** 某租户写入第一条审计事件
- **THEN** `prev_signature` 使用 `GENESIS`
- **AND** `audit_chain_head` 更新为该事件签名

#### Scenario: 后续事件

- **WHEN** 同一租户继续写入审计事件
- **THEN** 当前事件引用上一事件和上一签名
- **AND** 链头在同一写入事务中推进

#### Scenario: 篡改检测

- **WHEN** 审计记录的关键字段被篡改
- **THEN** 签名校验失败

### Requirement: 租户隔离审计查询

系统 SHALL 按当前 `RequestContext` 限制审计查询范围，不允许跨租户读取。

#### Scenario: 缺失租户上下文

- **WHEN** 调用方请求审计事件列表但没有租户上下文
- **THEN** API 返回租户上下文缺失错误

#### Scenario: 带过滤条件查询

- **WHEN** 调用方按行为、资源、操作者或时间范围查询
- **THEN** 系统只返回当前租户内匹配的数据
- **AND** 结果使用服务端游标分页

### Requirement: 真实合规审计 API

系统 SHALL 通过合规审计 API 返回真实持久化审计数据，而不是示例数据。

#### Scenario: 查询审计事件

- **WHEN** 具备 `audit.read` 权限的用户调用 `/api/v1/compliance/audit/events`
- **THEN** 返回真实审计记录和下一页游标

#### Scenario: 导出快照

- **WHEN** 具备 `audit.export` 权限的用户调用 `/api/v1/compliance/audit/snapshot`
- **THEN** 系统发布并持久化 `EXPORT` 审计事件
- **AND** 返回可追踪的审计记录视图

### Requirement: 持久化失败可观测

系统 SHALL 对审计持久化失败记录结构化日志和指标。

#### Scenario: 审计写入失败

- **WHEN** 审计持久化过程中出现异常
- **THEN** 业务调用方不收到审计异常
- **AND** 失败指标递增并记录事件标识、租户和 traceId
