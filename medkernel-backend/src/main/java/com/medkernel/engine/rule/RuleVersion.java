package com.medkernel.engine.rule;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("rule_version")
public record RuleVersion(
    @Id Long id,
    @Column("version_id") String versionId,
    @Column("tenant_id") String tenantId,
    @Column("rule_id") String ruleId,
    @Column("version_no") Integer versionNo,
    @Column("source_ref") String sourceRef,
    @Column("change_summary") String changeSummary,
    @Column("dsl_json") String dslJson,
    @Column("explanation_json") String explanationJson,
    RuleVersionStatus status,
    @Column("published_at") Instant publishedAt,
    @Column("published_by") String publishedBy,
    @Column("rollback_version_id") String rollbackVersionId,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy,
    @Column("trace_id") String traceId
) {}
