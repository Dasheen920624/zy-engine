package com.medkernel.engine.evaluation;

import jakarta.validation.constraints.NotNull;

public record RectificationReviewRequest(
    @NotNull RectificationReviewDecision decision,
    String comment,
    String evidenceRef
) {}
