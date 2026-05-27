package com.medkernel.engine.pkg;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * 同步日志 Repository 接口。
 */
@Repository
public interface SyncLogRepository extends ListCrudRepository<SyncLog, Long> {

    Optional<SyncLog> findByLogIdAndTenantId(String logId, String tenantId);

    List<SyncLog> findByTenantIdAndPlanId(String tenantId, String planId);
}
