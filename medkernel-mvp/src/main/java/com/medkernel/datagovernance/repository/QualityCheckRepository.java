package com.medkernel.datagovernance.repository;

import com.medkernel.datagovernance.entity.QualityCheckEntity;
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
 * 数据质量检查记录数据库访问层
 */
@Repository
public class QualityCheckRepository {
    private static final Logger log = LoggerFactory.getLogger(QualityCheckRepository.class);

    private final EnginePersistenceProperties properties;

    public QualityCheckRepository(EnginePersistenceProperties properties) {
        this.properties = properties;
    }

    /**
     * 保存数据质量检查记录
     */
    public void save(QualityCheckEntity entity) {
        if (!properties.isEnabled() || !properties.hasRequiredCredentials()) {
            return;
        }

        if (entity.getId() == null) {
            entity.setId(Ids.next());
        }

        try (Connection connection = connection()) {
            String sql = "INSERT INTO dg_quality_check " +
                    "(id, tenant_id, check_id, rule_code, target_entity, target_id, check_result, error_message, check_time, created_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                int i = 1;
                ps.setLong(i++, entity.getId());
                ps.setString(i++, entity.getTenantId());
                ps.setString(i++, entity.getCheckId());
                ps.setString(i++, entity.getRuleCode());
                ps.setString(i++, entity.getTargetEntity());
                ps.setString(i++, entity.getTargetId());
                ps.setString(i++, entity.getCheckResult());
                ps.setString(i++, entity.getErrorMessage());
                ps.setTimestamp(i++, entity.getCheckTime() != null ? Timestamp.valueOf(entity.getCheckTime()) : Timestamp.valueOf(LocalDateTime.now()));
                ps.setTimestamp(i++, entity.getCreatedTime() != null ? Timestamp.valueOf(entity.getCreatedTime()) : Timestamp.valueOf(LocalDateTime.now()));
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save quality check failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 根据租户ID和规则编码查找检查记录
     */
    public List<QualityCheckEntity> findByRuleCode(String tenantId, String ruleCode) {
        if (!properties.isEnabled() || !properties.hasRequiredCredentials()) {
            return new ArrayList<>();
        }

        String sql = "SELECT * FROM dg_quality_check WHERE tenant_id = ? AND rule_code = ? ORDER BY check_time DESC";
        List<QualityCheckEntity> result = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, tenantId);
            ps.setString(i++, ruleCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find quality checks failed: " + ex.getMessage(), ex);
        }
        return result;
    }

    /**
     * 根据租户ID查找所有检查记录
     */
    public List<QualityCheckEntity> findAllByTenantId(String tenantId) {
        if (!properties.isEnabled() || !properties.hasRequiredCredentials()) {
            return new ArrayList<>();
        }

        String sql = "SELECT * FROM dg_quality_check WHERE tenant_id = ? ORDER BY check_time DESC";
        List<QualityCheckEntity> result = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find quality checks failed: " + ex.getMessage(), ex);
        }
        return result;
    }

    private QualityCheckEntity mapRow(ResultSet rs) throws SQLException {
        QualityCheckEntity entity = new QualityCheckEntity();
        entity.setId(rs.getLong("id"));
        entity.setTenantId(rs.getString("tenant_id"));
        entity.setCheckId(rs.getString("check_id"));
        entity.setRuleCode(rs.getString("rule_code"));
        entity.setTargetEntity(rs.getString("target_entity"));
        entity.setTargetId(rs.getString("target_id"));
        entity.setCheckResult(rs.getString("check_result"));
        entity.setErrorMessage(rs.getString("error_message"));
        Timestamp checkTime = rs.getTimestamp("check_time");
        entity.setCheckTime(checkTime != null ? checkTime.toLocalDateTime() : null);
        Timestamp createdTime = rs.getTimestamp("created_time");
        entity.setCreatedTime(createdTime != null ? createdTime.toLocalDateTime() : null);
        return entity;
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(
                properties.getJdbcUrl(),
                properties.getUsername(),
                properties.getPassword());
    }
}