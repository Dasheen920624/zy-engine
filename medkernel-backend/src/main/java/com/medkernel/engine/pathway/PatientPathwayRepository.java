package com.medkernel.engine.pathway;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * 患者路径实例仓库。
 *
 * <p>保存患者与已发布路径模板的运行实例、当前节点、状态和退出/完成事实。
 */
@Repository
public interface PatientPathwayRepository extends ListCrudRepository<PatientPathway, Long> {

    /**
     * 按患者路径业务 ID 和租户查询运行实例。
     */
    Optional<PatientPathway> findByPatientPathwayIdAndTenantId(String patientPathwayId, String tenantId);

    /**
     * 查询模板下的患者路径实例，并按入径时间倒序排列。
     */
    List<PatientPathway> findByTemplateIdAndTenantIdOrderByEnteredAtDesc(String templateId, String tenantId);
}
