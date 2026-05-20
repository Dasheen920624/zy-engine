package com.medkernel.dify;

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

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai-governance")
public class AiGovernanceController {

    private final AiGovernanceService governanceService;
    private final OrganizationContextService orgContextService;

    public AiGovernanceController(AiGovernanceService governanceService,
                                  OrganizationContextService orgContextService) {
        this.governanceService = governanceService;
        this.orgContextService = orgContextService;
    }

    // =========================================================================
    // 模型注册
    // =========================================================================

    @PostMapping("/models")
    public ApiResult<AiModelRegistry> registerModel(
            @RequestBody AiModelRegistry model,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        try {
            AiModelRegistry registered = governanceService.registerModel(model);
            return ApiResult.success(registered);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    @PutMapping("/models/{modelId}")
    public ApiResult<AiModelRegistry> updateModel(
            @PathVariable("modelId") Long modelId,
            @RequestBody AiModelRegistry model,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        model.setId(modelId);
        try {
            AiModelRegistry updated = governanceService.updateModel(model);
            return ApiResult.success(updated);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    @GetMapping("/models")
    public ApiResult<List<AiModelRegistry>> listModels(
            @RequestParam(value = "tenant_id", required = false) Long tenantId,
            @RequestParam(value = "model_type", required = false) String modelType,
            @RequestParam(value = "status", required = false) String status,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        if (tenantId == null) {
            String headerTenantId = httpRequest.getHeader("X-Tenant-Id");
            if (headerTenantId != null && !headerTenantId.trim().isEmpty()) {
                try {
                    tenantId = Long.parseLong(headerTenantId.trim());
                } catch (NumberFormatException ex) {
                    tenantId = 0L;
                }
            } else {
                tenantId = 0L;
            }
        }
        try {
            List<AiModelRegistry> models = governanceService.listModels(tenantId, modelType, status);
            return ApiResult.success(models);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    @PostMapping("/models/{modelId}/review")
    public ApiResult<Map<String, Object>> reviewModel(
            @PathVariable("modelId") Long modelId,
            @RequestBody Map<String, String> reviewRequest,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        String reviewStatus = reviewRequest.get("review_status");
        String reviewedBy = reviewRequest.get("reviewed_by");
        String reviewNote = reviewRequest.get("review_note");
        if (reviewStatus == null || reviewStatus.trim().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "review_status is required");
        }
        try {
            governanceService.reviewModel(modelId, reviewStatus, reviewedBy, reviewNote);
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("modelId", modelId);
            result.put("reviewStatus", reviewStatus);
            return ApiResult.success(result);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    @PostMapping("/models/{modelId}/online")
    public ApiResult<Map<String, Object>> onlineModel(
            @PathVariable("modelId") Long modelId,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        try {
            governanceService.onlineModel(modelId);
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("modelId", modelId);
            result.put("status", "ONLINE");
            return ApiResult.success(result);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, ex.getMessage());
        }
    }

    @PostMapping("/models/{modelId}/offline")
    public ApiResult<Map<String, Object>> offlineModel(
            @PathVariable("modelId") Long modelId,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        try {
            governanceService.offlineModel(modelId);
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("modelId", modelId);
            result.put("status", "OFFLINE");
            return ApiResult.success(result);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    // =========================================================================
    // 提示词模板
    // =========================================================================

    @PostMapping("/prompt-templates")
    public ApiResult<PromptTemplate> savePromptTemplate(
            @RequestBody PromptTemplate template,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        try {
            PromptTemplate saved = governanceService.savePromptTemplate(template);
            return ApiResult.success(saved);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    @GetMapping("/prompt-templates")
    public ApiResult<List<PromptTemplate>> listPromptTemplates(
            @RequestParam(value = "tenant_id", required = false) Long tenantId,
            @RequestParam(value = "template_type", required = false) String templateType,
            @RequestParam(value = "status", required = false) String status,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        if (tenantId == null) {
            String headerTenantId = httpRequest.getHeader("X-Tenant-Id");
            if (headerTenantId != null && !headerTenantId.trim().isEmpty()) {
                try {
                    tenantId = Long.parseLong(headerTenantId.trim());
                } catch (NumberFormatException ex) {
                    tenantId = 0L;
                }
            } else {
                tenantId = 0L;
            }
        }
        try {
            List<PromptTemplate> templates = governanceService.listPromptTemplates(tenantId, templateType, status);
            return ApiResult.success(templates);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    @PostMapping("/prompt-templates/{templateId}/review")
    public ApiResult<Map<String, Object>> reviewPromptTemplate(
            @PathVariable("templateId") Long templateId,
            @RequestBody Map<String, String> reviewRequest,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        String reviewStatus = reviewRequest.get("review_status");
        String reviewedBy = reviewRequest.get("reviewed_by");
        String reviewNote = reviewRequest.get("review_note");
        if (reviewStatus == null || reviewStatus.trim().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "review_status is required");
        }
        try {
            governanceService.reviewPromptTemplate(templateId, reviewStatus, reviewedBy, reviewNote);
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("templateId", templateId);
            result.put("reviewStatus", reviewStatus);
            return ApiResult.success(result);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    @PostMapping("/prompt-templates/{templateId}/publish")
    public ApiResult<Map<String, Object>> publishPromptTemplate(
            @PathVariable("templateId") Long templateId,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        try {
            governanceService.publishPromptTemplate(templateId);
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("templateId", templateId);
            result.put("status", "PUBLISHED");
            return ApiResult.success(result);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, ex.getMessage());
        }
    }

    // =========================================================================
    // 评测任务
    // =========================================================================

    @PostMapping("/eval-tasks")
    public ApiResult<ModelEvalTask> createEvalTask(
            @RequestBody ModelEvalTask task,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        try {
            ModelEvalTask created = governanceService.createEvalTask(task);
            return ApiResult.success(created);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    @GetMapping("/eval-tasks")
    public ApiResult<List<ModelEvalTask>> listEvalTasks(
            @RequestParam(value = "tenant_id", required = false) Long tenantId,
            @RequestParam(value = "model_code", required = false) String modelCode,
            @RequestParam(value = "status", required = false) String status,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        if (tenantId == null) {
            String headerTenantId = httpRequest.getHeader("X-Tenant-Id");
            if (headerTenantId != null && !headerTenantId.trim().isEmpty()) {
                try {
                    tenantId = Long.parseLong(headerTenantId.trim());
                } catch (NumberFormatException ex) {
                    tenantId = 0L;
                }
            } else {
                tenantId = 0L;
            }
        }
        try {
            List<ModelEvalTask> tasks = governanceService.listEvalTasks(tenantId, modelCode, status);
            return ApiResult.success(tasks);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    @PostMapping("/eval-tasks/{taskId}/status")
    public ApiResult<Map<String, Object>> updateEvalTaskStatus(
            @PathVariable("taskId") Long taskId,
            @RequestBody Map<String, Object> statusRequest,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        String status = (String) statusRequest.get("status");
        if (status == null || status.trim().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "status is required");
        }
        Double accuracyScore = doubleOrNull(statusRequest.get("accuracy_score"));
        Double latencyMs = doubleOrNull(statusRequest.get("latency_ms"));
        Double passRate = doubleOrNull(statusRequest.get("pass_rate"));
        String resultSummary = statusRequest.get("result_summary") != null
                ? String.valueOf(statusRequest.get("result_summary")) : null;
        try {
            governanceService.updateEvalTaskStatus(taskId, status, accuracyScore, latencyMs, passRate, resultSummary);
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("taskId", taskId);
            result.put("status", status);
            return ApiResult.success(result);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    @GetMapping("/eval-tasks/summary")
    public ApiResult<Map<String, Object>> getEvalSummary(
            @RequestParam(value = "tenant_id", required = false) Long tenantId,
            @RequestParam(value = "model_code", required = false) String modelCode,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        if (tenantId == null) {
            String headerTenantId = httpRequest.getHeader("X-Tenant-Id");
            if (headerTenantId != null && !headerTenantId.trim().isEmpty()) {
                try {
                    tenantId = Long.parseLong(headerTenantId.trim());
                } catch (NumberFormatException ex) {
                    tenantId = 0L;
                }
            } else {
                tenantId = 0L;
            }
        }
        try {
            Map<String, Object> summary = governanceService.getEvalSummary(tenantId, modelCode);
            return ApiResult.success(summary);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    private Double doubleOrNull(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
