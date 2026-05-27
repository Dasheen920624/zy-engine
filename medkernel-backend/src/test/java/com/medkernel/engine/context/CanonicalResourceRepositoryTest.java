package com.medkernel.engine.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.test.context.TestPropertySource;

/**
 * CanonicalResource 仓储 trace_id 持久化 / 反查测试。
 *
 * <p>验证 V8 已加 trace_id 列在 entity 映射后能正确持久化与按 trace_id 查询，
 * 支撑第三层 trace 横向反查所需的 canonical_resource 视角。
 */
@DataJdbcTest
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:canonical-trace-${random.uuid};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration/h2"
})
class CanonicalResourceRepositoryTest {

    @Autowired CanonicalResourceRepository repository;

    @AfterEach
    void wipe() {
        repository.deleteAll();
    }

    @Test
    void persistsTraceIdAndReadsBack() {
        String snapshotId = "ctx-trace-" + UUID.randomUUID();
        String traceId = "trace-canonical-" + UUID.randomUUID();
        Instant now = Instant.now();

        CanonicalResource saved = repository.save(new CanonicalResource(
            null, "res-trace-1", snapshotId, "tenant-A",
            CanonicalResourceType.PATIENT, "{}", "src", "sr-1", null,
            null, now, QualityStatus.VALID, 0, traceId
        ));
        assertThat(saved.id()).isNotNull();
        assertThat(saved.traceId()).isEqualTo(traceId);

        List<CanonicalResource> rows = repository.findByTraceIdOrderBySeqNoAsc(traceId);
        assertThat(rows).extracting(CanonicalResource::resourceId).containsExactly("res-trace-1");
    }

    @Test
    void findByTraceIdOrdersBySeqNoAsc() {
        String snapshotId = "ctx-multi-" + UUID.randomUUID();
        String traceId = "trace-multi-" + UUID.randomUUID();
        Instant now = Instant.now();

        repository.save(new CanonicalResource(
            null, "res-2", snapshotId, "tenant-A",
            CanonicalResourceType.MEDICATION, "{}", "src", "sr-2", null,
            null, now, QualityStatus.VALID, 2, traceId
        ));
        repository.save(new CanonicalResource(
            null, "res-0", snapshotId, "tenant-A",
            CanonicalResourceType.PATIENT, "{}", "src", "sr-0", null,
            null, now, QualityStatus.VALID, 0, traceId
        ));
        repository.save(new CanonicalResource(
            null, "res-1", snapshotId, "tenant-A",
            CanonicalResourceType.ENCOUNTER, "{}", "src", "sr-1", null,
            null, now, QualityStatus.VALID, 1, traceId
        ));

        List<CanonicalResource> rows = repository.findByTraceIdOrderBySeqNoAsc(traceId);
        assertThat(rows).extracting(CanonicalResource::resourceId)
            .containsExactly("res-0", "res-1", "res-2");
    }

    @Test
    void findByTraceIdEmptyWhenNoMatch() {
        List<CanonicalResource> rows = repository.findByTraceIdOrderBySeqNoAsc("trace-no-match");
        assertThat(rows).isEmpty();
    }
}
