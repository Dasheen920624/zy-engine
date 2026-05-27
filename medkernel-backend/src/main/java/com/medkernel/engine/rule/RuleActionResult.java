package com.medkernel.engine.rule;

/**
 * 规则 DSL {@code then} 段动作产出值对象（GA-ENG-API-05）。
 *
 * <p>承载动作码、严重度、提示文本以及是否要求医师确认；高严重度动作以及
 * {@code BLOCK}/{@code STRONG_REMINDER}/{@code RECOMMEND_NEXT} 一律强制确认。
 */
public record RuleActionResult(
    String actionCode,
    RuleRiskLevel severity,
    String message,
    boolean requiresPhysicianConfirmation
) {}
