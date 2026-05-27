package com.medkernel.engine.recommendation;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendationFatigueSignalRepository extends ListCrudRepository<RecommendationFatigueSignal, Long> {

    List<RecommendationFatigueSignal> findByCardIdAndTenantIdOrderByCreatedAtAsc(String cardId, String tenantId);

    List<RecommendationFatigueSignal> findByTriggerIdAndTenantIdOrderByCreatedAtAsc(String triggerId, String tenantId);

    @Query("""
        SELECT *
        FROM recommendation_fatigue_signal
        WHERE tenant_id = :tenantId
          AND (:fatigueKey IS NULL OR fatigue_key = :fatigueKey)
          AND (:signalType IS NULL OR signal_type = :signalType)
        ORDER BY created_at DESC, id DESC
        LIMIT :limit OFFSET :offset
        """)
    List<RecommendationFatigueSignal> pageByFilter(String tenantId, String fatigueKey, String signalType,
                                                   int offset, int limit);
}
