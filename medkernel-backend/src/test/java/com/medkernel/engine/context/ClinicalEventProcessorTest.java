package com.medkernel.engine.context;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;
import com.medkernel.shared.audit.AuditAction;
import com.medkernel.shared.audit.AuditEventPublisher;
import com.medkernel.shared.observability.StateTransitionRecorder;

class ClinicalEventProcessorTest {

    private ClinicalEventRepository events;
    private ClinicalEventPayloadRepository payloads;
    private AuditEventPublisher auditPublisher;
    private StateTransitionRecorder transitions;
    private ApplicationEventPublisher applicationEvents;
    private ClinicalEventProcessor processor;

    @BeforeEach
    void setUp() {
        events = mock(ClinicalEventRepository.class);
        payloads = mock(ClinicalEventPayloadRepository.class);
        auditPublisher = mock(AuditEventPublisher.class);
        transitions = mock(StateTransitionRecorder.class);
        applicationEvents = mock(ApplicationEventPublisher.class);
        processor = new ClinicalEventProcessor(events, payloads, auditPublisher, transitions, applicationEvents);
        when(events.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void processMovesReceivedEventToMappedThenProcessed() {
        ClinicalEvent event = event(ClinicalEventStatus.RECEIVED);
        when(events.findByEventIdAndTenantId("evt-1", "tenant-A")).thenReturn(Optional.of(event));
        when(payloads.findByEventIdAndTenantId("evt-1", "tenant-A")).thenReturn(Optional.of(payload()));

        processor.process("evt-1", "tenant-A");

        ArgumentCaptor<ClinicalEvent> eventCap = ArgumentCaptor.forClass(ClinicalEvent.class);
        verify(events, org.mockito.Mockito.times(2)).save(eventCap.capture());
        org.assertj.core.api.Assertions.assertThat(eventCap.getAllValues())
            .extracting(ClinicalEvent::processingStatus)
            .containsExactly(ClinicalEventStatus.MAPPED, ClinicalEventStatus.PROCESSED);
        verify(transitions).record("clinical_event", "evt-1",
            "RECEIVED", "MAPPED", "TERMINOLOGY_OK", null);
        verify(transitions).record("clinical_event", "evt-1",
            "MAPPED", "PROCESSED", "RULES_OK", null);
        verify(auditPublisher).publish(AuditAction.EXECUTE, "clinical_event", "evt-1",
            "处理临床事件成功 type=DIAGNOSIS");
        verify(applicationEvents).publishEvent(any(ClinicalEventProcessedEvent.class));
    }

    @Test
    void processFailsWhenPayloadMissing() {
        when(events.findByEventIdAndTenantId("evt-1", "tenant-A")).thenReturn(Optional.of(event(ClinicalEventStatus.RECEIVED)));
        when(payloads.findByEventIdAndTenantId("evt-1", "tenant-A")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processor.process("evt-1", "tenant-A"))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENG_OBS_001);
    }

    private ClinicalEvent event(ClinicalEventStatus status) {
        return new ClinicalEvent(
            1L, "evt-1", "tenant-A", ClinicalEventType.DIAGNOSIS,
            "MPI-1", "ENC-1", "HIS", "kpv-1", "digest",
            Instant.parse("2026-05-27T01:00:00Z"), Instant.parse("2026-05-27T01:00:01Z"),
            null, status, null, null, 0, null, "trace-1");
    }

    private ClinicalEventPayload payload() {
        return new ClinicalEventPayload(
            1L, "evt-1", "tenant-A", "{\"a\":1}", null,
            "INLINE", "application/json", "digest", 7L, Instant.now(), null);
    }
}
