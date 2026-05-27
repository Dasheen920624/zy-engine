package com.medkernel.engine.followup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record FollowupPlanGenerateRequest(
    @NotBlank String patientId,
    @NotBlank String encounterId,
    String pathwayId,
    String diseaseCode,
    String riskLevel,
    @NotNull List<String> taskTypes
) {}
