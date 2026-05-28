package com.medkernel.engine.context;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * 临床事件原始载荷（Payload）仓储接口。
 *
 * <p>用于物理存储大字段 JSON 格式的原始临床事件报文，
 * 隔离高性能的主表索引查询，便于异常溯源、批量回放和模型分析。
 */
@Repository
public interface ClinicalEventPayloadRepository extends ListCrudRepository<ClinicalEventPayload, Long> {

    Optional<ClinicalEventPayload> findByEventIdAndTenantId(String eventId, String tenantId);
}
