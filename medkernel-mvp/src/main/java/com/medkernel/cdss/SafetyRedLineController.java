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
 * 医疗安全红线扫描 REST API（CDSS-004）。
 *
 * <p>提供：
 * <ul>
 *   <li>红线定义 — POST/PUT/GET /api/cdss/red-lines</li>
 *   <li>红线扫描 — POST /api/cdss/red-lines/scan</li>
 *   <li>扫描结果查询 — GET /api/cdss/red-lines/scan-results</li>
 *   <li>扫描结果解决 — POST /api/cdss/red-lines/scan-results/{resultId}/resolve</li>
 *   <li>扫描结果覆盖 — POST /api/cdss/red-lines/scan-results/{resultId}/override</li>
 *   <li>扫描统计 — GET /api/cdss/red-lines/scan-summary</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/cdss/red-lines")
public class SafetyRedLineController {

    private final SafetyRedLineService safetyRedLineService;
    private final OrganizationContextService organizationContextService;

    public SafetyRedLineController(SafetyRedLineService safetyRedLineService,
                                   OrganizationContextService organizationContextService) {
        this.safetyRedLineService = safetyRedLineService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 定义红线。
     */
    @PostMapping
    public ApiResult<SafetyRedLine> defineRedLine(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);

        SafetyRedLine redLine = new SafetyRedLine();
        redLine.setTenantId(parseLong(orgContext.getTenantId()));
        redLine.setRedLineCode(string(request.get("red_line_code")));
        redLine.setRedLineName(string(request.get("red_line_name")));
        redLine.setCategory(string(request.get("category")));
        redLine.setDescription(string(request.get("description")));
        redLine.setConditionExpression(string(request.get("condition_expression")));
        redLine.setBlockingAction(string(request.get("blocking_action")));
        redLine.setSeverity(string(request.get("severity")));
        redLine.setApplicableScenarios(string(request.get("applicable_scenarios")));
        redLine.setEnabled(string(request.get("enabled")));
        redLine.setCreatedBy(string(request.get("created_by")));

        if (redLine.getRedLineCode() == null || redLine.getRedLineCode().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "red_line_code is required");
        }
        if (redLine.getRedLineName() == null || redLine.getRedLineName().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "red_line_name is required");
        }

        SafetyRedLine saved = safetyRedLineService.defineRedLine(redLine);
        return ApiResult.success(saved);
    }

    /**
     * 更新红线。
     */
    @PutMapping("/{redLineId}")
    public ApiResult<SafetyRedLine> updateRedLine(
            @PathVariable Long redLineId,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);

        SafetyRedLine redLine = new SafetyRedLine();
        redLine.setId(redLineId);
        redLine.setTenantId(parseLong(orgContext.getTenantId()));
        redLine.setRedLineName(string(request.get("red_line_name")));
        redLine.setCategory(string(request.get("category")));
        redLine.setDescription(string(request.get("description")));
        redLine.setConditionExpression(string(request.get("condition_expression")));
        redLine.setBlockingAction(string(request.get("blocking_action")));
        redLine.setSeverity(string(request.get("severity")));
        redLine.setApplicableScenarios(string(request.get("applicable_scenarios")));
        redLine.setEnabled(string(request.get("enabled")));
        redLine.setUpdatedBy(string(request.get("updated_by")));

        try {
            SafetyRedLine updated = safetyRedLineService.updateRedLine(redLine);
            return ApiResult.success(updated);
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, e.getMessage());
        }
    }

    /**
     * 查询红线列表。
     */
    @GetMapping
    public ApiResult<List<SafetyRedLine>> listRedLines(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String enabled,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        Long tenantId = parseLong(orgContext.getTenantId());
        List<SafetyRedLine> redLines = safetyRedLineService.listRedLines(tenantId, category, enabled);
        return ApiResult.success(redLines);
    }

    /**
     * 执行红线扫描。
     */
    @PostMapping("/scan")
    public ApiResult<List<RedLineScanResult>> scanRedLines(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        Long tenantId = parseLong(orgContext.getTenantId());
        String patientId = string(request.get("patient_id"));
        String encounterId = string(request.get("encounter_id"));
        String scanType = string(request.get("scan_type"));

        List<RedLineScanResult> results = safetyRedLineService.scanRedLines(tenantId, patientId, encounterId, scanType);
        return ApiResult.success(results);
    }

    /**
     * 查询扫描结果。
     */
    @GetMapping("/scan-results")
    public ApiResult<List<RedLineScanResult>> listScanResults(
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        Long tenantId = parseLong(orgContext.getTenantId());
        List<RedLineScanResult> results = safetyRedLineService.listScanResults(tenantId, patientId, severity, status);
        return ApiResult.success(results);
    }

    /**
     * 解决扫描结果。
     */
    @PostMapping("/scan-results/{resultId}/resolve")
    public ApiResult<RedLineScanResult> resolveScanResult(
            @PathVariable Long resultId,
            @RequestBody Map<String, Object> request) {
        String resolvedBy = string(request.get("resolved_by"));
        String resolutionNote = string(request.get("resolution_note"));

        if (resolvedBy == null || resolvedBy.isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "resolved_by is required");
        }

        try {
            RedLineScanResult resolved = safetyRedLineService.resolveScanResult(resultId, resolvedBy, resolutionNote);
            return ApiResult.success(resolved);
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, e.getMessage());
        }
    }

    /**
     * 覆盖扫描结果。
     */
    @PostMapping("/scan-results/{resultId}/override")
    public ApiResult<RedLineScanResult> overrideScanResult(
            @PathVariable Long resultId,
            @RequestBody Map<String, Object> request) {
        String overriddenBy = string(request.get("overridden_by"));
        String overrideReason = string(request.get("override_reason"));

        if (overriddenBy == null || overriddenBy.isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "overridden_by is required");
        }
        if (overrideReason == null || overrideReason.isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "override_reason is required");
        }

        try {
            RedLineScanResult overridden = safetyRedLineService.overrideScanResult(resultId, overriddenBy, overrideReason);
            return ApiResult.success(overridden);
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, e.getMessage());
        }
    }

    /**
     * 扫描统计。
     */
    @GetMapping("/scan-summary")
    public ApiResult<Map<String, Object>> getScanSummary(HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        Long tenantId = parseLong(orgContext.getTenantId());
        Map<String, Object> summary = safetyRedLineService.getScanSummary(tenantId);
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
