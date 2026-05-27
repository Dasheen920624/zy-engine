package com.medkernel.engine.recommendation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecommendationSourceRequest(
    @NotNull RecommendationSourceType sourceType,
    String sourceRefId,
    String sourceVersion,
    @NotBlank String sourceTitle,
    String citationLocator,
    String sourceHash,
    String summary
) {}
