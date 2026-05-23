package com.medkernel.tenant.rule;

import jakarta.validation.constraints.NotBlank;

public record RuleValidateRequest(
    @NotBlank String patientMpi,
    @NotBlank String orderText
) {}
