package com.medkernel.engine.recommendation;

import jakarta.validation.constraints.NotNull;

public record RecommendationFeedbackRequest(
    @NotNull RecommendationFeedbackType feedbackType,
    String reasonCode,
    String reasonText,
    String operatorRole
) {}
