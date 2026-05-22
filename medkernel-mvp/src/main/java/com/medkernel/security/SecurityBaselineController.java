package com.medkernel.security;

import com.medkernel.audit.BaselineAuditChainService;
import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 安全基线 API。
 * 提供审计链校验、密钥轮换、漏洞扫描、安全基线状态查询。
 */
@Tag(name = "Security Baseline")
@RestController
@RequestMapping("/api/security")
public class SecurityBaselineController {
    private final BaselineAuditChainService auditChainService;
    private final KeyRotationService keyRotationService;
    private final OrganizationContextService organizationContextService;

    public SecurityBaselineController(BaselineAuditChainService auditChainService,
                                      KeyRotationService keyRotationService,
                                      OrganizationContextService organizationContextService) {
        this.auditChainService = auditChainService;
        this.keyRotationService = keyRotationService;
        this.organizationContextService = organizationContextService;
    }

    // ==================== 审计链 API ====================

    @Operation(summary = "Get audit chain status")
    @GetMapping("/audit-chain/status")
    public ApiResult<Map<String, Object>> getAuditChainStatus(HttpServletRequest httpRequest) {
        return ApiResult.success(auditChainService.getChainStatus());
    }

    @Operation(summary = "Verify audit chain")
    @PostMapping("/audit-chain/verify")
    public ApiResult<Map<String, Object>> verifyAuditChain(HttpServletRequest httpRequest) {
        return ApiResult.success(auditChainService.verifyChain());
    }

    @Operation(summary = "Append to chain")
    @PostMapping("/audit-chain/append")
    public ApiResult<Map<String, Object>> appendToChain(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        long auditLogId = toLong(request.get("audit_log_id"));
        String tenantId = orgContext.getTenantId();
        String engineType = (String) request.get("engine_type");
        String actionType = (String) request.get("action_type");
        String targetType = (String) request.get("target_type");
        String targetCode = (String) request.get("target_code");
        String operatorId = (String) request.get("operator_id");
        String detailJson = (String) request.get("detail_json");

        BaselineAuditChainService.AuditChainRecord record = auditChainService.appendToChain(
                auditLogId, tenantId, engineType, actionType,
                targetType, targetCode, operatorId, detailJson);
        return ApiResult.success(record.toView());
    }

    // ==================== 密钥轮换 API ====================

    @Operation(summary = "List key versions")
    @GetMapping("/keys")
    public ApiResult<List<Map<String, Object>>> listKeyVersions(HttpServletRequest httpRequest) {
        List<KeyRotationService.KeyVersion> keys = keyRotationService.listKeyVersions();
        List<Map<String, Object>> views = new ArrayList<Map<String, Object>>();
        for (KeyRotationService.KeyVersion key : keys) {
            views.add(key.toView());
        }
        return ApiResult.success(views);
    }

    @Operation(summary = "Get active key")
    @GetMapping("/keys/active")
    public ApiResult<Map<String, Object>> getActiveKey(HttpServletRequest httpRequest) {
        KeyRotationService.KeyVersion key = keyRotationService.getActiveKey();
        if (key == null) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, "No active key found");
        }
        return ApiResult.success(key.toView());
    }

    @Operation(summary = "Rotate key")
    @PostMapping("/keys/rotate")
    public ApiResult<Map<String, Object>> rotateKey(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        String newKeyAlias = (String) request.get("key_alias");
        String newKeyMaterial = (String) request.get("key_material");
        String rotatedBy = (String) request.get("rotated_by");
        if (newKeyMaterial == null || newKeyMaterial.isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "key_material is required");
        }
        try {
            KeyRotationService.KeyVersion newKey = keyRotationService.rotateKey(newKeyAlias, newKeyMaterial, rotatedBy);
            return ApiResult.success(newKey.toView());
        } catch (Exception e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "Revoke key")
    @PostMapping("/keys/{keyId}/revoke")
    public ApiResult<Map<String, Object>> revokeKey(
            @PathVariable long keyId,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        String revokedBy = (String) request.get("revoked_by");
        try {
            KeyRotationService.KeyVersion key = keyRotationService.revokeKey(keyId, revokedBy);
            return ApiResult.success(key.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    // ==================== 安全基线 API ====================

    @Operation(summary = "Get baseline status")
    @GetMapping("/baseline")
    public ApiResult<Map<String, Object>> getBaselineStatus(HttpServletRequest httpRequest) {
        Map<String, Object> baseline = keyRotationService.getSecurityBaselineStatus();
        // 附加审计链状态
        baseline.put("audit_chain", auditChainService.getChainStatus());
        return ApiResult.success(baseline);
    }

    @Operation(summary = "Perform vulnerability scan")
    @PostMapping("/vulnerability-scan")
    public ApiResult<Map<String, Object>> performVulnerabilityScan(HttpServletRequest httpRequest) {
        KeyRotationService.VulnerabilityScanResult result = keyRotationService.performVulnerabilityScan();
        return ApiResult.success(result.toView());
    }

    private long toLong(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(value.toString()); } catch (NumberFormatException e) { return 0; }
    }
}
