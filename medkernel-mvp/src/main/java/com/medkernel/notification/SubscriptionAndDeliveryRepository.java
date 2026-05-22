package com.medkernel.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medkernel.common.IdAllocatorRepository;
import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.PersistenceRepositorySupport;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class SubscriptionAndDeliveryRepository extends PersistenceRepositorySupport {

    public SubscriptionAndDeliveryRepository(EnginePersistenceProperties properties,
                                             ObjectMapper objectMapper,
                                             DataSource dataSource,
                                             IdAllocatorRepository idAllocatorRepository) {
        super(properties, objectMapper, dataSource, idAllocatorRepository);
    }

    // =========================================================================
    // NOTIFY_SUBSCRIPTION CRUD
    // =========================================================================

    /** 保存用户订阅设置（UPSERT），唯一键：tenant_id + user_id + notification_type + channel。 */
    public void saveSubscription(Map<String, Object> subscription) {
        if (!enabled()) {
            return;
        }
        String tenantId = truncate(string(subscription.get("tenant_id"), "default"), 64);
        String userId = truncate(string(subscription.get("user_id"), null), 64);
        String notificationType = truncate(string(subscription.get("notification_type"), ""), 32);
        String channel = truncate(string(subscription.get("channel"), "IN_APP"), 32);
        int enabledFlag = Boolean.TRUE.equals(subscription.get("enabled")) || "1".equals(string(subscription.get("enabled"), null)) ? 1 : 0;

        if (properties.localFileDatabase()) {
            saveSubscriptionLocal(tenantId, userId, notificationType, channel, enabledFlag);
        } else {
            saveSubscriptionOracle(tenantId, userId, notificationType, channel, enabledFlag);
        }
    }

    private void saveSubscriptionLocal(String tenantId, String userId, String notificationType,
                                       String channel, int enabledFlag) {
        String updateSql = "UPDATE NOTIFY_SUBSCRIPTION SET enabled=?, updated_time=CURRENT_TIMESTAMP " +
                "WHERE tenant_id=? AND user_id=? AND notification_type=? AND channel=?";
        String insertSql = "INSERT INTO NOTIFY_SUBSCRIPTION (id, tenant_id, user_id, notification_type, " +
                "channel, enabled, created_time, updated_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        try (Connection connection = connection()) {
            int affected;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setInt(i++, enabledFlag);
                ps.setString(i++, tenantId);
                ps.setString(i++, userId);
                ps.setString(i++, notificationType);
                ps.setString(i++, channel);
                affected = ps.executeUpdate();
            }
            if (affected == 0) {
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ps.setLong(i++, nextId(tenantId));
                    ps.setString(i++, tenantId);
                    ps.setString(i++, userId);
                    ps.setString(i++, notificationType);
                    ps.setString(i++, channel);
                    ps.setInt(i++, enabledFlag);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save subscription failed: " + ex.getMessage(), ex);
        }
    }

    private void saveSubscriptionOracle(String tenantId, String userId, String notificationType,
                                        String channel, int enabledFlag) {
        String updateSql = "UPDATE NOTIFY_SUBSCRIPTION SET enabled=?, updated_time=SYSTIMESTAMP " +
                "WHERE tenant_id=? AND user_id=? AND notification_type=? AND channel=?";
        String insertSql = "INSERT INTO NOTIFY_SUBSCRIPTION (id, tenant_id, user_id, notification_type, " +
                "channel, enabled, created_time, updated_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, SYSTIMESTAMP, SYSTIMESTAMP)";
        try (Connection connection = connection()) {
            int affected;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setInt(i++, enabledFlag);
                ps.setString(i++, tenantId);
                ps.setString(i++, userId);
                ps.setString(i++, notificationType);
                ps.setString(i++, channel);
                affected = ps.executeUpdate();
            }
            if (affected == 0) {
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ps.setLong(i++, nextId(tenantId));
                    ps.setString(i++, tenantId);
                    ps.setString(i++, userId);
                    ps.setString(i++, notificationType);
                    ps.setString(i++, channel);
                    ps.setInt(i++, enabledFlag);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save subscription failed: " + ex.getMessage(), ex);
        }
    }

    /** 查询用户订阅设置列表。 */
    public List<Map<String, Object>> listSubscriptions(String tenantId, String userId) {
        if (!enabled()) {
            return new ArrayList<Map<String, Object>>();
        }
        String sql = "SELECT id, tenant_id, user_id, notification_type, channel, enabled, " +
                "created_time, updated_time FROM NOTIFY_SUBSCRIPTION " +
                "WHERE tenant_id=? AND user_id=? ORDER BY notification_type, channel";
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(toSubscriptionMap(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list subscriptions failed: " + ex.getMessage(), ex);
        }
        return results;
    }

    /** 更新用户订阅设置（enabled 字段）。 */
    public void updateSubscription(String tenantId, String userId, String notificationType,
                                   String channel, boolean enabled) {
        if (!enabled()) {
            return;
        }
        String timestampExpr = properties.localFileDatabase() ? "CURRENT_TIMESTAMP" : "SYSTIMESTAMP";
        String sql = "UPDATE NOTIFY_SUBSCRIPTION SET enabled=?, updated_time=" + timestampExpr +
                " WHERE tenant_id=? AND user_id=? AND notification_type=? AND channel=?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, enabled ? 1 : 0);
            ps.setString(2, tenantId);
            ps.setString(3, userId);
            ps.setString(4, notificationType);
            ps.setString(5, channel);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update subscription failed: " + ex.getMessage(), ex);
        }
    }

    // =========================================================================
    // NOTIFY_DELIVERY_LOG CRUD
    // =========================================================================

    /** 保存投递日志（仅 INSERT）。 */
    public void saveDeliveryLog(Map<String, Object> log) {
        if (!enabled()) {
            return;
        }
        long notificationId = (long) doubleValue(log.get("notification_id"), 0);
        String channel = truncate(string(log.get("channel"), "IN_APP"), 32);
        String status = truncate(string(log.get("status"), "PENDING"), 32);
        String providerResponse = string(log.get("provider_response"), null);
        String errorMessage = truncate(string(log.get("error_message"), null), 1000);
        int retryCount = (int) doubleValue(log.get("retry_count"), 0);

        String tenantId = truncate(string(log.get("tenant_id"), "default"), 64);
        String timestampExpr = properties.localFileDatabase() ? "CURRENT_TIMESTAMP" : "SYSTIMESTAMP";
        String sql = "INSERT INTO NOTIFY_DELIVERY_LOG (id, notification_id, channel, status, " +
                "provider_response, error_message, retry_count, created_time, updated_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, " + timestampExpr + ", " + timestampExpr + ")";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, nextId(tenantId));
            ps.setLong(i++, notificationId);
            ps.setString(i++, channel);
            ps.setString(i++, status);
            ps.setString(i++, providerResponse);
            ps.setString(i++, errorMessage);
            ps.setInt(i++, retryCount);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("save delivery log failed: " + ex.getMessage(), ex);
        }
    }

    /** 查询投递日志列表。 */
    public List<Map<String, Object>> listDeliveryLogs(long notificationId) {
        if (!enabled()) {
            return new ArrayList<Map<String, Object>>();
        }
        String sql = "SELECT id, notification_id, channel, status, provider_response, " +
                "error_message, retry_count, created_time, updated_time " +
                "FROM NOTIFY_DELIVERY_LOG WHERE notification_id=? ORDER BY created_time DESC";
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, notificationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(toDeliveryLogMap(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list delivery logs failed: " + ex.getMessage(), ex);
        }
        return results;
    }

    // =========================================================================
    // ResultSet → Map 转换
    // =========================================================================

    private Map<String, Object> toSubscriptionMap(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("id", rs.getLong("id"));
        map.put("tenant_id", rs.getString("tenant_id"));
        map.put("user_id", rs.getString("user_id"));
        map.put("notification_type", rs.getString("notification_type"));
        map.put("channel", rs.getString("channel"));
        map.put("enabled", rs.getInt("enabled"));
        map.put("created_time", formatTimestamp(rs.getTimestamp("created_time")));
        map.put("updated_time", formatTimestamp(rs.getTimestamp("updated_time")));
        return map;
    }

    private Map<String, Object> toDeliveryLogMap(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("id", rs.getLong("id"));
        map.put("notification_id", rs.getLong("notification_id"));
        map.put("channel", rs.getString("channel"));
        map.put("status", rs.getString("status"));
        map.put("provider_response", rs.getString("provider_response"));
        map.put("error_message", rs.getString("error_message"));
        map.put("retry_count", rs.getInt("retry_count"));
        map.put("created_time", formatTimestamp(rs.getTimestamp("created_time")));
        map.put("updated_time", formatTimestamp(rs.getTimestamp("updated_time")));
        return map;
    }
}
