package com.medkernel.engine.recommendation;

public record RecommendationFatigueSignalFilter(
    String fatigueKey,
    RecommendationFatigueSignalType signalType
) {}
