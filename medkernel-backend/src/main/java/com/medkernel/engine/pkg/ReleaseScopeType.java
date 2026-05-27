package com.medkernel.engine.pkg;

/**
 * 灰度发布作用范围类型。
 */
public enum ReleaseScopeType {
    /** 全体 */
    ALL,
    /** 指定科室 */
    DEPARTMENT,
    /** 指定病区 */
    WARD,
    /** 指定医生团队 */
    DOCTOR_TEAM
}
