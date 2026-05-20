package com.medkernel.cdss;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.common.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 提醒疲劳治理和覆盖分析 API。
 */
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

    @PostMapping("/configs")
    public ApiResult<Map<String, Object>> createConfig(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        try {
            AlertFatigueConfig config = alertFatigueService.createConfig(request, orgContext);
            return ApiResult.success(config.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

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

    @PutMapping("/configs/{configId}")
    public ApiResult<Map<String, Object>> updateConfig(
            @PathVariable String configId,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        try {
            AlertFatigueConfig config = alertFatigueService.updateConfig(configId, request, orgContext);
            return ApiResult.success(config.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    // ==================== 覆盖分析 ====================

    @GetMapping("/override-analysis")
    public ApiResult<Map<String, Object>> getOverrideAnalysis(HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        return ApiResult.success(alertFatigueService.getOverrideAnalysis(orgContext.getTenantId()));
    }
}
