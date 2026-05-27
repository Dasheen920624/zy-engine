package com.medkernel.engine.followup;

import jakarta.validation.constraints.NotBlank;

public record FollowupQuestionnaireSubmitRequest(
    @NotBlank String taskId,
    @NotBlank String formData,
    String executorId,
    String executorType
) {}
