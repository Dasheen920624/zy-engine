package com.medkernel.cdss;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CDSS 覆盖治理 REST API。
 *
 * <p>提供：
 * <ul>
 *   <li>覆盖记录 — POST /api/cdss/overrides</li>
 *   <li>覆盖查询 — GET /api/cdss/overrides</li>
 *   <li>覆盖统计 — GET /api/cdss/overrides/statistics</li>
 *   <li>疲劳检查 — GET /api/cdss/overrides/fatigue-check</li>
 *   <li>疲劳配置保存 — POST /api/cdss/fatigue-configs</li>
 *   <li>疲劳配置查询 — GET /api/cdss/fatigue-configs</li>
 *   <li>疲劳配置更新 — PUT /api/cdss/fatigue-configs/{configId}</li>
 * </ul>
 */
@Tag(name = "Cdss Override")
@RestController
@RequestMapping("/api/cdss")
public class CdssOverrideController {

    private final CdssOverrideService overrideService;
    private final OrganizationContextService organizationContextService;

    public CdssOverrideController(CdssOverrideService overrideService,
                                  OrganizationContextService organizationContextService) {
        this.overrideService = overrideService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 记录覆盖。
     */
    @Operation(summary = "Record override")
    @PostMapping("/overrides")
    public ApiResult<CdssOverrideLog> recordOverride(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);

        CdssOverrideLog log = new CdssOverrideLog();
        log.setTenantId(parseLong(orgContext.getTenantId()));
        log.setAlertId(string(request.get("alert_id")));
        log.setTriggerCode(string(request.get("trigger_code")));
        log.setRuleCode(string(request.get("rule_code")));
        log.setRiskLevel(string(request.get("risk_level")));
        log.setAlertLevel(string(request.get("alert_level")));
        log.setOverrideType(string(request.get("override_type")));
        log.setOverrideReason(string(request.get("override_reason")));
        log.setOverrideCategory(string(request.get("override_category")));
        log.setSupervisorName(string(request.get("supervisor_name")));
        log.setConfirmedBy(string(request.get("confirmed_by")));
        log.setPatientId(string(request.get("patient_id")));
        log.setEncounterId(string(request.get("encounter_id")));
        log.setOperatorId(string(request.get("operator_id")));
        log.setDepartmentCode(orgContext.getDepartmentCode());
        log.setIsAuditRedLine(string(request.get("is_audit_red_line")));
        log.setFatigueSuppressed(string(request.get("fatigue_suppressed")));
        log.setOverrideTime(LocalDateTime.now());

        if (log.getOverrideType() == null || log.getOverrideType().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "override_type is required");
        }

        CdssOverrideLog saved = overrideService.recordOverride(log);
        return ApiResult.success(saved);
    }

    /**
     * 查询覆盖日志。
     */
    @Operation(summary = "List overrides")
    @GetMapping("/overrides")
    public ApiResult<List<CdssOverrideLog>> listOverrides(
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String operatorId,
            @RequestParam(required = false, defaultValue = "100") int limit,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        Long tenantId = parseLong(orgContext.getTenantId());
        List<CdssOverrideLog> overrides = overrideService.listOverrides(tenantId, patientId, operatorId, limit);
        return ApiResult.success(overrides);
    }

    /**
     * 覆盖统计。
     */
    @Operation(summary = "Get override statistics")
    @GetMapping("/overrides/statistics")
    public ApiResult<Map<String, Object>> getOverrideStatistics(
            @RequestParam(required = false) String operatorId,
            @RequestParam(required = false) String ruleCode,
            @RequestParam(required = false, defaultValue = "24") int hours,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        Long tenantId = parseLong(orgContext.getTenantId());
        Map<String, Object> stats = overrideService.getOverrideStatistics(tenantId, operatorId, ruleCode, hours);
        return ApiResult.success(stats);
    }

    /**
     * 疲劳检查。
     */
    @Operation(summary = "Check fatigue")
    @GetMapping("/overrides/fatigue-check")
    public ApiResult<Map<String, Object>> checkFatigue(
            @RequestParam String operatorId,
            @RequestParam(required = false) String ruleCode,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        Long tenantId = parseLong(orgContext.getTenantId());
        Map<String, Object> result = overrideService.checkFatigue(tenantId, operatorId, ruleCode);
        return ApiResult.success(result);
    }

    /**
     * 保存疲劳配置。
     */
    @Operation(summary = "Save fatigue config")
    @PostMapping("/fatigue-configs")
    public ApiResult<CdssFatigueConfig> saveFatigueConfig(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);

        CdssFatigueConfig config = new CdssFatigueConfig();
        config.setTenantId(parseLong(orgContext.getTenantId()));
        config.setConfigCode(string(request.get("config_code")));
        config.setConfigName(string(request.get("config_name")));
        config.setRuleCode(string(request.get("rule_code")));
        config.setDepartmentCode(string(request.get("department_code")));
        config.setTimeWindowHours(parseInt(request.get("time_window_hours"), 24));
        config.setOverrideThreshold(parseInt(request.get("override_threshold"), 5));
        config.setSuppressAction(string(request.get("suppress_action")));
        config.setSuppressLevel(string(request.get("suppress_level")));
        config.setEnabled(string(request.get("enabled")));
        config.setDescription(string(request.get("description")));
        config.setCreatedBy(string(request.get("created_by")));

        if (config.getConfigCode() == null || config.getConfigCode().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "config_code is required");
        }

        CdssFatigueConfig saved = overrideService.saveFatigueConfig(config);
        return ApiResult.success(saved);
    }

    /**
     * 查询疲劳配置。
     */
    @Operation(summary = "List fatigue configs")
    @GetMapping("/fatigue-configs")
    public ApiResult<List<CdssFatigueConfig>> listFatigueConfigs(HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        Long tenantId = parseLong(orgContext.getTenantId());
        List<CdssFatigueConfig> configs = overrideService.listFatigueConfigs(tenantId);
        return ApiResult.success(configs);
    }

    /**
     * 更新疲劳配置。
     */
    @Operation(summary = "Update fatigue config")
    @PutMapping("/fatigue-configs/{configId}")
    public ApiResult<CdssFatigueConfig> updateFatigueConfig(
            @PathVariable Long configId,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);

        CdssFatigueConfig config = new CdssFatigueConfig();
        config.setId(configId);
        config.setTenantId(parseLong(orgContext.getTenantId()));
        config.setConfigCode(string(request.get("config_code")));
        config.setConfigName(string(request.get("config_name")));
        config.setRuleCode(string(request.get("rule_code")));
        config.setDepartmentCode(string(request.get("department_code")));
        config.setTimeWindowHours(parseInt(request.get("time_window_hours"), 24));
        config.setOverrideThreshold(parseInt(request.get("override_threshold"), 5));
        config.setSuppressAction(string(request.get("suppress_action")));
        config.setSuppressLevel(string(request.get("suppress_level")));
        config.setEnabled(string(request.get("enabled")));
        config.setDescription(string(request.get("description")));
        config.setUpdatedBy(string(request.get("updated_by")));

        try {
            CdssFatigueConfig updated = overrideService.updateFatigueConfig(config);
            return ApiResult.success(updated);
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, e.getMessage());
        }
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

    private int parseInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
