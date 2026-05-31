package com.medkernel.engine.rule;

/**
 * 规则风险级别枚举（GA-ENG-API-05）。
 *
 * <p>取值含义：{@code LOW} 低、{@code MEDIUM} 中、{@code HIGH} 高、{@code CRITICAL} 红线；
 * 高级别动作（HIGH/CRITICAL）触发 {@code requiresPhysicianConfirmation=true}，并影响试运行/真实执行的最高严重度计算。
 */
public enum RuleRiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    /**
     * 取两侧风险级别的较高值，{@code null} 视为无值（返回非空对端）。
     */
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
