package com.medkernel.engine.rule;

/**
 * 规则一次执行的终态枚举（GA-ENG-API-05 写入 {@code rule_execution_log.status}）。
 *
 * <p>取值含义：{@code SUCCESS} 命中并产出动作、{@code MISS} DSL 条件未命中、{@code FAILED} 执行异常。
 */
public enum RuleExecutionStatus {
    SUCCESS,
    MISS,
    FAILED
}
