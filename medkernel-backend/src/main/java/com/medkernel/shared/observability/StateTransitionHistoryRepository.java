package com.medkernel.shared.observability;

import java.util.List;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * 引擎实体状态流转历史（State Transition History）仓储接口。
 *
 * <p>用于存储与追溯医学引擎中关键业务对象（如临床事件、评估任务、随访等）的状态流转历史轨迹，
 * 支撑 GA-ENG-OBS-01 引擎可观测性骨干的多维度医学轨迹解释与可视化审计追踪。
 */
@Repository
public interface StateTransitionHistoryRepository
    extends ListCrudRepository<StateTransitionHistory, Long> {

    List<StateTransitionHistory> findByEntityTypeAndEntityIdOrderByOccurredAtAsc(
        String entityType, String entityId);

    List<StateTransitionHistory> findByTraceIdOrderByOccurredAtAsc(String traceId);
}
