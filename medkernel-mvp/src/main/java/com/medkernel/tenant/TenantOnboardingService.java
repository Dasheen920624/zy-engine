package com.medkernel.tenant;

import com.medkernel.common.ApiException;
import com.medkernel.security.SecurityTenant;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 客户租户开通服务
 * 提供外网客户租户开通、管理员邀请、服务账号管理等功能
 */
@Service
public class TenantOnboardingService {
    private final Map<String, Map<String, Object>> applicationStore = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> invitationStore = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> serviceAccountStore = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    /**
     * 提交租户申请
     */
    public Map<String, Object> submitApplication(Map<String, Object> request) {
        String applicationCode = "APP-" + System.currentTimeMillis();
        
        Map<String, Object> application = new LinkedHashMap<>();
        application.put("id", sequence.incrementAndGet());
        application.put("applicationCode", applicationCode);
        application.put("companyName", request.get("companyName"));
        application.put("companyType", request.get("companyType"));
        application.put("contactName", request.get("contactName"));
        application.put("contactPhone", request.get("contactPhone"));
        application.put("contactEmail", request.get("contactEmail"));
        application.put("contactTitle", request.get("contactTitle"));
        application.put("province", request.get("province"));
        application.put("city", request.get("city"));
        application.put("address", request.get("address"));
        application.put("licenseNumber", request.get("licenseNumber"));
        application.put("licenseType", request.getOrDefault("licenseType", "TRIAL"));
        application.put("expectedUsers", request.get("expectedUsers"));
        application.put("businessNeeds", request.get("businessNeeds"));
        application.put("status", "PENDING");
        application.put("createdTime", LocalDateTime.now());
        
        applicationStore.put(applicationCode, application);
        return application;
    }

    /**
     * 审批租户申请
     */
    public Map<String, Object> reviewApplication(String applicationCode, Map<String, Object> request) {
        Map<String, Object> application = applicationStore.get(applicationCode);
        if (application == null) {
            throw new ApiException("申请不存在: " + applicationCode);
        }

        String action = (String) request.get("action");  // APPROVE/REJECT
        String reviewedBy = (String) request.get("reviewedBy");
        String comment = (String) request.get("comment");

        if ("APPROVE".equals(action)) {
            // 生成租户ID
            String tenantId = "TENANT-" + System.currentTimeMillis();
            application.put("status", "APPROVED");
            application.put("tenantId", tenantId);
            
            // 创建租户
            createTenant(application);
            
            // 创建平台运营授权
            createPlatformAuthorization(tenantId, (String) application.get("licenseType"));
        } else {
            application.put("status", "REJECTED");
        }

        application.put("reviewedBy", reviewedBy);
        application.put("reviewedTime", LocalDateTime.now());
        application.put("reviewComment", comment);
        application.put("updatedTime", LocalDateTime.now());

        return application;
    }

    /**
     * 创建租户
     */
    private void createTenant(Map<String, Object> application) {
        // 实际项目中应该创建 SecurityTenant 记录
        // 这里模拟创建
        System.out.println("Creating tenant: " + application.get("tenantId"));
    }

    /**
     * 创建平台运营授权
     */
    private void createPlatformAuthorization(String tenantId, String licenseType) {
        // 根据授权类型创建不同的授权
        List<String> features = new ArrayList<>();
        features.add("BASIC_FEATURES");
        
        if ("STANDARD".equals(licenseType)) {
            features.add("ADVANCED_FEATURES");
            features.add("API_ACCESS");
        } else if ("ENTERPRISE".equals(licenseType)) {
            features.add("ADVANCED_FEATURES");
            features.add("API_ACCESS");
            features.add("CUSTOM_INTEGRATION");
            features.add("PRIORITY_SUPPORT");
        }

        // 实际项目中应该创建 SEC_PLATFORM_AUTHORIZATION 记录
        System.out.println("Creating platform authorization for tenant: " + tenantId);
    }

    /**
     * 发送管理员邀请
     */
    public Map<String, Object> sendInvitation(Map<String, Object> request) {
        String invitationCode = "INV-" + System.currentTimeMillis();
        
        Map<String, Object> invitation = new LinkedHashMap<>();
        invitation.put("id", sequence.incrementAndGet());
        invitation.put("invitationCode", invitationCode);
        invitation.put("tenantId", request.get("tenantId"));
        invitation.put("email", request.get("email"));
        invitation.put("phone", request.get("phone"));
        invitation.put("invitedBy", request.get("invitedBy"));
        invitation.put("roleCode", request.getOrDefault("roleCode", "TENANT_ADMIN"));
        invitation.put("status", "PENDING");
        
        // 设置过期时间（7天后）
        LocalDateTime expireTime = LocalDateTime.now().plusDays(7);
        invitation.put("expireTime", expireTime);
        invitation.put("createdTime", LocalDateTime.now());
        
        invitationStore.put(invitationCode, invitation);
        
        // 实际项目中应该发送邀请邮件
        sendInvitationEmail(invitation);
        
        return invitation;
    }

    /**
     * 发送邀请邮件
     */
    private void sendInvitationEmail(Map<String, Object> invitation) {
        // 实际项目中应该调用邮件服务
        System.out.println("Sending invitation email to: " + invitation.get("email"));
    }

    /**
     * 接受邀请
     */
    public Map<String, Object> acceptInvitation(String invitationCode, Map<String, Object> request) {
        Map<String, Object> invitation = invitationStore.get(invitationCode);
        if (invitation == null) {
            throw new ApiException("邀请不存在: " + invitationCode);
        }

        if (!"PENDING".equals(invitation.get("status"))) {
            throw new ApiException("邀请状态无效: " + invitation.get("status"));
        }

        LocalDateTime expireTime = (LocalDateTime) invitation.get("expireTime");
        if (LocalDateTime.now().isAfter(expireTime)) {
            invitation.put("status", "EXPIRED");
            throw new ApiException("邀请已过期");
        }

        String userId = (String) request.get("userId");
        invitation.put("status", "ACCEPTED");
        invitation.put("acceptedTime", LocalDateTime.now());
        invitation.put("userId", userId);
        invitation.put("updatedTime", LocalDateTime.now());

        // 实际项目中应该创建用户角色关联
        assignTenantAdminRole(userId, (String) invitation.get("tenantId"));

        return invitation;
    }

    /**
     * 分配租户管理员角色
     */
    private void assignTenantAdminRole(String userId, String tenantId) {
        // 实际项目中应该创建 SEC_USER_ROLE 记录
        System.out.println("Assigning TENANT_ADMIN role to user: " + userId + " for tenant: " + tenantId);
    }

    /**
     * 创建服务账号
     */
    public Map<String, Object> createServiceAccount(Map<String, Object> request) {
        String accountCode = "SA-" + System.currentTimeMillis();
        String clientId = "client-" + UUID.randomUUID().toString().substring(0, 8);
        
        // 生成客户端密钥（实际项目中应该使用安全的随机生成）
        String clientSecret = UUID.randomUUID().toString();
        String clientSecretHash = hashSecret(clientSecret);
        
        Map<String, Object> account = new LinkedHashMap<>();
        account.put("id", sequence.incrementAndGet());
        account.put("accountCode", accountCode);
        account.put("tenantId", request.get("tenantId"));
        account.put("accountName", request.get("accountName"));
        account.put("accountType", request.getOrDefault("accountType", "API_CLIENT"));
        account.put("clientId", clientId);
        account.put("clientSecretHash", clientSecretHash);
        account.put("scopes", request.get("scopes"));
        account.put("status", "ACTIVE");
        account.put("rateLimit", request.getOrDefault("rateLimit", 1000));
        account.put("createdBy", request.get("createdBy"));
        account.put("createdTime", LocalDateTime.now());
        
        serviceAccountStore.put(accountCode, account);
        
        // 返回时包含明文密钥（仅此一次）
        Map<String, Object> result = new LinkedHashMap<>(account);
        result.put("clientSecret", clientSecret);
        
        return result;
    }

    /**
     * 哈希密钥
     */
    private String hashSecret(String secret) {
        // 实际项目中应该使用 BCrypt 或其他安全的哈希算法
        return "hashed-" + secret;
    }

    /**
     * 查询服务账号列表
     */
    public List<Map<String, Object>> listServiceAccounts(String tenantId) {
        List<Map<String, Object>> accounts = new ArrayList<>();
        for (Map<String, Object> account : serviceAccountStore.values()) {
            if (tenantId.equals(account.get("tenantId"))) {
                accounts.add(account);
            }
        }
        return accounts;
    }

    /**
     * 吊销服务账号
     */
    public Map<String, Object> revokeServiceAccount(String accountCode) {
        Map<String, Object> account = serviceAccountStore.get(accountCode);
        if (account == null) {
            throw new ApiException("服务账号不存在: " + accountCode);
        }

        account.put("status", "REVOKED");
        account.put("updatedTime", LocalDateTime.now());
        
        return account;
    }

    /**
     * 查询租户申请列表
     */
    public List<Map<String, Object>> listApplications(Map<String, String> filters) {
        List<Map<String, Object>> applications = new ArrayList<>();
        String status = filters.get("status");
        
        for (Map<String, Object> app : applicationStore.values()) {
            if (status == null || status.equals(app.get("status"))) {
                applications.add(app);
            }
        }
        
        return applications;
    }

    /**
     * 查询邀请列表
     */
    public List<Map<String, Object>> listInvitations(String tenantId) {
        List<Map<String, Object>> invitations = new ArrayList<>();
        for (Map<String, Object> inv : invitationStore.values()) {
            if (tenantId.equals(inv.get("tenantId"))) {
                invitations.add(inv);
            }
        }
        return invitations;
    }
}