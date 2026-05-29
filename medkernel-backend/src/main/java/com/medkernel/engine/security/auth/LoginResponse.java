package com.medkernel.engine.security.auth;

import java.util.List;

/**
 * 平台账号登录出参：用户标识、租户、角色编码列表与是否须首登改密。
 */
public record LoginResponse(String userId, String tenantId, List<String> roles, boolean mustChangePwd) {}
