package com.medkernel.engine.context;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * 临床上下文接入幂等键（Idempotency Key）仓储接口。
 *
 * <p>持久化记录客户端请求的幂等特征，防止同一就诊或患者上下文中，
 * 由于重试等网络行为导致的并发重复写入，保证底层医学数据的一致性。
 */
@Repository
public interface ContextIdempotencyKeyRepository extends ListCrudRepository<ContextIdempotencyKey, Long> {

    Optional<ContextIdempotencyKey> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);
}
