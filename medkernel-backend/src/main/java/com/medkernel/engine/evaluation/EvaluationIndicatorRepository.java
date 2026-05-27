package com.medkernel.engine.evaluation;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvaluationIndicatorRepository extends ListCrudRepository<EvaluationIndicator, Long> {

    Optional<EvaluationIndicator> findByIndicatorIdAndTenantId(String indicatorId, String tenantId);

    List<EvaluationIndicator> findByTenantIdOrderByUpdatedAtDesc(String tenantId);

    List<EvaluationIndicator> findByTenantIdAndIndicatorCodeAndStatus(
        String tenantId, String indicatorCode, EvaluationIndicatorStatus status);

    @Query("""
        SELECT COUNT(*)
        FROM evaluation_indicator
        WHERE tenant_id = :tenantId
          AND (:status IS NULL OR status = :status)
          AND (:subjectType IS NULL OR subject_type = :subjectType)
          AND (:indicatorCode IS NULL OR indicator_code = :indicatorCode)
        """)
    long countByFilter(String tenantId, String status, String subjectType, String indicatorCode);

    @Query("""
        SELECT *
        FROM evaluation_indicator
        WHERE tenant_id = :tenantId
          AND (:status IS NULL OR status = :status)
          AND (:subjectType IS NULL OR subject_type = :subjectType)
          AND (:indicatorCode IS NULL OR indicator_code = :indicatorCode)
        ORDER BY updated_at DESC, id DESC
        LIMIT :limit OFFSET :offset
        """)
    List<EvaluationIndicator> pageByFilter(
        String tenantId, String status, String subjectType, String indicatorCode, int offset, int limit);
}
