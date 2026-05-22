package com.medkernel.quality;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.organization.OrganizationContextService;
import com.medkernel.quality.dto.CreateScenarioRequest;
import com.medkernel.quality.dto.UpdateScenarioRequest;
import com.medkernel.quality.dto.ExecuteRedTeamTestRequest;
import com.medkernel.quality.dto.RecordDetectionRequest;
import com.medkernel.quality.dto.ReviewDetectionRequest;
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
import javax.validation.Valid;
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
            @RequestBody @Valid CreateScenarioRequest request,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        try {
            RedTeamScenario scenario = toEntity(request);
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
            @RequestBody @Valid UpdateScenarioRequest request,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        RedTeamScenario scenario = toEntity(request);
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
            @RequestBody @Valid ExecuteRedTeamTestRequest request,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        try {
            RedTeamResult result = aiSafetyService.executeRedTeamTest(request.getScenarioId(), request.getModelCode(), request.getModelVersion(), request.getExecutedBy());
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
            @RequestBody @Valid RecordDetectionRequest request,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        try {
            HallucinationDetection detection = toEntity(request);
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
            @RequestBody @Valid ReviewDetectionRequest request,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        String status = request.getStatus();
        if (status == null || status.trim().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "status is required");
        }
        try {
            HallucinationDetection reviewed = aiSafetyService.reviewDetection(detectionId, request.getReviewer(), request.getReviewNote(), status);
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

    private RedTeamScenario toEntity(CreateScenarioRequest request) {
        RedTeamScenario scenario = new RedTeamScenario();
        scenario.setScenarioCode(request.getScenarioCode());
        scenario.setScenarioName(request.getScenarioName());
        scenario.setCategory(request.getCategory());
        scenario.setDescription(request.getDescription());
        scenario.setAttackPrompt(request.getInputTemplate());
        scenario.setExpectedBehavior(request.getExpectedBehavior());
        scenario.setSeverity(request.getRiskLevel());
        scenario.setEnabled(request.getEnabled());
        return scenario;
    }

    private RedTeamScenario toEntity(UpdateScenarioRequest request) {
        RedTeamScenario scenario = new RedTeamScenario();
        scenario.setScenarioName(request.getScenarioName());
        scenario.setCategory(request.getCategory());
        scenario.setDescription(request.getDescription());
        scenario.setAttackPrompt(request.getInputTemplate());
        scenario.setExpectedBehavior(request.getExpectedBehavior());
        scenario.setSeverity(request.getRiskLevel());
        scenario.setEnabled(request.getEnabled());
        return scenario;
    }

    private HallucinationDetection toEntity(RecordDetectionRequest request) {
        HallucinationDetection detection = new HallucinationDetection();
        detection.setDetectionCode(request.getDetectionCode());
        detection.setModelCode(request.getModelCode());
        detection.setInputContent(request.getInputContent());
        detection.setOutputContent(request.getOutputContent());
        detection.setDetectionType(request.getDetectionType());
        detection.setVerdict(request.getVerdict());
        if (request.getConfidence() != null) {
            try {
                detection.setConfidenceScore(Double.parseDouble(request.getConfidence()));
            } catch (NumberFormatException ignored) {
            }
        }
        detection.setEvidence(request.getEvidence());
        return detection;
    }
}
