package com.medkernel.engine.pathway;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("pathway_template")
public record PathwayTemplate(
    @Id Long id,
    @Column("template_id") String templateId,
    @Column("tenant_id") String tenantId,
    @Column("package_id") String packageId,
    @Column("template_code") String templateCode,
    String name,
    @Column("disease_code") String diseaseCode,
    @Column("template_version") Integer templateVersion,
    @Column("template_level") PathwayTemplateLevel templateLevel,
    PathwayTemplateStatus status,
    @Column("start_node_code") String startNodeCode,
    @Column("source_ref") String sourceRef,
    String description,
    @Column("entry_criteria_json") String entryCriteriaJson,
    @Column("exit_criteria_json") String exitCriteriaJson,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy,
    @Column("trace_id") String traceId
) {}
