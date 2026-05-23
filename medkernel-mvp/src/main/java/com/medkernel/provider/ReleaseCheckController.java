package com.medkernel.provider;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.organization.OrganizationContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/release-check")
@Tag(name = "发布检查", description = "发布前检查清单管理、检查执行和审批")
public class ReleaseCheckController {

    private final ReleaseCheckService releaseCheckService;
    private final OrganizationContextService orgContextService;

    public ReleaseCheckController(ReleaseCheckService releaseCheckService,
                                  OrganizationContextService orgContextService) {
        this.releaseCheckService = releaseCheckService;
        this.orgContextService = orgContextService;
    }

    // =========================================================================
    // 清单管理
    // =========================================================================

    @Operation(summary = "创建发布检查清单")
    @PostMapping("/checklists")
    public ApiResult<ReleaseChecklist> createChecklist(
            @RequestBody ReleaseChecklist checklist,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        try {
            ReleaseChecklist created = releaseCheckService.createChecklist(checklist);
            return ApiResult.success(created);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    @Operation(summary = "更新发布检查清单")
    @PutMapping("/checklists/{checklistId}")
    public ApiResult<ReleaseChecklist> updateChecklist(
            @PathVariable("checklistId") Long checklistId,
            @RequestBody ReleaseChecklist checklist,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        checklist.setId(checklistId);
        try {
            ReleaseChecklist updated = releaseCheckService.updateChecklist(checklist);
            return ApiResult.success(updated);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    @Operation(summary = "查询发布检查清单列表")
    @GetMapping("/checklists")
    public ApiResult<List<ReleaseChecklist>> listChecklists(
            @RequestParam(value = "tenant_id", required = false) Long tenantId,
            @RequestParam(value = "resource_type", required = false) String resourceType,
            @RequestParam(value = "enabled", required = false) String enabled,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        if (tenantId == null) {
            tenantId = resolveTenantId(httpRequest);
        }
        try {
            List<ReleaseChecklist> checklists = releaseCheckService.listChecklists(tenantId, resourceType, enabled);
            return ApiResult.success(checklists);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    // =========================================================================
    // 检查执行
    // =========================================================================

    @Operation(summary = "执行发布检查")
    @PostMapping("/execute")
    public ApiResult<ReleaseCheckResult> executeCheck(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        Long tenantId = request.get("tenant_id") != null ? Long.parseLong(request.get("tenant_id")) : null;
        if (tenantId == null) {
            tenantId = resolveTenantId(httpRequest);
        }
        String resourceType = request.get("resource_type");
        String resourceCode = request.get("resource_code");
        String resourceVersion = request.get("resource_version");
        String checkedBy = request.get("checked_by");
        if (resourceType == null || resourceType.trim().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "resource_type is required");
        }
        if (resourceCode == null || resourceCode.trim().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "resource_code is required");
        }
        try {
            ReleaseCheckResult result = releaseCheckService.executeCheck(
                    tenantId, resourceType, resourceCode, resourceVersion, checkedBy);
            return ApiResult.success(result);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    @Operation(summary = "查询检查结果列表")
    @GetMapping("/results")
    public ApiResult<List<ReleaseCheckResult>> listCheckResults(
            @RequestParam(value = "tenant_id", required = false) Long tenantId,
            @RequestParam(value = "resource_type", required = false) String resourceType,
            @RequestParam(value = "check_status", required = false) String checkStatus,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        if (tenantId == null) {
            tenantId = resolveTenantId(httpRequest);
        }
        try {
            List<ReleaseCheckResult> results = releaseCheckService.listCheckResults(tenantId, resourceType, checkStatus);
            return ApiResult.success(results);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    @Operation(summary = "获取检查结果详情")
    @GetMapping("/results/{checkResultId}")
    public ApiResult<ReleaseCheckResult> getCheckResult(
            @PathVariable("checkResultId") Long checkResultId,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        try {
            ReleaseCheckResult result = releaseCheckService.getCheckResult(checkResultId);
            if (result == null) {
                return ApiResult.notFound("检查结果不存在: " + checkResultId);
            }
            return ApiResult.success(result);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    // =========================================================================
    // 审批管理
    // =========================================================================

    @Operation(summary = "审批通过发布")
    @PostMapping("/results/{checkResultId}/approve")
    public ApiResult<Map<String, Object>> approveRelease(
            @PathVariable("checkResultId") Long checkResultId,
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        String approvedBy = request.get("approved_by");
        String approvalNote = request.get("approval_note");
        if (approvedBy == null || approvedBy.trim().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "approved_by is required");
        }
        try {
            releaseCheckService.approveRelease(checkResultId, approvedBy, approvalNote);
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("checkResultId", checkResultId);
            result.put("checkStatus", "PASSED");
            return ApiResult.success(result);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    @Operation(summary = "豁免阻断项")
    @PostMapping("/results/{checkResultId}/waive")
    public ApiResult<Map<String, Object>> waiveBlock(
            @PathVariable("checkResultId") Long checkResultId,
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        String waivedBy = request.get("waived_by");
        String waiveReason = request.get("waive_reason");
        if (waivedBy == null || waivedBy.trim().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "waived_by is required");
        }
        if (waiveReason == null || waiveReason.trim().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "waive_reason is required");
        }
        try {
            releaseCheckService.waiveBlock(checkResultId, waivedBy, waiveReason);
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("checkResultId", checkResultId);
            result.put("checkStatus", "WAIVED");
            return ApiResult.success(result);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    @Operation(summary = "驳回发布")
    @PostMapping("/results/{checkResultId}/reject")
    public ApiResult<Map<String, Object>> rejectRelease(
            @PathVariable("checkResultId") Long checkResultId,
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        String approvedBy = request.get("approved_by");
        String approvalNote = request.get("approval_note");
        if (approvedBy == null || approvedBy.trim().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "approved_by is required");
        }
        try {
            releaseCheckService.rejectRelease(checkResultId, approvedBy, approvalNote);
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("checkResultId", checkResultId);
            result.put("checkStatus", "FAILED");
            return ApiResult.success(result);
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
}
