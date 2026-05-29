package com.medkernel.engine.security.auth;

import jakarta.validation.constraints.Pattern;

/**
 * 启用/停用成员账号入参：状态须为 ACTIVE / DISABLED / LOCKED 之一。
 */
public record SetStatusRequest(
    @Pattern(regexp = "ACTIVE|DISABLED|LOCKED", message = "状态须为 ACTIVE/DISABLED/LOCKED") String status
) {}
