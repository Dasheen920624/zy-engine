# Feature Acceptance: ADAPT-004 适配器运行日志

fa_id: FA-ADAPT-004-S01
task_id: ADAPT-004
claim_id: ADAPT-004-S01
owner: TraeAI-Main
status: PENDING
quality_level: SILVER
created_at: 2026-05-23T16:00+08:00

## 功能验收清单

### 1. AdapterExecutionLogService
- [x] 内存环形缓冲（ConcurrentLinkedDeque，容量500）
- [x] recordCallLog() — 记录适配器调用日志
- [x] listCallLogs() — 支持 adapterCode/queryCode/traceId/status/tenantId/hospitalCode/patientId 过滤
- [x] getCallLog() — 按 traceId 查找单条
- [x] summarizeCallLogs() — 统计 total/success_count/error_count/timeout_count/average_elapsed_ms/by_adapter/by_status/by_hospital_code
- [x] cleanupOldLogs() — 清理超过指定小时数的日志

### 2. AdapterExecutionLogController
- [x] GET /api/adapters/execution-logs — 查询日志列表（DTO + @Valid）
- [x] GET /api/adapters/execution-logs/{traceId} — 获取单条日志
- [x] GET /api/adapters/execution-logs/summary — 统计汇总
- [x] POST /api/adapters/execution-logs/cleanup — 清理旧日志（DTO + @Valid）
- [x] 所有端点使用 OrganizationContextService 做组织上下文过滤
- [x] 所有返回值使用 ApiResult<T> 包装

### 3. DTO
- [x] AdapterCallLogQueryRequest — 查询请求 DTO
- [x] AdapterCallLogCleanupRequest — 清理请求 DTO（@Min(1)）

### 4. 自动记录
- [x] AdapterHubService.audit() 中集成自动记录执行日志
- [x] 适配器调用时自动记录 traceId/adapterCode/queryCode/status/elapsedMs/patientId/encounterId
- [x] 执行日志记录失败不影响主链路

### 5. 验证
- [x] mvn compile PASS

## 验证结果

| 检查项 | 结果 |
|--------|------|
| mvn compile | PASS |
