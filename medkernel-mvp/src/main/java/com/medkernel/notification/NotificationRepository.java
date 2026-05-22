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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class NotificationRepository extends PersistenceRepositorySupport {

    public NotificationRepository(EnginePersistenceProperties properties,
                                  ObjectMapper objectMapper,
                                  DataSource dataSource,
                                  IdAllocatorRepository idAllocatorRepository) {
        super(properties, objectMapper, dataSource, idAllocatorRepository);
    }

    // =========================================================================
    // NOTIFY_NOTIFICATION CRUD
    // =========================================================================

    /** 保存通知（UPSERT），唯一键：tenant_id + notification_code。 */
    public void saveNotification(Map<String, Object> notification) {
        if (!enabled()) {
            return;
        }
        String tenantId = truncate(string(notification.get("tenant_id"), "default"), 64);
        String notificationCode = truncate(string(notification.get("notification_code"), null), 64);
        String orgCode = truncate(string(notification.get("org_code"), "ZYHOSPITAL"), 64);
        String title = truncate(string(notification.get("title"), ""), 200);
        String content = string(notification.get("content"), "");
        String notificationType = truncate(string(notification.get("notification_type"), "SYSTEM"), 32);
        String priority = truncate(string(notification.get("priority"), "NORMAL"), 32);
        String status = truncate(string(notification.get("status"), "UNREAD"), 32);
        String senderId = truncate(string(notification.get("sender_id"), null), 64);
        String senderName = truncate(string(notification.get("sender_name"), null), 100);
        String recipientId = truncate(string(notification.get("recipient_id"), null), 64);
        String recipientName = truncate(string(notification.get("recipient_name"), null), 100);
        String businessType = truncate(string(notification.get("business_type"), null), 64);
        String businessId = truncate(string(notification.get("business_id"), null), 64);
        String businessUrl = truncate(string(notification.get("business_url"), null), 500);
        String channel = truncate(string(notification.get("channel"), "IN_APP"), 32);
        Timestamp scheduledTime = parseTimestamp(string(notification.get("scheduled_time"), null));
        Timestamp sentTime = parseTimestamp(string(notification.get("sent_time"), null));
        Timestamp readTime = parseTimestamp(string(notification.get("read_time"), null));
        Timestamp expireTime = parseTimestamp(string(notification.get("expire_time"), null));
        int retryCount = (int) doubleValue(notification.get("retry_count"), 0);
        int maxRetries = (int) doubleValue(notification.get("max_retries"), 3);
        String errorMessage = truncate(string(notification.get("error_message"), null), 1000);
        String createdBy = truncate(string(notification.get("created_by"), null), 64);

        if (properties.localFileDatabase()) {
            saveNotificationLocal(tenantId, notificationCode, orgCode, title, content, notificationType,
                    priority, status, senderId, senderName, recipientId, recipientName, businessType,
                    businessId, businessUrl, channel, scheduledTime, sentTime, readTime, expireTime,
                    retryCount, maxRetries, errorMessage, createdBy);
        } else {
            saveNotificationOracle(tenantId, notificationCode, orgCode, title, content, notificationType,
                    priority, status, senderId, senderName, recipientId, recipientName, businessType,
                    businessId, businessUrl, channel, scheduledTime, sentTime, readTime, expireTime,
                    retryCount, maxRetries, errorMessage, createdBy);
        }
    }

    private void saveNotificationLocal(String tenantId, String notificationCode, String orgCode,
                                       String title, String content, String notificationType,
                                       String priority, String status, String senderId, String senderName,
                                       String recipientId, String recipientName, String businessType,
                                       String businessId, String businessUrl, String channel,
                                       Timestamp scheduledTime, Timestamp sentTime, Timestamp readTime,
                                       Timestamp expireTime, int retryCount, int maxRetries,
                                       String errorMessage, String createdBy) {
        String updateSql = "UPDATE NOTIFY_NOTIFICATION SET org_code=?, title=?, content=?, notification_type=?, " +
                "priority=?, status=?, sender_id=?, sender_name=?, recipient_id=?, recipient_name=?, " +
                "business_type=?, business_id=?, business_url=?, channel=?, scheduled_time=?, sent_time=?, " +
                "read_time=?, expire_time=?, retry_count=?, max_retries=?, error_message=?, " +
                "created_by=COALESCE(?, created_by), updated_time=CURRENT_TIMESTAMP " +
                "WHERE tenant_id=? AND notification_code=?";
        String insertSql = "INSERT INTO NOTIFY_NOTIFICATION (id, tenant_id, org_code, notification_code, title, " +
                "content, notification_type, priority, status, sender_id, sender_name, recipient_id, recipient_name, " +
                "business_type, business_id, business_url, channel, scheduled_time, sent_time, read_time, expire_time, " +
                "retry_count, max_retries, error_message, created_by, created_time, updated_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        try (Connection connection = connection()) {
            int affected;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, orgCode);
                ps.setString(i++, title);
                ps.setString(i++, content);
                ps.setString(i++, notificationType);
                ps.setString(i++, priority);
                ps.setString(i++, status);
                ps.setString(i++, senderId);
                ps.setString(i++, senderName);
                ps.setString(i++, recipientId);
                ps.setString(i++, recipientName);
                ps.setString(i++, businessType);
                ps.setString(i++, businessId);
                ps.setString(i++, businessUrl);
                ps.setString(i++, channel);
                ps.setTimestamp(i++, scheduledTime);
                ps.setTimestamp(i++, sentTime);
                ps.setTimestamp(i++, readTime);
                ps.setTimestamp(i++, expireTime);
                ps.setInt(i++, retryCount);
                ps.setInt(i++, maxRetries);
                ps.setString(i++, errorMessage);
                ps.setString(i++, createdBy);
                ps.setString(i++, tenantId);
                ps.setString(i++, notificationCode);
                affected = ps.executeUpdate();
            }
            if (affected == 0) {
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ps.setLong(i++, nextId(tenantId));
                    ps.setString(i++, tenantId);
                    ps.setString(i++, orgCode);
                    ps.setString(i++, notificationCode);
                    ps.setString(i++, title);
                    ps.setString(i++, content);
                    ps.setString(i++, notificationType);
                    ps.setString(i++, priority);
                    ps.setString(i++, status);
                    ps.setString(i++, senderId);
                    ps.setString(i++, senderName);
                    ps.setString(i++, recipientId);
                    ps.setString(i++, recipientName);
                    ps.setString(i++, businessType);
                    ps.setString(i++, businessId);
                    ps.setString(i++, businessUrl);
                    ps.setString(i++, channel);
                    ps.setTimestamp(i++, scheduledTime);
                    ps.setTimestamp(i++, sentTime);
                    ps.setTimestamp(i++, readTime);
                    ps.setTimestamp(i++, expireTime);
                    ps.setInt(i++, retryCount);
                    ps.setInt(i++, maxRetries);
                    ps.setString(i++, errorMessage);
                    ps.setString(i++, createdBy);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save notification failed: " + ex.getMessage(), ex);
        }
    }

    private void saveNotificationOracle(String tenantId, String notificationCode, String orgCode,
                                        String title, String content, String notificationType,
                                        String priority, String status, String senderId, String senderName,
                                        String recipientId, String recipientName, String businessType,
                                        String businessId, String businessUrl, String channel,
                                        Timestamp scheduledTime, Timestamp sentTime, Timestamp readTime,
                                        Timestamp expireTime, int retryCount, int maxRetries,
                                        String errorMessage, String createdBy) {
        String updateSql = "UPDATE NOTIFY_NOTIFICATION SET org_code=?, title=?, content=?, notification_type=?, " +
                "priority=?, status=?, sender_id=?, sender_name=?, recipient_id=?, recipient_name=?, " +
                "business_type=?, business_id=?, business_url=?, channel=?, scheduled_time=?, sent_time=?, " +
                "read_time=?, expire_time=?, retry_count=?, max_retries=?, error_message=?, " +
                "created_by=COALESCE(?, created_by), updated_time=SYSTIMESTAMP " +
                "WHERE tenant_id=? AND notification_code=?";
        String insertSql = "INSERT INTO NOTIFY_NOTIFICATION (id, tenant_id, org_code, notification_code, title, " +
                "content, notification_type, priority, status, sender_id, sender_name, recipient_id, recipient_name, " +
                "business_type, business_id, business_url, channel, scheduled_time, sent_time, read_time, expire_time, " +
                "retry_count, max_retries, error_message, created_by, created_time, updated_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, SYSTIMESTAMP)";
        try (Connection connection = connection()) {
            int affected;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, orgCode);
                ps.setString(i++, title);
                ps.setString(i++, content);
                ps.setString(i++, notificationType);
                ps.setString(i++, priority);
                ps.setString(i++, status);
                ps.setString(i++, senderId);
                ps.setString(i++, senderName);
                ps.setString(i++, recipientId);
                ps.setString(i++, recipientName);
                ps.setString(i++, businessType);
                ps.setString(i++, businessId);
                ps.setString(i++, businessUrl);
                ps.setString(i++, channel);
                ps.setTimestamp(i++, scheduledTime);
                ps.setTimestamp(i++, sentTime);
                ps.setTimestamp(i++, readTime);
                ps.setTimestamp(i++, expireTime);
                ps.setInt(i++, retryCount);
                ps.setInt(i++, maxRetries);
                ps.setString(i++, errorMessage);
                ps.setString(i++, createdBy);
                ps.setString(i++, tenantId);
                ps.setString(i++, notificationCode);
                affected = ps.executeUpdate();
            }
            if (affected == 0) {
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ps.setLong(i++, nextId(tenantId));
                    ps.setString(i++, tenantId);
                    ps.setString(i++, orgCode);
                    ps.setString(i++, notificationCode);
                    ps.setString(i++, title);
                    ps.setString(i++, content);
                    ps.setString(i++, notificationType);
                    ps.setString(i++, priority);
                    ps.setString(i++, status);
                    ps.setString(i++, senderId);
                    ps.setString(i++, senderName);
                    ps.setString(i++, recipientId);
                    ps.setString(i++, recipientName);
                    ps.setString(i++, businessType);
                    ps.setString(i++, businessId);
                    ps.setString(i++, businessUrl);
                    ps.setString(i++, channel);
                    ps.setTimestamp(i++, scheduledTime);
                    ps.setTimestamp(i++, sentTime);
                    ps.setTimestamp(i++, readTime);
                    ps.setTimestamp(i++, expireTime);
                    ps.setInt(i++, retryCount);
                    ps.setInt(i++, maxRetries);
                    ps.setString(i++, errorMessage);
                    ps.setString(i++, createdBy);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save notification failed: " + ex.getMessage(), ex);
        }
    }

    /** 查询通知列表，支持 tenantId/recipientId/status/notificationType/priority 过滤，按 created_time DESC 排序。 */
    public List<Map<String, Object>> listNotifications(Map<String, String> filters) {
        if (!enabled()) {
            return new ArrayList<Map<String, Object>>();
        }
        String tenantId = filterValue(filters, "tenantId");
        String recipientId = filterValue(filters, "recipientId");
        String status = filterValue(filters, "status");
        String notificationType = filterValue(filters, "notificationType");
        String priority = filterValue(filters, "priority");
        int limit = filterInt(filters, "limit", 100);
        if (limit <= 0) {
            limit = 100;
        }

        StringBuilder sql = new StringBuilder("SELECT id, tenant_id, org_code, notification_code, title, content, " +
                "notification_type, priority, status, sender_id, sender_name, recipient_id, recipient_name, " +
                "business_type, business_id, business_url, channel, scheduled_time, sent_time, read_time, " +
                "expire_time, retry_count, max_retries, error_message, created_by, created_time, updated_time " +
                "FROM NOTIFY_NOTIFICATION WHERE 1=1");
        List<Object> params = new ArrayList<Object>();
        if (tenantId != null) {
            sql.append(" AND tenant_id=?");
            params.add(tenantId);
        }
        if (recipientId != null) {
            sql.append(" AND recipient_id=?");
            params.add(recipientId);
        }
        if (status != null) {
            sql.append(" AND status=?");
            params.add(status);
        }
        if (notificationType != null) {
            sql.append(" AND notification_type=?");
            params.add(notificationType);
        }
        if (priority != null) {
            sql.append(" AND priority=?");
            params.add(priority);
        }
        sql.append(" ORDER BY created_time DESC FETCH FIRST ").append(limit).append(" ROWS ONLY");

        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(toNotificationMap(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list notifications failed: " + ex.getMessage(), ex);
        }
        return results;
    }

    /** 获取单条通知，找不到返回 null。 */
    public Map<String, Object> getNotification(String tenantId, String notificationCode) {
        if (!enabled()) {
            return null;
        }
        String sql = "SELECT id, tenant_id, org_code, notification_code, title, content, " +
                "notification_type, priority, status, sender_id, sender_name, recipient_id, recipient_name, " +
                "business_type, business_id, business_url, channel, scheduled_time, sent_time, read_time, " +
                "expire_time, retry_count, max_retries, error_message, created_by, created_time, updated_time " +
                "FROM NOTIFY_NOTIFICATION WHERE tenant_id=? AND notification_code=?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setString(2, notificationCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? toNotificationMap(rs) : null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get notification failed: " + ex.getMessage(), ex);
        }
    }

    /** 更新通知状态，支持额外字段（如 read_time, sent_time 等），更新 updated_time。 */
    public void updateNotificationStatus(String tenantId, String notificationCode, String status,
                                         Map<String, Object> extraFields) {
        if (!enabled()) {
            return;
        }
        String timestampExpr = properties.localFileDatabase() ? "CURRENT_TIMESTAMP" : "SYSTIMESTAMP";
        StringBuilder sql = new StringBuilder("UPDATE NOTIFY_NOTIFICATION SET status=?, updated_time=")
                .append(timestampExpr);
        List<Object> params = new ArrayList<Object>();
        params.add(truncate(status, 32));

        if (extraFields != null) {
            if (extraFields.containsKey("read_time")) {
                sql.append(", read_time=?");
                params.add(parseTimestamp(string(extraFields.get("read_time"), null)));
            }
            if (extraFields.containsKey("sent_time")) {
                sql.append(", sent_time=?");
                params.add(parseTimestamp(string(extraFields.get("sent_time"), null)));
            }
            if (extraFields.containsKey("expire_time")) {
                sql.append(", expire_time=?");
                params.add(parseTimestamp(string(extraFields.get("expire_time"), null)));
            }
            if (extraFields.containsKey("error_message")) {
                sql.append(", error_message=?");
                params.add(truncate(string(extraFields.get("error_message"), null), 1000));
            }
            if (extraFields.containsKey("retry_count")) {
                sql.append(", retry_count=?");
                params.add((int) doubleValue(extraFields.get("retry_count"), 0));
            }
        }

        sql.append(" WHERE tenant_id=? AND notification_code=?");
        params.add(tenantId);
        params.add(notificationCode);

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Timestamp) {
                    ps.setTimestamp(i + 1, (Timestamp) param);
                } else if (param instanceof Integer) {
                    ps.setInt(i + 1, (Integer) param);
                } else {
                    ps.setString(i + 1, (String) param);
                }
            }
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update notification status failed: " + ex.getMessage(), ex);
        }
    }

    /** 获取未读通知数量。 */
    public long getUnreadCount(String tenantId, String recipientId) {
        if (!enabled()) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM NOTIFY_NOTIFICATION WHERE tenant_id=? AND recipient_id=? AND status='UNREAD'";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setString(2, recipientId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get unread count failed: " + ex.getMessage(), ex);
        }
    }

    /** 获取通知统计：total/unread/read/archived。 */
    public Map<String, Object> getNotificationSummary(String tenantId, String recipientId) {
        if (!enabled()) {
            Map<String, Object> empty = new LinkedHashMap<String, Object>();
            empty.put("total", 0);
            empty.put("unread", 0);
            empty.put("read", 0);
            empty.put("archived", 0);
            return empty;
        }
        String sql = "SELECT COUNT(*) AS total, " +
                "SUM(CASE WHEN status='UNREAD' THEN 1 ELSE 0 END) AS unread, " +
                "SUM(CASE WHEN status='READ' THEN 1 ELSE 0 END) AS read_count, " +
                "SUM(CASE WHEN status='ARCHIVED' THEN 1 ELSE 0 END) AS archived " +
                "FROM NOTIFY_NOTIFICATION WHERE tenant_id=? AND recipient_id=?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setString(2, recipientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> summary = new LinkedHashMap<String, Object>();
                    summary.put("total", rs.getLong("total"));
                    summary.put("unread", rs.getLong("unread"));
                    summary.put("read", rs.getLong("read_count"));
                    summary.put("archived", rs.getLong("archived"));
                    return summary;
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get notification summary failed: " + ex.getMessage(), ex);
        }
        Map<String, Object> empty = new LinkedHashMap<String, Object>();
        empty.put("total", 0);
        empty.put("unread", 0);
        empty.put("read", 0);
        empty.put("archived", 0);
        return empty;
    }

    /** 删除过期通知，返回删除数量。 */
    public int cleanupExpiredNotifications() {
        if (!enabled()) {
            return 0;
        }
        String timestampExpr = properties.localFileDatabase() ? "CURRENT_TIMESTAMP" : "SYSTIMESTAMP";
        String sql = "DELETE FROM NOTIFY_NOTIFICATION WHERE expire_time IS NOT NULL AND expire_time < " + timestampExpr;
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            return ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("cleanup expired notifications failed: " + ex.getMessage(), ex);
        }
    }

    // =========================================================================
    // NOTIFY_CHANNEL_CONFIG CRUD
    // =========================================================================

    /** 保存渠道配置（UPSERT），唯一键：tenant_id + channel_code。 */
    public void saveChannelConfig(Map<String, Object> config) {
        if (!enabled()) {
            return;
        }
        String tenantId = truncate(string(config.get("tenant_id"), "default"), 64);
        String channelCode = truncate(string(config.get("channel_code"), null), 64);
        String channelName = truncate(string(config.get("channel_name"), ""), 100);
        String channelType = truncate(string(config.get("channel_type"), ""), 32);
        int enabledFlag = Boolean.TRUE.equals(config.get("enabled")) || "1".equals(string(config.get("enabled"), null)) ? 1 : 0;
        String configJson = string(config.get("config_json"), "{}");
        String createdBy = truncate(string(config.get("created_by"), null), 64);

        if (properties.localFileDatabase()) {
            saveChannelConfigLocal(tenantId, channelCode, channelName, channelType, enabledFlag, configJson, createdBy);
        } else {
            saveChannelConfigOracle(tenantId, channelCode, channelName, channelType, enabledFlag, configJson, createdBy);
        }
    }

    private void saveChannelConfigLocal(String tenantId, String channelCode, String channelName,
                                        String channelType, int enabledFlag, String configJson, String createdBy) {
        String updateSql = "UPDATE NOTIFY_CHANNEL_CONFIG SET channel_name=?, channel_type=?, enabled=?, " +
                "config_json=?, created_by=COALESCE(?, created_by), updated_time=CURRENT_TIMESTAMP " +
                "WHERE tenant_id=? AND channel_code=?";
        String insertSql = "INSERT INTO NOTIFY_CHANNEL_CONFIG (id, tenant_id, channel_code, channel_name, " +
                "channel_type, enabled, config_json, created_by, created_time, updated_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        try (Connection connection = connection()) {
            int affected;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, channelName);
                ps.setString(i++, channelType);
                ps.setInt(i++, enabledFlag);
                ps.setString(i++, configJson);
                ps.setString(i++, createdBy);
                ps.setString(i++, tenantId);
                ps.setString(i++, channelCode);
                affected = ps.executeUpdate();
            }
            if (affected == 0) {
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ps.setLong(i++, nextId(tenantId));
                    ps.setString(i++, tenantId);
                    ps.setString(i++, channelCode);
                    ps.setString(i++, channelName);
                    ps.setString(i++, channelType);
                    ps.setInt(i++, enabledFlag);
                    ps.setString(i++, configJson);
                    ps.setString(i++, createdBy);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save channel config failed: " + ex.getMessage(), ex);
        }
    }

    private void saveChannelConfigOracle(String tenantId, String channelCode, String channelName,
                                         String channelType, int enabledFlag, String configJson, String createdBy) {
        String updateSql = "UPDATE NOTIFY_CHANNEL_CONFIG SET channel_name=?, channel_type=?, enabled=?, " +
                "config_json=?, created_by=COALESCE(?, created_by), updated_time=SYSTIMESTAMP " +
                "WHERE tenant_id=? AND channel_code=?";
        String insertSql = "INSERT INTO NOTIFY_CHANNEL_CONFIG (id, tenant_id, channel_code, channel_name, " +
                "channel_type, enabled, config_json, created_by, created_time, updated_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, SYSTIMESTAMP)";
        try (Connection connection = connection()) {
            int affected;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, channelName);
                ps.setString(i++, channelType);
                ps.setInt(i++, enabledFlag);
                ps.setString(i++, configJson);
                ps.setString(i++, createdBy);
                ps.setString(i++, tenantId);
                ps.setString(i++, channelCode);
                affected = ps.executeUpdate();
            }
            if (affected == 0) {
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ps.setLong(i++, nextId(tenantId));
                    ps.setString(i++, tenantId);
                    ps.setString(i++, channelCode);
                    ps.setString(i++, channelName);
                    ps.setString(i++, channelType);
                    ps.setInt(i++, enabledFlag);
                    ps.setString(i++, configJson);
                    ps.setString(i++, createdBy);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save channel config failed: " + ex.getMessage(), ex);
        }
    }

    /** 查询渠道配置列表。 */
    public List<Map<String, Object>> listChannelConfigs(String tenantId) {
        if (!enabled()) {
            return new ArrayList<Map<String, Object>>();
        }
        String sql = "SELECT id, tenant_id, channel_code, channel_name, channel_type, enabled, " +
                "config_json, created_by, created_time, updated_time " +
                "FROM NOTIFY_CHANNEL_CONFIG WHERE tenant_id=? ORDER BY channel_code";
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(toChannelConfigMap(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list channel configs failed: " + ex.getMessage(), ex);
        }
        return results;
    }

    /** 获取单条渠道配置。 */
    public Map<String, Object> getChannelConfig(String tenantId, String channelCode) {
        if (!enabled()) {
            return null;
        }
        String sql = "SELECT id, tenant_id, channel_code, channel_name, channel_type, enabled, " +
                "config_json, created_by, created_time, updated_time " +
                "FROM NOTIFY_CHANNEL_CONFIG WHERE tenant_id=? AND channel_code=?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setString(2, channelCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? toChannelConfigMap(rs) : null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get channel config failed: " + ex.getMessage(), ex);
        }
    }

    // =========================================================================
    // NOTIFY_TEMPLATE CRUD
    // =========================================================================

    /** 保存通知模板（UPSERT），唯一键：tenant_id + template_code + channel。 */
    public void saveTemplate(Map<String, Object> template) {
        if (!enabled()) {
            return;
        }
        String tenantId = truncate(string(template.get("tenant_id"), "default"), 64);
        String templateCode = truncate(string(template.get("template_code"), null), 64);
        String templateName = truncate(string(template.get("template_name"), ""), 100);
        String templateType = truncate(string(template.get("template_type"), ""), 32);
        String titleTemplate = truncate(string(template.get("title_template"), null), 200);
        String contentTemplate = string(template.get("content_template"), "");
        String channel = truncate(string(template.get("channel"), "IN_APP"), 32);
        int enabledFlag = Boolean.TRUE.equals(template.get("enabled")) || "1".equals(string(template.get("enabled"), null)) ? 1 : 0;
        String createdBy = truncate(string(template.get("created_by"), null), 64);

        if (properties.localFileDatabase()) {
            saveTemplateLocal(tenantId, templateCode, templateName, templateType, titleTemplate,
                    contentTemplate, channel, enabledFlag, createdBy);
        } else {
            saveTemplateOracle(tenantId, templateCode, templateName, templateType, titleTemplate,
                    contentTemplate, channel, enabledFlag, createdBy);
        }
    }

    private void saveTemplateLocal(String tenantId, String templateCode, String templateName,
                                   String templateType, String titleTemplate, String contentTemplate,
                                   String channel, int enabledFlag, String createdBy) {
        String updateSql = "UPDATE NOTIFY_TEMPLATE SET template_name=?, template_type=?, title_template=?, " +
                "content_template=?, enabled=?, created_by=COALESCE(?, created_by), updated_time=CURRENT_TIMESTAMP " +
                "WHERE tenant_id=? AND template_code=? AND channel=?";
        String insertSql = "INSERT INTO NOTIFY_TEMPLATE (id, tenant_id, template_code, template_name, " +
                "template_type, title_template, content_template, channel, enabled, created_by, " +
                "created_time, updated_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        try (Connection connection = connection()) {
            int affected;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, templateName);
                ps.setString(i++, templateType);
                ps.setString(i++, titleTemplate);
                ps.setString(i++, contentTemplate);
                ps.setInt(i++, enabledFlag);
                ps.setString(i++, createdBy);
                ps.setString(i++, tenantId);
                ps.setString(i++, templateCode);
                ps.setString(i++, channel);
                affected = ps.executeUpdate();
            }
            if (affected == 0) {
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ps.setLong(i++, nextId(tenantId));
                    ps.setString(i++, tenantId);
                    ps.setString(i++, templateCode);
                    ps.setString(i++, templateName);
                    ps.setString(i++, templateType);
                    ps.setString(i++, titleTemplate);
                    ps.setString(i++, contentTemplate);
                    ps.setString(i++, channel);
                    ps.setInt(i++, enabledFlag);
                    ps.setString(i++, createdBy);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save template failed: " + ex.getMessage(), ex);
        }
    }

    private void saveTemplateOracle(String tenantId, String templateCode, String templateName,
                                    String templateType, String titleTemplate, String contentTemplate,
                                    String channel, int enabledFlag, String createdBy) {
        String updateSql = "UPDATE NOTIFY_TEMPLATE SET template_name=?, template_type=?, title_template=?, " +
                "content_template=?, enabled=?, created_by=COALESCE(?, created_by), updated_time=SYSTIMESTAMP " +
                "WHERE tenant_id=? AND template_code=? AND channel=?";
        String insertSql = "INSERT INTO NOTIFY_TEMPLATE (id, tenant_id, template_code, template_name, " +
                "template_type, title_template, content_template, channel, enabled, created_by, " +
                "created_time, updated_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, SYSTIMESTAMP)";
        try (Connection connection = connection()) {
            int affected;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, templateName);
                ps.setString(i++, templateType);
                ps.setString(i++, titleTemplate);
                ps.setString(i++, contentTemplate);
                ps.setInt(i++, enabledFlag);
                ps.setString(i++, createdBy);
                ps.setString(i++, tenantId);
                ps.setString(i++, templateCode);
                ps.setString(i++, channel);
                affected = ps.executeUpdate();
            }
            if (affected == 0) {
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ps.setLong(i++, nextId(tenantId));
                    ps.setString(i++, tenantId);
                    ps.setString(i++, templateCode);
                    ps.setString(i++, templateName);
                    ps.setString(i++, templateType);
                    ps.setString(i++, titleTemplate);
                    ps.setString(i++, contentTemplate);
                    ps.setString(i++, channel);
                    ps.setInt(i++, enabledFlag);
                    ps.setString(i++, createdBy);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save template failed: " + ex.getMessage(), ex);
        }
    }

    /** 查询通知模板列表。 */
    public List<Map<String, Object>> listTemplates(String tenantId) {
        if (!enabled()) {
            return new ArrayList<Map<String, Object>>();
        }
        String sql = "SELECT id, tenant_id, template_code, template_name, template_type, title_template, " +
                "content_template, channel, enabled, created_by, created_time, updated_time " +
                "FROM NOTIFY_TEMPLATE WHERE tenant_id=? ORDER BY template_code, channel";
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(toTemplateMap(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list templates failed: " + ex.getMessage(), ex);
        }
        return results;
    }

    /** 获取单条通知模板。 */
    public Map<String, Object> getTemplate(String tenantId, String templateCode, String channel) {
        if (!enabled()) {
            return null;
        }
        String sql = "SELECT id, tenant_id, template_code, template_name, template_type, title_template, " +
                "content_template, channel, enabled, created_by, created_time, updated_time " +
                "FROM NOTIFY_TEMPLATE WHERE tenant_id=? AND template_code=? AND channel=?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setString(2, templateCode);
            ps.setString(3, channel);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? toTemplateMap(rs) : null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get template failed: " + ex.getMessage(), ex);
        }
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

    private Map<String, Object> toNotificationMap(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("id", rs.getLong("id"));
        map.put("tenant_id", rs.getString("tenant_id"));
        map.put("org_code", rs.getString("org_code"));
        map.put("notification_code", rs.getString("notification_code"));
        map.put("title", rs.getString("title"));
        map.put("content", rs.getString("content"));
        map.put("notification_type", rs.getString("notification_type"));
        map.put("priority", rs.getString("priority"));
        map.put("status", rs.getString("status"));
        map.put("sender_id", rs.getString("sender_id"));
        map.put("sender_name", rs.getString("sender_name"));
        map.put("recipient_id", rs.getString("recipient_id"));
        map.put("recipient_name", rs.getString("recipient_name"));
        map.put("business_type", rs.getString("business_type"));
        map.put("business_id", rs.getString("business_id"));
        map.put("business_url", rs.getString("business_url"));
        map.put("channel", rs.getString("channel"));
        map.put("scheduled_time", formatTimestamp(rs.getTimestamp("scheduled_time")));
        map.put("sent_time", formatTimestamp(rs.getTimestamp("sent_time")));
        map.put("read_time", formatTimestamp(rs.getTimestamp("read_time")));
        map.put("expire_time", formatTimestamp(rs.getTimestamp("expire_time")));
        map.put("retry_count", rs.getInt("retry_count"));
        map.put("max_retries", rs.getInt("max_retries"));
        map.put("error_message", rs.getString("error_message"));
        map.put("created_by", rs.getString("created_by"));
        map.put("created_time", formatTimestamp(rs.getTimestamp("created_time")));
        map.put("updated_time", formatTimestamp(rs.getTimestamp("updated_time")));
        return map;
    }

    private Map<String, Object> toChannelConfigMap(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("id", rs.getLong("id"));
        map.put("tenant_id", rs.getString("tenant_id"));
        map.put("channel_code", rs.getString("channel_code"));
        map.put("channel_name", rs.getString("channel_name"));
        map.put("channel_type", rs.getString("channel_type"));
        map.put("enabled", rs.getInt("enabled"));
        map.put("config_json", rs.getString("config_json"));
        map.put("created_by", rs.getString("created_by"));
        map.put("created_time", formatTimestamp(rs.getTimestamp("created_time")));
        map.put("updated_time", formatTimestamp(rs.getTimestamp("updated_time")));
        return map;
    }

    private Map<String, Object> toTemplateMap(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("id", rs.getLong("id"));
        map.put("tenant_id", rs.getString("tenant_id"));
        map.put("template_code", rs.getString("template_code"));
        map.put("template_name", rs.getString("template_name"));
        map.put("template_type", rs.getString("template_type"));
        map.put("title_template", rs.getString("title_template"));
        map.put("content_template", rs.getString("content_template"));
        map.put("channel", rs.getString("channel"));
        map.put("enabled", rs.getInt("enabled"));
        map.put("created_by", rs.getString("created_by"));
        map.put("created_time", formatTimestamp(rs.getTimestamp("created_time")));
        map.put("updated_time", formatTimestamp(rs.getTimestamp("updated_time")));
        return map;
    }

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
