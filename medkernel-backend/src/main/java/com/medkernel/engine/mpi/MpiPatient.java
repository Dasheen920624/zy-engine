package com.medkernel.engine.mpi;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 患者主索引（MPI）数据模型实体。
 *
 * <p>表示跨系统、多就诊合并后的唯一患者数字资产，支持多租户强隔离下的多索引归集。
 */
@Table("mpi_patient")
public record MpiPatient(
    @Id Long id,
    @Column("mpi_id") String mpiId,
    @Column("tenant_id") String tenantId,
    @Column("masked_name") String maskedName,
    @Column("gender") String gender,
    @Column("age") Integer age,
    @Column("id_last4") String idLast4,
    @Column("merged_count") Integer mergedCount,
    @Column("status") String status,
    @Column("merged_into_mpi_id") String mergedIntoMpiId,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy
) {}
