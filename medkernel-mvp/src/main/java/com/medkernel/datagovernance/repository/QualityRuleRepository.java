package com.medkernel.datagovernance.repository;

import com.medkernel.datagovernance.entity.QualityRuleEntity;
import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据质量规则数据库访问层
 */
@Repository
public class QualityRuleRepository {
    private static final Logger log = LoggerFactory.getLogger(QualityRuleRepository.class);

    private final EnginePersistenceProperties properties;

    public QualityRuleRepository(EnginePersistenceProperties properties) {
        this.properties = properties;
    }

    /**
     * 保存数据质量规则（新增或更新）
     */
    public void save(QualityRuleEntity entity) {
        if (!properties.isEnabled() || !properties.hasRequiredCredentials()) {
            return;
        }

        if (entity.getId() == null) {
            entity.setId(Ids.next());
        }

        try (Connection connection = connection()) {
            Long existingId = findIdByUniqueKey(connection, entity.getTenantId(), entity.getRuleCode());
            if (existingId != null) {
                entity.setId(existingId);
                updateExisting(connection, entity);
            } else {
                insertNew(connection, entity);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save quality rule failed: " + ex.getMessage(), ex);
        }
    }

    private Long findIdByUniqueKey(Connection connection, String tenantId, String ruleCode) throws SQLException {
        String sql = "SELECT id FROM dg_quality_rule WHERE tenant_id = ? AND rule_code = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, tenantId);
            ps.setString(i++, ruleCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return null;
    }

    private void insertNew(Connection connection, QualityRuleEntity entity) throws SQLException {
        String sql = "INSERT INTO dg_quality_rule " +
                "(id, tenant_id, rule_code, rule_name, rule_type, target_entity, target_field, rule_expression, severity, status, created_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, entity.getId());
            ps.setString(i++, entity.getTenantId());
            ps.setString(i++, entity.getRuleCode());
            ps.setString(i++, entity.getRuleName());
            ps.setString(i++, entity.getRuleType());
            ps.setString(i++, entity.getTargetEntity());
            ps.setString(i++, entity.getTargetField());
            ps.setString(i++, entity.getRuleExpression());
            ps.setString(i++, entity.getSeverity());
            ps.setString(i++, entity.getStatus());
            ps.setTimestamp(i++, entity.getCreatedTime() != null ? Timestamp.valueOf(entity.getCreatedTime()) : Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    private void updateExisting(Connection connection, QualityRuleEntity entity) throws SQLException {
        String sql = "UPDATE dg_quality_rule SET " +
                "rule_name = ?, rule_type = ?, target_entity = ?, target_field = ?, rule_expression = ?, " +
                "severity = ?, status = ?, updated_time = ? " +
                "WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, entity.getRuleName());
            ps.setString(i++, entity.getRuleType());
            ps.setString(i++, entity.getTargetEntity());
            ps.setString(i++, entity.getTargetField());
            ps.setString(i++, entity.getRuleExpression());
            ps.setString(i++, entity.getSeverity());
            ps.setString(i++, entity.getStatus());
            ps.setTimestamp(i++, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(i++, entity.getId());
            ps.executeUpdate();
        }
    }

    /**
     * 根据租户ID和规则编码查找规则
     */
    public QualityRuleEntity findByRuleCode(String tenantId, String ruleCode) {
        if (!properties.isEnabled() || !properties.hasRequiredCredentials()) {
            return null;
        }

        String sql = "SELECT * FROM dg_quality_rule WHERE tenant_id = ? AND rule_code = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, tenantId);
            ps.setString(i++, ruleCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find quality rule failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 根据租户ID查找所有规则
     */
    public List<QualityRuleEntity> findAllByTenantId(String tenantId) {
        if (!properties.isEnabled() || !properties.hasRequiredCredentials()) {
            return new ArrayList<>();
        }

        String sql = "SELECT * FROM dg_quality_rule WHERE tenant_id = ? ORDER BY created_time DESC";
        List<QualityRuleEntity> result = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find quality rules failed: " + ex.getMessage(), ex);
        }
        return result;
    }

    private QualityRuleEntity mapRow(ResultSet rs) throws SQLException {
        QualityRuleEntity entity = new QualityRuleEntity();
        entity.setId(rs.getLong("id"));
        entity.setTenantId(rs.getString("tenant_id"));
        entity.setRuleCode(rs.getString("rule_code"));
        entity.setRuleName(rs.getString("rule_name"));
        entity.setRuleType(rs.getString("rule_type"));
        entity.setTargetEntity(rs.getString("target_entity"));
        entity.setTargetField(rs.getString("target_field"));
        entity.setRuleExpression(rs.getString("rule_expression"));
        entity.setSeverity(rs.getString("severity"));
        entity.setStatus(rs.getString("status"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        entity.setCreatedTime(createdTime != null ? createdTime.toLocalDateTime() : null);
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        entity.setUpdatedTime(updatedTime != null ? updatedTime.toLocalDateTime() : null);
        return entity;
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(
                properties.getJdbcUrl(),
                properties.getUsername(),
                properties.getPassword());
    }
}