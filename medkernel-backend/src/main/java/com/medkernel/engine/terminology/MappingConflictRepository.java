package com.medkernel.engine.terminology;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MappingConflictRepository extends ListCrudRepository<MappingConflict, Long> {

    Optional<MappingConflict> findByTenantIdAndId(String tenantId, Long id);

    @Query("""
        SELECT COUNT(*) FROM mapping_conflict
        WHERE tenant_id = :tenantId
          AND (:status IS NULL OR status = :status)
          AND (:riskLevel IS NULL OR risk_level = :riskLevel)
          AND (:conflictType IS NULL OR conflict_type = :conflictType)
        """)
    long countByFilter(String tenantId, String status, String riskLevel, String conflictType);

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
