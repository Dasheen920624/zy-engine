package com.medkernel.engine.evaluation;

/**
 * 评估闭环幂等操作类型枚举。
 *
 * <p>取值含义：{@code RECTIFICATION_SUBMIT} 整改提交；{@code RECTIFICATION_REVIEW} 整改复核。
 */
public enum EvaluationIdempotencyOperation {
    RECTIFICATION_SUBMIT,
    RECTIFICATION_REVIEW
}
