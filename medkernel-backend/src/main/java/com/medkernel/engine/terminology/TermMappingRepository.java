package com.medkernel.engine.terminology;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TermMappingRepository extends ListCrudRepository<TermMapping, Long> {

    Optional<TermMapping> findByTenantIdAndLocalTermIdAndStandardTermId(String tenantId, Long localTermId,
                                                                        Long standardTermId);

    @Query("""
        SELECT COUNT(*) FROM term_mapping tm
        WHERE tm.tenant_id = :tenantId
          AND (:sourceSystem IS NULL OR tm.source_system = :sourceSystem)
          AND (:category IS NULL OR tm.category = :category)
          AND (:status IS NULL OR tm.status = :status)
          AND (:keyword IS NULL OR LOWER(tm.evidence_text) LIKE :keyword)
        """)
    long countByFilter(String tenantId, String sourceSystem, String category, String status, String keyword);

    @Query("""
        SELECT tm.* FROM term_mapping tm
        WHERE tm.tenant_id = :tenantId
          AND (:sourceSystem IS NULL OR tm.source_system = :sourceSystem)
          AND (:category IS NULL OR tm.category = :category)
          AND (:status IS NULL OR tm.status = :status)
          AND (:keyword IS NULL OR LOWER(tm.evidence_text) LIKE :keyword)
        ORDER BY tm.updated_at DESC, tm.id DESC
        OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
        """)
    List<TermMapping> pageByFilter(String tenantId, String sourceSystem, String category, String status,
                                   String keyword, int offset, int limit);

    @Query("""
        SELECT tm.* FROM term_mapping tm
        JOIN local_term lt ON lt.id = tm.local_term_id AND lt.tenant_id = tm.tenant_id
        WHERE tm.tenant_id = :tenantId
          AND tm.status = 'CONFIRMED'
          AND (:scopeLevel IS NULL
               OR (:scopeLevel = 'DEPARTMENT' AND lt.department_id = :scopeCode)
               OR (:scopeLevel <> 'DEPARTMENT'))
        ORDER BY tm.updated_at DESC, tm.id DESC
        """)
    List<TermMapping> findConfirmedByTenantIdAndScope(String tenantId, String scopeLevel, String scopeCode);
}
