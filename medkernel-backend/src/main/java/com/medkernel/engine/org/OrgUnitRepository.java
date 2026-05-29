package com.medkernel.engine.org;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * 组织单元 Spring Data JDBC 仓库。
 *
 * <p>所有方法都按 {@code tenantId} 强制过滤；不提供未带租户的全表方法，避免误用造成跨租户泄漏。
 */
@Repository
public interface OrgUnitRepository extends ListCrudRepository<OrgUnit, Long> {

    Optional<OrgUnit> findByTenantIdAndCode(String tenantId, String code);

    List<OrgUnit> findByTenantIdAndLevelOrderByCodeAsc(String tenantId, OrgLevel level);

    List<OrgUnit> findByTenantIdOrderByLevelAscCodeAsc(String tenantId);

    List<OrgUnit> findByTenantIdAndParentIdOrderByCodeAsc(String tenantId, Long parentId);

    @Query("SELECT COUNT(*) FROM org_unit WHERE tenant_id = :tenantId")
    long countByTenantId(String tenantId);

    /**
     * 平台级：列出所有租户根组织（level=TENANT）。本方法**跨租户**，仅供平台开通/管理入口
     * （{@code tenant.write}/{@code tenant.read} 守卫）使用，不得用于租户内业务查询。
     */
    @Query("SELECT * FROM org_unit WHERE level_code = 'TENANT' ORDER BY created_at DESC")
    List<OrgUnit> findAllTenantRoots();

    @Query("""
        SELECT * FROM org_unit
        WHERE tenant_id = :tenantId
        ORDER BY level_code, code
        OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
        """)
    List<OrgUnit> pageByTenantId(String tenantId, int offset, int limit);
}
