package com.medkernel.engine.recommendation;

/**
 * 医师反馈类型：VIEW_SOURCE 查看依据 / ACCEPT 采纳 / REJECT 不采纳 / DEFER 稍后处理 / DISMISS 关闭忽略。
 *
 * <p>每种反馈对应推荐卡状态推进，由 {@link RecommendationEngineService#feedback} 维护映射。
 */
public enum RecommendationFeedbackType {
    VIEW_SOURCE,
    ACCEPT,
    REJECT,
    DEFER,
    DISMISS
}
