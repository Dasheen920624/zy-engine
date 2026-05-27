package com.medkernel.engine.pathway;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * 专病包仓库。
 *
 * <p>保存路径模板的专病资产包身份、病种、版本、状态和发布来源。
 */
@Repository
public interface SpecialtyPackageRepository extends ListCrudRepository<SpecialtyPackage, Long> {

    /**
     * 按专病包业务 ID 和租户查询专病包。
     */
    Optional<SpecialtyPackage> findByPackageIdAndTenantId(String packageId, String tenantId);

    /**
     * 按租户、专病包编码和版本查询专病包，用于版本唯一性判断。
     */
    Optional<SpecialtyPackage> findByTenantIdAndPackageCodeAndPackageVersion(
        String tenantId, String packageCode, String packageVersion);

    /**
     * 查询租户下所有专病包，并按更新时间倒序排列。
     */
    List<SpecialtyPackage> findByTenantIdOrderByUpdatedAtDesc(String tenantId);

    /**
     * 分页查询租户下的专病包。
     */
    @Query("""
        SELECT * FROM specialty_package
        WHERE tenant_id = :tenantId
        ORDER BY updated_at DESC, id DESC
        OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
        """)
    List<SpecialtyPackage> pageByTenantId(String tenantId, int offset, int limit);

    /**
     * 统计租户下专病包总数。
     */
    @Query("""
        SELECT COUNT(*) FROM specialty_package
        WHERE tenant_id = :tenantId
        """)
    long countByTenantId(String tenantId);
}
