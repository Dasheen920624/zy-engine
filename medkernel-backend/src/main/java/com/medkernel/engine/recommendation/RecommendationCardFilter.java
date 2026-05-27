package com.medkernel.engine.recommendation;

public record RecommendationCardFilter(
    RecommendationCardStatus status,
    RecommendationRiskLevel riskLevel,
    String scenarioCode,
    String patientId
) {}
