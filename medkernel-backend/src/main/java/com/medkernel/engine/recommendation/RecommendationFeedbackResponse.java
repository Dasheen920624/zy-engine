package com.medkernel.engine.recommendation;

/**
 * 医师反馈出参：返回 feedbackId、cardId、推进后的卡状态和 traceId，字段语义见 API spec。
 */
public record RecommendationFeedbackResponse(
    String feedbackId,
    String cardId,
    RecommendationCardStatus cardStatus,
    String traceId
) {}
