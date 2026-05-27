package com.medkernel.engine.recommendation;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * GA-ENG-API-07 疲劳治理信号事实实体（仅事实采集，不做自动屏蔽）。
 *
 * <p>信号类型 {@link RecommendationFatigueSignalType} 记录推荐卡的展示、查看、采纳、不采纳等事件；
 * 通过 {@code fatigueKey}、患者、操作者、时间窗聚合，为后续疲劳治理引擎提供输入。
 * 静默试运行（SILENT_RECORDED）不打扰医生但形成证据，可生成报告。
 */
@Table("recommendation_fatigue_signal")
public record RecommendationFatigueSignal(
    @Id Long id,
    @Column("signal_id") String signalId,
    @Column("tenant_id") String tenantId,
    @Column("trigger_id") String triggerId,
    @Column("card_id") String cardId,
    @Column("fatigue_key") String fatigueKey,
    @Column("patient_id") String patientId,
    @Column("encounter_id") String encounterId,
    @Column("operator_id") String operatorId,
    @Column("signal_type") RecommendationFatigueSignalType signalType,
    @Column("occurrence_count") int occurrenceCount,
    @Column("window_started_at") Instant windowStartedAt,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy,
    @Column("trace_id") String traceId
) {}
