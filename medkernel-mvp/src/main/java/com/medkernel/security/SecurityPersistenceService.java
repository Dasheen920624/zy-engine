package com.medkernel.security;
import com.medkernel.persistence.EnginePersistenceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
/**
 * 安全模块持久化门面：保留既有公开 API，将 SEC 表访问委托给专门仓储。
 */
@Service
public class SecurityPersistenceService extends SecurityRepositorySupport {
    private static final Logger log = LoggerFactory.getLogger(SecurityPersistenceService.class);
    private final UserRepository userRepository;
    private final PlatformUserRepository platformUserRepository;
    private final RoleRepository roleRepository;
    private final SecurityAuditRepository securityAuditRepository;
    private final IdentityProviderRepository identityProviderRepository;
    @Autowired
    public SecurityPersistenceService(EnginePersistenceProperties properties,
                                      DataSource dataSource,
                                      UserRepository userRepository,
                                      PlatformUserRepository platformUserRepository,
                                      RoleRepository roleRepository,
                                      SecurityAuditRepository securityAuditRepository,
                                      IdentityProviderRepository identityProviderRepository) {
        super(properties, dataSource);
        this.userRepository = userRepository;
        this.platformUserRepository = platformUserRepository;
        this.roleRepository = roleRepository;
        this.securityAuditRepository = securityAuditRepository;
        this.identityProviderRepository = identityProviderRepository;
    }
    public SecurityPersistenceService(EnginePersistenceProperties properties, DataSource dataSource) {
        super(properties, dataSource);
        RoleRepository roles = new RoleRepository(properties, dataSource);
        this.roleRepository = roles;
        this.userRepository = new UserRepository(properties, dataSource, roles);
        this.platformUserRepository = new PlatformUserRepository(properties, dataSource, roles);
        this.securityAuditRepository = new SecurityAuditRepository(properties, dataSource);
        this.identityProviderRepository = new IdentityProviderRepository(properties, dataSource);
    }
    @PostConstruct
    public void initializeSecuritySchema() {
        if (!properties.isEnabled() || !properties.localFileDatabase()) {
            return;
        }
        List<String> statements = loadSchemaStatements("/db/local/sec_ddl.sql");
        if (statements.isEmpty()) {
            return;
        }
        try (Connection connection = connection();
             Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }
            log.info("SEC schema initialized successfully");
        } catch (SQLException ex) {
            log.error("initialize SEC schema failed", ex);
            throw new IllegalStateException("initialize SEC schema failed: " + ex.getMessage(), ex);
        }
    }
    public SecurityUser findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    public SecurityUser findById(Long userId) {
        return userRepository.findById(userId);
    }
    public void updateLoginStatus(Long userId, boolean success, String ip) {
        userRepository.updateLoginStatus(userId, success, ip);
    }
    public void lockUser(Long userId, int durationMinutes) {
        userRepository.lockUser(userId, durationMinutes);
    }
    public void writeAuditLog(Long userId, Long tenantId, String action, String ip, String detail) {
        securityAuditRepository.writeAuditLog(userId, tenantId, action, ip, detail);
    }
    public IdentityProvider findIdentityProviderByType(Long tenantId, String providerType) {
        return identityProviderRepository.findIdentityProviderByType(tenantId, providerType);
    }
    public List<IdentityProvider> findAllIdentityProviders(Long tenantId) {
        return identityProviderRepository.findAllIdentityProviders(tenantId);
    }
    public IdentityBinding findIdentityBinding(Long tenantId, Long providerId, String externalSubject) {
        return identityProviderRepository.findIdentityBinding(tenantId, providerId, externalSubject);
    }
    public List<IdentityBinding> findIdentityBindingsByUser(Long tenantId, Long userId) {
        return identityProviderRepository.findIdentityBindingsByUser(tenantId, userId);
    }
    public Long createUser(Long tenantId, String username, String displayName, String email, String phone,
                           String userType, String employeeId, String status) {
        return userRepository.createUser(tenantId, username, displayName, email, phone, userType, employeeId, status);
    }
    public void updateUserSync(Long userId, String displayName, String email, String phone,
                               String status, String employeeId) {
        userRepository.updateUserSync(userId, displayName, email, phone, status, employeeId);
    }
    public void createIdentityBinding(Long tenantId, Long userId, Long providerId,
                                      String externalSubject, String externalOrgCode,
                                      String externalDisplayName) {
        identityProviderRepository.createIdentityBinding(
                tenantId, userId, providerId, externalSubject, externalOrgCode, externalDisplayName);
    }
    public void saveSyncLog(Long tenantId, Long providerId, String syncType, String syncStatus,
                            int totalCount, int createdCount, int updatedCount, int disabledCount,
                            int conflictCount, int errorCount, String errorDetail) {
        identityProviderRepository.saveSyncLog(tenantId, providerId, syncType, syncStatus, totalCount,
                createdCount, updatedCount, disabledCount, conflictCount, errorCount, errorDetail);
    }
    public void updateProviderSyncStatus(Long providerId, String syncResult, String syncSummary) {
        identityProviderRepository.updateProviderSyncStatus(providerId, syncResult, syncSummary);
    }
    public SecurityUser findUserByEmployeeId(Long tenantId, String employeeId) {
        return userRepository.findUserByEmployeeId(tenantId, employeeId);
    }
    public void saveIdentityProvider(IdentityProvider provider) {
        identityProviderRepository.saveIdentityProvider(provider);
    }
    public List<IdentityProvider> findIdentityProvidersByTenant(Long tenantId) {
        return identityProviderRepository.findIdentityProvidersByTenant(tenantId);
    }
    public IdentityProvider findIdentityProviderById(Long providerId) {
        return identityProviderRepository.findIdentityProviderById(providerId);
    }
    public void deleteIdentityProvider(Long providerId) {
        identityProviderRepository.deleteIdentityProvider(providerId);
    }
    public IdentityBinding findBinding(Long tenantId, Long providerId, String externalSubject) {
        return identityProviderRepository.findBinding(tenantId, providerId, externalSubject);
    }
    public List<IdentityBinding> findBindingsByUserId(Long userId) {
        return identityProviderRepository.findBindingsByUserId(userId);
    }
    public void saveIdentityBinding(IdentityBinding binding) {
        identityProviderRepository.saveIdentityBinding(binding);
    }
    public void updateBindingSyncTime(Long bindingId) {
        identityProviderRepository.updateBindingSyncTime(bindingId);
    }
    public Long createUser(Long tenantId, String username, String displayName,
                           String email, String phone, String status, String createdBy) {
        return platformUserRepository.createUser(tenantId, username, displayName, email, phone, status, createdBy);
    }
    public void updateUserSnapshot(Long userId, String displayName, String email, String phone, String status) {
        platformUserRepository.updateUserSnapshot(userId, displayName, email, phone, status);
    }
    public void disableUser(Long userId) {
        platformUserRepository.disableUser(userId);
    }
    public SecurityUser findByTenantAndUsername(Long tenantId, String username) {
        return platformUserRepository.findByTenantAndUsername(tenantId, username);
    }
    public void clearUserOrgScopes(Long userId) {
        platformUserRepository.clearUserOrgScopes(userId);
    }
    public void insertUserOrgScopes(Long tenantId, Long userId, List<SecurityUser.OrgScope> scopes) {
        platformUserRepository.insertUserOrgScopes(tenantId, userId, scopes);
    }
    public List<SecurityUser> listUsers(Long tenantId, String keyword, String status, String role,
                                        int page, int size) {
        return platformUserRepository.listUsers(tenantId, keyword, status, role, page, size);
    }
    public long countUsers(Long tenantId, String keyword, String status, String role) {
        return platformUserRepository.countUsers(tenantId, keyword, status, role);
    }
    public void updateUserStatus(Long userId, String status, String operatorUsername) {
        platformUserRepository.updateUserStatus(userId, status, operatorUsername);
    }
    public void unlockUser(Long userId, String operatorUsername) {
        platformUserRepository.unlockUser(userId, operatorUsername);
    }
    public void replaceUserRoles(Long userId, Long tenantId, List<String> roleCodes, String operatorUsername) {
        roleRepository.replaceUserRoles(userId, tenantId, roleCodes, operatorUsername);
    }
    public void resetPassword(Long userId, String newPasswordHash, String operatorUsername) {
        platformUserRepository.resetPassword(userId, newPasswordHash, operatorUsername);
    }
    public List<Map<String, String>> listRoles(Long tenantId) {
        return roleRepository.listRoles(tenantId);
    }
    public boolean usernameExists(Long tenantId, String username) {
        return platformUserRepository.usernameExists(tenantId, username);
    }
    public Long createUserWithPassword(Long tenantId, String username, String displayName,
                                       String email, String phone, String userType,
                                       String employeeId, String passwordHash, String createdBy) {
        return platformUserRepository.createUserWithPassword(
                tenantId, username, displayName, email, phone, userType, employeeId, passwordHash, createdBy);
    }
    private List<String> loadSchemaStatements(String resourcePath) {
        InputStream input = SecurityPersistenceService.class.getResourceAsStream(resourcePath);
        if (input == null) {
            log.warn("SEC schema resource not found: {}", resourcePath);
            return new ArrayList<String>();
        }
        List<String> statements = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                current.append(line).append('\n');
                if (trimmed.endsWith(";")) {
                    statements.add(current.toString().replace(";", ""));
                    current.setLength(0);
                }
            }
            if (current.length() > 0) {
                statements.add(current.toString());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("load SEC schema resource failed: " + ex.getMessage(), ex);
        }
        return statements;
    }
}
