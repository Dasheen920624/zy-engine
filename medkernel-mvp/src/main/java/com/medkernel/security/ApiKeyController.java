package com.medkernel.security;

import com.medkernel.common.ApiResult;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * API Key 管理 REST 控制器。
 * <p>
 * SEC-003: 提供第三方 API Key 的创建、查询、吊销功能。
 */
@RestController
@RequestMapping("/api/security/api-keys")
@Tag(name = "API Key 管理", description = "SEC-003 第三方鉴权：API Key 创建、查询、吊销")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final OrganizationContextService organizationContextService;

    public ApiKeyController(ApiKeyService apiKeyService, OrganizationContextService organizationContextService) {
        this.apiKeyService = apiKeyService;
        this.organizationContextService = organizationContextService;
    }

    @Operation(summary = "创建 API Key")
    @PostMapping
    public ResponseEntity<ApiResult<Map<String, Object>>> createKey(
            @RequestBody Map<String, Object> body, HttpServletRequest request) {
        OrganizationContext orgContext = organizationContextService.resolve(request);
        String tenantId = orgContext != null ? orgContext.getTenantId() : "default";

        String name = (String) body.get("name");
        String role = (String) body.getOrDefault("role", "API_CONSUMER");
        String expiresInDays = String.valueOf(body.getOrDefault("expires_in_days", 365));
        String operatorId = request.getHeader("X-Operator-Id");

        LocalDateTime expiresAt = null;
        if (expiresInDays != null) {
            try {
                int days = Integer.parseInt(expiresInDays);
                expiresAt = LocalDateTime.now().plusDays(days);
            } catch (NumberFormatException ignored) {
                // 不设置过期时间
            }
        }

        ApiKey apiKey = apiKeyService.createKey(name, tenantId, role, operatorId, expiresAt);
        return ResponseEntity.ok(ApiResult.success(apiKey.toCreateResponse()));
    }

    @Operation(summary = "列出 API Keys")
    @GetMapping
    public ResponseEntity<ApiResult<List<Map<String, Object>>>> listKeys(HttpServletRequest request) {
        OrganizationContext orgContext = organizationContextService.resolve(request);
        String tenantId = orgContext != null ? orgContext.getTenantId() : null;
        List<Map<String, Object>> keys = apiKeyService.listKeys(tenantId);
        return ResponseEntity.ok(ApiResult.success(keys));
    }

    @Operation(summary = "吊销 API Key")
    @DeleteMapping("/{keyId}")
    public ResponseEntity<ApiResult<Map<String, Object>>> revokeKey(
            @PathVariable String keyId, HttpServletRequest request) {
        OrganizationContext orgContext = organizationContextService.resolve(request);
        String tenantId = orgContext != null ? orgContext.getTenantId() : null;
        boolean revoked = apiKeyService.revokeKey(keyId, tenantId);
        Map<String, Object> result = new java.util.LinkedHashMap<String, Object>();
        result.put("key_id", keyId);
        result.put("revoked", revoked);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    @Operation(summary = "获取 Nonce 缓存统计")
    @GetMapping("/nonce-stats")
    public ResponseEntity<ApiResult<Map<String, Object>>> getNonceStats() {
        return ResponseEntity.ok(ApiResult.success(apiKeyService.getNonceStats()));
    }
}
