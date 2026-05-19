package com.medkernel.security;

import com.medkernel.adapter.AdapterHubService;
import com.medkernel.common.Ids;
import com.medkernel.common.OrgDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserSyncService {
    private static final Logger log = LoggerFactory.getLogger(UserSyncService.class);

    private final SecurityPersistenceService persistenceService;
    private final AdapterHubService adapterHubService;

    public UserSyncService(SecurityPersistenceService persistenceService,
                           AdapterHubService adapterHubService) {
        this.persistenceService = persistenceService;
        this.adapterHubService = adapterHubService;
    }

    /**
     * 全量同步：拉取外部系统所有用户，创建/更新/禁用平台用户
     */
    public UserSyncJob fullSync(Long providerId, String tenantId, String triggeredBy) {
        IdentityProvider provider = persistenceService.findIdentityProviderById(providerId);
        if (provider == null) {
            throw new IllegalArgumentException("Identity provider not found: " + providerId);
        }
        return executeSync(provider, tenantId, "FULL", null, triggeredBy);
    }

    /**
     * 增量同步：仅拉取最近变更的用户（由适配器支持增量查询）
     */
    public UserSyncJob incrementalSync(Long providerId, String tenantId, String triggeredBy) {
        IdentityProvider provider = persistenceService.findIdentityProviderById(providerId);
        if (provider == null) {
            throw new IllegalArgumentException("Identity provider not found: " + providerId);
        }
        return executeSync(provider, tenantId, "INCREMENTAL", null, triggeredBy);
    }

    /**
     * 手动同步：同步指定的外部用户列表
     */
    public UserSyncJob manualSync(Long providerId, String tenantId, List<String> externalSubjects, String triggeredBy) {
        IdentityProvider provider = persistenceService.findIdentityProviderById(providerId);
        if (provider == null) {
            throw new IllegalArgumentException("Identity provider not found: " + providerId);
        }
        if (externalSubjects == null || externalSubjects.isEmpty()) {
            throw new IllegalArgumentException("External subjects list cannot be empty for manual sync");
        }
        return executeSync(provider, tenantId, "MANUAL", externalSubjects, triggeredBy);
    }

    /**
     * 获取同步任务状态
     */
    public UserSyncJob getSyncJobStatus(Long jobId) {
        return persistenceService.findSyncJobById(jobId);
    }

    /**
     * 获取同步历史
     */
    public List<UserSyncJob> getSyncHistory(Long providerId, String tenantId, int limit) {
        // 如果指定了providerId，过滤特定身份源的同步历史
        List<UserSyncJob> allJobs = persistenceService.findSyncJobsByTenant(Long.parseLong(tenantId), limit);
        if (providerId == null) {
            return allJobs;
        }
        List<UserSyncJob> filtered = new ArrayList<>();
        for (UserSyncJob job : allJobs) {
            if (job.getProviderId().equals(providerId)) {
                filtered.add(job);
            }
        }
        return filtered;
    }

    /**
     * 执行同步的核心逻辑
     */
    private UserSyncJob executeSync(IdentityProvider provider, String tenantId, String syncType,
                                    List<String> externalSubjects, String triggeredBy) {
        // 创建同步任务
        UserSyncJob job = new UserSyncJob();
        job.setId(Ids.next());
        job.setTenantId(Long.parseLong(tenantId));
        job.setProviderId(provider.getId());
        job.setSyncType(syncType);
        job.setStatus("RUNNING");
        job.setTriggeredBy(triggeredBy);
        job.setStartedAt(LocalDateTime.now());
        Long jobId = persistenceService.createSyncJob(job);

        try {
            // 调用适配器获取用户数据
            List<Map<String, Object>> userRecords = fetchUserRecords(provider, tenantId, externalSubjects);
            job.setTotalCount(userRecords.size());

            // 处理用户记录
            processUserRecords(provider, userRecords, job);

            // 更新任务状态
            job.setStatus("COMPLETED");
            job.setFinishedAt(LocalDateTime.now());
            persistenceService.updateSyncJob(job);

            log.info("User sync completed for provider {}: total={}, created={}, updated={}, disabled={}, skipped={}, errors={}",
                    provider.getProviderCode(), job.getTotalCount(), job.getCreatedCount(),
                    job.getUpdatedCount(), job.getDisabledCount(), job.getSkippedCount(), job.getErrorCount());

        } catch (Exception e) {
            job.setStatus("FAILED");
            job.setErrorMessage(e.getMessage());
            job.setFinishedAt(LocalDateTime.now());
            persistenceService.updateSyncJob(job);
            log.error("User sync failed for provider {}: {}", provider.getProviderCode(), e.getMessage(), e);
            throw new RuntimeException("User sync failed: " + e.getMessage(), e);
        }

        return job;
    }

    /**
     * 从外部系统获取用户记录
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchUserRecords(IdentityProvider provider, String tenantId,
                                                       List<String> externalSubjects) {
        Map<String, Object> request = new HashMap<>();
        request.put("adapter_code", provider.getAdapterCode());
        request.put("query_code", provider.getQueryCode());

        Map<String, Object> params = new HashMap<>();
        params.put("tenant_id", tenantId);
        if (externalSubjects != null && !externalSubjects.isEmpty()) {
            params.put("user_ids", externalSubjects);
        }
        request.put("params", params);

        // 使用默认医院代码，实际应从组织上下文获取
        String hospitalCode = OrgDefaults.DEFAULT_HOSPITAL_CODE;
        Map<String, Object> result = adapterHubService.query(request, tenantId, hospitalCode);

        if (!"SUCCESS".equals(result.get("status"))) {
            throw new RuntimeException("Adapter query failed: " + result.get("message"));
        }

        Object rows = result.get("rows");
        if (rows instanceof List) {
            return (List<Map<String, Object>>) rows;
        }
        return new ArrayList<>();
    }

    /**
     * 处理用户记录列表
     */
    private void processUserRecords(IdentityProvider provider, List<Map<String, Object>> records, UserSyncJob job) {
        List<UserSyncDetail> details = new ArrayList<>();

        for (Map<String, Object> record : records) {
            try {
                UserSyncDetail detail = processSingleRecord(provider, record, job);
                details.add(detail);
            } catch (Exception e) {
                // 记录错误但继续处理其他用户
                UserSyncDetail errorDetail = new UserSyncDetail();
                errorDetail.setId(Ids.next());
                errorDetail.setJobId(job.getId());
                errorDetail.setTenantId(job.getTenantId());
                errorDetail.setExternalSubject(getString(record, "user_id"));
                errorDetail.setExternalName(getString(record, "display_name"));
                errorDetail.setAction("ERROR");
                errorDetail.setMessage(e.getMessage());
                errorDetail.setCreatedTime(LocalDateTime.now());
                details.add(errorDetail);
                job.setErrorCount(job.getErrorCount() + 1);
                log.warn("Failed to process user record {}: {}", record.get("user_id"), e.getMessage());
            }
        }

        // 批量保存同步明细
        if (!details.isEmpty()) {
            persistenceService.insertSyncDetails(details);
        }
    }

    /**
     * 处理单个用户记录
     */
    private UserSyncDetail processSingleRecord(IdentityProvider provider, Map<String, Object> record, UserSyncJob job) {
        String externalSubject = getString(record, "user_id");
        String externalName = getString(record, "display_name");

        // 查找现有绑定
        IdentityBinding binding = persistenceService.findBinding(job.getTenantId(), provider.getId(), externalSubject);

        UserSyncDetail detail = new UserSyncDetail();
        detail.setId(Ids.next());
        detail.setJobId(job.getId());
        detail.setTenantId(job.getTenantId());
        detail.setExternalSubject(externalSubject);
        detail.setExternalName(externalName);
        detail.setCreatedTime(LocalDateTime.now());

        if (binding != null) {
            // 已存在绑定，更新用户信息
            SecurityUser user = persistenceService.findById(binding.getUserId());
            if (user != null) {
                updateUserFromRecord(user, record);
                persistenceService.updateUserSnapshot(user.getId(), user.getDisplayName(),
                        user.getEmail(), user.getPhone(), user.getStatus());
                binding.setLastSyncTime(LocalDateTime.now());
                persistenceService.saveIdentityBinding(binding);
                detail.setAction("UPDATED");
                detail.setPlatformUserId(user.getId());
                job.setUpdatedCount(job.getUpdatedCount() + 1);
            } else {
                // 绑定存在但用户不存在，创建新用户
                SecurityUser newUser = createUserFromRecord(record, job.getTenantId());
                binding.setUserId(newUser.getId());
                binding.setLastSyncTime(LocalDateTime.now());
                persistenceService.saveIdentityBinding(binding);
                detail.setAction("CREATED");
                detail.setPlatformUserId(newUser.getId());
                job.setCreatedCount(job.getCreatedCount() + 1);
            }
        } else {
            // 无绑定，检查是否存在同名用户
            String username = getString(record, "user_name");
            SecurityUser existingUser = persistenceService.findByTenantAndUsername(job.getTenantId(), username);

            if (existingUser != null) {
                // 存在同名用户，创建绑定
                createBinding(existingUser, provider, record, job.getTenantId());
                detail.setAction("BOUND");
                detail.setPlatformUserId(existingUser.getId());
                job.setSkippedCount(job.getSkippedCount() + 1);
            } else {
                // 创建新用户和绑定
                SecurityUser newUser = createUserFromRecord(record, job.getTenantId());
                createBinding(newUser, provider, record, job.getTenantId());
                detail.setAction("CREATED");
                detail.setPlatformUserId(newUser.getId());
                job.setCreatedCount(job.getCreatedCount() + 1);
            }
        }

        return detail;
    }

    /**
     * 从记录创建新用户，返回包含ID的SecurityUser
     */
    private SecurityUser createUserFromRecord(Map<String, Object> record, Long tenantId) {
        String username = getString(record, "user_name");
        String displayName = getString(record, "display_name");
        String email = getString(record, "email");
        String phone = getString(record, "phone");
        String status = mapUserStatus(getString(record, "status"));
        Long userId = persistenceService.createUser(tenantId, username, displayName, email, phone, status, "SYNC");
        SecurityUser user = new SecurityUser();
        user.setId(userId);
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setStatus(status);
        return user;
    }

    /**
     * 更新用户信息（仅更新内存对象，持久化由调用方完成）
     */
    private void updateUserFromRecord(SecurityUser user, Map<String, Object> record) {
        user.setDisplayName(getString(record, "display_name"));
        user.setPhone(getString(record, "phone"));
        user.setEmail(getString(record, "email"));
        user.setStatus(mapUserStatus(getString(record, "status")));
    }

    /**
     * 创建身份绑定
     */
    private void createBinding(SecurityUser user, IdentityProvider provider, Map<String, Object> record, Long tenantId) {
        IdentityBinding binding = new IdentityBinding();
        binding.setId(Ids.next());
        binding.setTenantId(tenantId);
        binding.setUserId(user.getId());
        binding.setProviderId(provider.getId());
        binding.setExternalSubject(getString(record, "user_id"));
        binding.setExternalName(getString(record, "display_name"));
        binding.setExternalOrgCode(getString(record, "department_code"));
        binding.setExternalOrgName(getString(record, "department_name"));
        binding.setExternalPosition(getString(record, "position"));
        binding.setStatus("ACTIVE");
        binding.setLastSyncTime(LocalDateTime.now());
        persistenceService.saveIdentityBinding(binding);
    }

    /**
     * 映射用户状态
     */
    private String mapUserStatus(String externalStatus) {
        if (externalStatus == null) {
            return "ACTIVE";
        }
        switch (externalStatus.toUpperCase()) {
            case "ACTIVE":
            case "ENABLED":
            case "1":
                return "ACTIVE";
            case "INACTIVE":
            case "DISABLED":
            case "0":
                return "INACTIVE";
            default:
                return "ACTIVE";
        }
    }

    /**
     * 安全获取字符串值
     */
    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : null;
    }
}