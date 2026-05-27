package com.medkernel.engine.rule;

public record RuleActionResult(
    String actionCode,
    RuleRiskLevel severity,
    String message,
    boolean requiresPhysicianConfirmation
) {}
