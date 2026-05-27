package com.medkernel.engine.pathway;

/**
 * 路径模板生命周期状态。
 *
 * <p>草稿可编辑，发布后可入径，下线和归档状态仅保留查询与追溯。
 */
public enum PathwayTemplateStatus {
    DRAFT,
    PUBLISHED,
    OFFLINE,
    ARCHIVED
}
