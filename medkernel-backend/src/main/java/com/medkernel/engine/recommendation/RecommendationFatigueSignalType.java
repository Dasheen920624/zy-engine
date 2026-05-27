package com.medkernel.engine.recommendation;

/**
 * 疲劳治理信号类型：SHOWN 已展示 / SILENT_RECORDED 静默试运行 / VIEWED 用户查看 /
 * ACCEPTED 用户采纳 / REJECTED 用户不采纳 / DEFERRED 稍后处理 / DISMISSED 关闭忽略。
 *
 * <p>首版仅做事实采集，不做自动屏蔽；后续 {@code GA-ENG-CDSS-01} 疲劳治理引擎按
 * {@code fatigue_key} 与信号分布计算抑制策略。
 */
public enum RecommendationFatigueSignalType {
    SHOWN,
    SILENT_RECORDED,
    VIEWED,
    ACCEPTED,
    REJECTED,
    DEFERRED,
    DISMISSED
}
