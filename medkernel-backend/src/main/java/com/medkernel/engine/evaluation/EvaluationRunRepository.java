package com.medkernel.engine.evaluation;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvaluationRunRepository extends ListCrudRepository<EvaluationRun, Long> {

    Optional<EvaluationRun> findByRunIdAndTenantId(String runId, String tenantId);
}
