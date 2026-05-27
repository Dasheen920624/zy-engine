package com.medkernel.engine.recommendation;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * 推荐触发事实持久化仓库，按 trigger_id 查询触发。
 */
@Repository
public interface RecommendationTriggerRepository extends ListCrudRepository<RecommendationTrigger, Long> {

    Optional<RecommendationTrigger> findByTriggerIdAndTenantId(String triggerId, String tenantId);
}
