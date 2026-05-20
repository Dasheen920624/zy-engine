package com.medkernel.patient;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * MPI模块REST控制器：提供患者标识管理、就诊标识管理、冲突处理等API接口。
 */
@RestController
@RequestMapping("/api/v1/mpi")
public class MpiController {

    private final MpiService mpiService;

    public MpiController(MpiService mpiService) {
        this.mpiService = mpiService;
    }

    // ============================================================================
    // 患者标识管理
    // ============================================================================

    /**
     * 注册患者标识。
     */
    @PostMapping("/patient-identities")
    public ResponseEntity<PatientIdentity> registerPatientIdentity(@RequestBody Map<String, String> request) {
        String tenantId = request.get("tenantId");
        String platformPatientId = request.get("platformPatientId");
        String identityType = request.get("identityType");
        String externalId = request.get("externalId");
        String sourceSystem = request.get("sourceSystem");

        if (tenantId == null || platformPatientId == null || identityType == null || externalId == null || sourceSystem == null) {
            return ResponseEntity.badRequest().build();
        }

        PatientIdentity identity = mpiService.registerPatientIdentity(tenantId, platformPatientId, 
                                                                      identityType, externalId, sourceSystem);
        return ResponseEntity.ok(identity);
    }

    /**
     * 批量注册患者标识。
     */
    @PostMapping("/patient-identities/batch")
    public ResponseEntity<Map<String, Object>> batchRegisterPatientIdentities(@RequestBody Map<String, Object> request) {
        String tenantId = (String) request.get("tenantId");
        String platformPatientId = (String) request.get("platformPatientId");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> identities = (List<Map<String, String>>) request.get("identities");

        if (tenantId == null || platformPatientId == null || identities == null) {
            return ResponseEntity.badRequest().build();
        }

        int count = mpiService.batchRegisterPatientIdentities(tenantId, platformPatientId, identities);
        
        Map<String, Object> response = new HashMap<>();
        response.put("registeredCount", count);
        response.put("platformPatientId", platformPatientId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 查找患者的所有标识。
     */
    @GetMapping("/patient-identities/{tenantId}/{platformPatientId}")
    public ResponseEntity<List<PatientIdentity>> findPatientIdentities(@PathVariable String tenantId, 
                                                                       @PathVariable String platformPatientId) {
        List<PatientIdentity> identities = mpiService.findPatientIdentities(tenantId, platformPatientId);
        return ResponseEntity.ok(identities);
    }

    /**
     * 通过外部标识查找患者。
     */
    @GetMapping("/patient-identities/external")
    public ResponseEntity<PatientIdentity> findPatientByExternalId(@RequestParam String tenantId,
                                                                   @RequestParam String identityType,
                                                                   @RequestParam String sourceSystem,
                                                                   @RequestParam String externalId) {
        PatientIdentity identity = mpiService.findPatientByExternalId(tenantId, identityType, sourceSystem, externalId);
        if (identity == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(identity);
    }

    /**
     * 验证患者标识。
     */
    @PostMapping("/patient-identities/{identityId}/verify")
    public ResponseEntity<Void> verifyPatientIdentity(@PathVariable Long identityId, 
                                                      @RequestBody Map<String, String> request) {
        String verifiedBy = request.get("verifiedBy");
        if (verifiedBy == null) {
            return ResponseEntity.badRequest().build();
        }
        
        mpiService.verifyPatientIdentity(identityId, verifiedBy);
        return ResponseEntity.ok().build();
    }

    /**
     * 合并患者标识。
     */
    @PostMapping("/patient-identities/merge")
    public ResponseEntity<Void> mergePatientIdentities(@RequestBody Map<String, Object> request) {
        Long sourceId = Long.valueOf(request.get("sourceId").toString());
        Long targetId = Long.valueOf(request.get("targetId").toString());
        String mergedBy = (String) request.get("mergedBy");

        if (sourceId == null || targetId == null || mergedBy == null) {
            return ResponseEntity.badRequest().build();
        }

        mpiService.mergePatientIdentities(sourceId, targetId, mergedBy);
        return ResponseEntity.ok().build();
    }

    // ============================================================================
    // 就诊标识管理
    // ============================================================================

    /**
     * 注册就诊标识。
     */
    @PostMapping("/visit-identities")
    public ResponseEntity<VisitIdentity> registerVisitIdentity(@RequestBody Map<String, String> request) {
        String tenantId = request.get("tenantId");
        String platformVisitId = request.get("platformVisitId");
        String platformPatientId = request.get("platformPatientId");
        String visitType = request.get("visitType");
        String identityType = request.get("identityType");
        String externalId = request.get("externalId");
        String sourceSystem = request.get("sourceSystem");
        String visitDateStr = request.get("visitDate");
        String departmentCode = request.get("departmentCode");

        if (tenantId == null || platformVisitId == null || platformPatientId == null || 
            visitType == null || identityType == null || externalId == null || sourceSystem == null) {
            return ResponseEntity.badRequest().build();
        }

        LocalDate visitDate = visitDateStr != null ? LocalDate.parse(visitDateStr) : null;

        VisitIdentity identity = mpiService.registerVisitIdentity(tenantId, platformVisitId, platformPatientId,
                                                                  visitType, identityType, externalId, 
                                                                  sourceSystem, visitDate, departmentCode);
        return ResponseEntity.ok(identity);
    }

    /**
     * 查找就诊的所有标识。
     */
    @GetMapping("/visit-identities/{tenantId}/{platformVisitId}")
    public ResponseEntity<List<VisitIdentity>> findVisitIdentities(@PathVariable String tenantId, 
                                                                   @PathVariable String platformVisitId) {
        List<VisitIdentity> identities = mpiService.findVisitIdentities(tenantId, platformVisitId);
        return ResponseEntity.ok(identities);
    }

    /**
     * 查找患者的所有就诊标识。
     */
    @GetMapping("/visit-identities/patient/{tenantId}/{platformPatientId}")
    public ResponseEntity<List<VisitIdentity>> findPatientVisitIdentities(@PathVariable String tenantId, 
                                                                          @PathVariable String platformPatientId) {
        List<VisitIdentity> identities = mpiService.findPatientVisitIdentities(tenantId, platformPatientId);
        return ResponseEntity.ok(identities);
    }

    /**
     * 通过外部标识查找就诊。
     */
    @GetMapping("/visit-identities/external")
    public ResponseEntity<VisitIdentity> findVisitByExternalId(@RequestParam String tenantId,
                                                               @RequestParam String identityType,
                                                               @RequestParam String sourceSystem,
                                                               @RequestParam String externalId) {
        VisitIdentity identity = mpiService.findVisitByExternalId(tenantId, identityType, sourceSystem, externalId);
        if (identity == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(identity);
    }

    // ============================================================================
    // 冲突处理
    // ============================================================================

    /**
     * 检测冲突。
     */
    @PostMapping("/conflicts/detect/{tenantId}")
    public ResponseEntity<List<IdentityConflict>> detectConflicts(@PathVariable String tenantId) {
        List<IdentityConflict> conflicts = mpiService.detectConflicts(tenantId);
        return ResponseEntity.ok(conflicts);
    }

    /**
     * 获取待处理冲突。
     */
    @GetMapping("/conflicts/pending/{tenantId}")
    public ResponseEntity<List<IdentityConflict>> getPendingConflicts(@PathVariable String tenantId) {
        List<IdentityConflict> conflicts = mpiService.getPendingConflicts(tenantId);
        return ResponseEntity.ok(conflicts);
    }

    /**
     * 解决冲突。
     */
    @PostMapping("/conflicts/{conflictId}/resolve")
    public ResponseEntity<Void> resolveConflict(@PathVariable Long conflictId, 
                                                @RequestBody Map<String, Object> request) {
        String resolutionType = (String) request.get("resolutionType");
        String resolutionNotes = (String) request.get("resolutionNotes");
        String resolvedBy = (String) request.get("resolvedBy");
        Object targetIdObj = request.get("targetPatientIdentityId");
        Long targetPatientIdentityId = targetIdObj != null ? Long.valueOf(targetIdObj.toString()) : null;

        if (resolutionType == null || resolvedBy == null) {
            return ResponseEntity.badRequest().build();
        }

        mpiService.resolveConflict(conflictId, resolutionType, resolutionNotes, resolvedBy, targetPatientIdentityId);
        return ResponseEntity.ok().build();
    }

    // ============================================================================
    // 适配器同步
    // ============================================================================

    /**
     * 从外部系统同步患者标识。
     */
    @PostMapping("/sync/patients")
    public ResponseEntity<Map<String, Object>> syncPatientIdentities(@RequestBody Map<String, String> request) {
        String tenantId = request.get("tenantId");
        String adapterCode = request.get("adapterCode");
        String queryCode = request.get("queryCode");

        if (tenantId == null || adapterCode == null || queryCode == null) {
            return ResponseEntity.badRequest().build();
        }

        int syncCount = mpiService.syncPatientIdentitiesFromAdapter(tenantId, adapterCode, queryCode);
        
        Map<String, Object> response = new HashMap<>();
        response.put("syncedCount", syncCount);
        response.put("adapterCode", adapterCode);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 从外部系统同步就诊标识。
     */
    @PostMapping("/sync/visits")
    public ResponseEntity<Map<String, Object>> syncVisitIdentities(@RequestBody Map<String, String> request) {
        String tenantId = request.get("tenantId");
        String adapterCode = request.get("adapterCode");
        String queryCode = request.get("queryCode");

        if (tenantId == null || adapterCode == null || queryCode == null) {
            return ResponseEntity.badRequest().build();
        }

        int syncCount = mpiService.syncVisitIdentitiesFromAdapter(tenantId, adapterCode, queryCode);
        
        Map<String, Object> response = new HashMap<>();
        response.put("syncedCount", syncCount);
        response.put("adapterCode", adapterCode);
        
        return ResponseEntity.ok(response);
    }
}