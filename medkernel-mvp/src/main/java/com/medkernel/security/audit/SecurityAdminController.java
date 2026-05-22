package com.medkernel.security.audit;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.organization.OrganizationContextService;
import com.medkernel.security.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

/**
 * 安全管理控制器：审计链校验、密钥管理、安全基线检查。
 */
@Tag(name = "Security Admin")
@RestController
@RequestMapping("/api/security/admin")
public class SecurityAdminController {

    private static final Logger log = LoggerFactory.getLogger(SecurityAdminController.class);

    private final AuditChainService auditChainService;
    private final KeyManagementService keyManagementService;
    private final OrganizationContextService organizationContextService;

    public SecurityAdminController(AuditChainService auditChainService,
                                   KeyManagementService keyManagementService,
                                   OrganizationContextService organizationContextService) {
        this.auditChainService = auditChainService;
        this.keyManagementService = keyManagementService;
        this.organizationContextService = organizationContextService;
    }

    // ==================== 审计链校验 ====================

    /**
     * 校验指定审计表的链完整性。
     */
    @Operation(summary = "Verify audit chain")
    @PostMapping("/audit-chain/verify")
    public ApiResult<Map<String, Object>> verifyAuditChain(@RequestBody Map<String, String> body) {
        String tableName = body.get("table_name");
        if (tableName == null || tableName.trim().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "审计表名不能为空");
        }

        // 安全检查：只允许校验已知的审计表
        if (!isValidAuditTable(tableName)) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "不支持的审计表: " + tableName);
        }

        AuditChainCheckpoint checkpoint = auditChainService.verifyChain(tableName);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checkpoint_id", checkpoint.getId());
        result.put("checkpoint_time", checkpoint.getCheckpointTime());
        result.put("chain_status", checkpoint.getChainStatus());
        result.put("total_records", checkpoint.getTotalRecords());
        result.put("valid_records", checkpoint.getValidRecords());
        result.put("broken_records", checkpoint.getBrokenRecords());
        result.put("first_broken_id", checkpoint.getFirstBrokenId());

        return ApiResult.success(result);
    }

    /**
     * 获取所有审计表的校验状态。
     */
    @Operation(summary = "Get audit chain status")
    @GetMapping("/audit-chain/status")
    public ApiResult<Map<String, Object>> getAuditChainStatus() {
        Map<String, Object> status = new LinkedHashMap<>();

        String[] tables = {"engine_audit_log", "sec_auth_audit_log", "sec_sso_audit_log"};
        for (String table : tables) {
            Map<String, Object> tableStatus = new LinkedHashMap<>();
            tableStatus.put("status", "NOT_VERIFIED");
            status.put(table, tableStatus);
        }

        return ApiResult.success(status);
    }

    // ==================== 密钥管理 ====================

    /**
     * 获取当前活跃密钥信息（不包含密钥材料）。
     */
    @Operation(summary = "Get active key")
    @GetMapping("/keys/active")
    public ApiResult<Map<String, Object>> getActiveKey() {
        EncryptionKey key = keyManagementService.getActiveKey();
        if (key == null) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, "未找到活跃的加密密钥");
        }

        return ApiResult.success(serializeKey(key, false));
    }

    /**
     * 列出所有密钥（不包含密钥材料）。
     */
    @Operation(summary = "List keys")
    @GetMapping("/keys")
    public ApiResult<List<Map<String, Object>>> listKeys() {
        List<EncryptionKey> keys = keyManagementService.listKeys();
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (EncryptionKey key : keys) {
            result.add(serializeKey(key, false));
        }
        return ApiResult.success(result);
    }

    /**
     * 执行密钥轮换。
     */
    @Operation(summary = "Rotate key")
    @PostMapping("/keys/rotate")
    public ApiResult<Map<String, Object>> rotateKey(@RequestBody Map<String, String> body,
                                                     HttpServletRequest request) {
        String description = body.getOrDefault("description", "手动轮换");
        String operatorId = SecurityContext.getUserId() != null ?
                String.valueOf(SecurityContext.getUserId()) : "system";

        EncryptionKey newKey = keyManagementService.rotateKey(description, operatorId);

        return ApiResult.success(serializeKey(newKey, false));
    }

    /**
     * 加密测试接口。
     */
    @Operation(summary = "Encrypt")
    @PostMapping("/encrypt")
    public ApiResult<Map<String, String>> encrypt(@RequestBody Map<String, String> body) {
        String plaintext = body.get("plaintext");
        if (plaintext == null || plaintext.trim().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "明文不能为空");
        }

        String encrypted = keyManagementService.encrypt(plaintext);

        Map<String, String> result = new LinkedHashMap<>();
        result.put("plaintext", plaintext);
        result.put("encrypted", encrypted);
        return ApiResult.success(result);
    }

    /**
     * 解密测试接口。
     */
    @Operation(summary = "Decrypt")
    @PostMapping("/decrypt")
    public ApiResult<Map<String, String>> decrypt(@RequestBody Map<String, String> body) {
        String encrypted = body.get("encrypted");
        if (encrypted == null || encrypted.trim().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "密文不能为空");
        }

        try {
            String decrypted = keyManagementService.decrypt(encrypted);

            Map<String, String> result = new LinkedHashMap<>();
            result.put("encrypted", encrypted);
            result.put("decrypted", decrypted);
            return ApiResult.success(result);
        } catch (Exception e) {
            log.error("Decryption failed", e);
            return ApiResult.failure(ErrorCode.UNKNOWN_ERROR, "操作失败，请稍后重试");
        }
    }

    // ==================== 安全基线检查 ====================

    /**
     * 获取安全基线状态。
     */
    @Operation(summary = "Get security baseline")
    @GetMapping("/baseline")
    public ApiResult<Map<String, Object>> getSecurityBaseline() {
        Map<String, Object> baseline = new LinkedHashMap<>();

        // 密钥状态
        EncryptionKey activeKey = keyManagementService.getActiveKey();
        Map<String, Object> keyStatus = new LinkedHashMap<>();
        keyStatus.put("has_active_key", activeKey != null);
        if (activeKey != null) {
            keyStatus.put("algorithm", activeKey.getAlgorithm());
            keyStatus.put("activated_at", activeKey.getActivatedAt());
        }
        baseline.put("encryption", keyStatus);

        // 审计链状态
        Map<String, Object> auditStatus = new LinkedHashMap<>();
        auditStatus.put("tables", new String[]{"engine_audit_log", "sec_auth_audit_log", "sec_sso_audit_log"});
        auditStatus.put("hash_algorithm", "SHA-256");
        baseline.put("audit_chain", auditStatus);

        // 安全建议
        List<String> recommendations = new java.util.ArrayList<>();
        if (activeKey == null) {
            recommendations.add("建议立即生成加密密钥");
        }
        baseline.put("recommendations", recommendations);

        return ApiResult.success(baseline);
    }

    // ==================== 辅助方法 ====================

    private boolean isValidAuditTable(String tableName) {
        return "engine_audit_log".equals(tableName) ||
               "sec_auth_audit_log".equals(tableName) ||
               "sec_sso_audit_log".equals(tableName);
    }

    private Map<String, Object> serializeKey(EncryptionKey key, boolean includeMaterial) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", key.getId());
        result.put("key_id", key.getKeyId());
        result.put("key_version", key.getKeyVersion());
        result.put("algorithm", key.getAlgorithm());
        result.put("status", key.getStatus());
        result.put("activated_at", key.getActivatedAt());
        result.put("deprecated_at", key.getDeprecatedAt());
        result.put("expires_at", key.getExpiresAt());
        result.put("description", key.getDescription());
        result.put("created_by", key.getCreatedBy());
        result.put("created_time", key.getCreatedTime());

        if (includeMaterial) {
            result.put("key_material", key.getKeyMaterial());
        }

        return result;
    }
}
