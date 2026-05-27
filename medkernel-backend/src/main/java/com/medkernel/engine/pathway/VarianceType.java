package com.medkernel.engine.pathway;

/**
 * 路径变异类型。
 *
 * <p>区分医学原因、患者原因、资源原因、医生选择和系统原因导致的路径偏离。
 */
public enum VarianceType {
    MEDICAL,
    PATIENT_REASON,
    RESOURCE_REASON,
    DOCTOR_CHOICE,
    SYSTEM_REASON
}
