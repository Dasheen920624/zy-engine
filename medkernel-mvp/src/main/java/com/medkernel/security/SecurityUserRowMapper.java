package com.medkernel.security;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
final class SecurityUserRowMapper {
    private SecurityUserRowMapper() {
    }
        static SecurityUser mapUser(ResultSet rs) throws SQLException {
            SecurityUser user = new SecurityUser();
            user.setId(rs.getLong("id"));
            user.setTenantId(rs.getLong("tenant_id"));
            user.setUsername(rs.getString("username"));
            user.setPasswordHash(rs.getString("password_hash"));
            user.setDisplayName(rs.getString("display_name"));
            user.setEmail(rs.getString("email"));
            user.setPhone(rs.getString("phone"));
            user.setAvatarUrl(rs.getString("avatar_url"));
            user.setStatus(rs.getString("status"));
            Timestamp lastLogin = rs.getTimestamp("last_login_time");
            if (lastLogin != null) {
                user.setLastLoginTime(lastLogin.toLocalDateTime());
            }
            user.setLastLoginIp(rs.getString("last_login_ip"));
            user.setLoginAttempts(rs.getInt("login_attempts"));
            Timestamp lockedUntil = rs.getTimestamp("locked_until");
            if (lockedUntil != null) {
                user.setLockedUntil(lockedUntil.toLocalDateTime());
            }
            user.setUserType(rs.getString("user_type"));
            user.setEmployeeId(rs.getString("employee_id"));
            return user;
        }
        static IdentityProvider mapIdentityProvider(ResultSet rs) throws SQLException {
            IdentityProvider provider = new IdentityProvider();
            provider.setId(rs.getLong("id"));
            provider.setTenantId(rs.getLong("tenant_id"));
            provider.setProviderCode(rs.getString("provider_code"));
            provider.setProviderName(rs.getString("provider_name"));
            provider.setProviderType(rs.getString("provider_type"));
            provider.setAdapterCode(rs.getString("adapter_code"));
            provider.setSyncMode(rs.getString("sync_mode"));
            provider.setSyncCron(rs.getString("sync_cron"));
            provider.setPriority(rs.getInt("priority"));
            provider.setStatus(rs.getString("status"));
            Timestamp lastSync = rs.getTimestamp("last_sync_time");
            if (lastSync != null) { provider.setLastSyncTime(lastSync.toLocalDateTime()); }
            provider.setLastSyncResult(rs.getString("last_sync_result"));
            provider.setLastSyncSummary(rs.getString("last_sync_summary"));
            provider.setCreatedBy(rs.getString("created_by"));
            Timestamp created = rs.getTimestamp("created_time");
            if (created != null) { provider.setCreatedTime(created.toLocalDateTime()); }
            provider.setUpdatedBy(rs.getString("updated_by"));
            Timestamp updated = rs.getTimestamp("updated_time");
            if (updated != null) { provider.setUpdatedTime(updated.toLocalDateTime()); }
            return provider;
        }
        static SsoIdentityBinding mapIdentityBinding(ResultSet rs) throws SQLException {
            SsoIdentityBinding binding = new SsoIdentityBinding();
            binding.setId(rs.getLong("id"));
            binding.setTenantId(rs.getLong("tenant_id"));
            binding.setUserId(rs.getLong("user_id"));
            binding.setProviderId(rs.getLong("provider_id"));
            binding.setExternalSubject(rs.getString("external_subject"));
            binding.setExternalOrgCode(rs.getString("external_org_code"));
            binding.setExternalDisplayName(rs.getString("external_display_name"));
            binding.setBindingStatus(rs.getString("binding_status"));
            Timestamp verified = rs.getTimestamp("last_verified_time");
            if (verified != null) { binding.setLastVerifiedTime(verified.toLocalDateTime()); }
            binding.setCreatedBy(rs.getString("created_by"));
            Timestamp created = rs.getTimestamp("created_time");
            if (created != null) { binding.setCreatedTime(created.toLocalDateTime()); }
            binding.setUpdatedBy(rs.getString("updated_by"));
            Timestamp updated = rs.getTimestamp("updated_time");
            if (updated != null) { binding.setUpdatedTime(updated.toLocalDateTime()); }
            return binding;
        }
}
