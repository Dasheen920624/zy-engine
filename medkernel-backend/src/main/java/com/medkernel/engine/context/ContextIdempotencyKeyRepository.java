package com.medkernel.engine.context;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContextIdempotencyKeyRepository extends ListCrudRepository<ContextIdempotencyKey, Long> {

    Optional<ContextIdempotencyKey> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);
}
