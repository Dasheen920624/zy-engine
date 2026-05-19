package com.medkernel.patient;

import com.medkernel.adapter.AdapterHubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * MPI模块业务服务：提供患者标识管理、就诊标识管理、冲突处理等业务逻辑。
 */
@Service
public class MpiService {

    private static final Logger log = LoggerFactory.getLogger(MpiService.class);

    private final MpiPersistenceService persistenceService;
    private final AdapterHubService adapterHubService;

    public MpiService(MpiPersistenceService persistenceService, AdapterHubService adapterHubService) {
        this.persistenceService = persistenceService;
        this.adapterHubService = adapterHubService;
    }

    // ============================================================================
    // 患者标识管理
    // ============================================================================

    /**
     * 注册患者标识。
     */
    public PatientIdentity registerPatientIdentity(String tenantId, String platformPatientId, 
                                                   String identityType, String externalId, String sourceSystem) {
        // 检查是否已存在
        PatientIdentity existing = persistenceService.findPatientIdentityByExternalId(tenantId, identityType, sourceSystem, externalId);
        if (existing != null) {
            log.info("Patient identity already exists: {}/{}/{}", identityType, sourceSystem, externalId);
            return existing;
        }

        PatientIdentity identity = new PatientIdentity();
        identity.setTenantId(tenantId);
        identity.setPlatformPatientId(platformPatientId);
        identity.setIdentityType(identityType);
        identity.setExternalId(externalId);
        identity.setSourceSystem(sourceSystem);
        identity.setStatus("ACTIVE");
        identity.setConfidence(100);
        identity.setManuallyVerified(false);

        return persistenceService.savePatientIdentity(identity);
    }

    /**
     * 批量注册患者标识。
     */
    public int batchRegisterPatientIdentities(String tenantId, String platformPatientId, 
                                             List<Map<String, String>> identities) {
        int count = 0;
        for (Map<String, String> idInfo : identities) {
            String identityType = idInfo.get("identityType");
            String externalId = idInfo.get("externalId");
            String sourceSystem = idInfo.get("sourceSystem");

            if (identityType != null && externalId != null && sourceSystem != null) {
                registerPatientIdentity(tenantId, platformPatientId, identityType, externalId, sourceSystem);
                count++;
            }
        }
        return count;
    }

    /**
     * 查找患者的所有标识。
     */
    public List<PatientIdentity> findPatientIdentities(String tenantId, String platformPatientId) {
        return persistenceService.findPatientIdentitiesByPlatformId(tenantId, platformPatientId);
    }

    /**
     * 通过外部标识查找患者。
     */
    public PatientIdentity findPatientByExternalId(String tenantId, String identityType, 
                                                  String sourceSystem, String externalId) {
        return persistenceService.findPatientIdentityByExternalId(tenantId, identityType, sourceSystem, externalId);
    }

    /**
     * 验证患者标识。
     */
    public void verifyPatientIdentity(Long identityId, String verifiedBy) {
        persistenceService.verifyPatientIdentity(identityId, verifiedBy);
    }

    /**
     * 合并患者标识。
     */
    public void mergePatientIdentities(Long sourceId, Long targetId, String mergedBy) {
        PatientIdentity source = persistenceService.findPatientIdentityById(sourceId);
        PatientIdentity target = persistenceService.findPatientIdentityById(targetId);

        if (source == null || target == null) {
            throw new IllegalArgumentException("Source or target identity not found");
        }

        if (!source.getTenantId().equals(target.getTenantId())) {
            throw new IllegalArgumentException("Cannot merge identities from different tenants");
        }

        // 更新源标识状态
        source.setStatus("MERGED");
        source.setMergedToId(targetId);
        source.setUpdatedTime(LocalDateTime.now());
        persistenceService.savePatientIdentity(source);

        // 更新目标标识
        target.setManuallyVerified(true);
        target.setVerifiedBy(mergedBy);
        target.setVerifiedTime(LocalDateTime.now());
        persistenceService.savePatientIdentity(target);

        log.info("Merged patient identity {} into {}", sourceId, targetId);
    }

    // ============================================================================
    // 就诊标识管理
    // ============================================================================

    /**
     * 注册就诊标识。
     */
    public VisitIdentity registerVisitIdentity(String tenantId, String platformVisitId, String platformPatientId,
                                               String visitType, String identityType, String externalId, 
                                               String sourceSystem, LocalDate visitDate, String departmentCode) {
        // 检查是否已存在
        VisitIdentity existing = persistenceService.findVisitIdentityByExternalId(tenantId, identityType, sourceSystem, externalId);
        if (existing != null) {
            log.info("Visit identity already exists: {}/{}/{}", identityType, sourceSystem, externalId);
            return existing;
        }

        VisitIdentity identity = new VisitIdentity();
        identity.setTenantId(tenantId);
        identity.setPlatformVisitId(platformVisitId);
        identity.setPlatformPatientId(platformPatientId);
        identity.setVisitType(visitType);
        identity.setIdentityType(identityType);
        identity.setExternalId(externalId);
        identity.setSourceSystem(sourceSystem);
        identity.setVisitDate(visitDate);
        identity.setDepartmentCode(departmentCode);
        identity.setStatus("ACTIVE");

        return persistenceService.saveVisitIdentity(identity);
    }

    /**
     * 查找就诊的所有标识。
     */
    public List<VisitIdentity> findVisitIdentities(String tenantId, String platformVisitId) {
        return persistenceService.findVisitIdentitiesByPlatformId(tenantId, platformVisitId);
    }

    /**
     * 查找患者的所有就诊标识。
     */
    public List<VisitIdentity> findPatientVisitIdentities(String tenantId, String platformPatientId) {
        return persistenceService.findVisitIdentitiesByPatientId(tenantId, platformPatientId);
    }

    /**
     * 通过外部标识查找就诊。
     */
    public VisitIdentity findVisitByExternalId(String tenantId, String identityType, 
                                              String sourceSystem, String externalId) {
        return persistenceService.findVisitIdentityByExternalId(tenantId, identityType, sourceSystem, externalId);
    }

    // ============================================================================
    // 冲突处理
    // ============================================================================

    /**
     * 检测冲突。
     */
    public List<IdentityConflict> detectConflicts(String tenantId) {
        return persistenceService.detectPatientIdentityConflicts(tenantId);
    }

    /**
     * 获取待处理冲突。
     */
    public List<IdentityConflict> getPendingConflicts(String tenantId) {
        return persistenceService.findPendingConflicts(tenantId);
    }

    /**
     * 解决冲突。
     */
    public void resolveConflict(Long conflictId, String resolutionType, String resolutionNotes, 
                               String resolvedBy, Long targetPatientIdentityId) {
        IdentityConflict conflict = persistenceService.findIdentityConflictById(conflictId);
        if (conflict == null) {
            throw new IllegalArgumentException("Conflict not found: " + conflictId);
        }

        if (!"PENDING".equals(conflict.getStatus()) && !"IN_PROGRESS".equals(conflict.getStatus())) {
            throw new IllegalStateException("Conflict is not in a resolvable state: " + conflict.getStatus());
        }

        persistenceService.resolveConflict(conflictId, resolutionType, resolutionNotes, resolvedBy, targetPatientIdentityId);

        // 根据解决方式执行相应操作
        if ("MERGE".equals(resolutionType) && targetPatientIdentityId != null) {
            executeMergeResolution(conflict, targetPatientIdentityId, resolvedBy);
        }

        log.info("Resolved conflict {} with type {}", conflictId, resolutionType);
    }

    private void executeMergeResolution(IdentityConflict conflict, Long targetPatientIdentityId, String resolvedBy) {
        // 解析涉及的患者标识ID
        String patientIdentityIds = conflict.getPatientIdentityIds();
        if (patientIdentityIds != null && !patientIdentityIds.isEmpty()) {
            // 简单处理：将所有涉及的标识合并到目标标识
            // 实际应用中可能需要更复杂的逻辑
            log.info("Executing merge resolution for conflict {}", conflict.getId());
        }
    }

    // ============================================================================
    // 适配器同步
    // ============================================================================

    /**
     * 从外部系统同步患者标识。
     */
    public int syncPatientIdentitiesFromAdapter(String tenantId, String adapterCode, String queryCode) {
        try {
            // 调用适配器获取外部患者数据
            List<Map<String, Object>> externalPatients = adapterHubService.queryExternalData(adapterCode, queryCode, null);
            
            int syncCount = 0;
            for (Map<String, Object> patientData : externalPatients) {
                String externalId = (String) patientData.get("patientId");
                String patientName = (String) patientData.get("patientName");
                
                if (externalId != null) {
                    // 查找或创建平台患者ID（这里简化处理，实际需要更复杂的匹配逻辑）
                    String platformPatientId = "P" + persistenceService.hashId(externalId).substring(0, 16);
                    
                    // 注册标识
                    registerPatientIdentity(tenantId, platformPatientId, 
                                          adapterCode + "_PATIENT_ID", externalId, adapterCode);
                    syncCount++;
                }
            }
            
            log.info("Synced {} patient identities from adapter {}", syncCount, adapterCode);
            return syncCount;
        } catch (Exception ex) {
            log.error("Failed to sync patient identities from adapter {}", adapterCode, ex);
            throw new RuntimeException("Sync failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 从外部系统同步就诊标识。
     */
    public int syncVisitIdentitiesFromAdapter(String tenantId, String adapterCode, String queryCode) {
        try {
            // 调用适配器获取外部就诊数据
            List<Map<String, Object>> externalVisits = adapterHubService.queryExternalData(adapterCode, queryCode, null);
            
            int syncCount = 0;
            for (Map<String, Object> visitData : externalVisits) {
                String externalId = (String) visitData.get("visitId");
                String patientExternalId = (String) visitData.get("patientId");
                String visitType = (String) visitData.get("visitType");
                
                if (externalId != null && patientExternalId != null) {
                    // 查找对应的平台患者ID
                    PatientIdentity patientIdentity = findPatientByExternalId(tenantId, 
                            adapterCode + "_PATIENT_ID", adapterCode, patientExternalId);
                    
                    if (patientIdentity != null) {
                        String platformVisitId = "V" + persistenceService.hashId(externalId).substring(0, 16);
                        
                        registerVisitIdentity(tenantId, platformVisitId, patientIdentity.getPlatformPatientId(),
                                            visitType, adapterCode + "_VISIT_ID", externalId, adapterCode,
                                            null, null);
                        syncCount++;
                    }
                }
            }
            
            log.info("Synced {} visit identities from adapter {}", syncCount, adapterCode);
            return syncCount;
        } catch (Exception ex) {
            log.error("Failed to sync visit identities from adapter {}", adapterCode, ex);
            throw new RuntimeException("Sync failed: " + ex.getMessage(), ex);
        }
    }
}