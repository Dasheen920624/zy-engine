package com.medkernel.engine.terminology;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalTermRepository extends ListCrudRepository<LocalTerm, Long> {

    Optional<LocalTerm> findByTenantIdAndId(String tenantId, Long id);

    @Query("""
        SELECT COUNT(*) FROM local_term
        WHERE tenant_id = :tenantId
          AND (:sourceSystem IS NULL OR source_system = :sourceSystem)
          AND (:category IS NULL OR category = :category)
          AND (:status IS NULL OR status = :status)
          AND (:keyword IS NULL OR LOWER(local_name) LIKE :keyword OR LOWER(local_code) LIKE :keyword)
        """)
    long countByFilter(String tenantId, String sourceSystem, String category, String status, String keyword);

    @Query("""
        SELECT * FROM local_term
        WHERE tenant_id = :tenantId
          AND (:sourceSystem IS NULL OR source_system = :sourceSystem)
          AND (:category IS NULL OR category = :category)
          AND (:status IS NULL OR status = :status)
          AND (:keyword IS NULL OR LOWER(local_name) LIKE :keyword OR LOWER(local_code) LIKE :keyword)
        ORDER BY updated_at DESC, id DESC
        OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
        """)
    List<LocalTerm> pageByFilter(String tenantId, String sourceSystem, String category, String status,
                                 String keyword, int offset, int limit);
}
