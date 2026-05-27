package com.medkernel.engine.rule;

import java.util.List;

/**
 * 规则详情出参（GA-ENG-API-05）：聚合规则定义、当前激活版本与该版本下全部测试用例。
 */
public record RuleDetailResponse(
    RuleDefinition definition,
    RuleVersion version,
    List<RuleTestCase> testCases
) {
    public RuleDetailResponse {
        testCases = testCases == null ? List.of() : List.copyOf(testCases);
    }
}
