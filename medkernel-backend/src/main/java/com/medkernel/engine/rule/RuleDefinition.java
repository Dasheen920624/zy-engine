package com.medkernel.engine.rule;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("rule_definition")
public record RuleDefinition(
    @Id Long id,
    @Column("rule_id") String ruleId,
    @Column("tenant_id") String tenantId,
    @Column("rule_code") String ruleCode,
    String name,
    @Column("rule_type") RuleType ruleType,
    @Column("authoring_mode") RuleAuthoringMode authoringMode,
    @Column("risk_level") RuleRiskLevel riskLevel,
    RuleDefinitionStatus status,
    @Column("active_version_id") String activeVersionId,
    @Column("package_version") String packageVersion,
    @Column("applicable_org_unit_id") String applicableOrgUnitId,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy,
    @Column("trace_id") String traceId
) {}
