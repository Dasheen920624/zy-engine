# Feature Acceptance: NOTIFY-001 通知和消息中心

fa_id: FA-NOTIFY-001-S01
task_id: NOTIFY-001
claim_id: NOTIFY-001-S01
owner: TraeAI-Main
status: PENDING
quality_level: SILVER
created_at: 2026-05-23T11:00+08:00

## 功能验收清单

### 1. 通知主 CRUD 持久化
- [x] NotificationRepository 继承 PersistenceRepositorySupport，支持 H2/Oracle 双模式
- [x] NotificationService 优先使用 Repository 持久化，未启用时回退到内存存储
- [x] 通知创建使用 UUID 生成 notificationCode（NOTIFY-XXXXXXXX）
- [x] 通知列表查询支持 tenantId/recipientId/status/notificationType/priority 过滤
- [x] 标记已读/批量已读/归档功能正常
- [x] 未读计数和统计使用 SQL 聚合查询
- [x] 过期通知清理功能

### 2. 渠道配置（NOTIFY_CHANNEL_CONFIG）
- [x] saveChannelConfig / listChannelConfigs / getChannelConfig
- [x] UPSERT 语义，唯一键 tenant_id + channel_code
- [x] Controller 端点: POST/GET /api/notifications/channels

### 3. 通知模板（NOTIFY_TEMPLATE）
- [x] saveTemplate / listTemplates / getTemplate
- [x] UPSERT 语义，唯一键 tenant_id + template_code + channel
- [x] Controller 端点: POST/GET /api/notifications/templates

### 4. 用户订阅设置（NOTIFY_SUBSCRIPTION）
- [x] saveSubscription / listSubscriptions / updateSubscription
- [x] batchSaveSubscriptions 批量保存
- [x] Controller 端点: POST/GET/PUT /api/notifications/subscriptions

### 5. 投递日志（NOTIFY_DELIVERY_LOG）
- [x] saveDeliveryLog / listDeliveryLogs
- [x] Controller 端点: GET /api/notifications/{id}/delivery-logs

### 6. 工作流联动
- [x] createWorkflowNotification 方法
- [x] Controller 端点: POST /api/notifications/workflow
- [x] 自动关联 businessType/businessId/businessUrl

### 7. 前端
- [x] notification.ts API 新增渠道配置/模板/订阅/投递日志/工作流联动类型和方法
- [x] NotificationSettings 页面接入后端 API（加载订阅设置 + 批量保存）
- [x] TypeScript 编译通过
- [x] ESLint 检查通过
- [x] Vite build 成功

### 8. DDL 同步
- [x] H2 本地 DDL 已存在（ai-dev-input/04_database/local/notify_ddl.sql）
- [x] PG DDL 已存在（ai-dev-input/04_database/pg/notify_ddl.sql）
- [x] Oracle DDL 已存在（ai-dev-input/04_database/oracle/notify_ddl.sql）
- [x] DM DDL 已存在（ai-dev-input/04_database/dm/notify_ddl.sql）
- [x] H2 DDL 已在 EnginePersistenceService.loadLocalSchemaStatements 中注册

### 9. 安全与隐私
- [x] 所有端点使用 OrganizationContextService 做组织上下文解析和过滤
- [x] 租户隔离：所有查询自动注入 tenantId 过滤
- [x] 无硬编码单医院逻辑

## 验证结果

| 检查项 | 结果 |
|--------|------|
| mvn compile | PASS |
| tsc --noEmit | PASS |
| eslint --max-warnings 0 | PASS |
| vite build | PASS |
| check-ai-collaboration | PASS (0 active claims before this) |
| verify-task-prereq | PASS (19 PASS / 0 FAIL / 1 WARN) |
