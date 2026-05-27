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
