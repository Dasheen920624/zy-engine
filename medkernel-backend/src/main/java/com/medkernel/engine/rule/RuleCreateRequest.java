package com.medkernel.engine.rule;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建规则的入参（GA-ENG-API-05 {@code POST /api/v1/engine/rules}）。
 *
 * <p>字段语义见规则引擎 API 设计文档：{@code ruleCode}/{@code name}/{@code sourceRef}/{@code dsl} 为必填，
 * {@code authoringMode}/{@code riskLevel} 缺省由服务端兜底（默认 DSL / MEDIUM）。
 */
public record RuleCreateRequest(
    @NotBlank String ruleCode,
    @NotBlank String name,
    @NotNull RuleType ruleType,
    RuleAuthoringMode authoringMode,
    RuleRiskLevel riskLevel,
    String packageVersion,
    String applicableOrgUnitId,
    @NotBlank String sourceRef,
    String changeSummary,
    @NotNull JsonNode dsl,
    JsonNode explanation
) {}
