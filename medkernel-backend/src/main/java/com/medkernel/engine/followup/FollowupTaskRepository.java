package com.medkernel.engine.followup;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * 随访任务存储库。
 */
public interface FollowupTaskRepository extends CrudRepository<FollowupTask, Long>, PagingAndSortingRepository<FollowupTask, Long> {
    Optional<FollowupTask> findByTaskId(String taskId);
    List<FollowupTask> findByTenantIdAndPlanId(String tenantId, String planId);
}
