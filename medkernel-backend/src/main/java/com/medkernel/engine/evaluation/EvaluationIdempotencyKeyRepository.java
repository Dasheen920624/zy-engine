package com.medkernel.engine.evaluation;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvaluationIdempotencyKeyRepository extends ListCrudRepository<EvaluationIdempotencyKey, Long> {

    Optional<EvaluationIdempotencyKey> findByTenantIdAndOperationTypeAndIdempotencyKey(
        String tenantId, EvaluationIdempotencyOperation operationType, String idempotencyKey);
}
