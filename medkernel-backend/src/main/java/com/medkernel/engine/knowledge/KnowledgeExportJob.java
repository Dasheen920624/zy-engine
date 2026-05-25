package com.medkernel.engine.knowledge;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 知识异步导出作业。{@code job_code} 是对外可见的 UUID（不暴露 DB 主键）。
 *
 * <p>本 PR 提供单机线程池执行器；后续 GA-ENG-PKG-01 / GA-ENG-API-13 可替换为分布式队列实现。
 */
@Table("knowledge_export_job")
public record KnowledgeExportJob(
    @Id Long id,
    @Column("tenant_id") String tenantId,
    @Column("job_code") String jobCode,
    @Column("requested_by") String requestedBy,
    @Column("export_type") ExportType exportType,
    @Column("filter_json") String filterJson,
    @Column("status") ExportStatus status,
    @Column("progress") Integer progress,
    @Column("result_uri") String resultUri,
    @Column("item_count") Long itemCount,
    @Column("error_message") String errorMessage,
    @Column("created_at") Instant createdAt,
    @Column("started_at") Instant startedAt,
    @Column("completed_at") Instant completedAt,
    @Column("expires_at") Instant expiresAt
) {

    public boolean isTerminal() {
        return status != null && status.isTerminal();
    }
}
