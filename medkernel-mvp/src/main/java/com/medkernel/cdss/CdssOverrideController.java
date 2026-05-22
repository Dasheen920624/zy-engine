package com.medkernel.cdss;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import com.medkernel.cdss.dto.RecordOverrideRequest;
import com.medkernel.cdss.dto.SaveFatigueConfigRequest;
import com.medkernel.cdss.dto.UpdateFatigueConfigRequest;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
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
            @RequestBody @Valid RecordOverrideRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> bodyMap = new LinkedHashMap<>();
        bodyMap.put("alert_id", request.getAlertId());
        bodyMap.put("trigger_code", request.getTriggerCode());
        bodyMap.put("rule_code", request.getRuleCode());
        bodyMap.put("risk_level", request.getRiskLevel());
        bodyMap.put("alert_level", request.getAlertLevel());
        bodyMap.put("override_type", request.getOverrideType());
        bodyMap.put("override_reason", request.getOverrideReason());
        bodyMap.put("override_category", request.getOverrideCategory());
        bodyMap.put("supervisor_name", request.getSupervisorName());
        bodyMap.put("confirmed_by", request.getConfirmedBy());
        bodyMap.put("patient_id", request.getPatientId());
        bodyMap.put("encounter_id", request.getEncounterId());
        bodyMap.put("operator_id", request.getOperatorId());
        bodyMap.put("is_audit_red_line", request.getIsAuditRedLine());
        bodyMap.put("fatigue_suppressed", request.getFatigueSuppressed());
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, bodyMap);

        CdssOverrideLog log = new CdssOverrideLog();
        log.setTenantId(parseLong(orgContext.getTenantId()));
        log.setAlertId(request.getAlertId());
        log.setTriggerCode(request.getTriggerCode());
        log.setRuleCode(request.getRuleCode());
        log.setRiskLevel(request.getRiskLevel());
        log.setAlertLevel(request.getAlertLevel());
        log.setOverrideType(request.getOverrideType());
        log.setOverrideReason(request.getOverrideReason());
        log.setOverrideCategory(request.getOverrideCategory());
        log.setSupervisorName(request.getSupervisorName());
        log.setConfirmedBy(request.getConfirmedBy());
        log.setPatientId(request.getPatientId());
        log.setEncounterId(request.getEncounterId());
        log.setOperatorId(request.getOperatorId());
        log.setDepartmentCode(orgContext.getDepartmentCode());
        log.setIsAuditRedLine(request.getIsAuditRedLine());
        log.setFatigueSuppressed(request.getFatigueSuppressed());
        log.setOverrideTime(LocalDateTime.now());

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
            @RequestBody @Valid SaveFatigueConfigRequest request,
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
        bodyMap.put("created_by", request.getCreatedBy());
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, bodyMap);

        CdssFatigueConfig config = new CdssFatigueConfig();
        config.setTenantId(parseLong(orgContext.getTenantId()));
        config.setConfigCode(request.getConfigCode());
        config.setConfigName(request.getConfigName());
        config.setRuleCode(request.getRuleCode());
        config.setDepartmentCode(request.getDepartmentCode());
        config.setTimeWindowHours(request.getTimeWindowHours() != null ? request.getTimeWindowHours() : 24);
        config.setOverrideThreshold(request.getOverrideThreshold() != null ? request.getOverrideThreshold() : 5);
        config.setSuppressAction(request.getSuppressAction());
        config.setSuppressLevel(request.getSuppressLevel());
        config.setEnabled(request.getEnabled());
        config.setDescription(request.getDescription());
        config.setCreatedBy(request.getCreatedBy());

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

        CdssFatigueConfig config = new CdssFatigueConfig();
        config.setId(configId);
        config.setTenantId(parseLong(orgContext.getTenantId()));
        config.setConfigCode(request.getConfigCode());
        config.setConfigName(request.getConfigName());
        config.setRuleCode(request.getRuleCode());
        config.setDepartmentCode(request.getDepartmentCode());
        config.setTimeWindowHours(request.getTimeWindowHours() != null ? request.getTimeWindowHours() : 24);
        config.setOverrideThreshold(request.getOverrideThreshold() != null ? request.getOverrideThreshold() : 5);
        config.setSuppressAction(request.getSuppressAction());
        config.setSuppressLevel(request.getSuppressLevel());
        config.setEnabled(request.getEnabled());
        config.setDescription(request.getDescription());
        config.setUpdatedBy(request.getUpdatedBy());

        try {
            CdssFatigueConfig updated = overrideService.updateFatigueConfig(config);
            return ApiResult.success(updated);
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, e.getMessage());
        }
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
