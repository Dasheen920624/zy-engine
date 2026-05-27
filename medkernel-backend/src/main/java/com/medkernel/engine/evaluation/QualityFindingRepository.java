package com.medkernel.engine.evaluation;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QualityFindingRepository extends ListCrudRepository<QualityFinding, Long> {

    Optional<QualityFinding> findByFindingIdAndTenantId(String findingId, String tenantId);

    List<QualityFinding> findByResultIdAndTenantIdOrderByCreatedAtAsc(String resultId, String tenantId);

    List<QualityFinding> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    @Query("""
        SELECT COUNT(*)
        FROM quality_finding
        WHERE tenant_id = :tenantId
          AND (:severity IS NULL OR severity = :severity)
          AND (:status IS NULL OR status = :status)
          AND (:departmentId IS NULL OR responsible_department_id = :departmentId)
        """)
    long countByFilter(String tenantId, String severity, String status, String departmentId);

    @Query("""
        SELECT *
        FROM quality_finding
        WHERE tenant_id = :tenantId
          AND (:severity IS NULL OR severity = :severity)
          AND (:status IS NULL OR status = :status)
          AND (:departmentId IS NULL OR responsible_department_id = :departmentId)
        ORDER BY created_at DESC, id DESC
        LIMIT :limit OFFSET :offset
        """)
    List<QualityFinding> pageByFilter(
        String tenantId, String severity, String status, String departmentId, int offset, int limit);
}
