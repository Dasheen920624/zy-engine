package com.medkernel.cdss;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import com.medkernel.cdss.dto.CreateFatigueConfigRequest;
import com.medkernel.cdss.dto.UpdateFatigueConfigRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 提醒疲劳治理和覆盖分析 API。
 */
@Tag(name = "Alert Fatigue")
@RestController
@RequestMapping("/api/cdss/fatigue")
public class AlertFatigueController {
    private final AlertFatigueService alertFatigueService;
    private final OrganizationContextService organizationContextService;

    public AlertFatigueController(AlertFatigueService alertFatigueService,
                                  OrganizationContextService organizationContextService) {
        this.alertFatigueService = alertFatigueService;
        this.organizationContextService = organizationContextService;
    }

    // ==================== 疲劳治理配置 ====================

    @Operation(summary = "Create config")
    @PostMapping("/configs")
    public ApiResult<Map<String, Object>> createConfig(
            @RequestBody @Valid CreateFatigueConfigRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> bodyMap = new LinkedHashMap<>();
        bodyMap.put("config_code", request.getConfigCode());
        bodyMap.put("config_name", request.getConfigName());
        bodyMap.put("rule_code", request.getRuleCode());
        bodyMap.put("time_window_hours", request.getTimeWindowHours());
        bodyMap.put("override_threshold", request.getOverrideThreshold());
        bodyMap.put("suppress_action", request.getSuppressAction());
        bodyMap.put("suppress_level", request.getSuppressLevel());
        bodyMap.put("enabled", request.getEnabled());
        bodyMap.put("description", request.getDescription());
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, bodyMap);
        try {
            AlertFatigueConfig config = alertFatigueService.createConfig(bodyMap, orgContext);
            return ApiResult.success(config.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "List configs")
    @GetMapping("/configs")
    public ApiResult<List<Map<String, Object>>> listConfigs(HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        List<AlertFatigueConfig> configs = alertFatigueService.listConfigs(orgContext);
        List<Map<String, Object>> views = new ArrayList<Map<String, Object>>();
        for (AlertFatigueConfig config : configs) {
            views.add(config.toView());
        }
        return ApiResult.success(views);
    }

    @Operation(summary = "Update config")
    @PutMapping("/configs/{configId}")
    public ApiResult<Map<String, Object>> updateConfig(
            @PathVariable String configId,
            @RequestBody @Valid UpdateFatigueConfigRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> bodyMap = new LinkedHashMap<>();
        bodyMap.put("config_code", request.getConfigCode());
        bodyMap.put("config_name", request.getConfigName());
        bodyMap.put("rule_code", request.getRuleCode());
        bodyMap.put("department_code", request.getDepartmentCode());
        bodyMap.put("time_window_hours", request.getTimeWindowHours());
        bodyMap.put("override_threshold", request.getOverrideThreshold());
        bodyMap.put("suppress_action", request.getSuppressAction());
        bodyMap.put("suppress_level", request.getSuppressLevel());
        bodyMap.put("enabled", request.getEnabled());
        bodyMap.put("description", request.getDescription());
        bodyMap.put("updated_by", request.getUpdatedBy());
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, bodyMap);
        try {
            AlertFatigueConfig config = alertFatigueService.updateConfig(configId, bodyMap, orgContext);
            return ApiResult.success(config.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    // ==================== 覆盖分析 ====================

    @Operation(summary = "Get override analysis")
    @GetMapping("/override-analysis")
    public ApiResult<Map<String, Object>> getOverrideAnalysis(HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        return ApiResult.success(alertFatigueService.getOverrideAnalysis(orgContext.getTenantId()));
    }
}
