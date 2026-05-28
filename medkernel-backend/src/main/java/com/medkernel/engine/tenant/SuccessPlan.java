package com.medkernel.engine.tenant;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 租户多维生命周期与客户成功计划实体。
 *
 * <p>支持 6 大步骤演进及多维模块与专病包授权激活管理。
 */
@Table("tenant_success_plan")
public record SuccessPlan(
    @Id Long id,
    @Column("tenant_id") String tenantId,
    @Column("current_stage") String currentStage,
    @Column("health_score") Integer healthScore,
    @Column("activated_modules") String activatedModules,
    @Column("activated_pathways") String activatedPathways,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy
) {}
