package com.medkernel.shared.audit.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import com.medkernel.shared.audit.AuditAction;
import com.medkernel.shared.audit.AuditEvent;
import com.medkernel.shared.observability.BusinessMetrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Sink 失败兜底测试。
 *
 * <p>当 {@link AuditChainWriter#persist} 抛错时：
 * <ul>
 *   <li>异常被吞，不向调用方传播（业务主链路不受影响）</li>
 *   <li>{@code medkernel_audit_persistence_failures_total} 计数器递增</li>
 *   <li>验签成功计数器 {@code medkernel_audit_chain_signed_total} 不增长</li>
 * </ul>
 */
class AuditPersistenceSinkTest {

    @Test
    void persistenceFailureIsSwallowedAndCountsAsFailure() {
        AuditChainWriter writer = mock(AuditChainWriter.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BusinessMetrics metrics = new BusinessMetrics(registry);
        metrics.register();

        doThrow(new DataAccessResourceFailureException("db down"))
            .when(writer).persist(org.mockito.ArgumentMatchers.any(AuditEvent.class));

        AuditPersistenceSink sink = new AuditPersistenceSink(writer, metrics);

        AuditEvent event = AuditEvent.of(AuditAction.CREATE, "rule", "r-1", "test");

        assertThatCode(() -> sink.onNoTransaction(event)).doesNotThrowAnyException();

        verify(writer, times(1)).persist(event);
        assertThat(registry.counter("medkernel_audit_persistence_failures_total").count())
            .isEqualTo(1.0);
        assertThat(registry.counter("medkernel_audit_chain_signed_total").count())
            .isEqualTo(0.0);
    }

    @Test
    void successfulPersistenceIncrementsSignedCounter() {
        AuditChainWriter writer = mock(AuditChainWriter.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BusinessMetrics metrics = new BusinessMetrics(registry);
        metrics.register();

        // mocked writer returns null by default — sink only inspects metrics
        AuditPersistenceSink sink = new AuditPersistenceSink(writer, metrics);
        AuditEvent event = AuditEvent.of(AuditAction.PUBLISH, "rule", "r-2", "test");
        sink.onNoTransaction(event);

        assertThat(registry.counter("medkernel_audit_chain_signed_total").count())
            .isEqualTo(1.0);
        assertThat(registry.counter("medkernel_audit_persistence_failures_total").count())
            .isEqualTo(0.0);
    }
}
