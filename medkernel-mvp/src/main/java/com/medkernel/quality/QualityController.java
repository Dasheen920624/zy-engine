package com.medkernel.quality;

import com.medkernel.common.ApiResult;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "Quality")
@RestController
@RequestMapping("/api/quality")
public class QualityController {
    private final QualityService qualityService;
    private final OrganizationContextService organizationContextService;

    public QualityController(QualityService qualityService,
                             OrganizationContextService organizationContextService) {
        this.qualityService = qualityService;
        this.organizationContextService = organizationContextService;
    }

    @Operation(summary = "Metrics")
    @GetMapping("/metrics")
    public ApiResult<Map<String, Object>> metrics(@RequestParam(required = false) String pathwayCode,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String patientId,
                                                  @RequestParam(required = false) String encounterId,
                                                  @RequestParam(required = false) String currentNodeCode,
                                                  @RequestParam(required = false) String workflowCode,
                                                  @RequestParam(required = false) String workflowVersion,
                                                  @RequestParam(required = false) String workflowStatus,
                                                  HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("pathwayCode", pathwayCode);
        filters.put("status", status);
        filters.put("patientId", patientId);
        filters.put("encounterId", encounterId);
        filters.put("currentNodeCode", currentNodeCode);
        filters.put("workflowCode", workflowCode);
        filters.put("workflowVersion", workflowVersion);
        filters.put("workflowStatus", workflowStatus);
        organizationContextService.applyExplicitFilters(filters, request);
        return ApiResult.success(qualityService.metrics(filters));
    }

    @Operation(summary = "List alerts")
    @GetMapping("/alerts")
    public ApiResult<Map<String, Object>> listAlerts(
            @RequestParam(required = false) String dept,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") String page,
            @RequestParam(required = false, defaultValue = "20") String size,
            HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("dept", dept);
        filters.put("severity", severity);
        filters.put("date", date);
        filters.put("status", status);
        filters.put("page", page);
        filters.put("size", size);
        organizationContextService.applyExplicitFilters(filters, request);
        return ApiResult.success(qualityService.listAlerts(filters));
    }

    @Operation(summary = "Alert summary")
    @GetMapping("/alerts/summary")
    public ApiResult<Map<String, Object>> alertSummary(HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        organizationContextService.applyExplicitFilters(filters, request);
        return ApiResult.success(qualityService.alertSummary(filters));
    }

    @Operation(summary = "Assign problem")
    @PostMapping("/problems/{id}/assign")
    public ApiResult<Map<String, Object>> assignProblem(
            @PathVariable String id,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        organizationContextService.resolveWithBody(httpRequest, request);
        return ApiResult.success(qualityService.assignProblem(id, request));
    }
}
