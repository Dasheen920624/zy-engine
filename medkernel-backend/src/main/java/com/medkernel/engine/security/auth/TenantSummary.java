package com.medkernel.engine.security.auth;

import java.time.Instant;

/**
 * 租户摘要（平台租户列表用）：租户标识、名称、状态与开通时间。
 */
public record TenantSummary(String tenantId, String name, String status, Instant createdAt) {}
