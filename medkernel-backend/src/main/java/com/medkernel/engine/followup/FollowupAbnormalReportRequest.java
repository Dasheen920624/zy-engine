package com.medkernel.engine.followup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FollowupAbnormalReportRequest(
    @NotBlank String planId,
    @NotNull FollowupEventType eventType,
    @NotBlank String payload,
    String triggeredBy
) {}
