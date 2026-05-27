package com.medkernel.engine.pathway;

/**
 * 路径边类型。
 *
 * <p>覆盖默认推进、条件分支、风险分层、患者选择、资源不可用、医生决策和异常回退。
 */
public enum PathwayEdgeType {
    DEFAULT,
    CONDITION,
    RISK_STRATIFICATION,
    PATIENT_CHOICE,
    RESOURCE_UNAVAILABLE,
    PHYSICIAN_DECISION,
    ROLLBACK
}
