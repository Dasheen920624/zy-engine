package com.medkernel.engine.evaluation;

import java.util.List;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RectificationReviewRepository extends ListCrudRepository<RectificationReview, Long> {

    List<RectificationReview> findByFindingIdAndTenantIdOrderByReviewedAtAsc(String findingId, String tenantId);
}
