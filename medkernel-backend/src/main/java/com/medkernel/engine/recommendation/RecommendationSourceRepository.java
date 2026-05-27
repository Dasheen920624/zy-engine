package com.medkernel.engine.recommendation;

import java.util.List;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendationSourceRepository extends ListCrudRepository<RecommendationSource, Long> {

    List<RecommendationSource> findByCardIdAndTenantIdOrderByCreatedAtAsc(String cardId, String tenantId);
}
