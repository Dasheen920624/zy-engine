package com.medkernel.engine.evaluation;

import jakarta.validation.constraints.NotBlank;

public record RectificationSubmitRequest(
    @NotBlank String rectificationSummary,
    @NotBlank String evidenceRef
) {}
