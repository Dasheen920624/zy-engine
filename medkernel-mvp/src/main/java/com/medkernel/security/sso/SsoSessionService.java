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
import java.util.UUID;

@Service
public class SsoSessionService {

    private static final Logger log = LoggerFactory.getLogger(SsoSessionService.class);

    private final DataSource dataSource;

    public SsoSessionService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<SsoSession> listUserSessions(Long tenantId, Long userId) {
        String sql = "SELECT id, tenant_id, user_id, config_id, external_subject, external_name, external_email, "
                + "session_token, access_token, refresh_token, id_token, token_type, issued_at, expires_at, "
                + "refresh_expires_at, status, ip_address, user_agent, last_access_time, created_time, updated_time "
                + "FROM sec_sso_session WHERE tenant_id = ? AND user_id = ? AND status = 'ACTIVE' ORDER BY created_time DESC";
        List<SsoSession> sessions = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapSsoSession(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list user sessions failed: " + ex.getMessage(), ex);
        }
        return sessions;
    }

    public SsoSession createSsoSession(Long tenantId, Long userId, Long configId, SsoUserInfo externalUser,
                                        String ipAddress, String userAgent) {
        SsoSession session = new SsoSession();
        session.setId(Ids.next());
        session.setTenantId(tenantId);
        session.setUserId(userId);
        session.setConfigId(configId);
        session.setExternalSubject(externalUser.getSubject());
        session.setExternalName(externalUser.getName());
        session.setExternalEmail(externalUser.getEmail());
        session.setSessionToken(UUID.randomUUID().toString());
        session.setAccessToken(externalUser.getAccessToken());
        session.setRefreshToken(externalUser.getRefreshToken());
        session.setIdToken(externalUser.getIdToken());
        session.setTokenType(externalUser.getTokenType());
        session.setIssuedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusMinutes(480));
        session.setStatus("ACTIVE");
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        session.setLastAccessTime(LocalDateTime.now());
        session.setCreatedTime(LocalDateTime.now());

        String sql = "INSERT INTO sec_sso_session (id, tenant_id, user_id, config_id, external_subject, external_name, external_email, "
                + "session_token, access_token, refresh_token, id_token, token_type, issued_at, expires_at, "
                + "status, ip_address, user_agent, last_access_time, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, session.getId());
            ps.setLong(2, session.getTenantId());
            ps.setLong(3, session.getUserId());
            ps.setLong(4, session.getConfigId());
            ps.setString(5, session.getExternalSubject());
            ps.setString(6, session.getExternalName());
            ps.setString(7, session.getExternalEmail());
            ps.setString(8, session.getSessionToken());
            ps.setString(9, session.getAccessToken());
            ps.setString(10, session.getRefreshToken());
            ps.setString(11, session.getIdToken());
            ps.setString(12, session.getTokenType());
            ps.setTimestamp(13, Timestamp.valueOf(session.getIssuedAt()));
            ps.setTimestamp(14, Timestamp.valueOf(session.getExpiresAt()));
            ps.setString(15, session.getStatus());
            ps.setString(16, session.getIpAddress());
            ps.setString(17, session.getUserAgent());
            ps.setTimestamp(18, Timestamp.valueOf(session.getLastAccessTime()));
            ps.setTimestamp(19, Timestamp.valueOf(session.getCreatedTime()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("create sso session failed: " + ex.getMessage(), ex);
        }
        return session;
    }

    public SsoSession findSessionByToken(Long tenantId, String sessionToken) {
        String sql = "SELECT id, tenant_id, user_id, config_id, external_subject, external_name, external_email, "
                + "session_token, access_token, refresh_token, id_token, token_type, issued_at, expires_at, "
                + "refresh_expires_at, status, ip_address, user_agent, last_access_time, created_time, updated_time "
                + "FROM sec_sso_session WHERE tenant_id = ? AND session_token = ? AND status = 'ACTIVE'";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setString(2, sessionToken);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSsoSession(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find session by token failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    public void invalidateSession(Long sessionId) {
        String sql = "UPDATE sec_sso_session SET status = 'REVOKED', updated_time = ? WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(2, sessionId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("invalidate session failed for session {}", sessionId, ex);
        }
    }

    SsoSession mapSsoSession(ResultSet rs) throws SQLException {
        SsoSession session = new SsoSession();
        session.setId(rs.getLong("id"));
        session.setTenantId(rs.getLong("tenant_id"));
        session.setUserId(rs.getLong("user_id"));
        session.setConfigId(rs.getLong("config_id"));
        session.setExternalSubject(rs.getString("external_subject"));
        session.setExternalName(rs.getString("external_name"));
        session.setExternalEmail(rs.getString("external_email"));
        session.setSessionToken(rs.getString("session_token"));
        session.setAccessToken(rs.getString("access_token"));
        session.setRefreshToken(rs.getString("refresh_token"));
        session.setIdToken(rs.getString("id_token"));
        session.setTokenType(rs.getString("token_type"));

        Timestamp issuedAt = rs.getTimestamp("issued_at");
        if (issuedAt != null) {
            session.setIssuedAt(issuedAt.toLocalDateTime());
        }
        Timestamp expiresAt = rs.getTimestamp("expires_at");
        if (expiresAt != null) {
            session.setExpiresAt(expiresAt.toLocalDateTime());
        }
        Timestamp refreshExpiresAt = rs.getTimestamp("refresh_expires_at");
        if (refreshExpiresAt != null) {
            session.setRefreshExpiresAt(refreshExpiresAt.toLocalDateTime());
        }

        session.setStatus(rs.getString("status"));
        session.setIpAddress(rs.getString("ip_address"));
        session.setUserAgent(rs.getString("user_agent"));

        Timestamp lastAccessTime = rs.getTimestamp("last_access_time");
        if (lastAccessTime != null) {
            session.setLastAccessTime(lastAccessTime.toLocalDateTime());
        }
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            session.setCreatedTime(createdTime.toLocalDateTime());
        }
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            session.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        return session;
    }
}
