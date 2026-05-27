package com.medkernel.engine.pkg;

/**
 * 知识包生命周期状态枚举。
 */
public enum KnowledgePackageStatus {
    /** 草稿 */
    DRAFT,
    /** 待审核 */
    PENDING_REVIEW,
    /** 已发布 */
    PUBLISHED,
    /** 激活（当前在用） */
    ACTIVE,
    /** 下线 */
    OFFLINE,
    /** 归档 */
    ARCHIVED
}
