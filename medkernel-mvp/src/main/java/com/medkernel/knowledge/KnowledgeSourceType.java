package com.medkernel.knowledge;

/**
 * 知识来源类型。
 * 对标 AI 医疗知识工厂方案中的 7 种来源类型。
 */
public enum KnowledgeSourceType {
    /** 标准术语集 */
    STANDARD_TERMINOLOGY("STANDARD_TERMINOLOGY", "标准术语集"),
    /** 医保政策 */
    INSURANCE_POLICY("INSURANCE_POLICY", "医保政策"),
    /** 药品说明书 */
    DRUG_LABEL("DRUG_LABEL", "药品说明书"),
    /** 临床指南 */
    CLINICAL_GUIDELINE("CLINICAL_GUIDELINE", "临床指南"),
    /** 质控政策 */
    QUALITY_POLICY("QUALITY_POLICY", "质控政策"),
    /** 院内字典 */
    HOSPITAL_DICTIONARY("HOSPITAL_DICTIONARY", "院内字典"),
    /** 厂商接口文档 */
    VENDOR_INTERFACE_DOC("VENDOR_INTERFACE_DOC", "厂商接口文档");

    private final String code;
    private final String label;

    KnowledgeSourceType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    public static KnowledgeSourceType fromCode(String code) {
        if (code == null) return null;
        for (KnowledgeSourceType t : values()) {
            if (t.code.equalsIgnoreCase(code)) return t;
        }
        return null;
    }
}
