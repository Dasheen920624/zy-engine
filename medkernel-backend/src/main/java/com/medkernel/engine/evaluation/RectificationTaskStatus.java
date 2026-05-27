package com.medkernel.engine.evaluation;

/**
 * 整改任务状态枚举。
 *
 * <p>取值含义：{@code ASSIGNED} 已派发、{@code SUBMITTED} 已提交、
 * {@code RETURNED} 已退回、{@code CLOSED} 已关闭、{@code WAIVED} 已豁免。
 */
public enum RectificationTaskStatus {
    ASSIGNED,
    SUBMITTED,
    RETURNED,
    CLOSED,
    WAIVED
}
