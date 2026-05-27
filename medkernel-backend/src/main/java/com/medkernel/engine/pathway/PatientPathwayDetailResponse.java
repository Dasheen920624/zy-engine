package com.medkernel.engine.pathway;

import java.util.List;

public record PatientPathwayDetailResponse(
    PatientPathway patientPathway,
    List<PathwayVariance> variances,
    List<ClinicalClock> clocks,
    String traceId
) {

    public PatientPathwayDetailResponse {
        variances = variances == null ? List.of() : List.copyOf(variances);
        clocks = clocks == null ? List.of() : List.copyOf(clocks);
    }
}
