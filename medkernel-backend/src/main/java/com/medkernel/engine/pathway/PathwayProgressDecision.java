package com.medkernel.engine.pathway;

public record PathwayProgressDecision(
    String previousNodeCode,
    String nextNodeCode,
    PatientPathwayStatus status,
    PathwayEdgeType edgeType
) {}
