package com.medkernel.engine.security.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank String username,
    @NotBlank String password,
    String tenantId
) {
    public String tenantOrDefault() {
        return (tenantId == null || tenantId.isBlank()) ? "t-1" : tenantId;
    }
}
