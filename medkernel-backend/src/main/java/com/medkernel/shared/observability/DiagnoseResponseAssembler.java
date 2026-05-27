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
