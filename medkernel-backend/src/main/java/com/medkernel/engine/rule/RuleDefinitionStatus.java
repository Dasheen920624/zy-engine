package com.medkernel.engine.rule;

/**
 * 规则定义生命周期状态枚举（GA-ENG-API-05）。
 *
 * <p>取值含义：{@code DRAFT} 草稿（可编辑/补测试用例/提交发布）、{@code PUBLISHED} 已发布（可执行/解释/下线）、
 * {@code OFFLINE} 已下线（仅历史查询）、{@code ARCHIVED} 归档（仅审计追溯）。
 */
public enum RuleDefinitionStatus {
    DRAFT,
    PUBLISHED,
    OFFLINE,
    ARCHIVED
}
