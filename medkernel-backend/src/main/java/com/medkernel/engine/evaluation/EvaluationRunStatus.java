package com.medkernel.engine.evaluation;

/**
 * 评估运行状态枚举。
 *
 * <p>取值含义：{@code RECEIVED} 已接收、{@code RECORDED} 已完整记录、{@code FAILED} 入库失败。
 */
public enum EvaluationRunStatus {
    RECEIVED,
    RECORDED,
    FAILED
}
