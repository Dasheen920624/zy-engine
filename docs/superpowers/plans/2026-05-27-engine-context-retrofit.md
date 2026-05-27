# GA-ENG-API-01b 标准上下文 retrofit 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 API-01 标准上下文 API 享受 OBS-01 可观测性骨干的所有能力：每次 snapshot 创建写一条 state_transition_history、canonical_resource 持久化 trace_id、提供一键 diagnose 端点、抽象 PackageVersionPort、失败路径发结构化 audit_event。

**Architecture:** 零外部组件叠加层；V9 五方言迁移仅给 audit_event 加 outcome + error_code 两列；新增 IsolatedAuditPublisher 用 PROPAGATION_REQUIRES_NEW 子事务保证失败 audit 不被业务事务回滚带走；PackageVersionPort 接口默认 LenientAdapter（@ConditionalOnMissingBean），API-10 引入真实 KnowledgePackageVersionAdapter 时自动让位；diagnose 端点复用 OBS-01 的 DiagnoseResponseAssembler 零额外装配代码。

**Tech Stack:** JDK 21 / Spring Boot 3.3.5 / Spring Data JDBC 3.3 / Flyway 10.20（五方言）/ Spring TransactionTemplate / JUnit 5 / Mockito / AssertJ。

**Spec：** [docs/superpowers/specs/2026-05-27-engine-observability-and-clinical-events-design.md §6](../specs/2026-05-27-engine-observability-and-clinical-events-design.md)

---

## Preflight

- [ ] **确认在 feature 分支基线**

```bash
cd /Users/zhikunzheng/个人/郑志坤/medkernel/claude
git branch --show-current   # 预期 feature/ga-eng-api-01b-context-retrofit
git log --oneline -3
```

预期：HEAD 在最新 main（含 OBS-01 PR #89 / 8016e5d）。

- [ ] **确认依赖组件就位**

```bash
ls medkernel-backend/src/main/java/com/medkernel/shared/observability/StateTransitionRecorder.java
ls medkernel-backend/src/main/java/com/medkernel/shared/observability/DiagnoseResponseAssembler.java
ls medkernel-backend/src/main/resources/db/migration/h2/V8__observability_baseline.sql
```

预期：三个文件均存在（OBS-01 已合入 main）。

---

## File Structure

```
medkernel-backend/src/main/
├── java/com/medkernel/
│   ├── shared/audit/
│   │   ├── AuditEvent.java                          [Modify] +outcome/errorCode 字段 + failure() 工厂
│   │   └── IsolatedAuditPublisher.java              [New] PROPAGATION_REQUIRES_NEW 子事务发失败 audit
│   ├── shared/audit/persistence/
│   │   ├── AuditEventRecord.java                    [Modify] +outcome/errorCode 字段
│   │   └── AuditChainWriter.java                    [Modify] 写新两列
│   └── engine/context/
│       ├── CanonicalResource.java                   [Modify] +traceId 字段
│       ├── PackageVersionPort.java                  [New] exists(tenantId, packageType, version)/getActive(...)
│       ├── LenientPackageVersionAdapter.java        [New] @ConditionalOnMissingBean 默认实现
│       ├── PackageVersionResolver.java              [Remove] 由 PackageVersionPort 接口 + LenientAdapter 替代
│       ├── ContextSnapshotService.java              [Modify] 注入 recorder + IsolatedAuditPublisher + 接 traceId
│       └── ContextSnapshotController.java           [Modify] +GET /{snapshotId}/diagnose
└── resources/db/migration/
    ├── h2/V9__audit_event_outcome.sql               [New] audit_event +outcome +error_code
    ├── postgres/V9__audit_event_outcome.sql         [New]
    ├── oracle/V9__audit_event_outcome.sql           [New]
    ├── dm/V9__audit_event_outcome.sql               [New]
    └── kingbase/V9__audit_event_outcome.sql         [New]

medkernel-backend/src/test/
└── java/com/medkernel/
    ├── migration/
    │   ├── MigrationBaselineContractTest.java       [Modify] +V9 用例
    │   └── FlywayMultiDialectSmokeTest.java         [Modify] expected 9 个迁移
    ├── shared/audit/
    │   ├── AuditEventTest.java                      [New] failure() 工厂 + outcome=FAILED 默认 errorCode
    │   └── IsolatedAuditPublisherTest.java          [New] @Transactional 内调用 → 主回滚但 audit 进库
    ├── shared/audit/persistence/
    │   └── AuditChainWriterTest.java                [Modify] 加 outcome/errorCode 持久化用例
    └── engine/context/
        ├── PackageVersionPortTest.java              [New] LenientAdapter 单测
        ├── CanonicalResourceRepositoryTest.java     [New if missing / Modify] traceId roundtrip
        ├── ContextSnapshotServiceTest.java          [Modify] +recorder/audit/failure 路径用例
        ├── ContextSnapshotDiagnoseTest.java         [New] @SpringBootTest GET /diagnose 集成
        └── ContextSnapshotControllerSecurityTest.java [Modify] diagnose 端点权限矩阵
```

10 个 task；每个 task TDD 红→绿→commit。

---

## Task 1：V9 五方言迁移 audit_event 加 outcome + error_code

**Files：**
- Create：`medkernel-backend/src/main/resources/db/migration/h2/V9__audit_event_outcome.sql`
- Create：`medkernel-backend/src/main/resources/db/migration/postgres/V9__audit_event_outcome.sql`
- Create：`medkernel-backend/src/main/resources/db/migration/oracle/V9__audit_event_outcome.sql`
- Create：`medkernel-backend/src/main/resources/db/migration/dm/V9__audit_event_outcome.sql`
- Create：`medkernel-backend/src/main/resources/db/migration/kingbase/V9__audit_event_outcome.sql`
- Modify：`medkernel-backend/src/test/java/com/medkernel/migration/MigrationBaselineContractTest.java`
- Modify：`medkernel-backend/src/test/java/com/medkernel/migration/FlywayMultiDialectSmokeTest.java`

- [ ] **Step 1.1：先扩契约测试（TDD red）**

打开 `MigrationBaselineContractTest.java`，在已有断言末尾追加：

```java
@Test
void v9ShouldExtendAuditEventWithOutcome() {
    String h2 = readMigration("h2", "V9__audit_event_outcome.sql");
    assertThat(h2).contains("ALTER TABLE audit_event ADD COLUMN");
    assertThat(h2).contains("outcome");
    assertThat(h2).contains("error_code");
    assertThat(h2).contains("ck_audit_event_outcome");
    assertThat(h2).contains("idx_audit_event_outcome");
}

@Test
void v9ShouldExistInAllFiveDialects() {
    for (String dialect : List.of("postgres", "oracle", "dm", "kingbase", "h2")) {
        assertThat(migrationPathFor(dialect, "V9__audit_event_outcome.sql"))
            .as("dialect %s must ship V9", dialect)
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

写 `db/migration/h2/V9__audit_event_outcome.sql`：

```sql
-- MedKernel v1.0 GA · GA-ENG-API-01b retrofit V9
-- audit_event 加 outcome / error_code，让业务失败结构化留痕（spec §6.1 第 5 条）。
-- 注：audit_event.status 是审计链状态（RECORDED/SIGNED/TSA_SIGNED/REJECTED），
-- 与业务 outcome（SUCCESS/FAILED）语义不同，因此独立加列而非复用。

ALTER TABLE audit_event ADD COLUMN IF NOT EXISTS outcome VARCHAR(16) NOT NULL DEFAULT 'SUCCESS';
ALTER TABLE audit_event ADD COLUMN IF NOT EXISTS error_code VARCHAR(64) NULL;

ALTER TABLE audit_event ADD CONSTRAINT ck_audit_event_outcome CHECK (outcome IN ('SUCCESS','FAILED'));

CREATE INDEX IF NOT EXISTS idx_audit_event_outcome ON audit_event (tenant_id, outcome, occurred_at);
```

- [ ] **Step 1.4：写 postgres 迁移**

写 `db/migration/postgres/V9__audit_event_outcome.sql`：

```sql
ALTER TABLE audit_event ADD COLUMN IF NOT EXISTS outcome VARCHAR(16) NOT NULL DEFAULT 'SUCCESS';
ALTER TABLE audit_event ADD COLUMN IF NOT EXISTS error_code VARCHAR(64) NULL;

ALTER TABLE audit_event ADD CONSTRAINT ck_audit_event_outcome CHECK (outcome IN ('SUCCESS','FAILED'));

CREATE INDEX IF NOT EXISTS idx_audit_event_outcome ON audit_event (tenant_id, outcome, occurred_at);
```

- [ ] **Step 1.5：写 oracle 迁移**

写 `db/migration/oracle/V9__audit_event_outcome.sql`（Oracle 无 ADD COLUMN IF NOT EXISTS，靠 V9 一次性应用且不重复）：

```sql
ALTER TABLE audit_event ADD outcome VARCHAR2(16) DEFAULT 'SUCCESS' NOT NULL;
ALTER TABLE audit_event ADD error_code VARCHAR2(64) NULL;

ALTER TABLE audit_event ADD CONSTRAINT ck_audit_event_outcome CHECK (outcome IN ('SUCCESS','FAILED'));

CREATE INDEX idx_audit_event_outcome ON audit_event (tenant_id, outcome, occurred_at);
```

- [ ] **Step 1.6：写 dm 迁移**

写 `db/migration/dm/V9__audit_event_outcome.sql`：与 oracle 一致（DM Oracle 兼容）。直接复制 oracle 内容。

- [ ] **Step 1.7：写 kingbase 迁移**

写 `db/migration/kingbase/V9__audit_event_outcome.sql`：与 postgres 一致（Kingbase PG 兼容）。直接复制 postgres 内容。

- [ ] **Step 1.8：跑契约测试（绿）**

```bash
mvn -q -Dtest=MigrationBaselineContractTest test
```

预期：V9 两个用例通过。

- [ ] **Step 1.9：跑 H2 baseline 真实迁移验证 V1→V9**

```bash
mvn -q -Dtest=H2BaselineMigrationTest test
```

预期：H2 V1→V9 全部迁移成功。

- [ ] **Step 1.10：修复 FlywayMultiDialectSmokeTest**

打开 `FlywayMultiDialectSmokeTest.java`，把 `assertThat(result.migrationsExecuted).isEqualTo(8)` 改成 9；把 `containsExactly("1",...,"8")` 末尾加 `"9"`。具体：

```java
assertThat(result.migrationsExecuted).as("%s 九个基线迁移执行", vendorName).isEqualTo(9);
// ...
assertThat(applied).extracting(info -> info.getVersion().getVersion())
    .as("%s 完整迁移版本序列", vendorName)
    .containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9");
```

- [ ] **Step 1.11：跑 H2 smoke**

```bash
mvn -q -Dtest='FlywayMultiDialectSmokeTest#h2FlywayBaselineMigrates' test
```

预期：H2 用例通过。

- [ ] **Step 1.12：commit**

```bash
git add medkernel-backend/src/main/resources/db/migration/ medkernel-backend/src/test/java/com/medkernel/migration/
git commit -m "feat(GA-ENG-API-01b): V9 五方言迁移 audit_event +outcome +error_code

跨 postgres/oracle/dm/kingbase/h2 同步给 audit_event 加 outcome
（SUCCESS/FAILED，CHECK 约束）+ error_code 列 + 按 outcome 反查失败
审计的复合索引。契约测试覆盖五方言文件存在性 + 关键 schema 元素；
FlywayMultiDialectSmokeTest 同步到 9 个基线迁移。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 2：AuditEvent + AuditEventRecord + AuditChainWriter 加 outcome/errorCode

**Files：**
- Modify：`medkernel-backend/src/main/java/com/medkernel/shared/audit/AuditEvent.java`
- Modify：`medkernel-backend/src/main/java/com/medkernel/shared/audit/persistence/AuditEventRecord.java`
- Modify：`medkernel-backend/src/main/java/com/medkernel/shared/audit/persistence/AuditChainWriter.java`
- Create：`medkernel-backend/src/test/java/com/medkernel/shared/audit/AuditEventTest.java`
- Modify：`medkernel-backend/src/test/java/com/medkernel/shared/audit/persistence/AuditChainWriterTest.java`（若不存在则跳过修改并新建必要测试）

- [ ] **Step 2.1：先写 AuditEvent 单测（TDD red）**

写 `AuditEventTest.java`：

```java
package com.medkernel.shared.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuditEventTest {

    @Test
    void ofDefaultsToSuccessOutcome() {
        AuditEvent ev = AuditEvent.of(AuditAction.CREATE, "context_snapshot", "ctx-1", "ok");
        assertThat(ev.outcome()).isEqualTo("SUCCESS");
        assertThat(ev.errorCode()).isNull();
    }

    @Test
    void failureFactoryMarksOutcomeAndErrorCode() {
        AuditEvent ev = AuditEvent.failure(AuditAction.EXECUTE, "context_snapshot", "ctx-1",
            "ENG-CONTEXT-003", "INVALID quality 被拒绝");
        assertThat(ev.outcome()).isEqualTo("FAILED");
        assertThat(ev.errorCode()).isEqualTo("ENG-CONTEXT-003");
        assertThat(ev.summary()).isEqualTo("INVALID quality 被拒绝");
        assertThat(ev.action()).isEqualTo(AuditAction.EXECUTE);
    }

    @Test
    void withPayloadDigestPreservesOutcome() {
        AuditEvent ev = AuditEvent.failure(AuditAction.EXECUTE, "x", "y", "ENG-CONTEXT-002", "包不存在")
            .withPayloadDigest("abc");
        assertThat(ev.outcome()).isEqualTo("FAILED");
        assertThat(ev.errorCode()).isEqualTo("ENG-CONTEXT-002");
        assertThat(ev.payloadDigest()).isEqualTo("abc");
    }
}
```

- [ ] **Step 2.2：跑红**

```bash
mvn -q -Dtest=AuditEventTest test
```

预期：编译失败（outcome/errorCode/failure 不存在）。

- [ ] **Step 2.3：扩 AuditEvent record**

打开 `AuditEvent.java`，整体替换 record 头与方法：

```java
package com.medkernel.shared.audit;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuditEvent(
    String id,
    String traceId,
    Instant occurredAt,
    String actorUserId,
    AuditAction action,
    String resourceType,
    String resourceId,
    String summary,
    String payloadDigest,
    OrgScope orgScope,
    String outcome,
    String errorCode
) {

    public static final String OUTCOME_SUCCESS = "SUCCESS";
    public static final String OUTCOME_FAILED  = "FAILED";

    public static AuditEvent of(AuditAction action, String resourceType, String resourceId, String summary) {
        return new AuditEvent(
            UUID.randomUUID().toString(),
            RequestContext.currentTraceId(),
            Instant.now(),
            RequestContext.currentUserId().orElse(null),
            action, resourceType, resourceId, summary,
            null,
            RequestContext.currentOrgScope(),
            OUTCOME_SUCCESS, null
        );
    }

    /** 业务失败留痕：发出 outcome=FAILED + errorCode 的 audit。 */
    public static AuditEvent failure(AuditAction action, String resourceType, String resourceId,
                                     String errorCode, String summary) {
        return new AuditEvent(
            UUID.randomUUID().toString(),
            RequestContext.currentTraceId(),
            Instant.now(),
            RequestContext.currentUserId().orElse(null),
            action, resourceType, resourceId, summary,
            null,
            RequestContext.currentOrgScope(),
            OUTCOME_FAILED, errorCode
        );
    }

    public AuditEvent withPayloadDigest(String digest) {
        return new AuditEvent(id, traceId, occurredAt, actorUserId, action,
            resourceType, resourceId, summary, digest, orgScope, outcome, errorCode);
    }
}
```

- [ ] **Step 2.4：跑 AuditEventTest 绿**

```bash
mvn -q -Dtest=AuditEventTest test
```

预期：3 用例通过。

- [ ] **Step 2.5：扩 AuditEventRecord**

打开 `AuditEventRecord.java`，在末尾两个字段之前插入 outcome/errorCode 字段：

```java
public record AuditEventRecord(
    Long id,
    String eventId,
    String traceId,
    Instant occurredAt,
    String actorUserId,
    String action,
    String resourceType,
    String resourceId,
    String summary,
    String payloadDigest,
    String tenantId,
    String hospitalId,
    String departmentId,
    String prevEventId,
    String prevSignature,
    String signature,
    String status,
    String outcome,
    String errorCode,
    Instant createdAt
) {
}
```

- [ ] **Step 2.6.a：扩 AuditEventRepository.insertEvent INSERT SQL（4 处 SQL 之一）**

打开 `medkernel-backend/src/main/java/com/medkernel/shared/audit/persistence/AuditEventRepository.java`。

把 `insertEvent` 方法整体替换为（在原有 16 字段末尾加 outcome/error_code）：

```java
@Transactional
public void insertEvent(AuditEventRecord record) {
    jdbc.update(
        """
        INSERT INTO audit_event (
            event_id, trace_id, occurred_at, actor_user_id, action,
            resource_type, resource_id, summary, payload_digest,
            tenant_id, hospital_id, department_id,
            prev_event_id, prev_signature, signature, status,
            outcome, error_code
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        record.eventId(),
        record.traceId(),
        Timestamp.from(record.occurredAt()),
        record.actorUserId(),
        record.action(),
        record.resourceType(),
        record.resourceId(),
        record.summary(),
        record.payloadDigest(),
        record.tenantId(),
        record.hospitalId(),
        record.departmentId(),
        record.prevEventId(),
        record.prevSignature(),
        record.signature(),
        record.status(),
        record.outcome(),
        record.errorCode()
    );
}
```

- [ ] **Step 2.6.b：扩 findPage 与 findByEventId 的 SELECT 列**

在 `findPage` 方法内，把 SQL 起始的 `SELECT id, event_id, trace_id, ..., signature, status, created_at` 改为：

```java
StringBuilder sql = new StringBuilder("""
    SELECT id, event_id, trace_id, occurred_at, actor_user_id, action,
           resource_type, resource_id, summary, payload_digest,
           tenant_id, hospital_id, department_id,
           prev_event_id, prev_signature, signature, status,
           outcome, error_code, created_at
      FROM audit_event
     WHERE tenant_id = ?
    """);
```

在 `findByEventId` 方法的 SQL 字符串同样改：

```java
List<AuditEventRecord> rows = jdbc.query(
    """
    SELECT id, event_id, trace_id, occurred_at, actor_user_id, action,
           resource_type, resource_id, summary, payload_digest,
           tenant_id, hospital_id, department_id,
           prev_event_id, prev_signature, signature, status,
           outcome, error_code, created_at
      FROM audit_event
     WHERE tenant_id = ? AND event_id = ?
    """,
    ROW_MAPPER,
    tenantId, eventId);
```

- [ ] **Step 2.6.c：扩 mapRow**

把 `mapRow` 方法整体替换为：

```java
private static AuditEventRecord mapRow(ResultSet rs, int n) throws SQLException {
    return new AuditEventRecord(
        rs.getLong("id"),
        rs.getString("event_id"),
        rs.getString("trace_id"),
        toInstant(rs.getTimestamp("occurred_at")),
        rs.getString("actor_user_id"),
        rs.getString("action"),
        rs.getString("resource_type"),
        rs.getString("resource_id"),
        rs.getString("summary"),
        rs.getString("payload_digest"),
        rs.getString("tenant_id"),
        rs.getString("hospital_id"),
        rs.getString("department_id"),
        rs.getString("prev_event_id"),
        rs.getString("prev_signature"),
        rs.getString("signature"),
        rs.getString("status"),
        rs.getString("outcome"),
        rs.getString("error_code"),
        toInstant(rs.getTimestamp("created_at"))
    );
}
```

- [ ] **Step 2.6.d：扩 AuditChainWriter.persist 构造 AuditEventRecord**

打开 `medkernel-backend/src/main/java/com/medkernel/shared/audit/persistence/AuditChainWriter.java`。

`persist` 方法内构造 `new AuditEventRecord(...)` 处（17 字段，结尾两行是 `STATUS_SIGNED, null`），改为：

```java
AuditEventRecord record = new AuditEventRecord(
    null,
    event.id(),
    event.traceId(),
    event.occurredAt() == null ? Instant.now() : event.occurredAt(),
    event.actorUserId(),
    event.action().name(),
    event.resourceType(),
    event.resourceId(),
    event.summary(),
    event.payloadDigest(),
    tenantId,
    event.orgScope() == null ? null : event.orgScope().hospitalId(),
    event.orgScope() == null ? null : event.orgScope().departmentId(),
    head.lastEventId(),
    prevSignature,
    signature,
    STATUS_SIGNED,
    event.outcome() == null ? AuditEvent.OUTCOME_SUCCESS : event.outcome(),
    event.errorCode(),
    null
);
```

- [ ] **Step 2.6.e：扩 AuditChainWriter.verify 重构 AuditEvent**

`verify` 方法内 `AuditEvent reconstructed = new AuditEvent(...)` 处（10 字段），按 Task 2.3 的 AuditEvent 新 record 顺序在末尾追加 outcome/errorCode：

```java
AuditEvent reconstructed = new AuditEvent(
    record.eventId(),
    record.traceId(),
    record.occurredAt(),
    record.actorUserId(),
    com.medkernel.shared.audit.AuditAction.valueOf(record.action()),
    record.resourceType(),
    record.resourceId(),
    record.summary(),
    record.payloadDigest(),
    new OrgScope(
        isSystemTenant(record.tenantId()) ? null : record.tenantId(),
        null,
        record.hospitalId(),
        null,
        null,
        record.departmentId(),
        null,
        null),
    record.outcome(),
    record.errorCode()
);
```

`canonicalize()` 不需要改：哈希链规范载荷有意保持旧 9 字段以保证旧签名继续可验。outcome/errorCode 是新增可选元数据，不参与签名（避免破坏既有审计链）。

- [ ] **Step 2.7：跑既有 audit 集成测试**

```bash
mvn -q -Dtest='AuditChainWriterTest,AuditPersistenceSinkTest,AuditEventEndToEndTest' test
```

如果项目无某测试类，仅跑存在的。预期：通过（既有用例不破坏，outcome 默认 SUCCESS 兼容）。

- [ ] **Step 2.8：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/shared/audit/AuditEvent.java medkernel-backend/src/main/java/com/medkernel/shared/audit/persistence/AuditEventRecord.java medkernel-backend/src/main/java/com/medkernel/shared/audit/persistence/AuditChainWriter.java medkernel-backend/src/test/java/com/medkernel/shared/audit/AuditEventTest.java
git commit -m "feat(GA-ENG-API-01b): AuditEvent + Record 加 outcome + errorCode

AuditEvent record 加 outcome (SUCCESS/FAILED) + errorCode 字段 +
failure() 工厂；AuditEventRecord 同步扩字段；AuditChainWriter INSERT
两列；既有调用方默认 outcome=SUCCESS 向后兼容。3 单测覆盖 of 默认
SUCCESS / failure 设 FAILED+errorCode / withPayloadDigest 保留 outcome。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 3：IsolatedAuditPublisher（REQUIRES_NEW 子事务发失败 audit）

**Files：**
- Create：`medkernel-backend/src/main/java/com/medkernel/shared/audit/IsolatedAuditPublisher.java`
- Create：`medkernel-backend/src/test/java/com/medkernel/shared/audit/IsolatedAuditPublisherTest.java`

- [ ] **Step 3.1：先写集成测试（TDD red）**

写 `IsolatedAuditPublisherTest.java`：

```java
package com.medkernel.shared.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration/h2"
})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class IsolatedAuditPublisherTest {

    @Autowired IsolatedAuditPublisher isolated;
    @Autowired JdbcTemplate jdbc;

    @Test
    void publishInNewTxSurvivesOuterRollback() {
        RequestContext.restore(new RequestContext.Snapshot(
            "trace-iso", OrgScope.tenant("tenant-iso"), "tester"));
        try {
            try {
                runOuterFailingTx();
            } catch (RuntimeException expected) { /* 主事务回滚 */ }

            Integer found = jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_event WHERE trace_id = ? AND outcome = 'FAILED'",
                Integer.class, "trace-iso");
            assertThat(found).as("失败 audit 必须落库，即使主事务回滚").isEqualTo(1);
        } finally {
            RequestContext.clear();
            jdbc.update("DELETE FROM audit_event WHERE trace_id = ?", "trace-iso");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void runOuterFailingTx() {
        isolated.publishInNewTx(AuditEvent.failure(
            AuditAction.EXECUTE, "context_snapshot", "ctx-iso-1",
            "ENG-CONTEXT-003", "INVALID quality 被拒绝"));
        throw new RuntimeException("主业务失败，触发回滚");
    }
}
```

- [ ] **Step 3.2：跑红**

```bash
mvn -q -Dtest=IsolatedAuditPublisherTest test
```

预期：编译失败（IsolatedAuditPublisher 不存在）。

- [ ] **Step 3.3：写 IsolatedAuditPublisher**

```java
package com.medkernel.shared.audit;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * MedKernel v1.0 GA · GA-ENG-API-01b 失败留痕子事务发布器。
 *
 * <p>当业务方法 {@code @Transactional} 中途抛 ApiException 时，业务事务回滚 →
 * {@code @TransactionalEventListener(AFTER_COMMIT)} 不触发 → 失败 audit 丢失。
 * 本组件用 {@code PROPAGATION_REQUIRES_NEW} 让 audit 走独立子事务，保证 outcome=FAILED
 * 的审计事件不被主事务回滚带走。
 *
 * <p>使用场景仅限于业务失败留痕；成功路径继续走 {@link AuditEventPublisher#publish}
 * 由 AFTER_COMMIT 同事务保证一致性。
 */
@Component
public class IsolatedAuditPublisher {

    private final AuditEventPublisher delegate;
    private final TransactionTemplate requiresNew;

    public IsolatedAuditPublisher(AuditEventPublisher delegate, PlatformTransactionManager txm) {
        this.delegate = delegate;
        this.requiresNew = new TransactionTemplate(txm);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void publishInNewTx(AuditEvent event) {
        requiresNew.executeWithoutResult(status -> delegate.publish(event));
    }
}
```

- [ ] **Step 3.4：跑绿**

```bash
mvn -q -Dtest=IsolatedAuditPublisherTest test
```

预期：用例通过（DELETE 清理确保独立运行）。

- [ ] **Step 3.5：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/shared/audit/IsolatedAuditPublisher.java medkernel-backend/src/test/java/com/medkernel/shared/audit/IsolatedAuditPublisherTest.java
git commit -m "feat(GA-ENG-API-01b): IsolatedAuditPublisher 失败留痕子事务

@Component 内置 PROPAGATION_REQUIRES_NEW TransactionTemplate，
让 outcome=FAILED 的审计事件走独立子事务持久化，避免业务事务回滚
连带丢失失败留痕。集成测试覆盖：主事务回滚后 audit_event 仍可
查到 trace_id 对应的 FAILED 行。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 4：CanonicalResource entity 加 traceId 字段

**Files：**
- Modify：`medkernel-backend/src/main/java/com/medkernel/engine/context/CanonicalResource.java`
- Modify or Create：`medkernel-backend/src/test/java/com/medkernel/engine/context/CanonicalResourceRepositoryTest.java`

- [ ] **Step 4.1：先写仓储测试（TDD red）**

如果 `CanonicalResourceRepositoryTest.java` 不存在则新建；存在则在 class 末尾追加：

```java
@Test
void persistsTraceIdAndReadsBack() {
    String snapshotId = "ctx-trace-1";
    Instant now = Instant.now();
    CanonicalResource saved = repository.save(new CanonicalResource(
        null, "res-trace-1", snapshotId, "tenant-A",
        CanonicalResourceType.PATIENT, "{}", "src", "sr-1", null,
        null, now, QualityStatus.VALID, 0, "trace-canonical"
    ));
    assertThat(saved.id()).isNotNull();

    List<CanonicalResource> rows = repository.findByTraceIdOrderBySeqNoAsc("trace-canonical");
    assertThat(rows).extracting(CanonicalResource::resourceId).contains("res-trace-1");
}
```

测试类头按现有仓储测试约定使用 `@DataJdbcTest + @ImportAutoConfiguration(FlywayAutoConfiguration.class) + @AutoConfigureTestDatabase(replace = NONE) + @TestPropertySource(properties = { "spring.flyway.locations=classpath:db/migration/h2" })`。

如果当前 `CanonicalResourceRepository` 无 `findByTraceIdOrderBySeqNoAsc`，本 Step 同时新增该方法签名（在仓储接口内补 `List<CanonicalResource> findByTraceIdOrderBySeqNoAsc(String traceId);`）。

- [ ] **Step 4.2：跑红**

```bash
mvn -q -Dtest=CanonicalResourceRepositoryTest test
```

预期：编译失败（构造器 15 参，entity 缺 traceId 字段）。

- [ ] **Step 4.3：扩 CanonicalResource entity**

打开 `CanonicalResource.java`，在 `seqNo` 后追加：

```java
@Table("canonical_resource")
public record CanonicalResource(
    @Id Long id,
    @Column("resource_id") String resourceId,
    @Column("snapshot_id") String snapshotId,
    @Column("tenant_id") String tenantId,
    @Column("resource_type") CanonicalResourceType resourceType,
    @Column("resource_payload") String resourcePayloadJson,
    @Column("source_system") String sourceSystem,
    @Column("source_record_id") String sourceRecordId,
    @Column("mapped_version") String mappedVersion,
    @Column("event_time") Instant eventTime,
    @Column("received_time") Instant receivedTime,
    @Column("quality_status") QualityStatus qualityStatus,
    @Column("seq_no") Integer seqNo,
    @Column("trace_id") String traceId
) {}
```

- [ ] **Step 4.4：调整 ContextSnapshotService.persistOne 现有调用**

打开 `ContextSnapshotService.java`，找到 `persistOne` 方法的 `resources.save(new CanonicalResource(...))`，在最后参数 `seq` 后追加 `RequestContext.currentTraceId()`：

```java
resources.save(new CanonicalResource(
    null, "res-" + UUID.randomUUID(), snapshotId, tenantId, type,
    writeJson(payload), null, null, null,
    null, Instant.now(), quality == null ? QualityStatus.VALID : quality, seq,
    RequestContext.currentTraceId()
));
```

- [ ] **Step 4.5：跑绿 + 既有 ContextSnapshotService 测试**

```bash
mvn -q -Dtest='CanonicalResourceRepositoryTest,ContextSnapshotServiceTest' test
```

预期：通过（既有 ContextSnapshotServiceTest 因构造器扩参变化也能编过，逻辑不变）。

- [ ] **Step 4.6：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/engine/context/CanonicalResource.java medkernel-backend/src/main/java/com/medkernel/engine/context/CanonicalResourceRepository.java medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotService.java medkernel-backend/src/test/java/com/medkernel/engine/context/CanonicalResourceRepositoryTest.java
git commit -m "feat(GA-ENG-API-01b): CanonicalResource +traceId 字段

V8 已加 canonical_resource.trace_id 列；本提交让 entity 与 Repository
映射该字段，并在 ContextSnapshotService.persistOne 写入时从
RequestContext 取 traceId 持久化。新增按 trace_id 反查所有 canonical
resource 的 Repository 查询方法，便于第三层横向 trace 反查。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 5：PackageVersionPort 抽象 + LenientPackageVersionAdapter

**Files：**
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/PackageVersionPort.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/LenientPackageVersionAdapter.java`
- Remove：`medkernel-backend/src/main/java/com/medkernel/engine/context/PackageVersionResolver.java`
- Create：`medkernel-backend/src/test/java/com/medkernel/engine/context/PackageVersionPortTest.java`
- Modify：`medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotService.java`（构造器参数与字段类型由 Resolver 换为 Port）
- Modify：现有 ContextSnapshotServiceTest 中 mock 类型从 `PackageVersionResolver` 改为 `PackageVersionPort`

- [ ] **Step 5.1：写 PackageVersionPortTest（TDD red）**

写 `PackageVersionPortTest.java`：

```java
package com.medkernel.engine.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class PackageVersionPortTest {

    private final PackageVersionPort port = new LenientPackageVersionAdapter();

    @Test
    void existsTrueWhenVersionNonBlank() {
        assertThat(port.exists("tenant-A", "knowledge", "v1.0")).isTrue();
    }

    @Test
    void existsFalseWhenVersionNullOrBlank() {
        assertThat(port.exists("tenant-A", "knowledge", null)).isFalse();
        assertThat(port.exists("tenant-A", "knowledge", "  ")).isFalse();
    }

    @Test
    void getActiveReturnsRequestedVersionIfNonBlank() {
        assertThat(port.getActive("tenant-A", "knowledge"))
            .as("Lenient 实现没有真实包注册，返回 empty 让上游 fail-fast")
            .isEmpty();
    }

    @Test
    void getActiveEmptyWhenMissing() {
        Optional<String> v = port.getActive("tenant-X", "rule");
        assertThat(v).isEmpty();
    }
}
```

- [ ] **Step 5.2：跑红**

```bash
mvn -q -Dtest=PackageVersionPortTest test
```

预期：编译失败。

- [ ] **Step 5.3：写 PackageVersionPort 接口**

```java
package com.medkernel.engine.context;

import java.util.Optional;

/**
 * 包版本解析端口。
 *
 * <p>抽象层让 {@link ContextSnapshotService} 不依赖具体的包版本注册中心；
 * 当前默认实现 {@link LenientPackageVersionAdapter}（@ConditionalOnMissingBean）
 * 沿用"非空即合法"行为；API-10 包发布 API 落地后引入
 * {@code KnowledgePackageVersionAdapter} 通过 {@code @Primary} 自动覆盖。
 */
public interface PackageVersionPort {

    /** 包版本是否存在（exists 表示业务可用，含发布、灰度等状态由实现自行决定）。 */
    boolean exists(String tenantId, String packageType, String version);

    /** 当前 tenant + packageType 的活跃版本；未注册返回 empty。 */
    Optional<String> getActive(String tenantId, String packageType);
}
```

- [ ] **Step 5.4：写 LenientPackageVersionAdapter**

```java
package com.medkernel.engine.context;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * MedKernel v1.0 GA · GA-ENG-API-01b 默认包版本端口适配。
 *
 * <p>沿用旧 PackageVersionResolver "非空即合法" 行为；当 API-10 引入真实
 * KnowledgePackageVersionAdapter 时通过 {@code @Primary} 自动覆盖本默认实现。
 */
@Component
@ConditionalOnMissingBean(PackageVersionPort.class)
public class LenientPackageVersionAdapter implements PackageVersionPort {

    @Override
    public boolean exists(String tenantId, String packageType, String version) {
        return version != null && !version.isBlank();
    }

    @Override
    public Optional<String> getActive(String tenantId, String packageType) {
        // Lenient 实现没有真实包注册中心，返回 empty 让上游决策 fail-fast
        return Optional.empty();
    }
}
```

- [ ] **Step 5.5：删除旧 PackageVersionResolver + Service/测试切换类型**

```bash
git rm medkernel-backend/src/main/java/com/medkernel/engine/context/PackageVersionResolver.java
```

打开 `ContextSnapshotService.java`：

字段 `private final PackageVersionResolver versions;` → `private final PackageVersionPort versions;`
构造器参数 `PackageVersionResolver versions,` → `PackageVersionPort versions,`

打开 `ContextSnapshotServiceTest.java`，把字段类型与 setUp 改为：

```java
private PackageVersionPort versions;
// ...
versions = new LenientPackageVersionAdapter();  // 用真实默认实现保留与旧 PackageVersionResolver 等价行为
```

不要 mock —— 现有用例（含 `shouldRejectWhenPackageVersionBlank` 用空版本字符串）依赖 "非空即合法" 行为，真实 Lenient 实现可直接覆盖。

import 追加：`import com.medkernel.engine.context.LenientPackageVersionAdapter;`（同包内可省略）+ 去掉 `PackageVersionResolver` 旧 import。

- [ ] **Step 5.6：跑绿**

```bash
mvn -q -Dtest='PackageVersionPortTest,ContextSnapshotServiceTest' test
```

预期：通过。

- [ ] **Step 5.7：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/engine/context/PackageVersionPort.java medkernel-backend/src/main/java/com/medkernel/engine/context/LenientPackageVersionAdapter.java medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotService.java medkernel-backend/src/test/java/com/medkernel/engine/context/PackageVersionPortTest.java medkernel-backend/src/test/java/com/medkernel/engine/context/ContextSnapshotServiceTest.java
git rm medkernel-backend/src/main/java/com/medkernel/engine/context/PackageVersionResolver.java 2>/dev/null || true
git commit -m "feat(GA-ENG-API-01b): PackageVersionPort 抽象 + Lenient 默认实现

新 PackageVersionPort 接口（exists/getActive）+ LenientPackageVersionAdapter
@ConditionalOnMissingBean 默认实现保留 '非空即合法' 行为；删除旧
PackageVersionResolver；ContextSnapshotService 注入类型切换为
PackageVersionPort，便于 API-10 引入真实 KnowledgePackageVersionAdapter
时通过 @Primary 自动覆盖。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 6：ContextSnapshotService 接入 StateTransitionRecorder（成功路径）

**Files：**
- Modify：`medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotService.java`
- Modify：`medkernel-backend/src/test/java/com/medkernel/engine/context/ContextSnapshotServiceTest.java`

- [ ] **Step 6.1：先写单测（TDD red）**

打开 `ContextSnapshotServiceTest.java`，加 import：

```java
import static org.mockito.ArgumentMatchers.isNull;
import org.mockito.ArgumentCaptor;
import com.medkernel.shared.observability.StateTransitionRecorder;
```

`setUp()` 内（在 `mock(AuditEventPublisher.class)` 之后、`service = new ContextSnapshotService(...)` 之前）加：

```java
recorder = mock(StateTransitionRecorder.class);
```

并把字段声明加在 class 顶部其他 mock 字段旁：`private StateTransitionRecorder recorder;`

修改 `service = new ContextSnapshotService(...)` 调用，按 Task 6.3 的最终构造器顺序补 recorder 参数。

新增用例（贴在 class 末尾，`sampleRequest()` 之前）：

```java
@Test
void createWritesInitialStateTransition() {
    service.create(sampleRequest(), null);

    ArgumentCaptor<String> entityType = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> toStatus = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> reason = ArgumentCaptor.forClass(String.class);
    verify(recorder).record(entityType.capture(), anyString(), isNull(),
        toStatus.capture(), reason.capture(), isNull());

    assertThat(entityType.getValue()).isEqualTo("context_snapshot");
    assertThat(toStatus.getValue()).isEqualTo("ACTIVE");
    assertThat(reason.getValue()).isEqualTo("INITIAL_CREATE");
}
```

`sampleRequest()` 是文件已有的私有工厂（line 241）。

- [ ] **Step 6.2：跑红**

```bash
mvn -q -Dtest='ContextSnapshotServiceTest#createWritesInitialStateTransition' test
```

预期：失败（recorder 未注入，service 未调用 record）。

- [ ] **Step 6.3：Service 注入 recorder 并在 create 成功路径调用**

打开 `ContextSnapshotService.java`：

构造器加参数（紧贴 `AuditEventPublisher auditPublisher`）：

```java
StateTransitionRecorder transitions,
```

字段：

```java
private final StateTransitionRecorder transitions;
```

构造体内 `this.transitions = transitions;`。

在 `create()` 方法末尾 `return ... response` 之前追加：

```java
transitions.record("context_snapshot", saved.snapshotId(),
    null, ContextSnapshotStatus.ACTIVE.name(), "INITIAL_CREATE", null);
```

- [ ] **Step 6.4：跑绿**

```bash
mvn -q -Dtest=ContextSnapshotServiceTest test
```

预期：所有用例（含新增）通过。

- [ ] **Step 6.5：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotService.java medkernel-backend/src/test/java/com/medkernel/engine/context/ContextSnapshotServiceTest.java
git commit -m "feat(GA-ENG-API-01b): ContextSnapshotService 接入 StateTransitionRecorder

create() 成功落库后写一条 state_transition_history
(context_snapshot, snapshotId, null → ACTIVE, INITIAL_CREATE)；
同事务写入，业务回滚则历史一并回滚。单测加 ArgumentCaptor 校验
recorder 被以正确实体类型 + 状态 + reason 调用。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 7：ContextSnapshotService 失败路径用 IsolatedAuditPublisher 发 audit

**Files：**
- Modify：`medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotService.java`
- Modify：`medkernel-backend/src/test/java/com/medkernel/engine/context/ContextSnapshotServiceTest.java`

- [ ] **Step 7.1：先写单测（TDD red）**

注意：现有用例 `shouldNotEmitAuditWhenRejectedByInvalidQuality` 在 Task 7 的新逻辑下含义会变（INVALID 路径现在**会**发 outcome=FAILED 的 audit，只是不发 CREATE 类型的成功 audit）。需要把它**重命名 + 调整断言**为：

```java
@Test
void shouldEmitFailureAuditOnInvalidQualityWithoutCreateAudit() {
    var resourcesDto = new ContextSnapshotResources(null,
        List.of(), List.of(), List.of(), List.of(), List.of(),
        List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    var req = new ContextSnapshotRequest("MPI-1", null, "ORG-1",
        "kpv-1", "rpv-1", "ppv-1", resourcesDto);
    assertThatThrownBy(() -> service.create(req, null)).isInstanceOf(ApiException.class);
    // 成功审计：从未被发布
    verify(auditPublisher, never()).publish(any(AuditAction.class), anyString(), anyString(), anyString());
    // 失败审计：恰好一次，含 outcome=FAILED 与 errorCode
    ArgumentCaptor<AuditEvent> evCap = ArgumentCaptor.forClass(AuditEvent.class);
    verify(isolatedAudit, times(1)).publishInNewTx(evCap.capture());
    assertThat(evCap.getValue().outcome()).isEqualTo(AuditEvent.OUTCOME_FAILED);
    assertThat(evCap.getValue().errorCode()).isEqualTo(ErrorCode.ENG_CONTEXT_003.code());
}
```

在 setUp 内加：

```java
isolatedAudit = mock(IsolatedAuditPublisher.class);
```

字段：`private IsolatedAuditPublisher isolatedAudit;`，并按 Task 7.3 最终构造器顺序补到 `new ContextSnapshotService(...)` 调用。

import 追加：

```java
import com.medkernel.shared.audit.AuditEvent;
import com.medkernel.shared.audit.IsolatedAuditPublisher;
```

继续新增用例（贴在 class 末尾 `sampleRequest()` 之前）：

```java
@Test
void packageVersionMissingTriggersFailureAudit() {
    var req = new ContextSnapshotRequest("MPI-1", null, "ORG-1",
        "kpv-1", "", "ppv-1", validResources());  // rule 包版本空 → ENG-CONTEXT-002

    assertThatThrownBy(() -> service.create(req, null))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.ENG_CONTEXT_002);

    ArgumentCaptor<AuditEvent> evCap = ArgumentCaptor.forClass(AuditEvent.class);
    verify(isolatedAudit, times(1)).publishInNewTx(evCap.capture());
    AuditEvent ev = evCap.getValue();
    assertThat(ev.outcome()).isEqualTo(AuditEvent.OUTCOME_FAILED);
    assertThat(ev.errorCode()).isEqualTo(ErrorCode.ENG_CONTEXT_002.code());
    assertThat(ev.action()).isEqualTo(AuditAction.EXECUTE);
    assertThat(ev.resourceType()).isEqualTo("context_snapshot");
}
```

注：原有 `shouldRejectWhenPackageVersionBlank` 用例与新增 `packageVersionMissingTriggersFailureAudit` 业务断言不同（前者只断 errorCode，后者还断 audit），两个并存即可。

- [ ] **Step 7.2：跑红**

```bash
mvn -q -Dtest=ContextSnapshotServiceTest test
```

预期：两个新用例失败（service 未注入 isolatedAudit 也未调用）。

- [ ] **Step 7.3：Service 注入 IsolatedAuditPublisher + 失败路径**

打开 `ContextSnapshotService.java`：

构造器参数加：

```java
IsolatedAuditPublisher isolatedAudit,
```

字段：

```java
private final IsolatedAuditPublisher isolatedAudit;
```

修改 `validatePackageVersions`（抛 ENG_CONTEXT_002 之前先发 audit）：

```java
private void validatePackageVersions(String tenantId, ContextSnapshotRequest req) {
    if (!versions.exists(tenantId, "knowledge", req.knowledgePackageVersion())
        || !versions.exists(tenantId, "rule", req.rulePackageVersion())
        || !versions.exists(tenantId, "pathway", req.pathwayPackageVersion())) {
        publishFailureAudit(ErrorCode.ENG_CONTEXT_002, "包版本不存在 patient=" + req.patientId());
        throw new ApiException(ErrorCode.ENG_CONTEXT_002, "包版本不存在");
    }
}
```

在 `create()` 方法中 `if (quality == QualityStatus.INVALID)` 块改为：

```java
if (quality == QualityStatus.INVALID) {
    publishFailureAudit(ErrorCode.ENG_CONTEXT_003, "INVALID quality 拒绝创建 patient=" + req.patientId());
    throw new ApiException(ErrorCode.ENG_CONTEXT_003, "INVALID quality 拒绝创建");
}
```

类末尾追加私有方法：

```java
private void publishFailureAudit(ErrorCode code, String summary) {
    isolatedAudit.publishInNewTx(AuditEvent.failure(
        AuditAction.EXECUTE, "context_snapshot", null, code.code(), summary));
}
```

- [ ] **Step 7.4：跑绿**

```bash
mvn -q -Dtest=ContextSnapshotServiceTest test
```

预期：全过。

- [ ] **Step 7.5：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotService.java medkernel-backend/src/test/java/com/medkernel/engine/context/ContextSnapshotServiceTest.java
git commit -m "feat(GA-ENG-API-01b): 失败路径用 IsolatedAuditPublisher 留痕

ContextSnapshotService 在抛 ENG-CONTEXT-002 / ENG-CONTEXT-003
等业务失败前，通过 IsolatedAuditPublisher 在独立子事务里发
AuditEvent.failure(action=EXECUTE, outcome=FAILED, errorCode=...)；
业务事务回滚但 audit_event 留痕，可按 outcome='FAILED' 反查复现
失败请求链路。单测两用例覆盖包版本缺失 / INVALID quality 两路径。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 8：新增 GET /snapshots/{id}/diagnose 端点

**Files：**
- Modify：`medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotService.java`（加 diagnose 方法）
- Modify：`medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotController.java`（加端点）
- Modify：`medkernel-backend/src/test/java/com/medkernel/engine/context/ContextSnapshotControllerSecurityTest.java`（diagnose 权限矩阵）
- Create：`medkernel-backend/src/test/java/com/medkernel/engine/context/ContextSnapshotDiagnoseTest.java`

- [ ] **Step 8.1：先写集成测试（TDD red）**

**调研指令：** 先 Read `medkernel-backend/src/test/java/com/medkernel/engine/context/ContextSnapshotControllerSecurityTest.java` 全文，复用它的 class 注解头（含 @SpringBootTest / @AutoConfigureMockMvc / 任何自定义 @ExtendWith 或 BasePath helper）、租户头注入方式（X-Tenant-Id 或 setupTenant() helper）、@WithMockUser 注解的 authorities 字符串约定（"context.read" 或 "PERM_CONTEXT_READ"）。本 Step 的代码模板按那个文件约定调整。

按调研结果写 `ContextSnapshotDiagnoseTest.java`：

```java
package com.medkernel.engine.context;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;

@SpringBootTest
@AutoConfigureMockMvc
class ContextSnapshotDiagnoseTest {

    @Autowired MockMvc mvc;
    @Autowired ContextSnapshotService service;

    private String existingId;

    @BeforeEach
    void seedSnapshot() {
        // 直接用 service 在测试 setup 阶段创建一个真实 snapshot，避免 @Sql 与权限设置冲突
        RequestContext.restore(new RequestContext.Snapshot(
            "trace-diag-seed", OrgScope.tenant("tenant-A"), "seed-user"));
        try {
            ContextSnapshotResponse resp = service.create(
                ContextSnapshotServiceFixtures.sampleRequest(), null);  // 见 Step 8.1.a
            existingId = resp.snapshotId();
        } finally {
            RequestContext.clear();
        }
    }

    @Test
    @WithMockUser(authorities = {"context.read"})
    void diagnoseReturnsAssembledResponseForExistingSnapshot() throws Exception {
        mvc.perform(get("/api/v1/engine/context/snapshots/{id}/diagnose", existingId)
                .header("X-Tenant-Id", "tenant-A"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.entityType").value("context_snapshot"))
            .andExpect(jsonPath("$.data.currentStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.data.stateHistory").isArray())
            .andExpect(jsonPath("$.data.links.self")
                .value("/api/v1/engine/context_snapshot/" + existingId + "/diagnose"));
    }

    @Test
    @WithMockUser(authorities = {"context.read"})
    void diagnoseReturnsNotFoundForMissingSnapshot() throws Exception {
        mvc.perform(get("/api/v1/engine/context/snapshots/{id}/diagnose", "ctx-nope")
                .header("X-Tenant-Id", "tenant-A"))
            .andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 8.1.a：抽公共 fixture（如果 ContextSnapshotServiceFixtures 不存在则新建）**

把 `ContextSnapshotServiceTest.sampleRequest()` 与 `validResources()` 的内容提取到一个共享 fixture 类 `medkernel-backend/src/test/java/com/medkernel/engine/context/ContextSnapshotServiceFixtures.java`：

```java
package com.medkernel.engine.context;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.medkernel.engine.context.canonical.CanonicalEncounter;
import com.medkernel.engine.context.canonical.CanonicalPatient;

public final class ContextSnapshotServiceFixtures {

    private ContextSnapshotServiceFixtures() {}

    public static ContextSnapshotRequest sampleRequest() {
        return new ContextSnapshotRequest("MPI-1", "ENC-1", "ORG-1",
            "kpv-1", "rpv-1", "ppv-1", validResources());
    }

    public static ContextSnapshotResources validResources() {
        var patient = new CanonicalPatient("MPI-1", "张三",
            LocalDate.of(1980, 1, 1), "M",
            List.of(), List.of(), "HIS", "rec-1", "v1",
            Instant.now(), Instant.now(), QualityStatus.VALID);
        var enc = new CanonicalEncounter("ENC-1", "IP", Instant.now(), null,
            "DEPT-A", "DOC-A", null, "HIS", "rec-2", "v1",
            Instant.now(), Instant.now(), QualityStatus.VALID);
        return new ContextSnapshotResources(patient,
            List.of(enc), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
```

把 `ContextSnapshotServiceTest` 内的 `sampleRequest()` / `validResources()` 私有方法替换为对此 fixture 类静态方法的调用（搜索替换：`sampleRequest()` → `ContextSnapshotServiceFixtures.sampleRequest()`；`validResources()` → `ContextSnapshotServiceFixtures.validResources()`），并删除原私有方法。

- [ ] **Step 8.2：跑红**

```bash
mvn -q -Dtest=ContextSnapshotDiagnoseTest test
```

预期：404 用例可能通过（无 mapping 默认 404），200 用例失败（端点不存在）。

- [ ] **Step 8.3：Service 加 diagnose 方法**

打开 `ContextSnapshotService.java`，加 import：

```java
import com.medkernel.shared.observability.DiagnoseResponse;
import com.medkernel.shared.observability.DiagnoseResponseAssembler;
```

构造器加 `DiagnoseResponseAssembler assembler,` + 字段 `private final DiagnoseResponseAssembler assembler;`。

类内新增方法（紧贴 `findById` 之后）：

```java
@Transactional(readOnly = true)
public DiagnoseResponse diagnose(String snapshotId) {
    String tenantId = requireCurrentTenant();
    ContextSnapshot snap = snapshots.findBySnapshotIdAndTenantId(snapshotId, tenantId)
        .orElseThrow(() -> new ApiException(ErrorCode.ENG_CONTEXT_001,
            "snapshot 不存在: " + snapshotId));
    return assembler.assemble(
        "context_snapshot", snap.snapshotId(), snap.tenantId(),
        snap.status() == null ? null : snap.status().name(),
        snap,
        java.util.List.of(),
        java.util.Map.of(),
        null,
        snap.traceId()
    );
}
```

- [ ] **Step 8.4：Controller 加端点**

打开 `ContextSnapshotController.java`，import：

```java
import com.medkernel.shared.observability.DiagnoseResponse;
```

类内追加：

```java
@GetMapping("/{snapshotId}/diagnose")
@PreAuthorize("@perm.has('context.read')")
public ApiResult<DiagnoseResponse> diagnose(@PathVariable String snapshotId) {
    return ApiResult.ok(service.diagnose(snapshotId));
}
```

- [ ] **Step 8.5：扩 ContextSnapshotControllerSecurityTest 加 diagnose 权限矩阵**

在 `ContextSnapshotControllerSecurityTest.java` 类末尾追加（参考既有 GET by id 的 401/403 用例风格）：

```java
@Test
void diagnoseRequires401WhenAnonymous() throws Exception {
    mvc.perform(get("/api/v1/engine/context/snapshots/ctx-1/diagnose"))
        .andExpect(status().isUnauthorized());
}

@Test
@WithMockUser(authorities = {"context.write"})  // 仅 write，无 read
void diagnoseRequires403WithoutReadPerm() throws Exception {
    mvc.perform(get("/api/v1/engine/context/snapshots/ctx-1/diagnose")
            .header("X-Tenant-Id", "tenant-A"))
        .andExpect(status().isForbidden());
}
```

- [ ] **Step 8.6：跑绿**

```bash
mvn -q -Dtest='ContextSnapshotDiagnoseTest,ContextSnapshotControllerSecurityTest' test
```

预期：全过。

- [ ] **Step 8.7：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotService.java medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotController.java medkernel-backend/src/test/java/com/medkernel/engine/context/ContextSnapshotDiagnoseTest.java medkernel-backend/src/test/java/com/medkernel/engine/context/ContextSnapshotControllerSecurityTest.java
git commit -m "feat(GA-ENG-API-01b): GET /snapshots/{id}/diagnose 端点

Controller 新增 @GetMapping('/{snapshotId}/diagnose') 复用
DiagnoseResponseAssembler 装配 DiagnoseResponse；Service.diagnose
读 snapshot + 当前状态 + 跨 entity 反查 state_transition_history。
权限矩阵：@perm.has('context.read') + @DataScope(requireTenant=true)
匹配既有 GET by id 行为；集成测试覆盖 200/404 + 权限 401/403。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 9：spec 验收 + 端到端 trace 反查测试

**Files：**
- Create：`medkernel-backend/src/test/java/com/medkernel/engine/context/ContextSnapshotTraceEndToEndTest.java`

- [ ] **Step 9.1：写端到端测试覆盖完整链路**

写 `ContextSnapshotTraceEndToEndTest.java`（@SpringBootTest，启用真实事务 + 真实 H2，验证 spec §6.2 验收）：

```java
package com.medkernel.engine.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;

@SpringBootTest
class ContextSnapshotTraceEndToEndTest {

    @Autowired ContextSnapshotService service;
    @Autowired JdbcTemplate jdbc;

    @Test
    void createWritesStateHistoryAndCanonicalTraceIdAndDiagnoseAssemblesAll() {
        String traceId = "trace-e2e-" + System.nanoTime();
        RequestContext.restore(new RequestContext.Snapshot(
            traceId, OrgScope.tenant("tenant-e2e"), "e2e-user"));
        try {
            ContextSnapshotResponse resp = service.create(buildValidRequest(), null);

            // 验收 1：state_transition_history 写入一行
            Integer stateRows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM state_transition_history WHERE entity_id = ? AND to_status = 'ACTIVE'",
                Integer.class, resp.snapshotId());
            assertThat(stateRows).isEqualTo(1);

            // 验收 2：canonical_resource 持久化 trace_id
            Integer traceRows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM canonical_resource WHERE trace_id = ?",
                Integer.class, traceId);
            assertThat(traceRows).isGreaterThan(0);

            // 验收 3：diagnose 返回完整结构
            DiagnoseResponse diag = service.diagnose(resp.snapshotId());
            assertThat(diag.entityType()).isEqualTo("context_snapshot");
            assertThat(diag.currentStatus()).isEqualTo("ACTIVE");
            assertThat(diag.stateHistory()).hasSizeGreaterThanOrEqualTo(1);
            assertThat(diag.traceId()).isEqualTo(traceId);
            assertThat(diag.links().self())
                .isEqualTo("/api/v1/engine/context_snapshot/" + resp.snapshotId() + "/diagnose");
        } finally {
            RequestContext.clear();
            jdbc.update("DELETE FROM state_transition_history WHERE entity_id IN (SELECT snapshot_id FROM context_snapshot WHERE tenant_id = 'tenant-e2e')");
            jdbc.update("DELETE FROM canonical_resource WHERE tenant_id = 'tenant-e2e'");
            jdbc.update("DELETE FROM context_snapshot WHERE tenant_id = 'tenant-e2e'");
        }
    }

    private ContextSnapshotRequest buildValidRequest() {
        return ContextSnapshotServiceFixtures.sampleRequest();
    }
}
```

注：`ContextSnapshotServiceFixtures` 在 Step 8.1.a 已新建并抽公共 fixture。

- [ ] **Step 9.2：跑端到端**

```bash
mvn -q -Dtest=ContextSnapshotTraceEndToEndTest test
```

预期：通过。如失败，定位是 service.create 未写 traceId / state_history 未写 / diagnose 装配丢字段，回到对应 task 修复。

- [ ] **Step 9.3：commit**

```bash
git add medkernel-backend/src/test/java/com/medkernel/engine/context/ContextSnapshotTraceEndToEndTest.java
git commit -m "test(GA-ENG-API-01b): 端到端验证 spec §6.2 三验收

@SpringBootTest 真实 H2 + 真实事务：service.create() 后
state_transition_history / canonical_resource.trace_id /
diagnose 三处一并校验，覆盖 spec §6.2 表头 6 项验收的核心
三项（其余三项已在 Task 3/5/7 单测覆盖）。

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

预期：`Tests run: ≥ 270, Failures: 0, Errors: 0, Skipped: 3, BUILD SUCCESS`。
（OBS-01 后是 255，本 PR 新增约 15-20 用例。）

- [ ] **Step 10.2：跑迁移契约 + H2 baseline**

```bash
mvn -q -Dtest='MigrationBaselineContractTest,H2BaselineMigrationTest,FlywayMultiDialectSmokeTest#h2FlywayBaselineMigrates' test
```

预期：通过。

- [ ] **Step 10.3：跑前端全门禁（确认无回归）**

```bash
cd ../frontend && npm run lint && npm run typecheck && npm test -- --run && npm run build
```

预期：四步全过（API-01b 不动前端，但跑一遍确认）。

- [ ] **Step 10.4：更新 backlog.md**

打开 `docs/backlog.md`。

(a) 在 `## E2 · 引擎接口上线` 表内 `GA-ENG-OBS-01` 行下面、`GA-ENG-API-01` 行下面 插入：

```markdown
| GA-ENG-API-01b 标准上下文 retrofit：snapshot 接 StateTransitionRecorder / canonical_resource 持久化 trace_id / GET /diagnose / PackageVersionPort 抽象 / 失败 audit 留痕 + V9 audit_event +outcome | claude | done |
```

(b) 修订记录表顶部追加（紧贴 4.15 之前）：

```markdown
| 4.16 | 2026-05-27 | Claude | GA-ENG-API-01b 完成：V9 五方言迁移（audit_event +outcome +error_code）+ AuditEvent.failure 工厂 + IsolatedAuditPublisher（PROPAGATION_REQUIRES_NEW 子事务保失败 audit 不丢）+ CanonicalResource +traceId 字段 + PackageVersionPort 抽象 + LenientPackageVersionAdapter 默认实现 + ContextSnapshotService 接入 StateTransitionRecorder + 失败路径发 outcome=FAILED audit + GET /snapshots/{id}/diagnose 端点 + 端到端验证。后端 ≥270 测试 / 前端 79 测试 / lint/typecheck/build 全绿 |
```

- [ ] **Step 10.5：commit backlog**

```bash
cd /Users/zhikunzheng/个人/郑志坤/medkernel/claude
git add docs/backlog.md
git commit -m "chore(GA-ENG-API-01b): backlog 4.16 闭环

GA-ENG-API-01b 标准上下文 retrofit 完成：snapshot 接
StateTransitionRecorder / canonical_resource trace_id 持久化 /
GET /diagnose 端点 / PackageVersionPort 抽象 / 失败 audit 子事务
留痕 / V9 五方言 audit_event +outcome +error_code。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

- [ ] **Step 10.6：推送 + 开 PR**

```bash
git push -u origin feature/ga-eng-api-01b-context-retrofit
gh pr create --title "feat(GA-ENG-API-01b): 标准上下文 retrofit" --body "$(cat <<'EOF'
## Summary
- E2 第二层 retrofit：让 GA-ENG-API-01 标准上下文 API 享受 OBS-01 横切基线全部能力
- 零外部组件叠加；V9 五方言迁移仅给 audit_event +outcome +error_code 两列
- 5 大变更：StateTransitionRecorder 接入 / canonical_resource trace_id 持久化 / diagnose 端点 / PackageVersionPort 抽象 / 失败 audit 留痕

## 变更范围
- V9 五方言迁移：audit_event +outcome (CHECK SUCCESS/FAILED) +error_code +复合索引
- 审计契约：AuditEvent.failure() 工厂 + AuditEventRecord 同步扩字段 + AuditChainWriter INSERT 新列
- 失败留痕：IsolatedAuditPublisher PROPAGATION_REQUIRES_NEW 子事务保 outcome=FAILED audit 不被业务回滚带走
- canonical_resource：entity 加 traceId 字段（V8 已加列）+ Repository findByTraceIdOrderBySeqNoAsc
- PackageVersionPort 抽象：接口 + LenientPackageVersionAdapter @ConditionalOnMissingBean 默认实现；删除旧 PackageVersionResolver
- ContextSnapshotService：注入 recorder + isolatedAudit + assembler；create 写 state_history；失败路径子事务 audit；新增 diagnose 方法
- Controller：新增 @GetMapping("/{snapshotId}/diagnose")

## 验证结果
- 后端 mvn clean test：≥270 测试 / 0 failures / 0 errors / 3 skipped (docker)
- V1→V9 H2 baseline + 五方言契约通过
- 前端 lint/typecheck/test/build 四步无回归

## 医疗安全 · 部署 · 数据迁移影响
- V9 纯增量（audit_event 加可空列 + 一个默认值 SUCCESS 的 NOT NULL 列），向后兼容
- 失败 audit 子事务隔离防止业务异常导致审计追溯失真
- PackageVersionPort 切换路径：API-10 引入真实 Adapter 时 @Primary 自动覆盖，本 PR 零业务影响

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

否则保留在本地等用户决定。

---

## 实施完成

执行完 Task 1..10 后：

- ✅ V9 五方言迁移已合入主线，audit_event 加 outcome + error_code
- ✅ AuditEvent / AuditEventRecord / AuditChainWriter 全链路 outcome/errorCode
- ✅ IsolatedAuditPublisher 失败 audit 子事务隔离落地
- ✅ CanonicalResource entity 加 traceId 字段 + 持久化
- ✅ PackageVersionPort 抽象就绪，API-10 可平滑接入
- ✅ ContextSnapshotService 接 StateTransitionRecorder + 失败留痕 + diagnose
- ✅ GET /snapshots/{id}/diagnose 端点上线
- ✅ 端到端测试验证 spec §6.2 验收
- ✅ 后端 ≥270 测试全绿；前端无回归
- ✅ backlog.md 4.16 闭环

下一步可启动 **GA-ENG-API-02 临床事件 API**（第三层）：V10 迁移 clinical_event_payload + outbox 表、OutboxWorker（FOR UPDATE SKIP LOCKED 五方言）、五个 endpoint（receive/async/batch/payload/replay）+ trace 横向反查端点。

---

**End of GA-ENG-API-01b implementation plan.**
