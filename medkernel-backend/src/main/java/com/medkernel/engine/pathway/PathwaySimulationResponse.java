package com.medkernel.engine.pathway;

import java.util.List;

public record PathwaySimulationResponse(
    String templateId,
    List<String> nodeTrajectory,
    PatientPathwayStatus finalStatus,
    String traceId
) {

    public PathwaySimulationResponse {
        nodeTrajectory = nodeTrajectory == null ? List.of() : List.copyOf(nodeTrajectory);
    }
}
