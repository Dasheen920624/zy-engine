package com.medkernel.engine.security.auth;

/**
 * 平台开通租户出参：新租户标识、首个管理员用户标识与登录名；
 * tempPassword 仅当系统生成临时密码时一次性返回（否则为 null）。
 */
public record ProvisionTenantResponse(
    String tenantId,
    String adminUserId,
    String adminUsername,
    String tempPassword
) {}
