package com.medkernel.engine.evaluation;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * 整改任务持久化仓库（GA-ENG-API-08）。
 *
 * <p>按质控问题定位当前整改任务，用于提交整改、复核流转和问题详情展示。
 */
@Repository
public interface RectificationTaskRepository extends ListCrudRepository<RectificationTask, Long> {

    /**
     * 按质控问题 ID 与租户 ID 查询对应整改任务。
     */
    Optional<RectificationTask> findByFindingIdAndTenantId(String findingId, String tenantId);
}
