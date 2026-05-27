package com.medkernel.engine.evaluation;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * 评估质控幂等键持久化仓库（GA-ENG-API-08）。
 *
 * <p>按租户、操作类型和幂等键查询首次成功闭环写入，用于整改提交和复核接口的重放与冲突检测。
 */
@Repository
public interface EvaluationIdempotencyKeyRepository extends ListCrudRepository<EvaluationIdempotencyKey, Long> {

    /**
     * 查询指定租户下某闭环操作的幂等键记录；同键异文由服务层比较请求摘要后拒绝。
     */
    Optional<EvaluationIdempotencyKey> findByTenantIdAndOperationTypeAndIdempotencyKey(
        String tenantId, EvaluationIdempotencyOperation operationType, String idempotencyKey);
}
