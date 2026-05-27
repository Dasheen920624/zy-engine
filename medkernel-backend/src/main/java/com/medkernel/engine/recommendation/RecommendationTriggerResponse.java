package com.medkernel.engine.recommendation;

/**
 * 推荐触发出参：返回 triggerId、触发状态、本次落库的推荐卡数量和 traceId，字段语义见 API spec。
 */
public record RecommendationTriggerResponse(
    String triggerId,
    RecommendationTriggerStatus status,
    int cardCount,
    String traceId
) {}
