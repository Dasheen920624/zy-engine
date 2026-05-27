package com.medkernel.engine.pathway;

public record PathwayProgressCommand(
    PathwayGraph graph,
    String currentNodeCode,
    PathwayAdvanceEventType eventType,
    String requestedNextNodeCode
) {}
