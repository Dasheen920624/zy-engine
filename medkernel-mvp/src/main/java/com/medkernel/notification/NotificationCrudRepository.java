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
public class NotificationCrudRepository extends PersistenceRepositorySupport {

    public NotificationCrudRepository(EnginePersistenceProperties properties,
                                      ObjectMapper objectMapper,
                                      DataSource dataSource,
                                      IdAllocatorRepository idAllocatorRepository) {
        super(properties, objectMapper, dataSource, idAllocatorRepository);
    }

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
}
