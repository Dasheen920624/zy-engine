package com.medkernel.engine.recommendation;

/**
 * 疲劳治理信号列表过滤条件：按 fatigueKey 与信号类型过滤；空字段表示不过滤。
 */
public record RecommendationFatigueSignalFilter(
    String fatigueKey,
    RecommendationFatigueSignalType signalType
) {}
