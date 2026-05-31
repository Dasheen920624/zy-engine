package com.medkernel.engine.pathway;

import java.util.List;

/**
 * 路径试运行响应。
 *
 * <p>返回模板 ID、节点轨迹、最终状态和 traceId，用于在发布或调试前回放路径走向。
 */
public record PathwaySimulationResponse(
    String templateId,
    List<String> nodeTrajectory,
    PatientPathwayStatus finalStatus,
    String traceId
) {

    /**
     * 创建不可变试运行响应，并将空轨迹归一为空列表。
     */
    public PathwaySimulationResponse {
        nodeTrajectory = nodeTrajectory == null ? List.of() : List.copyOf(nodeTrajectory);
    }
}
