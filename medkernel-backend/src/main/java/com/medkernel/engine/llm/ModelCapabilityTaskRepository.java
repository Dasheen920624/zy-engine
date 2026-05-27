package com.medkernel.engine.llm;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
  * 模型网关调用任务数据访问存储库。
  */
@Repository
public interface ModelCapabilityTaskRepository extends CrudRepository<ModelCapabilityTask, Long> {

    /**
      * 根据任务唯一ID查询调用任务详情。
      *
      * @param taskId 任务唯一ID
      * @return 任务实体
      */
    Optional<ModelCapabilityTask> findByTaskId(String taskId);
}
