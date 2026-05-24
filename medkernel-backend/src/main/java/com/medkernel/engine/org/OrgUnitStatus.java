package com.medkernel.engine.org;

/**
 * 组织单元生命周期状态。与 DDL CHECK 约束严格对应。
 */
public enum OrgUnitStatus {
    /** 正常服务 */
    ACTIVE,
    /** 暂停（保留数据但不参与新业务，如试点结束未续约） */
    SUSPENDED,
    /** 归档（不可再启用；保留历史审计） */
    ARCHIVED
}
