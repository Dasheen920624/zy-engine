package com.medkernel.engine.pkg;

/**
 * 知识包发布计划执行状态枚举。
 */
public enum ReleasePlanStatus {
    /** 草稿 */
    DRAFT,
    /** 执行中 */
    EXECUTING,
    /** 发布成功 */
    SUCCESS,
    /** 发布失败 */
    FAILED,
    /** 已回滚 */
    ROLLBACKED
}
