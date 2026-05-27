package com.medkernel.engine.pathway;

import jakarta.validation.constraints.NotBlank;

public record SpecialtyMetricBindingRequest(
    @NotBlank String nodeCode,
    @NotBlank String metricCode,
    Boolean required
) {}
