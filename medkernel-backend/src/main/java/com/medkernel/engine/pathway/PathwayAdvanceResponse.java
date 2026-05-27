package com.medkernel.engine.pathway;

public record PathwayAdvanceResponse(
    String patientPathwayId,
    String previousNodeCode,
    String nextNodeCode,
    PatientPathwayStatus status,
    String varianceId,
    String traceId
) {}
