package com.medkernel.engine.recommendation;

/**
 * 推荐卡状态机：PENDING 待处理 / VIEWED 已查看依据 / ACCEPTED 已采纳 / REJECTED 不采纳 /
 * DEFERRED 稍后处理 / DISMISSED 关闭忽略 / SUPPRESSED 疲劳治理抑制 / EXPIRED 过期失效。
 *
 * <p>反馈接口按 {@link RecommendationFeedbackType} 推进状态；终止态（ACCEPTED/REJECTED/DISMISSED/SUPPRESSED/EXPIRED）
 * 不允许再次反馈，由 {@link RecommendationEngineService#feedback} 校验。
 */
public enum RecommendationCardStatus {
    PENDING,
    VIEWED,
    ACCEPTED,
    REJECTED,
    DEFERRED,
    DISMISSED,
    SUPPRESSED,
    EXPIRED
}
