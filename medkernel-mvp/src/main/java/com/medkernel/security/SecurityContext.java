package com.medkernel.security;

/**
 * 安全上下文：ThreadLocal 持有当前请求的 platform_user_id 和 tenant_id。
 * 不变量 #2：每个请求必须通过 SecurityFilter 注入 platform_user_id。
 */
public final class SecurityContext {

    public static final String HEADER_USER_ID = "X-Platform-User-Id";
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<Long>();
    private static final ThreadLocal<Long> CURRENT_TENANT_ID = new ThreadLocal<Long>();
    private static final ThreadLocal<String> CURRENT_USERNAME = new ThreadLocal<String>();

    private SecurityContext() {
    }

    public static Long getUserId() {
        return CURRENT_USER_ID.get();
    }

    public static void setUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static Long getTenantId() {
        return CURRENT_TENANT_ID.get();
    }

    public static void setTenantId(Long tenantId) {
        CURRENT_TENANT_ID.set(tenantId);
    }

    public static String getUsername() {
        return CURRENT_USERNAME.get();
    }

    public static void setUsername(String username) {
        CURRENT_USERNAME.set(username);
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
        CURRENT_TENANT_ID.remove();
        CURRENT_USERNAME.remove();
    }
}
