package com.medkernel.security;
import com.medkernel.persistence.EnginePersistenceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
@Repository
public class SecurityAuditRepository extends SecurityRepositorySupport {
    private static final Logger log = LoggerFactory.getLogger(SecurityAuditRepository.class);
    public SecurityAuditRepository(EnginePersistenceProperties properties, DataSource dataSource) {
        super(properties, dataSource);
    }
        public void writeAuditLog(Long userId, Long tenantId, String action, String ip, String detail) {
            String eventResult = (action != null && (action.endsWith("FAILED") || action.endsWith("LOCKED")))
                    ? "FAILURE" : "SUCCESS";
            String sql = "INSERT INTO sec_auth_audit_log (id, user_id, tenant_id, username, event_type, "
                    + "event_result, ip_address, failure_reason, created_time) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, nextId());
                ps.setLong(2, userId);
                ps.setLong(3, tenantId);
                ps.setString(4, null);
                ps.setString(5, action);
                ps.setString(6, eventResult);
                ps.setString(7, ip);
                ps.setString(8, detail);
                ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
                ps.executeUpdate();
            } catch (SQLException ex) {
                log.error("write audit log failed for user {} action {}", userId, action, ex);
            }
        }
}
