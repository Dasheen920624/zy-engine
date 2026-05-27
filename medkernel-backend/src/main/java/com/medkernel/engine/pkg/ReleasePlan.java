package com.medkernel.engine.pkg;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 发布与灰度计划实体。
 *
 * <p>记录灰度与全量发布目标、策略及发布状态机。
 */
@Table("release_plan")
public record ReleasePlan(
    @Id Long id,
    @Column("plan_id") String planId,
    @Column("tenant_id") String tenantId,
    @Column("package_id") String packageId,
    @Column("target_org_unit_id") String targetOrgUnitId,
    ReleaseStrategy strategy,
    @Column("scope_type") ReleaseScopeType scopeType,
    @Column("scope_value") String scopeValue,
    ReleasePlanStatus status,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy,
    @Column("trace_id") String traceId
) {
    public ReleasePlan withStatus(ReleasePlanStatus newStatus) {
        return new ReleasePlan(
            id, planId, tenantId, packageId, targetOrgUnitId,
            strategy, scopeType, scopeValue, newStatus,
            createdAt, createdBy, Instant.now(), updatedBy, traceId
        );
    }
}
