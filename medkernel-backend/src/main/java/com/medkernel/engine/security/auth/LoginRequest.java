package com.medkernel.engine.security.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * 平台账号登录入参：用户名与密码必填，租户可选（缺省回退默认租户）。
 */
public record LoginRequest(
    @NotBlank String username,
    @NotBlank String password,
    String tenantId
) {
    public String tenantOrDefault() {
        return (tenantId == null || tenantId.isBlank()) ? "t-1" : tenantId;
    }
}
