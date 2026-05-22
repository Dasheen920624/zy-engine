package com.medkernel.datagovernance.service;

import com.medkernel.datagovernance.entity.*;
import com.medkernel.datagovernance.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 数据治理服务 - 统一管理主数据、数据字典和数据质量
 */
@Service
public class DataGovernanceService {
    private static final Logger log = LoggerFactory.getLogger(DataGovernanceService.class);

    private final PatientService patientService;
    private final DoctorService doctorService;
    private final DepartmentService departmentService;
    private final QualityRuleRepository qualityRuleRepository;
    private final QualityCheckRepository qualityCheckRepository;

    public DataGovernanceService(PatientService patientService,
                                 DoctorService doctorService,
                                 DepartmentService departmentService,
                                 QualityRuleRepository qualityRuleRepository,
                                 QualityCheckRepository qualityCheckRepository) {
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.departmentService = departmentService;
        this.qualityRuleRepository = qualityRuleRepository;
        this.qualityCheckRepository = qualityCheckRepository;
    }

    /**
     * 获取数据治理概览
     */
    public Map<String, Object> getOverview(String tenantId) {
        Map<String, Object> overview = new LinkedHashMap<>();
        
        // 主数据统计
        Map<String, Object> masterDataStats = new LinkedHashMap<>();
        masterDataStats.put("patient_count", patientService.findAllByTenantId(tenantId).size());
        masterDataStats.put("doctor_count", doctorService.findAllByTenantId(tenantId).size());
        masterDataStats.put("department_count", departmentService.findAllByTenantId(tenantId).size());
        overview.put("master_data", masterDataStats);
        
        // 数据质量统计
        Map<String, Object> qualityStats = new LinkedHashMap<>();
        List<QualityRuleEntity> rules = qualityRuleRepository.findAllByTenantId(tenantId);
        qualityStats.put("rule_count", rules.size());
        qualityStats.put("active_rule_count", rules.stream().filter(r -> "ACTIVE".equals(r.getStatus())).count());
        overview.put("data_quality", qualityStats);
        
        overview.put("generated_time", LocalDateTime.now());
        
        return overview;
    }

    /**
     * 执行数据质量检查
     */
    public Map<String, Object> executeQualityCheck(String tenantId, String ruleCode) {
        QualityRuleEntity rule = qualityRuleRepository.findByRuleCode(tenantId, ruleCode);
        if (rule == null) {
            throw new IllegalArgumentException("Quality rule not found: " + ruleCode);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rule_code", ruleCode);
        result.put("rule_name", rule.getRuleName());
        result.put("target_entity", rule.getTargetEntity());
        result.put("check_time", LocalDateTime.now());

        // 根据规则类型执行不同的检查逻辑
        List<Map<String, Object>> checkResults = new ArrayList<>();
        switch (rule.getRuleType()) {
            case "COMPLETENESS":
                checkResults = checkCompleteness(tenantId, rule);
                break;
            case "ACCURACY":
                checkResults = checkAccuracy(tenantId, rule);
                break;
            case "CONSISTENCY":
                checkResults = checkConsistency(tenantId, rule);
                break;
            default:
                log.warn("Unknown rule type: {}", rule.getRuleType());
        }

        result.put("total_checked", checkResults.size());
        result.put("passed", checkResults.stream().filter(r -> "PASS".equals(r.get("result"))).count());
        result.put("failed", checkResults.stream().filter(r -> "FAIL".equals(r.get("result"))).count());
        result.put("details", checkResults);

        return result;
    }

    /**
     * 完整性检查
     */
    private List<Map<String, Object>> checkCompleteness(String tenantId, QualityRuleEntity rule) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        // 根据目标实体执行不同的完整性检查
        switch (rule.getTargetEntity()) {
            case "md_patient":
                List<PatientEntity> patients = patientService.findAllByTenantId(tenantId);
                for (PatientEntity patient : patients) {
                    Map<String, Object> checkResult = new LinkedHashMap<>();
                    checkResult.put("id", patient.getPatientId());
                    checkResult.put("entity", "md_patient");
                    
                    boolean passed = isComplete(patient, rule.getTargetField());
                    checkResult.put("result", passed ? "PASS" : "FAIL");
                    if (!passed) {
                        checkResult.put("error", "Missing field: " + rule.getTargetField());
                    }
                    
                    // 保存检查记录
                    saveCheckRecord(tenantId, rule.getRuleCode(), "md_patient", 
                                   patient.getPatientId(), passed ? "PASS" : "FAIL", 
                                   passed ? null : "Missing field: " + rule.getTargetField());
                    
                    results.add(checkResult);
                }
                break;
            // 其他实体的检查...
        }
        
        return results;
    }

    /**
     * 准确性检查
     */
    private List<Map<String, Object>> checkAccuracy(String tenantId, QualityRuleEntity rule) {
        // 实现准确性检查逻辑
        return new ArrayList<>();
    }

    /**
     * 一致性检查
     */
    private List<Map<String, Object>> checkConsistency(String tenantId, QualityRuleEntity rule) {
        // 实现一致性检查逻辑
        return new ArrayList<>();
    }

    private boolean isComplete(PatientEntity patient, String field) {
        switch (field) {
            case "patient_name":
                return patient.getPatientName() != null && !patient.getPatientName().isEmpty();
            case "gender":
                return patient.getGender() != null && !patient.getGender().isEmpty();
            case "birth_date":
                return patient.getBirthDate() != null;
            // 其他字段...
            default:
                return true;
        }
    }

    private void saveCheckRecord(String tenantId, String ruleCode, String targetEntity, 
                                String targetId, String result, String errorMessage) {
        QualityCheckEntity checkEntity = new QualityCheckEntity();
        checkEntity.setTenantId(tenantId);
        checkEntity.setCheckId(UUID.randomUUID().toString());
        checkEntity.setRuleCode(ruleCode);
        checkEntity.setTargetEntity(targetEntity);
        checkEntity.setTargetId(targetId);
        checkEntity.setCheckResult(result);
        checkEntity.setErrorMessage(errorMessage);
        checkEntity.setCheckTime(LocalDateTime.now());
        checkEntity.setCreatedTime(LocalDateTime.now());
        
        qualityCheckRepository.save(checkEntity);
    }
}