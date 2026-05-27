package com.medkernel.engine.pathway;

/**
 * 路径节点类型。
 *
 * <p>覆盖筛查、评估、检查、检验、用药、手术、护理、康复、出院、随访和质控节点。
 */
public enum PathwayNodeType {
    SCREENING,
    ASSESSMENT,
    EXAM,
    LAB,
    MEDICATION,
    SURGERY,
    NURSING,
    REHAB,
    DISCHARGE,
    FOLLOWUP,
    QUALITY
}
