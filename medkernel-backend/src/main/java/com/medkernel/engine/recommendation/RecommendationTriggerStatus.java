package com.medkernel.engine.recommendation;

/**
 * 推荐触发状态：RECEIVED 已接收 / EVALUATED 已生成推荐卡 / NO_CARD 触发已记录但本次无卡 / FAILED 触发处理失败。
 */
public enum RecommendationTriggerStatus {
    RECEIVED,
    EVALUATED,
    NO_CARD,
    FAILED
}
