package com.medkernel.engine.security.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 平台开通租户入参：租户标识（仅小写字母/数字/连字符）、租户名、首个管理员登录名；
 * 管理员初始密码可选，留空则生成临时密码一次性返回。
 */
public record ProvisionTenantRequest(
    @NotBlank @Pattern(regexp = "[a-z0-9-]{2,64}", message = "租户标识仅允许小写字母/数字/连字符，2-64 位") String tenantId,
    @NotBlank String tenantName,
    @NotBlank String adminUsername,
    String adminInitialPassword
) {}
