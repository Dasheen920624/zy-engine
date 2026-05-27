package com.medkernel.engine.pathway;

/**
 * 路径推进器输出决策。
 *
 * <p>描述本次推进的上一节点、下一节点、运行后状态和命中的路径边类型。
 */
public record PathwayProgressDecision(
    String previousNodeCode,
    String nextNodeCode,
    PatientPathwayStatus status,
    PathwayEdgeType edgeType
) {}
