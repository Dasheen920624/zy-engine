package com.medkernel.engine.pathway;

/**
 * 专病包生命周期状态。
 *
 * <p>草稿可维护，发布后可绑定路径模板，下线和归档用于历史追溯。
 */
public enum SpecialtyPackageStatus {
    DRAFT,
    PUBLISHED,
    OFFLINE,
    ARCHIVED
}
