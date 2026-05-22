package com.medkernel.security;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SEC-002 数据权限管理 API：策略管理、权限分配、权限检查。
 */
@RestController
@RequestMapping("/api/security/data-permission")
public class DataPermissionController {

    private final DataPermissionService dataPermissionService;
    private final OrganizationContextService organizationContextService;

    public DataPermissionController(DataPermissionService dataPermissionService,
                                     OrganizationContextService organizationContextService) {
        this.dataPermissionService = dataPermissionService;
        this.organizationContextService = organizationContextService;
    }

    // ============================================================
    // 策略管理
    // ============================================================

    /**
     * 创建数据权限策略。
     */
    @PostMapping("/policies")
    public ApiResult<Map<String, Object>> createPolicy(@RequestBody Map<String, Object> body,
                                                        HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, body);
        DataPermissionPolicy policy = new DataPermissionPolicy();
        policy.setTenantId(resolveTenantId(orgCtx));
        policy.setPolicyCode(stringValue(body, "policy_code", "policyCode"));
        policy.setPolicyName(stringValue(body, "policy_name", "policyName"));
        policy.setPolicyType(stringValue(body, "policy_type", "policyType"));
        policy.setDescription(stringValue(body, "description", "description"));
        policy.setScopeExpression(stringValue(body, "scope_expression", "scopeExpression"));
        policy.setFilterExpression(stringValue(body, "filter_expression", "filterExpression"));
        policy.setPriority(stringValue(body, "priority", "priority"));
        policy.setEnabled(stringValue(body, "enabled", "enabled"));
        policy.setCreatedBy(resolveOperator(httpRequest));

        if (policy.getPolicyCode() == null || policy.getPolicyCode().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "policy_code is required");
        }
        if (policy.getPolicyName() == null || policy.getPolicyName().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "policy_name is required");
        }

        try {
            DataPermissionPolicy created = dataPermissionService.createPolicy(policy);
            return ApiResult.success(toPolicyView(created));
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    /**
     * 更新数据权限策略。
     */
    @PutMapping("/policies/{policyId}")
    public ApiResult<Map<String, Object>> updatePolicy(@PathVariable Long policyId,
                                                        @RequestBody Map<String, Object> body,
                                                        HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, body);
        DataPermissionPolicy policy = new DataPermissionPolicy();
        policy.setId(policyId);
        policy.setTenantId(resolveTenantId(orgCtx));
        policy.setPolicyName(stringValue(body, "policy_name", "policyName"));
        policy.setPolicyType(stringValue(body, "policy_type", "policyType"));
        policy.setDescription(stringValue(body, "description", "description"));
        policy.setScopeExpression(stringValue(body, "scope_expression", "scopeExpression"));
        policy.setFilterExpression(stringValue(body, "filter_expression", "filterExpression"));
        policy.setPriority(stringValue(body, "priority", "priority"));
        policy.setEnabled(stringValue(body, "enabled", "enabled"));
        policy.setUpdatedBy(resolveOperator(httpRequest));

        try {
            DataPermissionPolicy updated = dataPermissionService.updatePolicy(policy);
            return ApiResult.success(toPolicyView(updated));
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage());
        }
    }

    /**
     * 查询数据权限策略列表。
     */
    @GetMapping("/policies")
    public ApiResult<List<Map<String, Object>>> listPolicies(
            @RequestParam(required = false) String policyType,
            @RequestParam(required = false) String enabled,
            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        List<DataPermissionPolicy> policies = dataPermissionService.listPolicies(
                resolveTenantId(orgCtx), policyType, enabled);
        return ApiResult.success(toPolicyViews(policies));
    }

    // ============================================================
    // 权限分配
    // ============================================================

    /**
     * 分配数据权限。
     */
    @PostMapping("/assignments")
    public ApiResult<Map<String, Object>> assignPermission(@RequestBody Map<String, Object> body,
                                                             HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, body);
        DataPermissionAssignment assignment = new DataPermissionAssignment();
        assignment.setTenantId(resolveTenantId(orgCtx));
        assignment.setAssignmentCode(stringValue(body, "assignment_code", "assignmentCode"));
        assignment.setPrincipalType(stringValue(body, "principal_type", "principalType"));
        assignment.setPrincipalCode(stringValue(body, "principal_code", "principalCode"));
        assignment.setPrincipalName(stringValue(body, "principal_name", "principalName"));
        assignment.setPolicyId(longValue(body, "policy_id", "policyId"));
        assignment.setPolicyCode(stringValue(body, "policy_code", "policyCode"));
        assignment.setPolicyName(stringValue(body, "policy_name", "policyName"));
        assignment.setResourceType(stringValue(body, "resource_type", "resourceType"));
        assignment.setEffect(stringValue(body, "effect", "effect"));
        assignment.setConditions(stringValue(body, "conditions", "conditions"));
        assignment.setEnabled(stringValue(body, "enabled", "enabled"));
        assignment.setCreatedBy(resolveOperator(httpRequest));

        if (assignment.getPrincipalType() == null || assignment.getPrincipalType().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "principal_type is required");
        }
        if (assignment.getPrincipalCode() == null || assignment.getPrincipalCode().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "principal_code is required");
        }
        if (assignment.getPolicyId() == null) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "policy_id is required");
        }

        try {
            DataPermissionAssignment created = dataPermissionService.assignPermission(assignment);
            return ApiResult.success(toAssignmentView(created));
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    /**
     * 更新权限分配。
     */
    @PutMapping("/assignments/{assignmentId}")
    public ApiResult<Map<String, Object>> updateAssignment(@PathVariable Long assignmentId,
                                                             @RequestBody Map<String, Object> body,
                                                             HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, body);
        DataPermissionAssignment assignment = new DataPermissionAssignment();
        assignment.setId(assignmentId);
        assignment.setTenantId(resolveTenantId(orgCtx));
        assignment.setPrincipalType(stringValue(body, "principal_type", "principalType"));
        assignment.setPrincipalCode(stringValue(body, "principal_code", "principalCode"));
        assignment.setPrincipalName(stringValue(body, "principal_name", "principalName"));
        assignment.setPolicyId(longValue(body, "policy_id", "policyId"));
        assignment.setPolicyCode(stringValue(body, "policy_code", "policyCode"));
        assignment.setPolicyName(stringValue(body, "policy_name", "policyName"));
        assignment.setResourceType(stringValue(body, "resource_type", "resourceType"));
        assignment.setEffect(stringValue(body, "effect", "effect"));
        assignment.setConditions(stringValue(body, "conditions", "conditions"));
        assignment.setEnabled(stringValue(body, "enabled", "enabled"));
        assignment.setUpdatedBy(resolveOperator(httpRequest));

        try {
            DataPermissionAssignment updated = dataPermissionService.updateAssignment(assignment);
            return ApiResult.success(toAssignmentView(updated));
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage());
        }
    }

    /**
     * 查询权限分配列表。
     */
    @GetMapping("/assignments")
    public ApiResult<List<Map<String, Object>>> listAssignments(
            @RequestParam(required = false) String principalType,
            @RequestParam(required = false) String principalCode,
            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        List<DataPermissionAssignment> assignments = dataPermissionService.listAssignments(
                resolveTenantId(orgCtx), principalType, principalCode);
        return ApiResult.success(toAssignmentViews(assignments));
    }

    /**
     * 移除权限分配。
     */
    @DeleteMapping("/assignments/{assignmentId}")
    public ApiResult<String> removeAssignment(@PathVariable Long assignmentId) {
        try {
            dataPermissionService.removeAssignment(assignmentId);
            return ApiResult.success("removed");
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage());
        }
    }

    // ============================================================
    // 权限检查
    // ============================================================

    /**
     * 检查权限。
     */
    @PostMapping("/check")
    public ApiResult<Map<String, Object>> checkPermission(@RequestBody Map<String, Object> body,
                                                            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, body);
        Long tenantId = resolveTenantId(orgCtx);
        String principalType = stringValue(body, "principal_type", "principalType");
        String principalCode = stringValue(body, "principal_code", "principalCode");
        String resourceType = stringValue(body, "resource_type", "resourceType");
        String action = stringValue(body, "action", "action");

        if (principalType == null || principalType.isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "principal_type is required");
        }
        if (principalCode == null || principalCode.isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "principal_code is required");
        }
        if (resourceType == null || resourceType.isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "resource_type is required");
        }

        boolean allowed = dataPermissionService.checkPermission(tenantId, principalType, principalCode,
                resourceType, action);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("allowed", allowed);
        result.put("principal_type", principalType);
        result.put("principal_code", principalCode);
        result.put("resource_type", resourceType);
        return ApiResult.success(result);
    }

    /**
     * 获取数据过滤条件。
     */
    @PostMapping("/filter")
    public ApiResult<List<Map<String, Object>>> filterData(@RequestBody Map<String, Object> body,
                                                             HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, body);
        Long tenantId = resolveTenantId(orgCtx);
        String principalType = stringValue(body, "principal_type", "principalType");
        String principalCode = stringValue(body, "principal_code", "principalCode");
        String resourceType = stringValue(body, "resource_type", "resourceType");

        if (principalType == null || principalType.isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "principal_type is required");
        }
        if (principalCode == null || principalCode.isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "principal_code is required");
        }
        if (resourceType == null || resourceType.isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "resource_type is required");
        }

        List<Map<String, Object>> filters = dataPermissionService.filterData(
                tenantId, principalType, principalCode, resourceType);
        return ApiResult.success(filters);
    }

    // ============================================================
    // 内部方法
    // ============================================================

    private Long resolveTenantId(OrganizationContext orgCtx) {
        try {
            return Long.parseLong(orgCtx.getTenantId());
        } catch (NumberFormatException ex) {
            return 1L;
        }
    }

    private String resolveOperator(HttpServletRequest request) {
        String username = request.getHeader("X-Username");
        if (username != null && !username.trim().isEmpty()) {
            return username.trim();
        }
        return "system";
    }

    private Long longValue(Map<String, Object> body, String snakeKey, String camelKey) {
        Object value = body.get(snakeKey);
        if (value == null) {
            value = body.get(camelKey);
        }
        if (value == null) {
            return null;
        }
        return Long.valueOf(String.valueOf(value));
    }

    private String stringValue(Map<String, Object> body, String snakeKey, String camelKey) {
        Object value = body.get(snakeKey);
        if (value == null) {
            value = body.get(camelKey);
        }
        return value == null ? null : String.valueOf(value);
    }

    private List<Map<String, Object>> toPolicyViews(List<DataPermissionPolicy> policies) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (DataPermissionPolicy policy : policies) {
            result.add(toPolicyView(policy));
        }
        return result;
    }

    private Map<String, Object> toPolicyView(DataPermissionPolicy policy) {
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("id", policy.getId());
        view.put("tenant_id", policy.getTenantId());
        view.put("policy_code", policy.getPolicyCode());
        view.put("policy_name", policy.getPolicyName());
        view.put("policy_type", policy.getPolicyType());
        view.put("description", policy.getDescription());
        view.put("scope_expression", policy.getScopeExpression());
        view.put("filter_expression", policy.getFilterExpression());
        view.put("priority", policy.getPriority());
        view.put("enabled", policy.getEnabled());
        view.put("created_by", policy.getCreatedBy());
        view.put("created_time", policy.getCreatedTime());
        view.put("updated_by", policy.getUpdatedBy());
        view.put("updated_time", policy.getUpdatedTime());
        return view;
    }

    private List<Map<String, Object>> toAssignmentViews(List<DataPermissionAssignment> assignments) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (DataPermissionAssignment assignment : assignments) {
            result.add(toAssignmentView(assignment));
        }
        return result;
    }

    private Map<String, Object> toAssignmentView(DataPermissionAssignment assignment) {
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("id", assignment.getId());
        view.put("tenant_id", assignment.getTenantId());
        view.put("assignment_code", assignment.getAssignmentCode());
        view.put("principal_type", assignment.getPrincipalType());
        view.put("principal_code", assignment.getPrincipalCode());
        view.put("principal_name", assignment.getPrincipalName());
        view.put("policy_id", assignment.getPolicyId());
        view.put("policy_code", assignment.getPolicyCode());
        view.put("policy_name", assignment.getPolicyName());
        view.put("resource_type", assignment.getResourceType());
        view.put("effect", assignment.getEffect());
        view.put("conditions", assignment.getConditions());
        view.put("enabled", assignment.getEnabled());
        view.put("created_by", assignment.getCreatedBy());
        view.put("created_time", assignment.getCreatedTime());
        view.put("updated_by", assignment.getUpdatedBy());
        view.put("updated_time", assignment.getUpdatedTime());
        return view;
    }
}
