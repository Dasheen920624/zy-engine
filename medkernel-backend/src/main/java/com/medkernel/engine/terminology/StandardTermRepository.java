package com.medkernel.engine.terminology;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StandardTermRepository extends ListCrudRepository<StandardTerm, Long> {

    Optional<StandardTerm> findByTenantIdAndId(String tenantId, Long id);

    @Query("""
        SELECT COUNT(*) FROM standard_term
        WHERE tenant_id = :tenantId
          AND (:standardSystem IS NULL OR standard_system = :standardSystem)
          AND (:category IS NULL OR category = :category)
          AND (:status IS NULL OR status = :status)
          AND (:keyword IS NULL OR LOWER(display_name) LIKE :keyword OR LOWER(term_code) LIKE :keyword)
        """)
    long countByFilter(String tenantId, String standardSystem, String category, String status, String keyword);

    @Query("""
        SELECT * FROM standard_term
        WHERE tenant_id = :tenantId
          AND (:standardSystem IS NULL OR standard_system = :standardSystem)
          AND (:category IS NULL OR category = :category)
          AND (:status IS NULL OR status = :status)
          AND (:keyword IS NULL OR LOWER(display_name) LIKE :keyword OR LOWER(term_code) LIKE :keyword)
        ORDER BY updated_at DESC, id DESC
        OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
        """)
    List<StandardTerm> pageByFilter(String tenantId, String standardSystem, String category, String status,
                                    String keyword, int offset, int limit);
}
