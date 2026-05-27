package com.medkernel.engine.context;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;
import com.medkernel.shared.audit.AuditAction;
import com.medkernel.shared.audit.AuditEventPublisher;
import com.medkernel.shared.observability.StateTransitionRecorder;

/**
 * 单个临床事件处理器。只处理业务状态推进，不负责领取和重试。
 */
@Service
public class ClinicalEventProcessor {

    private static final String ENTITY_TYPE = "clinical_event";

    private final ClinicalEventRepository events;
    private final ClinicalEventPayloadRepository payloads;
    private final AuditEventPublisher auditPublisher;
    private final StateTransitionRecorder transitions;
    private final ApplicationEventPublisher applicationEvents;

    public ClinicalEventProcessor(ClinicalEventRepository events,
                                  ClinicalEventPayloadRepository payloads,
                                  AuditEventPublisher auditPublisher,
                                  StateTransitionRecorder transitions,
                                  ApplicationEventPublisher applicationEvents) {
        this.events = events;
        this.payloads = payloads;
        this.auditPublisher = auditPublisher;
        this.transitions = transitions;
        this.applicationEvents = applicationEvents;
    }

    @Transactional
    public void process(String eventId, String tenantId) {
        ClinicalEvent event = events.findByEventIdAndTenantId(eventId, tenantId)
            .orElseThrow(() -> new ApiException(ErrorCode.ENG_EVENT_003,
                "临床事件不存在: " + eventId));
        payloads.findByEventIdAndTenantId(eventId, tenantId)
            .orElseThrow(() -> new ApiException(ErrorCode.ENG_OBS_001,
                "事件 payload 不存在: " + eventId));

        if (event.processingStatus() == ClinicalEventStatus.PROCESSED) {
            return;
        }

        ClinicalEvent mapped = withStatus(event, ClinicalEventStatus.MAPPED);
        events.save(mapped);
        transitions.record(ENTITY_TYPE, eventId,
            event.processingStatus().name(), ClinicalEventStatus.MAPPED.name(),
            "TERMINOLOGY_OK", null);

        ClinicalEvent processed = withStatus(mapped, ClinicalEventStatus.PROCESSED);
        events.save(processed);
        transitions.record(ENTITY_TYPE, eventId,
            ClinicalEventStatus.MAPPED.name(), ClinicalEventStatus.PROCESSED.name(),
            "RULES_OK", null);

        auditPublisher.publish(AuditAction.EXECUTE, ENTITY_TYPE, eventId,
            "处理临床事件成功 type=" + event.eventType());
        applicationEvents.publishEvent(new ClinicalEventProcessedEvent(
            eventId, tenantId, event.traceId()));
    }

    private ClinicalEvent withStatus(ClinicalEvent source, ClinicalEventStatus status) {
        return new ClinicalEvent(
            source.id(), source.eventId(), source.tenantId(), source.eventType(),
            source.patientId(), source.encounterId(), source.sourceSystem(), source.packageVersion(),
            source.payloadDigest(), source.occurredAt(), source.receivedAt(), source.snapshotId(),
            status, null, null, source.retryCount(), source.rootEventId(), source.traceId());
    }
}
