package com.medkernel.cdss;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import com.medkernel.cdss.dto.DefineRedLineRequest;
import com.medkernel.cdss.dto.UpdateRedLineRequest;
import com.medkernel.cdss.dto.ScanRedLinesRequest;
import com.medkernel.cdss.dto.ResolveScanResultRequest;
import com.medkernel.cdss.dto.OverrideScanResultRequest;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.LinkedHashMap;
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
@Tag(name = "Safety Red Line")
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
    @Operation(summary = "Define red line")
    @PostMapping
    public ApiResult<SafetyRedLine> defineRedLine(
            @RequestBody @Valid DefineRedLineRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> bodyMap = new LinkedHashMap<>();
        bodyMap.put("red_line_code", request.getRedLineCode());
        bodyMap.put("red_line_name", request.getRedLineName());
        bodyMap.put("category", request.getCategory());
        bodyMap.put("description", request.getDescription());
        bodyMap.put("condition_expression", request.getConditionExpression());
        bodyMap.put("blocking_action", request.getBlockingAction());
        bodyMap.put("severity", request.getSeverity());
        bodyMap.put("applicable_scenarios", request.getApplicableScenarios());
        bodyMap.put("enabled", request.getEnabled());
        bodyMap.put("created_by", request.getCreatedBy());
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, bodyMap);

        SafetyRedLine redLine = new SafetyRedLine();
        redLine.setTenantId(parseLong(orgContext.getTenantId()));
        redLine.setRedLineCode(request.getRedLineCode());
        redLine.setRedLineName(request.getRedLineName());
        redLine.setCategory(request.getCategory());
        redLine.setDescription(request.getDescription());
        redLine.setConditionExpression(request.getConditionExpression());
        redLine.setBlockingAction(request.getBlockingAction());
        redLine.setSeverity(request.getSeverity());
        redLine.setApplicableScenarios(request.getApplicableScenarios());
        redLine.setEnabled(request.getEnabled());
        redLine.setCreatedBy(request.getCreatedBy());

        SafetyRedLine saved = safetyRedLineService.defineRedLine(redLine);
        return ApiResult.success(saved);
    }

    /**
     * 更新红线。
     */
    @Operation(summary = "Update red line")
    @PutMapping("/{redLineId}")
    public ApiResult<SafetyRedLine> updateRedLine(
            @PathVariable Long redLineId,
            @RequestBody @Valid UpdateRedLineRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> bodyMap = new LinkedHashMap<>();
        bodyMap.put("red_line_name", request.getRedLineName());
        bodyMap.put("category", request.getCategory());
        bodyMap.put("description", request.getDescription());
        bodyMap.put("condition_expression", request.getConditionExpression());
        bodyMap.put("blocking_action", request.getBlockingAction());
        bodyMap.put("severity", request.getSeverity());
        bodyMap.put("applicable_scenarios", request.getApplicableScenarios());
        bodyMap.put("enabled", request.getEnabled());
        bodyMap.put("updated_by", request.getUpdatedBy());
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, bodyMap);

        SafetyRedLine redLine = new SafetyRedLine();
        redLine.setId(redLineId);
        redLine.setTenantId(parseLong(orgContext.getTenantId()));
        redLine.setRedLineName(request.getRedLineName());
        redLine.setCategory(request.getCategory());
        redLine.setDescription(request.getDescription());
        redLine.setConditionExpression(request.getConditionExpression());
        redLine.setBlockingAction(request.getBlockingAction());
        redLine.setSeverity(request.getSeverity());
        redLine.setApplicableScenarios(request.getApplicableScenarios());
        redLine.setEnabled(request.getEnabled());
        redLine.setUpdatedBy(request.getUpdatedBy());

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
    @Operation(summary = "List red lines")
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
    @Operation(summary = "Scan red lines")
    @PostMapping("/scan")
    public ApiResult<List<RedLineScanResult>> scanRedLines(
            @RequestBody @Valid ScanRedLinesRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> bodyMap = new LinkedHashMap<>();
        bodyMap.put("patient_id", request.getPatientId());
        bodyMap.put("encounter_id", request.getEncounterId());
        bodyMap.put("scan_type", request.getScanType());
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, bodyMap);
        Long tenantId = parseLong(orgContext.getTenantId());
        String patientId = request.getPatientId();
        String encounterId = request.getEncounterId();
        String scanType = request.getScanType();

        List<RedLineScanResult> results = safetyRedLineService.scanRedLines(tenantId, patientId, encounterId, scanType);
        return ApiResult.success(results);
    }

    /**
     * 查询扫描结果。
     */
    @Operation(summary = "List scan results")
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
    @Operation(summary = "Resolve scan result")
    @PostMapping("/scan-results/{resultId}/resolve")
    public ApiResult<RedLineScanResult> resolveScanResult(
            @PathVariable Long resultId,
            @RequestBody @Valid ResolveScanResultRequest request) {
        String resolvedBy = request.getResolvedBy();
        String resolutionNote = request.getResolutionNote();

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
    @Operation(summary = "Override scan result")
    @PostMapping("/scan-results/{resultId}/override")
    public ApiResult<RedLineScanResult> overrideScanResult(
            @PathVariable Long resultId,
            @RequestBody @Valid OverrideScanResultRequest request) {
        String overriddenBy = request.getOverriddenBy();
        String overrideReason = request.getOverrideReason();

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
    @Operation(summary = "Get scan summary")
    @GetMapping("/scan-summary")
    public ApiResult<Map<String, Object>> getScanSummary(HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        Long tenantId = parseLong(orgContext.getTenantId());
        Map<String, Object> summary = safetyRedLineService.getScanSummary(tenantId);
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
