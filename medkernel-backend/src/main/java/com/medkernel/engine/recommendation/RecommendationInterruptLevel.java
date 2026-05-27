package com.medkernel.engine.recommendation;

/**
 * 推荐卡打扰级别：SILENT 静默（仅记录，不展示）/ INFO 信息提示 /
 * WEAK_INTERRUPTIVE 弱打断 / STRONG_INTERRUPTIVE 强打断。
 *
 * <p>强打断必须配合高风险/红线风险卡，由 {@link RecommendationEngineService#trigger} 校验。
 */
public enum RecommendationInterruptLevel {
    SILENT,
    INFO,
    WEAK_INTERRUPTIVE,
    STRONG_INTERRUPTIVE
}
