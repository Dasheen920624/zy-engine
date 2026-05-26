# GA-ENG-API-01 标准上下文 API 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `com.medkernel.engine.context` 下交付 `POST /api/v1/engine/context/snapshots`、`GET /{snapshotId}`、`GET ?patientId=...` 三接口，承担 E2 后续 API-02..07 的标准临床数据入口。

**Architecture:** Spring Data JDBC + Record entity + 扁平 package + Bean Validation；新增 V7 五方言迁移建 4 张表（`context_snapshot` / `canonical_resource` / `clinical_event` / `context_idempotency_key`）；schema 校验、缺失字段分级、quality_status 计算、包版本解析、字典映射端口隔离全在 service 层完成；权限通过 `context.read/context.write` 两个 `PermissionCode` 追加，数据范围通过 `@DataScope(requireTenant=true)` 强制。

**Tech Stack:** JDK 21 / Spring Boot 3.3.5 / Spring Data JDBC 3.3 / Spring Security 6.3 / Flyway 10.20（postgres/oracle/dm/kingbase/h2 五方言）/ jakarta.validation / springdoc-openapi 2.6 / JUnit 5 / Mockito。

**Spec：** [docs/superpowers/specs/2026-05-26-engine-context-snapshot-api-design.md](../specs/2026-05-26-engine-context-snapshot-api-design.md)

---

## Task 1：V7 五方言迁移 + 契约测试

**Files：**
- Create：`medkernel-backend/src/main/resources/db/migration/h2/V7__clinical_context_baseline.sql`
- Create：`medkernel-backend/src/main/resources/db/migration/postgres/V7__clinical_context_baseline.sql`
- Create：`medkernel-backend/src/main/resources/db/migration/oracle/V7__clinical_context_baseline.sql`
- Create：`medkernel-backend/src/main/resources/db/migration/dm/V7__clinical_context_baseline.sql`
- Create：`medkernel-backend/src/main/resources/db/migration/kingbase/V7__clinical_context_baseline.sql`
- Modify：`medkernel-backend/src/test/java/com/medkernel/migration/MigrationBaselineContractTest.java`

- [ ] **Step 1.1：先扩契约测试（TDD red）**

打开 `MigrationBaselineContractTest.java`，在已有断言之后加入 V7 期望（参考现有 V2/V3 的写法）：

```java
@Test
void v7ShouldDeclareClinicalContextTables() {
    String h2 = readMigration("h2", "V7__clinical_context_baseline.sql");
    assertThat(h2).contains("CREATE TABLE IF NOT EXISTS context_snapshot");
    assertThat(h2).contains("CREATE TABLE IF NOT EXISTS canonical_resource");
    assertThat(h2).contains("CREATE TABLE IF NOT EXISTS clinical_event");
    assertThat(h2).contains("CREATE TABLE IF NOT EXISTS context_idempotency_key");
    assertThat(h2).contains("ck_context_snapshot_status");
    assertThat(h2).contains("ck_canonical_resource_type");
    assertThat(h2).contains("ck_canonical_resource_quality");
    assertThat(h2).contains("uk_context_idempotency_tenant_key");
}

@Test
void v7ShouldExistInAllFiveDialects() {
    for (String dialect : List.of("postgres", "oracle", "dm", "kingbase", "h2")) {
        assertThat(migrationPathFor(dialect, "V7__clinical_context_baseline.sql"))
            .as("dialect %s must ship V7", dialect)
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

写 `db/migration/h2/V7__clinical_context_baseline.sql`：

```sql
-- MedKernel v1.0 GA · GA-ENG-API-01 标准临床上下文表族
-- snapshot 一经创建即不可变；唯一权威生效，包版本快照保证可重放。

CREATE TABLE IF NOT EXISTS context_snapshot (
    id                          BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    snapshot_id                 VARCHAR(64)  NOT NULL,
    tenant_id                   VARCHAR(64)  NOT NULL,
    org_unit_id                 VARCHAR(64)  NOT NULL,
    patient_id                  VARCHAR(64)  NOT NULL,
    encounter_id                VARCHAR(64)  NULL,
    knowledge_pkg_version       VARCHAR(64)  NOT NULL,
    rule_pkg_version            VARCHAR(64)  NOT NULL,
    pathway_pkg_version         VARCHAR(64)  NOT NULL,
    status                      VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    missing_fields              CLOB         NULL,
    mapping_status              CLOB         NULL,
    quality_status              VARCHAR(32)  NOT NULL DEFAULT 'VALID',
    trace_id                    VARCHAR(128) NULL,
    signature                   VARCHAR(512) NULL,
    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(64)  NOT NULL DEFAULT 'system',
    CONSTRAINT uk_context_snapshot_id UNIQUE (snapshot_id),
    CONSTRAINT ck_context_snapshot_status CHECK (status IN ('DRAFT','ACTIVE','SUPERSEDED','REJECTED')),
    CONSTRAINT ck_context_snapshot_quality CHECK (quality_status IN ('VALID','PARTIAL','INVALID'))
);

CREATE INDEX IF NOT EXISTS idx_context_snapshot_tenant_patient ON context_snapshot (tenant_id, patient_id, created_at);
CREATE INDEX IF NOT EXISTS idx_context_snapshot_tenant_enc     ON context_snapshot (tenant_id, encounter_id);
CREATE INDEX IF NOT EXISTS idx_context_snapshot_status         ON context_snapshot (tenant_id, status, created_at);

CREATE TABLE IF NOT EXISTS canonical_resource (
    id                  BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    resource_id         VARCHAR(64)  NOT NULL,
    snapshot_id         VARCHAR(64)  NOT NULL,
    tenant_id           VARCHAR(64)  NOT NULL,
    resource_type       VARCHAR(32)  NOT NULL,
    resource_payload    CLOB         NOT NULL,
    source_system       VARCHAR(64)  NULL,
    source_record_id    VARCHAR(128) NULL,
    mapped_version      VARCHAR(64)  NULL,
    event_time          TIMESTAMP    NULL,
    received_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    quality_status      VARCHAR(32)  NOT NULL DEFAULT 'VALID',
    seq_no              INT          NOT NULL DEFAULT 0,
    CONSTRAINT uk_canonical_resource_id UNIQUE (resource_id),
    CONSTRAINT ck_canonical_resource_type CHECK (resource_type IN (
        'PATIENT','ENCOUNTER','CONDITION','SYMPTOM','OBSERVATION',
        'DIAGNOSTIC_REPORT','MEDICATION','PROCEDURE','DOCUMENT',
        'CARE_PLAN','FOLLOW_UP','CLAIM'
    )),
    CONSTRAINT ck_canonical_resource_quality CHECK (quality_status IN ('VALID','PARTIAL','INVALID'))
);

CREATE INDEX IF NOT EXISTS idx_canonical_resource_snapshot      ON canonical_resource (snapshot_id, resource_type, seq_no);
CREATE INDEX IF NOT EXISTS idx_canonical_resource_tenant_type   ON canonical_resource (tenant_id, resource_type);

CREATE TABLE IF NOT EXISTS clinical_event (
    id                  BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id            VARCHAR(64)  NOT NULL,
    tenant_id           VARCHAR(64)  NOT NULL,
    event_type          VARCHAR(32)  NOT NULL,
    source_system       VARCHAR(64)  NULL,
    payload_digest      VARCHAR(128) NULL,
    occurred_at         TIMESTAMP    NOT NULL,
    received_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    snapshot_id         VARCHAR(64)  NULL,
    processing_status   VARCHAR(32)  NOT NULL DEFAULT 'RECEIVED',
    trace_id            VARCHAR(128) NULL,
    CONSTRAINT uk_clinical_event_id UNIQUE (event_id),
    CONSTRAINT ck_clinical_event_type CHECK (event_type IN ('DIAGNOSIS','ORDER','REPORT','DISCHARGE','FOLLOWUP','ADMISSION')),
    CONSTRAINT ck_clinical_event_status CHECK (processing_status IN ('RECEIVED','MAPPED','PROCESSED','FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_clinical_event_tenant_received ON clinical_event (tenant_id, received_at);
CREATE INDEX IF NOT EXISTS idx_clinical_event_snapshot        ON clinical_event (snapshot_id);

CREATE TABLE IF NOT EXISTS context_idempotency_key (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id       VARCHAR(64)  NOT NULL,
    idem_key        VARCHAR(128) NOT NULL,
    snapshot_id     VARCHAR(64)  NOT NULL,
    payload_digest  VARCHAR(128) NOT NULL,
    expires_at      TIMESTAMP    NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_context_idempotency_tenant_key UNIQUE (tenant_id, idem_key)
);

CREATE INDEX IF NOT EXISTS idx_context_idempotency_expires ON context_idempotency_key (expires_at);
```

- [ ] **Step 1.4：复制为 postgres / oracle / dm / kingbase 四方言**

四个方言文件结构与 H2 完全一致，仅做语法适配：

- **postgres**：把 `CLOB` 替换为 `JSONB`；`GENERATED ALWAYS AS IDENTITY` 保留；`DEFAULT CURRENT_TIMESTAMP` 保留。
- **oracle**：`CLOB` 保留；`GENERATED ALWAYS AS IDENTITY` 保留（Oracle 12c+ 支持）；`TIMESTAMP DEFAULT CURRENT_TIMESTAMP` 保留；移除 `IF NOT EXISTS`。
- **dm**：与 oracle 完全一致（DM 是 oracle 兼容）。
- **kingbase**：与 postgres 一致（人大金仓 V8R6+ JSONB 兼容）。

参考 `db/migration/postgres/V2__org_audit_baseline.sql` 中已有的多方言适配范例。

- [ ] **Step 1.5：跑绿**

```bash
cd medkernel-backend && mvn -q -Dtest=MigrationBaselineContractTest test
```

预期：契约测试两个 V7 用例全 PASS。

- [ ] **Step 1.6：跑 H2 baseline 真实迁移验证**

```bash
cd medkernel-backend && mvn -q -Dtest=H2BaselineMigrationTest test
```

预期：H2 V1→V7 全部迁移成功。

- [ ] **Step 1.7：commit**

```bash
git add medkernel-backend/src/main/resources/db/migration/ medkernel-backend/src/test/java/com/medkernel/migration/MigrationBaselineContractTest.java
git commit -m "feat(GA-ENG-API-01): V7 五方言迁移 clinical context 表族

新增 context_snapshot / canonical_resource / clinical_event /
context_idempotency_key 四张表，跨 postgres/oracle/dm/kingbase/h2
同步生效。契约测试覆盖 12 类资源类型枚举、状态机约束、租户唯一索引。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 2：枚举与状态

**Files：**
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotStatus.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/CanonicalResourceType.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/QualityStatus.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/ClinicalEventType.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/ClinicalEventStatus.java`

- [ ] **Step 2.1：写 5 个枚举**

```java
// ContextSnapshotStatus.java
package com.medkernel.engine.context;

public enum ContextSnapshotStatus {
    DRAFT, ACTIVE, SUPERSEDED, REJECTED
}
```

```java
// CanonicalResourceType.java
package com.medkernel.engine.context;

public enum CanonicalResourceType {
    PATIENT, ENCOUNTER, CONDITION, SYMPTOM, OBSERVATION,
    DIAGNOSTIC_REPORT, MEDICATION, PROCEDURE, DOCUMENT,
    CARE_PLAN, FOLLOW_UP, CLAIM
}
```

```java
// QualityStatus.java
package com.medkernel.engine.context;

public enum QualityStatus { VALID, PARTIAL, INVALID }
```

```java
// ClinicalEventType.java
package com.medkernel.engine.context;

public enum ClinicalEventType {
    DIAGNOSIS, ORDER, REPORT, DISCHARGE, FOLLOWUP, ADMISSION
}
```

```java
// ClinicalEventStatus.java
package com.medkernel.engine.context;

public enum ClinicalEventStatus {
    RECEIVED, MAPPED, PROCESSED, FAILED
}
```

- [ ] **Step 2.2：编译验证**

```bash
cd medkernel-backend && mvn -q compile
```

预期：成功。

- [ ] **Step 2.3：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/engine/context/
git commit -m "feat(GA-ENG-API-01): 上下文模块基础枚举

新增 ContextSnapshotStatus / CanonicalResourceType /
QualityStatus / ClinicalEventType / ClinicalEventStatus
5 个枚举，对齐 V7 表 CHECK 约束。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 3：12 个 Canonical Record DTO

**Files：**
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/canonical/CanonicalPatient.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/canonical/CanonicalEncounter.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/canonical/CanonicalCondition.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/canonical/CanonicalSymptom.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/canonical/CanonicalObservation.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/canonical/CanonicalDiagnosticReport.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/canonical/CanonicalMedication.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/canonical/CanonicalProcedure.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/canonical/CanonicalDocument.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/canonical/CanonicalCarePlan.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/canonical/CanonicalFollowUp.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/canonical/CanonicalClaim.java`
- Test：`medkernel-backend/src/test/java/com/medkernel/engine/context/canonical/CanonicalDtoValidationTest.java`

每个 Record 都有：业务必填字段 + 7 个标准元数据（source_system / source_record_id / mapped_version / event_time / received_time / quality_status / trace_id 实际通过顶层 snapshot 注入，DTO 仅携带前 6 个，trace_id 不在此层）。

- [ ] **Step 3.1：先写校验测试（red）**

```java
// CanonicalDtoValidationTest.java
package com.medkernel.engine.context.canonical;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.medkernel.engine.context.QualityStatus;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class CanonicalDtoValidationTest {

    private final Validator validator =
        Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void canonicalPatientRequiresMpiAndName() {
        var invalid = new CanonicalPatient(null, null, null, null, List.of(), List.of(),
            "HIS", "REC-1", "v1", Instant.now(), Instant.now(), QualityStatus.VALID);
        Set<ConstraintViolation<CanonicalPatient>> violations = validator.validate(invalid);
        assertThat(violations).extracting("propertyPath")
            .extracting(Object::toString)
            .contains("mpi", "name");
    }

    @Test
    void canonicalConditionRequiresCodeAndOnsetTime() {
        var invalid = new CanonicalCondition(null, null, null, null, null, null,
            "EMR", "REC-2", "v1", null, Instant.now(), QualityStatus.PARTIAL);
        Set<ConstraintViolation<CanonicalCondition>> violations = validator.validate(invalid);
        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
            .contains("code", "displayName");
    }
}
```

- [ ] **Step 3.2：跑红**

```bash
cd medkernel-backend && mvn -q -Dtest=CanonicalDtoValidationTest test
```

预期：编译失败（DTO 不存在）。

- [ ] **Step 3.3：写 12 个 Record**

参考结构（其他 11 个同样模式，必填字段按 detail spec §7.4 表对齐）：

```java
// CanonicalPatient.java
package com.medkernel.engine.context.canonical;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.medkernel.engine.context.QualityStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CanonicalPatient(
    @NotBlank String mpi,
    @NotBlank String name,
    LocalDate birthDate,
    String gender,                  // M/F/U/O
    List<String> allergies,
    List<String> specialPopulations,
    String sourceSystem,
    String sourceRecordId,
    String mappedVersion,
    Instant eventTime,
    Instant receivedTime,
    @NotNull QualityStatus qualityStatus
) {}
```

```java
// CanonicalEncounter.java
package com.medkernel.engine.context.canonical;

import java.time.Instant;

import com.medkernel.engine.context.QualityStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CanonicalEncounter(
    @NotBlank String encounterId,
    @NotBlank String encounterType,   // OP/IP/ED/DAY_SURGERY
    @NotNull Instant admissionTime,
    Instant dischargeTime,
    String departmentId,
    String attendingDoctorId,
    String bedId,
    String sourceSystem,
    String sourceRecordId,
    String mappedVersion,
    Instant eventTime,
    Instant receivedTime,
    @NotNull QualityStatus qualityStatus
) {}
```

```java
// CanonicalCondition.java
package com.medkernel.engine.context.canonical;

import java.time.Instant;

import com.medkernel.engine.context.QualityStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CanonicalCondition(
    @NotBlank String conditionId,
    @NotBlank String code,
    @NotBlank String codeSystem,      // ICD-10/SNOMED/院内
    @NotBlank String displayName,
    String stage,
    String severity,
    String sourceSystem,
    String sourceRecordId,
    String mappedVersion,
    Instant onsetTime,
    Instant receivedTime,
    @NotNull QualityStatus qualityStatus
) {}
```

```java
// CanonicalSymptom.java
package com.medkernel.engine.context.canonical;

import java.time.Instant;

import com.medkernel.engine.context.QualityStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CanonicalSymptom(
    @NotBlank String symptomId,
    @NotBlank String name,
    String severity,                  // MILD/MODERATE/SEVERE
    String negation,                  // YES/NO/UNKNOWN
    Instant observedAt,
    String sourceSystem,
    String sourceRecordId,
    String mappedVersion,
    Instant eventTime,
    Instant receivedTime,
    @NotNull QualityStatus qualityStatus
) {}
```

```java
// CanonicalObservation.java
package com.medkernel.engine.context.canonical;

import java.math.BigDecimal;
import java.time.Instant;

import com.medkernel.engine.context.QualityStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CanonicalObservation(
    @NotBlank String observationId,
    @NotBlank String code,
    @NotBlank String displayName,
    BigDecimal valueNumeric,
    String valueString,
    String unit,
    String referenceRange,
    String criticalFlag,              // NORMAL/HIGH/LOW/CRITICAL
    String sourceSystem,
    String sourceRecordId,
    String mappedVersion,
    Instant eventTime,
    Instant receivedTime,
    @NotNull QualityStatus qualityStatus
) {}
```

```java
// CanonicalDiagnosticReport.java
package com.medkernel.engine.context.canonical;

import java.time.Instant;
import java.util.List;

import com.medkernel.engine.context.QualityStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CanonicalDiagnosticReport(
    @NotBlank String reportId,
    @NotBlank String reportType,       // LAB/RADIOLOGY/PATHOLOGY/ENDOSCOPY/ECG
    @NotBlank String conclusion,
    List<String> keyFindings,
    String signedBy,
    Instant signedAt,
    String sourceSystem,
    String sourceRecordId,
    String mappedVersion,
    Instant eventTime,
    Instant receivedTime,
    @NotNull QualityStatus qualityStatus
) {}
```

```java
// CanonicalMedication.java
package com.medkernel.engine.context.canonical;

import java.math.BigDecimal;
import java.time.Instant;

import com.medkernel.engine.context.QualityStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CanonicalMedication(
    @NotBlank String medicationId,
    @NotBlank String code,
    @NotBlank String displayName,
    BigDecimal dose,
    String doseUnit,
    String route,
    String frequency,
    String durationDays,
    String prescriptionStatus,        // ACTIVE/COMPLETED/CANCELLED
    String sourceSystem,
    String sourceRecordId,
    String mappedVersion,
    Instant eventTime,
    Instant receivedTime,
    @NotNull QualityStatus qualityStatus
) {}
```

```java
// CanonicalProcedure.java
package com.medkernel.engine.context.canonical;

import java.time.Instant;

import com.medkernel.engine.context.QualityStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CanonicalProcedure(
    @NotBlank String procedureId,
    @NotBlank String code,
    @NotBlank String displayName,
    String anesthesiaType,
    String surgeonId,
    Instant performedAt,
    String sourceSystem,
    String sourceRecordId,
    String mappedVersion,
    Instant eventTime,
    Instant receivedTime,
    @NotNull QualityStatus qualityStatus
) {}
```

```java
// CanonicalDocument.java
package com.medkernel.engine.context.canonical;

import java.time.Instant;

import com.medkernel.engine.context.QualityStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CanonicalDocument(
    @NotBlank String documentId,
    @NotBlank String documentType,    // ADMISSION_NOTE/PROGRESS_NOTE/DISCHARGE_SUMMARY
    String contentDigest,
    String signedBy,
    Instant signedAt,
    String sourceSystem,
    String sourceRecordId,
    String mappedVersion,
    Instant eventTime,
    Instant receivedTime,
    @NotNull QualityStatus qualityStatus
) {}
```

```java
// CanonicalCarePlan.java
package com.medkernel.engine.context.canonical;

import java.time.Instant;

import com.medkernel.engine.context.QualityStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CanonicalCarePlan(
    @NotBlank String planId,
    @NotBlank String pathwayId,
    String currentNodeId,
    String varianceCode,
    Instant plannedFinishAt,
    String sourceSystem,
    String sourceRecordId,
    String mappedVersion,
    Instant eventTime,
    Instant receivedTime,
    @NotNull QualityStatus qualityStatus
) {}
```

```java
// CanonicalFollowUp.java
package com.medkernel.engine.context.canonical;

import java.time.Instant;

import com.medkernel.engine.context.QualityStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CanonicalFollowUp(
    @NotBlank String followUpId,
    @NotBlank String planType,        // POST_DISCHARGE/CHRONIC/SPECIALIST
    Instant plannedAt,
    String questionnaireId,
    String abnormalFlag,
    String sourceSystem,
    String sourceRecordId,
    String mappedVersion,
    Instant eventTime,
    Instant receivedTime,
    @NotNull QualityStatus qualityStatus
) {}
```

```java
// CanonicalClaim.java
package com.medkernel.engine.context.canonical;

import java.math.BigDecimal;
import java.time.Instant;

import com.medkernel.engine.context.QualityStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CanonicalClaim(
    @NotBlank String claimId,
    @NotBlank String drgCode,
    BigDecimal totalCost,
    BigDecimal insurancePaid,
    String sourceSystem,
    String sourceRecordId,
    String mappedVersion,
    Instant eventTime,
    Instant receivedTime,
    @NotNull QualityStatus qualityStatus
) {}
```

- [ ] **Step 3.4：跑绿**

```bash
cd medkernel-backend && mvn -q -Dtest=CanonicalDtoValidationTest test
```

预期：通过。

- [ ] **Step 3.5：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/engine/context/canonical/ medkernel-backend/src/test/java/com/medkernel/engine/context/canonical/
git commit -m "feat(GA-ENG-API-01): 12 个标准临床对象 Record DTO

Patient/Encounter/Condition/Symptom/Observation/DiagnosticReport/
Medication/Procedure/Document/CarePlan/FollowUp/Claim。
每个对象都带 Bean Validation 必填项 + 7 项标准元数据
（source_system/source_record_id/mapped_version/event_time/
received_time/quality_status）。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 4：聚合根与实体

**Files：**
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshot.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/CanonicalResource.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/ClinicalEvent.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/ContextIdempotencyKey.java`

- [ ] **Step 4.1：写 4 个 Spring Data JDBC Entity Record**

```java
// ContextSnapshot.java
package com.medkernel.engine.context;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("context_snapshot")
public record ContextSnapshot(
    @Id Long id,
    @Column("snapshot_id") String snapshotId,
    @Column("tenant_id") String tenantId,
    @Column("org_unit_id") String orgUnitId,
    @Column("patient_id") String patientId,
    @Column("encounter_id") String encounterId,
    @Column("knowledge_pkg_version") String knowledgePackageVersion,
    @Column("rule_pkg_version") String rulePackageVersion,
    @Column("pathway_pkg_version") String pathwayPackageVersion,
    @Column("status") ContextSnapshotStatus status,
    @Column("missing_fields") String missingFieldsJson,
    @Column("mapping_status") String mappingStatusJson,
    @Column("quality_status") QualityStatus qualityStatus,
    @Column("trace_id") String traceId,
    @Column("signature") String signature,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy
) {}
```

```java
// CanonicalResource.java
package com.medkernel.engine.context;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

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
    @Column("seq_no") Integer seqNo
) {}
```

```java
// ClinicalEvent.java
package com.medkernel.engine.context;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("clinical_event")
public record ClinicalEvent(
    @Id Long id,
    @Column("event_id") String eventId,
    @Column("tenant_id") String tenantId,
    @Column("event_type") ClinicalEventType eventType,
    @Column("source_system") String sourceSystem,
    @Column("payload_digest") String payloadDigest,
    @Column("occurred_at") Instant occurredAt,
    @Column("received_at") Instant receivedAt,
    @Column("snapshot_id") String snapshotId,
    @Column("processing_status") ClinicalEventStatus processingStatus,
    @Column("trace_id") String traceId
) {}
```

```java
// ContextIdempotencyKey.java
package com.medkernel.engine.context;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("context_idempotency_key")
public record ContextIdempotencyKey(
    @Id Long id,
    @Column("tenant_id") String tenantId,
    @Column("idem_key") String idempotencyKey,
    @Column("snapshot_id") String snapshotId,
    @Column("payload_digest") String payloadDigest,
    @Column("expires_at") Instant expiresAt,
    @Column("created_at") Instant createdAt
) {}
```

- [ ] **Step 4.2：编译验证**

```bash
cd medkernel-backend && mvn -q compile
```

预期：通过。

- [ ] **Step 4.3：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshot.java medkernel-backend/src/main/java/com/medkernel/engine/context/CanonicalResource.java medkernel-backend/src/main/java/com/medkernel/engine/context/ClinicalEvent.java medkernel-backend/src/main/java/com/medkernel/engine/context/ContextIdempotencyKey.java
git commit -m "feat(GA-ENG-API-01): 上下文聚合根与实体

ContextSnapshot（聚合根）/ CanonicalResource / ClinicalEvent /
ContextIdempotencyKey 四个 Record entity，Spring Data JDBC 映射
对齐 V7 表 snake_case 列名。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 5：仓储 + 仓储测试

**Files：**
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotRepository.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/CanonicalResourceRepository.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/ClinicalEventRepository.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/ContextIdempotencyKeyRepository.java`
- Test：`medkernel-backend/src/test/java/com/medkernel/engine/context/ContextSnapshotRepositoryTest.java`

- [ ] **Step 5.1：先写仓储测试（red）**

```java
// ContextSnapshotRepositoryTest.java
package com.medkernel.engine.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.context.annotation.Import;

import com.medkernel.shared.persistence.JdbcInfraConfig;

@DataJdbcTest
@Import(JdbcInfraConfig.class)
class ContextSnapshotRepositoryTest {

    @Autowired ContextSnapshotRepository snapshots;
    @Autowired CanonicalResourceRepository resources;
    @Autowired ClinicalEventRepository events;
    @Autowired ContextIdempotencyKeyRepository idem;

    @Test
    void shouldPersistAndFindSnapshotBySnapshotId() {
        String snapshotId = "ctx-" + UUID.randomUUID();
        ContextSnapshot saved = snapshots.save(new ContextSnapshot(
            null, snapshotId, "tenant-A", "ORG-1", "MPI-100", "ENC-1",
            "kpv-1", "rpv-1", "ppv-1",
            ContextSnapshotStatus.ACTIVE, "[]", "{}",
            QualityStatus.VALID, "trace-x", null, Instant.now(), "tester"
        ));
        assertThat(saved.id()).isNotNull();

        var found = snapshots.findBySnapshotIdAndTenantId(snapshotId, "tenant-A");
        assertThat(found).isPresent();
        assertThat(found.get().status()).isEqualTo(ContextSnapshotStatus.ACTIVE);
    }

    @Test
    void shouldListResourcesBySnapshotOrderedBySeq() {
        String snapshotId = "ctx-" + UUID.randomUUID();
        snapshots.save(new ContextSnapshot(
            null, snapshotId, "tenant-A", "ORG-1", "MPI-100", "ENC-1",
            "kpv-1", "rpv-1", "ppv-1",
            ContextSnapshotStatus.ACTIVE, "[]", "{}",
            QualityStatus.VALID, "trace-x", null, Instant.now(), "tester"
        ));
        resources.save(new CanonicalResource(null, "res-1", snapshotId, "tenant-A",
            CanonicalResourceType.PATIENT, "{}", "HIS", "REC-1", "v1",
            Instant.now(), Instant.now(), QualityStatus.VALID, 0));
        resources.save(new CanonicalResource(null, "res-2", snapshotId, "tenant-A",
            CanonicalResourceType.MEDICATION, "{}", "HIS", "REC-2", "v1",
            Instant.now(), Instant.now(), QualityStatus.PARTIAL, 1));

        List<CanonicalResource> list = resources.findBySnapshotIdOrderBySeqNoAsc(snapshotId);
        assertThat(list).extracting(CanonicalResource::resourceType)
            .containsExactly(CanonicalResourceType.PATIENT, CanonicalResourceType.MEDICATION);
    }

    @Test
    void shouldEnforceTenantScopedIdempotencyKeyUniqueness() {
        String key = "idem-" + UUID.randomUUID();
        idem.save(new ContextIdempotencyKey(
            null, "tenant-A", key, "ctx-1", "digest-1",
            Instant.now().plusSeconds(86400), Instant.now()
        ));
        var sameTenant = idem.findByTenantIdAndIdempotencyKey("tenant-A", key);
        assertThat(sameTenant).isPresent();

        idem.save(new ContextIdempotencyKey(
            null, "tenant-B", key, "ctx-2", "digest-2",
            Instant.now().plusSeconds(86400), Instant.now()
        ));
        var otherTenant = idem.findByTenantIdAndIdempotencyKey("tenant-B", key);
        assertThat(otherTenant).isPresent();
        assertThat(otherTenant.get().snapshotId()).isEqualTo("ctx-2");
    }
}
```

- [ ] **Step 5.2：跑红**

```bash
cd medkernel-backend && mvn -q -Dtest=ContextSnapshotRepositoryTest test
```

预期：编译失败（4 个 Repository 不存在）。

- [ ] **Step 5.3：实现 4 个 Repository**

```java
// ContextSnapshotRepository.java
package com.medkernel.engine.context;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContextSnapshotRepository extends CrudRepository<ContextSnapshot, Long> {

    Optional<ContextSnapshot> findBySnapshotIdAndTenantId(String snapshotId, String tenantId);

    List<ContextSnapshot> findByTenantIdAndPatientIdOrderByCreatedAtDesc(
        String tenantId, String patientId, Pageable pageable);

    long countByTenantIdAndPatientId(String tenantId, String patientId);

    List<ContextSnapshot> findByTenantIdAndEncounterIdOrderByCreatedAtDesc(
        String tenantId, String encounterId, Pageable pageable);

    long countByTenantIdAndEncounterId(String tenantId, String encounterId);
}
```

```java
// CanonicalResourceRepository.java
package com.medkernel.engine.context;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CanonicalResourceRepository extends CrudRepository<CanonicalResource, Long> {

    List<CanonicalResource> findBySnapshotIdOrderBySeqNoAsc(String snapshotId);
}
```

```java
// ClinicalEventRepository.java
package com.medkernel.engine.context;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClinicalEventRepository extends CrudRepository<ClinicalEvent, Long> {

    Optional<ClinicalEvent> findByEventIdAndTenantId(String eventId, String tenantId);
}
```

```java
// ContextIdempotencyKeyRepository.java
package com.medkernel.engine.context;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContextIdempotencyKeyRepository extends CrudRepository<ContextIdempotencyKey, Long> {

    Optional<ContextIdempotencyKey> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);
}
```

- [ ] **Step 5.4：跑绿**

```bash
cd medkernel-backend && mvn -q -Dtest=ContextSnapshotRepositoryTest test
```

预期：3 个用例通过。

- [ ] **Step 5.5：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/engine/context/*Repository.java medkernel-backend/src/test/java/com/medkernel/engine/context/ContextSnapshotRepositoryTest.java
git commit -m "feat(GA-ENG-API-01): 上下文仓储 + 数据层契约测试

四个 Spring Data JDBC CrudRepository（snapshot/resource/event/idempotency）
+ @DataJdbcTest 三用例覆盖：snapshot 查找、resource 按 seq 列序、
idempotency_key 租户隔离。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 6：字典映射端口接口 + Stub 实现

**Files：**
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/TerminologyMappingPort.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/NoopTerminologyMappingPort.java`

- [ ] **Step 6.1：写端口与 stub 实现**

```java
// TerminologyMappingPort.java
package com.medkernel.engine.context;

import java.util.Map;

/**
 * 字典映射端口。
 *
 * <p>{@link ContextSnapshotService} 通过此端口查询每类资源的映射状态，
 * 而非直连 {@code engine.terminology} 内部实现，避免循环依赖。
 *
 * <p>当 terminology 模块未提供该端口的 Bean 时，使用 {@link NoopTerminologyMappingPort}
 * 默认 stub，返回 UNKNOWN 状态，snapshot 仍可创建但 mapping_status 标记为待补全。
 */
public interface TerminologyMappingPort {

    /**
     * 评估 snapshot 中各资源类型的映射状态。
     *
     * @param tenantId          租户
     * @param snapshotSummary   每个资源类型的 code 列表（按 {@link CanonicalResourceType} 分组）
     * @return                  resource_type.field → "VALID" / "PARTIAL" / "UNKNOWN"
     */
    Map<String, String> evaluate(String tenantId, Map<CanonicalResourceType, java.util.List<String>> snapshotSummary);
}
```

```java
// NoopTerminologyMappingPort.java
package com.medkernel.engine.context;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * 字典映射端口的默认空实现，所有类型返回 UNKNOWN。
 *
 * <p>当 terminology 模块尚未提供 {@link TerminologyMappingPort} Bean 时启用，
 * 不阻断 snapshot 创建，便于 API-01 单独验收。
 */
@Component
@ConditionalOnMissingBean(TerminologyMappingPort.class)
class NoopTerminologyMappingPort implements TerminologyMappingPort {

    @Override
    public Map<String, String> evaluate(String tenantId,
            Map<CanonicalResourceType, List<String>> snapshotSummary) {
        return snapshotSummary.keySet().stream()
            .collect(Collectors.toMap(t -> t.name() + ".code", t -> "UNKNOWN"));
    }
}
```

- [ ] **Step 6.2：编译验证**

```bash
cd medkernel-backend && mvn -q compile
```

预期：通过。

- [ ] **Step 6.3：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/engine/context/TerminologyMappingPort.java medkernel-backend/src/main/java/com/medkernel/engine/context/NoopTerminologyMappingPort.java
git commit -m "feat(GA-ENG-API-01): 字典映射端口 + 默认 noop 实现

TerminologyMappingPort 接口隔离 engine.context 与 engine.terminology
循环依赖。NoopTerminologyMappingPort 通过 @ConditionalOnMissingBean
作为默认 stub 返回 UNKNOWN，便于 API-01 独立验收。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 7：DTO 入参出参

**Files：**
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotRequest.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotResources.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotResponse.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotSummary.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotFilter.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/MissingFieldEntry.java`

- [ ] **Step 7.1：写 6 个 API DTO**

```java
// MissingFieldEntry.java
package com.medkernel.engine.context;

public record MissingFieldEntry(
    String resourceType,
    String field,
    String level         // WARN / ERROR / CRITICAL
) {}
```

```java
// ContextSnapshotResources.java
package com.medkernel.engine.context;

import java.util.List;

import com.medkernel.engine.context.canonical.CanonicalCarePlan;
import com.medkernel.engine.context.canonical.CanonicalClaim;
import com.medkernel.engine.context.canonical.CanonicalCondition;
import com.medkernel.engine.context.canonical.CanonicalDiagnosticReport;
import com.medkernel.engine.context.canonical.CanonicalDocument;
import com.medkernel.engine.context.canonical.CanonicalEncounter;
import com.medkernel.engine.context.canonical.CanonicalFollowUp;
import com.medkernel.engine.context.canonical.CanonicalMedication;
import com.medkernel.engine.context.canonical.CanonicalObservation;
import com.medkernel.engine.context.canonical.CanonicalPatient;
import com.medkernel.engine.context.canonical.CanonicalProcedure;
import com.medkernel.engine.context.canonical.CanonicalSymptom;

import jakarta.validation.Valid;

public record ContextSnapshotResources(
    @Valid CanonicalPatient patient,
    @Valid List<CanonicalEncounter> encounters,
    @Valid List<CanonicalCondition> conditions,
    @Valid List<CanonicalSymptom> symptoms,
    @Valid List<CanonicalObservation> observations,
    @Valid List<CanonicalDiagnosticReport> diagnosticReports,
    @Valid List<CanonicalMedication> medications,
    @Valid List<CanonicalProcedure> procedures,
    @Valid List<CanonicalDocument> documents,
    @Valid List<CanonicalCarePlan> carePlans,
    @Valid List<CanonicalFollowUp> followUps,
    @Valid List<CanonicalClaim> claims
) {}
```

```java
// ContextSnapshotRequest.java
package com.medkernel.engine.context;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ContextSnapshotRequest(
    @NotBlank String patientId,
    String encounterId,
    @NotBlank String orgUnitId,
    @NotBlank String knowledgePackageVersion,
    @NotBlank String rulePackageVersion,
    @NotBlank String pathwayPackageVersion,
    @NotNull @Valid ContextSnapshotResources resources
) {}
```

```java
// ContextSnapshotResponse.java
package com.medkernel.engine.context;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ContextSnapshotResponse(
    String snapshotId,
    ContextSnapshotStatus status,
    QualityStatus qualityStatus,
    List<MissingFieldEntry> missingFields,
    Map<String, String> mappingStatus,
    Instant createdAt,
    String traceId
) {}
```

```java
// ContextSnapshotSummary.java
package com.medkernel.engine.context;

import java.time.Instant;

public record ContextSnapshotSummary(
    String snapshotId,
    String patientId,
    String encounterId,
    ContextSnapshotStatus status,
    QualityStatus qualityStatus,
    Instant createdAt
) {}
```

```java
// ContextSnapshotFilter.java
package com.medkernel.engine.context;

import java.time.Instant;

public record ContextSnapshotFilter(
    String patientId,
    String encounterId,
    ContextSnapshotStatus status,
    Instant eventTimeFrom,
    Instant eventTimeTo
) {}
```

- [ ] **Step 7.2：编译验证**

```bash
cd medkernel-backend && mvn -q compile
```

预期：通过。

- [ ] **Step 7.3：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotRequest.java medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotResources.java medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotResponse.java medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotSummary.java medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotFilter.java medkernel-backend/src/main/java/com/medkernel/engine/context/MissingFieldEntry.java
git commit -m "feat(GA-ENG-API-01): API DTO 入参出参

ContextSnapshotRequest（顶层入参）+ ContextSnapshotResources
（12 类资源容器）+ ContextSnapshotResponse / Summary / Filter +
MissingFieldEntry 共 6 个 Record，覆盖三接口的全部 IO 契约。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 8：ContextValidator + 单测

**Files：**
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/ContextValidator.java`
- Test：`medkernel-backend/src/test/java/com/medkernel/engine/context/ContextValidatorTest.java`

- [ ] **Step 8.1：先写单测（red）**

```java
// ContextValidatorTest.java
package com.medkernel.engine.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.medkernel.engine.context.canonical.CanonicalEncounter;
import com.medkernel.engine.context.canonical.CanonicalPatient;

class ContextValidatorTest {

    private final ContextValidator validator = new ContextValidator();

    @Test
    void shouldDetectMissingPatientField() {
        var patient = new CanonicalPatient("MPI-1", "张三", null, "M",
            List.of(), List.of(), "HIS", "rec-1", "v1",
            Instant.now(), Instant.now(), QualityStatus.PARTIAL);
        var resources = new ContextSnapshotResources(patient,
            List.of(), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

        List<MissingFieldEntry> missing = validator.findMissingFields(resources);

        assertThat(missing).extracting(MissingFieldEntry::field).contains("birthDate");
        assertThat(missing).extracting(MissingFieldEntry::level).contains("WARN");
    }

    @Test
    void shouldComputeOverallQualityFromResources() {
        var patient = new CanonicalPatient("MPI-1", "张三", null, "M",
            List.of(), List.of(), "HIS", "rec-1", "v1",
            Instant.now(), Instant.now(), QualityStatus.PARTIAL);
        var resources = new ContextSnapshotResources(patient,
            List.of(), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

        assertThat(validator.computeQuality(resources)).isEqualTo(QualityStatus.PARTIAL);
    }

    @Test
    void shouldRejectWhenPatientIsNull() {
        var resources = new ContextSnapshotResources(null,
            List.of(), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

        List<MissingFieldEntry> missing = validator.findMissingFields(resources);
        assertThat(missing).extracting(MissingFieldEntry::level).contains("CRITICAL");
    }

    @Test
    void shouldFlagEncounterAdmissionTimeMissingAsError() {
        var patient = new CanonicalPatient("MPI-1", "张三",
            java.time.LocalDate.of(1980, 1, 1), "M",
            List.of(), List.of(), "HIS", "rec-1", "v1",
            Instant.now(), Instant.now(), QualityStatus.VALID);
        // CanonicalEncounter 的 admissionTime @NotNull，校验先在 Bean Validation 拦截；
        // 但若上层放行（例如 PATIAL 接受），ContextValidator 也应识别这种业务必填缺失。
        var enc = new CanonicalEncounter("ENC-1", "IP", null, null,
            "DEPT-A", "DOC-A", null, "HIS", "rec-2", "v1",
            Instant.now(), Instant.now(), QualityStatus.PARTIAL);
        var resources = new ContextSnapshotResources(patient,
            List.of(enc), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

        List<MissingFieldEntry> missing = validator.findMissingFields(resources);
        assertThat(missing).anyMatch(m ->
            m.resourceType().equals("ENCOUNTER") && m.field().equals("admissionTime") && m.level().equals("ERROR"));
    }
}
```

- [ ] **Step 8.2：跑红**

```bash
cd medkernel-backend && mvn -q -Dtest=ContextValidatorTest test
```

预期：编译失败（ContextValidator 不存在）。

- [ ] **Step 8.3：实现 ContextValidator**

```java
// ContextValidator.java
package com.medkernel.engine.context;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.medkernel.engine.context.canonical.CanonicalCondition;
import com.medkernel.engine.context.canonical.CanonicalEncounter;
import com.medkernel.engine.context.canonical.CanonicalPatient;

/**
 * 标准上下文 schema 与业务必填校验。
 *
 * <p>Bean Validation 已拦截基本必填项；本组件追加：
 * <ul>
 *   <li>Patient 缺失（CRITICAL，拒绝创建）</li>
 *   <li>关键业务字段缺失（如 Encounter.admissionTime ERROR / Patient.birthDate WARN）</li>
 *   <li>从各资源 qualityStatus 计算 snapshot 整体 quality</li>
 * </ul>
 */
@Component
public class ContextValidator {

    public List<MissingFieldEntry> findMissingFields(ContextSnapshotResources resources) {
        List<MissingFieldEntry> missing = new ArrayList<>();
        if (resources.patient() == null) {
            missing.add(new MissingFieldEntry("PATIENT", "*", "CRITICAL"));
            return missing;
        }
        CanonicalPatient p = resources.patient();
        if (p.birthDate() == null) {
            missing.add(new MissingFieldEntry("PATIENT", "birthDate", "WARN"));
        }
        if (resources.encounters() != null) {
            for (CanonicalEncounter enc : resources.encounters()) {
                if (enc.admissionTime() == null) {
                    missing.add(new MissingFieldEntry("ENCOUNTER", "admissionTime", "ERROR"));
                }
            }
        }
        if (resources.conditions() != null) {
            for (CanonicalCondition c : resources.conditions()) {
                if (c.codeSystem() == null || c.codeSystem().isBlank()) {
                    missing.add(new MissingFieldEntry("CONDITION", "codeSystem", "ERROR"));
                }
            }
        }
        return missing;
    }

    public QualityStatus computeQuality(ContextSnapshotResources resources) {
        if (resources.patient() == null) {
            return QualityStatus.INVALID;
        }
        boolean hasInvalid = false;
        boolean hasPartial = false;
        for (QualityStatus s : collectStatuses(resources)) {
            if (s == QualityStatus.INVALID) hasInvalid = true;
            else if (s == QualityStatus.PARTIAL) hasPartial = true;
        }
        if (hasInvalid) return QualityStatus.INVALID;
        if (hasPartial) return QualityStatus.PARTIAL;
        return QualityStatus.VALID;
    }

    private List<QualityStatus> collectStatuses(ContextSnapshotResources r) {
        List<QualityStatus> all = new ArrayList<>();
        if (r.patient() != null) all.add(r.patient().qualityStatus());
        r.encounters().forEach(e -> all.add(e.qualityStatus()));
        r.conditions().forEach(e -> all.add(e.qualityStatus()));
        r.symptoms().forEach(e -> all.add(e.qualityStatus()));
        r.observations().forEach(e -> all.add(e.qualityStatus()));
        r.diagnosticReports().forEach(e -> all.add(e.qualityStatus()));
        r.medications().forEach(e -> all.add(e.qualityStatus()));
        r.procedures().forEach(e -> all.add(e.qualityStatus()));
        r.documents().forEach(e -> all.add(e.qualityStatus()));
        r.carePlans().forEach(e -> all.add(e.qualityStatus()));
        r.followUps().forEach(e -> all.add(e.qualityStatus()));
        r.claims().forEach(e -> all.add(e.qualityStatus()));
        return all;
    }
}
```

- [ ] **Step 8.4：跑绿**

```bash
cd medkernel-backend && mvn -q -Dtest=ContextValidatorTest test
```

预期：4 个用例通过。

- [ ] **Step 8.5：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/engine/context/ContextValidator.java medkernel-backend/src/test/java/com/medkernel/engine/context/ContextValidatorTest.java
git commit -m "feat(GA-ENG-API-01): ContextValidator schema + quality 计算

业务必填校验：Patient 缺失 = CRITICAL；Encounter.admissionTime /
Condition.codeSystem 缺失 = ERROR；Patient.birthDate 缺失 = WARN。
quality_status 从 12 类资源聚合：INVALID > PARTIAL > VALID。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 9：权限码与错误码扩展

**Files：**
- Modify：`medkernel-backend/src/main/java/com/medkernel/engine/security/PermissionCode.java`
- Modify：`medkernel-backend/src/main/java/com/medkernel/shared/api/error/ErrorCode.java`
- Test：`medkernel-backend/src/test/java/com/medkernel/engine/security/PermissionCodeTest.java`（新建或扩展）

- [ ] **Step 9.1：找到 PermissionCode 现有枚举末尾**

读 `PermissionCode.java`：在 `SYSTEM_MANAGE` 之前新增 context 权限码区段。

- [ ] **Step 9.2：编辑 PermissionCode 末尾追加（不删旧值）**

定位到 `SYSTEM_MANAGE("system.manage", Risk.HIGH, ...)` 这一行的上一行（行号在文件中），在前面插入：

```java
    // ─── 标准上下文（GA-ENG-API-01）────────────────────────────
    CONTEXT_READ("context.read", Risk.LOW, "查看标准上下文 snapshot"),
    CONTEXT_WRITE("context.write", Risk.MEDIUM, "创建标准上下文 snapshot"),

```

- [ ] **Step 9.3：在 ErrorCode.java 末尾追加 4 个上下文错误码**

```java
    ENG_CONTEXT_001("ENG-CONTEXT-001", 400, "上下文 schema 校验失败"),
    ENG_CONTEXT_002("ENG-CONTEXT-002", 400, "包版本不存在"),
    ENG_CONTEXT_003("ENG-CONTEXT-003", 400, "标准上下文 quality_status=INVALID 被拒绝"),
    ENG_CONTEXT_004("ENG-CONTEXT-004", 409, "幂等键冲突且 payload 不一致"),
```

参照已有的 ErrorCode 命名习惯与构造器签名（如果现有码使用不同字段，按现状对齐）。

- [ ] **Step 9.4：跑现有 PermissionEvaluator/DefaultPermissionPolicyTest 确保未破坏**

```bash
cd medkernel-backend && mvn -q -Dtest=PermissionEvaluatorTest,DefaultPermissionPolicyTest test
```

预期：通过。

- [ ] **Step 9.5：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/engine/security/PermissionCode.java medkernel-backend/src/main/java/com/medkernel/shared/api/error/ErrorCode.java
git commit -m "feat(GA-ENG-API-01): 追加 context 权限码与错误码

PermissionCode 末尾新增 CONTEXT_READ (low) / CONTEXT_WRITE (medium)。
ErrorCode 新增 ENG-CONTEXT-001..004 覆盖 schema 失败、包版本不存在、
quality=INVALID 拒绝、幂等冲突四类错误。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 10：ContextSnapshotService + 单测

**Files：**
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/PackageVersionResolver.java`
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotService.java`
- Test：`medkernel-backend/src/test/java/com/medkernel/engine/context/ContextSnapshotServiceTest.java`

- [ ] **Step 10.1：写 PackageVersionResolver（最小实现 + 占位）**

```java
// PackageVersionResolver.java
package com.medkernel.engine.context;

import org.springframework.stereotype.Component;

/**
 * 包版本解析。
 *
 * <p>本任务以"非空即合法"作为最小实现；待 API-10 包发布 API 落地后接其真实查询。
 * 通过该组件单独隔离，未来切换实现不动 Service。
 */
@Component
public class PackageVersionResolver {

    public boolean exists(String tenantId, String packageType, String version) {
        return version != null && !version.isBlank();
    }
}
```

- [ ] **Step 10.2：先写 Service 单测（red，覆盖核心路径）**

```java
// ContextSnapshotServiceTest.java
package com.medkernel.engine.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medkernel.engine.context.canonical.CanonicalEncounter;
import com.medkernel.engine.context.canonical.CanonicalPatient;
import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.context.RequestContext;

class ContextSnapshotServiceTest {

    private ContextSnapshotRepository snapshots;
    private CanonicalResourceRepository resources;
    private ContextIdempotencyKeyRepository idemRepo;
    private ContextValidator validator;
    private PackageVersionResolver versions;
    private TerminologyMappingPort mapping;
    private ContextSnapshotService service;

    @BeforeEach
    void setUp() {
        snapshots = mock(ContextSnapshotRepository.class);
        resources = mock(CanonicalResourceRepository.class);
        idemRepo = mock(ContextIdempotencyKeyRepository.class);
        validator = new ContextValidator();
        versions = new PackageVersionResolver();
        mapping = mock(TerminologyMappingPort.class);
        when(mapping.evaluate(anyString(), any())).thenReturn(Map.of());
        service = new ContextSnapshotService(snapshots, resources, idemRepo,
            validator, versions, mapping, new ObjectMapper());

        when(snapshots.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(resources.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(idemRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void shouldCreateSnapshotWhenAllValid() {
        var req = sampleRequest();
        RequestContext.runWith(tenantContext("tenant-A"), () -> {
            ContextSnapshotResponse resp = service.create(req, null);
            assertThat(resp.snapshotId()).startsWith("ctx-");
            assertThat(resp.status()).isEqualTo(ContextSnapshotStatus.ACTIVE);
            assertThat(resp.qualityStatus()).isEqualTo(QualityStatus.VALID);
            return null;
        });
    }

    @Test
    void shouldRejectWhenPatientMissing() {
        var resourcesDto = new ContextSnapshotResources(null,
            List.of(), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        var req = new ContextSnapshotRequest("MPI-1", null, "ORG-1",
            "kpv-1", "rpv-1", "ppv-1", resourcesDto);

        RequestContext.runWith(tenantContext("tenant-A"), () -> {
            assertThatThrownBy(() -> service.create(req, null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("ENG-CONTEXT-003");
            return null;
        });
    }

    @Test
    void shouldReturnCachedSnapshotWhenIdempotencyKeyMatches() {
        when(idemRepo.findByTenantIdAndIdempotencyKey("tenant-A", "key-1"))
            .thenReturn(Optional.of(new ContextIdempotencyKey(
                1L, "tenant-A", "key-1", "ctx-cached", "digest",
                Instant.now().plusSeconds(60), Instant.now())));
        when(snapshots.findBySnapshotIdAndTenantId("ctx-cached", "tenant-A"))
            .thenReturn(Optional.of(new ContextSnapshot(
                1L, "ctx-cached", "tenant-A", "ORG-1", "MPI-1", null,
                "kpv-1", "rpv-1", "ppv-1",
                ContextSnapshotStatus.ACTIVE, "[]", "{}",
                QualityStatus.VALID, "trace", null, Instant.now(), "tester")));

        RequestContext.runWith(tenantContext("tenant-A"), () -> {
            ContextSnapshotResponse resp = service.create(sampleRequest(), "key-1");
            assertThat(resp.snapshotId()).isEqualTo("ctx-cached");
            return null;
        });
    }

    private ContextSnapshotRequest sampleRequest() {
        var patient = new CanonicalPatient("MPI-1", "张三",
            LocalDate.of(1980, 1, 1), "M",
            List.of(), List.of(), "HIS", "rec-1", "v1",
            Instant.now(), Instant.now(), QualityStatus.VALID);
        var enc = new CanonicalEncounter("ENC-1", "IP", Instant.now(), null,
            "DEPT-A", "DOC-A", null, "HIS", "rec-2", "v1",
            Instant.now(), Instant.now(), QualityStatus.VALID);
        var resourcesDto = new ContextSnapshotResources(patient,
            List.of(enc), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        return new ContextSnapshotRequest("MPI-1", "ENC-1", "ORG-1",
            "kpv-1", "rpv-1", "ppv-1", resourcesDto);
    }

    private RequestContext tenantContext(String tenantId) {
        // 使用项目已有的 RequestContext.builder() 或工厂方法构造（按现状对齐）。
        // 这里写法示例：
        return RequestContext.builder().tenantId(tenantId).userId("tester").traceId("trace-test").build();
    }
}
```

> **执行者注意**：`RequestContext` 与 `ApiException` 的实际构造方式按代码现状对齐。若 `RequestContext.runWith` 不存在，改用 `RequestContextHolder.set(...)` + try/finally clear 的现有写法（参考 `OrgUnitServiceTest`）。

- [ ] **Step 10.3：跑红**

```bash
cd medkernel-backend && mvn -q -Dtest=ContextSnapshotServiceTest test
```

预期：编译失败（Service 不存在）。

- [ ] **Step 10.4：实现 ContextSnapshotService**

```java
// ContextSnapshotService.java
package com.medkernel.engine.context;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medkernel.engine.context.canonical.CanonicalCarePlan;
import com.medkernel.engine.context.canonical.CanonicalClaim;
import com.medkernel.engine.context.canonical.CanonicalCondition;
import com.medkernel.engine.context.canonical.CanonicalDiagnosticReport;
import com.medkernel.engine.context.canonical.CanonicalDocument;
import com.medkernel.engine.context.canonical.CanonicalEncounter;
import com.medkernel.engine.context.canonical.CanonicalFollowUp;
import com.medkernel.engine.context.canonical.CanonicalMedication;
import com.medkernel.engine.context.canonical.CanonicalObservation;
import com.medkernel.engine.context.canonical.CanonicalPatient;
import com.medkernel.engine.context.canonical.CanonicalProcedure;
import com.medkernel.engine.context.canonical.CanonicalSymptom;
import com.medkernel.shared.api.PageResponse;
import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;
import com.medkernel.shared.context.RequestContext;

@Service
public class ContextSnapshotService {

    private final ContextSnapshotRepository snapshots;
    private final CanonicalResourceRepository resources;
    private final ContextIdempotencyKeyRepository idemRepo;
    private final ContextValidator validator;
    private final PackageVersionResolver versions;
    private final TerminologyMappingPort mapping;
    private final ObjectMapper json;

    public ContextSnapshotService(ContextSnapshotRepository snapshots,
                                  CanonicalResourceRepository resources,
                                  ContextIdempotencyKeyRepository idemRepo,
                                  ContextValidator validator,
                                  PackageVersionResolver versions,
                                  TerminologyMappingPort mapping,
                                  ObjectMapper json) {
        this.snapshots = snapshots;
        this.resources = resources;
        this.idemRepo = idemRepo;
        this.validator = validator;
        this.versions = versions;
        this.mapping = mapping;
        this.json = json;
    }

    @Transactional
    public ContextSnapshotResponse create(ContextSnapshotRequest req, String idempotencyKey) {
        String tenantId = RequestContext.currentTenantId();
        String userId = RequestContext.currentUserId();
        String traceId = RequestContext.currentTraceId();

        if (idempotencyKey != null) {
            Optional<ContextIdempotencyKey> existing =
                idemRepo.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey);
            if (existing.isPresent()) {
                ContextSnapshot snap = snapshots.findBySnapshotIdAndTenantId(
                    existing.get().snapshotId(), tenantId)
                    .orElseThrow(() -> ApiException.of(ErrorCode.ENG_CONTEXT_004, "幂等记录无对应 snapshot"));
                return toResponse(snap, List.of(), Map.of());
            }
        }

        validatePackageVersions(tenantId, req);

        List<MissingFieldEntry> missing = validator.findMissingFields(req.resources());
        QualityStatus quality = validator.computeQuality(req.resources());
        if (quality == QualityStatus.INVALID) {
            throw ApiException.of(ErrorCode.ENG_CONTEXT_003, "INVALID quality 拒绝创建");
        }

        Map<CanonicalResourceType, List<String>> summary = summarizeForMapping(req.resources());
        Map<String, String> mappingStatus = mapping.evaluate(tenantId, summary);

        String snapshotId = "ctx-" + UUID.randomUUID();
        Instant now = Instant.now();
        ContextSnapshot saved = snapshots.save(new ContextSnapshot(
            null, snapshotId, tenantId, req.orgUnitId(),
            req.patientId(), req.encounterId(),
            req.knowledgePackageVersion(), req.rulePackageVersion(), req.pathwayPackageVersion(),
            ContextSnapshotStatus.ACTIVE,
            writeJson(missing), writeJson(mappingStatus),
            quality, traceId, null, now, userId
        ));

        persistResources(snapshotId, tenantId, req.resources());

        if (idempotencyKey != null) {
            idemRepo.save(new ContextIdempotencyKey(
                null, tenantId, idempotencyKey, snapshotId,
                digest(req), now.plusSeconds(86_400), now
            ));
        }

        return new ContextSnapshotResponse(snapshotId, ContextSnapshotStatus.ACTIVE,
            quality, missing, mappingStatus, now, traceId);
    }

    @Transactional(readOnly = true)
    public ContextSnapshotResponse findById(String snapshotId) {
        String tenantId = RequestContext.currentTenantId();
        ContextSnapshot snap = snapshots.findBySnapshotIdAndTenantId(snapshotId, tenantId)
            .orElseThrow(() -> ApiException.of(ErrorCode.ENG_CONTEXT_001, "snapshot 不存在"));
        return toResponse(snap, List.of(), Map.of());
    }

    @Transactional(readOnly = true)
    public PageResponse<ContextSnapshotSummary> list(ContextSnapshotFilter filter,
                                                     com.medkernel.shared.api.PageRequest page) {
        String tenantId = RequestContext.currentTenantId();
        Sort sort = Sort.by(Sort.Direction.DESC, "created_at");
        PageRequest pageable = PageRequest.of(page.page(), page.size(), sort);

        List<ContextSnapshot> rows;
        long total;
        if (filter.patientId() != null) {
            rows = snapshots.findByTenantIdAndPatientIdOrderByCreatedAtDesc(
                tenantId, filter.patientId(), pageable);
            total = snapshots.countByTenantIdAndPatientId(tenantId, filter.patientId());
        } else if (filter.encounterId() != null) {
            rows = snapshots.findByTenantIdAndEncounterIdOrderByCreatedAtDesc(
                tenantId, filter.encounterId(), pageable);
            total = snapshots.countByTenantIdAndEncounterId(tenantId, filter.encounterId());
        } else {
            rows = List.of();
            total = 0;
        }

        List<ContextSnapshotSummary> items = rows.stream().map(s -> new ContextSnapshotSummary(
            s.snapshotId(), s.patientId(), s.encounterId(), s.status(),
            s.qualityStatus(), s.createdAt()
        )).toList();
        return new PageResponse<>(items, page.page(), page.size(), total);
    }

    // ── 私有辅助 ─────────────────────────────────────────

    private void validatePackageVersions(String tenantId, ContextSnapshotRequest req) {
        if (!versions.exists(tenantId, "knowledge", req.knowledgePackageVersion())
            || !versions.exists(tenantId, "rule", req.rulePackageVersion())
            || !versions.exists(tenantId, "pathway", req.pathwayPackageVersion())) {
            throw ApiException.of(ErrorCode.ENG_CONTEXT_002, "包版本不存在");
        }
    }

    private void persistResources(String snapshotId, String tenantId, ContextSnapshotResources r) {
        int seq = 0;
        if (r.patient() != null) {
            persistOne(snapshotId, tenantId, CanonicalResourceType.PATIENT, r.patient(), seq++);
        }
        for (CanonicalEncounter e : r.encounters()) {
            persistOne(snapshotId, tenantId, CanonicalResourceType.ENCOUNTER, e, seq++);
        }
        for (CanonicalCondition c : r.conditions()) {
            persistOne(snapshotId, tenantId, CanonicalResourceType.CONDITION, c, seq++);
        }
        for (CanonicalSymptom s : r.symptoms()) {
            persistOne(snapshotId, tenantId, CanonicalResourceType.SYMPTOM, s, seq++);
        }
        for (CanonicalObservation o : r.observations()) {
            persistOne(snapshotId, tenantId, CanonicalResourceType.OBSERVATION, o, seq++);
        }
        for (CanonicalDiagnosticReport d : r.diagnosticReports()) {
            persistOne(snapshotId, tenantId, CanonicalResourceType.DIAGNOSTIC_REPORT, d, seq++);
        }
        for (CanonicalMedication m : r.medications()) {
            persistOne(snapshotId, tenantId, CanonicalResourceType.MEDICATION, m, seq++);
        }
        for (CanonicalProcedure p : r.procedures()) {
            persistOne(snapshotId, tenantId, CanonicalResourceType.PROCEDURE, p, seq++);
        }
        for (CanonicalDocument d : r.documents()) {
            persistOne(snapshotId, tenantId, CanonicalResourceType.DOCUMENT, d, seq++);
        }
        for (CanonicalCarePlan c : r.carePlans()) {
            persistOne(snapshotId, tenantId, CanonicalResourceType.CARE_PLAN, c, seq++);
        }
        for (CanonicalFollowUp f : r.followUps()) {
            persistOne(snapshotId, tenantId, CanonicalResourceType.FOLLOW_UP, f, seq++);
        }
        for (CanonicalClaim c : r.claims()) {
            persistOne(snapshotId, tenantId, CanonicalResourceType.CLAIM, c, seq++);
        }
    }

    private void persistOne(String snapshotId, String tenantId, CanonicalResourceType type,
                            Object payload, int seq) {
        resources.save(new CanonicalResource(
            null, "res-" + UUID.randomUUID(), snapshotId, tenantId, type,
            writeJson(payload), null, null, null,
            null, Instant.now(), QualityStatus.VALID, seq
        ));
    }

    private Map<CanonicalResourceType, List<String>> summarizeForMapping(ContextSnapshotResources r) {
        Map<CanonicalResourceType, List<String>> map = new HashMap<>();
        if (r.medications() != null && !r.medications().isEmpty()) {
            map.put(CanonicalResourceType.MEDICATION,
                r.medications().stream().map(CanonicalMedication::code).toList());
        }
        if (r.conditions() != null && !r.conditions().isEmpty()) {
            map.put(CanonicalResourceType.CONDITION,
                r.conditions().stream().map(CanonicalCondition::code).toList());
        }
        if (r.observations() != null && !r.observations().isEmpty()) {
            map.put(CanonicalResourceType.OBSERVATION,
                r.observations().stream().map(CanonicalObservation::code).toList());
        }
        return map;
    }

    private ContextSnapshotResponse toResponse(ContextSnapshot snap,
            List<MissingFieldEntry> missing, Map<String, String> mappingStatus) {
        return new ContextSnapshotResponse(snap.snapshotId(), snap.status(),
            snap.qualityStatus(), missing, mappingStatus, snap.createdAt(), snap.traceId());
    }

    private String writeJson(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw ApiException.of(ErrorCode.ENG_CONTEXT_001, "JSON 序列化失败");
        }
    }

    private String digest(ContextSnapshotRequest req) {
        return Integer.toHexString(req.hashCode()); // 简化实现，后续可换 SM3
    }
}
```

> **执行者注意**：
> - `org.springframework.data.domain.PageRequest` 与 `com.medkernel.shared.api.PageRequest` 同名冲突，list() 签名用 FQN `com.medkernel.shared.api.PageRequest`，Spring 的 PageRequest 通过类内 `PageRequest.of(...)` 调用。如项目里现有 pattern 已经解决（例如 Spring PageRequest 用了别名），按现状处理。
> - `ApiException.of(...)` 和 `RequestContext.currentXxx()` / `RequestContext.runWith(...)` 按项目实际签名调整：参考 `OrgUnitService` / `KnowledgeIdentityService` 的现有 pattern。
> - `RequestContext.builder()` 若不存在，改用现状的 `RequestContextHolder.set(new RequestContext(...))` + `try/finally clear()` 写法（参考 `OrgUnitServiceTest`）。

- [ ] **Step 10.5：跑绿**

```bash
cd medkernel-backend && mvn -q -Dtest=ContextSnapshotServiceTest test
```

预期：3 个用例通过。

- [ ] **Step 10.6：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotService.java medkernel-backend/src/main/java/com/medkernel/engine/context/PackageVersionResolver.java medkernel-backend/src/test/java/com/medkernel/engine/context/ContextSnapshotServiceTest.java
git commit -m "feat(GA-ENG-API-01): ContextSnapshotService 核心业务编排

create/findById/list 三接口实现：包版本校验、schema 校验、
quality 计算、字典映射端口调用、幂等键查找与缓存命中、JSON 持久化。
单测覆盖正常创建、INVALID 拒绝、幂等命中三条主路径。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 11：ContextSnapshotController + 安全测试

**Files：**
- Create：`medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotController.java`
- Test：`medkernel-backend/src/test/java/com/medkernel/engine/context/ContextSnapshotControllerSecurityTest.java`

- [ ] **Step 11.1：先写 Controller 安全测试（red）**

参考已有的 `OrgUnitControllerSecurityTest` 与 `KnowledgeIdentityControllerSecurityTest`：

```java
// ContextSnapshotControllerSecurityTest.java
package com.medkernel.engine.context;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("dev")
class ContextSnapshotControllerSecurityTest {

    @Autowired WebApplicationContext ctx;
    @Autowired ObjectMapper json;

    private MockMvc mvc;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx)
            .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
            .build();
    }

    @Test
    void postShouldRejectWithoutAuthority() throws Exception {
        mvc.perform(post("/api/v1/engine/context/snapshots")
                .with(csrf())
                .with(jwt().jwt(b -> b.claim("tenant_id", "tenant-A").claim("sub", "u-1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void postShouldRejectWithoutTenant() throws Exception {
        mvc.perform(post("/api/v1/engine/context/snapshots")
                .with(csrf())
                .with(jwt().jwt(b -> b.claim("sub", "u-1").claim("roles", java.util.List.of("ROLE_doctor")))
                    .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERM_context.write")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ENG-BASE-001"));
    }

    @Test
    void getByIdShouldReturn404ForUnknownSnapshot() throws Exception {
        mvc.perform(get("/api/v1/engine/context/snapshots/ctx-not-exist")
                .with(jwt().jwt(b -> b.claim("tenant_id", "tenant-A").claim("sub", "u-1"))
                    .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERM_context.read"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ENG-CONTEXT-001"));
    }
}
```

> **执行者注意**：项目里 JWT 权限映射可能是 `ROLE_*` 或 `PERM_*` 或自定义。按 `KnowledgeIdentityControllerSecurityTest` 现有写法对齐。

- [ ] **Step 11.2：跑红**

```bash
cd medkernel-backend && mvn -q -Dtest=ContextSnapshotControllerSecurityTest test
```

预期：编译失败（Controller 不存在）。

- [ ] **Step 11.3：实现 Controller**

```java
// ContextSnapshotController.java
package com.medkernel.engine.context;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.medkernel.shared.api.ApiResult;
import com.medkernel.shared.api.PageRequest;
import com.medkernel.shared.api.PageResponse;
import com.medkernel.shared.datascope.DataScope;

import jakarta.validation.Valid;

/**
 * GA-ENG-API-01 标准上下文 API。
 *
 * <p>三接口：POST 创建 / GET by ID / GET 列表。
 * 类级 {@link DataScope}(requireTenant=true) 强制租户上下文；
 * 方法级 {@code @perm.has('context.*')} 控制权限。
 */
@RestController
@RequestMapping("/api/v1/engine/context/snapshots")
@DataScope(requireTenant = true)
public class ContextSnapshotController {

    private final ContextSnapshotService service;

    public ContextSnapshotController(ContextSnapshotService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("@perm.has('context.write')")
    public ResponseEntity<ApiResult<ContextSnapshotResponse>> create(
            @RequestBody @Valid ContextSnapshotRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        ContextSnapshotResponse resp = service.create(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(resp));
    }

    @GetMapping("/{snapshotId}")
    @PreAuthorize("@perm.has('context.read')")
    public ApiResult<ContextSnapshotResponse> findById(@PathVariable String snapshotId) {
        return ApiResult.ok(service.findById(snapshotId));
    }

    @GetMapping
    @PreAuthorize("@perm.has('context.read')")
    public ApiResult<PageResponse<ContextSnapshotSummary>> list(
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String encounterId,
            @RequestParam(required = false) ContextSnapshotStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        ContextSnapshotFilter filter = new ContextSnapshotFilter(patientId, encounterId, status, null, null);
        PageRequest req = new PageRequest(page, size, sort);
        return ApiResult.ok(service.list(filter, req));
    }
}
```

- [ ] **Step 11.4：跑绿**

```bash
cd medkernel-backend && mvn -q -Dtest=ContextSnapshotControllerSecurityTest test
```

预期：3 个用例通过。

- [ ] **Step 11.5：跑完整测试套件**

```bash
cd medkernel-backend && mvn test 2>&1 | grep -E "Tests run:|BUILD" | tail -5
```

预期：所有原 182 测试 + 新增测试全绿（Tests run ≥ 200，Failures: 0）。

- [ ] **Step 11.6：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotController.java medkernel-backend/src/test/java/com/medkernel/engine/context/ContextSnapshotControllerSecurityTest.java
git commit -m "feat(GA-ENG-API-01): ContextSnapshotController + 安全测试

REST 入口：POST 创建 / GET by ID / GET 列表三接口，
@perm.has('context.write'/'context.read') 控制权限，
@DataScope(requireTenant=true) 强制租户上下文。
安全测试覆盖未授权、租户缺失、不存在 snapshot 三类场景。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 12：审计接入

**Files：**
- Modify：`medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotService.java`

- [ ] **Step 12.1：参考现有审计写法**

读 `engine/knowledge/KnowledgeIdentityService.java` 或 `compliance/audit/*` 找出 audit_event 写入的统一入口（应该是 `AuditEventPublisher` 或 `AuditService.record(...)`）。

- [ ] **Step 12.2：在 ContextSnapshotService.create() 成功后追加审计**

伪代码（按现有 publisher 签名调整）：

```java
auditEventPublisher.record(
    AuditEvent.builder()
        .action("CONTEXT_CREATED")
        .resourceType("context_snapshot")
        .resourceId(snapshotId)
        .summary("创建标准上下文 quality=" + quality)
        .traceId(traceId)
        .actorUserId(userId)
        .tenantId(tenantId)
        .occurredAt(now)
        .build()
);
```

- [ ] **Step 12.3：在 findById 成功后追加 CONTEXT_READ 审计（如项目对读操作也留痕）**

如果项目惯例只对写留痕，跳过 read 审计。参考 `KnowledgeIdentityService` 是否对 read 写审计。

- [ ] **Step 12.4：跑测试**

```bash
cd medkernel-backend && mvn -q -Dtest=ContextSnapshotServiceTest,ContextSnapshotControllerSecurityTest test
```

预期：通过（Service 测试可能需要 mock audit publisher）。

- [ ] **Step 12.5：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/engine/context/ContextSnapshotService.java medkernel-backend/src/test/java/com/medkernel/engine/context/ContextSnapshotServiceTest.java
git commit -m "feat(GA-ENG-API-01): 审计接入

snapshot 创建成功后通过 AuditEventPublisher 写 audit_event：
action=CONTEXT_CREATED / resource_type=context_snapshot /
resource_id=snapshotId，traceId 全链路串联。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 13：默认权限策略接入

**Files：**
- Modify：`medkernel-backend/src/main/java/com/medkernel/engine/security/DefaultPermissionPolicy.java`（或同名类）
- Modify：`medkernel-backend/src/test/java/com/medkernel/engine/security/DefaultPermissionPolicyTest.java`

- [ ] **Step 13.1：找到现有策略表**

读 `DefaultPermissionPolicy.java`，看每个 `RoleCode` 对应的权限集合。

- [ ] **Step 13.2：把 CONTEXT_READ / CONTEXT_WRITE 加到合适的角色**

业务约定：
- `CONTEXT_READ`：医师 / 护士 / 实施 / 信息科 / 平台管理员 / 质控员 — 凡能进入临床上下文页都应能读
- `CONTEXT_WRITE`：实施 / 信息科 / 平台管理员 / 系统集成账号 — 只有数据接入流程能创建 snapshot

具体 RoleCode 映射按项目现状追加。

- [ ] **Step 13.3：扩展 DefaultPermissionPolicyTest 用例**

```java
@Test
void doctorShouldReadContext() {
    assertThat(policy.permissionsFor(RoleCode.DOCTOR)).contains(PermissionCode.CONTEXT_READ);
}

@Test
void doctorShouldNotWriteContext() {
    assertThat(policy.permissionsFor(RoleCode.DOCTOR)).doesNotContain(PermissionCode.CONTEXT_WRITE);
}

@Test
void operatorShouldWriteContext() {
    assertThat(policy.permissionsFor(RoleCode.OPERATOR)).contains(PermissionCode.CONTEXT_WRITE);
}
```

> RoleCode 实际枚举名按现状对齐。

- [ ] **Step 13.4：跑绿**

```bash
cd medkernel-backend && mvn -q -Dtest=DefaultPermissionPolicyTest test
```

预期：通过。

- [ ] **Step 13.5：commit**

```bash
git add medkernel-backend/src/main/java/com/medkernel/engine/security/DefaultPermissionPolicy.java medkernel-backend/src/test/java/com/medkernel/engine/security/DefaultPermissionPolicyTest.java
git commit -m "feat(GA-ENG-API-01): 默认权限策略加入 context.read/context.write

医师/护士/实施/信息科/平台管理员/质控员 获 context.read；
实施/信息科/平台管理员/系统集成账号 获 context.write。
DefaultPermissionPolicyTest 追加三用例覆盖典型角色映射。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 14：全门禁验证 + backlog 闭环

- [ ] **Step 14.1：跑后端完整测试**

```bash
cd medkernel-backend && mvn clean test 2>&1 | grep -E "Tests run:|BUILD" | tail -5
```

预期：`Tests run: ≥200, Failures: 0, Errors: 0, Skipped: 3, BUILD SUCCESS`。

- [ ] **Step 14.2：跑迁移契约 + H2 baseline**

```bash
cd medkernel-backend && mvn -q -Dtest=MigrationBaselineContractTest,H2BaselineMigrationTest test
```

预期：通过。

- [ ] **Step 14.3：跑前端全门禁（防止后端改动间接破坏前端）**

```bash
cd frontend && npm run lint && npm run typecheck && npm test -- --run && npm run build
```

预期：四步全过（lint 0 errors / typecheck OK / 79+ tests / build OK）。

- [ ] **Step 14.4：更新 backlog.md**

把 4.14 修订记录追加到表顶（紧贴 4.13 之前），并把 GA-ENG-API-01 行的 status 改为 done、owner 改为 claude：

```markdown
| GA-ENG-API-01 标准上下文 API：患者、就诊、诊断、医嘱、报告、组织、包版本快照 | claude | done |
```

修订记录新增行：

```markdown
| 4.14 | 2026-05-26 | Claude | GA-ENG-API-01 完成：V7 五方言迁移（context_snapshot/canonical_resource/clinical_event/context_idempotency_key）+ 12 个 Canonical Record DTO + ContextValidator/PackageVersionResolver/TerminologyMappingPort + ContextSnapshotService（含幂等）+ Controller 三接口（POST/GET by ID/GET 列表）+ PermissionCode 追加 context.read/context.write + ErrorCode 追加 ENG-CONTEXT-001..004 + 默认权限策略接入 + 审计 action=CONTEXT_CREATED。后端 ≥200 测试全绿 |
```

- [ ] **Step 14.5：commit backlog**

```bash
git add docs/backlog.md
git commit -m "chore(GA-ENG-API-01): backlog 4.14 闭环

API-01 标准上下文 API 状态置 done，记入 4.14 修订。
E2 引擎接口阶段首单完成，下一项 API-02 临床事件 API 可启动。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

- [ ] **Step 14.6：（可选）开 PR**

如果项目惯例是 PR 模式：

```bash
git push -u origin main
gh pr create --title "feat(GA-ENG-API-01): 标准上下文 API" --body "$(cat <<'EOF'
## Summary
- E2 引擎接口阶段首单：GA-ENG-API-01 标准上下文 API
- 12 个标准临床对象一次到位（Patient/Encounter/Condition/Symptom/Observation/DiagnosticReport/Medication/Procedure/Document/CarePlan/FollowUp/Claim）
- 三接口：POST 创建 / GET by ID / GET 列表
- V7 五方言迁移、PermissionCode/ErrorCode 扩展、审计接入

## Test plan
- [x] MigrationBaselineContractTest V7 用例
- [x] H2BaselineMigrationTest V1..V7
- [x] CanonicalDtoValidationTest Bean Validation
- [x] ContextSnapshotRepositoryTest 仓储数据层
- [x] ContextValidatorTest schema + quality 计算
- [x] ContextSnapshotServiceTest 创建/幂等/INVALID 拒绝
- [x] ContextSnapshotControllerSecurityTest 权限 + 租户
- [x] DefaultPermissionPolicyTest 新角色映射
- [x] 前端 lint/typecheck/test/build

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

否则直接保留在本地 main，由用户决定合并/推送时机。

---

## 实施完成

执行完 Task 1..14 后：

- ✅ V7 五方言迁移已合入主线
- ✅ 12 个 Canonical Record DTO + 完整聚合
- ✅ 三接口（POST 创建 / GET by ID / GET 列表）可用
- ✅ 权限、数据范围、审计、traceId、幂等全链路打通
- ✅ 后端 ≥200 测试全绿
- ✅ backlog.md 4.14 闭环

下一步可启动 **GA-ENG-API-02 临床事件 API**，可直接复用 `clinical_event` 表与 `ContextSnapshot` 模型。

---

**End of GA-ENG-API-01 implementation plan.**
