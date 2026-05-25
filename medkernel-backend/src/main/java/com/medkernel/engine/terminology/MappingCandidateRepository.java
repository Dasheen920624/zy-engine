package com.medkernel.engine.terminology;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MappingCandidateRepository extends ListCrudRepository<MappingCandidate, Long> {

    Optional<MappingCandidate> findByTenantIdAndId(String tenantId, Long id);

    @Query("""
        SELECT COUNT(*) FROM mapping_candidate
        WHERE tenant_id = :tenantId
          AND (:status IS NULL OR status = :status)
          AND (:riskLevel IS NULL OR risk_level = :riskLevel)
          AND (:conflictFlag IS NULL OR conflict_flag = :conflictFlag)
        """)
    long countByFilter(String tenantId, String status, String riskLevel, Boolean conflictFlag);

    @Query("""
        SELECT * FROM mapping_candidate
        WHERE tenant_id = :tenantId
          AND (:status IS NULL OR status = :status)
          AND (:riskLevel IS NULL OR risk_level = :riskLevel)
          AND (:conflictFlag IS NULL OR conflict_flag = :conflictFlag)
        ORDER BY updated_at DESC, id DESC
        OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
        """)
    List<MappingCandidate> pageByFilter(String tenantId, String status, String riskLevel, Boolean conflictFlag,
                                        int offset, int limit);
}
