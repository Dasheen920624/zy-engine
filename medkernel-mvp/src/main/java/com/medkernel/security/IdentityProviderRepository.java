package com.medkernel.security;
import com.medkernel.persistence.EnginePersistenceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Repository
public class IdentityProviderRepository extends SecurityRepositorySupport {
    private static final Logger log = LoggerFactory.getLogger(IdentityProviderRepository.class);
    public IdentityProviderRepository(EnginePersistenceProperties properties, DataSource dataSource) {
        super(properties, dataSource);
    }
        public IdentityProvider findIdentityProviderByType(Long tenantId, String providerType) {
            String sql = "SELECT id, tenant_id, provider_code, provider_name, provider_type, adapter_code, "
                    + "sync_mode, sync_cron, priority, status, "
                    + "last_sync_time, last_sync_result, last_sync_summary, "
                    + "created_by, created_time, updated_by, updated_time "
                    + "FROM sec_identity_provider WHERE tenant_id = ? AND provider_type = ?";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, tenantId);
                ps.setString(2, providerType);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return SecurityUserRowMapper.mapIdentityProvider(rs);
                    }
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("find identity provider failed: " + ex.getMessage(), ex);
            }
            return null;
        }
        public List<IdentityProvider> findAllIdentityProviders(Long tenantId) {
            String sql = "SELECT id, tenant_id, provider_code, provider_name, provider_type, adapter_code, "
                    + "sync_mode, sync_cron, priority, status, "
                    + "last_sync_time, last_sync_result, last_sync_summary, "
                    + "created_by, created_time, updated_by, updated_time "
                    + "FROM sec_identity_provider WHERE tenant_id = ? ORDER BY priority";
            List<IdentityProvider> providers = new ArrayList<IdentityProvider>();
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, tenantId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        providers.add(SecurityUserRowMapper.mapIdentityProvider(rs));
                    }
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("find identity providers failed: " + ex.getMessage(), ex);
            }
            return providers;
        }
        public SsoIdentityBinding findIdentityBinding(Long tenantId, Long providerId, String externalSubject) {
            String sql = "SELECT id, tenant_id, user_id, provider_id, external_subject, external_org_code, "
                    + "external_display_name, binding_status, last_verified_time, "
                    + "created_by, created_time, updated_by, updated_time "
                    + "FROM sec_identity_binding WHERE tenant_id = ? AND provider_id = ? AND external_subject = ?";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, tenantId);
                ps.setLong(2, providerId);
                ps.setString(3, externalSubject);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return SecurityUserRowMapper.mapIdentityBinding(rs);
                    }
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("find identity binding failed: " + ex.getMessage(), ex);
            }
            return null;
        }
        public List<SsoIdentityBinding> findIdentityBindingsByUser(Long tenantId, Long userId) {
            String sql = "SELECT id, tenant_id, user_id, provider_id, external_subject, external_org_code, "
                    + "external_display_name, binding_status, last_verified_time, "
                    + "created_by, created_time, updated_by, updated_time "
                    + "FROM sec_identity_binding WHERE tenant_id = ? AND user_id = ?";
            List<SsoIdentityBinding> bindings = new ArrayList<SsoIdentityBinding>();
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, tenantId);
                ps.setLong(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        bindings.add(SecurityUserRowMapper.mapIdentityBinding(rs));
                    }
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("find identity bindings by user failed: " + ex.getMessage(), ex);
            }
            return bindings;
        }
        public void createIdentityBinding(Long tenantId, Long userId, Long providerId,
                                           String externalSubject, String externalOrgCode,
                                           String externalDisplayName) {
            Long bindingId = nextId();
            String sql = "INSERT INTO sec_identity_binding (id, tenant_id, user_id, provider_id, external_subject, "
                    + "external_org_code, external_display_name, binding_status, "
                    + "last_verified_time, created_time) VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?)";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, bindingId);
                ps.setLong(2, tenantId);
                ps.setLong(3, userId);
                ps.setLong(4, providerId);
                ps.setString(5, externalSubject);
                ps.setString(6, externalOrgCode);
                ps.setString(7, externalDisplayName);
                ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
                ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
                ps.executeUpdate();
            } catch (SQLException ex) {
                throw new IllegalStateException("create identity binding failed: " + ex.getMessage(), ex);
            }
        }
        public void saveSyncLog(Long tenantId, Long providerId, String syncType, String syncStatus,
                                int totalCount, int createdCount, int updatedCount, int disabledCount,
                                int conflictCount, int errorCount, String errorDetail) {
            Long logId = nextId();
            String sql = "INSERT INTO sec_user_sync_log (id, tenant_id, provider_id, sync_type, sync_status, "
                    + "total_count, created_count, updated_count, disabled_count, conflict_count, error_count, "
                    + "error_detail, started_time, created_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, logId);
                ps.setLong(2, tenantId);
                ps.setLong(3, providerId);
                ps.setString(4, syncType);
                ps.setString(5, syncStatus);
                ps.setInt(6, totalCount);
                ps.setInt(7, createdCount);
                ps.setInt(8, updatedCount);
                ps.setInt(9, disabledCount);
                ps.setInt(10, conflictCount);
                ps.setInt(11, errorCount);
                ps.setString(12, errorDetail);
                ps.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));
                ps.setTimestamp(14, Timestamp.valueOf(LocalDateTime.now()));
                ps.executeUpdate();
            } catch (SQLException ex) {
                log.error("save sync log failed", ex);
            }
        }
        public void updateProviderSyncStatus(Long providerId, String syncResult, String syncSummary) {
            String sql = "UPDATE sec_identity_provider SET last_sync_time = ?, last_sync_result = ?, "
                    + "last_sync_summary = ?, updated_time = ? WHERE id = ?";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                ps.setString(2, syncResult);
                ps.setString(3, syncSummary);
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                ps.setLong(5, providerId);
                ps.executeUpdate();
            } catch (SQLException ex) {
                log.error("update provider sync status failed", ex);
            }
        }
        public void saveIdentityProvider(IdentityProvider provider) {
            String updateSql = "UPDATE sec_identity_provider SET provider_name = ?, provider_type = ?, "
                    + "adapter_code = ?, sync_mode = ?, sync_cron = ?, priority = ?, status = ?, "
                    + "updated_by = ?, updated_time = ? WHERE tenant_id = ? AND provider_code = ?";
            String insertSql = "INSERT INTO sec_identity_provider (id, tenant_id, provider_code, provider_name, "
                    + "provider_type, adapter_code, sync_mode, sync_cron, priority, status, created_by, created_time) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection connection = connection()) {
                // Try UPDATE first
                try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                    ps.setString(1, provider.getProviderName());
                    ps.setString(2, provider.getProviderType());
                    ps.setString(3, provider.getAdapterCode());
                    ps.setString(4, provider.getSyncMode());
                    ps.setString(5, provider.getSyncCron());
                    ps.setInt(6, provider.getPriority());
                    ps.setString(7, provider.getStatus());
                    ps.setString(8, provider.getUpdatedBy());
                    ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
                    ps.setLong(10, provider.getTenantId());
                    ps.setString(11, provider.getProviderCode());
                    int updated = ps.executeUpdate();
                    if (updated > 0) {
                        return;
                    }
                }
                // If no rows updated, INSERT
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    ps.setLong(1, provider.getId() != null ? provider.getId() : nextId());
                    ps.setLong(2, provider.getTenantId());
                    ps.setString(3, provider.getProviderCode());
                    ps.setString(4, provider.getProviderName());
                    ps.setString(5, provider.getProviderType());
                    ps.setString(6, provider.getAdapterCode());
                    ps.setString(7, provider.getSyncMode());
                    ps.setString(8, provider.getSyncCron());
                    ps.setInt(9, provider.getPriority());
                    ps.setString(10, provider.getStatus());
                    ps.setString(11, provider.getCreatedBy());
                    ps.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
                    ps.executeUpdate();
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("save identity provider failed: " + ex.getMessage(), ex);
            }
        }
        public List<IdentityProvider> findIdentityProvidersByTenant(Long tenantId) {
            String sql = "SELECT id, tenant_id, provider_code, provider_name, provider_type, "
                    + "adapter_code, sync_mode, sync_cron, priority, status, "
                    + "last_sync_time, last_sync_result, last_sync_summary, "
                    + "created_by, created_time, updated_by, updated_time "
                    + "FROM sec_identity_provider WHERE tenant_id = ? ORDER BY priority";
            List<IdentityProvider> providers = new ArrayList<>();
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, tenantId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        providers.add(SecurityUserRowMapper.mapIdentityProvider(rs));
                    }
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("find identity providers by tenant failed: " + ex.getMessage(), ex);
            }
            return providers;
        }
        public IdentityProvider findIdentityProviderById(Long providerId) {
            String sql = "SELECT id, tenant_id, provider_code, provider_name, provider_type, "
                    + "adapter_code, sync_mode, sync_cron, priority, status, "
                    + "last_sync_time, last_sync_result, last_sync_summary, "
                    + "created_by, created_time, updated_by, updated_time "
                    + "FROM sec_identity_provider WHERE id = ?";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, providerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return SecurityUserRowMapper.mapIdentityProvider(rs);
                    }
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("find identity provider by id failed: " + ex.getMessage(), ex);
            }
            return null;
        }
        public void deleteIdentityProvider(Long providerId) {
            String sql = "DELETE FROM sec_identity_provider WHERE id = ?";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, providerId);
                ps.executeUpdate();
            } catch (SQLException ex) {
                throw new IllegalStateException("delete identity provider failed: " + ex.getMessage(), ex);
            }
        }
        public SsoIdentityBinding findBinding(Long tenantId, Long providerId, String externalSubject) {
            String sql = "SELECT id, tenant_id, user_id, provider_id, external_subject, external_org_code, "
                    + "external_display_name, binding_status, last_verified_time, "
                    + "created_by, created_time, updated_by, updated_time "
                    + "FROM sec_identity_binding WHERE tenant_id = ? AND provider_id = ? AND external_subject = ?";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, tenantId);
                ps.setLong(2, providerId);
                ps.setString(3, externalSubject);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return SecurityUserRowMapper.mapIdentityBinding(rs);
                    }
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("find binding failed: " + ex.getMessage(), ex);
            }
            return null;
        }
        public List<SsoIdentityBinding> findBindingsByUserId(Long userId) {
            String sql = "SELECT id, tenant_id, user_id, provider_id, external_subject, external_org_code, "
                    + "external_display_name, binding_status, last_verified_time, "
                    + "created_by, created_time, updated_by, updated_time "
                    + "FROM sec_identity_binding WHERE user_id = ?";
            List<SsoIdentityBinding> bindings = new ArrayList<>();
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        bindings.add(SecurityUserRowMapper.mapIdentityBinding(rs));
                    }
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("find bindings by user id failed: " + ex.getMessage(), ex);
            }
            return bindings;
        }
        public void saveIdentityBinding(SsoIdentityBinding binding) {
            String updateSql = "UPDATE sec_identity_binding SET user_id = ?, "
                    + "external_org_code = ?, external_display_name = ?, binding_status = ?, "
                    + "last_verified_time = ?, updated_by = ?, updated_time = ? "
                    + "WHERE tenant_id = ? AND provider_id = ? AND external_subject = ?";
            String insertSql = "INSERT INTO sec_identity_binding (id, tenant_id, user_id, provider_id, "
                    + "external_subject, external_org_code, external_display_name, "
                    + "binding_status, last_verified_time, created_by, created_time) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection connection = connection()) {
                // Try UPDATE first
                try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                    ps.setLong(1, binding.getUserId());
                    ps.setString(2, binding.getExternalOrgCode());
                    ps.setString(3, binding.getExternalDisplayName());
                    ps.setString(4, binding.getBindingStatus());
                    ps.setTimestamp(5, binding.getLastVerifiedTime() != null ? Timestamp.valueOf(binding.getLastVerifiedTime()) : null);
                    ps.setString(6, binding.getUpdatedBy());
                    ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
                    ps.setLong(8, binding.getTenantId());
                    ps.setLong(9, binding.getProviderId());
                    ps.setString(10, binding.getExternalSubject());
                    int updated = ps.executeUpdate();
                    if (updated > 0) {
                        return;
                    }
                }
                // If no rows updated, INSERT
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    ps.setLong(1, binding.getId() != null ? binding.getId() : nextId());
                    ps.setLong(2, binding.getTenantId());
                    ps.setLong(3, binding.getUserId());
                    ps.setLong(4, binding.getProviderId());
                    ps.setString(5, binding.getExternalSubject());
                    ps.setString(6, binding.getExternalOrgCode());
                    ps.setString(7, binding.getExternalDisplayName());
                    ps.setString(8, binding.getBindingStatus());
                    ps.setTimestamp(9, binding.getLastVerifiedTime() != null ? Timestamp.valueOf(binding.getLastVerifiedTime()) : null);
                    ps.setString(10, binding.getCreatedBy());
                    ps.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
                    ps.executeUpdate();
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("save identity binding failed: " + ex.getMessage(), ex);
            }
        }
        public void updateBindingSyncTime(Long bindingId) {
            String sql = "UPDATE sec_identity_binding SET last_verified_time = ? WHERE id = ?";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                ps.setLong(2, bindingId);
                ps.executeUpdate();
            } catch (SQLException ex) {
                log.error("update binding sync time failed for binding {}", bindingId, ex);
            }
        }
}
