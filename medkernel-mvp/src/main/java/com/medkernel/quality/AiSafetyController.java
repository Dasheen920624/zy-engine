package com.medkernel.quality;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Ai Safety")
@RestController
@RequestMapping("/api/ai-safety")
public class AiSafetyController {

    private final AiSafetyService aiSafetyService;
    private final OrganizationContextService orgContextService;

    public AiSafetyController(AiSafetyService aiSafetyService,
                              OrganizationContextService orgContextService) {
        this.aiSafetyService = aiSafetyService;
        this.orgContextService = orgContextService;
    }

    // =========================================================================
    // 红队场景管理
    // =========================================================================

    @Operation(summary = "Create scenario")
    @PostMapping("/red-team/scenarios")
    public ApiResult<RedTeamScenario> createScenario(
            @RequestBody RedTeamScenario scenario,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        try {
            RedTeamScenario created = aiSafetyService.createScenario(scenario);
            return ApiResult.success(created);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    @Operation(summary = "Update scenario")
    @PutMapping("/red-team/scenarios/{scenarioId}")
    public ApiResult<RedTeamScenario> updateScenario(
            @PathVariable("scenarioId") Long scenarioId,
            @RequestBody RedTeamScenario scenario,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        scenario.setId(scenarioId);
        try {
            RedTeamScenario updated = aiSafetyService.updateScenario(scenario);
            return ApiResult.success(updated);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, ex.getMessage());
        }
    }

    @Operation(summary = "List scenarios")
    @GetMapping("/red-team/scenarios")
    public ApiResult<List<RedTeamScenario>> listScenarios(
            @RequestParam(value = "tenant_id", required = false) Long tenantId,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "status", required = false) String status,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        if (tenantId == null) {
            tenantId = resolveTenantId(httpRequest);
        }
        try {
            List<RedTeamScenario> scenarios = aiSafetyService.listScenarios(tenantId, category, status);
            return ApiResult.success(scenarios);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    // =========================================================================
    // 红队测试执行
    // =========================================================================

    @Operation(summary = "Execute red team test")
    @PostMapping("/red-team/execute")
    public ApiResult<RedTeamResult> executeRedTeamTest(
            @RequestBody Map<String, Object> executeRequest,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        Long scenarioId = longOrNull(executeRequest.get("scenario_id"));
        String modelCode = executeRequest.get("model_code") != null
                ? String.valueOf(executeRequest.get("model_code")) : null;
        String modelVersion = executeRequest.get("model_version") != null
                ? String.valueOf(executeRequest.get("model_version")) : null;
        String executedBy = executeRequest.get("executed_by") != null
                ? String.valueOf(executeRequest.get("executed_by")) : null;
        if (scenarioId == null) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "scenario_id is required");
        }
        try {
            RedTeamResult result = aiSafetyService.executeRedTeamTest(scenarioId, modelCode, modelVersion, executedBy);
            return ApiResult.success(result);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, ex.getMessage());
        }
    }

    @Operation(summary = "List red team results")
    @GetMapping("/red-team/results")
    public ApiResult<List<RedTeamResult>> listRedTeamResults(
            @RequestParam(value = "tenant_id", required = false) Long tenantId,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "verdict", required = false) String verdict,
            @RequestParam(value = "severity", required = false) String severity,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        if (tenantId == null) {
            tenantId = resolveTenantId(httpRequest);
        }
        try {
            List<RedTeamResult> results = aiSafetyService.listRedTeamResults(tenantId, category, verdict, severity);
            return ApiResult.success(results);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    @Operation(summary = "Get red team summary")
    @GetMapping("/red-team/summary")
    public ApiResult<Map<String, Object>> getRedTeamSummary(
            @RequestParam(value = "tenant_id", required = false) Long tenantId,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        if (tenantId == null) {
            tenantId = resolveTenantId(httpRequest);
        }
        try {
            Map<String, Object> summary = aiSafetyService.getRedTeamSummary(tenantId);
            return ApiResult.success(summary);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    // =========================================================================
    // 幻觉检测
    // =========================================================================

    @Operation(summary = "Record detection")
    @PostMapping("/hallucination/detections")
    public ApiResult<HallucinationDetection> recordDetection(
            @RequestBody HallucinationDetection detection,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        try {
            HallucinationDetection recorded = aiSafetyService.recordDetection(detection);
            return ApiResult.success(recorded);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    @Operation(summary = "List detections")
    @GetMapping("/hallucination/detections")
    public ApiResult<List<HallucinationDetection>> listDetections(
            @RequestParam(value = "tenant_id", required = false) Long tenantId,
            @RequestParam(value = "verdict", required = false) String verdict,
            @RequestParam(value = "status", required = false) String status,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        if (tenantId == null) {
            tenantId = resolveTenantId(httpRequest);
        }
        try {
            List<HallucinationDetection> detections = aiSafetyService.listDetections(tenantId, verdict, status);
            return ApiResult.success(detections);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    @Operation(summary = "Review detection")
    @PostMapping("/hallucination/detections/{detectionId}/review")
    public ApiResult<HallucinationDetection> reviewDetection(
            @PathVariable("detectionId") Long detectionId,
            @RequestBody Map<String, String> reviewRequest,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        String reviewer = reviewRequest.get("reviewer");
        String reviewNote = reviewRequest.get("review_note");
        String status = reviewRequest.get("status");
        if (status == null || status.trim().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "status is required");
        }
        try {
            HallucinationDetection reviewed = aiSafetyService.reviewDetection(detectionId, reviewer, reviewNote, status);
            return ApiResult.success(reviewed);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, ex.getMessage());
        }
    }

    @Operation(summary = "Get hallucination summary")
    @GetMapping("/hallucination/summary")
    public ApiResult<Map<String, Object>> getHallucinationSummary(
            @RequestParam(value = "tenant_id", required = false) Long tenantId,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        if (tenantId == null) {
            tenantId = resolveTenantId(httpRequest);
        }
        try {
            Map<String, Object> summary = aiSafetyService.getHallucinationSummary(tenantId);
            return ApiResult.success(summary);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    // =========================================================================
    // 内部方法
    // =========================================================================

    private Long resolveTenantId(HttpServletRequest httpRequest) {
        String headerTenantId = httpRequest.getHeader("X-Tenant-Id");
        if (headerTenantId != null && !headerTenantId.trim().isEmpty()) {
            try {
                return Long.parseLong(headerTenantId.trim());
            } catch (NumberFormatException ex) {
                return 0L;
            }
        }
        return 0L;
    }

    private Long longOrNull(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
