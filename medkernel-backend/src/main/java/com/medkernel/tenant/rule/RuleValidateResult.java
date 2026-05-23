package com.medkernel.tenant.rule;

import java.util.List;

public record RuleValidateResult(
    String patientMpi,
    Integer hitCount,
    List<RuleHit> hits
) {
    public record RuleHit(
        String ruleId,
        String ruleName,
        String severity,
        String source,
        String suggestion
    ) {}
}
