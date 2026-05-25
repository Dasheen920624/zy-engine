package com.medkernel.engine.knowledge;

/**
 * 引用关系。对应 {@code citation.relation} CHECK 约束。
 */
public enum CitationRelation {
    /** 资产派生自来源片段（最常见） */
    DERIVED_FROM,
    /** 资产引用支持来源片段 */
    CITES,
    /** 资产与来源片段冲突（用于标记矛盾） */
    CONTRADICTS,
    /** 资产被来源片段支持（如临床研究证据） */
    SUPPORTS,
    /** 资产代替前一份资产（间接来源关系） */
    SUPERSEDES_OF
}
