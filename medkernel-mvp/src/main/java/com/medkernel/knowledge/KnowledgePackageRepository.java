package com.medkernel.knowledge;
import com.medkernel.common.TraceContext;
import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.EnginePersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
@Repository
public class KnowledgePackageRepository {
    private static final Logger log = LoggerFactory.getLogger(KnowledgePackageRepository.class);
    private final EnginePersistenceProperties properties;
    private final EnginePersistenceService persistenceService;
    private final DataSource dataSource;
    public KnowledgePackageRepository(EnginePersistenceProperties properties,
                                      EnginePersistenceService persistenceService,
                                      DataSource dataSource) {
        this.properties = properties;
        this.persistenceService = persistenceService;
        this.dataSource = dataSource;
    }
        public void save(KnowledgePackage pkg) {
            if (!persistenceService.enabled()) {
                return;
            }
            if (properties.localFileDatabase()) {
                saveLocal(pkg);
                return;
            }
            String sql = "INSERT INTO knowledge_package " +
                    "(id, tenant_id, package_code, package_name, package_version, description, " +
                    "export_type, status, source_tenant_id, source_tenant_name, " +
                    "target_tenant_id, target_tenant_name, " +
                    "rule_count, terminology_count, pathway_count, graph_count, source_count, " +
                    "content_hash, content_json, conflict_strategy, " +
                    "sync_mode, sync_status, sync_error, sync_time, " +
                    "created_by, created_time, updated_by, updated_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, ?, SYSTIMESTAMP)";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                int i = 1;
                ps.setLong(i++, pkg.getId());
                setNullableLong(ps, i++, pkg.getTenantId());
                ps.setString(i++, pkg.getPackageCode());
                ps.setString(i++, pkg.getPackageName());
                ps.setString(i++, pkg.getPackageVersion());
                ps.setString(i++, pkg.getDescription());
                ps.setString(i++, pkg.getExportType());
                ps.setString(i++, pkg.getStatus());
                ps.setString(i++, pkg.getSourceTenantId());
                ps.setString(i++, pkg.getSourceTenantName());
                ps.setString(i++, pkg.getTargetTenantId());
                ps.setString(i++, pkg.getTargetTenantName());
                ps.setInt(i++, pkg.getRuleCount());
                ps.setInt(i++, pkg.getTerminologyCount());
                ps.setInt(i++, pkg.getPathwayCount());
                ps.setInt(i++, pkg.getGraphCount());
                ps.setInt(i++, pkg.getSourceCount());
                ps.setString(i++, pkg.getContentHash());
                ps.setString(i++, pkg.getContentJson());
                ps.setString(i++, pkg.getConflictStrategy());
                ps.setString(i++, pkg.getSyncMode());
                ps.setString(i++, pkg.getSyncStatus());
                ps.setString(i++, pkg.getSyncError());
                ps.setTimestamp(i++, pkg.getSyncTime() == null ? null : Timestamp.valueOf(pkg.getSyncTime()));
                ps.setString(i++, pkg.getCreatedBy());
                ps.setString(i++, pkg.getUpdatedBy());
                ps.executeUpdate();
            } catch (SQLException ex) {
                log.warn("[traceId={}] save knowledge package to database failed: {}",
                        TraceContext.getTraceId(), ex.getMessage());
            }
        }
        private void saveLocal(KnowledgePackage pkg) {
            String sql = "INSERT INTO knowledge_package " +
                    "(id, tenant_id, package_code, package_name, package_version, description, " +
                    "export_type, status, source_tenant_id, source_tenant_name, " +
                    "target_tenant_id, target_tenant_name, " +
                    "rule_count, terminology_count, pathway_count, graph_count, source_count, " +
                    "content_hash, content_json, conflict_strategy, " +
                    "sync_mode, sync_status, sync_error, sync_time, " +
                    "created_by, created_time, updated_by, updated_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP)";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                int i = 1;
                ps.setLong(i++, pkg.getId());
                setNullableLong(ps, i++, pkg.getTenantId());
                ps.setString(i++, pkg.getPackageCode());
                ps.setString(i++, pkg.getPackageName());
                ps.setString(i++, pkg.getPackageVersion());
                ps.setString(i++, pkg.getDescription());
                ps.setString(i++, pkg.getExportType());
                ps.setString(i++, pkg.getStatus());
                ps.setString(i++, pkg.getSourceTenantId());
                ps.setString(i++, pkg.getSourceTenantName());
                ps.setString(i++, pkg.getTargetTenantId());
                ps.setString(i++, pkg.getTargetTenantName());
                ps.setInt(i++, pkg.getRuleCount());
                ps.setInt(i++, pkg.getTerminologyCount());
                ps.setInt(i++, pkg.getPathwayCount());
                ps.setInt(i++, pkg.getGraphCount());
                ps.setInt(i++, pkg.getSourceCount());
                ps.setString(i++, pkg.getContentHash());
                ps.setString(i++, pkg.getContentJson());
                ps.setString(i++, pkg.getConflictStrategy());
                ps.setString(i++, pkg.getSyncMode());
                ps.setString(i++, pkg.getSyncStatus());
                ps.setString(i++, pkg.getSyncError());
                ps.setTimestamp(i++, pkg.getSyncTime() == null ? null : Timestamp.valueOf(pkg.getSyncTime()));
                ps.setString(i++, pkg.getCreatedBy());
                ps.setString(i++, pkg.getUpdatedBy());
                ps.executeUpdate();
            } catch (SQLException ex) {
                log.warn("[traceId={}] save knowledge package to local database failed: {}",
                        TraceContext.getTraceId(), ex.getMessage());
            }
        }
        public void update(KnowledgePackage pkg) {
            if (!persistenceService.enabled()) {
                return;
            }
            if (properties.localFileDatabase()) {
                updateLocal(pkg);
                return;
            }
            String sql = "UPDATE knowledge_package SET status=?, sync_mode=?, sync_status=?, sync_error=?, " +
                    "sync_time=?, updated_by=?, updated_time=SYSTIMESTAMP " +
                    "WHERE id=?";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                int i = 1;
                ps.setString(i++, pkg.getStatus());
                ps.setString(i++, pkg.getSyncMode());
                ps.setString(i++, pkg.getSyncStatus());
                ps.setString(i++, pkg.getSyncError());
                ps.setTimestamp(i++, pkg.getSyncTime() == null ? null : Timestamp.valueOf(pkg.getSyncTime()));
                ps.setString(i++, pkg.getUpdatedBy());
                ps.setLong(i++, pkg.getId());
                ps.executeUpdate();
            } catch (SQLException ex) {
                log.warn("[traceId={}] update knowledge package in database failed: {}",
                        TraceContext.getTraceId(), ex.getMessage());
            }
        }
        private void updateLocal(KnowledgePackage pkg) {
            String sql = "UPDATE knowledge_package SET status=?, sync_mode=?, sync_status=?, sync_error=?, " +
                    "sync_time=?, updated_by=?, updated_time=CURRENT_TIMESTAMP " +
                    "WHERE id=?";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                int i = 1;
                ps.setString(i++, pkg.getStatus());
                ps.setString(i++, pkg.getSyncMode());
                ps.setString(i++, pkg.getSyncStatus());
                ps.setString(i++, pkg.getSyncError());
                ps.setTimestamp(i++, pkg.getSyncTime() == null ? null : Timestamp.valueOf(pkg.getSyncTime()));
                ps.setString(i++, pkg.getUpdatedBy());
                ps.setLong(i++, pkg.getId());
                ps.executeUpdate();
            } catch (SQLException ex) {
                log.warn("[traceId={}] update knowledge package in local database failed: {}",
                        TraceContext.getTraceId(), ex.getMessage());
            }
        }
        public KnowledgePackage findById(Long packageId) {
            if (!persistenceService.enabled()) {
                return null;
            }
            String sql = "SELECT id, tenant_id, package_code, package_name, package_version, description, " +
                    "export_type, status, source_tenant_id, source_tenant_name, " +
                    "target_tenant_id, target_tenant_name, " +
                    "rule_count, terminology_count, pathway_count, graph_count, source_count, " +
                    "content_hash, content_json, conflict_strategy, " +
                    "sync_mode, sync_status, sync_error, sync_time, " +
                    "created_by, created_time, updated_by, updated_time " +
                    "FROM knowledge_package WHERE id=?";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, packageId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? toKnowledgePackage(rs) : null;
                }
            } catch (SQLException ex) {
                log.warn("[traceId={}] find knowledge package from database failed: {}",
                        TraceContext.getTraceId(), ex.getMessage());
                return null;
            }
        }
        public List<KnowledgePackage> list(Long tenantId, String status) {
            if (!persistenceService.enabled()) {
                return new ArrayList<KnowledgePackage>();
            }
            StringBuilder sql = new StringBuilder(
                    "SELECT id, tenant_id, package_code, package_name, package_version, description, " +
                    "export_type, status, source_tenant_id, source_tenant_name, " +
                    "target_tenant_id, target_tenant_name, " +
                    "rule_count, terminology_count, pathway_count, graph_count, source_count, " +
                    "content_hash, content_json, conflict_strategy, " +
                    "sync_mode, sync_status, sync_error, sync_time, " +
                    "created_by, created_time, updated_by, updated_time " +
                    "FROM knowledge_package WHERE 1=1");
            List<Object> params = new ArrayList<Object>();
            if (tenantId != null) {
                sql.append(" AND tenant_id=?");
                params.add(tenantId);
            }
            if (status != null && !status.isEmpty()) {
                sql.append(" AND status=?");
                params.add(status);
            }
            sql.append(" ORDER BY created_time DESC");
            List<KnowledgePackage> result = new ArrayList<KnowledgePackage>();
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    Object param = params.get(i);
                    if (param instanceof Long) {
                        ps.setLong(i + 1, (Long) param);
                    } else {
                        ps.setString(i + 1, String.valueOf(param));
                    }
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.add(toKnowledgePackage(rs));
                    }
                }
            } catch (SQLException ex) {
                log.warn("[traceId={}] list knowledge packages from database failed: {}",
                        TraceContext.getTraceId(), ex.getMessage());
            }
            return result;
        }
        private KnowledgePackage toKnowledgePackage(ResultSet rs) throws SQLException {
            KnowledgePackage pkg = new KnowledgePackage();
            pkg.setId(rs.getLong("id"));
            long tenantId = rs.getLong("tenant_id");
            pkg.setTenantId(rs.wasNull() ? null : tenantId);
            pkg.setPackageCode(rs.getString("package_code"));
            pkg.setPackageName(rs.getString("package_name"));
            pkg.setPackageVersion(rs.getString("package_version"));
            pkg.setDescription(rs.getString("description"));
            pkg.setExportType(rs.getString("export_type"));
            pkg.setStatus(rs.getString("status"));
            pkg.setSourceTenantId(rs.getString("source_tenant_id"));
            pkg.setSourceTenantName(rs.getString("source_tenant_name"));
            pkg.setTargetTenantId(rs.getString("target_tenant_id"));
            pkg.setTargetTenantName(rs.getString("target_tenant_name"));
            pkg.setRuleCount(rs.getInt("rule_count"));
            pkg.setTerminologyCount(rs.getInt("terminology_count"));
            pkg.setPathwayCount(rs.getInt("pathway_count"));
            pkg.setGraphCount(rs.getInt("graph_count"));
            pkg.setSourceCount(rs.getInt("source_count"));
            pkg.setContentHash(rs.getString("content_hash"));
            pkg.setContentJson(rs.getString("content_json"));
            pkg.setConflictStrategy(rs.getString("conflict_strategy"));
            pkg.setSyncMode(rs.getString("sync_mode"));
            pkg.setSyncStatus(rs.getString("sync_status"));
            pkg.setSyncError(rs.getString("sync_error"));
            Timestamp syncTime = rs.getTimestamp("sync_time");
            pkg.setSyncTime(syncTime == null ? null : syncTime.toLocalDateTime());
            pkg.setCreatedBy(rs.getString("created_by"));
            Timestamp createdTime = rs.getTimestamp("created_time");
            pkg.setCreatedTime(createdTime == null ? null : createdTime.toLocalDateTime());
            pkg.setUpdatedBy(rs.getString("updated_by"));
            Timestamp updatedTime = rs.getTimestamp("updated_time");
            pkg.setUpdatedTime(updatedTime == null ? null : updatedTime.toLocalDateTime());
            return pkg;
        }
        // ==================== 辅助方法 ====================
        private Connection connection() throws SQLException {
            // PR-FINAL-15b: use the shared HikariCP DataSource from EngineDataSourceConfig.
            return dataSource.getConnection();
        }
        private void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
            if (value == null) {
                ps.setNull(index, java.sql.Types.BIGINT);
            } else {
                ps.setLong(index, value);
            }
        }
}
