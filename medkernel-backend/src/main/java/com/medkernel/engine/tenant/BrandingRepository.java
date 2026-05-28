package com.medkernel.engine.tenant;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * 租户定制品牌信息 Spring Data JDBC 仓库。
 *
 * <p>所有访问逻辑均强制绑定租户上下文，规避跨租户泄漏风险。
 */
@Repository
public interface BrandingRepository extends ListCrudRepository<Branding, Long> {

    Optional<Branding> findByTenantId(String tenantId);
}
