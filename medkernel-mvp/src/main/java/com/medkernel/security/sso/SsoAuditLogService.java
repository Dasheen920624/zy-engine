package com.medkernel.security.sso;

import com.medkernel.persistence.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SsoAuditLogService {

    private static final Logger log = LoggerFactory.getLogger(SsoAuditLogService.class);

    private final DataSource dataSource;

    public SsoAuditLogService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<SsoAuditLog> listAuditLogs(Long tenantId, int limit) {
        String sql = "SELECT id, tenant_id, user_id, config_id, event_type, event_result, external_subject, "
                + "error_code, error_message, ip_address, user_agent, trace_id, created_time "
                + "FROM sec_sso_audit_log WHERE tenant_id = ? ORDER BY created_time DESC";
        List<SsoAuditLog> logs = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next() && logs.size() < limit) {
                    logs.add(mapSsoAuditLog(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list audit logs failed: " + ex.getMessage(), ex);
        }
        return logs;
    }

    public void writeAuditLog(Long tenantId, Long userId, Long configId, String eventType, String eventResult,
                               String externalSubject, String errorCode, String errorMessage,
                               String ipAddress, String userAgent) {
        String sql = "INSERT INTO sec_sso_audit_log (id, tenant_id, user_id, config_id, event_type, event_result, "
                + "external_subject, error_code, error_message, ip_address, user_agent, trace_id, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, Ids.next());
            ps.setLong(2, tenantId);
            if (userId != null) {
                ps.setLong(3, userId);
            } else {
                ps.setNull(3, java.sql.Types.BIGINT);
            }
            if (configId != null) {
                ps.setLong(4, configId);
            } else {
                ps.setNull(4, java.sql.Types.BIGINT);
            }
            ps.setString(5, eventType);
            ps.setString(6, eventResult);
            ps.setString(7, externalSubject);
            ps.setString(8, errorCode);
            ps.setString(9, errorMessage);
            ps.setString(10, ipAddress);
            ps.setString(11, userAgent);
            ps.setString(12, null);
            ps.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("write audit log failed", ex);
        }
    }

    SsoAuditLog mapSsoAuditLog(ResultSet rs) throws SQLException {
        SsoAuditLog log = new SsoAuditLog();
        log.setId(rs.getLong("id"));
        log.setTenantId(rs.getLong("tenant_id"));
        long userId = rs.getLong("user_id");
        if (!rs.wasNull()) {
            log.setUserId(userId);
        }
        long configId = rs.getLong("config_id");
        if (!rs.wasNull()) {
            log.setConfigId(configId);
        }
        log.setEventType(rs.getString("event_type"));
        log.setEventResult(rs.getString("event_result"));
        log.setExternalSubject(rs.getString("external_subject"));
        log.setErrorCode(rs.getString("error_code"));
        log.setErrorMessage(rs.getString("error_message"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setUserAgent(rs.getString("user_agent"));
        log.setTraceId(rs.getString("trace_id"));

        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            log.setCreatedTime(createdTime.toLocalDateTime());
        }
        return log;
    }
}
