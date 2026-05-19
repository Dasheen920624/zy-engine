package com.medkernel.security;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户同步控制器：管理院内用户体系同步
 */
@RestController
@RequestMapping("/api/security")
public class UserSyncController {

    private final UserSyncService userSyncService;
    private final SecurityPersistenceService persistenceService;

    public UserSyncController(UserSyncService userSyncService, SecurityPersistenceService persistenceService) {
        this.userSyncService = userSyncService;
        this.persistenceService = persistenceService;
    }

    /**
     * 全量同步
     */
    @PostMapping("/identity-providers/{providerId}/sync/full")
    public ApiResult<UserSyncJob> fullSync(@PathVariable Long providerId,
                                           @RequestParam String tenantId,
                                           @RequestParam(defaultValue = "SYSTEM") String triggeredBy) {
        try {
            UserSyncJob job = userSyncService.fullSync(providerId, tenantId, triggeredBy);
            return ApiResult.success(job);
        } catch (Exception e) {
            return ApiResult.failure(ErrorCode.INTERNAL_ERROR, "全量同步失败: " + e.getMessage());
        }
    }

    /**
     * 增量同步
     */
    @PostMapping("/identity-providers/{providerId}/sync/incremental")
    public ApiResult<UserSyncJob> incrementalSync(@PathVariable Long providerId,
                                                  @RequestParam String tenantId,
                                                  @RequestParam(defaultValue = "SYSTEM") String triggeredBy) {
        try {
            UserSyncJob job = userSyncService.incrementalSync(providerId, tenantId, triggeredBy);
            return ApiResult.success(job);
        } catch (Exception e) {
            return ApiResult.failure(ErrorCode.INTERNAL_ERROR, "增量同步失败: " + e.getMessage());
        }
    }

    /**
     * 手动同步
     */
    @PostMapping("/identity-providers/{providerId}/sync/manual")
    public ApiResult<UserSyncJob> manualSync(@PathVariable Long providerId,
                                             @RequestParam String tenantId,
                                             @RequestBody List<String> externalSubjects,
                                             @RequestParam(defaultValue = "ADMIN") String triggeredBy) {
        try {
            if (externalSubjects == null || externalSubjects.isEmpty()) {
                return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "外部用户标识列表不能为空");
            }
            UserSyncJob job = userSyncService.manualSync(providerId, tenantId, externalSubjects, triggeredBy);
            return ApiResult.success(job);
        } catch (Exception e) {
            return ApiResult.failure(ErrorCode.INTERNAL_ERROR, "手动同步失败: " + e.getMessage());
        }
    }

    /**
     * 获取同步任务状态
     */
    @GetMapping("/sync-jobs/{jobId}")
    public ApiResult<UserSyncJob> getSyncJobStatus(@PathVariable Long jobId) {
        try {
            UserSyncJob job = userSyncService.getSyncJobStatus(jobId);
            if (job == null) {
                return ApiResult.failure(ErrorCode.NOT_FOUND, "同步任务不存在");
            }
            return ApiResult.success(job);
        } catch (Exception e) {
            return ApiResult.failure(ErrorCode.INTERNAL_ERROR, "获取同步任务状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取同步历史
     */
    @GetMapping("/identity-providers/{providerId}/sync-jobs")
    public ApiResult<List<UserSyncJob>> getSyncHistory(@PathVariable Long providerId,
                                                       @RequestParam String tenantId,
                                                       @RequestParam(defaultValue = "20") int limit) {
        try {
            List<UserSyncJob> jobs = userSyncService.getSyncHistory(providerId, tenantId, limit);
            return ApiResult.success(jobs);
        } catch (Exception e) {
            return ApiResult.failure(ErrorCode.INTERNAL_ERROR, "获取同步历史失败: " + e.getMessage());
        }
    }

    /**
     * 获取同步任务详情
     */
    @GetMapping("/sync-jobs/{jobId}/details")
    public ApiResult<List<UserSyncDetail>> getSyncJobDetails(@PathVariable Long jobId,
                                                             @RequestParam(defaultValue = "100") int limit) {
        try {
            List<UserSyncDetail> details = persistenceService.findSyncDetailsByJobId(jobId, limit);
            return ApiResult.success(details);
        } catch (Exception e) {
            return ApiResult.failure(ErrorCode.INTERNAL_ERROR, "获取同步任务详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取身份源列表
     */
    @GetMapping("/identity-providers")
    public ApiResult<List<IdentityProvider>> getIdentityProviders(@RequestParam String tenantId) {
        try {
            List<IdentityProvider> providers = persistenceService.findIdentityProvidersByTenant(Long.parseLong(tenantId));
            return ApiResult.success(providers);
        } catch (Exception e) {
            return ApiResult.failure(ErrorCode.INTERNAL_ERROR, "获取身份源列表失败: " + e.getMessage());
        }
    }

    /**
     * 创建身份源
     */
    @PostMapping("/identity-providers")
    public ApiResult<IdentityProvider> createIdentityProvider(@RequestBody IdentityProvider provider) {
        try {
            if (provider.getProviderCode() == null || provider.getProviderCode().trim().isEmpty()) {
                return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "身份源编码不能为空");
            }
            if (provider.getProviderName() == null || provider.getProviderName().trim().isEmpty()) {
                return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "身份源名称不能为空");
            }
            provider.setId(com.medkernel.common.Ids.next());
            persistenceService.saveIdentityProvider(provider);
            return ApiResult.success(provider);
        } catch (Exception e) {
            return ApiResult.failure(ErrorCode.INTERNAL_ERROR, "创建身份源失败: " + e.getMessage());
        }
    }

    /**
     * 获取身份源详情
     */
    @GetMapping("/identity-providers/{providerId}")
    public ApiResult<IdentityProvider> getIdentityProvider(@PathVariable Long providerId) {
        try {
            IdentityProvider provider = persistenceService.findIdentityProviderById(providerId);
            if (provider == null) {
                return ApiResult.failure(ErrorCode.NOT_FOUND, "身份源不存在");
            }
            return ApiResult.success(provider);
        } catch (Exception e) {
            return ApiResult.failure(ErrorCode.INTERNAL_ERROR, "获取身份源详情失败: " + e.getMessage());
        }
    }

    /**
     * 删除身份源
     */
    @DeleteMapping("/identity-providers/{providerId}")
    public ApiResult<Void> deleteIdentityProvider(@PathVariable Long providerId) {
        try {
            persistenceService.deleteIdentityProvider(providerId);
            return ApiResult.success(null);
        } catch (Exception e) {
            return ApiResult.failure(ErrorCode.INTERNAL_ERROR, "删除身份源失败: " + e.getMessage());
        }
    }
}