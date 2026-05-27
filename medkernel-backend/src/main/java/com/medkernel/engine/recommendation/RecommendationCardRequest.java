package com.medkernel.engine.recommendation;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecommendationCardRequest(
    @NotBlank String cardCode,
    @NotNull RecommendationCardType cardType,
    @NotBlank String title,
    @NotBlank String summary,
    @NotBlank String suggestedAction,
    @NotNull RecommendationRiskLevel riskLevel,
    @NotNull RecommendationInterruptLevel interruptLevel,
    boolean requiresPhysicianConfirmation,
    boolean aiGenerated,
    @NotBlank String sourceSummary,
    String explanationJson,
    String fatigueKey,
    Instant expiresAt,
    @Valid List<RecommendationSourceRequest> sources
) {
    public RecommendationCardRequest {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
