package com.medkernel.engine.terminology;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * 术语映射冲突持久化仓库；按 tenant_id 隔离。
 */
@Repository
public interface MappingConflictRepository extends ListCrudRepository<MappingConflict, Long> {

    Optional<MappingConflict> findByTenantIdAndId(String tenantId, Long id);

    /**
     * 按租户 + 可选过滤条件（状态 / 风险等级 / 冲突类型）统计冲突数量。
     */
    @Query("""
        SELECT COUNT(*) FROM mapping_conflict
        WHERE tenant_id = :tenantId
          AND (:status IS NULL OR status = :status)
          AND (:riskLevel IS NULL OR risk_level = :riskLevel)
          AND (:conflictType IS NULL OR conflict_type = :conflictType)
        """)
    long countByFilter(String tenantId, String status, String riskLevel, String conflictType);

    /**
     * 按租户 + 可选过滤条件分页查询冲突（更新时间倒序），用于冲突处置工作台。
     */
    @Query("""
        SELECT * FROM mapping_conflict
        WHERE tenant_id = :tenantId
          AND (:status IS NULL OR status = :status)
          AND (:riskLevel IS NULL OR risk_level = :riskLevel)
          AND (:conflictType IS NULL OR conflict_type = :conflictType)
        ORDER BY updated_at DESC, id DESC
        OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
        """)
    List<MappingConflict> pageByFilter(String tenantId, String status, String riskLevel, String conflictType,
                                       int offset, int limit);
}
