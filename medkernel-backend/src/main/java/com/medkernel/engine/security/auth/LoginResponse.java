package com.medkernel.engine.security.auth;

import java.util.List;

public record LoginResponse(String userId, String tenantId, List<String> roles, boolean mustChangePwd) {}
