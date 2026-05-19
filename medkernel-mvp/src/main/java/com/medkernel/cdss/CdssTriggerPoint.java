package com.medkernel.cdss;

/**
 * CDSS 触发点枚举。
 * 对标临床决策支持系统标准触发场景。
 */
public enum CdssTriggerPoint {
    /** 医嘱下达时 */
    ORDER_PLACED("ORDER_PLACED", "医嘱下达"),
    /** 病历保存时 */
    EMR_SAVED("EMR_SAVED", "病历保存"),
    /** 检查申请时 */
    EXAM_REQUESTED("EXAM_REQUESTED", "检查申请"),
    /** 入径评估时 */
    PATHWAY_ENTRY("PATHWAY_ENTRY", "入径评估"),
    /** 医保结算前 */
    INSURANCE_SETTLEMENT("INSURANCE_SETTLEMENT", "医保结算"),
    /** 药品调配时 */
    DRUG_DISPENSED("DRUG_DISPENSED", "药品调配"),
    /** 出院前 */
    DISCHARGE("DISCHARGE", "出院前");

    private final String code;
    private final String label;

    CdssTriggerPoint(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
