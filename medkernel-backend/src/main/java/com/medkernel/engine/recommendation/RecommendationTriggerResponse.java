package com.medkernel.engine.recommendation;

public record RecommendationTriggerResponse(
    String triggerId,
    RecommendationTriggerStatus status,
    int cardCount,
    String traceId
) {}
