package com.medkernel.engine.pathway;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
