package com.medkernel.engine.rule;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("rule_execution_log")
public record RuleExecutionLog(
    @Id Long id,
    @Column("execution_id") String executionId,
    @Column("tenant_id") String tenantId,
    @Column("rule_id") String ruleId,
    @Column("version_id") String versionId,
    @Column("trigger_point") String triggerPoint,
    @Column("event_id") String eventId,
    @Column("actor_user_id") String actorUserId,
    @Column("input_digest") String inputDigest,
    Boolean hit,
    RuleRiskLevel severity,
    @Column("actions_json") String actionsJson,
    @Column("explanation_json") String explanationJson,
    RuleExecutionStatus status,
    @Column("error_code") String errorCode,
    @Column("error_class") String errorClass,
    @Column("executed_at") Instant executedAt,
    @Column("created_at") Instant createdAt,
    @Column("trace_id") String traceId
) {}
