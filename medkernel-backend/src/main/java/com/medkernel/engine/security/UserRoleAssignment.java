package com.medkernel.engine.security;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 用户角色分配。
 *
 * <p>JWT roles 是身份源实时断言；本表用于医院侧补充分配与数据范围审计。
 */
@Table("user_role_assignment")
public record UserRoleAssignment(
    @Id Long id,
    @Column("tenant_id") String tenantId,
    @Column("user_id") String userId,
    @Column("role_code") String roleCode,
    @Column("scope_level") String scopeLevel,
    @Column("scope_code") String scopeCode,
    @Column("active_flag") String activeFlag,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy
) {

    public Optional<RoleCode> role() {
        return RoleCode.fromCode(roleCode);
    }

    public boolean active() {
        return "Y".equalsIgnoreCase(activeFlag);
    }
}
