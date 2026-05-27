package com.medkernel.shared.observability;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 引擎状态机跳转历史。所有 API 共享，按 entityType + entityId 定位某实体的历史轨迹。
 */
@Table("state_transition_history")
public record StateTransitionHistory(
    @Id Long id,
    @Column("entity_type")     String entityType,
    @Column("entity_id")       String entityId,
    @Column("tenant_id")       String tenantId,
    @Column("from_status")     String fromStatus,
    @Column("to_status")       String toStatus,
    @Column("reason")          String reason,
    @Column("actor")           String actor,
    @Column("trace_id")        String traceId,
    @Column("error_code")      String errorCode,
    @Column("error_class")     String errorClass,
    @Column("error_message")   String errorMessage,
    @Column("retry_count")     Integer retryCount,
    @Column("next_retry_at")   Instant nextRetryAt,
    @Column("occurred_at")     Instant occurredAt
) {}
