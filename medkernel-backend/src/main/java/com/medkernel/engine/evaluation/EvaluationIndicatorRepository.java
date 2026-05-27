package com.medkernel.engine.evaluation;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvaluationIndicatorRepository extends ListCrudRepository<EvaluationIndicator, Long> {

    Optional<EvaluationIndicator> findByIndicatorIdAndTenantId(String indicatorId, String tenantId);

    List<EvaluationIndicator> findByTenantIdOrderByUpdatedAtDesc(String tenantId);

    List<EvaluationIndicator> findByTenantIdAndIndicatorCodeAndStatus(
        String tenantId, String indicatorCode, EvaluationIndicatorStatus status);
}
