package com.medkernel.security;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户实体：平台独立用户，不依赖院内身份源。
 * 对应表 sec_user。
 */
public class SecurityUser {
    private Long id;
    private Long tenantId;
    private String username;
    private String passwordHash;
    private String displayName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String status;
    private String userType;
    private String employeeId;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private int loginAttempts;
    private LocalDateTime lockedUntil;
    private List<String> roles;
    private List<String> permissions;
    private List<OrgScope> orgScopes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }

    public int getLoginAttempts() {
        return loginAttempts;
    }

    public void setLoginAttempts(int loginAttempts) {
        this.loginAttempts = loginAttempts;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public List<OrgScope> getOrgScopes() {
        return orgScopes;
    }

    public void setOrgScopes(List<OrgScope> orgScopes) {
        this.orgScopes = orgScopes;
    }

    /**
     * 用户组织范围内部类
     */
    public static class OrgScope {
        private String scopeLevel;
        private String scopeCode;
        private String scopeName;

        public OrgScope() {
        }

        public OrgScope(String scopeLevel, String scopeCode, String scopeName) {
            this.scopeLevel = scopeLevel;
            this.scopeCode = scopeCode;
            this.scopeName = scopeName;
        }

        public String getScopeLevel() {
            return scopeLevel;
        }

        public void setScopeLevel(String scopeLevel) {
            this.scopeLevel = scopeLevel;
        }

        public String getScopeCode() {
            return scopeCode;
        }

        public void setScopeCode(String scopeCode) {
            this.scopeCode = scopeCode;
        }

        public String getScopeName() {
            return scopeName;
        }

        public void setScopeName(String scopeName) {
            this.scopeName = scopeName;
        }
    }
}
