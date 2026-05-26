package com.medkernel.engine.security;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 租户级角色权限覆盖。
 *
 * <p>表内只保存“相对默认策略”的显式 ALLOW / DENY，不复制整张默认矩阵。
 */
@Table("role_permission")
public record RolePermissionOverride(
    @Id Long id,
    @Column("tenant_id") String tenantId,
    @Column("role_code") String roleCode,
    @Column("permission_code") String permissionCode,
    @Column("effect") PermissionEffect effect,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy
) {

    public Optional<RoleCode> role() {
        return RoleCode.fromCode(roleCode);
    }

    public Optional<PermissionCode> permission() {
        return PermissionCode.fromCode(permissionCode);
    }
}
