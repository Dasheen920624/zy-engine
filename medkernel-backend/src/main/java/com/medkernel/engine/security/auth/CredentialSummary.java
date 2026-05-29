package com.medkernel.engine.security.auth;

import java.time.Instant;

/**
 * 成员账号摘要（管理列表用）：不含口令哈希，仅暴露标识、登录名、状态与是否须改密。
 */
public record CredentialSummary(
    String userId,
    String username,
    String status,
    boolean mustChangePwd,
    Instant createdAt
) {}
