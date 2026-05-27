package com.medkernel.engine.pathway;

import java.util.List;

/**
 * 路径仿真请求。
 *
 * <p>可指定仿真起点和每一步期望进入的下一节点，用于验证模板图的可达性。
 */
public record PathwaySimulateRequest(
    String startNodeCode,
    List<String> requestedNextNodeCodes
) {

    /**
     * 创建不可变仿真请求，并将空目标节点序列归一为空列表。
     */
    public PathwaySimulateRequest {
        requestedNextNodeCodes = requestedNextNodeCodes == null
            ? List.of() : List.copyOf(requestedNextNodeCodes);
    }
}
