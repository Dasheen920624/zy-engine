package com.medkernel.persistence;

import com.medkernel.organization.OrganizationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 组织目录持久化服务。
 * <p>
 * 支持 LOCAL_H2_FILE（开发库）和 Oracle/DM/PG-Kingbase（生产库）双模式。
 * 遵循 EnginePersistenceService 的 dual-mode 模式：
 * 生产库使用 MERGE INTO，开发库使用 UPDATE + INSERT fallback。
 */
@Service
public class OrganizationPersistenceService {
    private static final Logger log = LoggerFactory.getLogger(OrganizationPersistenceService.class);

    private final EnginePersistenceProperties properties;
    private final DataSource dataSource;

    public OrganizationPersistenceService(EnginePersistenceProperties properties, DataSource dataSource) {
        this.dataSource = dataSource;
        this.properties = properties;
    }

    public boolean enabled() {
        return properties.isEnabled() && properties.hasRequiredCredentials();
    }

    /**
     * 启动时从数据库加载全部组织单元到内存缓存。
     */
    @PostConstruct
    public void init() {
        if (!enabled()) {
            log.info("organization persistence disabled, skipping load");
            return;
        }
        List<OrganizationUnit> units = loadAllOrganizationUnits();
        log.info("loaded {} organization units from database", units.size());
    }

    /**
     * 保存或更新单个组织单元。
     */
    public void saveOrganizationUnit(OrganizationUnit unit) {
        if (!enabled()) {
            return;
        }
        // ORG-003 原本仅 H2 走 UPDATE+INSERT、Oracle 走 MERGE INTO USING FROM dual；后者 DM/PG/Kingbase 不兼容。
        // REVIEW-FIX-002 把所有方言统一到 UPDATE+INSERT 两阶段 upsert（CURRENT_TIMESTAMP 跨方言可用），
        // 兼顾 Oracle/DM/PG/H2 多方言，且保留 (tenant_id, level_code, org_code) 唯一约束保护。
        saveOrganizationUnitLocal(unit);
    }

    /**
     * 批量保存组织单元（导入场景）。
     */
    public void saveOrganizationUnits(List<OrganizationUnit> units) {
        if (!enabled() || units.isEmpty()) {
            return;
        }
        for (OrganizationUnit unit : units) {
            saveOrganizationUnit(unit);
        }
    }

    /**
     * 加载所有组织单元。
     */
    public List<OrganizationUnit> loadAllOrganizationUnits() {
        if (!enabled()) {
            return new ArrayList<OrganizationUnit>();
        }
        String sql = "SELECT tenant_id, level_code, org_code, org_name, parent_level_code, parent_org_code, " +
                "status, display_order, created_by, created_time, updated_time " +
                "FROM org_unit ORDER BY tenant_id, level_code, display_order, org_code";
        List<OrganizationUnit> units = new ArrayList<OrganizationUnit>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                units.add(toOrganizationUnit(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("load organization units failed: " + ex.getMessage(), ex);
        }
        return units;
    }

    /**
     * 按租户加载组织单元。
     */
    public List<OrganizationUnit> loadOrganizationUnitsByTenant(String tenantId) {
        if (!enabled()) {
            return new ArrayList<OrganizationUnit>();
        }
        String sql = "SELECT tenant_id, level_code, org_code, org_name, parent_level_code, parent_org_code, " +
                "status, display_order, created_by, created_time, updated_time " +
                "FROM org_unit WHERE tenant_id=? ORDER BY level_code, display_order, org_code";
        List<OrganizationUnit> units = new ArrayList<OrganizationUnit>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    units.add(toOrganizationUnit(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("load organization units by tenant failed: " + ex.getMessage(), ex);
        }
        return units;
    }

    /**
     * 删除指定组织单元。
     */
    public void deleteOrganizationUnit(String tenantId, String levelCode, String orgCode) {
        if (!enabled()) {
            return;
        }
        String sql = "DELETE FROM org_unit WHERE tenant_id=? AND level_code=? AND org_code=?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setString(2, levelCode);
            ps.setString(3, orgCode);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("delete organization unit failed: " + ex.getMessage(), ex);
        }
    }

    private void saveOrganizationUnitLocal(OrganizationUnit unit) {
        String updateSql = "UPDATE org_unit SET org_name=?, parent_level_code=?, parent_org_code=?, " +
                "status=?, display_order=?, updated_time=CURRENT_TIMESTAMP " +
                "WHERE tenant_id=? AND level_code=? AND org_code=?";
        String insertSql = "INSERT INTO org_unit (id, tenant_id, level_code, org_code, org_name, " +
                "parent_level_code, parent_org_code, status, display_order, created_by, created_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection connection = connection()) {
            int updated;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, unit.getName());
                ps.setString(i++, unit.getParentLevel());
                ps.setString(i++, unit.getParentCode());
                ps.setString(i++, unit.getStatus());
                ps.setInt(i++, unit.getDisplayOrder() != null ? unit.getDisplayOrder() : 0);
                ps.setString(i++, unit.getTenantId());
                ps.setString(i++, unit.getLevel());
                ps.setString(i++, unit.getCode());
                updated = ps.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ps.setLong(i++, Ids.next());
                    ps.setString(i++, unit.getTenantId());
                    ps.setString(i++, unit.getLevel());
                    ps.setString(i++, unit.getCode());
                    ps.setString(i++, unit.getName());
                    ps.setString(i++, unit.getParentLevel());
                    ps.setString(i++, unit.getParentCode());
                    ps.setString(i++, unit.getStatus());
                    ps.setInt(i++, unit.getDisplayOrder() != null ? unit.getDisplayOrder() : 0);
                    ps.setString(i++, unit.getCreatedBy());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save local organization unit failed: " + ex.getMessage(), ex);
        }
    }

    private OrganizationUnit toOrganizationUnit(ResultSet rs) throws SQLException {
        OrganizationUnit unit = new OrganizationUnit();
        unit.setTenantId(rs.getString("tenant_id"));
        unit.setLevel(rs.getString("level_code"));
        unit.setCode(rs.getString("org_code"));
        unit.setName(rs.getString("org_name"));
        unit.setParentLevel(rs.getString("parent_level_code"));
        unit.setParentCode(rs.getString("parent_org_code"));
        unit.setStatus(rs.getString("status"));
        int displayOrder = rs.getInt("display_order");
        unit.setDisplayOrder(rs.wasNull() ? 0 : displayOrder);
        unit.setCreatedBy(rs.getString("created_by"));
        java.sql.Timestamp createdTime = rs.getTimestamp("created_time");
        unit.setCreatedTime(createdTime != null ? createdTime.toInstant().toString() : null);
        java.sql.Timestamp updatedTime = rs.getTimestamp("updated_time");
        unit.setUpdatedTime(updatedTime != null ? updatedTime.toInstant().toString() : null);
        return unit;
    }

    private Connection connection() throws SQLException {
        // PR-FINAL-15: 走 HikariCP 连接池（EngineDataSourceConfig 暴露的 DataSource）。
        // HikariCP 启动期通过 jdbcUrl 自动 Class.forName 加载驱动，不再需要 loadDriver()。
        return dataSource.getConnection();
    }
}
