package com.medkernel.engine.security.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 自助改密入参：校验原密码后设置新密码（≥8 位），并清除"首登须改密"标志。
 */
public record ChangePasswordRequest(
    @NotBlank String oldPassword,
    @NotBlank @Size(min = 8, message = "新密码至少 8 位") String newPassword
) {}
