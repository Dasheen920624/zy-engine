package com.medkernel.engine.pathway;

import jakarta.validation.constraints.NotBlank;

public record PatientPathwayEnterRequest(
    @NotBlank String patientId,
    String encounterId,
    @NotBlank String templateId,
    String startNodeCode
) {}
