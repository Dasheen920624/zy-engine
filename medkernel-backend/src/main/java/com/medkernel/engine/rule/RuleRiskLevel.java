package com.medkernel.engine.rule;

public enum RuleRiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static RuleRiskLevel max(RuleRiskLevel left, RuleRiskLevel right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.ordinal() >= right.ordinal() ? left : right;
    }
}
