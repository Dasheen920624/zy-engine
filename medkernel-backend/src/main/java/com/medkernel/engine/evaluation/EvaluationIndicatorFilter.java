package com.medkernel.engine.evaluation;

/**
 * 评估指标分页查询过滤条件。
 *
 * <p>按指标状态、评估对象类型和指标编码组合过滤；字段为 {@code null} 时表示不过滤。
 */
public record EvaluationIndicatorFilter(
    EvaluationIndicatorStatus status,
    EvaluationSubjectType subjectType,
    String indicatorCode
) {}
