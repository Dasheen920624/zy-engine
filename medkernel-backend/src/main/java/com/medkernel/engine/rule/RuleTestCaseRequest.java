package com.medkernel.engine.rule;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotNull;

/**
 * 新增规则测试用例的入参（GA-ENG-API-05 {@code POST /api/v1/engine/rules/{ruleId}/test-cases}）。
 *
 * <p>{@code caseType} 与 {@code inputPayload} 必填；{@code expectedSeverity}/{@code expectedActionCode}
 * 在期望命中场景下为发布门禁的对照值。
 */
public record RuleTestCaseRequest(
    @NotNull RuleTestCaseType caseType,
    @NotNull JsonNode inputPayload,
    boolean expectedHit,
    RuleRiskLevel expectedSeverity,
    String expectedActionCode
) {}
