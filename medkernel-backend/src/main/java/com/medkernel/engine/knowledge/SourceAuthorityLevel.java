package com.medkernel.engine.knowledge;

/**
 * 来源权威级别。对应 {@code source_document.authority_level} CHECK 约束。
 *
 * <p>用于详细规范 §8.3 的"权威分级"门禁：高风险动作引用必须 ≥ SOCIETY；
 * 院内私自整理的资料归为 {@link #HOSPITAL}，不能用于跨院推广的临床决策。
 */
public enum SourceAuthorityLevel {
    /** 中国国家级（卫健委 / 国家药监局 / 国家中医药管理局发布的标准与规范） */
    CHINA_NATIONAL,
    /** 国际权威（WHO / FDA / ICH / 国际医学会主流指南） */
    INTERNATIONAL,
    /** 国家级或省级专业学会共识 */
    SOCIETY,
    /** 院内制度 */
    HOSPITAL,
    /** 其他 */
    OTHER
}
