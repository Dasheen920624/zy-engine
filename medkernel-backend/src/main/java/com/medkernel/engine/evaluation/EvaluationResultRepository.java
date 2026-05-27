package com.medkernel.engine.evaluation;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvaluationResultRepository extends ListCrudRepository<EvaluationResult, Long> {

    List<EvaluationResult> findByRunIdAndTenantIdOrderByCreatedAtAsc(String runId, String tenantId);

    List<EvaluationResult> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    @Query("""
        SELECT COUNT(*)
        FROM evaluation_result
        WHERE tenant_id = :tenantId
          AND (:indicatorCode IS NULL OR indicator_code = :indicatorCode)
          AND (:resultLevel IS NULL OR result_level = :resultLevel)
          AND (:departmentId IS NULL OR responsible_department_id = :departmentId)
        """)
    long countByFilter(String tenantId, String indicatorCode, String resultLevel, String departmentId);

    @Query("""
        SELECT *
        FROM evaluation_result
        WHERE tenant_id = :tenantId
          AND (:indicatorCode IS NULL OR indicator_code = :indicatorCode)
          AND (:resultLevel IS NULL OR result_level = :resultLevel)
          AND (:departmentId IS NULL OR responsible_department_id = :departmentId)
        ORDER BY created_at DESC, id DESC
        LIMIT :limit OFFSET :offset
        """)
    List<EvaluationResult> pageByFilter(
        String tenantId, String indicatorCode, String resultLevel, String departmentId, int offset, int limit);
}
