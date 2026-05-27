package com.medkernel.engine.evaluation;

import java.util.List;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvaluationResultRepository extends ListCrudRepository<EvaluationResult, Long> {

    List<EvaluationResult> findByRunIdAndTenantIdOrderByCreatedAtAsc(String runId, String tenantId);

    List<EvaluationResult> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}
