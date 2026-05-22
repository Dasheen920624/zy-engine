package com.medkernel.cdss;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import com.medkernel.cdss.dto.CreateHazardRequest;
import com.medkernel.cdss.dto.UpdateHazardRequest;
import com.medkernel.cdss.dto.AcceptHazardRequest;
import com.medkernel.cdss.dto.CreateSafetyCaseRequest;
import com.medkernel.cdss.dto.UpdateSafetyCaseRequest;
import com.medkernel.cdss.dto.ReviewSafetyCaseRequest;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.LinkedHashMap;
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
@Tag(name = "Clinical Safety")
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
    @Operation(summary = "Create hazard")
    @PostMapping("/hazards")
    public ApiResult<HazardLog> createHazard(
            @RequestBody @Valid CreateHazardRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> bodyMap = new LinkedHashMap<>();
        bodyMap.put("hazard_code", request.getHazardCode());
        bodyMap.put("hazard_name", request.getHazardName());
        bodyMap.put("hazard_category", request.getHazardCategory());
        bodyMap.put("hazard_description", request.getHazardDescription());
        bodyMap.put("affected_process", request.getAffectedProcess());
        bodyMap.put("likelihood", request.getLikelihood());
        bodyMap.put("severity", request.getSeverity());
        bodyMap.put("control_measures", request.getControlMeasures());
        bodyMap.put("residual_risk", request.getResidualRisk());
        bodyMap.put("blocking_strategy", request.getBlockingStrategy());
        bodyMap.put("created_by", request.getCreatedBy());
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, bodyMap);

        HazardLog hazard = new HazardLog();
        hazard.setTenantId(parseLong(orgContext.getTenantId()));
        hazard.setHazardCode(request.getHazardCode());
        hazard.setHazardName(request.getHazardName());
        hazard.setHazardCategory(request.getHazardCategory());
        hazard.setHazardDescription(request.getHazardDescription());
        hazard.setAffectedProcess(request.getAffectedProcess());
        hazard.setLikelihood(request.getLikelihood());
        hazard.setSeverity(request.getSeverity());
        hazard.setControlMeasures(request.getControlMeasures());
        hazard.setResidualRisk(request.getResidualRisk());
        hazard.setBlockingStrategy(request.getBlockingStrategy());
        hazard.setCreatedBy(request.getCreatedBy());

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
    @Operation(summary = "Update hazard")
    @PutMapping("/hazards/{hazardId}")
    public ApiResult<HazardLog> updateHazard(
            @PathVariable Long hazardId,
            @RequestBody @Valid UpdateHazardRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> bodyMap = new LinkedHashMap<>();
        bodyMap.put("hazard_name", request.getHazardName());
        bodyMap.put("hazard_category", request.getHazardCategory());
        bodyMap.put("hazard_description", request.getHazardDescription());
        bodyMap.put("affected_process", request.getAffectedProcess());
        bodyMap.put("likelihood", request.getLikelihood());
        bodyMap.put("severity", request.getSeverity());
        bodyMap.put("control_measures", request.getControlMeasures());
        bodyMap.put("residual_risk", request.getResidualRisk());
        bodyMap.put("status", request.getStatus());
        bodyMap.put("blocking_strategy", request.getBlockingStrategy());
        bodyMap.put("updated_by", request.getUpdatedBy());
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, bodyMap);

        HazardLog hazard = new HazardLog();
        hazard.setId(hazardId);
        hazard.setTenantId(parseLong(orgContext.getTenantId()));
        hazard.setHazardName(request.getHazardName());
        hazard.setHazardCategory(request.getHazardCategory());
        hazard.setHazardDescription(request.getHazardDescription());
        hazard.setAffectedProcess(request.getAffectedProcess());
        hazard.setLikelihood(request.getLikelihood());
        hazard.setSeverity(request.getSeverity());
        hazard.setControlMeasures(request.getControlMeasures());
        hazard.setResidualRisk(request.getResidualRisk());
        hazard.setStatus(request.getStatus());
        hazard.setBlockingStrategy(request.getBlockingStrategy());
        hazard.setUpdatedBy(request.getUpdatedBy());

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
    @Operation(summary = "List hazards")
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
    @Operation(summary = "Accept hazard")
    @PostMapping("/hazards/{hazardId}/accept")
    public ApiResult<HazardLog> acceptHazard(
            @PathVariable Long hazardId,
            @RequestBody @Valid AcceptHazardRequest request) {
        String acceptedBy = request.getAcceptedBy();
        String acceptanceNote = request.getAcceptanceNote();

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
    @Operation(summary = "Close hazard")
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
    @Operation(summary = "Create safety case")
    @PostMapping("/safety-cases")
    public ApiResult<SafetyCase> createSafetyCase(
            @RequestBody @Valid CreateSafetyCaseRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> bodyMap = new LinkedHashMap<>();
        bodyMap.put("case_code", request.getCaseCode());
        bodyMap.put("case_name", request.getCaseName());
        bodyMap.put("case_type", request.getCaseType());
        bodyMap.put("scope", request.getScope());
        bodyMap.put("goal", request.getGoal());
        bodyMap.put("argument", request.getArgument());
        bodyMap.put("evidence_refs", request.getEvidenceRefs());
        bodyMap.put("version", request.getVersion());
        bodyMap.put("created_by", request.getCreatedBy());
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, bodyMap);

        SafetyCase safetyCase = new SafetyCase();
        safetyCase.setTenantId(parseLong(orgContext.getTenantId()));
        safetyCase.setCaseCode(request.getCaseCode());
        safetyCase.setCaseName(request.getCaseName());
        safetyCase.setCaseType(request.getCaseType());
        safetyCase.setScope(request.getScope());
        safetyCase.setGoal(request.getGoal());
        safetyCase.setArgument(request.getArgument());
        safetyCase.setEvidenceRefs(request.getEvidenceRefs());
        safetyCase.setVersion(request.getVersion());
        safetyCase.setCreatedBy(request.getCreatedBy());

        SafetyCase saved = clinicalSafetyService.createSafetyCase(safetyCase);
        return ApiResult.success(saved);
    }

    /**
     * 更新安全案例。
     */
    @Operation(summary = "Update safety case")
    @PutMapping("/safety-cases/{caseId}")
    public ApiResult<SafetyCase> updateSafetyCase(
            @PathVariable Long caseId,
            @RequestBody @Valid UpdateSafetyCaseRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> bodyMap = new LinkedHashMap<>();
        bodyMap.put("case_name", request.getCaseName());
        bodyMap.put("case_type", request.getCaseType());
        bodyMap.put("scope", request.getScope());
        bodyMap.put("goal", request.getGoal());
        bodyMap.put("argument", request.getArgument());
        bodyMap.put("evidence_refs", request.getEvidenceRefs());
        bodyMap.put("status", request.getStatus());
        bodyMap.put("version", request.getVersion());
        bodyMap.put("updated_by", request.getUpdatedBy());
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, bodyMap);

        SafetyCase safetyCase = new SafetyCase();
        safetyCase.setId(caseId);
        safetyCase.setTenantId(parseLong(orgContext.getTenantId()));
        safetyCase.setCaseName(request.getCaseName());
        safetyCase.setCaseType(request.getCaseType());
        safetyCase.setScope(request.getScope());
        safetyCase.setGoal(request.getGoal());
        safetyCase.setArgument(request.getArgument());
        safetyCase.setEvidenceRefs(request.getEvidenceRefs());
        safetyCase.setStatus(request.getStatus());
        safetyCase.setVersion(request.getVersion());
        safetyCase.setUpdatedBy(request.getUpdatedBy());

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
    @Operation(summary = "List safety cases")
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
    @Operation(summary = "Review safety case")
    @PostMapping("/safety-cases/{caseId}/review")
    public ApiResult<SafetyCase> reviewSafetyCase(
            @PathVariable Long caseId,
            @RequestBody @Valid ReviewSafetyCaseRequest request) {
        String reviewStatus = request.getReviewStatus();
        String reviewedBy = request.getReviewedBy();
        String reviewNote = request.getReviewNote();

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
    @Operation(summary = "Get risk summary")
    @GetMapping("/risk-summary")
    public ApiResult<Map<String, Object>> getRiskSummary(HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        Long tenantId = parseLong(orgContext.getTenantId());
        Map<String, Object> summary = clinicalSafetyService.getRiskSummary(tenantId);
        return ApiResult.success(summary);
    }

    // ─── 内部方法 ────────────────────────────────────────────────────

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
