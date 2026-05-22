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
public class ChannelConfigRepository extends PersistenceRepositorySupport {

    public ChannelConfigRepository(EnginePersistenceProperties properties,
                                   ObjectMapper objectMapper,
                                   DataSource dataSource,
                                   IdAllocatorRepository idAllocatorRepository) {
        super(properties, objectMapper, dataSource, idAllocatorRepository);
    }

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
}
