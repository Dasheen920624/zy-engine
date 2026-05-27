package com.medkernel.engine.pathway;

/**
 * 路径推进器输入命令。
 *
 * <p>将路径图、当前节点、推进事件和可选目标节点组合为一次纯计算输入。
 */
public record PathwayProgressCommand(
    PathwayGraph graph,
    String currentNodeCode,
    PathwayAdvanceEventType eventType,
    String requestedNextNodeCode
) {}
