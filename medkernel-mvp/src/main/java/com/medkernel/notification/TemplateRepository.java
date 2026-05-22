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
public class TemplateRepository extends PersistenceRepositorySupport {

    public TemplateRepository(EnginePersistenceProperties properties,
                              ObjectMapper objectMapper,
                              DataSource dataSource,
                              IdAllocatorRepository idAllocatorRepository) {
        super(properties, objectMapper, dataSource, idAllocatorRepository);
    }

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
}
