package com.medkernel.engine.evaluation;

/**
 * 评估结果等级枚举。
 *
 * <p>取值含义：{@code PASS} 通过、{@code ATTENTION} 需关注、
 * {@code NON_COMPLIANT} 不符合、{@code CRITICAL} 安全红线。
 */
public enum EvaluationResultLevel {
    PASS,
    ATTENTION,
    NON_COMPLIANT,
    CRITICAL
}
