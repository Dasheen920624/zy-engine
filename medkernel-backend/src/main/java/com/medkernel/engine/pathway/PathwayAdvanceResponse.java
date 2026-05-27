package com.medkernel.engine.pathway;

/**
 * 患者路径推进响应。
 *
 * <p>返回上一个节点、下一节点、推进后状态、可选变异 ID 和 traceId，便于前端回放执行轨迹。
 */
public record PathwayAdvanceResponse(
    String patientPathwayId,
    String previousNodeCode,
    String nextNodeCode,
    PatientPathwayStatus status,
    String varianceId,
    String traceId
) {}
