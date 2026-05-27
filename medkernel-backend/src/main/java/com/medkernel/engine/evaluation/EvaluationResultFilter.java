package com.medkernel.engine.evaluation;

/**
 * 评估结果分页查询过滤条件。
 *
 * <p>按指标编码、结果等级和责任科室组合过滤；字段为 {@code null} 时表示不过滤。
 */
public record EvaluationResultFilter(
    String indicatorCode,
    EvaluationResultLevel resultLevel,
    String responsibleDepartmentId
) {}
