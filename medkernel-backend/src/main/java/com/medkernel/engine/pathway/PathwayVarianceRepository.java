package com.medkernel.engine.pathway;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * 路径变异仓库。
 *
 * <p>保存患者路径执行中因医学、患者、资源、医生选择或系统原因产生的偏离事实。
 */
@Repository
public interface PathwayVarianceRepository extends ListCrudRepository<PathwayVariance, Long> {

    /**
     * 按业务 ID 和租户查询单条路径变异记录。
     */
    Optional<PathwayVariance> findByVarianceIdAndTenantId(String varianceId, String tenantId);

    /**
     * 查询患者路径实例下所有变异记录，并按创建时间升序排列。
     */
    List<PathwayVariance> findByPatientPathwayIdAndTenantIdOrderByCreatedAtAsc(
        String patientPathwayId, String tenantId);
}
