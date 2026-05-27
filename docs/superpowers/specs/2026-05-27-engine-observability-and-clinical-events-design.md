# MedKernel 引擎可观测性骨干 + 临床事件 API 设计

**版本**：v2.0 · 2026-05-27
**关联 backlog**：
- 新增 `GA-ENG-OBS-01 引擎可观测性骨干`（第一层）
- 新增 `GA-ENG-API-01b 标准上下文 retrofit`（第二层）
- 原有 `GA-ENG-API-02 临床事件 API`（第三层）

> **v2.0 变更**：根据用户"全流程管理 / 可追溯 / 可排查 / 可定位 / 可复现 / 引入最少组件"诉求，整体方案从 Kafka+MinIO+Zipkin 三外部组件改为 **DB-Only 零新外部依赖**。Payload 改用 **独立旁路表**（`clinical_event_payload`），主表保持精简，未来切 OSS 改造点局部、主表零改动。

---

## 1. 设计哲学

> **"用最少手段达成所有目标"才是工程上的最完美**

医疗内核引擎的"完美"必须同时满足八条约束：

1. **可靠性**：医疗数据不可丢
2. **可定位**：任何问题用 traceId 5 分钟定位
3. **可复现**：任何事件可重放
4. **可演进**：医联体规模可扩展
5. **可运维**：备份/恢复/升级简单
6. **可审计**：每个动作留痕不可篡改
7. **国产化**：信创要求
8. **零外部依赖**：故障面小、教学成本低

医疗事件量被业务天然封顶：单三甲日 < 100 万，集团医联体 < 1000 万。峰值 < 1000 msg/s——**DB-Only 完全 hold 住**。引入 Kafka 反而增加故障面、增加 traceId 跨组件追问题的复杂度。

---

## 2. 背景与目标

### 2.1 背景

GA-ENG-API-01 已落地标准上下文 API。运行此前，需要先建一套**可定位、可回溯**的引擎可观测性基线，让 API-02..13 后续 11 个引擎接口直接复用，不再各自重做。

引擎当前可观测能力盘点：

| 能力 | 现状 | 缺口 |
|---|---|---|
| trace_id 全请求贯通 | RequestContext.traceId（同步） | 异步/后台任务 traceId 不强制串联 |
| audit_event SM3 哈希链 | BASE-04 已落地 | 仅"动作级"留痕；状态机跳转无历史，FAILED 时 root cause / retry 多少次都不知 |
| ContextSnapshot.trace_id | 已持久化 | canonical_resource 无 trace_id；资源级反查请求不可能 |
| ApiException + ErrorCode | 已统一 | ErrorCode 缺 errorClass 维度，FAILED 时 root cause 不会附加到持久化对象 |
| processing_status 字段 | clinical_event / 后续 API 普遍存在 | 跳转无历史，回滚一次原因即丢 |
| 结构化日志 | logback 默认配置 | MDC 字段未约定，traceId/tenantId/entityId 不在每条日志里 |

### 2.2 目标

让任何引擎实体（snapshot / event / rule / pathway / recommendation / 评估结果 / 随访任务 / 包发布）满足三条强承诺：

1. **traceId 全链路串联**：从 HTTP POST → DB → 后台 Worker → 状态跳转 → 审计 → 下游触发，任意 traceId 一查到底。
2. **状态历史可追溯**：任何 processing_status / lifecycle_status 跳转都留一条 history，含 from/to/reason/actor/error_*/timestamp。
3. **一键诊断 + 反向追踪**：
   - `GET /{entity}/{id}/diagnose` 返回实体+状态历史+审计列表+关联实体+payload，一次定位完毕
   - `GET /diagnose/trace/{traceId}` 横向反向追踪同一 traceId 涉及的全部实体

### 2.3 非目标

- ❌ 不引入 Kafka / Pulsar / RabbitMQ 等外部 MQ
- ❌ 不引入 MinIO / S3 等外部对象存储
- ❌ 不引入 Zipkin / Jaeger 等外部 trace 后端（仅用 Spring Boot 3.3 内置 Micrometer Observation + MDC）
- ❌ 不做完整 Event Sourcing（仅状态历史 + payload 持久化）
- ❌ 不做事件回放/死信查询/回调 URL（留 API-02b 后续）

---

## 3. 设计原则

1. **零新外部组件**：所有能力靠 DB（已有五方言迁移）+ Spring 内建（已在 pom）实现
2. **横切复用**：可观测性做成共享组件，所有引擎 API 强制接入
3. **接口先行**：Payload / 字典映射 / 包版本解析 等走 Port 接口，未来切 OSS / 外部依赖可无缝替换
4. **YAGNI**：不做"将来可能用到"的预留字段；架构留扩展点（如 payload 旁路表的 `storage_type`/`payload_uri`）
5. **API-01 兼容**：第一层引入新能力不破坏 API-01 现有契约；第二层 retrofit 让 API-01 受益
6. **测试驱动**：每个组件先写测试再实现；状态历史与诊断接口必须有 @DataJdbcTest + @SpringBootTest 双层验证

---

## 4. 三层规划概览

| 层 | backlog 项 | 主要交付 | 工作量 | 新外部组件 | 依赖 |
|---|---|---|---|---|---|
| 第一层 | GA-ENG-OBS-01 | StateTransitionRecorder / PayloadStoragePort（DB 旁路实现） / ErrorCode 增强 / DiagnoseResponse / MDC 注入 / TraceIdPropagator / V8 五方言迁移 | ~9 task / 1 PR | **0** | 无 |
| 第二层 | GA-ENG-API-01b | snapshot 接 recorder / canonical_resource 加 trace_id / GET /snapshots/{id}/diagnose / PackageVersionPort 抽象 | ~5 task / 1 PR | **0** | 第一层 |
| 第三层 | GA-ENG-API-02 | V9 outbox + clinical_event_payload 表 / OutboxWorker / ClinicalEventService / SnapshotTriggerService / 三 endpoint + diagnose + replay | ~16 task / 2 PR | **0** | 第一层 + 第二层 |

---

## 5. 第一层：GA-ENG-OBS-01 可观测性骨干（详细 spec）

### 5.1 StateTransitionRecorder

**位置**：`com.medkernel.shared.observability.StateTransitionRecorder`

**职责**：所有引擎状态机跳转一律由此组件记录历史。

**接口**：

```java
@Component
public class StateTransitionRecorder {

    /**
     * 记录一次实体状态机跳转。
     *
     * @param entityType  实体类型，约定常量，如 "context_snapshot"、"clinical_event"
     * @param entityId    业务 ID（snapshot_id / event_id）
     * @param fromStatus  起始状态；首次入库时填 null
     * @param toStatus    目标状态
     * @param reason      跳转原因（业务语义，如 "INITIAL_CREATE"、"TERMINOLOGY_OK"、"DISCHARGE_TRIGGER"）
     * @param error       失败时的结构化错误；成功时为 null
     */
    public void record(String entityType, String entityId,
                       String fromStatus, String toStatus,
                       String reason, TransitionError error);
}

public record TransitionError(
    String errorCode,        // ErrorCode.code()
    String errorClass,       // INPUT/AUTH/DATA/EXTERNAL/INTERNAL
    String message,          // 摘要 ≤ 512 字符
    int retryCount,
    Instant nextRetryAt      // 仅 retryable 错误
) {}
```

**实现要点**：

- **同事务写历史**：业务事务内调用 recorder → INSERT 写入与业务一同提交/回滚（强一致：业务回滚则历史回滚，不会出现"业务无效但历史保留"导致追溯失真）
- traceId / tenantId / actor 从 `RequestContext` 自动注入
- recorder 内部 try-catch 兜底：捕获非预期 RuntimeException（如 NPE、序列化异常）转为 `WARN` 日志 + `state_transition_recorder.error` Micrometer counter，避免可观测性组件自身 bug 反噬业务；DataAccessException 仍向上抛由业务事务回滚

**表 schema**（V8 五方言迁移）：

```sql
CREATE TABLE state_transition_history (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
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
    retry_count     INT          NULL,
    next_retry_at   TIMESTAMP    NULL,
    occurred_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_sth_error_class CHECK (error_class IS NULL OR error_class IN ('INPUT','AUTH','DATA','EXTERNAL','INTERNAL'))
);

CREATE INDEX idx_sth_entity      ON state_transition_history (entity_type, entity_id, occurred_at);
CREATE INDEX idx_sth_tenant_time ON state_transition_history (tenant_id, occurred_at);
CREATE INDEX idx_sth_trace       ON state_transition_history (trace_id);
CREATE INDEX idx_sth_failed      ON state_transition_history (tenant_id, error_class, occurred_at);
```

### 5.2 PayloadStoragePort

**位置**：`com.medkernel.shared.observability.PayloadStoragePort`

**职责**：抽象 payload 的"持久化与读取"，与底层存储介质解耦。第一阶段实现写入旁路表（DB CLOB/JSONB），未来切 OSS 时新增 `OssPayloadStoragePort` 实现 + 配置切换即可。

**接口**：

```java
public interface PayloadStoragePort {

    /**
     * 持久化 payload，返回引用（旁路表 row 主键的封装）。
     *
     * @param descriptor 元信息：tenantId / entityType / entityId / contentType
     * @param payload    原始字节，调用方负责字符集
     * @return           PayloadRef，包含 digest 与定位信息
     */
    PayloadRef put(PayloadDescriptor descriptor, byte[] payload);

    /** 按 PayloadRef 取 payload；不存在时抛 ApiException(ENG-OBS-001) */
    byte[] get(PayloadRef ref);

    /** 软删除（标记 deleted_at）；GA 阶段不真正物理删除，由归档任务统一处理 */
    void delete(PayloadRef ref);
}

public record PayloadDescriptor(
    String tenantId,
    String entityType,
    String entityId,
    String contentType        // "application/json" / "application/cda+xml" 等
) {}

public record PayloadRef(
    String storageType,       // "INLINE" | "URI"
    String digest,            // SM3 摘要
    String uri,               // 当 storageType=URI 时填外部存储 URI；INLINE 时填表名+主键
    long sizeBytes
) {}
```

**默认实现**：`DbPayloadStorage`

- 写入 `clinical_event_payload`（第三层时新建；第一层提供 abstract base class）
- 计算 SM3 digest
- 同事务写入，保证 payload 与 metadata 一致性
- 返回 `PayloadRef(storageType="INLINE", uri="db://clinical_event_payload/{event_id}")`

**演进**：未来引入 OSS 时新增 `OssPayloadStorage` 实现 + `@ConditionalOnProperty(prefix="medkernel.payload-storage", name="impl", havingValue="oss")`；现有数据保持 INLINE，新数据走 URI；老数据可由后台任务渐进迁移。

### 5.3 ErrorCode 增强

**当前**：`ErrorCode(code, httpStatus, defaultMessage)` 共 16 项

**增强**：补 `errorClass`（5 类）+ `retryable`（boolean）两字段

```java
public enum ErrorCode {
    BAD_REQUEST("ENG-API-001", 400, "请求参数无效", ErrorClass.INPUT, false),
    VALIDATION_FAILED("ENG-API-002", 400, "请求参数校验失败", ErrorClass.INPUT, false),
    UNAUTHORIZED("ENG-API-003", 401, "未授权访问", ErrorClass.AUTH, false),
    FORBIDDEN("ENG-API-004", 403, "无权限执行该操作", ErrorClass.AUTH, false),
    NOT_FOUND("ENG-API-005", 404, "资源不存在", ErrorClass.DATA, false),
    CONFLICT("ENG-API-007", 409, "资源冲突", ErrorClass.DATA, false),
    TOO_MANY_REQUESTS("ENG-API-008", 429, "请求过于频繁", ErrorClass.INPUT, true),
    DOWNSTREAM_UNAVAILABLE("ENG-SYS-002", 503, "下游服务不可用", ErrorClass.EXTERNAL, true),
    INTERNAL_ERROR("ENG-SYS-001", 500, "服务内部错误", ErrorClass.INTERNAL, false),
    // ...（其余按类型对齐）
    ;

    public enum ErrorClass {
        INPUT,        // 输入数据问题：客户端可修复
        AUTH,         // 权限/认证问题
        DATA,         // 业务数据不一致：管理员排查
        EXTERNAL,     // 外部依赖：可重试
        INTERNAL      // 系统内部错误：研发排查
    }
}
```

**衍生**：
- `ApiException.errorClass()` / `retryable()` 便利方法
- `GlobalExceptionHandler` 在响应里返回 errorClass（用于客户端决策是否重试）
- `StateTransitionRecorder` 收到的 `TransitionError.errorClass` 直接来自 `ErrorCode.errorClass`

### 5.4 DiagnoseResponse 模板

**位置**：`com.medkernel.shared.observability.DiagnoseResponse`

```java
public record DiagnoseResponse(
    String entityType,
    String entityId,
    String tenantId,
    String currentStatus,
    Object entity,                                  // 当前完整实体
    List<StateTransitionEntry> stateHistory,        // 按 occurredAt 升序
    List<AuditEventSummary> auditEvents,            // 关联 audit_event
    Map<String, List<String>> relatedEntities,      // type → IDs（如 "context_snapshot" → ["ctx-..."]）
    PayloadSummary payloadSummary,                  // 不返 payload 本身，仅元信息
    String traceId,
    DiagnoseLinks links                             // 自描述：本接口可调用的相关 API
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

    public record PayloadSummary(
        String digest, long sizeBytes, String contentType,
        String storageType, String fetchUri        // 调用方按 fetchUri 主动取
    ) {}

    public record DiagnoseLinks(
        String self,
        String fetchPayload,                        // 可空：本实体有 payload 时填
        String traceTimeline                        // /diagnose/trace/{traceId}
    ) {}
}
```

**装配组件**：`DiagnoseResponseAssembler`（@Component），各实体的 diagnose 端点都调用它，避免重复装配逻辑。

### 5.5 MDC 自动注入

**位置**：`com.medkernel.shared.observability.MdcEnrichmentFilter`

ServletFilter：请求进入 → 写 `traceId / tenantId / userId / requestPath` 到 MDC；请求结束 → 清理 MDC。

**logback pattern 升级**：默认 + `dev` profile 保持 console pattern；`prod` / `govcloud` profile 切 `LogstashEncoder` JSON 输出，字段含 `mdc.*`。

**约定 MDC key**：

```
traceId          全链路 trace ID
tenantId         当前租户
userId           当前用户
requestPath      请求路径（仅 web 请求）
entityType       当前操作的实体类型（在 service 层手动 push/pop）
entityId         当前操作的实体 ID
```

### 5.6 TraceIdPropagator

**位置**：`com.medkernel.shared.observability.TraceIdPropagator`

```java
public final class TraceIdPropagator {

    /** 用于 @Async / TaskExecutor：包装 Runnable 自动复制 MDC 与 RequestContext */
    public static Runnable wrap(Runnable task);

    /** 同上，泛型 Callable */
    public static <T> Callable<T> wrap(Callable<T> task);

    /** 后台 Worker（OutboxWorker / @Scheduled）从 DB 恢复 traceId 到 RequestContext + MDC */
    public static void restoreFromTrace(String traceId, String tenantId, String userId);

    /** 当前线程清理 */
    public static void clear();
}
```

**Spring 集成**：注入自定义 `TaskDecorator` 到 `ThreadPoolTaskExecutor`：

```java
@Configuration
public class AsyncTaskExecutorConfig {
    @Bean
    public TaskDecorator traceTaskDecorator() {
        return TraceIdPropagator::wrap;
    }
}
```

### 5.7 Micrometer Observation（Spring 内置）

Spring Boot 3.3 已内置 `spring-boot-starter-actuator`（已在 pom）含 Micrometer Observation API。**不引入** brave/zipkin/otel exporter。

收益：
- HTTP controller / `RestClient` / JDBC 调用 / `@Async` 自动埋观察点
- traceId/spanId 自动生成，写入 MDC（通过 `MicrometerObservationConfig` 桥接）
- `/actuator/metrics` 暴露调用次数 / 时长 / 失败率

留扩展点：未来要接 Zipkin/Jaeger 时只需加 `micrometer-tracing-bridge-brave + zipkin-reporter-brave` 依赖 + 配置 exporter URL，业务代码零改动。

### 5.8 V8 五方言迁移（第一层）

`db/migration/{h2,postgres,oracle,dm,kingbase}/V8__observability_baseline.sql`：

```sql
-- 1. 全局状态历史表
CREATE TABLE state_transition_history (...);

-- 2. canonical_resource 加 trace_id 字段（API-01 retrofit 第二层会使用，schema 在第一层先加）
ALTER TABLE canonical_resource ADD COLUMN trace_id VARCHAR(128) NULL;
CREATE INDEX idx_canonical_resource_trace ON canonical_resource (trace_id);
```

> 第三层另立 V9 迁移（§6.1），不与 V8 合并，避免跨 PR 共享迁移版本号。

### 5.9 ErrorCode 新增（第一层）

| Code | HTTP | Class | retryable | Message |
|---|---|---|---|---|
| ENG-OBS-001 | 404 | DATA | false | payload 不存在或已归档 |
| ENG-OBS-002 | 500 | INTERNAL | false | 状态历史写入失败 |

### 5.10 第一层验收

- [ ] V8 五方言契约测试 + H2 baseline 通过
- [ ] StateTransitionRecorder 单测覆盖：成功路径 / 失败摘要持久化 / RequestContext 自动注入
- [ ] PayloadStoragePort 默认实现单测：put 返回 PayloadRef / get 还原原字节 / delete 标记软删
- [ ] ErrorCode 全 enum 都有 errorClass + retryable，roundtrip 测试
- [ ] DiagnoseResponseAssembler 单测：装配 snapshot/event 的完整诊断包
- [ ] MdcEnrichmentFilter integration test：MDC 字段写入与清理（含异常路径）
- [ ] TraceIdPropagator 单测：@Async wrap / Outbox 恢复 / 清理
- [ ] 后端完整测试 ≥ 230 用例 / 全绿
- [ ] 前端 lint/typecheck/test/build 全绿

---

## 6. 第二层：GA-ENG-API-01b retrofit（概述）

让 API-01 享受第一层能力。

### 6.1 主要改动

1. **ContextSnapshotService 接入 StateTransitionRecorder**：
   - `create()` 写一条 `state_transition_history`（null → ACTIVE, reason="INITIAL_CREATE"）
   - 未来 snapshot 状态变更（SUPERSEDED/REJECTED）都走 recorder
2. **canonical_resource 持久化 trace_id**：CanonicalResource entity 加 traceId 字段，Service 创建时从 RequestContext 取
3. **新增 `GET /api/v1/engine/context/snapshots/{snapshotId}/diagnose`**：用 DiagnoseResponseAssembler 装配返回
4. **PackageVersionPort 抽象**：
   - 定义 `PackageVersionPort` 接口（exists/getActive）
   - 当前 stub "非空即合法" 改为默认 `LenientPackageVersionAdapter` 实现
   - 后续 API-10 提供真实 `KnowledgePackageVersionAdapter` 替换
5. **失败留痕**：ContextSnapshotService 抛 ApiException 时发一条 audit_event（action=EXECUTE, outcome=FAILED, errorCode=...）

### 6.2 第二层验收

- [ ] ContextSnapshotService.create() 写一条 state_transition_history（INITIAL_CREATE）
- [ ] canonical_resource 持久化时 trace_id 来自 RequestContext，可反查
- [ ] GET /snapshots/{id}/diagnose 返回完整 DiagnoseResponse
- [ ] PackageVersionPort 抽象 + 默认实现单测
- [ ] FAILED 路径发 audit_event（action=EXECUTE, outcome=FAILED）
- [ ] 既有 API-01 测试全绿（零回归）

---

## 7. 第三层：GA-ENG-API-02 临床事件 API（概述）

### 7.1 V9 五方言迁移

`db/migration/{h2,postgres,oracle,dm,kingbase}/V9__clinical_event_payload_and_outbox.sql`：

```sql
-- 1. payload 旁路表（独立于 clinical_event 主表）
CREATE TABLE clinical_event_payload (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id        VARCHAR(64)  NOT NULL,
    tenant_id       VARCHAR(64)  NOT NULL,
    payload         CLOB         NULL,                                    -- PG/Kingbase 改 JSONB
    payload_uri     VARCHAR(256) NULL,                                    -- 预留 OSS 切换：当 storage_type=URI 时填
    storage_type    VARCHAR(16)  NOT NULL DEFAULT 'INLINE',
    content_type    VARCHAR(64)  NOT NULL DEFAULT 'application/json',
    digest          VARCHAR(128) NOT NULL,
    size_bytes      BIGINT       NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP    NULL,
    CONSTRAINT uk_event_payload UNIQUE (event_id),
    CONSTRAINT ck_storage_type  CHECK (storage_type IN ('INLINE','URI'))
);
CREATE INDEX idx_cep_tenant_time ON clinical_event_payload (tenant_id, created_at);

-- 2. clinical_event 主表加列（不放 payload；只加 metadata）
ALTER TABLE clinical_event ADD COLUMN error_code VARCHAR(64)  NULL;
ALTER TABLE clinical_event ADD COLUMN error_class VARCHAR(32) NULL;
ALTER TABLE clinical_event ADD COLUMN retry_count INT NOT NULL DEFAULT 0;
ALTER TABLE clinical_event ADD COLUMN root_event_id VARCHAR(64) NULL;    -- replay 时填原始 event_id

-- 3. Outbox 表（claim 抢任务模式，替代 Kafka）
CREATE TABLE clinical_event_outbox (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id        VARCHAR(64)  NOT NULL,
    tenant_id       VARCHAR(64)  NOT NULL,
    claim_status    VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    claimed_by      VARCHAR(64)  NULL,                                    -- 哪个实例抢到
    claimed_at      TIMESTAMP    NULL,
    next_attempt_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retry_count     INT          NOT NULL DEFAULT 0,
    last_error_code VARCHAR(64)  NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at    TIMESTAMP    NULL,
    CONSTRAINT uk_outbox_event_id UNIQUE (event_id),
    CONSTRAINT ck_outbox_status   CHECK (claim_status IN ('PENDING','CLAIMED','PROCESSED','DEAD'))
);
CREATE INDEX idx_outbox_pending ON clinical_event_outbox (claim_status, next_attempt_at);
CREATE INDEX idx_outbox_tenant  ON clinical_event_outbox (tenant_id, created_at);
```

### 7.2 endpoint

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/v1/engine/events` | event.write | 同步：DB 入 outbox + payload + history 同事务，等 OutboxWorker 处理完返回 PROCESSED/FAILED |
| POST | `/api/v1/engine/events/async` | event.write | 异步：入库即 202 Accepted（含 eventId / traceId） |
| POST | `/api/v1/engine/events/batch` | event.write | 批量异步：一次最多 100 条；返回 batchId + 各 eventId |
| GET | `/api/v1/engine/events/{eventId}` | event.read | 实体查询（不含 payload，仅元数据） |
| GET | `/api/v1/engine/events/{eventId}/payload` | event.read | 拉 payload（PayloadStoragePort.get） |
| GET | `/api/v1/engine/events/{eventId}/diagnose` | event.read | 一键诊断（DiagnoseResponse） |
| POST | `/api/v1/engine/events/{eventId}/replay` | event.write + AUDIT_COMPLIANCE | 重放（新建 event_id，root_event_id 指向源；老事件状态 SUPERSEDED） |
| GET | `/api/v1/engine/events?patientId=...` | event.read | 列表查询 |
| GET | `/api/v1/engine/diagnose/trace/{traceId}` | event.read 或 audit.read | 横向反查：返回该 traceId 涉及的全部实体（snapshot/event/audit） |

### 7.3 OutboxWorker（DB-Only 替代 Kafka 的关键）

**位置**：`com.medkernel.engine.context.OutboxWorker`

```java
@Component
public class OutboxWorker {

    @Scheduled(fixedDelayString = "${medkernel.events.worker.poll-interval-ms:200}")
    public void pollAndClaim() {
        // 1. 单事务抢一批任务（多实例安全）
        List<ClinicalEventOutbox> claimed = claimBatch(50);
        
        // 2. 每条单事务处理（隔离失败）
        for (var outbox : claimed) {
            processOne(outbox);
        }
    }

    @Transactional
    protected List<ClinicalEventOutbox> claimBatch(int limit) {
        // SQL（五方言适配）：
        //   SELECT * FROM clinical_event_outbox
        //   WHERE claim_status='PENDING' AND next_attempt_at <= NOW()
        //   ORDER BY created_at
        //   LIMIT ?
        //   FOR UPDATE SKIP LOCKED
        // → UPDATE 抢到的 rows SET claim_status='CLAIMED', claimed_by=hostname, claimed_at=NOW()
    }

    protected void processOne(ClinicalEventOutbox outbox) {
        TraceIdPropagator.restoreFromTrace(outbox.traceId(), outbox.tenantId(), outbox.actor());
        try {
            clinicalEventProcessor.process(outbox.eventId());
            markProcessed(outbox);
        } catch (ApiException e) {
            handleFailure(outbox, e);
        } finally {
            TraceIdPropagator.clear();
        }
    }
}
```

**SKIP LOCKED 五方言适配**：

| 方言 | 语法 | 备注 |
|---|---|---|
| PostgreSQL | `FOR UPDATE SKIP LOCKED` | 原生 |
| Oracle | `FOR UPDATE SKIP LOCKED` | 11g+ |
| DM 达梦 | `FOR UPDATE SKIP LOCKED` | Oracle 兼容 |
| Kingbase | `FOR UPDATE SKIP LOCKED` | PG 兼容 |
| H2 | `FOR UPDATE` + 应用层 retry | H2 无 SKIP LOCKED；用乐观锁 + version 列兜底（dev/test 场景并发低，足够） |

### 7.4 处理链路

```
HTTP POST /events
  ↓ RequestContextFilter 已分配 traceId
  ↓
ClinicalEventService.receive(req):
  单事务 {
    INSERT clinical_event (id, ..., status=RECEIVED, payload_digest, trace_id)
    INSERT clinical_event_payload (event_id, payload, storage_type='INLINE', digest, size_bytes)
    INSERT clinical_event_outbox (event_id, claim_status='PENDING')
    StateTransitionRecorder.record("clinical_event", id, null, "RECEIVED", "INITIAL_RECEIVE", null)
    AuditEventPublisher.publish(CREATE, "clinical_event", id, "receive")
  } commit
  ↓ sync 模式：阻塞等 OutboxWorker 处理完返回
  ↓ async 模式：202 Accepted (含 eventId, traceId)
  ↓
OutboxWorker @Scheduled(fixedDelay=200ms) [多实例可并跑]
  ↓ tx { SELECT ... FOR UPDATE SKIP LOCKED, UPDATE → CLAIMED }
  ↓ FOR each:
     TraceIdPropagator.restoreFromTrace(...)
     单事务 {
       1. 字典映射检查（TerminologyMappingPort.evaluate）
       2. 业务规则验证
       3. UPDATE clinical_event SET processing_status='MAPPED'
       4. StateTransitionRecorder.record(RECEIVED→MAPPED, "TERMINOLOGY_OK")
       5. UPDATE clinical_event SET processing_status='PROCESSED'
       6. StateTransitionRecorder.record(MAPPED→PROCESSED, "RULES_OK")
       7. UPDATE clinical_event_outbox SET claim_status='PROCESSED', processed_at=NOW()
       8. AuditEventPublisher.publish(EXECUTE, "clinical_event", id, "process success")
       9. publish ClinicalEventProcessedEvent (Spring ApplicationEvent)
     }
     失败 → 单事务 {
       UPDATE clinical_event SET error_code, error_class, retry_count++
       StateTransitionRecorder.record(?→FAILED, error)
       UPDATE outbox SET claim_status='PENDING', retry_count++, 
                       next_attempt_at=NOW()+backoff(retry_count)
       （或 retry_count >= max → claim_status='DEAD'）
     }
  ↓
SnapshotTriggerService @EventListener(ClinicalEventProcessedEvent):
  按 EventType (DISCHARGE/ADMISSION/REPORT) 触发 ContextSnapshotService.create(同 traceId)
  其余 (ORDER/DIAGNOSIS/FOLLOWUP) 跳过
```

### 7.5 重试策略（指数退避）

| retry_count | backoff |
|---|---|
| 1 | 5s |
| 2 | 30s |
| 3 | 5min |
| 4 | 30min |
| 5 | 失败 → DEAD |

### 7.6 PermissionCode 新增

- `EVENT_READ`（"event.read", LOW, "查看临床事件"）
- `EVENT_WRITE`（"event.write", MEDIUM, "创建/重放临床事件"）

### 7.7 ErrorCode 新增

| Code | HTTP | Class | retryable | Message |
|---|---|---|---|---|
| ENG-EVENT-001 | 400 | INPUT | false | 事件 schema 校验失败 |
| ENG-EVENT-002 | 409 | INPUT | false | 事件 ID 已存在（客户端应换 ID 重试） |
| ENG-EVENT-003 | 404 | DATA | false | 事件不存在 |
| ENG-EVENT-004 | 503 | EXTERNAL | true | payload 存储不可用 |
| ENG-EVENT-005 | 500 | INTERNAL | false | 事件处理失败已进入死信 |
| ENG-EVENT-006 | 400 | INPUT | false | 重放仅允许 PROCESSED/FAILED 状态 |

### 7.8 默认权限策略

- `EVENT_READ`：DOCTOR / NURSE / SPECIALIST / DEPT_HEAD / MEDICAL_AFFAIRS / QA_MANAGER / AUDIT_COMPLIANCE / IT_OPS / IMPLEMENTATION_ENGINEER
- `EVENT_WRITE`：IT_OPS / IMPLEMENTATION_ENGINEER（数据接入角色） + HOSPITAL_ADMIN / PLATFORM_ADMIN / GROUP_ADMIN（默认全权）

### 7.9 配置

```yaml
medkernel:
  events:
    worker:
      poll-interval-ms: 200       # OutboxWorker 拉取间隔
      batch-size: 50              # 每次抢任务批大小
      max-retries: 5
      backoff-seconds: [5, 30, 300, 1800]
    payload:
      max-size-bytes: 1048576     # 1MB；超限 400 INPUT
    sync-timeout-ms: 3000         # POST /events 同步模式等 OutboxWorker 处理最长 3s
```

### 7.10 第三层验收

- [ ] V9 五方言迁移合同测试 + H2 baseline 通过（V1..V9 全过）
- [ ] ClinicalEventService 单测：同步创建 / 异步 / 批量 / 同 event_id 幂等 / FAILED 写 history
- [ ] OutboxWorker 单测（@DataJdbcTest H2）：PENDING → CLAIMED → PROCESSED；失败重试；DEAD 终态
- [ ] OutboxWorker 多实例并发测试（postgres testcontainer）：SKIP LOCKED 不重复消费
- [ ] PayloadStoragePort.DbPayloadStorage 单测：put/get 字节级一致 / digest 校验 / 软删
- [ ] SnapshotTriggerService 单测：DISCHARGE 触发 API-01；ORDER 跳过
- [ ] Controller 安全测试：未授权 403 / 缺 tenant 400 / event.write 角色 + tenant 通过
- [ ] DefaultPermissionPolicy 加 EVENT_READ/WRITE 映射测试
- [ ] GET /events/{id}/diagnose 返回完整 DiagnoseResponse（含 payload summary）
- [ ] GET /diagnose/trace/{traceId} 横向反查 ≥ 2 个实体（event + snapshot）
- [ ] POST /events/{id}/replay：新建 event 状态 RECEIVED + root_event_id 指向源；源状态 SUPERSEDED
- [ ] 后端全测 ≥ 280 用例 / 全绿
- [ ] 前端 lint/typecheck/test/build 全绿

---

## 8. 数据模型变更总览

| 表 | 变更 | 迁移版本 | 层 |
|---|---|---|---|
| `state_transition_history` | 新建 | V8 | 第一层 |
| `canonical_resource` | ADD trace_id | V8 | 第一层 |
| `clinical_event_payload` | 新建（旁路） | V9 | 第三层 |
| `clinical_event` | ADD error_code/error_class/retry_count/root_event_id | V9 | 第三层 |
| `clinical_event_outbox` | 新建 | V9 | 第三层 |

---

## 9. 权限码与错误码新增总览

| 项 | 第一层 | 第二层 | 第三层 |
|---|---|---|---|
| PermissionCode | (无) | (无) | EVENT_READ / EVENT_WRITE |
| ErrorCode | 现有 16 个补 errorClass + retryable；新增 ENG-OBS-001..002 | (无) | ENG-EVENT-001..006 |

---

## 10. 非范围

- ❌ 完整 Event Sourcing（仅状态历史 + payload 持久化）
- ❌ ELK/Loki/Grafana/Prometheus 集成（BASE-07 监控运维任务）
- ❌ OpenTelemetry exporter 外发（仅 Spring 内置 Observation + MDC）
- ❌ 事件回调 URL 推送（API-02b 后期）
- ❌ DLT 查询导出 API（API-02b 后期，但 outbox 表本身已留 DEAD 状态可查）
- ❌ Saga 跨服务事务（v1 仅单服务 + DB 原生事务）

---

## 11. 回退策略

每层独立可回退：

- **第一层**：每个组件 `@ConditionalOnProperty(prefix="medkernel.obs", name="enabled")` 控制；异常路径仅 warn 不阻塞主链路；V8 迁移可单独 drop 回 V7
- **第二层**：retrofit 是叠加式，关掉 recorder/diagnose 端点即回原 API-01
- **第三层**：V9 迁移可 drop 回 V8；OutboxWorker `@ConditionalOnProperty` 关闭即停止后台处理

---

## 12. 风险与缓解

| 风险 | 缓解 |
|---|---|
| DB 体积膨胀（payload 旁路表） | 90 天热数据保留，冷归档（按 tenant_id + month 分区）；BASE-07 备份恢复策略覆盖 |
| OutboxWorker 多实例竞争 | `FOR UPDATE SKIP LOCKED` 五方言验证；H2 单 worker 兜底 |
| 高峰期 DB 压力 | OutboxWorker batch=50 + poll=200ms 限速；监控 outbox PENDING 队列长度 |
| H2 无 SKIP LOCKED | 应用层 `@Version` 乐观锁兜底；dev/test 单 worker 场景天然不冲突 |
| 同步模式（sync-timeout）期间 OutboxWorker 处理超时 | 超时返回 202 + eventId，客户端按 GET /events/{id}/diagnose 自查 |
| 国产化方言细节差 | V8/V9 迁移合同测试覆盖五方言；Flyway smoke 在 CI 三方言矩阵跑 |
| traceId 跨异步丢失 | TraceIdPropagator + TaskDecorator 强制注入；OutboxWorker 从 outbox.trace_id 恢复；测试用例覆盖 |
| 大 payload 阻塞 DB | `medkernel.events.payload.max-size-bytes=1MB` 入参限制；超限 400 INPUT |

---

## 13. 演进路径（未来切 OSS / Kafka 的预留）

### 13.1 切 OSS（payload 外置）

- 新增 `OssPayloadStorage` 实现 `PayloadStoragePort`
- 配置切换：`medkernel.payload-storage.impl=oss`
- 新数据 `storage_type='URI'` + `payload_uri='oss://...'`，老数据保 INLINE
- 后台迁移任务（可选）：把 INLINE 渐进搬到 OSS
- `clinical_event_payload` 主表零改动

### 13.2 切 Kafka（量级超 1000 msg/s 时）

- `OutboxWorker.processOne` 改为发 Kafka topic
- Kafka consumer 取出后调 `ClinicalEventProcessor.process`
- 状态历史 / 审计 / payload 旁路全部沿用
- 切换前后 `GET /events/{id}/diagnose` 输出格式不变（traceId/状态历史/audit 都在 DB）

### 13.3 切 OpenTelemetry

- pom 加 `micrometer-tracing-bridge-otel + opentelemetry-exporter-otlp`
- 配置 `management.tracing.endpoint=http://otel-collector:4317`
- 业务代码零改动（Micrometer Observation 已埋）

---

## 14. 实施顺序

1. **第一层 GA-ENG-OBS-01**：spec → plan → 实施 → PR，独立合入 main
2. **第二层 GA-ENG-API-01b**：第一层合入后启动
3. **第三层 GA-ENG-API-02**：第二层合入后启动；可拆 2 PR：
   - PR-A：V9 迁移 + payload 表 + outbox 表 + DTO + ClinicalEventService 单事件入库
   - PR-B：OutboxWorker + Consumer 处理链 + SnapshotTriggerService + Controller 三接口 + diagnose + replay + backlog 闭环

每层 PR 合入前必须：
- 后端全测全绿
- 前端 lint/typecheck/test/build 全绿
- 迁移合同测试通过
- 安全测试通过

---

**End of design v2.0.**
