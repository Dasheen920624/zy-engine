package com.medkernel.datagovernance.service;

import com.medkernel.datagovernance.entity.PatientEntity;
import com.medkernel.datagovernance.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 患者主数据服务
 */
@Service
public class PatientService {
    private static final Logger log = LoggerFactory.getLogger(PatientService.class);

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    /**
     * 保存患者主数据
     */
    public PatientEntity save(PatientEntity entity) {
        if (entity.getCreatedTime() == null) {
            entity.setCreatedTime(LocalDateTime.now());
        }
        entity.setUpdatedTime(LocalDateTime.now());
        patientRepository.save(entity);
        return entity;
    }

    /**
     * 根据患者ID查找患者
     */
    public PatientEntity findByPatientId(String tenantId, String patientId) {
        return patientRepository.findByPatientId(tenantId, patientId);
    }

    /**
     * 根据租户ID查找所有患者
     */
    public List<PatientEntity> findAllByTenantId(String tenantId) {
        return patientRepository.findAllByTenantId(tenantId);
    }

    /**
     * 批量导入患者数据
     */
    public int batchImport(String tenantId, List<Map<String, Object>> dataList) {
        int successCount = 0;
        for (Map<String, Object> data : dataList) {
            try {
                PatientEntity entity = convertToEntity(tenantId, data);
                save(entity);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to import patient: {}", data, e);
            }
        }
        return successCount;
    }

    private PatientEntity convertToEntity(String tenantId, Map<String, Object> data) {
        PatientEntity entity = new PatientEntity();
        entity.setTenantId(tenantId);
        entity.setPatientId((String) data.get("patient_id"));
        entity.setPatientName((String) data.get("patient_name"));
        entity.setGender((String) data.get("gender"));
        // 其他字段转换...
        return entity;
    }
}