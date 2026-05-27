package com.medkernel.engine.rule;

import java.util.List;

/**
 * 规则发布出参（GA-ENG-API-05）：返回发布后的规则状态以及发布门禁所有测试用例的执行结果。
 */
public record RulePublishResponse(
    String ruleId,
    String versionId,
    RuleDefinitionStatus status,
    String traceId,
    List<RuleTestCaseResult> results
) {
    public RulePublishResponse {
        results = results == null ? List.of() : List.copyOf(results);
    }
}
