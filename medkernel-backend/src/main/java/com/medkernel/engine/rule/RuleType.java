package com.medkernel.engine.rule;

/**
 * 规则业务类型枚举（GA-ENG-API-05）。
 *
 * <p>取值含义：{@code DIAGNOSIS} 诊断、{@code ORDER} 医嘱、{@code LAB} 检验、{@code REPORT} 报告、
 * {@code DISCHARGE} 出院、{@code FOLLOWUP} 随访、{@code INSURANCE} 医保、{@code QUALITY} 质控、
 * {@code RECORD} 病历、{@code PATHWAY} 路径，用于按业务域聚合规则资产并配合 {@code rule_type} CHECK 约束。
 */
public enum RuleType {
    DIAGNOSIS,
    ORDER,
    LAB,
    REPORT,
    DISCHARGE,
    FOLLOWUP,
    INSURANCE,
    QUALITY,
    RECORD,
    PATHWAY
}
