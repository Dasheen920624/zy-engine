package com.medkernel.config;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.config.entity.ConfigPackageRollbackRecord;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 配置包回滚控制器：提供配置包版本回滚管理的 REST API
 *
 * <p>API 端点：
 * <ul>
 *   <li>POST   /api/config-packages/rollbacks - 创建回滚记录</li>
 *   <li>POST   /api/config-packages/rollbacks/{recordId}/pre-check - 回滚前检查</li>
 *   <li>POST   /api/config-packages/rollbacks/{recordId}/approve - 审批回滚</li>
 *   <li>POST   /api/config-packages/rollbacks/{recordId}/execute - 执行回滚</li>
 *   <li>POST   /api/config-packages/rollbacks/{recordId}/post-check - 回滚后验证</li>
 *   <li>POST   /api/config-packages/rollbacks/{recordId}/cancel - 取消回滚</li>
 *   <li>GET    /api/config-packages/rollbacks - 查询回滚记录</li>
 *   <li>GET    /api/config-packages/rollbacks/{recordId} - 获取回滚记录详情</li>
 * </ul>
 */
@Tag(name = "Config Package Rollback")
@RestController
@RequestMapping("/api/config-packages/rollbacks")
public class ConfigPackageRollbackController {

    private final ConfigPackageRollbackService rollbackService;
    private final OrganizationContextService organizationContextService;

    public ConfigPackageRollbackController(ConfigPackageRollbackService rollbackService,
                                           OrganizationContextService organizationContextService) {
        this.rollbackService = rollbackService;
        this.organizationContextService = organizationContextService;
    }

    @Operation(summary = "Create rollback record")
    @PostMapping
    public ApiResult<ConfigPackageRollbackRecord> createRollbackRecord(
            @RequestBody CreateRollbackRequest request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        try {
            ConfigPackageRollbackRecord record = rollbackService.createRollbackRecord(
                    orgContext.getTenantId(),
                    request.getPackageCode(),
                    request.getPackageVersion(),
                    request.getTargetVersion(),
                    request.getRollbackType(),
                    request.getRollbackReason());
            return ApiResult.success(record);
        } catch (IllegalArgumentException ex) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, ex.getMessage());
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.CONFLICT, ex.getMessage());
        }
    }

    @Operation(summary = "Pre-check before rollback")
    @PostMapping("/{recordId}/pre-check")
    public ApiResult<ConfigPackageRollbackRecord> preCheck(@PathVariable Long recordId,
                                                            HttpServletRequest httpRequest) {
        try {
            ConfigPackageRollbackRecord record = rollbackService.preCheck(recordId);
            return ApiResult.success(record);
        } catch (IllegalArgumentException ex) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage());
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.CONFLICT, ex.getMessage());
        }
    }

    @Operation(summary = "Approve rollback")
    @PostMapping("/{recordId}/approve")
    public ApiResult<ConfigPackageRollbackRecord> approveRollback(
            @PathVariable Long recordId,
            @RequestBody ApproveRollbackRequest request,
            HttpServletRequest httpRequest) {
        try {
            ConfigPackageRollbackRecord record = rollbackService.approveRollback(recordId, request.getApprovedBy());
            return ApiResult.success(record);
        } catch (IllegalArgumentException ex) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage());
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.CONFLICT, ex.getMessage());
        }
    }

    @Operation(summary = "Execute rollback")
    @PostMapping("/{recordId}/execute")
    public ApiResult<ConfigPackageRollbackRecord> executeRollback(
            @PathVariable Long recordId,
            @RequestBody(required = false) ExecuteRollbackRequest request,
            HttpServletRequest httpRequest) {
        try {
            String rolledBackBy = request != null ? request.getRolledBackBy() : null;
            ConfigPackageRollbackRecord record = rollbackService.executeRollback(recordId, rolledBackBy);
            return ApiResult.success(record);
        } catch (IllegalArgumentException ex) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage());
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.CONFLICT, ex.getMessage());
        }
    }

    @Operation(summary = "Post-check after rollback")
    @PostMapping("/{recordId}/post-check")
    public ApiResult<ConfigPackageRollbackRecord> postCheck(@PathVariable Long recordId,
                                                             HttpServletRequest httpRequest) {
        try {
            ConfigPackageRollbackRecord record = rollbackService.postCheck(recordId);
            return ApiResult.success(record);
        } catch (IllegalArgumentException ex) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage());
        }
    }

    @Operation(summary = "Cancel rollback")
    @PostMapping("/{recordId}/cancel")
    public ApiResult<ConfigPackageRollbackRecord> cancelRollback(@PathVariable Long recordId,
                                                                   HttpServletRequest httpRequest) {
        try {
            ConfigPackageRollbackRecord record = rollbackService.cancelRollback(recordId);
            return ApiResult.success(record);
        } catch (IllegalArgumentException ex) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage());
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.CONFLICT, ex.getMessage());
        }
    }

    @Operation(summary = "List rollback records")
    @GetMapping
    public ApiResult<List<ConfigPackageRollbackRecord>> listRollbackRecords(
            @RequestParam(required = false) String packageCode,
            @RequestParam(required = false) String status,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        List<ConfigPackageRollbackRecord> records = rollbackService.listRollbackRecords(
                orgContext.getTenantId(), packageCode, status);
        return ApiResult.success(records);
    }

    @Operation(summary = "Get rollback record detail")
    @GetMapping("/{recordId}")
    public ApiResult<ConfigPackageRollbackRecord> getRollbackRecord(@PathVariable Long recordId,
                                                                      HttpServletRequest httpRequest) {
        ConfigPackageRollbackRecord record = rollbackService.getRollbackRecord(recordId);
        if (record == null) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, "Rollback record not found: " + recordId);
        }
        return ApiResult.success(record);
    }

    // ==================== 请求参数类 ====================

    public static class CreateRollbackRequest {
        private String packageCode;
        private String packageVersion;
        private String targetVersion;
        private String rollbackType;
        private String rollbackReason;

        public String getPackageCode() {
            return packageCode;
        }

        public void setPackageCode(String packageCode) {
            this.packageCode = packageCode;
        }

        public String getPackageVersion() {
            return packageVersion;
        }

        public void setPackageVersion(String packageVersion) {
            this.packageVersion = packageVersion;
        }

        public String getTargetVersion() {
            return targetVersion;
        }

        public void setTargetVersion(String targetVersion) {
            this.targetVersion = targetVersion;
        }

        public String getRollbackType() {
            return rollbackType;
        }

        public void setRollbackType(String rollbackType) {
            this.rollbackType = rollbackType;
        }

        public String getRollbackReason() {
            return rollbackReason;
        }

        public void setRollbackReason(String rollbackReason) {
            this.rollbackReason = rollbackReason;
        }
    }

    public static class ApproveRollbackRequest {
        private String approvedBy;

        public String getApprovedBy() {
            return approvedBy;
        }

        public void setApprovedBy(String approvedBy) {
            this.approvedBy = approvedBy;
        }
    }

    public static class ExecuteRollbackRequest {
        private String rolledBackBy;

        public String getRolledBackBy() {
            return rolledBackBy;
        }

        public void setRolledBackBy(String rolledBackBy) {
            this.rolledBackBy = rolledBackBy;
        }
    }
}
