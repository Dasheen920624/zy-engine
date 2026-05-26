# MedKernel 引擎可观测性骨干 + 临床事件 API 设计

**版本**：v1.0 · 2026-05-27
**关联 backlog**：
- 新增 `GA-ENG-OBS-01 引擎可观测性骨干`（第一层）
- 新增 `GA-ENG-API-01b 标准上下文 retrofit`（第二层）
- 原有 `GA-ENG-API-02 临床事件 API`（第三层）

---

## 1. 背景与目标

### 1.1 背景

GA-ENG-API-01 已落地标准上下文 API（context_snapshot / canonical_resource / clinical_event / context_idempotency_key）。运行此前，需要先建一套**可定位、可回溯**的引擎可观测性基线，以避免在 API-02..13 后续 11 个引擎接口中各自重做。

引擎当前可观测能力盘点：

| 能力 | 现状 | 缺口 |
|---|---|---|
| trace_id 全请求贯通 | RequestContext.traceId | 异步/Kafka/后台线程不强制串联 |
| audit_event SM3 哈希链 | BASE-04 已落地 | 仅"动作级"留痕；状态机跳转无历史，FAILED 时 root cause 在哪里、retry 多少次都不知 |
| ContextSnapshot.trace_id | 已持久化 | canonical_resource 无 trace_id；资源级反查请求不可能 |
| ApiException + ErrorCode | 已统一 | ErrorCode 缺 errorClass 维度，FAILED 时 root cause 不会附加到持久化对象 |
| processing_status 字段 | clinical_event / 后续 API 普遍存在 | 跳转无历史，回滚一次原因即丢 |
| 结构化日志 | logback 默认配置 | MDC 字段未约定，traceId/tenantId/entityId 不在每条日志里 |

### 1.2 目标

让任何引擎实体（snapshot / event / rule / pathway / recommendation / 评估结果 / 随访任务 / 包发布）**满足三条强承诺**：

1. **traceId 全链路串联**：从 HTTP POST → DB → Kafka → Consumer → 状态跳转 → 审计 → 下游触发，任意 traceId 一查到底。
2. **状态历史可追溯**：任何 processing_status / lifecycle_status 跳转都留一条 history，含 from/to/reason/actor/error_*/timestamp。
3. **一键诊断**：任一实体提供 `GET /{id}/diagnose` 返回实体+状态历史+审计列表+关联实体+payloadUri，一次定位完毕。

### 1.3 非目标

- 不引入 ELK / Loki / Grafana 等外部聚合栈（留 BASE-07 监控运维任务做）
- 不做事件溯源完整重建（不是 Event Sourcing；仅是状态历史 + payload 版本化）
- 不替换现有 audit_event SM3 链（仍保留，但状态历史是其补充而非替代）

---

## 2. 设计原则

1. **横切复用**：可观测性能力做成**共享组件**（shared 模块或独立 observability 包），所有引擎 API 强制接入，避免每个 API 重做。
2. **接口先行**：MinIO / 字典映射 / 包版本解析 等外部依赖一律走 Port 接口 + 默认 noop 实现，govcloud profile 可替换实现。
3. **YAGNI**：不做"将来可能用到"的预留字段；缺什么加什么，但加的时候按统一约定。
4. **API-01 兼容**：第一层引入新能力但**不破坏** API-01 的现有 API 契约；第二层用 retrofit 让 API-01 受益。
5. **测试驱动**：每个组件先写测试再实现；状态历史与诊断接口必须有 @DataJdbcTest + @SpringBootTest 双层验证。

---

## 3. 三层规划概览

| 层 | backlog 项 | 主要交付 | 工作量 | 依赖 |
|---|---|---|---|---|
| 第一层 | GA-ENG-OBS-01 | StateTransitionRecorder / PayloadStoragePort / ErrorCode 增强 / DiagnoseResponse / MDC 注入 / TraceIdPropagator / Micrometer Tracing / V8 五方言迁移 | ~10 task / 1-2 PR | 无 |
| 第二层 | GA-ENG-API-01b | context_snapshot 接入 recorder / canonical_resource 加 trace_id / GET /snapshots/{id}/diagnose / PackageVersionResolver SPI 化 | ~5 task / 1 PR | 第一层 |
| 第三层 | GA-ENG-API-02 | Outbox Pattern / Spring Kafka starter / ClinicalEventService / Consumer / MinIO payload / 三 endpoint + diagnose / SnapshotTriggerService | ~20 task / 2-3 PR | 第一层 + 第二层 |

---

## 4. 第一层：GA-ENG-OBS-01 可观测性骨干（详细 spec）

### 4.1 StateTransitionRecorder

**位置**：`com.medkernel.shared.observability.StateTransitionRecorder`

**接口**：

```java
@Component
public class StateTransitionRecorder {

    /**
     * 记录一次实体状态机跳转。
     *
     * @param entityType  实体类型，如 "context_snapshot"、"clinical_event"
     * @param entityId    业务 ID（snapshot_id / event_id）
     * @param fromStatus  起始状态；首次入库时填 null
     * @param toStatus    目标状态
     * @param reason      跳转原因（业务语义，如 "INITIAL_CREATE"、"USER_DISCHARGE_TRIGGER"）
     * @param error       失败时的结构化错误；成功时为 null
     */
    public void record(String entityType, String entityId,
                       String fromStatus, String toStatus,
                       String reason, TransitionError error);
}
```

**实现**：

- 异步任务（@Async TaskExecutor）异步写 `state_transition_history` 表
- 出错只 warn 日志，不阻塞主链路
- traceId / tenantId / actor 从 RequestContext 取，自动注入

**表 schema**（V8 五方言迁移）：

```sql
CREATE TABLE state_transition_history (
    id              BIGINT  GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entity_type     VARCHAR(64)  NOT NULL,
    entity_id       VARCHAR(128) NOT NULL,
    tenant_id       VARCHAR(64)  NOT NULL,
    from_status     VARCHAR(64)  NULL,
    to_status       VARCHAR(64)  NOT NULL,
    reason          VARCHAR(128) NOT NULL,
    actor           VARCHAR(64)  NULL,
    trace_id        VARCHAR(128) NULL,
    error_code      VARCHAR(64)  NULL,
    error_class     VARCHAR(32)  NULL,
    error_message   VARCHAR(512) NULL,
    occurred_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sth_entity      ON state_transition_history (entity_type, entity_id, occurred_at);
CREATE INDEX idx_sth_tenant_time ON state_transition_history (tenant_id, occurred_at);
CREATE INDEX idx_sth_trace       ON state_transition_history (trace_id);
```

### 4.2 PayloadStoragePort

**位置**：`com.medkernel.shared.observability.PayloadStoragePort`

**接口**：

```java
public interface PayloadStoragePort {
    /** 上传 payload，返回 URI（mc://bucket/key 形式） */
    String put(String tenantId, String entityType, String entityId, byte[] payload);

    /** 按 URI 取 payload */
    byte[] get(String uri);

    /** 删除（用于退档清理） */
    void delete(String uri);
}
```

**三套实现**：

| 实现 | profile | 用途 |
|---|---|---|
| `InMemoryPayloadStorage` | default(dev/test) | ConcurrentHashMap，零依赖 |
| `MinioPayloadStorage` | prod / staging | spring-cloud-aws / minio Java SDK |
| `NoopPayloadStorage` | govcloud（早期） | 直接抛 UnsupportedOperationException，强制接入真国产 OSS |

通过 `@ConditionalOnProperty(prefix="medkernel.payload-storage", name="impl")` 切换。

**配置示例**：

```yaml
medkernel:
  payload-storage:
    impl: minio            # in-memory | minio | noop
    minio:
      endpoint: http://minio:9000
      access-key: ${MINIO_ACCESS_KEY}
      secret-key: ${MINIO_SECRET_KEY}
      bucket: medkernel-payloads
```

**URI 规范**：`mc://{bucket}/{tenantId}/{entityType}/{yyyy-MM-dd}/{entityId}.json`

### 4.3 ErrorCode 增强

**当前**：`ErrorCode(code, httpStatus, defaultMessage)`

**增强后**：

```java
public enum ErrorCode {
    BAD_REQUEST("ENG-API-001", 400, "请求参数无效", ErrorClass.INPUT, false),
    DOWNSTREAM_UNAVAILABLE("ENG-SYS-002", 503, "下游服务不可用", ErrorClass.EXTERNAL, true),
    // ...
    ;

    private final String code;
    private final int httpStatus;
    private final String defaultMessage;
    private final ErrorClass errorClass;   // 新增
    private final boolean retryable;       // 新增

    public enum ErrorClass {
        INPUT,        // 输入数据问题
        AUTH,         // 权限/认证问题
        DATA,         // 业务数据不一致
        EXTERNAL,     // 外部依赖
        INTERNAL      // 系统内部错误
    }
}
```

**TransitionError**（持久化错误对象）：

```java
public record TransitionError(
    String errorCode,        // ErrorCode.code()
    String errorClass,       // ErrorCode.errorClass().name()
    String message,          // 摘要（≤512 字符）
    int retryCount,
    Instant nextRetryAt      // 仅 retryable
) {}
```

**改造影响**：现有 16 个 ErrorCode 全部补 errorClass + retryable 两字段；测试加 roundtrip 校验。

### 4.4 DiagnoseResponse 模板

**位置**：`com.medkernel.shared.observability.DiagnoseResponse`

```java
public record DiagnoseResponse(
    String entityType,
    String entityId,
    String tenantId,
    String currentStatus,
    Object entity,                              // 当前完整实体
    List<StateTransitionEntry> stateHistory,    // 状态历史
    List<AuditEventSummary> auditEvents,        // 审计列表
    Map<String, String> relatedEntities,        // key=关联类型, value=ID（如 snapshot_id, payload_uri）
    String payloadUri                           // 可空
) {
    public record StateTransitionEntry(
        String fromStatus, String toStatus, String reason,
        String actor, String traceId,
        TransitionError error,
        Instant occurredAt
    ) {}

    public record AuditEventSummary(
        String action, String resourceType, String resourceId,
        String summary, String traceId, Instant occurredAt
    ) {}
}
```

**装配组件**：`DiagnoseResponseAssembler`（Spring component），各 Service 调用它一次性返回。

### 4.5 MDC 自动注入

**位置**：`com.medkernel.shared.observability.MdcEnrichmentFilter`

- ServletFilter 在请求进入时把 `traceId/tenantId/userId` 写入 MDC，请求完成清理
- `@Async` / Kafka consumer 通过 `TraceIdPropagator` 显式复制 MDC

**logback 默认 pattern 升级**：JSON-line，包含 timestamp / level / logger / thread / message / mdc.* / exception。

### 4.6 TraceIdPropagator

**位置**：`com.medkernel.shared.observability.TraceIdPropagator`

```java
public final class TraceIdPropagator {
    /** 把当前 RequestContext 的 traceId 注入 Kafka header */
    public static void injectIntoHeaders(Headers headers);

    /** 从 Kafka header 取出 traceId 回填 RequestContext + MDC */
    public static void extractFromHeaders(Headers headers);

    /** 用于 @Async：返回 Runnable 包装，自动传递 MDC */
    public static Runnable wrap(Runnable task);
    public static <T> Callable<T> wrap(Callable<T> task);
}
```

### 4.7 Micrometer Tracing 集成

- pom.xml 引入 `spring-boot-starter-actuator` + `micrometer-tracing-bridge-brave` + `zipkin-reporter-brave`
- 默认 disabled，govcloud / prod profile 启用
- 自动覆盖 controller → service → DataSource → KafkaTemplate → restTemplate

### 4.8 V8 五方言迁移（第一层）

`db/migration/{h2,postgres,oracle,dm,kingbase}/V8__observability_baseline.sql`：

```sql
-- 1. 全局状态历史表
CREATE TABLE state_transition_history (...);

-- 2. canonical_resource 加 trace_id 字段（API-01 retrofit 第二层会用，但 schema 在第一层先加）
ALTER TABLE canonical_resource ADD COLUMN trace_id VARCHAR(128) NULL;
```

> **第三层会另立 V9 迁移**（见 §6.1），不复用 V8，避免跨 PR 共享迁移版本号导致 Flyway 冲突。

---

## 5. 第二层：GA-ENG-API-01b retrofit（概述）

让 API-01 享受第一层能力：

1. **ContextSnapshotService 接入 StateTransitionRecorder**：create() 调用 `recorder.record("context_snapshot", id, null, "ACTIVE", "INITIAL_CREATE", null)`
2. **canonical_resource 持久化 trace_id**：CanonicalResource entity 加 traceId 字段，Service 创建时从 RequestContext 取
3. **新增 `GET /api/v1/engine/context/snapshots/{snapshotId}/diagnose`**：返回 DiagnoseResponse
4. **PackageVersionResolver 抽象为 SPI**：定义 `PackageVersionPort` 接口，当前 stub "非空即合法" 改为默认实现；后续 API-10 提供真实实现
5. **AuditEvent 失败留痕**：当 ContextSnapshotService 抛 ApiException 时也发一次 audit event（action=EXECUTE / outcome=FAILED）

---

## 6. 第三层：GA-ENG-API-02 临床事件 API（概述）

基于前两层基线，落地 Event API：

### 6.1 V9 五方言迁移（第三层）

`db/migration/{h2,postgres,oracle,dm,kingbase}/V9__clinical_event_outbox.sql`：

```sql
CREATE TABLE clinical_event_outbox (
    id              BIGINT  GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id        VARCHAR(64)  NOT NULL,
    tenant_id       VARCHAR(64)  NOT NULL,
    topic           VARCHAR(128) NOT NULL,
    payload_uri     VARCHAR(256) NULL,
    relay_status    VARCHAR(32)  NOT NULL DEFAULT 'PENDING',  -- PENDING/RELAYED/FAILED
    relay_attempts  INT          NOT NULL DEFAULT 0,
    next_relay_at   TIMESTAMP    NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    relayed_at      TIMESTAMP    NULL,
    CONSTRAINT uk_outbox_event_id UNIQUE (event_id)
);

-- clinical_event 表加列
ALTER TABLE clinical_event ADD COLUMN payload_uri VARCHAR(256) NULL;
ALTER TABLE clinical_event ADD COLUMN error_code VARCHAR(64) NULL;
ALTER TABLE clinical_event ADD COLUMN error_class VARCHAR(32) NULL;
ALTER TABLE clinical_event ADD COLUMN retry_count INT NOT NULL DEFAULT 0;
```

### 6.2 endpoint

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/v1/engine/events` | event.write | 同步处理：DB + outbox + MinIO 同事务完成；返回 PROCESSED 或 FAILED |
| POST | `/api/v1/engine/events/async` | event.write | 异步：DB + outbox 入库后立即 202；后台 relay → consumer 处理 |
| POST | `/api/v1/engine/events/batch` | event.write | 批量异步：一次最多 N 条；返回 batchId + eventIds |
| GET | `/api/v1/engine/events/{eventId}` | event.read | 实体查询 |
| GET | `/api/v1/engine/events/{eventId}/diagnose` | event.read | 一键诊断（DiagnoseResponse） |
| GET | `/api/v1/engine/events?patientId=...` | event.read | 列表查询 |

### 6.3 处理链路

```
POST /events
  ↓ tx { INSERT clinical_event (RECEIVED) → INSERT clinical_event_outbox → INSERT state_transition_history }
  ↓ commit
  ↓
  Sync: 200 OK with status
  Async: 202 Accepted with eventId
  ↓
OutboxRelay (固定间隔 5s)
  ↓ poll PENDING outbox → MinIO.put(payload) → KafkaTemplate.send(clinical-events topic)
  ↓ mark relayed
  ↓
ClinicalEventConsumer
  ↓ extract traceId from header → MDC
  ↓ payload 字典映射检查 → status RECEIVED→MAPPED + history
  ↓ 业务规则检查（如 source_system 合法、event_type 合法）→ status MAPPED→PROCESSED + history + AuditEvent
  ↓ 发 ApplicationEvent ClinicalEventProcessedEvent
  ↓
SnapshotTriggerService listens
  ↓ 按 EventType 判断（DISCHARGE/ADMISSION/REPORT 触发；其余跳过）
  ↓ 调 ContextSnapshotService.create()（共享 traceId）
```

### 6.4 重试与死信

Spring Kafka 内建：

- consumer 失败 → 重试 3 次（指数退避 1s/5s/30s）
- 仍失败 → 发到 DLQ topic `clinical-events.DLT`
- DLT consumer 写状态 FAILED + history(error_code/error_class/error_message)

### 6.5 PermissionCode 新增

- `EVENT_READ`（"event.read", LOW）
- `EVENT_WRITE`（"event.write", MEDIUM）

### 6.6 ErrorCode 新增

- `ENG_EVENT_001` 400 INPUT "事件 schema 校验失败"
- `ENG_EVENT_002` 409 INPUT "事件 ID 已存在"
- `ENG_EVENT_003` 503 EXTERNAL retryable "payload 存储不可用"
- `ENG_EVENT_004` 503 EXTERNAL retryable "Kafka 不可用"
- `ENG_EVENT_005` 500 INTERNAL "事件处理失败已进入死信"

### 6.7 默认权限策略

- `EVENT_READ`：DOCTOR / NURSE / SPECIALIST / DEPT_HEAD / MEDICAL_AFFAIRS / QA_MANAGER / AUDIT_COMPLIANCE / IT_OPS / IMPLEMENTATION_ENGINEER
- `EVENT_WRITE`：IT_OPS / IMPLEMENTATION_ENGINEER / HOSPITAL_ADMIN / PLATFORM_ADMIN / GROUP_ADMIN（仅数据接入角色）

### 6.8 配置（双轨 broker）

```yaml
medkernel:
  events:
    kafka:
      bootstrap-servers: ${KAFKA_BROKERS:localhost:9092}
      topic-events: clinical-events
      topic-dlt: clinical-events.DLT
      consumer-group: medkernel-event-consumer
      max-retries: 3
      retry-backoff-ms: 1000
```

- **dev/test**：`spring-kafka-test` 嵌入式 Kafka broker
- **CI（docker available）**：testcontainers-kafka
- **govcloud / prod**：配置指向真实 broker（阿里云 Kafka / 华为云 DMS / 国产 Kafka 兼容）

---

## 7. 数据模型变更总览

| 表 | 变更 | 迁移版本 | 层 |
|---|---|---|---|
| `state_transition_history` | 新建 | V8 | 第一层 |
| `canonical_resource` | ADD trace_id | V8 | 第一层 |
| `clinical_event` | ADD payload_uri/error_code/error_class/retry_count | V9 | 第三层 |
| `clinical_event_outbox` | 新建 | V9 | 第三层 |

---

## 8. 权限码与错误码新增总览

| 项 | 第一层 | 第二层 | 第三层 |
|---|---|---|---|
| PermissionCode | (无) | (无) | EVENT_READ / EVENT_WRITE |
| ErrorCode | 现有 16 个补 errorClass + retryable | (无) | ENG_EVENT_001..005 |

---

## 9. 非范围

- ❌ 完整 Event Sourcing：仅状态历史 + payload 版本化
- ❌ ELK/Loki/Grafana 集成：BASE-07 监控运维任务做
- ❌ 跨服务分布式 trace（OpenTelemetry exporter 配置）：留 govcloud profile 增强
- ❌ 事件回放 API：留 API-02b 后续
- ❌ 事件死信查询 API：留 API-02b 后续
- ❌ 事件回调 URL：留 API-02b 后续

---

## 10. 回退策略

- 第一层每个组件可独立开关（`@ConditionalOnProperty(prefix="medkernel.obs", name="enabled")`），异常时不影响主链路
- StateTransitionRecorder 异步写历史失败仅 warn 日志，不阻塞业务
- PayloadStoragePort 失败时 ClinicalEventService 标记 FAILED 不阻塞 outbox 入库
- Kafka 不可用时 outbox 累积，OutboxRelay 持续重试，业务 API 仍可写入
- V8 迁移如有问题可降级（state_transition_history / outbox 表 drop 即回到 V7 状态，主链路不依赖）

---

## 11. 风险与缓解

| 风险 | 缓解 |
|---|---|
| MinIO 在 govcloud 无对应实现 | NoopPayloadStorage 抛错强制提示；govcloud profile 配置时必须指定真实国产 OSS 实现 |
| Kafka broker 不可用 | outbox 累积 + 持续重试；监控告警 OutboxRelay 滞后 |
| 状态历史表膨胀 | 90 天分区/归档（BASE-07 备份恢复策略覆盖） |
| trace_id 跨异步丢失 | TraceIdPropagator + Spring TaskDecorator 强制注入；测试用例覆盖 |
| 三层串联依赖增长 | 每层独立 PR + 各自验证；第二/三层执行前重跑第一层 smoke |

---

## 12. 验收

### 12.1 第一层验收

- [ ] V8 五方言迁移合同测试 + H2 baseline 通过
- [ ] StateTransitionRecorder 单测覆盖：成功路径 / 异步失败仅 warn / RequestContext 自动注入
- [ ] PayloadStoragePort 三实现切换测试（InMemory / Minio testcontainer / Noop 抛错）
- [ ] ErrorCode 全 enum 都有 errorClass + retryable，roundtrip 测试
- [ ] DiagnoseResponseAssembler 单测覆盖：snapshot 装配出完整历史
- [ ] MdcEnrichmentFilter integration test：MDC 字段写入与清理
- [ ] TraceIdPropagator 单测：Kafka header inject/extract、@Async wrap
- [ ] 后端完整测试 ≥ 230 用例 / 全绿
- [ ] 前端 lint/typecheck/test/build 全绿

### 12.2 第二层验收

- [ ] ContextSnapshotService.create() 写一条 state_transition_history
- [ ] canonical_resource 持久化时 trace_id 来自 RequestContext
- [ ] GET /snapshots/{id}/diagnose 返回完整 DiagnoseResponse
- [ ] PackageVersionPort 抽象 + 默认实现单测
- [ ] 既有 API-01 测试全绿（无回归）

### 12.3 第三层验收

- [ ] V8 outbox 迁移 + clinical_event alter 五方言通过
- [ ] ClinicalEventService 单测覆盖：同步创建 / 异步创建 / 批量入库 / 同事件幂等 / FAILED 状态写 history
- [ ] OutboxRelay 单测：PENDING → RELAYED；失败重试上限进 FAILED
- [ ] ClinicalEventConsumer 单测（embedded Kafka）：traceId 跨 header 传递 / 字典映射 / 状态推进
- [ ] DLT 死信测试：重试 3 次失败后状态 FAILED + history error_class=EXTERNAL
- [ ] SnapshotTriggerService 单测：DISCHARGE 触发 API-01；ORDER 跳过
- [ ] Controller 安全测试：未授权 403 / 缺 tenant 400 / event.write 角色 + tenant 通过
- [ ] DefaultPermissionPolicy 加 EVENT_READ/WRITE 映射测试
- [ ] GET /events/{id}/diagnose 返回完整 DiagnoseResponse
- [ ] 后端全测 ≥ 280 用例 / 全绿

---

## 13. 实施顺序

1. **第一层（GA-ENG-OBS-01）**：先 spec→plan→实施→PR，独立合入 main
2. **第二层（GA-ENG-API-01b）**：第一层合入后，spec→plan→实施→PR
3. **第三层（GA-ENG-API-02）**：第二层合入后，分 2-3 个子 PR：(a) V8 迁移 + outbox + DTO；(b) Kafka producer/consumer + Service + Controller；(c) SnapshotTriggerService + 文档闭环

每层 PR 合入前必须：
- 后端全测全绿
- 前端 lint/typecheck/test/build 全绿
- 迁移合同测试通过
- 安全测试通过

---

**End of design.**
