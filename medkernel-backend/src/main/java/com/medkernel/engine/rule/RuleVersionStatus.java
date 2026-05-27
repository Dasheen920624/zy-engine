package com.medkernel.engine.rule;

/**
 * 规则版本生命周期状态枚举（GA-ENG-API-05）。
 *
 * <p>取值含义：{@code DRAFT} 草稿版本（编辑中）、{@code PUBLISHED} 已发布版本（参与执行）、
 * {@code OFFLINE} 已下线版本（不参与默认执行）、{@code ARCHIVED} 归档版本（仅追溯）。
 */
public enum RuleVersionStatus {
    DRAFT,
    PUBLISHED,
    OFFLINE,
    ARCHIVED
}
