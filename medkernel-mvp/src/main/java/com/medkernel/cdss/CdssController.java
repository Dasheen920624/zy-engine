package com.medkernel.cdss;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import com.medkernel.cdss.dto.CdssEvaluateRequest;
import com.medkernel.cdss.dto.ResolveAlertRequest;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 临床决策支持 API。
 *
 * <p>对标 CDSS 标准，提供：
 * <ul>
 *   <li>触发评估 — POST /api/cdss/evaluate</li>
 *   <li>告警确认/覆盖 — POST /api/cdss/alerts/{alertId}/resolve</li>
 *   <li>活动告警列表 — GET /api/cdss/alerts</li>
 *   <li>告警详情 — GET /api/cdss/alerts/{alertId}</li>
 * </ul>
 */
@Tag(name = "Cdss")
@RestController
@RequestMapping("/api/cdss")
public class CdssController {

    private final CdssService cdssService;
    private final OrganizationContextService organizationContextService;

    public CdssController(CdssService cdssService,
                          OrganizationContextService organizationContextService) {
        this.cdssService = cdssService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * CDSS 触发评估。
     *
     * @param request 包含 trigger_point 和 patient_context
     */
    @Operation(summary = "Evaluate")
    @PostMapping("/evaluate")
    public ApiResult<List<CdssAlert>> evaluate(
            @RequestBody @Valid CdssEvaluateRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> bodyMap = new LinkedHashMap<>();
        bodyMap.put("trigger_point", request.getTriggerPoint());
        bodyMap.put("patient_context", request.getPatientContext());
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, bodyMap);

        String triggerPoint = request.getTriggerPoint();
        Map<String, Object> patientContext = request.getPatientContext();

        List<CdssAlert> alerts = cdssService.evaluate(triggerPoint, patientContext,
                orgContext.getTenantId());
        return ApiResult.success(alerts);
    }

    /**
     * 医生确认/覆盖告警。
     */
    @Operation(summary = "Resolve alert")
    @PostMapping("/alerts/{alertId}/resolve")
    public ApiResult<CdssAlert> resolveAlert(
            @PathVariable String alertId,
            @RequestBody @Valid ResolveAlertRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> bodyMap = new LinkedHashMap<>();
        bodyMap.put("override_type", request.getOverrideType());
        bodyMap.put("override_reason", request.getOverrideReason());
        bodyMap.put("operator_name", request.getOperatorName());
        bodyMap.put("supervisor_name", request.getSupervisorName());
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, bodyMap);

        String overrideType = request.getOverrideType();
        String overrideReason = request.getOverrideReason();
        String operatorName = request.getOperatorName();
        String supervisorName = request.getSupervisorName();

        try {
            CdssAlert alert = cdssService.resolveAlert(alertId, overrideType, overrideReason,
                    operatorName, supervisorName, orgContext.getTenantId());
            return ApiResult.success(alert);
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    /**
     * 获取活动告警列表。
     */
    @Operation(summary = "List alerts")
    @GetMapping("/alerts")
    public ApiResult<List<CdssAlert>> listAlerts(
            @RequestParam(required = false) String patientId) {
        List<CdssAlert> alerts = cdssService.listActiveAlerts(patientId);
        return ApiResult.success(alerts);
    }

    /**
     * 获取告警详情。
     */
    @Operation(summary = "Get alert")
    @GetMapping("/alerts/{alertId}")
    public ApiResult<CdssAlert> getAlert(@PathVariable String alertId) {
        CdssAlert alert = cdssService.getAlert(alertId);
        if (alert == null) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, "Alert not found: " + alertId);
        }
        return ApiResult.success(alert);
    }
}
