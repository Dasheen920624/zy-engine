package com.medkernel.shared.observability;

import java.util.List;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StateTransitionHistoryRepository
    extends ListCrudRepository<StateTransitionHistory, Long> {

    List<StateTransitionHistory> findByEntityTypeAndEntityIdOrderByOccurredAtAsc(
        String entityType, String entityId);

    List<StateTransitionHistory> findByTraceIdOrderByOccurredAtAsc(String traceId);
}
