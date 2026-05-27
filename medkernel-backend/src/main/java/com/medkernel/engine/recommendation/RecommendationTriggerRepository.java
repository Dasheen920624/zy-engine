package com.medkernel.engine.recommendation;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendationTriggerRepository extends ListCrudRepository<RecommendationTrigger, Long> {

    Optional<RecommendationTrigger> findByTriggerIdAndTenantId(String triggerId, String tenantId);
}
