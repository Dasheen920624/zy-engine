package com.medkernel.engine.pathway;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("pathway_node")
public record PathwayNode(
    @Id Long id,
    @Column("node_id") String nodeId,
    @Column("tenant_id") String tenantId,
    @Column("template_id") String templateId,
    @Column("node_code") String nodeCode,
    String name,
    @Column("node_type") PathwayNodeType nodeType,
    @Column("sort_order") Integer sortOrder,
    @Column("responsible_role") String responsibleRole,
    @Column("dependency_json") String dependencyJson,
    @Column("time_window_minutes") Integer timeWindowMinutes,
    @Column("terminal_flag") Boolean terminalFlag,
    @Column("config_json") String configJson,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy,
    @Column("trace_id") String traceId
) {}
