package com.medkernel.engine.recommendation;

import java.util.List;

public record RecommendationCardDetailResponse(
    RecommendationCard card,
    List<RecommendationSource> sources,
    List<RecommendationFeedback> feedback,
    List<RecommendationFatigueSignal> fatigueSignals
) {
    public RecommendationCardDetailResponse {
        sources = sources == null ? List.of() : List.copyOf(sources);
        feedback = feedback == null ? List.of() : List.copyOf(feedback);
        fatigueSignals = fatigueSignals == null ? List.of() : List.copyOf(fatigueSignals);
    }
}
