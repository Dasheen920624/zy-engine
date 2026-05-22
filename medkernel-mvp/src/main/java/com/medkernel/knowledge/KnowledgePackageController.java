package com.medkernel.knowledge;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge/packages")
public class KnowledgePackageController {
    private static final Logger log = LoggerFactory.getLogger(KnowledgePackageController.class);

    private final KnowledgePackageService knowledgePackageService;
    private final OrganizationContextService organizationContextService;

    public KnowledgePackageController(KnowledgePackageService knowledgePackageService,
                                      OrganizationContextService organizationContextService) {
        this.knowledgePackageService = knowledgePackageService;
        this.organizationContextService = organizationContextService;
    }

    @PostMapping("/export")
    public ApiResult<Map<String, Object>> exportPackage(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        try {
            KnowledgePackage pkg = toKnowledgePackage(request, orgContext);
            KnowledgePackage exported = knowledgePackageService.exportPackage(pkg);
            return ApiResult.success(toView(exported));
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        } catch (IllegalStateException e) {
            log.error("Knowledge package export failed", e);
            return ApiResult.failure(ErrorCode.UNKNOWN_ERROR, "操作失败，请稍后重试");
        }
    }

    @PostMapping("/{packageId}/import")
    public ApiResult<Map<String, Object>> importPackage(
            @PathVariable Long packageId,
            @RequestBody(required = false) Map<String, Object> request,
            HttpServletRequest httpRequest) {
        String conflictStrategy = "SKIP";
        if (request != null && request.containsKey("conflict_strategy")) {
            conflictStrategy = (String) request.get("conflict_strategy");
        }
        try {
            Map<String, Object> result = knowledgePackageService.importPackage(packageId, conflictStrategy);
            return ApiResult.success(result);
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        } catch (IllegalStateException e) {
            log.error("Knowledge package import failed", e);
            return ApiResult.failure(ErrorCode.UNKNOWN_ERROR, "操作失败，请稍后重试");
        }
    }

    @PostMapping("/{packageId}/preview")
    public ApiResult<Map<String, Object>> previewImport(
            @PathVariable Long packageId,
            HttpServletRequest httpRequest) {
        try {
            Map<String, Object> preview = knowledgePackageService.previewImport(packageId);
            return ApiResult.success(preview);
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @GetMapping
    public ApiResult<List<Map<String, Object>>> listPackages(
            @RequestParam(required = false) Long tenant_id,
            @RequestParam(required = false) String status,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        Long effectiveTenantId = tenant_id;
        if (effectiveTenantId == null && orgContext.getTenantId() != null) {
            try {
                effectiveTenantId = Long.valueOf(orgContext.getTenantId());
            } catch (NumberFormatException ignored) {
                // 非数字 tenantId 不转换
            }
        }
        List<KnowledgePackage> packages = knowledgePackageService.listPackages(effectiveTenantId, status);
        List<Map<String, Object>> views = new ArrayList<Map<String, Object>>();
        for (KnowledgePackage pkg : packages) {
            views.add(toView(pkg));
        }
        return ApiResult.success(views);
    }

    @GetMapping("/{packageId}")
    public ApiResult<Map<String, Object>> getPackage(
            @PathVariable Long packageId,
            HttpServletRequest httpRequest) {
        try {
            KnowledgePackage pkg = knowledgePackageService.getPackage(packageId);
            return ApiResult.success(toDetailView(pkg));
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/{packageId}/sync")
    public ApiResult<Map<String, Object>> syncPackage(
            @PathVariable Long packageId,
            @RequestBody(required = false) Map<String, Object> request,
            HttpServletRequest httpRequest) {
        String syncMode = "MANUAL";
        if (request != null && request.containsKey("sync_mode")) {
            syncMode = (String) request.get("sync_mode");
        }
        try {
            Map<String, Object> result = knowledgePackageService.syncPackage(packageId, syncMode);
            return ApiResult.success(result);
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        } catch (IllegalStateException e) {
            log.error("Knowledge package sync failed", e);
            return ApiResult.failure(ErrorCode.UNKNOWN_ERROR, "操作失败，请稍后重试");
        }
    }

    @GetMapping("/{packageId}/sync-status")
    public ApiResult<Map<String, Object>> getSyncStatus(
            @PathVariable Long packageId,
            HttpServletRequest httpRequest) {
        try {
            Map<String, Object> status = knowledgePackageService.getSyncStatus(packageId);
            return ApiResult.success(status);
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, e.getMessage());
        }
    }

    // ==================== 内部方法 ====================

    private KnowledgePackage toKnowledgePackage(Map<String, Object> request, OrganizationContext orgContext) {
        KnowledgePackage pkg = new KnowledgePackage();
        pkg.setPackageCode(string(request.get("package_code"), null));
        pkg.setPackageName(string(request.get("package_name"), null));
        pkg.setPackageVersion(string(request.get("package_version"), null));
        pkg.setDescription(string(request.get("description"), null));
        pkg.setExportType(string(request.get("export_type"), "FULL"));
        pkg.setSourceTenantId(string(request.get("source_tenant_id"),
                orgContext != null ? orgContext.getTenantId() : null));
        pkg.setSourceTenantName(string(request.get("source_tenant_name"), null));
        pkg.setTargetTenantId(string(request.get("target_tenant_id"), null));
        pkg.setTargetTenantName(string(request.get("target_tenant_name"), null));
        pkg.setConflictStrategy(string(request.get("conflict_strategy"), "SKIP"));
        pkg.setSyncMode(string(request.get("sync_mode"), "MANUAL"));
        pkg.setCreatedBy(string(request.get("created_by"), null));

        if (orgContext != null && orgContext.getTenantId() != null) {
            try {
                pkg.setTenantId(Long.valueOf(orgContext.getTenantId()));
            } catch (NumberFormatException ignored) {
                // 非数字 tenantId 不设置
            }
        }
        return pkg;
    }

    private Map<String, Object> toView(KnowledgePackage pkg) {
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("id", pkg.getId());
        view.put("tenant_id", pkg.getTenantId());
        view.put("package_code", pkg.getPackageCode());
        view.put("package_name", pkg.getPackageName());
        view.put("package_version", pkg.getPackageVersion());
        view.put("description", pkg.getDescription());
        view.put("export_type", pkg.getExportType());
        view.put("status", pkg.getStatus());
        view.put("source_tenant_id", pkg.getSourceTenantId());
        view.put("source_tenant_name", pkg.getSourceTenantName());
        view.put("target_tenant_id", pkg.getTargetTenantId());
        view.put("target_tenant_name", pkg.getTargetTenantName());
        view.put("rule_count", pkg.getRuleCount());
        view.put("terminology_count", pkg.getTerminologyCount());
        view.put("pathway_count", pkg.getPathwayCount());
        view.put("graph_count", pkg.getGraphCount());
        view.put("source_count", pkg.getSourceCount());
        view.put("content_hash", pkg.getContentHash());
        view.put("conflict_strategy", pkg.getConflictStrategy());
        view.put("sync_mode", pkg.getSyncMode());
        view.put("sync_status", pkg.getSyncStatus());
        view.put("sync_error", pkg.getSyncError());
        view.put("sync_time", pkg.getSyncTime() == null ? null
                : java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(pkg.getSyncTime()));
        view.put("created_by", pkg.getCreatedBy());
        view.put("created_time", pkg.getCreatedTime() == null ? null
                : java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(pkg.getCreatedTime()));
        view.put("updated_by", pkg.getUpdatedBy());
        view.put("updated_time", pkg.getUpdatedTime() == null ? null
                : java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(pkg.getUpdatedTime()));
        return view;
    }

    private Map<String, Object> toDetailView(KnowledgePackage pkg) {
        Map<String, Object> view = toView(pkg);
        // 详情视图不包含完整 contentJson（可能很大），仅包含摘要信息
        view.put("content_json_length", pkg.getContentJson() == null ? 0 : pkg.getContentJson().length());
        return view;
    }

    private String string(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? defaultValue : text;
    }
}
