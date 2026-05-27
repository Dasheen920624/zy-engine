package com.medkernel.engine.recommendation;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * 推荐触发入参：triggerCode / triggerType / scenarioCode / inputDigest 必填，
 * 可携带候选 {@link RecommendationCardRequest} 列表（首版允许上游直接提交候选卡）。
 */
public record RecommendationTriggerRequest(
    @NotBlank String triggerCode,
    @NotBlank String triggerType,
    String sourceEventId,
    String contextSnapshotId,
    String patientId,
    String encounterId,
    String patientPathwayId,
    @NotBlank String scenarioCode,
    String packageVersion,
    @NotBlank String inputDigest,
    Instant occurredAt,
    @Valid List<RecommendationCardRequest> candidateCards
) {
    public RecommendationTriggerRequest {
        candidateCards = candidateCards == null ? List.of() : List.copyOf(candidateCards);
    }
}
