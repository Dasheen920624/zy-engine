package com.medkernel.engine.pathway;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 患者路径推进请求。
 *
 * <p>携带患者路径实例、事件类型、当前节点、目标节点、变异说明或退出原因等流程事实。
 */
public record PathwayAdvanceRequest(
    @NotBlank String patientPathwayId,
    @NotNull PathwayAdvanceEventType eventType,
    String currentNodeCode,
    String requestedNextNodeCode,
    VarianceType varianceType,
    String varianceReason,
    String resolutionAction,
    String exitReason,
    String eventId
) {}
