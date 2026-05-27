package com.medkernel.engine.evaluation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建评估指标草稿版本的请求。
 *
 * <p>包含指标编码、版本号、评估对象类型、分母/分子定义、适用范围、责任科室和来源引用。
 */
public record EvaluationIndicatorCreateRequest(
    @NotBlank String indicatorCode,
    @Min(1) int versionNo,
    @NotBlank String name,
    @NotNull EvaluationSubjectType subjectType,
    @NotBlank String denominatorDefinition,
    @NotBlank String numeratorDefinition,
    String exclusionDefinition,
    String scoringDefinition,
    @NotBlank String timeWindow,
    @NotBlank String organizationScope,
    @NotBlank String responsibleDepartmentId,
    @NotBlank String sourceRef,
    String packageVersion
) {}
