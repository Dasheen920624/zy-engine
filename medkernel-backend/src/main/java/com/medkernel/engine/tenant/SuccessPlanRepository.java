package com.medkernel.engine.tenant;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * 租户客户成功多维生命周期 Spring Data JDBC 仓库。
 *
 * <p>所有访问逻辑均强制绑定租户上下文，规避跨租户泄漏风险。
 */
@Repository
public interface SuccessPlanRepository extends ListCrudRepository<SuccessPlan, Long> {

    Optional<SuccessPlan> findByTenantId(String tenantId);
}
