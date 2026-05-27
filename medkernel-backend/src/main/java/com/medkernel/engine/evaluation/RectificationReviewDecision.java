package com.medkernel.engine.evaluation;

/**
 * 整改复核结论枚举。
 *
 * <p>取值含义：{@code APPROVED} 通过关闭、{@code RETURNED} 退回继续整改、{@code WAIVED} 豁免关闭。
 */
public enum RectificationReviewDecision {
    APPROVED,
    RETURNED,
    WAIVED
}
