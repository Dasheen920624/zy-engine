package com.medkernel.engine.recommendation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.medkernel.shared.api.PageRequest;
import com.medkernel.shared.api.PageResponse;
import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;
import com.medkernel.shared.audit.AuditAction;
import com.medkernel.shared.audit.AuditEventPublisher;
import com.medkernel.shared.context.RequestContext;
import com.medkernel.shared.observability.DiagnoseResponse;
import com.medkernel.shared.observability.DiagnoseResponseAssembler;
import com.medkernel.shared.observability.StateTransitionRecorder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationEngineService {

    private final RecommendationTriggerRepository triggers;
    private final RecommendationCardRepository cards;
    private final RecommendationSourceRepository sources;
    private final RecommendationFeedbackRepository feedback;
    private final RecommendationFatigueSignalRepository fatigueSignals;
    private final AuditEventPublisher auditPublisher;
    private final StateTransitionRecorder transitions;
    private final DiagnoseResponseAssembler diagnoseAssembler;

    public RecommendationEngineService(
            RecommendationTriggerRepository triggers,
            RecommendationCardRepository cards,
            RecommendationSourceRepository sources,
            RecommendationFeedbackRepository feedback,
            RecommendationFatigueSignalRepository fatigueSignals,
            AuditEventPublisher auditPublisher,
            StateTransitionRecorder transitions,
            DiagnoseResponseAssembler diagnoseAssembler) {
        this.triggers = triggers;
        this.cards = cards;
        this.sources = sources;
        this.feedback = feedback;
        this.fatigueSignals = fatigueSignals;
        this.auditPublisher = auditPublisher;
        this.transitions = transitions;
        this.diagnoseAssembler = diagnoseAssembler;
    }

    @Transactional
    public RecommendationTriggerResponse trigger(RecommendationTriggerRequest request) {
        validateCards(request.candidateCards());
        String tenantId = tenantId();
        String actor = actor();
        String traceId = traceId();
        Instant now = Instant.now();
        String triggerId = "rt-" + UUID.randomUUID();
        RecommendationTriggerStatus status = request.candidateCards().isEmpty()
            ? RecommendationTriggerStatus.NO_CARD
            : RecommendationTriggerStatus.EVALUATED;

        RecommendationTrigger trigger = triggers.save(new RecommendationTrigger(
            null, triggerId, tenantId, request.triggerCode(), request.triggerType(),
            request.sourceEventId(), request.contextSnapshotId(), request.patientId(), request.encounterId(),
            request.patientPathwayId(), request.scenarioCode(), request.packageVersion(), request.inputDigest(),
            status, null, request.occurredAt() == null ? now : request.occurredAt(),
            now, actor, now, actor, traceId));

        for (RecommendationCardRequest cardRequest : request.candidateCards()) {
            RecommendationCard card = saveCard(trigger, cardRequest, now, actor, traceId);
            for (RecommendationSourceRequest sourceRequest : cardRequest.sources()) {
                saveSource(card.cardId(), sourceRequest, now, actor, traceId);
            }
            saveFatigueSignal(trigger, card, initialSignal(cardRequest), null, now, actor, traceId);
        }

        transitions.record("recommendation_trigger", triggerId, null, status.name(), "接收推荐触发", null);
        auditPublisher.publish(AuditAction.EXECUTE, "recommendation_trigger", triggerId,
            "接收推荐触发 " + request.triggerCode());
        return new RecommendationTriggerResponse(triggerId, status, request.candidateCards().size(), traceId);
    }

    @Transactional(readOnly = true)
    public PageResponse<RecommendationCard> listCards(RecommendationCardFilter filter, PageRequest pageRequest) {
        PageRequest req = pageRequest == null ? PageRequest.defaults() : pageRequest;
        RecommendationCardFilter f = filter == null ? new RecommendationCardFilter(null, null, null, null) : filter;
        String status = f.status() == null ? null : f.status().name();
        String risk = f.riskLevel() == null ? null : f.riskLevel().name();
        long total = cards.countByFilter(tenantId(), status, risk, f.scenarioCode(), f.patientId());
        List<RecommendationCard> rows = cards.pageByFilter(
            tenantId(), status, risk, f.scenarioCode(), f.patientId(), req.offset(), req.safeSize());
        return PageResponse.of(rows, req, total);
    }

    @Transactional(readOnly = true)
    public RecommendationCardDetailResponse cardDetail(String cardId) {
        RecommendationCard card = findCard(cardId);
        return new RecommendationCardDetailResponse(
            card,
            sources.findByCardIdAndTenantIdOrderByCreatedAtAsc(cardId, tenantId()),
            feedback.findByCardIdAndTenantIdOrderByCreatedAtAsc(cardId, tenantId()),
            fatigueSignals.findByCardIdAndTenantIdOrderByCreatedAtAsc(cardId, tenantId())
        );
    }

    @Transactional(readOnly = true)
    public List<RecommendationSource> sources(String cardId) {
        findCard(cardId);
        return sources.findByCardIdAndTenantIdOrderByCreatedAtAsc(cardId, tenantId());
    }

    @Transactional
    public RecommendationFeedbackResponse feedback(String cardId, RecommendationFeedbackRequest request) {
        RecommendationCard card = findCard(cardId);
        if (isClosed(card) || isExpired(card)) {
            throw new ApiException(ErrorCode.ENG_REC_004);
        }

        String tenantId = tenantId();
        String actor = actor();
        String traceId = traceId();
        Instant now = Instant.now();
        RecommendationCardStatus nextStatus = nextStatus(request.feedbackType());
        RecommendationCard savedCard = cards.save(rewriteStatus(card, nextStatus, now, actor));
        String feedbackId = "rf-" + UUID.randomUUID();
        feedback.save(new RecommendationFeedback(
            null, feedbackId, tenantId, cardId, request.feedbackType(), request.reasonCode(),
            request.reasonText(), actor, request.operatorRole(), now, actor, now, actor, traceId));

        RecommendationTrigger trigger = triggers.findByTriggerIdAndTenantId(card.triggerId(), tenantId).orElse(null);
        saveFatigueSignal(trigger, savedCard, feedbackSignal(request.feedbackType()), actor, now, actor, traceId);
        transitions.record("recommendation_card", cardId, card.status().name(), nextStatus.name(),
            "推荐反馈 " + request.feedbackType(), null);
        auditPublisher.publish(AuditAction.FEEDBACK, "recommendation_card", cardId,
            "推荐卡反馈 " + request.feedbackType());
        return new RecommendationFeedbackResponse(feedbackId, cardId, nextStatus, traceId);
    }

    @Transactional(readOnly = true)
    public PageResponse<RecommendationFatigueSignal> fatigueSignals(
            RecommendationFatigueSignalFilter filter, PageRequest pageRequest) {
        PageRequest req = pageRequest == null ? PageRequest.defaults() : pageRequest;
        RecommendationFatigueSignalFilter f = filter == null
            ? new RecommendationFatigueSignalFilter(null, null)
            : filter;
        String signalType = f.signalType() == null ? null : f.signalType().name();
        List<RecommendationFatigueSignal> rows =
            fatigueSignals.pageByFilter(tenantId(), f.fatigueKey(), signalType, req.offset(), req.safeSize());
        boolean hasNext = rows.size() == req.safeSize();
        long estimated = (long) req.offset() + rows.size() + (hasNext ? 1 : 0);
        return PageResponse.ofEstimated(rows, req, estimated, hasNext);
    }

    @Transactional(readOnly = true)
    public DiagnoseResponse diagnose(String triggerId) {
        RecommendationTrigger trigger = triggers.findByTriggerIdAndTenantId(triggerId, tenantId())
            .orElseThrow(() -> new ApiException(ErrorCode.ENG_REC_002));
        List<RecommendationCard> triggerCards =
            cards.findByTriggerIdAndTenantIdOrderByCreatedAtAsc(triggerId, tenantId());
        List<RecommendationFeedback> allFeedback = new ArrayList<>();
        for (RecommendationCard card : triggerCards) {
            allFeedback.addAll(feedback.findByCardIdAndTenantIdOrderByCreatedAtAsc(card.cardId(), tenantId()));
        }
        List<RecommendationFatigueSignal> signals =
            fatigueSignals.findByTriggerIdAndTenantIdOrderByCreatedAtAsc(triggerId, tenantId());
        Map<String, List<String>> related = Map.of(
            "cards", triggerCards.stream().map(RecommendationCard::cardId).toList(),
            "feedback", allFeedback.stream().map(RecommendationFeedback::feedbackId).toList(),
            "fatigueSignals", signals.stream().map(RecommendationFatigueSignal::signalId).toList()
        );
        return diagnoseAssembler.assemble(
            "recommendation_trigger", triggerId, tenantId(), trigger.status().name(),
            trigger, List.of(), related, null,
            trigger.traceId() == null ? traceId() : trigger.traceId());
    }

    private void validateCards(List<RecommendationCardRequest> cardRequests) {
        for (RecommendationCardRequest card : cardRequests) {
            if (card.sources().isEmpty()) {
                throw new ApiException(ErrorCode.ENG_REC_005);
            }
            if (isHighRisk(card.riskLevel()) && !card.requiresPhysicianConfirmation()) {
                throw new ApiException(ErrorCode.ENG_REC_006);
            }
            if (card.interruptLevel() == RecommendationInterruptLevel.STRONG_INTERRUPTIVE
                    && !isHighRisk(card.riskLevel())) {
                throw new ApiException(ErrorCode.ENG_REC_001, "强打断推荐必须是高风险或红线风险");
            }
        }
    }

    private RecommendationCard saveCard(RecommendationTrigger trigger, RecommendationCardRequest request,
                                        Instant now, String actor, String traceId) {
        return cards.save(new RecommendationCard(
            null, "rc-" + UUID.randomUUID(), trigger.tenantId(), trigger.triggerId(), request.cardCode(),
            request.cardType(), request.title(), request.summary(), request.suggestedAction(),
            request.riskLevel(), request.interruptLevel(), RecommendationCardStatus.PENDING,
            request.requiresPhysicianConfirmation(), request.aiGenerated(), request.sourceSummary(),
            request.explanationJson(), request.fatigueKey(), request.expiresAt(),
            now, actor, now, actor, traceId));
    }

    private RecommendationSource saveSource(String cardId, RecommendationSourceRequest request,
                                            Instant now, String actor, String traceId) {
        return sources.save(new RecommendationSource(
            null, "rs-" + UUID.randomUUID(), tenantId(), cardId, request.sourceType(),
            request.sourceRefId(), request.sourceVersion(), request.sourceTitle(), request.citationLocator(),
            request.sourceHash(), request.summary(), now, actor, now, actor, traceId));
    }

    private void saveFatigueSignal(RecommendationTrigger trigger, RecommendationCard card,
                                   RecommendationFatigueSignalType signalType, String operatorId,
                                   Instant now, String actor, String traceId) {
        fatigueSignals.save(new RecommendationFatigueSignal(
            null, "rfs-" + UUID.randomUUID(), tenantId(),
            trigger == null ? card.triggerId() : trigger.triggerId(),
            card.cardId(), card.fatigueKey(),
            trigger == null ? null : trigger.patientId(),
            trigger == null ? null : trigger.encounterId(),
            operatorId, signalType, 1, now, now, actor, now, actor, traceId));
    }

    private RecommendationCard findCard(String cardId) {
        return cards.findByCardIdAndTenantId(cardId, tenantId())
            .orElseThrow(() -> new ApiException(ErrorCode.ENG_REC_003));
    }

    private RecommendationCard rewriteStatus(RecommendationCard card, RecommendationCardStatus status,
                                             Instant now, String actor) {
        return new RecommendationCard(
            card.id(), card.cardId(), card.tenantId(), card.triggerId(), card.cardCode(), card.cardType(),
            card.title(), card.summary(), card.suggestedAction(), card.riskLevel(), card.interruptLevel(),
            status, card.requiresPhysicianConfirmation(), card.aiGenerated(), card.sourceSummary(),
            card.explanationJson(), card.fatigueKey(), card.expiresAt(),
            card.createdAt(), card.createdBy(), now, actor, card.traceId());
    }

    private RecommendationCardStatus nextStatus(RecommendationFeedbackType feedbackType) {
        return switch (feedbackType) {
            case VIEW_SOURCE -> RecommendationCardStatus.VIEWED;
            case ACCEPT -> RecommendationCardStatus.ACCEPTED;
            case REJECT -> RecommendationCardStatus.REJECTED;
            case DEFER -> RecommendationCardStatus.DEFERRED;
            case DISMISS -> RecommendationCardStatus.DISMISSED;
        };
    }

    private RecommendationFatigueSignalType initialSignal(RecommendationCardRequest request) {
        return request.interruptLevel() == RecommendationInterruptLevel.SILENT
            ? RecommendationFatigueSignalType.SILENT_RECORDED
            : RecommendationFatigueSignalType.SHOWN;
    }

    private RecommendationFatigueSignalType feedbackSignal(RecommendationFeedbackType feedbackType) {
        return switch (feedbackType) {
            case VIEW_SOURCE -> RecommendationFatigueSignalType.VIEWED;
            case ACCEPT -> RecommendationFatigueSignalType.ACCEPTED;
            case REJECT -> RecommendationFatigueSignalType.REJECTED;
            case DEFER -> RecommendationFatigueSignalType.DEFERRED;
            case DISMISS -> RecommendationFatigueSignalType.DISMISSED;
        };
    }

    private boolean isHighRisk(RecommendationRiskLevel riskLevel) {
        return riskLevel == RecommendationRiskLevel.HIGH || riskLevel == RecommendationRiskLevel.CRITICAL;
    }

    private boolean isClosed(RecommendationCard card) {
        return card.status() == RecommendationCardStatus.ACCEPTED
            || card.status() == RecommendationCardStatus.REJECTED
            || card.status() == RecommendationCardStatus.DISMISSED
            || card.status() == RecommendationCardStatus.SUPPRESSED
            || card.status() == RecommendationCardStatus.EXPIRED;
    }

    private boolean isExpired(RecommendationCard card) {
        return card.expiresAt() != null && card.expiresAt().isBefore(Instant.now());
    }

    private String tenantId() {
        String tenantId = RequestContext.currentOrgScope().tenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw ApiException.tenantMissing();
        }
        return tenantId;
    }

    private String actor() {
        return RequestContext.currentUserId().orElse("system");
    }

    private String traceId() {
        String traceId = RequestContext.currentTraceId();
        return traceId == null ? RequestContext.snapshot().traceId() : traceId;
    }
}
