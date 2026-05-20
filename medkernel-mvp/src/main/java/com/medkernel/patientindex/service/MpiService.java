package com.medkernel.patientindex.service;

import com.medkernel.common.OrgDefaults;
import com.medkernel.common.TraceContext;
import com.medkernel.patientindex.entity.MpiPatientIndexEntity;
import com.medkernel.patientindex.entity.MpiIdentifierMappingEntity;
import com.medkernel.patientindex.entity.MpiEncounterEntity;
import com.medkernel.patientindex.entity.MpiInsuranceSettlementEntity;
import com.medkernel.patientindex.entity.MpiIdentifierConflictEntity;
import com.medkernel.patientindex.util.MpiHashUtil;
import com.medkernel.persistence.EnginePersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 患者主索引（MPI）核心服务
 * 支持患者标识映射、脱敏引用、hash 匹配和冲突处理
 */
@Service("patientIndexMpiService")
public class MpiService {
    private static final Logger logger = LoggerFactory.getLogger(MpiService.class);
    
    private final EnginePersistenceService persistenceService;
    
    // 内存缓存（生产环境应使用数据库）
    private final Map<String, MpiPatientIndexEntity> patientIndexCache = new ConcurrentHashMap<>();
    private final Map<String, List<MpiIdentifierMappingEntity>> identifierMappingCache = new ConcurrentHashMap<>();
    private final Map<String, List<MpiEncounterEntity>> encounterCache = new ConcurrentHashMap<>();
    private final Map<String, List<MpiInsuranceSettlementEntity>> insuranceSettlementCache = new ConcurrentHashMap<>();
    private final Map<String, MpiIdentifierConflictEntity> conflictCache = new ConcurrentHashMap<>();
    
    public MpiService(EnginePersistenceService persistenceService) {
        this.persistenceService = persistenceService;
        seedSampleData();
    }
    
    /**
     * 创建或更新患者主索引
     */
    public Map<String, Object> createOrUpdatePatient(Map<String, Object> request, String tenantId, String hospitalCode) {
        long start = System.currentTimeMillis();
        String patientName = required(request, "patient_name");
        String gender = optional(request, "gender");
        LocalDate birthDate = parseDate(optional(request, "birth_date"));
        String idCardNo = optional(request, "id_card_no");
        String phone = optional(request, "phone");
        
        // 计算 hash
        String patientNameHash = MpiHashUtil.hash(patientName);
        String birthDateHash = birthDate != null ? MpiHashUtil.hash(birthDate.toString()) : null;
        String idCardNoHash = idCardNo != null ? MpiHashUtil.hash(idCardNo) : null;
        String phoneHash = phone != null ? MpiHashUtil.hash(phone) : null;
        
        // 尝试匹配现有患者
        MpiPatientIndexEntity existingPatient = findPatientByHash(tenantId, patientNameHash, birthDateHash, idCardNoHash);
        
        MpiPatientIndexEntity patient;
        boolean isNew = false;
        
        if (existingPatient != null) {
            // 更新现有患者
            patient = existingPatient;
            if (gender != null) patient.setGender(gender);
            if (phoneHash != null) patient.setPhoneHash(phoneHash);
            patient.setUpdatedTime(LocalDateTime.now());
        } else {
            // 创建新患者
            patient = new MpiPatientIndexEntity();
            patient.setTenantId(tenantId);
            patient.setMpiId(generateMpiId());
            patient.setPatientName(MpiHashUtil.maskName(patientName));
            patient.setPatientNameHash(patientNameHash);
            patient.setGender(gender);
            patient.setBirthDate(birthDate);
            patient.setBirthDateHash(birthDateHash);
            patient.setIdCardNoHash(idCardNoHash);
            patient.setPhoneHash(phoneHash);
            patient.setStatus("ACTIVE");
            patient.setCreatedTime(LocalDateTime.now());
            patient.setUpdatedTime(LocalDateTime.now());
            isNew = true;
        }
        
        // 保存
        patientIndexCache.put(tenantId + "::" + patient.getMpiId(), patient);
        
        // 审计
        auditPatientOperation(isNew ? "CREATE" : "UPDATE", tenantId, patient.getMpiId(), patientName);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mpi_id", patient.getMpiId());
        result.put("patient_name_masked", patient.getPatientName());
        result.put("gender", patient.getGender());
        result.put("birth_date", patient.getBirthDate());
        result.put("status", patient.getStatus());
        result.put("is_new", isNew);
        result.put("elapsed_ms", System.currentTimeMillis() - start);
        result.put("trace_id", TraceContext.getTraceId());
        
        return result;
    }
    
    /**
     * 添加标识映射
     */
    public Map<String, Object> addIdentifierMapping(Map<String, Object> request, String tenantId) {
        long start = System.currentTimeMillis();
        String mpiId = required(request, "mpi_id");
        String sourceSystem = required(request, "source_system");
        String identifierType = required(request, "identifier_type");
        String identifierValue = required(request, "identifier_value");
        Boolean isPrimary = optionalBool(request, "is_primary");
        
        // 验证患者存在
        MpiPatientIndexEntity patient = patientIndexCache.get(tenantId + "::" + mpiId);
        if (patient == null) {
            throw new IllegalArgumentException("Patient not found: " + mpiId);
        }
        
        // 计算 hash 和脱敏值
        String identifierHash = MpiHashUtil.hash(identifierValue);
        String identifierMasked = maskIdentifier(identifierType, identifierValue);
        
        // 创建映射
        MpiIdentifierMappingEntity mapping = new MpiIdentifierMappingEntity();
        mapping.setTenantId(tenantId);
        mapping.setMpiId(mpiId);
        mapping.setSourceSystem(sourceSystem.toUpperCase());
        mapping.setIdentifierType(identifierType.toUpperCase());
        mapping.setIdentifierValue(identifierValue); // 实际应加密存储
        mapping.setIdentifierHash(identifierHash);
        mapping.setIdentifierMasked(identifierMasked);
        mapping.setIsPrimary(isPrimary != null ? isPrimary : false);
        mapping.setStatus("ACTIVE");
        mapping.setCreatedTime(LocalDateTime.now());
        mapping.setUpdatedTime(LocalDateTime.now());
        
        // 保存
        String cacheKey = tenantId + "::" + mpiId;
        identifierMappingCache.computeIfAbsent(cacheKey, k -> new ArrayList<>()).add(mapping);
        
        // 审计
        auditIdentifierOperation("ADD_MAPPING", tenantId, mpiId, sourceSystem, identifierType);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mpi_id", mpiId);
        result.put("source_system", mapping.getSourceSystem());
        result.put("identifier_type", mapping.getIdentifierType());
        result.put("identifier_masked", identifierMasked);
        result.put("is_primary", mapping.getIsPrimary());
        result.put("status", mapping.getStatus());
        result.put("elapsed_ms", System.currentTimeMillis() - start);
        result.put("trace_id", TraceContext.getTraceId());
        
        return result;
    }
    
    /**
     * 查询患者主索引
     */
    public Map<String, Object> queryPatient(Map<String, Object> request, String tenantId) {
        long start = System.currentTimeMillis();
        String mpiId = optional(request, "mpi_id");
        String patientName = optional(request, "patient_name");
        String birthDate = optional(request, "birth_date");
        String idCardNo = optional(request, "id_card_no");
        String phone = optional(request, "phone");
        
        MpiPatientIndexEntity patient = null;
        
        if (mpiId != null) {
            // 按 MPI ID 查询
            patient = patientIndexCache.get(tenantId + "::" + mpiId);
        } else if (patientName != null || idCardNo != null || phone != null) {
            // 按 hash 匹配查询
            String nameHash = patientName != null ? MpiHashUtil.hash(patientName) : null;
            String birthDateHash = birthDate != null ? MpiHashUtil.hash(birthDate) : null;
            String idCardHash = idCardNo != null ? MpiHashUtil.hash(idCardNo) : null;
            String phoneHash = phone != null ? MpiHashUtil.hash(phone) : null;
            patient = findPatientByHash(tenantId, nameHash, birthDateHash, idCardHash);
        }
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("trace_id", TraceContext.getTraceId());
        result.put("elapsed_ms", System.currentTimeMillis() - start);
        
        if (patient != null) {
            result.put("found", true);
            result.put("mpi_id", patient.getMpiId());
            result.put("patient_name_masked", patient.getPatientName());
            result.put("gender", patient.getGender());
            result.put("birth_date", patient.getBirthDate());
            result.put("status", patient.getStatus());
            
            // 查询关联的标识映射
            List<MpiIdentifierMappingEntity> mappings = identifierMappingCache.get(tenantId + "::" + patient.getMpiId());
            if (mappings != null) {
                List<Map<String, Object>> mappingViews = new ArrayList<>();
                for (MpiIdentifierMappingEntity mapping : mappings) {
                    Map<String, Object> view = new LinkedHashMap<>();
                    view.put("source_system", mapping.getSourceSystem());
                    view.put("identifier_type", mapping.getIdentifierType());
                    view.put("identifier_masked", mapping.getIdentifierMasked());
                    view.put("is_primary", mapping.getIsPrimary());
                    mappingViews.add(view);
                }
                result.put("identifier_mappings", mappingViews);
            }
            
            // 查询关联的就诊记录
            List<MpiEncounterEntity> encounters = encounterCache.get(tenantId + "::" + patient.getMpiId());
            if (encounters != null) {
                List<Map<String, Object>> encounterViews = new ArrayList<>();
                for (MpiEncounterEntity encounter : encounters) {
                    Map<String, Object> view = new LinkedHashMap<>();
                    view.put("encounter_id", encounter.getEncounterId());
                    view.put("encounter_type", encounter.getEncounterType());
                    view.put("hospital_code", encounter.getHospitalCode());
                    view.put("admission_time", encounter.getAdmissionTime());
                    view.put("discharge_time", encounter.getDischargeTime());
                    view.put("status", encounter.getStatus());
                    encounterViews.add(view);
                }
                result.put("encounters", encounterViews);
            }
        } else {
            result.put("found", false);
            result.put("message", "未找到匹配的患者记录");
        }
        
        return result;
    }
    
    /**
     * 创建就诊记录
     */
    public Map<String, Object> createEncounter(Map<String, Object> request, String tenantId) {
        long start = System.currentTimeMillis();
        String mpiId = required(request, "mpi_id");
        String encounterId = required(request, "encounter_id");
        String encounterType = required(request, "encounter_type");
        String hospitalCode = required(request, "hospital_code");
        String departmentCode = optional(request, "department_code");
        String attendingDoctorId = optional(request, "attending_doctor_id");
        String diagnosisCode = optional(request, "diagnosis_code");
        String diagnosisName = optional(request, "diagnosis_name");
        
        // 验证患者存在
        MpiPatientIndexEntity patient = patientIndexCache.get(tenantId + "::" + mpiId);
        if (patient == null) {
            throw new IllegalArgumentException("Patient not found: " + mpiId);
        }
        
        // 创建就诊记录
        MpiEncounterEntity encounter = new MpiEncounterEntity();
        encounter.setTenantId(tenantId);
        encounter.setMpiId(mpiId);
        encounter.setEncounterId(encounterId);
        encounter.setEncounterType(encounterType.toUpperCase());
        encounter.setHospitalCode(hospitalCode);
        encounter.setDepartmentCode(departmentCode);
        encounter.setAdmissionTime(LocalDateTime.now());
        encounter.setAttendingDoctorId(attendingDoctorId);
        encounter.setDiagnosisCode(diagnosisCode);
        encounter.setDiagnosisName(diagnosisName);
        encounter.setStatus("ACTIVE");
        encounter.setCreatedTime(LocalDateTime.now());
        encounter.setUpdatedTime(LocalDateTime.now());
        
        // 保存
        String cacheKey = tenantId + "::" + mpiId;
        encounterCache.computeIfAbsent(cacheKey, k -> new ArrayList<>()).add(encounter);
        
        // 审计
        auditEncounterOperation("CREATE", tenantId, mpiId, encounterId, encounterType);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mpi_id", mpiId);
        result.put("encounter_id", encounterId);
        result.put("encounter_type", encounter.getEncounterType());
        result.put("hospital_code", hospitalCode);
        result.put("status", encounter.getStatus());
        result.put("elapsed_ms", System.currentTimeMillis() - start);
        result.put("trace_id", TraceContext.getTraceId());
        
        return result;
    }
    
    /**
     * 处理标识冲突
     */
    public Map<String, Object> resolveConflict(Map<String, Object> request, String tenantId) {
        long start = System.currentTimeMillis();
        Long conflictId = requiredLong(request, "conflict_id");
        String resolution = required(request, "resolution");
        String resolutionNotes = optional(request, "resolution_notes");
        String resolvedBy = optional(request, "resolved_by");
        
        // 查找冲突记录
        MpiIdentifierConflictEntity conflict = findConflictById(tenantId, conflictId);
        if (conflict == null) {
            throw new IllegalArgumentException("Conflict not found: " + conflictId);
        }
        
        // 更新冲突解决状态
        conflict.setResolution(resolution.toUpperCase());
        conflict.setResolutionNotes(resolutionNotes);
        conflict.setResolvedBy(resolvedBy);
        conflict.setResolvedTime(LocalDateTime.now());
        conflict.setUpdatedTime(LocalDateTime.now());
        
        // 如果是合并操作，更新患者状态
        if ("MERGED".equalsIgnoreCase(resolution) && conflict.getExistingMpiId() != null && conflict.getNewMpiId() != null) {
            MpiPatientIndexEntity targetPatient = patientIndexCache.get(tenantId + "::" + conflict.getExistingMpiId());
            MpiPatientIndexEntity sourcePatient = patientIndexCache.get(tenantId + "::" + conflict.getNewMpiId());
            
            if (targetPatient != null && sourcePatient != null) {
                sourcePatient.setStatus("MERGED");
                sourcePatient.setMergeTargetMpiId(conflict.getExistingMpiId());
                sourcePatient.setUpdatedTime(LocalDateTime.now());
                
                // 迁移标识映射
                List<MpiIdentifierMappingEntity> sourceMappings = identifierMappingCache.get(tenantId + "::" + conflict.getNewMpiId());
                if (sourceMappings != null) {
                    for (MpiIdentifierMappingEntity mapping : sourceMappings) {
                        mapping.setMpiId(conflict.getExistingMpiId());
                        mapping.setUpdatedTime(LocalDateTime.now());
                    }
                    identifierMappingCache.computeIfAbsent(tenantId + "::" + conflict.getExistingMpiId(), k -> new ArrayList<>()).addAll(sourceMappings);
                    identifierMappingCache.remove(tenantId + "::" + conflict.getNewMpiId());
                }
            }
        }
        
        // 审计
        auditConflictOperation("RESOLVE", tenantId, conflictId, resolution);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conflict_id", conflictId);
        result.put("resolution", conflict.getResolution());
        result.put("resolved_by", conflict.getResolvedBy());
        result.put("resolved_time", conflict.getResolvedTime());
        result.put("elapsed_ms", System.currentTimeMillis() - start);
        result.put("trace_id", TraceContext.getTraceId());
        
        return result;
    }
    
    /**
     * 列出所有患者主索引
     */
    public List<Map<String, Object>> listPatients(String tenantId) {
        String prefix = tenantId + "::";
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Map.Entry<String, MpiPatientIndexEntity> entry : patientIndexCache.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                MpiPatientIndexEntity patient = entry.getValue();
                Map<String, Object> view = new LinkedHashMap<>();
                view.put("mpi_id", patient.getMpiId());
                view.put("patient_name_masked", patient.getPatientName());
                view.put("gender", patient.getGender());
                view.put("birth_date", patient.getBirthDate());
                view.put("status", patient.getStatus());
                view.put("created_time", patient.getCreatedTime());
                result.add(view);
            }
        }
        
        return result;
    }
    
    /**
     * 列出待处理的标识冲突
     */
    public List<Map<String, Object>> listPendingConflicts(String tenantId) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (MpiIdentifierConflictEntity conflict : conflictCache.values()) {
            if (tenantId.equals(conflict.getTenantId()) && "PENDING".equals(conflict.getResolution())) {
                Map<String, Object> view = new LinkedHashMap<>();
                view.put("conflict_id", conflict.getId());
                view.put("conflict_type", conflict.getConflictType());
                view.put("source_system", conflict.getSourceSystem());
                view.put("identifier_type", conflict.getIdentifierType());
                view.put("existing_mpi_id", conflict.getExistingMpiId());
                view.put("new_mpi_id", conflict.getNewMpiId());
                view.put("created_time", conflict.getCreatedTime());
                result.add(view);
            }
        }
        
        return result;
    }
    
    // ==================== 私有方法 ====================
    
    private MpiPatientIndexEntity findPatientByHash(String tenantId, String nameHash, String birthDateHash, String idCardHash) {
        for (MpiPatientIndexEntity patient : patientIndexCache.values()) {
            if (!tenantId.equals(patient.getTenantId()) || !"ACTIVE".equals(patient.getStatus())) {
                continue;
            }
            
            boolean nameMatch = nameHash != null && nameHash.equals(patient.getPatientNameHash());
            boolean birthMatch = birthDateHash != null && birthDateHash.equals(patient.getBirthDateHash());
            boolean idCardMatch = idCardHash != null && idCardHash.equals(patient.getIdCardNoHash());
            
            // 至少两个字段匹配才算同一个人
            if ((nameMatch && birthMatch) || (nameMatch && idCardMatch) || (birthMatch && idCardMatch)) {
                return patient;
            }
        }
        return null;
    }
    
    private MpiIdentifierConflictEntity findConflictById(String tenantId, Long conflictId) {
        for (MpiIdentifierConflictEntity conflict : conflictCache.values()) {
            if (tenantId.equals(conflict.getTenantId()) && conflictId.equals(conflict.getId())) {
                return conflict;
            }
        }
        return null;
    }
    
    private String generateMpiId() {
        return "MPI_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
    
    private String maskIdentifier(String identifierType, String value) {
        if (value == null) return null;
        switch (identifierType.toUpperCase()) {
            case "PATIENT_NAME":
                return MpiHashUtil.maskName(value);
            case "ID_CARD_NO":
                return MpiHashUtil.maskIdCard(value);
            case "PHONE":
                return MpiHashUtil.maskPhone(value);
            default:
                return value;
        }
    }
    
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            logger.warn("Failed to parse date: {}", dateStr);
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    private String required(Map<String, Object> request, String field) {
        Object value = request.get(field);
        if (value == null || value.toString().trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.toString().trim();
    }
    
    @SuppressWarnings("unchecked")
    private String optional(Map<String, Object> request, String field) {
        Object value = request.get(field);
        return value != null ? value.toString().trim() : null;
    }
    
    @SuppressWarnings("unchecked")
    private Boolean optionalBool(Map<String, Object> request, String field) {
        Object value = request.get(field);
        if (value instanceof Boolean) return (Boolean) value;
        if (value != null) return Boolean.parseBoolean(value.toString());
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private Long requiredLong(Map<String, Object> request, String field) {
        Object value = request.get(field);
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " must be a number");
        }
    }
    
    private void seedSampleData() {
        String tenant = OrgDefaults.DEFAULT_TENANT_ID;
        
        // 创建示例患者
        MpiPatientIndexEntity patient1 = new MpiPatientIndexEntity();
        patient1.setTenantId(tenant);
        patient1.setMpiId("MPI_SAMPLE_001");
        patient1.setPatientName("张*");
        patient1.setPatientNameHash(MpiHashUtil.hash("张三"));
        patient1.setGender("男");
        patient1.setBirthDate(LocalDate.of(1990, 1, 1));
        patient1.setBirthDateHash(MpiHashUtil.hash("1990-01-01"));
        patient1.setIdCardNoHash(MpiHashUtil.hash("110101199001011234"));
        patient1.setPhoneHash(MpiHashUtil.hash("13812345678"));
        patient1.setStatus("ACTIVE");
        patient1.setCreatedTime(LocalDateTime.now());
        patient1.setUpdatedTime(LocalDateTime.now());
        patientIndexCache.put(tenant + "::MPI_SAMPLE_001", patient1);
        
        // 添加标识映射
        MpiIdentifierMappingEntity mapping1 = new MpiIdentifierMappingEntity();
        mapping1.setTenantId(tenant);
        mapping1.setMpiId("MPI_SAMPLE_001");
        mapping1.setSourceSystem("HIS");
        mapping1.setIdentifierType("PATIENT_ID");
        mapping1.setIdentifierValue("HIS_P001");
        mapping1.setIdentifierHash(MpiHashUtil.hash("HIS_P001"));
        mapping1.setIdentifierMasked("HIS_P001");
        mapping1.setIsPrimary(true);
        mapping1.setStatus("ACTIVE");
        mapping1.setCreatedTime(LocalDateTime.now());
        mapping1.setUpdatedTime(LocalDateTime.now());
        identifierMappingCache.computeIfAbsent(tenant + "::MPI_SAMPLE_001", k -> new ArrayList<>()).add(mapping1);
        
        // 添加就诊记录
        MpiEncounterEntity encounter1 = new MpiEncounterEntity();
        encounter1.setTenantId(tenant);
        encounter1.setMpiId("MPI_SAMPLE_001");
        encounter1.setEncounterId("E_SAMPLE_001");
        encounter1.setEncounterType("OUTPATIENT");
        encounter1.setHospitalCode("ZYHOSPITAL");
        encounter1.setDepartmentCode("DEPT_001");
        encounter1.setAdmissionTime(LocalDateTime.now().minusDays(7));
        encounter1.setAttendingDoctorId("DOC_001");
        encounter1.setDiagnosisCode("J06.9");
        encounter1.setDiagnosisName("急性上呼吸道感染");
        encounter1.setStatus("DISCHARGED");
        encounter1.setCreatedTime(LocalDateTime.now());
        encounter1.setUpdatedTime(LocalDateTime.now());
        encounterCache.computeIfAbsent(tenant + "::MPI_SAMPLE_001", k -> new ArrayList<>()).add(encounter1);
        
        logger.info("MPI sample data seeded: 1 patient, 1 identifier mapping, 1 encounter");
    }
    
    private void auditPatientOperation(String operation, String tenantId, String mpiId, String patientName) {
        try {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("operation", operation);
            detail.put("mpi_id", mpiId);
            detail.put("patient_name_masked", MpiHashUtil.maskName(patientName));
            persistenceService.saveAuditLog("MPI", operation, "MPI_PATIENT_" + operation,
                    mpiId, null, null, null, detail);
        } catch (Exception e) {
            logger.warn("[traceId={}] MPI audit log failed: {}", TraceContext.getTraceId(), e.getMessage());
        }
    }
    
    private void auditIdentifierOperation(String operation, String tenantId, String mpiId, String sourceSystem, String identifierType) {
        try {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("operation", operation);
            detail.put("mpi_id", mpiId);
            detail.put("source_system", sourceSystem);
            detail.put("identifier_type", identifierType);
            persistenceService.saveAuditLog("MPI", operation, "MPI_IDENTIFIER_" + operation,
                    mpiId, null, null, null, detail);
        } catch (Exception e) {
            logger.warn("[traceId={}] MPI audit log failed: {}", TraceContext.getTraceId(), e.getMessage());
        }
    }
    
    private void auditEncounterOperation(String operation, String tenantId, String mpiId, String encounterId, String encounterType) {
        try {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("operation", operation);
            detail.put("mpi_id", mpiId);
            detail.put("encounter_id", encounterId);
            detail.put("encounter_type", encounterType);
            persistenceService.saveAuditLog("MPI", operation, "MPI_ENCOUNTER_" + operation,
                    mpiId, null, null, null, detail);
        } catch (Exception e) {
            logger.warn("[traceId={}] MPI audit log failed: {}", TraceContext.getTraceId(), e.getMessage());
        }
    }
    
    private void auditConflictOperation(String operation, String tenantId, Long conflictId, String resolution) {
        try {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("operation", operation);
            detail.put("conflict_id", conflictId);
            detail.put("resolution", resolution);
            persistenceService.saveAuditLog("MPI", operation, "MPI_CONFLICT_" + operation,
                    conflictId.toString(), null, null, null, detail);
        } catch (Exception e) {
            logger.warn("[traceId={}] MPI audit log failed: {}", TraceContext.getTraceId(), e.getMessage());
        }
    }
}
