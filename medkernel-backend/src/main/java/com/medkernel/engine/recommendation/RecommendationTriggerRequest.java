package com.medkernel.engine.recommendation;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

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
