package com.medkernel.engine.rule;

/**
 * 规则编写模式枚举（GA-ENG-API-05）。
 *
 * <p>取值含义：{@code TEMPLATE} 模板向导、{@code VISUAL} 可视化编辑器、{@code DSL} JSON DSL；
 * 首版主要支持 {@code DSL}，{@code TEMPLATE}/{@code VISUAL} 为后续 GA-ENG-RULE-01 规则编辑器预留。
 */
public enum RuleAuthoringMode {
    TEMPLATE,
    VISUAL,
    DSL
}
