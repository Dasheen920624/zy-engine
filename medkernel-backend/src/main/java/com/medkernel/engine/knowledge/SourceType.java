package com.medkernel.engine.knowledge;

/**
 * 来源文件类型。对应 {@code source_document.source_type} CHECK 约束。
 */
public enum SourceType {
    /** 临床指南 */
    GUIDELINE,
    /** 药品说明书 */
    DRUG_LABEL,
    /** 行业 / 国家标准 */
    STANDARD,
    /** 医保 / 公卫 / 行政政策 */
    POLICY,
    /** 院内制度 / SOP */
    HOSPITAL_PROTOCOL,
    /** 中医典籍 */
    TCM_CLASSIC,
    /** 学术文献 */
    LITERATURE,
    /** 专家共识 */
    CONSENSUS,
    /** 其他 */
    OTHER
}
