package com.medkernel.engine.security;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 平台自建身份凭证（外网 SaaS / 本地 dev）。真实身份认证内网走院方 IdP，本表仅用于平台签发。
 */
@Table("platform_credential")
public record PlatformCredential(
    @Id Long id,
    @Column("credential_id") String credentialId,
    @Column("tenant_id") String tenantId,
    @Column("user_id") String userId,
    @Column("username") String username,
    @Column("password_hash") String passwordHash,
    @Column("status") String status,
    @Column("must_change_pwd") String mustChangePwd,
    @Column("mfa_secret") String mfaSecret,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy,
    @Column("trace_id") String traceId
) {
    public boolean active() {
        return "ACTIVE".equalsIgnoreCase(status);
    }
}
