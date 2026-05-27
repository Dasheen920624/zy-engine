package com.medkernel.engine.pathway;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * 关键时钟仓库。
 *
 * <p>按租户隔离读取患者路径节点的开始、到期、完成和状态事实。
 */
@Repository
public interface ClinicalClockRepository extends ListCrudRepository<ClinicalClock, Long> {

    /**
     * 按业务 ID 和租户查询单个关键时钟。
     */
    Optional<ClinicalClock> findByClockIdAndTenantId(String clockId, String tenantId);

    /**
     * 查询患者路径实例下所有关键时钟，并按启动时间升序排列。
     */
    List<ClinicalClock> findByPatientPathwayIdAndTenantIdOrderByStartedAtAsc(
        String patientPathwayId, String tenantId);
}
