package com.medkernel.engine.llm;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
  * 场景模型路由与脱敏配置策略数据访问存储库。
  */
@Repository
public interface ModelCapabilityPolicyRepository extends CrudRepository<ModelCapabilityPolicy, Long> {

    /**
      * 根据租户ID和能力编码唯一获取对应的路由策略与脱敏配置。
      *
      * @param tenantId 租户ID
      * @param capabilityCode 能力代码
      * @return 路由脱敏策略
      */
    Optional<ModelCapabilityPolicy> findByTenantIdAndCapabilityCode(String tenantId, String capabilityCode);
}
