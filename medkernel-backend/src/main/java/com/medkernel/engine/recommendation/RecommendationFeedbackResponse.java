package com.medkernel.engine.recommendation;

public record RecommendationFeedbackResponse(
    String feedbackId,
    String cardId,
    RecommendationCardStatus cardStatus,
    String traceId
) {}
