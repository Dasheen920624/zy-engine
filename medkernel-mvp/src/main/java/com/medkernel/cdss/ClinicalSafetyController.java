package com.medkernel.cdss;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 临床安全风险管理 REST API（RISK-001）。
 *
 * <p>提供：
 * <ul>
 *   <li>危险日志管理 — POST/PUT/GET /api/clinical-safety/hazards</li>
 *   <li>风险接受 — POST /api/clinical-safety/hazards/{hazardId}/accept</li>
 *   <li>风险关闭 — POST /api/clinical-safety/hazards/{hazardId}/close</li>
 *   <li>安全案例管理 — POST/PUT/GET /api/clinical-safety/safety-cases</li>
 *   <li>安全案例审核 — POST /api/clinical-safety/safety-cases/{caseId}/review</li>
 *   <li>风险摘要 — GET /api/clinical-safety/risk-summary</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/clinical-safety")
public class ClinicalSafetyController {

    private final ClinicalSafetyService clinicalSafetyService;
    private final OrganizationContextService organizationContextService;

    public ClinicalSafetyController(ClinicalSafetyService clinicalSafetyService,
                                    OrganizationContextService organizationContextService) {
        this.clinicalSafetyService = clinicalSafetyService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 创建危险日志。
     */
    @PostMapping("/hazards")
    public ApiResult<HazardLog> createHazard(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);

        HazardLog hazard = new HazardLog();
        hazard.setTenantId(parseLong(orgContext.getTenantId()));
        hazard.setHazardCode(string(request.get("hazard_code")));
        hazard.setHazardName(string(request.get("hazard_name")));
        hazard.setHazardCategory(string(request.get("hazard_category")));
        hazard.setHazardDescription(string(request.get("hazard_description")));
        hazard.setAffectedProcess(string(request.get("affected_process")));
        hazard.setLikelihood(string(request.get("likelihood")));
        hazard.setSeverity(string(request.get("severity")));
        hazard.setControlMeasures(string(request.get("control_measures")));
        hazard.setResidualRisk(string(request.get("residual_risk")));
        hazard.setBlockingStrategy(string(request.get("blocking_strategy")));
        hazard.setCreatedBy(string(request.get("created_by")));

        if (hazard.getHazardCode() == null || hazard.getHazardCode().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "hazard_code is required");
        }

        // 自动计算风险等级
        if (hazard.getLikelihood() != null && hazard.getSeverity() != null) {
            hazard.setRiskLevel(clinicalSafetyService.calculateRiskLevel(hazard.getLikelihood(), hazard.getSeverity()));
        }

        // 自动设置阻断策略
        if (hazard.getRiskLevel() != null && (hazard.getBlockingStrategy() == null || hazard.getBlockingStrategy().isEmpty())) {
            hazard.setBlockingStrategy(clinicalSafetyService.getBlockingStrategy(hazard.getRiskLevel()));
        }

        HazardLog saved = clinicalSafetyService.createHazard(hazard);
        return ApiResult.success(saved);
    }

    /**
     * 更新危险日志。
     */
    @PutMapping("/hazards/{hazardId}")
    public ApiResult<HazardLog> updateHazard(
            @PathVariable Long hazardId,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);

        HazardLog hazard = new HazardLog();
        hazard.setId(hazardId);
        hazard.setTenantId(parseLong(orgContext.getTenantId()));
        hazard.setHazardName(string(request.get("hazard_name")));
        hazard.setHazardCategory(string(request.get("hazard_category")));
        hazard.setHazardDescription(string(request.get("hazard_description")));
        hazard.setAffectedProcess(string(request.get("affected_process")));
        hazard.setLikelihood(string(request.get("likelihood")));
        hazard.setSeverity(string(request.get("severity")));
        hazard.setControlMeasures(string(request.get("control_measures")));
        hazard.setResidualRisk(string(request.get("residual_risk")));
        hazard.setStatus(string(request.get("status")));
        hazard.setBlockingStrategy(string(request.get("blocking_strategy")));
        hazard.setUpdatedBy(string(request.get("updated_by")));

        // 自动计算风险等级
        if (hazard.getLikelihood() != null && hazard.getSeverity() != null) {
            hazard.setRiskLevel(clinicalSafetyService.calculateRiskLevel(hazard.getLikelihood(), hazard.getSeverity()));
        }

        try {
            HazardLog updated = clinicalSafetyService.updateHazard(hazard);
            return ApiResult.success(updated);
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, e.getMessage());
        }
    }

    /**
     * 查询危险日志。
     */
    @GetMapping("/hazards")
    public ApiResult<List<HazardLog>> listHazards(
            @RequestParam(required = false) String hazardCategory,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String status,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        Long tenantId = parseLong(orgContext.getTenantId());
        List<HazardLog> hazards = clinicalSafetyService.listHazards(tenantId, hazardCategory, riskLevel, status);
        return ApiResult.success(hazards);
    }

    /**
     * 院方风险接受。
     */
    @PostMapping("/hazards/{hazardId}/accept")
    public ApiResult<HazardLog> acceptHazard(
            @PathVariable Long hazardId,
            @RequestBody Map<String, Object> request) {
        String acceptedBy = string(request.get("accepted_by"));
        String acceptanceNote = string(request.get("acceptance_note"));

        if (acceptedBy == null || acceptedBy.isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "accepted_by is required");
        }

        try {
            HazardLog accepted = clinicalSafetyService.acceptHazard(hazardId, acceptedBy, acceptanceNote);
            return ApiResult.success(accepted);
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, e.getMessage());
        }
    }

    /**
     * 关闭危险日志。
     */
    @PostMapping("/hazards/{hazardId}/close")
    public ApiResult<HazardLog> closeHazard(@PathVariable Long hazardId) {
        try {
            HazardLog closed = clinicalSafetyService.closeHazard(hazardId);
            return ApiResult.success(closed);
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, e.getMessage());
        }
    }

    /**
     * 创建安全案例。
     */
    @PostMapping("/safety-cases")
    public ApiResult<SafetyCase> createSafetyCase(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);

        SafetyCase safetyCase = new SafetyCase();
        safetyCase.setTenantId(parseLong(orgContext.getTenantId()));
        safetyCase.setCaseCode(string(request.get("case_code")));
        safetyCase.setCaseName(string(request.get("case_name")));
        safetyCase.setCaseType(string(request.get("case_type")));
        safetyCase.setScope(string(request.get("scope")));
        safetyCase.setGoal(string(request.get("goal")));
        safetyCase.setArgument(string(request.get("argument")));
        safetyCase.setEvidenceRefs(string(request.get("evidence_refs")));
        safetyCase.setVersion(string(request.get("version")));
        safetyCase.setCreatedBy(string(request.get("created_by")));

        if (safetyCase.getCaseCode() == null || safetyCase.getCaseCode().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "case_code is required");
        }

        SafetyCase saved = clinicalSafetyService.createSafetyCase(safetyCase);
        return ApiResult.success(saved);
    }

    /**
     * 更新安全案例。
     */
    @PutMapping("/safety-cases/{caseId}")
    public ApiResult<SafetyCase> updateSafetyCase(
            @PathVariable Long caseId,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);

        SafetyCase safetyCase = new SafetyCase();
        safetyCase.setId(caseId);
        safetyCase.setTenantId(parseLong(orgContext.getTenantId()));
        safetyCase.setCaseName(string(request.get("case_name")));
        safetyCase.setCaseType(string(request.get("case_type")));
        safetyCase.setScope(string(request.get("scope")));
        safetyCase.setGoal(string(request.get("goal")));
        safetyCase.setArgument(string(request.get("argument")));
        safetyCase.setEvidenceRefs(string(request.get("evidence_refs")));
        safetyCase.setStatus(string(request.get("status")));
        safetyCase.setVersion(string(request.get("version")));
        safetyCase.setUpdatedBy(string(request.get("updated_by")));

        try {
            SafetyCase updated = clinicalSafetyService.updateSafetyCase(safetyCase);
            return ApiResult.success(updated);
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, e.getMessage());
        }
    }

    /**
     * 查询安全案例。
     */
    @GetMapping("/safety-cases")
    public ApiResult<List<SafetyCase>> listSafetyCases(
            @RequestParam(required = false) String caseType,
            @RequestParam(required = false) String status,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        Long tenantId = parseLong(orgContext.getTenantId());
        List<SafetyCase> cases = clinicalSafetyService.listSafetyCases(tenantId, caseType, status);
        return ApiResult.success(cases);
    }

    /**
     * 审核安全案例。
     */
    @PostMapping("/safety-cases/{caseId}/review")
    public ApiResult<SafetyCase> reviewSafetyCase(
            @PathVariable Long caseId,
            @RequestBody Map<String, Object> request) {
        String reviewStatus = string(request.get("review_status"));
        String reviewedBy = string(request.get("reviewed_by"));
        String reviewNote = string(request.get("review_note"));

        if (reviewStatus == null || reviewStatus.isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "review_status is required");
        }
        if (reviewedBy == null || reviewedBy.isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "reviewed_by is required");
        }

        try {
            SafetyCase reviewed = clinicalSafetyService.reviewSafetyCase(caseId, reviewStatus, reviewedBy, reviewNote);
            return ApiResult.success(reviewed);
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, e.getMessage());
        }
    }

    /**
     * 风险摘要统计。
     */
    @GetMapping("/risk-summary")
    public ApiResult<Map<String, Object>> getRiskSummary(HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        Long tenantId = parseLong(orgContext.getTenantId());
        Map<String, Object> summary = clinicalSafetyService.getRiskSummary(tenantId);
        return ApiResult.success(summary);
    }

    // ─── 内部方法 ────────────────────────────────────────────────────

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long parseLong(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
