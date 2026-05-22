package com.medkernel.impl.service;

import com.medkernel.common.TraceContext;
import com.medkernel.impl.entity.OnboardingChecklistItem;
import com.medkernel.impl.entity.OnboardingChecklistTemplate;
import com.medkernel.persistence.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 入驻清单服务：清单模板 + 清单检查项 CRUD
 */
@Service
public class OnboardingChecklistService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingChecklistService.class);

    private final DataSource dataSource;

    public OnboardingChecklistService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ==================== 清单模板管理 ====================

    public OnboardingChecklistTemplate createTemplate(OnboardingChecklistTemplate template) {
        String sql = "INSERT INTO impl_checklist_template (id, tenant_id, template_code, template_name, "
                + "description, phase, category, is_active, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            Long id = Ids.next();
            template.setId(id);
            ps.setLong(1, id);
            ps.setLong(2, template.getTenantId());
            ps.setString(3, template.getTemplateCode());
            ps.setString(4, template.getTemplateName());
            ps.setString(5, template.getDescription());
            ps.setString(6, template.getPhase());
            ps.setString(7, template.getCategory());
            ps.setBoolean(8, template.getIsActive() != null ? template.getIsActive() : true);
            ps.setString(9, TraceContext.getUsername());
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            log.info("Created checklist template: id={}, code={}", id, template.getTemplateCode());
        } catch (SQLException ex) {
            throw new IllegalStateException("create checklist template failed: " + ex.getMessage(), ex);
        }
        return template;
    }

    public OnboardingChecklistTemplate updateTemplate(OnboardingChecklistTemplate template) {
        String sql = "UPDATE impl_checklist_template SET template_name = ?, description = ?, "
                + "phase = ?, category = ?, is_active = ?, updated_by = ?, updated_time = ? "
                + "WHERE id = ? AND tenant_id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, template.getTemplateName());
            ps.setString(2, template.getDescription());
            ps.setString(3, template.getPhase());
            ps.setString(4, template.getCategory());
            ps.setBoolean(5, template.getIsActive() != null ? template.getIsActive() : true);
            ps.setString(6, TraceContext.getUsername());
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(8, template.getId());
            ps.setLong(9, template.getTenantId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update checklist template failed: " + ex.getMessage(), ex);
        }
        return template;
    }

    public List<OnboardingChecklistTemplate> listTemplates(Long tenantId, String phase, Boolean isActive) {
        StringBuilder sql = new StringBuilder("SELECT id, tenant_id, template_code, template_name, "
                + "description, phase, category, is_active, created_by, created_time, updated_by, updated_time "
                + "FROM impl_checklist_template WHERE tenant_id = ?");
        List<Object> params = new ArrayList<>();
        if (phase != null && !phase.isEmpty()) {
            sql.append(" AND phase = ?");
            params.add(phase);
        }
        if (isActive != null) {
            sql.append(" AND is_active = ?");
            params.add(isActive);
        }
        sql.append(" ORDER BY created_time DESC");

        List<OnboardingChecklistTemplate> templates = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            ps.setLong(1, tenantId);
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof String) {
                    ps.setString(i + 2, (String) param);
                } else if (param instanceof Boolean) {
                    ps.setBoolean(i + 2, (Boolean) param);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    templates.add(mapTemplate(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list checklist templates failed: " + ex.getMessage(), ex);
        }
        return templates;
    }

    public OnboardingChecklistTemplate getTemplate(Long templateId) {
        String sql = "SELECT id, tenant_id, template_code, template_name, "
                + "description, phase, category, is_active, created_by, created_time, updated_by, updated_time "
                + "FROM impl_checklist_template WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, templateId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTemplate(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get checklist template failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    // ==================== 清单检查项管理 ====================

    public OnboardingChecklistItem addChecklistItem(OnboardingChecklistItem item) {
        String sql = "INSERT INTO impl_checklist_item (id, tenant_id, template_id, item_code, item_name, "
                + "description, sort_order, is_required, check_method, expected_result, status, "
                + "checked_by, checked_time, remark, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            Long id = Ids.next();
            item.setId(id);
            ps.setLong(1, id);
            ps.setLong(2, item.getTenantId());
            ps.setLong(3, item.getTemplateId());
            ps.setString(4, item.getItemCode());
            ps.setString(5, item.getItemName());
            ps.setString(6, item.getDescription());
            ps.setInt(7, item.getSortOrder() != null ? item.getSortOrder() : 0);
            ps.setBoolean(8, item.getIsRequired() != null ? item.getIsRequired() : true);
            ps.setString(9, item.getCheckMethod());
            ps.setString(10, item.getExpectedResult());
            ps.setString(11, item.getStatus() != null ? item.getStatus() : "PENDING");
            ps.setString(12, item.getCheckedBy());
            if (item.getCheckedTime() != null) {
                ps.setTimestamp(13, Timestamp.valueOf(item.getCheckedTime()));
            } else {
                ps.setNull(13, Types.TIMESTAMP);
            }
            ps.setString(14, item.getRemark());
            ps.setString(15, TraceContext.getUsername());
            ps.setTimestamp(16, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            log.info("Added checklist item: id={}, code={}", id, item.getItemCode());
        } catch (SQLException ex) {
            throw new IllegalStateException("add checklist item failed: " + ex.getMessage(), ex);
        }
        return item;
    }

    public OnboardingChecklistItem updateChecklistItem(OnboardingChecklistItem item) {
        String sql = "UPDATE impl_checklist_item SET item_name = ?, description = ?, "
                + "sort_order = ?, is_required = ?, check_method = ?, expected_result = ?, "
                + "remark = ?, updated_by = ?, updated_time = ? "
                + "WHERE id = ? AND tenant_id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, item.getItemName());
            ps.setString(2, item.getDescription());
            ps.setInt(3, item.getSortOrder() != null ? item.getSortOrder() : 0);
            ps.setBoolean(4, item.getIsRequired() != null ? item.getIsRequired() : true);
            ps.setString(5, item.getCheckMethod());
            ps.setString(6, item.getExpectedResult());
            ps.setString(7, item.getRemark());
            ps.setString(8, TraceContext.getUsername());
            ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(10, item.getId());
            ps.setLong(11, item.getTenantId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update checklist item failed: " + ex.getMessage(), ex);
        }
        return item;
    }

    public OnboardingChecklistItem checkItem(Long itemId, Long tenantId, boolean checked) {
        String sql = "UPDATE impl_checklist_item SET status = ?, checked_by = ?, checked_time = ?, "
                + "updated_by = ?, updated_time = ? WHERE id = ? AND tenant_id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            String status = checked ? "CHECKED" : "PENDING";
            ps.setString(1, status);
            ps.setString(2, checked ? TraceContext.getUsername() : null);
            ps.setTimestamp(3, checked ? Timestamp.valueOf(LocalDateTime.now()) : null);
            ps.setString(4, TraceContext.getUsername());
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(6, itemId);
            ps.setLong(7, tenantId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("check checklist item failed: " + ex.getMessage(), ex);
        }
        return getChecklistItem(itemId);
    }

    public List<OnboardingChecklistItem> listChecklistItems(Long tenantId, Long templateId) {
        String sql = "SELECT id, tenant_id, template_id, item_code, item_name, description, "
                + "sort_order, is_required, check_method, expected_result, status, "
                + "checked_by, checked_time, remark, created_by, created_time, updated_by, updated_time "
                + "FROM impl_checklist_item WHERE tenant_id = ? AND template_id = ? "
                + "ORDER BY sort_order, created_time";
        List<OnboardingChecklistItem> items = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setLong(2, templateId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapChecklistItem(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list checklist items failed: " + ex.getMessage(), ex);
        }
        return items;
    }

    public OnboardingChecklistItem getChecklistItem(Long itemId) {
        String sql = "SELECT id, tenant_id, template_id, item_code, item_name, description, "
                + "sort_order, is_required, check_method, expected_result, status, "
                + "checked_by, checked_time, remark, created_by, created_time, updated_by, updated_time "
                + "FROM impl_checklist_item WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapChecklistItem(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get checklist item failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    // ==================== ResultSet 映射 ====================

    private OnboardingChecklistTemplate mapTemplate(ResultSet rs) throws SQLException {
        OnboardingChecklistTemplate t = new OnboardingChecklistTemplate();
        t.setId(rs.getLong("id"));
        t.setTenantId(rs.getLong("tenant_id"));
        t.setTemplateCode(rs.getString("template_code"));
        t.setTemplateName(rs.getString("template_name"));
        t.setDescription(rs.getString("description"));
        t.setPhase(rs.getString("phase"));
        t.setCategory(rs.getString("category"));
        t.setIsActive(rs.getBoolean("is_active"));
        t.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            t.setCreatedTime(createdTime.toLocalDateTime());
        }
        t.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            t.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        return t;
    }

    private OnboardingChecklistItem mapChecklistItem(ResultSet rs) throws SQLException {
        OnboardingChecklistItem item = new OnboardingChecklistItem();
        item.setId(rs.getLong("id"));
        item.setTenantId(rs.getLong("tenant_id"));
        item.setTemplateId(rs.getLong("template_id"));
        item.setItemCode(rs.getString("item_code"));
        item.setItemName(rs.getString("item_name"));
        item.setDescription(rs.getString("description"));
        item.setSortOrder(rs.getInt("sort_order"));
        item.setIsRequired(rs.getBoolean("is_required"));
        item.setCheckMethod(rs.getString("check_method"));
        item.setExpectedResult(rs.getString("expected_result"));
        item.setStatus(rs.getString("status"));
        item.setCheckedBy(rs.getString("checked_by"));
        Timestamp checkedTime = rs.getTimestamp("checked_time");
        if (checkedTime != null) {
            item.setCheckedTime(checkedTime.toLocalDateTime());
        }
        item.setRemark(rs.getString("remark"));
        item.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            item.setCreatedTime(createdTime.toLocalDateTime());
        }
        item.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            item.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        return item;
    }

    private Connection connection() throws SQLException {
        return dataSource.getConnection();
    }
}
