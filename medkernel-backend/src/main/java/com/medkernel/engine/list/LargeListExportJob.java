package com.medkernel.engine.list;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 大规模数据异步导出任务实体。
 *
 * <p>用于存储后台 CSV 异步导出任务的进度、文件路径及元数据。
 */
@Table("large_list_export_job")
public record LargeListExportJob(
    @Id Long id,
    @Column("job_id") String jobId,
    @Column("tenant_id") String tenantId,
    @Column("resource_type") String resourceType,
    @Column("filter_criteria") String filterCriteria,
    String status,
    @Column("file_name") String fileName,
    @Column("file_path") String filePath,
    @Column("file_size") Long fileSize,
    @Column("error_message") String errorMessage,
    @Column("time_cost_ms") Long timeCostMs,
    @Column("trace_id") String traceId,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy
) {
    /**
     * 辅助工厂方法，创建一个带有初始默认值的待执行任务。
     *
     * @param jobId 异步导出任务全局唯一ID
     * @param tenantId 租户ID
     * @param resourceType 导出的列表资源类型
     * @param filterCriteria 导出筛选条件
     * @param traceId 请求链路追踪ID
     * @param creator 创建人账号或系统标识
     * @return 初始的导出任务实体
     */
    public static LargeListExportJob createPending(String jobId, String tenantId, String resourceType, String filterCriteria, String traceId, String creator) {
        Instant now = Instant.now();
        return new LargeListExportJob(
            null,
            jobId,
            tenantId,
            resourceType,
            filterCriteria,
            "PENDING",
            null,
            null,
            0L,
            null,
            0L,
            traceId,
            now,
            creator,
            now,
            creator
        );
    }
}
