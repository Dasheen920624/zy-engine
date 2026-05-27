package com.medkernel.engine.recommendation;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * GA-ENG-API-07 推荐卡来源解释实体（实现 100% 可追溯）。
 *
 * <p>每张 {@link RecommendationCard} 至少含一条来源；来源类型 {@link RecommendationSourceType}
 * 覆盖规则、路径、知识、上下文、术语和人工六类。包含 source_ref_id、source_version、source_hash 中至少一项
 * 可追溯信息，并通过 {@code citation_locator}（章节/段落/字段路径）定位证据片段。
 */
@Table("recommendation_source")
public record RecommendationSource(
    @Id Long id,
    @Column("source_id") String sourceId,
    @Column("tenant_id") String tenantId,
    @Column("card_id") String cardId,
    @Column("source_type") RecommendationSourceType sourceType,
    @Column("source_ref_id") String sourceRefId,
    @Column("source_version") String sourceVersion,
    @Column("source_title") String sourceTitle,
    @Column("citation_locator") String citationLocator,
    @Column("source_hash") String sourceHash,
    String summary,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy,
    @Column("trace_id") String traceId
) {}
