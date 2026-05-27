package com.medkernel.engine.recommendation;

/**
 * 推荐卡风险级别：LOW 低 / MEDIUM 中 / HIGH 高 / CRITICAL 红线。
 *
 * <p>HIGH 与 CRITICAL 视为高风险，必须 {@code requires_physician_confirmation=true}，
 * 由 {@link RecommendationEngineService#trigger} 校验。
 */
public enum RecommendationRiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
