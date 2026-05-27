# GA-ENG-OBS-01 引擎可观测性骨干 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `com.medkernel.shared.observability` 下交付一套零外部依赖的可观测性骨干（StateTransitionRecorder + PayloadStoragePort + ErrorCode 增强 + DiagnoseResponse + MDC 注入 + TraceIdPropagator + V8 五方言迁移），让后续 API-01 retrofit、API-02 临床事件 API 及 API-03..13 全部共用，避免每个接口重做。

**Architecture:** 全 DB + Spring 内建能力（无 Kafka / MinIO / Zipkin）；状态历史同事务写 `state_transition_history`；PayloadStoragePort 接口先行 + InMemory 默认实现（第三层引入 DbPayloadStorage 切换）；MDC + TraceIdPropagator 串联 HTTP→@Async→@Scheduled 全链路 traceId；ErrorCode 加 errorClass + retryable 维度，TransitionError 持久化结构化错误供诊断接口直接读取。

**Tech Stack:** JDK 21 / Spring Boot 3.3.5 / Spring Data JDBC 3.3 / Flyway 10.20（五方言）/ Bouncy Castle SM3 / Micrometer Observation（内置 actuator）/ JUnit 5 / Mockito / AssertJ。

**Spec：** [docs/superpowers/specs/2026-05-27-engine-observability-and-clinical-events-design.md](../specs/2026-05-27-engine-observability-and-clinical-events-design.md)

---

## Preflight

- [ ] **创建 feature 分支**

```bash
cd /Users/zhikunzheng/个人/郑志坤/medkernel/claude
git checkout main
git pull
git checkout -b feature/ga-eng-obs-01-observability-baseline
```

- [ ] **确认当前在 main 分支基线**

```bash
git log --oneline -3
```

预期：HEAD 在最新 main（含 GA-ENG-API-01 + v2.0 spec commit）。

---

## File Structure

```
medkernel-backend/src/main/
├── java/com/medkernel/shared/
│   ├── api/error/
│   │   ├── ErrorCode.java                              [Modify] 加 ErrorClass 枚举 + errorClass/retryable 字段；新增 ENG-OBS-001/002
│   │   └── ApiException.java                           [Modify] 暴露 errorClass()/retryable() 便利方法
│   └── observability/                                  [New package]
│       ├── StateTransitionHistory.java                 Record entity 映射 state_transition_history 表
│       ├── StateTransitionHistoryRepository.java       Spring Data JDBC ListCrudRepository
│       ├── TransitionError.java                        持久化错误 record
│       ├── StateTransitionRecorder.java                @Component 写历史 + try-catch 兜底
│       ├── PayloadDescriptor.java                      put() 入参 record
│       ├── PayloadRef.java                             put() 返回值 record
│       ├── PayloadStoragePort.java                     存储抽象接口
│       ├── InMemoryPayloadStorage.java                 @Component 默认实现，ConcurrentHashMap
│       ├── DiagnoseResponse.java                       诊断响应 record（嵌套 StateTransitionEntry / AuditEventSummary / PayloadSummary / DiagnoseLinks）
│       ├── DiagnoseResponseAssembler.java              @Component 装配器
│       ├── MdcEnrichmentFilter.java                    OncePerRequestFilter 写 MDC
│       ├── TraceIdPropagator.java                      静态工具：wrap Runnable/Callable + restoreFromTrace + clear
│       └── AsyncTaskExecutorConfig.java                @Configuration 注入 TaskDecorator
└── resources/db/migration/
    ├── h2/V8__observability_baseline.sql               [New] state_transition_history + canonical_resource trace_id
    ├── postgres/V8__observability_baseline.sql         [New]
    ├── oracle/V8__observability_baseline.sql           [New]
    ├── dm/V8__observability_baseline.sql               [New]
    └── kingbase/V8__observability_baseline.sql         [New]

medkernel-backend/src/test/
└── java/com/medkernel/
    ├── migration/
    │   ├── MigrationBaselineContractTest.java          [Modify] 加 V8 用例
    │   └── FlywayMultiDialectSmokeTest.java            [Modify] expected 8 个迁移 + 序列 1..8
    ├── shared/api/error/
    │   └── ErrorCodeTest.java                          [New] errorClass/retryable roundtrip
    └── shared/observability/                           [New package]
        ├── StateTransitionRecorderTest.java
        ├── StateTransitionHistoryRepositoryTest.java   @DataJdbcTest
        ├── InMemoryPayloadStorageTest.java
        ├── DiagnoseResponseAssemblerTest.java
        ├── MdcEnrichmentFilterTest.java                @SpringBootTest + MockMvc
        └── TraceIdPropagatorTest.java
```

10 个任务，每个任务对应一个上述组件或迁移点；每个任务都按 TDD 红→绿→commit 推进。

---

## Task 1：V8 五方言迁移 + 契约测试

**Files：**
- Create：`medkernel-backend/src/main/resources/db/migration/h2/V8__observability_baseline.sql`
- Create：`medkernel-backend/src/main/resources/db/migration/postgres/V8__observability_baseline.sql`
- Create：`medkernel-backend/src/main/resources/db/migration/oracle/V8__observability_baseline.sql`
- Create：`medkernel-backend/src/main/resources/db/migration/dm/V8__observability_baseline.sql`
- Create：`medkernel-backend/src/main/resources/db/migration/kingbase/V8__observability_baseline.sql`
- Modify：`medkernel-backend/src/test/java/com/medkernel/migration/MigrationBaselineContractTest.java`
- Modify：`medkernel-backend/src/test/java/com/medkernel/migration/FlywayMultiDialectSmokeTest.java`

- [ ] **Step 1.1：先扩契约测试（TDD red）**

打开 `MigrationBaselineContractTest.java`，在已有断言末尾追加 V8 期望（参考 V7 写法）：

```java
@Test
void v8ShouldDeclareObservabilityBaseline() {
    String h2 = readMigration("h2", "V8__observability_baseline.sql");
    assertThat(h2).contains("CREATE TABLE IF NOT EXISTS state_transition_history");
    assertThat(h2).contains("ALTER TABLE canonical_resource ADD COLUMN trace_id");
    assertThat(h2).contains("ck_sth_error_class");
    assertThat(h2).contains("idx_sth_entity");
    assertThat(h2).contains("idx_sth_trace");
}

@Test
void v8ShouldExistInAllFiveDialects() {
    for (String dialect : List.of("postgres", "oracle", "dm", "kingbase", "h2")) {
        assertThat(migrationPathFor(dialect, "V8__observability_baseline.sql"))
            .as("dialect %s must ship V8", dialect)
            .exists();
    }
}
```

- [ ] **Step 1.2：跑红**

```bash
cd medkernel-backend && mvn -q -Dtest=MigrationBaselineContractTest test
```

预期：两个新测试失败（迁移文件不存在）。

- [ ] **Step 1.3：写 H2 迁移**

写 `db/migration/h2/V8__observability_baseline.sql`：

```sql
-- MedKernel v1.0 GA · GA-ENG-OBS-01 可观测性骨干迁移
-- 1. 全局状态历史表（所有引擎实体状态机跳转的统一历史）
-- 2. canonical_resource 加 trace_id（第二层 API-01 retrofit 用）

CREATE TABLE IF NOT EXISTS state_transition_history (
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

CREATE INDEX IF NOT EXISTS idx_sth_entity       ON state_transition_history (entity_type, entity_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_sth_tenant_time  ON state_transition_history (tenant_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_sth_trace        ON state_transition_history (trace_id);
CREATE INDEX IF NOT EXISTS idx_sth_failed       ON state_transition_history (tenant_id, error_class, occurred_at);

ALTER TABLE canonical_resource ADD COLUMN IF NOT EXISTS trace_id VARCHAR(128) NULL;
CREATE INDEX IF NOT EXISTS idx_canonical_resource_trace ON canonical_resource (trace_id);
```

- [ ] **Step 1.4：写 postgres 迁移**

写 `db/migration/postgres/V8__observability_baseline.sql`：

```sql
CREATE TABLE IF NOT EXISTS state_transition_history (
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

CREATE INDEX IF NOT EXISTS idx_sth_entity       ON state_transition_history (entity_type, entity_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_sth_tenant_time  ON state_transition_history (tenant_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_sth_trace        ON state_transition_history (trace_id);
CREATE INDEX IF NOT EXISTS idx_sth_failed       ON state_transition_history (tenant_id, error_class, occurred_at);

ALTER TABLE canonical_resource ADD COLUMN IF NOT EXISTS trace_id VARCHAR(128) NULL;
CREATE INDEX IF NOT EXISTS idx_canonical_resource_trace ON canonical_resource (trace_id);
```

- [ ] **Step 1.5：写 oracle 迁移**

写 `db/migration/oracle/V8__observability_baseline.sql`：

```sql
CREATE TABLE state_transition_history (
    id              NUMBER(19)   GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entity_type     VARCHAR2(64) NOT NULL,
    entity_id       VARCHAR2(128) NOT NULL,
    tenant_id       VARCHAR2(64) NOT NULL,
    from_status     VARCHAR2(64) NULL,
    to_status       VARCHAR2(64) NOT NULL,
    reason          VARCHAR2(128) NOT NULL,
    actor           VARCHAR2(64) NULL,
    trace_id        VARCHAR2(128) NULL,
    error_code      VARCHAR2(64) NULL,
    error_class     VARCHAR2(32) NULL,
    error_message   VARCHAR2(512) NULL,
    retry_count     NUMBER(10)   NULL,
    next_retry_at   TIMESTAMP    NULL,
    occurred_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_sth_error_class CHECK (error_class IS NULL OR error_class IN ('INPUT','AUTH','DATA','EXTERNAL','INTERNAL'))
);

CREATE INDEX idx_sth_entity       ON state_transition_history (entity_type, entity_id, occurred_at);
CREATE INDEX idx_sth_tenant_time  ON state_transition_history (tenant_id, occurred_at);
CREATE INDEX idx_sth_trace        ON state_transition_history (trace_id);
CREATE INDEX idx_sth_failed       ON state_transition_history (tenant_id, error_class, occurred_at);

ALTER TABLE canonical_resource ADD trace_id VARCHAR2(128) NULL;
CREATE INDEX idx_canonical_resource_trace ON canonical_resource (trace_id);
```

- [ ] **Step 1.6：写 dm 迁移**

写 `db/migration/dm/V8__observability_baseline.sql`：与 oracle 完全一致（DM 是 Oracle 兼容）。直接复制 oracle 文件内容。

- [ ] **Step 1.7：写 kingbase 迁移**

写 `db/migration/kingbase/V8__observability_baseline.sql`：与 postgres 完全一致（Kingbase 是 PG 兼容）。直接复制 postgres 文件内容。

- [ ] **Step 1.8：跑契约测试（绿）**

```bash
mvn -q -Dtest=MigrationBaselineContractTest test
```

预期：V8 两个用例通过。

- [ ] **Step 1.9：跑 H2 baseline 真实迁移验证 V1→V8**

```bash
mvn -q -Dtest=H2BaselineMigrationTest test
```

预期：H2 V1→V8 全部迁移成功（含新表与 alter 列）。

- [ ] **Step 1.10：修复 FlywayMultiDialectSmokeTest**

打开 `medkernel-backend/src/test/java/com/medkernel/migration/FlywayMultiDialectSmokeTest.java`，找到 `runFlyway` 方法的两处断言：

```java
assertThat(result.migrationsExecuted).as("%s 七个基线迁移执行", vendorName).isEqualTo(7);
// ...
assertThat(applied).extracting(info -> info.getVersion().getVersion())
    .as("%s 完整迁移版本序列", vendorName)
    .containsExactly("1", "2", "3", "4", "5", "6", "7");
```

改为：

```java
assertThat(result.migrationsExecuted).as("%s 八个基线迁移执行", vendorName).isEqualTo(8);
// ...
assertThat(applied).extracting(info -> info.getVersion().getVersion())
    .as("%s 完整迁移版本序列", vendorName)
    .containsExactly("1", "2", "3", "4", "5", "6", "7", "8");
```

- [ ] **Step 1.11：跑 H2 smoke**

```bash
mvn -q -Dtest='FlywayMultiDialectSmokeTest#h2FlywayBaselineMigrates' test
```

预期：H2 用例通过（docker 用例需 CI 环境执行）。

- [ ] **Step 1.12：commit**

```bash
git add medkernel-backend/src/main/resources/db/migration/ medkernel-backend/src/test/java/com/medkernel/migration/
git commit -m "feat(GA-ENG-OBS-01): V8 五方言迁移 state_transition_history 表

跨 postgres/oracle/dm/kingbase/h2 同步建 state_transition_history
表 + canonical_resource ADD trace_id；契约测试覆盖五方言文件存在性
+ 关键 schema 元素（CHECK ck_sth_error_class、4 个索引）；
FlywayMultiDialectSmokeTest 同步更新到 8 个基线迁移。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 2：ErrorCode 增强（errorClass + retryable + ENG-OBS-001/002）

**Files：**
- Modify：`medkernel-backend/src/main/java/com/medkernel/shared/api/error/ErrorCode.java`
- Modify：`medkernel-backend/src/main/java/com/medkernel/shared/api/error/ApiException.java`
- Create：`medkernel-backend/src/test/java/com/medkernel/shared/api/error/ErrorCodeTest.java`

- [ ] **Step 2.1：先写 ErrorCode 单测（red）**

写 `ErrorCodeTest.java`：

```java
package com.medkernel.shared.api.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.medkernel.shared.api.error.ErrorCode.ErrorClass;

class ErrorCodeTest {

    @Test
    void allErrorCodesHaveErrorClassAndRetryableFlag() {
        for (ErrorCode code : ErrorCode.values()) {
            assertThat(code.errorClass())
                .as("ErrorCode %s 必须声明 errorClass", code.name())
                .isNotNull();
        }
    }

    @Test
    void retryableExternalErrorsAreFlagged() {
        assertThat(ErrorCode.DOWNSTREAM_UNAVAILABLE.retryable()).isTrue();
        assertThat(ErrorCode.TOO_MANY_REQUESTS.retryable()).isTrue();
    }

    @Test
    void nonRetryableInputErrors() {
        assertThat(ErrorCode.BAD_REQUEST.retryable()).isFalse();
        assertThat(ErrorCode.VALIDATION_FAILED.retryable()).isFalse();
        assertThat(ErrorCode.NOT_FOUND.retryable()).isFalse();
    }

    @Test
    void errorClassMatchesNaturalGrouping() {
        assertThat(ErrorCode.BAD_REQUEST.errorClass()).isEqualTo(ErrorClass.INPUT);
        assertThat(ErrorCode.UNAUTHORIZED.errorClass()).isEqualTo(ErrorClass.AUTH);
        assertThat(ErrorCode.FORBIDDEN.errorClass()).isEqualTo(ErrorClass.AUTH);
        assertThat(ErrorCode.NOT_FOUND.errorClass()).isEqualTo(ErrorClass.DATA);
        assertThat(ErrorCode.DOWNSTREAM_UNAVAILABLE.errorClass()).isEqualTo(ErrorClass.EXTERNAL);
        assertThat(ErrorCode.INTERNAL_ERROR.errorClass()).isEqualTo(ErrorClass.INTERNAL);
    }

    @Test
    void obsErrorCodesAreRegistered() {
        assertThat(ErrorCode.fromCode("ENG-OBS-001")).hasValueSatisfying(code -> {
            assertThat(code.httpStatus()).isEqualTo(404);
            assertThat(code.errorClass()).isEqualTo(ErrorClass.DATA);
        });
        assertThat(ErrorCode.fromCode("ENG-OBS-002")).hasValueSatisfying(code -> {
            assertThat(code.httpStatus()).isEqualTo(500);
            assertThat(code.errorClass()).isEqualTo(ErrorClass.INTERNAL);
        });
    }
}
```

注：`ErrorCode.fromCode(String)` 是要新增的方法（按当前 PermissionCode.fromCode 风格）。

- [ ] **Step 2.2：跑红**

```bash
mvn -q -Dtest=ErrorCodeTest test
```

预期：编译失败（errorClass / retryable / fromCode 不存在）。

- [ ] **Step 2.3：修改 ErrorCode 加 ErrorClass 枚举与字段**

修改 `ErrorCode.java`，整体替换为：

```java
package com.medkernel.shared.api.error;

import java.util.Arrays;
import java.util.Optional;

/**
 * MedKernel v1.0 GA 统一错误码。
 *
 * <p>命名前缀：
 * <ul>
 *   <li>{@code ENG-API-*}：API 契约（参数、鉴权、HTTP 语义）</li>
 *   <li>{@code ENG-BASE-*}：基础底座（租户、组织、权限上下文）</li>
 *   <li>{@code ENG-SYS-*}：系统级（内部错误、下游故障）</li>
 *   <li>{@code ENG-OBS-*}：可观测性骨干（GA-ENG-OBS-01）</li>
 *   <li>{@code ENG-CONTEXT-*}、{@code ENG-EVENT-*} 等业务域</li>
 * </ul>
 *
 * <p>每个 ErrorCode 含 errorClass（INPUT/AUTH/DATA/EXTERNAL/INTERNAL）+ retryable，
 * 用于客户端决策与状态历史持久化分类。
 */
public enum ErrorCode {

    OK("OK", 200, "操作成功", ErrorClass.INTERNAL, false),

    BAD_REQUEST("ENG-API-001", 400, "请求参数无效", ErrorClass.INPUT, false),
    VALIDATION_FAILED("ENG-API-002", 400, "请求参数校验失败", ErrorClass.INPUT, false),
    UNAUTHORIZED("ENG-API-003", 401, "未授权访问", ErrorClass.AUTH, false),
    FORBIDDEN("ENG-API-004", 403, "无权限执行该操作", ErrorClass.AUTH, false),
    NOT_FOUND("ENG-API-005", 404, "资源不存在", ErrorClass.DATA, false),
    METHOD_NOT_ALLOWED("ENG-API-006", 405, "方法不允许", ErrorClass.INPUT, false),
    CONFLICT("ENG-API-007", 409, "资源冲突", ErrorClass.DATA, false),
    TOO_MANY_REQUESTS("ENG-API-008", 429, "请求过于频繁，请稍后重试", ErrorClass.INPUT, true),
    UNSUPPORTED_MEDIA_TYPE("ENG-API-009", 415, "不支持的请求媒体类型", ErrorClass.INPUT, false),

    TENANT_CONTEXT_MISSING("ENG-BASE-001", 400, "租户上下文缺失", ErrorClass.AUTH, false),
    TENANT_FORBIDDEN("ENG-BASE-002", 403, "无权访问该租户数据", ErrorClass.AUTH, false),
    DATA_SCOPE_DENIED("ENG-BASE-003", 403, "数据范围权限不足", ErrorClass.AUTH, false),

    INTERNAL_ERROR("ENG-SYS-001", 500, "服务内部错误", ErrorClass.INTERNAL, false),
    DOWNSTREAM_UNAVAILABLE("ENG-SYS-002", 503, "下游服务不可用", ErrorClass.EXTERNAL, true),
    MODEL_DEGRADED("ENG-SYS-003", 503, "AI 模型不可用，已降级到无模型基线", ErrorClass.EXTERNAL, true),

    ENG_CONTEXT_001("ENG-CONTEXT-001", 400, "上下文 schema 校验失败", ErrorClass.INPUT, false),
    ENG_CONTEXT_002("ENG-CONTEXT-002", 400, "包版本不存在", ErrorClass.DATA, false),
    ENG_CONTEXT_003("ENG-CONTEXT-003", 400, "标准上下文 quality_status=INVALID 被拒绝", ErrorClass.DATA, false),
    ENG_CONTEXT_004("ENG-CONTEXT-004", 409, "幂等键冲突且 payload 不一致", ErrorClass.DATA, false),

    ENG_OBS_001("ENG-OBS-001", 404, "payload 不存在或已归档", ErrorClass.DATA, false),
    ENG_OBS_002("ENG-OBS-002", 500, "状态历史写入失败", ErrorClass.INTERNAL, false),
    ;

    private final String code;
    private final int httpStatus;
    private final String defaultMessage;
    private final ErrorClass errorClass;
    private final boolean retryable;

    ErrorCode(String code, int httpStatus, String defaultMessage,
              ErrorClass errorClass, boolean retryable) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
        this.errorClass = errorClass;
        this.retryable = retryable;
    }

    public String code() {
        return code;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    public ErrorClass errorClass() {
        return errorClass;
    }

    public boolean retryable() {
        return retryable;
    }

    public static Optional<ErrorCode> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        String normalized = code.trim();
        return Arrays.stream(values())
            .filter(c -> c.code.equalsIgnoreCase(normalized))
            .findFirst();
    }

    /**
     * 错误分类，配合 retryable 决定客户端是否重试。
     */
    public enum ErrorClass {
        /** 输入数据问题：客户端可修复 */
        INPUT,
        /** 权限/认证问题 */
        AUTH,
        /** 业务数据不一致：管理员排查 */
        DATA,
        /** 外部依赖：可重试 */
        EXTERNAL,
        /** 系统内部错误：研发排查 */
        INTERNAL
    }
}
```

- [ ] **Step 2.4：修改 ApiException 暴露便利方法**

打开 `ApiException.java`，在 `public ErrorCode errorCode()` 之后追加：

```java
    public ErrorCode.ErrorClass errorClass() {
        return errorCode.errorClass();
    }

    public boolean retryable() {
        return errorCode.retryable();
    }
```

- [ ] **Step 2.5：跑绿**

```bash
mvn -q -Dtest=ErrorCodeTest test
```

预期：5 用例全过。

- [ ] **Step 2.6：跑现有 GlobalExceptionHandler/ApiException 相关测试不破坏**

```bash
mvn -q -Dtest='GlobalExceptionHandlerTest,ApiExceptionTest' test
```

预期：通过（如果项目未启用 ApiExceptionTest 名字差异，按现状跳过；GlobalExceptionHandler 测试必须通过）。

- [ ] **Step 2.7：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/shared/api/error/ErrorCode.java medkernel-backend/src/main/java/com/medkernel/shared/api/error/ApiException.java medkernel-backend/src/test/java/com/medkernel/shared/api/error/ErrorCodeTest.java
git commit -m "feat(GA-ENG-OBS-01): ErrorCode 加 errorClass + retryable

每个 ErrorCode 新增 ErrorClass（INPUT/AUTH/DATA/EXTERNAL/INTERNAL）
+ retryable 标志；ApiException 暴露 errorClass()/retryable() 便利方法；
新增 ErrorCode.fromCode(String) 反查。新增 ENG-OBS-001 payload
不存在 + ENG-OBS-002 状态历史写入失败。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 3：StateTransitionHistory entity + Repository + 仓储测试

**Files：**
- Create：`medkernel-backend/src/main/java/com/medkernel/shared/observability/StateTransitionHistory.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/shared/observability/StateTransitionHistoryRepository.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/shared/observability/TransitionError.java`
- Test：`medkernel-backend/src/test/java/com/medkernel/shared/observability/StateTransitionHistoryRepositoryTest.java`

- [ ] **Step 3.1：先写仓储测试（red）**

写 `StateTransitionHistoryRepositoryTest.java`：

```java
package com.medkernel.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.test.context.TestPropertySource;

@DataJdbcTest
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:sth-repo-${random.uuid};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration/h2"
})
class StateTransitionHistoryRepositoryTest {

    @Autowired
    StateTransitionHistoryRepository repository;

    @AfterEach
    void wipe() {
        repository.deleteAll();
    }

    @Test
    void savesAndReadsBackSuccessTransition() {
        Instant now = Instant.now();
        StateTransitionHistory saved = repository.save(new StateTransitionHistory(
            null, "context_snapshot", "ctx-1", "tenant-A",
            null, "ACTIVE", "INITIAL_CREATE", "tester", "trace-x",
            null, null, null, null, null, now
        ));
        assertThat(saved.id()).isNotNull();

        List<StateTransitionHistory> found =
            repository.findByEntityTypeAndEntityIdOrderByOccurredAtAsc("context_snapshot", "ctx-1");
        assertThat(found).hasSize(1);
        assertThat(found.get(0).toStatus()).isEqualTo("ACTIVE");
        assertThat(found.get(0).traceId()).isEqualTo("trace-x");
    }

    @Test
    void persistsFailureWithStructuredError() {
        Instant now = Instant.now();
        repository.save(new StateTransitionHistory(
            null, "clinical_event", "evt-1", "tenant-A",
            "MAPPED", "FAILED", "TERMINOLOGY_FAILED", "system", "trace-y",
            "ENG-CONTEXT-001", "INPUT", "code missing", 1, now.plusSeconds(30), now
        ));

        List<StateTransitionHistory> found =
            repository.findByEntityTypeAndEntityIdOrderByOccurredAtAsc("clinical_event", "evt-1");
        assertThat(found).hasSize(1);
        assertThat(found.get(0).errorClass()).isEqualTo("INPUT");
        assertThat(found.get(0).errorCode()).isEqualTo("ENG-CONTEXT-001");
        assertThat(found.get(0).retryCount()).isEqualTo(1);
    }

    @Test
    void queriesByTraceId() {
        Instant now = Instant.now();
        repository.save(newRow("context_snapshot", "ctx-1", "tenant-A", "ACTIVE", "trace-shared", now));
        repository.save(newRow("clinical_event", "evt-1", "tenant-A", "PROCESSED", "trace-shared", now.plusSeconds(1)));
        repository.save(newRow("clinical_event", "evt-2", "tenant-A", "PROCESSED", "trace-other", now));

        List<StateTransitionHistory> found = repository.findByTraceIdOrderByOccurredAtAsc("trace-shared");
        assertThat(found).hasSize(2);
        assertThat(found).extracting(StateTransitionHistory::entityType)
            .containsExactly("context_snapshot", "clinical_event");
    }

    private StateTransitionHistory newRow(String entityType, String entityId, String tenantId,
                                          String toStatus, String traceId, Instant at) {
        return new StateTransitionHistory(
            null, entityType, entityId, tenantId,
            null, toStatus, "TEST", "tester", traceId,
            null, null, null, null, null, at
        );
    }
}
```

- [ ] **Step 3.2：跑红**

```bash
mvn -q -Dtest=StateTransitionHistoryRepositoryTest test
```

预期：编译失败（entity/repository 不存在）。

- [ ] **Step 3.3：写 TransitionError record**

写 `TransitionError.java`：

```java
package com.medkernel.shared.observability;

import java.time.Instant;

import com.medkernel.shared.api.error.ErrorCode.ErrorClass;

/**
 * 状态机跳转失败时的结构化错误。
 *
 * @param errorCode    {@link com.medkernel.shared.api.error.ErrorCode#code()}
 * @param errorClass   {@link ErrorClass#name()}
 * @param message      错误摘要 ≤ 512 字符
 * @param retryCount   当前重试次数
 * @param nextRetryAt  下次重试时间（仅 retryable 错误）
 */
public record TransitionError(
    String errorCode,
    String errorClass,
    String message,
    Integer retryCount,
    Instant nextRetryAt
) {

    public static TransitionError of(String errorCode, ErrorClass errorClass,
                                     String message, Integer retryCount, Instant nextRetryAt) {
        return new TransitionError(errorCode, errorClass.name(), truncate(message), retryCount, nextRetryAt);
    }

    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() <= 512 ? s : s.substring(0, 512);
    }
}
```

- [ ] **Step 3.4：写 StateTransitionHistory entity**

写 `StateTransitionHistory.java`：

```java
package com.medkernel.shared.observability;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 引擎状态机跳转历史。所有 API 共享，按 entityType + entityId 定位某实体的历史轨迹。
 */
@Table("state_transition_history")
public record StateTransitionHistory(
    @Id Long id,
    @Column("entity_type")     String entityType,
    @Column("entity_id")       String entityId,
    @Column("tenant_id")       String tenantId,
    @Column("from_status")     String fromStatus,
    @Column("to_status")       String toStatus,
    @Column("reason")          String reason,
    @Column("actor")           String actor,
    @Column("trace_id")        String traceId,
    @Column("error_code")      String errorCode,
    @Column("error_class")     String errorClass,
    @Column("error_message")   String errorMessage,
    @Column("retry_count")     Integer retryCount,
    @Column("next_retry_at")   Instant nextRetryAt,
    @Column("occurred_at")     Instant occurredAt
) {}
```

- [ ] **Step 3.5：写 Repository**

写 `StateTransitionHistoryRepository.java`：

```java
package com.medkernel.shared.observability;

import java.util.List;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StateTransitionHistoryRepository
    extends ListCrudRepository<StateTransitionHistory, Long> {

    List<StateTransitionHistory> findByEntityTypeAndEntityIdOrderByOccurredAtAsc(
        String entityType, String entityId);

    List<StateTransitionHistory> findByTraceIdOrderByOccurredAtAsc(String traceId);
}
```

- [ ] **Step 3.6：跑绿**

```bash
mvn -q -Dtest=StateTransitionHistoryRepositoryTest test
```

预期：3 用例通过。

- [ ] **Step 3.7：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/shared/observability/StateTransitionHistory.java medkernel-backend/src/main/java/com/medkernel/shared/observability/StateTransitionHistoryRepository.java medkernel-backend/src/main/java/com/medkernel/shared/observability/TransitionError.java medkernel-backend/src/test/java/com/medkernel/shared/observability/StateTransitionHistoryRepositoryTest.java
git commit -m "feat(GA-ENG-OBS-01): state_transition_history entity + repository

Record entity + ListCrudRepository 映射 V8 表；TransitionError
record 封装结构化错误（errorCode/errorClass/message/retryCount/
nextRetryAt）并自动截断 message ≤ 512 字符。仓储测试覆盖：
保存读回、失败错误持久化、按 traceId 跨实体反查。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 4：StateTransitionRecorder + 单测

**Files：**
- Create：`medkernel-backend/src/main/java/com/medkernel/shared/observability/StateTransitionRecorder.java`
- Test：`medkernel-backend/src/test/java/com/medkernel/shared/observability/StateTransitionRecorderTest.java`

- [ ] **Step 4.1：先写单测（red）**

写 `StateTransitionRecorderTest.java`：

```java
package com.medkernel.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;

import com.medkernel.shared.api.error.ErrorCode.ErrorClass;
import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;

class StateTransitionRecorderTest {

    private StateTransitionHistoryRepository repository;
    private StateTransitionRecorder recorder;

    @BeforeEach
    void setUp() {
        repository = mock(StateTransitionHistoryRepository.class);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        recorder = new StateTransitionRecorder(repository);
        RequestContext.restore(new RequestContext.Snapshot(
            "trace-test", OrgScope.tenant("tenant-A"), "tester"));
    }

    @AfterEach
    void clear() {
        RequestContext.clear();
    }

    @Test
    void recordsSuccessTransitionWithRequestContext() {
        recorder.record("context_snapshot", "ctx-1", null, "ACTIVE", "INITIAL_CREATE", null);

        ArgumentCaptor<StateTransitionHistory> captor = ArgumentCaptor.forClass(StateTransitionHistory.class);
        verify(repository).save(captor.capture());
        StateTransitionHistory saved = captor.getValue();
        assertThat(saved.entityType()).isEqualTo("context_snapshot");
        assertThat(saved.entityId()).isEqualTo("ctx-1");
        assertThat(saved.tenantId()).isEqualTo("tenant-A");
        assertThat(saved.toStatus()).isEqualTo("ACTIVE");
        assertThat(saved.reason()).isEqualTo("INITIAL_CREATE");
        assertThat(saved.actor()).isEqualTo("tester");
        assertThat(saved.traceId()).isEqualTo("trace-test");
        assertThat(saved.errorCode()).isNull();
    }

    @Test
    void recordsFailureWithStructuredError() {
        TransitionError error = TransitionError.of("ENG-CONTEXT-001", ErrorClass.INPUT,
            "schema invalid", 1, Instant.now().plusSeconds(30));

        recorder.record("clinical_event", "evt-1", "MAPPED", "FAILED", "TERMINOLOGY_FAILED", error);

        ArgumentCaptor<StateTransitionHistory> captor = ArgumentCaptor.forClass(StateTransitionHistory.class);
        verify(repository).save(captor.capture());
        StateTransitionHistory saved = captor.getValue();
        assertThat(saved.errorCode()).isEqualTo("ENG-CONTEXT-001");
        assertThat(saved.errorClass()).isEqualTo("INPUT");
        assertThat(saved.errorMessage()).isEqualTo("schema invalid");
        assertThat(saved.retryCount()).isEqualTo(1);
    }

    @Test
    void truncatesLongMessage() {
        String longMsg = "a".repeat(1000);
        TransitionError error = TransitionError.of("ENG-SYS-001", ErrorClass.INTERNAL, longMsg, 0, null);

        recorder.record("foo", "id-1", "A", "B", "TEST", error);

        ArgumentCaptor<StateTransitionHistory> captor = ArgumentCaptor.forClass(StateTransitionHistory.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().errorMessage()).hasSize(512);
    }

    @Test
    void recordsWithMissingRequestContextDoesNotThrow() {
        RequestContext.clear();
        // 不应抛异常，actor/tenant/trace 可为 null
        recorder.record("entity", "id", null, "STATE", "REASON", null);

        ArgumentCaptor<StateTransitionHistory> captor = ArgumentCaptor.forClass(StateTransitionHistory.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().traceId()).isNull();
        assertThat(captor.getValue().actor()).isNull();
    }

    @Test
    void runtimeExceptionInRepositoryDoesNotPropagate() {
        // 模拟 NPE / 序列化异常这类非业务异常：吞掉并 warn 日志
        when(repository.save(any())).thenThrow(new RuntimeException("unexpected"));

        // 不应抛 — recorder 内部 try-catch 兜底
        recorder.record("entity", "id", null, "STATE", "REASON", null);
    }

    @Test
    void dataAccessExceptionPropagatesToBusinessTx() {
        // DataAccessException 必须 propagate，让业务事务回滚
        when(repository.save(any())).thenThrow(new DataAccessResourceFailureException("db down"));

        try {
            recorder.record("entity", "id", null, "STATE", "REASON", null);
            // 应该已抛出
            assertThat(false).as("DataAccessException 必须 propagate").isTrue();
        } catch (DataAccessResourceFailureException expected) {
            // OK
        }
    }
}
```

- [ ] **Step 4.2：跑红**

```bash
mvn -q -Dtest=StateTransitionRecorderTest test
```

预期：编译失败（StateTransitionRecorder 不存在）。

- [ ] **Step 4.3：写 StateTransitionRecorder**

写 `StateTransitionRecorder.java`：

```java
package com.medkernel.shared.observability;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;

/**
 * MedKernel v1.0 GA · GA-ENG-OBS-01 统一状态机跳转记录器。
 *
 * <p>所有引擎实体（context_snapshot / clinical_event / rule / pathway / ...）
 * 状态机跳转一律由本组件写历史。traceId / tenantId / actor 从 {@link RequestContext}
 * 自动注入，调用方无需关心。
 *
 * <p>同事务写历史：保证业务事务回滚则历史一并回滚，不会出现"业务无效但历史保留"
 * 导致追溯失真。RuntimeException 兜底为 WARN 日志（避免可观测组件自身 bug 反噬业务），
 * DataAccessException 仍向上抛由业务事务回滚处理。
 */
@Component
public class StateTransitionRecorder {

    private static final Logger log = LoggerFactory.getLogger(StateTransitionRecorder.class);

    private final StateTransitionHistoryRepository repository;

    public StateTransitionRecorder(StateTransitionHistoryRepository repository) {
        this.repository = repository;
    }

    /**
     * 记录一次实体状态机跳转。
     *
     * @param entityType  实体类型常量（如 "context_snapshot"、"clinical_event"）
     * @param entityId    业务 ID
     * @param fromStatus  起始状态；首次入库时填 null
     * @param toStatus    目标状态
     * @param reason      跳转原因（业务语义）
     * @param error       失败时的结构化错误；成功时填 null
     */
    public void record(String entityType, String entityId,
                       String fromStatus, String toStatus,
                       String reason, TransitionError error) {
        try {
            OrgScope scope = RequestContext.currentOrgScope();
            String tenantId = scope != null ? scope.tenantId() : null;
            String actor = RequestContext.currentUserId().orElse(null);
            String traceId = RequestContext.currentTraceId();

            StateTransitionHistory entry = new StateTransitionHistory(
                null, entityType, entityId, tenantId,
                fromStatus, toStatus, reason, actor, traceId,
                error == null ? null : error.errorCode(),
                error == null ? null : error.errorClass(),
                error == null ? null : error.message(),
                error == null ? null : error.retryCount(),
                error == null ? null : error.nextRetryAt(),
                Instant.now()
            );
            repository.save(entry);
        } catch (DataAccessException e) {
            // 数据访问异常向上抛，业务事务回滚
            throw e;
        } catch (RuntimeException e) {
            // 非预期 RuntimeException 仅 warn，不阻塞业务
            log.warn("STATE_TRANSITION_RECORDER_FAILED entityType={} entityId={} reason={} cause={}",
                entityType, entityId, reason, e.toString());
        }
    }
}
```

- [ ] **Step 4.4：跑绿**

```bash
mvn -q -Dtest=StateTransitionRecorderTest test
```

预期：6 用例通过。

- [ ] **Step 4.5：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/shared/observability/StateTransitionRecorder.java medkernel-backend/src/test/java/com/medkernel/shared/observability/StateTransitionRecorderTest.java
git commit -m "feat(GA-ENG-OBS-01): StateTransitionRecorder 同事务写历史

@Component 写 state_transition_history；traceId/tenantId/actor
自动从 RequestContext 注入；DataAccessException 向上抛配合业务
事务回滚，RuntimeException 兜底为 WARN 日志避免可观测组件反噬
业务。单测 6 用例覆盖：成功路径、失败结构化错误、message 自动截断、
RequestContext 缺失不抛、RuntimeException 兜底、DataAccessException
向上传播。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 5：PayloadStoragePort 接口 + DTO

**Files：**
- Create：`medkernel-backend/src/main/java/com/medkernel/shared/observability/PayloadDescriptor.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/shared/observability/PayloadRef.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/shared/observability/PayloadStoragePort.java`

- [ ] **Step 5.1：写 PayloadDescriptor**

```java
package com.medkernel.shared.observability;

/**
 * 持久化 payload 时的元描述。
 *
 * @param tenantId     租户
 * @param entityType   关联实体类型（如 "clinical_event"）
 * @param entityId     关联实体 ID
 * @param contentType  payload MIME（如 "application/json"、"application/cda+xml"）
 */
public record PayloadDescriptor(
    String tenantId,
    String entityType,
    String entityId,
    String contentType
) {}
```

- [ ] **Step 5.2：写 PayloadRef**

```java
package com.medkernel.shared.observability;

/**
 * 持久化 payload 的引用信息。
 *
 * <p>当前默认实现使用 storageType=INLINE；未来切对象存储时新写入 storageType=URI、
 * uri 填外部地址，老数据保持 INLINE，{@link PayloadStoragePort#get(PayloadRef)} 兼容
 * 两种来源。
 *
 * @param storageType  "INLINE" 或 "URI"
 * @param digest       SM3 摘要
 * @param uri          INLINE 时填 "db://table/id"；URI 时填外部存储 URI（mc://、oss://）
 * @param sizeBytes    字节数
 */
public record PayloadRef(
    String storageType,
    String digest,
    String uri,
    long sizeBytes
) {

    public static final String STORAGE_INLINE = "INLINE";
    public static final String STORAGE_URI = "URI";
}
```

- [ ] **Step 5.3：写 PayloadStoragePort 接口**

```java
package com.medkernel.shared.observability;

/**
 * MedKernel v1.0 GA · GA-ENG-OBS-01 payload 存储端口。
 *
 * <p>所有大字段 payload 的持久化与读取一律走本接口，与底层介质解耦。
 *
 * <p>当前默认实现：{@code InMemoryPayloadStorage}（ConcurrentHashMap）；
 * 第三层 API-02 引入 {@code DbPayloadStorage} 写入 clinical_event_payload 旁路表；
 * 未来切 OSS 时新增 {@code OssPayloadStorage} + 配置切换即可，主表零改动。
 */
public interface PayloadStoragePort {

    /**
     * 持久化 payload，返回引用。
     *
     * @param descriptor 元信息
     * @param payload    原始字节
     * @return           {@link PayloadRef}，含 SM3 digest 与定位信息
     */
    PayloadRef put(PayloadDescriptor descriptor, byte[] payload);

    /**
     * 按 PayloadRef 取 payload；不存在或已归档时抛 ApiException(ENG-OBS-001)。
     */
    byte[] get(PayloadRef ref);

    /**
     * 软删除（标记 deleted_at）；GA 阶段不真正物理删除，由归档任务统一处理。
     */
    void delete(PayloadRef ref);
}
```

- [ ] **Step 5.4：编译验证**

```bash
mvn -q compile
```

预期：成功。

- [ ] **Step 5.5：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/shared/observability/PayloadDescriptor.java medkernel-backend/src/main/java/com/medkernel/shared/observability/PayloadRef.java medkernel-backend/src/main/java/com/medkernel/shared/observability/PayloadStoragePort.java
git commit -m "feat(GA-ENG-OBS-01): PayloadStoragePort 接口 + DTO

PayloadDescriptor / PayloadRef record + PayloadStoragePort 接口。
storageType=INLINE/URI 二分，预留未来切对象存储时主表零改动。
默认实现 InMemoryPayloadStorage 与 DbPayloadStorage 在后续 task
落地。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 6：InMemoryPayloadStorage + 单测

**Files：**
- Create：`medkernel-backend/src/main/java/com/medkernel/shared/observability/InMemoryPayloadStorage.java`
- Test：`medkernel-backend/src/test/java/com/medkernel/shared/observability/InMemoryPayloadStorageTest.java`

- [ ] **Step 6.1：先写单测（red）**

写 `InMemoryPayloadStorageTest.java`：

```java
package com.medkernel.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;

class InMemoryPayloadStorageTest {

    private InMemoryPayloadStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryPayloadStorage();
    }

    @Test
    void putReturnsRefWithDigestAndSize() {
        var descriptor = new PayloadDescriptor("tenant-A", "clinical_event", "evt-1", "application/json");
        byte[] payload = "{\"foo\":\"bar\"}".getBytes(StandardCharsets.UTF_8);

        PayloadRef ref = storage.put(descriptor, payload);

        assertThat(ref.storageType()).isEqualTo(PayloadRef.STORAGE_INLINE);
        assertThat(ref.digest()).isNotBlank();
        assertThat(ref.sizeBytes()).isEqualTo(payload.length);
        assertThat(ref.uri()).startsWith("inmem://");
    }

    @Test
    void getReturnsExactBytes() {
        var descriptor = new PayloadDescriptor("tenant-A", "clinical_event", "evt-1", "application/json");
        byte[] payload = "hello 你好".getBytes(StandardCharsets.UTF_8);

        PayloadRef ref = storage.put(descriptor, payload);
        byte[] back = storage.get(ref);

        assertThat(back).isEqualTo(payload);
    }

    @Test
    void getReturnsSameDigestOnIdenticalPayload() {
        var descriptor1 = new PayloadDescriptor("tenant-A", "clinical_event", "evt-1", "application/json");
        var descriptor2 = new PayloadDescriptor("tenant-A", "clinical_event", "evt-2", "application/json");
        byte[] payload = "same content".getBytes(StandardCharsets.UTF_8);

        PayloadRef ref1 = storage.put(descriptor1, payload);
        PayloadRef ref2 = storage.put(descriptor2, payload);

        assertThat(ref1.digest()).isEqualTo(ref2.digest());
        assertThat(ref1.uri()).isNotEqualTo(ref2.uri());  // 但 uri 不同（按 entityId 区分）
    }

    @Test
    void getMissingThrowsApiExceptionWithObs001() {
        PayloadRef bogus = new PayloadRef(PayloadRef.STORAGE_INLINE, "deadbeef", "inmem://nonexistent", 0L);

        assertThatThrownBy(() -> storage.get(bogus))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENG_OBS_001);
    }

    @Test
    void deleteMarksRefAsRemoved() {
        var descriptor = new PayloadDescriptor("tenant-A", "clinical_event", "evt-1", "application/json");
        PayloadRef ref = storage.put(descriptor, "data".getBytes(StandardCharsets.UTF_8));

        storage.delete(ref);

        assertThatThrownBy(() -> storage.get(ref))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENG_OBS_001);
    }
}
```

- [ ] **Step 6.2：跑红**

```bash
mvn -q -Dtest=InMemoryPayloadStorageTest test
```

预期：编译失败（InMemoryPayloadStorage 不存在）。

- [ ] **Step 6.3：写 InMemoryPayloadStorage**

写 `InMemoryPayloadStorage.java`：

```java
package com.medkernel.shared.observability;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;

/**
 * MedKernel v1.0 GA · GA-ENG-OBS-01 payload 存储默认 in-memory 实现。
 *
 * <p>使用 ConcurrentHashMap 持久化在 JVM 内存，仅供 dev/test 与 OBS-01 单独验收。
 * 第三层 API-02 引入 {@code DbPayloadStorage} 后通过 {@code @Primary} 自动覆盖此默认实现。
 *
 * <p>digest 算法：SHA-256（GA 阶段；后续可切 SM3）。
 */
public class InMemoryPayloadStorage implements PayloadStoragePort {

    private final ConcurrentMap<String, byte[]> store = new ConcurrentHashMap<>();

    @Override
    public PayloadRef put(PayloadDescriptor descriptor, byte[] payload) {
        String digest = sha256(payload);
        String uri = String.format("inmem://%s/%s/%s",
            descriptor.tenantId(), descriptor.entityType(), descriptor.entityId());
        store.put(uri, payload.clone());
        return new PayloadRef(PayloadRef.STORAGE_INLINE, digest, uri, payload.length);
    }

    @Override
    public byte[] get(PayloadRef ref) {
        byte[] payload = store.get(ref.uri());
        if (payload == null) {
            throw new ApiException(ErrorCode.ENG_OBS_001,
                "payload 不存在: " + ref.uri());
        }
        return payload.clone();
    }

    @Override
    public void delete(PayloadRef ref) {
        store.remove(ref.uri());
    }

    private static String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
```

- [ ] **Step 6.4：写 PayloadStorageConfig 装配（@ConditionalOnMissingBean）**

写 `medkernel-backend/src/main/java/com/medkernel/shared/observability/PayloadStorageConfig.java`：

```java
package com.medkernel.shared.observability;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * payload 存储装配。
 *
 * <p>默认注入 {@link InMemoryPayloadStorage}（dev/test/OBS-01 单独验收）；
 * 第三层 API-02 引入 {@code DbPayloadStorage} bean 时通过 {@code @ConditionalOnMissingBean}
 * 自动让位（API-02 那边声明 @Primary 即可覆盖）。
 */
@Configuration
public class PayloadStorageConfig {

    @Bean
    @ConditionalOnMissingBean(PayloadStoragePort.class)
    public PayloadStoragePort inMemoryPayloadStorage() {
        return new InMemoryPayloadStorage();
    }
}
```

- [ ] **Step 6.5：跑绿**

```bash
mvn -q -Dtest=InMemoryPayloadStorageTest test
```

预期：5 用例通过。

- [ ] **Step 6.6：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/shared/observability/InMemoryPayloadStorage.java medkernel-backend/src/main/java/com/medkernel/shared/observability/PayloadStorageConfig.java medkernel-backend/src/test/java/com/medkernel/shared/observability/InMemoryPayloadStorageTest.java
git commit -m "feat(GA-ENG-OBS-01): InMemoryPayloadStorage 默认 in-memory 实现

ConcurrentHashMap 持久化 + SHA-256 摘要；put/get/delete 三方法。
@ConditionalOnMissingBean 装配，第三层 DbPayloadStorage 落地时
自动让位。单测 5 用例覆盖：put 返回 ref / 字节级 roundtrip / 同
payload 同 digest / get 不存在抛 ENG-OBS-001 / delete 软删。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 7：DiagnoseResponse + DiagnoseResponseAssembler + 单测

**Files：**
- Create：`medkernel-backend/src/main/java/com/medkernel/shared/observability/DiagnoseResponse.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/shared/observability/DiagnoseResponseAssembler.java`
- Test：`medkernel-backend/src/test/java/com/medkernel/shared/observability/DiagnoseResponseAssemblerTest.java`

- [ ] **Step 7.1：写 DiagnoseResponse + 嵌套 record**

写 `DiagnoseResponse.java`：

```java
package com.medkernel.shared.observability;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * MedKernel v1.0 GA · GA-ENG-OBS-01 一键诊断响应。
 *
 * <p>各引擎实体 diagnose 端点统一返回本结构，自描述含状态历史、审计列表、
 * 关联实体、payload 元信息与跳转链接。
 */
public record DiagnoseResponse(
    String entityType,
    String entityId,
    String tenantId,
    String currentStatus,
    Object entity,
    List<StateTransitionEntry> stateHistory,
    List<AuditEventSummary> auditEvents,
    Map<String, List<String>> relatedEntities,
    PayloadSummary payloadSummary,
    String traceId,
    DiagnoseLinks links
) {

    public record StateTransitionEntry(
        String fromStatus,
        String toStatus,
        String reason,
        String actor,
        String traceId,
        TransitionError error,
        Instant occurredAt
    ) {}

    public record AuditEventSummary(
        String action,
        String resourceType,
        String resourceId,
        String summary,
        String traceId,
        Instant occurredAt
    ) {}

    public record PayloadSummary(
        String digest,
        long sizeBytes,
        String contentType,
        String storageType,
        String fetchUri
    ) {}

    public record DiagnoseLinks(
        String self,
        String fetchPayload,
        String traceTimeline
    ) {}
}
```

- [ ] **Step 7.2：先写单测（red）**

写 `DiagnoseResponseAssemblerTest.java`：

```java
package com.medkernel.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DiagnoseResponseAssemblerTest {

    private StateTransitionHistoryRepository historyRepo;
    private DiagnoseResponseAssembler assembler;

    @BeforeEach
    void setUp() {
        historyRepo = mock(StateTransitionHistoryRepository.class);
        assembler = new DiagnoseResponseAssembler(historyRepo);
    }

    @Test
    void assemblesBasicDiagnose() {
        Instant now = Instant.now();
        when(historyRepo.findByEntityTypeAndEntityIdOrderByOccurredAtAsc("clinical_event", "evt-1"))
            .thenReturn(List.of(
                new StateTransitionHistory(1L, "clinical_event", "evt-1", "tenant-A",
                    null, "RECEIVED", "INITIAL_RECEIVE", "tester", "trace-x",
                    null, null, null, null, null, now),
                new StateTransitionHistory(2L, "clinical_event", "evt-1", "tenant-A",
                    "RECEIVED", "PROCESSED", "RULES_OK", "system", "trace-x",
                    null, null, null, null, null, now.plusSeconds(1))
            ));

        DiagnoseResponse resp = assembler.assemble(
            "clinical_event", "evt-1", "tenant-A", "PROCESSED",
            new SampleEntity("evt-1", "PROCESSED"),
            List.of(),
            Map.of(),
            null,
            "trace-x"
        );

        assertThat(resp.entityType()).isEqualTo("clinical_event");
        assertThat(resp.entityId()).isEqualTo("evt-1");
        assertThat(resp.currentStatus()).isEqualTo("PROCESSED");
        assertThat(resp.stateHistory()).hasSize(2);
        assertThat(resp.stateHistory().get(0).toStatus()).isEqualTo("RECEIVED");
        assertThat(resp.stateHistory().get(1).toStatus()).isEqualTo("PROCESSED");
        assertThat(resp.traceId()).isEqualTo("trace-x");
        assertThat(resp.links().self())
            .isEqualTo("/api/v1/engine/clinical_event/evt-1/diagnose");
        assertThat(resp.links().traceTimeline())
            .isEqualTo("/api/v1/engine/diagnose/trace/trace-x");
    }

    @Test
    void assemblesPayloadSummaryWhenRefProvided() {
        when(historyRepo.findByEntityTypeAndEntityIdOrderByOccurredAtAsc("clinical_event", "evt-1"))
            .thenReturn(List.of());

        PayloadRef ref = new PayloadRef(PayloadRef.STORAGE_INLINE, "abc123",
            "inmem://t/e/evt-1", 1024L);

        DiagnoseResponse resp = assembler.assemble(
            "clinical_event", "evt-1", "tenant-A", "PROCESSED",
            new SampleEntity("evt-1", "PROCESSED"),
            List.of(),
            Map.of(),
            ref,
            "trace-x"
        );

        assertThat(resp.payloadSummary()).isNotNull();
        assertThat(resp.payloadSummary().digest()).isEqualTo("abc123");
        assertThat(resp.payloadSummary().sizeBytes()).isEqualTo(1024L);
        assertThat(resp.payloadSummary().storageType()).isEqualTo("INLINE");
        assertThat(resp.links().fetchPayload())
            .isEqualTo("/api/v1/engine/clinical_event/evt-1/payload");
    }

    @Test
    void translatesErrorIntoStateTransitionEntry() {
        Instant now = Instant.now();
        when(historyRepo.findByEntityTypeAndEntityIdOrderByOccurredAtAsc("clinical_event", "evt-1"))
            .thenReturn(List.of(
                new StateTransitionHistory(1L, "clinical_event", "evt-1", "tenant-A",
                    "MAPPED", "FAILED", "TERMINOLOGY_FAILED", "system", "trace-x",
                    "ENG-CONTEXT-001", "INPUT", "code missing", 2, now.plusSeconds(60), now)
            ));

        DiagnoseResponse resp = assembler.assemble(
            "clinical_event", "evt-1", "tenant-A", "FAILED",
            new SampleEntity("evt-1", "FAILED"),
            List.of(),
            Map.of(),
            null,
            "trace-x"
        );

        var entry = resp.stateHistory().get(0);
        assertThat(entry.error()).isNotNull();
        assertThat(entry.error().errorCode()).isEqualTo("ENG-CONTEXT-001");
        assertThat(entry.error().errorClass()).isEqualTo("INPUT");
        assertThat(entry.error().retryCount()).isEqualTo(2);
    }

    record SampleEntity(String id, String status) {}
}
```

- [ ] **Step 7.3：跑红**

```bash
mvn -q -Dtest=DiagnoseResponseAssemblerTest test
```

预期：编译失败（DiagnoseResponseAssembler 不存在）。

- [ ] **Step 7.4：写 DiagnoseResponseAssembler**

写 `DiagnoseResponseAssembler.java`：

```java
package com.medkernel.shared.observability;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.medkernel.shared.observability.DiagnoseResponse.AuditEventSummary;
import com.medkernel.shared.observability.DiagnoseResponse.DiagnoseLinks;
import com.medkernel.shared.observability.DiagnoseResponse.PayloadSummary;
import com.medkernel.shared.observability.DiagnoseResponse.StateTransitionEntry;

/**
 * MedKernel v1.0 GA · GA-ENG-OBS-01 诊断响应装配器。
 *
 * <p>各引擎实体 diagnose 端点调用本组件装配完整 {@link DiagnoseResponse}，
 * 避免重复装配逻辑。
 */
@Component
public class DiagnoseResponseAssembler {

    private static final String LINK_PATTERN_SELF       = "/api/v1/engine/%s/%s/diagnose";
    private static final String LINK_PATTERN_PAYLOAD    = "/api/v1/engine/%s/%s/payload";
    private static final String LINK_PATTERN_TRACE      = "/api/v1/engine/diagnose/trace/%s";

    private final StateTransitionHistoryRepository historyRepository;

    public DiagnoseResponseAssembler(StateTransitionHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    public DiagnoseResponse assemble(
            String entityType, String entityId, String tenantId, String currentStatus,
            Object entity,
            List<AuditEventSummary> auditEvents,
            Map<String, List<String>> relatedEntities,
            PayloadRef payloadRef,
            String traceId) {

        List<StateTransitionEntry> stateHistory =
            historyRepository.findByEntityTypeAndEntityIdOrderByOccurredAtAsc(entityType, entityId)
                .stream()
                .map(this::toStateEntry)
                .toList();

        PayloadSummary payloadSummary = payloadRef == null ? null : new PayloadSummary(
            payloadRef.digest(), payloadRef.sizeBytes(), null,
            payloadRef.storageType(), payloadRef.uri()
        );

        String selfLink = String.format(LINK_PATTERN_SELF, entityType, entityId);
        String payloadLink = payloadRef == null ? null
            : String.format(LINK_PATTERN_PAYLOAD, entityType, entityId);
        String traceLink = traceId == null ? null
            : String.format(LINK_PATTERN_TRACE, traceId);

        return new DiagnoseResponse(
            entityType, entityId, tenantId, currentStatus,
            entity, stateHistory, auditEvents, relatedEntities,
            payloadSummary, traceId,
            new DiagnoseLinks(selfLink, payloadLink, traceLink)
        );
    }

    private StateTransitionEntry toStateEntry(StateTransitionHistory h) {
        TransitionError error = h.errorCode() == null ? null : new TransitionError(
            h.errorCode(), h.errorClass(), h.errorMessage(),
            h.retryCount(), h.nextRetryAt()
        );
        return new StateTransitionEntry(
            h.fromStatus(), h.toStatus(), h.reason(),
            h.actor(), h.traceId(), error, h.occurredAt()
        );
    }
}
```

- [ ] **Step 7.5：跑绿**

```bash
mvn -q -Dtest=DiagnoseResponseAssemblerTest test
```

预期：3 用例通过。

- [ ] **Step 7.6：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/shared/observability/DiagnoseResponse.java medkernel-backend/src/main/java/com/medkernel/shared/observability/DiagnoseResponseAssembler.java medkernel-backend/src/test/java/com/medkernel/shared/observability/DiagnoseResponseAssemblerTest.java
git commit -m "feat(GA-ENG-OBS-01): DiagnoseResponse + Assembler

DiagnoseResponse record + 4 个嵌套 record（StateTransitionEntry /
AuditEventSummary / PayloadSummary / DiagnoseLinks）。
DiagnoseResponseAssembler 装配器：取 state_transition_history
+ 翻译 TransitionError + 拼自描述 links（self/fetchPayload/
traceTimeline）。单测 3 用例覆盖基础装配、payload summary 装配、
错误结构化翻译。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 8：MdcEnrichmentFilter + 集成测试

**Files：**
- Create：`medkernel-backend/src/main/java/com/medkernel/shared/observability/MdcEnrichmentFilter.java`
- Test：`medkernel-backend/src/test/java/com/medkernel/shared/observability/MdcEnrichmentFilterTest.java`

- [ ] **Step 8.1：先写集成测试（red）**

写 `MdcEnrichmentFilterTest.java`：

```java
package com.medkernel.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class MdcEnrichmentFilterTest {

    @Autowired
    MockMvc mvc;

    @Test
    void clearsMdcAfterRequest() throws Exception {
        // 请求之前 MDC 应为空（或者无 traceId）
        assertThat(MDC.get("traceId")).isNull();

        mvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());

        // 请求结束后 MDC 应被清理
        assertThat(MDC.get("traceId")).isNull();
        assertThat(MDC.get("tenantId")).isNull();
        assertThat(MDC.get("userId")).isNull();
        assertThat(MDC.get("requestPath")).isNull();
    }
}
```

- [ ] **Step 8.2：跑红**

```bash
mvn -q -Dtest=MdcEnrichmentFilterTest test
```

预期：编译失败 OR 测试失败（MdcEnrichmentFilter 不存在 / MDC 没被填充）。

- [ ] **Step 8.3：写 MdcEnrichmentFilter**

```java
package com.medkernel.shared.observability;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * MedKernel v1.0 GA · GA-ENG-OBS-01 MDC 注入 Filter。
 *
 * <p>在请求进入时把 traceId / tenantId / userId / requestPath 写入 MDC，
 * 请求结束（含异常路径）清理。配合 logback JSON pattern 让每条日志带这些字段。
 *
 * <p>Order 设在 TraceIdFilter 之后（确保 RequestContext.traceId 已经填充）。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class MdcEnrichmentFilter extends OncePerRequestFilter {

    public static final String MDC_TRACE_ID    = "traceId";
    public static final String MDC_TENANT_ID   = "tenantId";
    public static final String MDC_USER_ID     = "userId";
    public static final String MDC_REQUEST_PATH = "requestPath";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String traceId = RequestContext.currentTraceId();
            if (traceId != null) {
                MDC.put(MDC_TRACE_ID, traceId);
            }
            OrgScope scope = RequestContext.currentOrgScope();
            if (scope != null && scope.hasTenant()) {
                MDC.put(MDC_TENANT_ID, scope.tenantId());
            }
            RequestContext.currentUserId().ifPresent(uid -> MDC.put(MDC_USER_ID, uid));
            MDC.put(MDC_REQUEST_PATH, request.getRequestURI());

            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID);
            MDC.remove(MDC_TENANT_ID);
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_REQUEST_PATH);
        }
    }
}
```

- [ ] **Step 8.4：跑绿**

```bash
mvn -q -Dtest=MdcEnrichmentFilterTest test
```

预期：通过。

- [ ] **Step 8.5：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/shared/observability/MdcEnrichmentFilter.java medkernel-backend/src/test/java/com/medkernel/shared/observability/MdcEnrichmentFilterTest.java
git commit -m "feat(GA-ENG-OBS-01): MdcEnrichmentFilter 写入 + 清理 MDC

OncePerRequestFilter @Component，写入 traceId/tenantId/userId/
requestPath；finally 清理避免线程池泄漏。Order 设在 TraceIdFilter
之后保证 RequestContext.traceId 已填充。集成测试通过 MockMvc 验证
请求后 MDC 全清。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 9：TraceIdPropagator + AsyncTaskExecutorConfig + 单测

**Files：**
- Create：`medkernel-backend/src/main/java/com/medkernel/shared/observability/TraceIdPropagator.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/shared/observability/AsyncTaskExecutorConfig.java`
- Test：`medkernel-backend/src/test/java/com/medkernel/shared/observability/TraceIdPropagatorTest.java`

- [ ] **Step 9.1：先写单测（red）**

写 `TraceIdPropagatorTest.java`：

```java
package com.medkernel.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;

class TraceIdPropagatorTest {

    @AfterEach
    void clear() {
        RequestContext.clear();
        MDC.clear();
    }

    @Test
    void wrapPropagatesRequestContextAndMdcToAsyncThread() throws Exception {
        RequestContext.restore(new RequestContext.Snapshot(
            "trace-async", OrgScope.tenant("tenant-A"), "tester"));
        MDC.put(MdcEnrichmentFilter.MDC_TRACE_ID, "trace-async");
        MDC.put(MdcEnrichmentFilter.MDC_TENANT_ID, "tenant-A");

        AtomicReference<String> capturedTrace = new AtomicReference<>();
        AtomicReference<String> capturedMdc = new AtomicReference<>();
        AtomicReference<String> capturedTenant = new AtomicReference<>();

        Runnable task = TraceIdPropagator.wrap(() -> {
            capturedTrace.set(RequestContext.currentTraceId());
            capturedMdc.set(MDC.get(MdcEnrichmentFilter.MDC_TRACE_ID));
            OrgScope scope = RequestContext.currentOrgScope();
            capturedTenant.set(scope == null ? null : scope.tenantId());
        });

        CompletableFuture.runAsync(task).get();

        assertThat(capturedTrace.get()).isEqualTo("trace-async");
        assertThat(capturedMdc.get()).isEqualTo("trace-async");
        assertThat(capturedTenant.get()).isEqualTo("tenant-A");
    }

    @Test
    void wrapClearsContextAfterTaskExecution() throws Exception {
        RequestContext.restore(new RequestContext.Snapshot(
            "trace-x", OrgScope.tenant("tenant-A"), "tester"));

        AtomicReference<String> insideTask = new AtomicReference<>();

        Runnable task = TraceIdPropagator.wrap(() -> {
            insideTask.set(RequestContext.currentTraceId());
        });

        // 在另一个线程执行；该线程开始时无 context；wrap 应注入；结束时清理
        Thread t = new Thread(() -> {
            assertThat(RequestContext.currentTraceId()).isNull();  // 任务前
            task.run();
            assertThat(RequestContext.currentTraceId()).isNull();  // 任务后清理
            assertThat(MDC.get(MdcEnrichmentFilter.MDC_TRACE_ID)).isNull();
        });
        t.start();
        t.join();

        assertThat(insideTask.get()).isEqualTo("trace-x");
    }

    @Test
    void restoreFromTraceSetsRequestContextAndMdc() {
        TraceIdPropagator.restoreFromTrace("trace-restored", "tenant-B", "system");

        assertThat(RequestContext.currentTraceId()).isEqualTo("trace-restored");
        assertThat(RequestContext.currentOrgScope().tenantId()).isEqualTo("tenant-B");
        assertThat(MDC.get(MdcEnrichmentFilter.MDC_TRACE_ID)).isEqualTo("trace-restored");
        assertThat(MDC.get(MdcEnrichmentFilter.MDC_TENANT_ID)).isEqualTo("tenant-B");

        TraceIdPropagator.clear();
        assertThat(RequestContext.currentTraceId()).isNull();
        assertThat(MDC.get(MdcEnrichmentFilter.MDC_TRACE_ID)).isNull();
    }
}
```

- [ ] **Step 9.2：跑红**

```bash
mvn -q -Dtest=TraceIdPropagatorTest test
```

预期：编译失败（TraceIdPropagator 不存在）。

- [ ] **Step 9.3：写 TraceIdPropagator**

```java
package com.medkernel.shared.observability;

import java.util.Map;
import java.util.concurrent.Callable;

import org.slf4j.MDC;

import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;

/**
 * MedKernel v1.0 GA · GA-ENG-OBS-01 跨异步线程的 traceId / RequestContext / MDC 传播工具。
 *
 * <p>Spring @Async / @Scheduled / CompletableFuture 等场景下，调用 {@link #wrap(Runnable)}
 * 包装任务即可让 traceId 跨线程串联。
 *
 * <p>后台 worker（如 OutboxWorker）从 DB 读出 traceId 时用 {@link #restoreFromTrace} 恢复。
 */
public final class TraceIdPropagator {

    private TraceIdPropagator() {
    }

    /**
     * 包装 Runnable，自动复制当前线程的 RequestContext + MDC 到任务执行线程，
     * 任务结束清理执行线程的状态。
     */
    public static Runnable wrap(Runnable task) {
        RequestContext.Snapshot snapshot = RequestContext.snapshot();
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        return () -> {
            RequestContext.Snapshot prev = RequestContext.snapshot();
            Map<String, String> prevMdc = MDC.getCopyOfContextMap();
            try {
                RequestContext.restore(snapshot);
                setMdc(mdc);
                task.run();
            } finally {
                RequestContext.restore(prev);
                setMdc(prevMdc);
            }
        };
    }

    /** 同上，泛型 Callable */
    public static <T> Callable<T> wrap(Callable<T> task) {
        RequestContext.Snapshot snapshot = RequestContext.snapshot();
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        return () -> {
            RequestContext.Snapshot prev = RequestContext.snapshot();
            Map<String, String> prevMdc = MDC.getCopyOfContextMap();
            try {
                RequestContext.restore(snapshot);
                setMdc(mdc);
                return task.call();
            } finally {
                RequestContext.restore(prev);
                setMdc(prevMdc);
            }
        };
    }

    /**
     * 后台 worker（OutboxWorker / @Scheduled）从 DB 恢复 traceId 时调用：
     * 在当前线程注入 RequestContext + MDC。完成后必须调 {@link #clear()}。
     */
    public static void restoreFromTrace(String traceId, String tenantId, String userId) {
        RequestContext.restore(new RequestContext.Snapshot(
            traceId, OrgScope.tenant(tenantId), userId));
        if (traceId != null) {
            MDC.put(MdcEnrichmentFilter.MDC_TRACE_ID, traceId);
        }
        if (tenantId != null) {
            MDC.put(MdcEnrichmentFilter.MDC_TENANT_ID, tenantId);
        }
        if (userId != null) {
            MDC.put(MdcEnrichmentFilter.MDC_USER_ID, userId);
        }
    }

    /** 清理当前线程 RequestContext + MDC */
    public static void clear() {
        RequestContext.clear();
        MDC.remove(MdcEnrichmentFilter.MDC_TRACE_ID);
        MDC.remove(MdcEnrichmentFilter.MDC_TENANT_ID);
        MDC.remove(MdcEnrichmentFilter.MDC_USER_ID);
        MDC.remove(MdcEnrichmentFilter.MDC_REQUEST_PATH);
    }

    private static void setMdc(Map<String, String> mdc) {
        MDC.clear();
        if (mdc != null) {
            mdc.forEach(MDC::put);
        }
    }
}
```

- [ ] **Step 9.4：写 AsyncTaskExecutorConfig**

```java
package com.medkernel.shared.observability;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * MedKernel v1.0 GA · GA-ENG-OBS-01 异步任务执行器配置。
 *
 * <p>启用 @EnableAsync；注入 {@link TraceIdPropagator#wrap(Runnable)} 作为
 * TaskDecorator，确保所有 @Async / @Scheduled 自动传递 RequestContext + MDC。
 */
@Configuration
@EnableAsync
public class AsyncTaskExecutorConfig {

    @Bean
    public TaskDecorator traceTaskDecorator() {
        return TraceIdPropagator::wrap;
    }

    @Bean(name = "applicationTaskExecutor")
    public ThreadPoolTaskExecutor applicationTaskExecutor(TaskDecorator traceTaskDecorator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("medkernel-async-");
        executor.setTaskDecorator(traceTaskDecorator);
        executor.initialize();
        return executor;
    }
}
```

- [ ] **Step 9.5：跑绿**

```bash
mvn -q -Dtest=TraceIdPropagatorTest test
```

预期：3 用例通过。

- [ ] **Step 9.6：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/shared/observability/TraceIdPropagator.java medkernel-backend/src/main/java/com/medkernel/shared/observability/AsyncTaskExecutorConfig.java medkernel-backend/src/test/java/com/medkernel/shared/observability/TraceIdPropagatorTest.java
git commit -m "feat(GA-ENG-OBS-01): TraceIdPropagator + AsyncTaskExecutorConfig

TraceIdPropagator 静态工具类：wrap(Runnable/Callable) 跨线程复制
RequestContext + MDC + 任务结束清理执行线程；restoreFromTrace
后台 worker 从 DB 恢复；clear() 清理。
AsyncTaskExecutorConfig 注入 TaskDecorator 让所有 @Async/@Scheduled
自动传递上下文；applicationTaskExecutor bean 含 traceTaskDecorator。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 10：全门禁验证 + backlog 闭环

**Files：**
- Modify：`docs/backlog.md`

- [ ] **Step 10.1：跑后端完整测试**

```bash
cd medkernel-backend && mvn clean test 2>&1 | grep -E "Tests run:|BUILD" | tail -5
```

预期：`Tests run: ≥ 240, Failures: 0, Errors: 0, Skipped: 3, BUILD SUCCESS`。

- [ ] **Step 10.2：跑迁移契约 + H2 baseline**

```bash
mvn -q -Dtest='MigrationBaselineContractTest,H2BaselineMigrationTest,FlywayMultiDialectSmokeTest#h2FlywayBaselineMigrates' test
```

预期：通过。

- [ ] **Step 10.3：跑前端全门禁**

```bash
cd ../frontend && npm run lint && npm run typecheck && npm test -- --run && npm run build
```

预期：四步全过（lint 0 errors / typecheck OK / 79+ tests / build OK）。

- [ ] **Step 10.4：更新 backlog.md**

打开 `docs/backlog.md`。

(a) 在 E1 表末尾追加一行（如果 E1 没有 OBS 行就添加；如果在 E2 表内则不动）：

实际上 GA-ENG-OBS-01 是 E2 准备阶段的横切能力，按 spec §3 表归类。让我们放在 E2 表头部前面增加一行：

在 `## E2 · 引擎接口上线` 标题下面表头之后、第一行 `GA-ENG-API-01` 之前插入：

```markdown
| GA-ENG-OBS-01 引擎可观测性骨干：StateTransitionRecorder / PayloadStoragePort / ErrorCode 增强 / DiagnoseResponse / MDC / TraceIdPropagator / V8 五方言迁移 | claude | done |
```

(b) 修订记录表顶部追加（紧贴 4.14 之前）：

```markdown
| 4.15 | 2026-05-27 | Claude | GA-ENG-OBS-01 完成：V8 五方言迁移（state_transition_history 表 + canonical_resource ADD trace_id）+ ErrorCode 加 ErrorClass(INPUT/AUTH/DATA/EXTERNAL/INTERNAL) + retryable + ENG-OBS-001/002 + StateTransitionRecorder（同事务写历史、RuntimeException 兜底、DataAccessException 向上抛）+ PayloadStoragePort 接口 + InMemoryPayloadStorage 默认实现（@ConditionalOnMissingBean，第三层 DbPayloadStorage 自动让位）+ DiagnoseResponse + Assembler + MdcEnrichmentFilter + TraceIdPropagator + AsyncTaskExecutorConfig。后端 240+ 测试全绿 / 前端四步门禁全过 |
```

- [ ] **Step 10.5：commit backlog**

```bash
cd /Users/zhikunzheng/个人/郑志坤/medkernel/claude
git add docs/backlog.md
git commit -m "chore(GA-ENG-OBS-01): backlog 4.15 闭环

引擎可观测性骨干完成：StateTransitionRecorder / PayloadStoragePort /
ErrorCode 增强 / DiagnoseResponse / MDC / TraceIdPropagator /
V8 五方言迁移。零新外部组件，后续 API-01 retrofit (API-01b) 与
API-02 临床事件 API 可直接复用。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

- [ ] **Step 10.6：（可选）开 PR**

如果项目惯例是 PR 模式：

```bash
git push -u origin feature/ga-eng-obs-01-observability-baseline
gh pr create --title "feat(GA-ENG-OBS-01): 引擎可观测性骨干" --body "$(cat <<'EOF'
## Summary
- E2 引擎接口阶段横切基线：GA-ENG-OBS-01 引擎可观测性骨干
- 零新外部组件（无 Kafka / MinIO / Zipkin），全 DB + Spring 内建
- V8 五方言迁移：state_transition_history 表 + canonical_resource ADD trace_id
- ErrorCode 加 ErrorClass + retryable + 新增 ENG-OBS-001/002
- StateTransitionRecorder + PayloadStoragePort + DiagnoseResponse + Assembler + MdcEnrichmentFilter + TraceIdPropagator

## Test plan
- [x] MigrationBaselineContractTest V8 五方言契约
- [x] H2BaselineMigrationTest V1..V8 全过
- [x] FlywayMultiDialectSmokeTest h2 用例
- [x] ErrorCodeTest errorClass/retryable roundtrip
- [x] StateTransitionHistoryRepositoryTest 仓储集成
- [x] StateTransitionRecorderTest 6 用例（含 RuntimeException 兜底 + DataAccessException 向上抛）
- [x] InMemoryPayloadStorageTest 5 用例（含 ENG-OBS-001 不存在场景）
- [x] DiagnoseResponseAssemblerTest 3 用例（基础装配 + payload summary + 错误翻译）
- [x] MdcEnrichmentFilterTest MDC 写入与清理
- [x] TraceIdPropagatorTest 3 用例（wrap 跨线程 + 清理 + restoreFromTrace）
- [x] 前端 lint/typecheck/test/build

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

否则保留在本地 main 等用户决定。

---

## 实施完成

执行完 Task 1..10 后：

- ✅ V8 五方言迁移已合入主线，新增 state_transition_history 表 + canonical_resource trace_id 列
- ✅ ErrorCode 16 项现有码全部补 errorClass + retryable，新增 ENG-OBS-001/002
- ✅ StateTransitionRecorder 同事务写历史，traceId/tenantId/actor 自动注入
- ✅ PayloadStoragePort 接口 + InMemory 默认实现，预留切 OSS 路径
- ✅ DiagnoseResponse 模板 + Assembler 装配器，后续 API 用同一模板提供 diagnose 端点
- ✅ MdcEnrichmentFilter + TraceIdPropagator 串联 HTTP→@Async/@Scheduled
- ✅ 后端 240+ 测试全绿
- ✅ backlog.md 4.15 闭环

下一步可启动 **GA-ENG-API-01b 标准上下文 retrofit**（第二层）：ContextSnapshotService 接入 StateTransitionRecorder + canonical_resource 持久化 trace_id + GET /snapshots/{id}/diagnose + PackageVersionPort 抽象。

---

**End of GA-ENG-OBS-01 implementation plan.**
