package com.medkernel.engine.recommendation;

import java.util.List;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendationFeedbackRepository extends ListCrudRepository<RecommendationFeedback, Long> {

    List<RecommendationFeedback> findByCardIdAndTenantIdOrderByCreatedAtAsc(String cardId, String tenantId);
}
