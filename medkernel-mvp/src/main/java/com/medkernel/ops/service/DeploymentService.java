package com.medkernel.ops.service;

import com.medkernel.common.TraceContext;
import com.medkernel.ops.entity.DeploymentPackage;
import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/**
 * 部署服务：离线部署与回滚管理
 *
 * <p>核心功能：
 * <ul>
 *   <li>部署包管理 - 创建、更新、查询部署包</li>
 *   <li>部署操作 - 执行部署（含前置/后置检查）</li>
 *   <li>回滚操作 - 执行回滚（含回滚脚本）</li>
 *   <li>部署历史 - 查询部署历史和当前版本</li>
 * </ul>
 */
@Service
public class DeploymentService {

    private static final Logger log = LoggerFactory.getLogger(DeploymentService.class);

    private final EnginePersistenceProperties properties;
    private final DataSource dataSource;

    public DeploymentService(EnginePersistenceProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
    }

    // ==================== 部署包管理 ====================

    /**
     * 创建部署包
     */
    public DeploymentPackage createDeploymentPackage(DeploymentPackage pkg) {
        String sql = "INSERT INTO ops_deployment_package (id, tenant_id, package_code, package_name, "
                + "version, description, package_type, target_environment, status, artifact_path, "
                + "artifact_hash, artifact_size, config_snapshot, db_migration_scripts, rollback_script, "
                + "created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            Long id = Ids.next();
            pkg.setId(id);
            ps.setLong(1, id);
            ps.setLong(2, pkg.getTenantId());
            ps.setString(3, pkg.getPackageCode());
            ps.setString(4, pkg.getPackageName());
            ps.setString(5, pkg.getVersion());
            ps.setString(6, pkg.getDescription());
            ps.setString(7, pkg.getPackageType());
            ps.setString(8, pkg.getTargetEnvironment());
            ps.setString(9, pkg.getStatus() != null ? pkg.getStatus() : "BUILDING");
            ps.setString(10, pkg.getArtifactPath());
            ps.setString(11, pkg.getArtifactHash());
            ps.setLong(12, pkg.getArtifactSize());
            ps.setString(13, pkg.getConfigSnapshot());
            ps.setString(14, pkg.getDbMigrationScripts());
            ps.setString(15, pkg.getRollbackScript());
            ps.setString(16, TraceContext.getUsername());
            ps.setTimestamp(17, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("create deployment package failed: " + ex.getMessage(), ex);
        }
        return pkg;
    }

    /**
     * 更新部署包
     */
    public DeploymentPackage updateDeploymentPackage(DeploymentPackage pkg) {
        String sql = "UPDATE ops_deployment_package SET package_name = ?, version = ?, "
                + "description = ?, package_type = ?, target_environment = ?, status = ?, "
                + "artifact_path = ?, artifact_hash = ?, artifact_size = ?, "
                + "config_snapshot = ?, db_migration_scripts = ?, rollback_script = ?, "
                + "updated_by = ?, updated_time = ? "
                + "WHERE id = ? AND tenant_id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, pkg.getPackageName());
            ps.setString(2, pkg.getVersion());
            ps.setString(3, pkg.getDescription());
            ps.setString(4, pkg.getPackageType());
            ps.setString(5, pkg.getTargetEnvironment());
            ps.setString(6, pkg.getStatus());
            ps.setString(7, pkg.getArtifactPath());
            ps.setString(8, pkg.getArtifactHash());
            ps.setLong(9, pkg.getArtifactSize());
            ps.setString(10, pkg.getConfigSnapshot());
            ps.setString(11, pkg.getDbMigrationScripts());
            ps.setString(12, pkg.getRollbackScript());
            ps.setString(13, TraceContext.getUsername());
            ps.setTimestamp(14, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(15, pkg.getId());
            ps.setLong(16, pkg.getTenantId());
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new IllegalArgumentException("Deployment package not found: " + pkg.getId());
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("update deployment package failed: " + ex.getMessage(), ex);
        }
        return pkg;
    }

    /**
     * 查询部署包
     */
    public List<DeploymentPackage> listDeploymentPackages(Long tenantId, String targetEnvironment, String status) {
        StringBuilder sql = new StringBuilder("SELECT id, tenant_id, package_code, package_name, "
                + "version, description, package_type, target_environment, status, artifact_path, "
                + "artifact_hash, artifact_size, config_snapshot, db_migration_scripts, rollback_script, "
                + "deployed_by, deployed_time, rolled_back_by, rolled_back_time, rollback_reason, "
                + "pre_check_result, post_check_result, created_by, created_time, updated_by, updated_time "
                + "FROM ops_deployment_package WHERE tenant_id = ?");
        List<String> params = new ArrayList<>();
        if (targetEnvironment != null && !targetEnvironment.isEmpty()) {
            sql.append(" AND target_environment = ?");
            params.add(targetEnvironment);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY created_time DESC");

        List<DeploymentPackage> packages = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            ps.setLong(1, tenantId);
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 2, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    packages.add(mapPackage(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list deployment packages failed: " + ex.getMessage(), ex);
        }
        return packages;
    }

    /**
     * 获取部署包详情
     */
    public DeploymentPackage getDeploymentPackage(Long packageId) {
        String sql = "SELECT id, tenant_id, package_code, package_name, "
                + "version, description, package_type, target_environment, status, artifact_path, "
                + "artifact_hash, artifact_size, config_snapshot, db_migration_scripts, rollback_script, "
                + "deployed_by, deployed_time, rolled_back_by, rolled_back_time, rollback_reason, "
                + "pre_check_result, post_check_result, created_by, created_time, updated_by, updated_time "
                + "FROM ops_deployment_package WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, packageId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPackage(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get deployment package failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    // ==================== 部署操作 ====================

    /**
     * 执行部署
     */
    public DeploymentPackage deploy(Long packageId, String deployedBy) {
        DeploymentPackage pkg = getDeploymentPackage(packageId);
        if (pkg == null) {
            throw new IllegalArgumentException("Deployment package not found: " + packageId);
        }
        if (!"READY".equals(pkg.getStatus()) && !"FAILED".equals(pkg.getStatus())) {
            throw new IllegalStateException("Package cannot be deployed in status: " + pkg.getStatus());
        }

        // 更新状态为 DEPLOYING
        updateStatus(packageId, "DEPLOYING", deployedBy);

        try {
            // 执行部署前检查
            String preCheckResult = preCheck(packageId);
            updatePreCheckResult(packageId, preCheckResult);

            // 记录部署时间和操作人
            updateDeployInfo(packageId, deployedBy);

            // 执行部署后检查
            String postCheckResult = postCheck(packageId);
            updatePostCheckResult(packageId, postCheckResult);

            // 更新状态为 DEPLOYED
            updateStatus(packageId, "DEPLOYED", deployedBy);

            log.info("Deployment completed: packageId={}, packageCode={}, version={}, deployedBy={}",
                    packageId, pkg.getPackageCode(), pkg.getVersion(), deployedBy);
        } catch (Exception ex) {
            // 更新状态为 FAILED
            updateStatus(packageId, "FAILED", deployedBy);
            log.error("Deployment failed: packageId={}, packageCode={}", packageId, pkg.getPackageCode(), ex);
            throw new IllegalStateException("Deployment failed: " + ex.getMessage(), ex);
        }

        return getDeploymentPackage(packageId);
    }

    /**
     * 执行回滚
     */
    public DeploymentPackage rollback(Long packageId, String rolledBackBy, String rollbackReason) {
        DeploymentPackage pkg = getDeploymentPackage(packageId);
        if (pkg == null) {
            throw new IllegalArgumentException("Deployment package not found: " + packageId);
        }
        if (!"DEPLOYED".equals(pkg.getStatus())) {
            throw new IllegalStateException("Only deployed packages can be rolled back, current status: " + pkg.getStatus());
        }

        // 更新状态为 ROLLING_BACK
        updateStatus(packageId, "ROLLING_BACK", rolledBackBy);

        try {
            // 执行回滚脚本
            executeRollbackScript(pkg);

            // 记录回滚时间和原因
            updateRollbackInfo(packageId, rolledBackBy, rollbackReason);

            // 更新状态为 ROLLED_BACK
            updateStatus(packageId, "ROLLED_BACK", rolledBackBy);

            log.info("Rollback completed: packageId={}, packageCode={}, version={}, rolledBackBy={}",
                    packageId, pkg.getPackageCode(), pkg.getVersion(), rolledBackBy);
        } catch (Exception ex) {
            // 更新状态为 FAILED
            updateStatus(packageId, "FAILED", rolledBackBy);
            log.error("Rollback failed: packageId={}, packageCode={}", packageId, pkg.getPackageCode(), ex);
            throw new IllegalStateException("Rollback failed: " + ex.getMessage(), ex);
        }

        return getDeploymentPackage(packageId);
    }

    /**
     * 部署前检查
     */
    public String preCheck(Long packageId) {
        DeploymentPackage pkg = getDeploymentPackage(packageId);
        if (pkg == null) {
            throw new IllegalArgumentException("Deployment package not found: " + packageId);
        }

        StringBuilder result = new StringBuilder();
        result.append("{\"checks\":[");

        // 检查制品路径
        boolean artifactOk = pkg.getArtifactPath() != null && !pkg.getArtifactPath().isEmpty();
        result.append("{\"name\":\"artifact_path\",\"passed\":").append(artifactOk).append("}");

        // 检查制品哈希
        boolean hashOk = pkg.getArtifactHash() != null && !pkg.getArtifactHash().isEmpty();
        result.append(",{\"name\":\"artifact_hash\",\"passed\":").append(hashOk).append("}");

        // 检查数据库迁移脚本
        boolean migrationOk = pkg.getDbMigrationScripts() != null && !pkg.getDbMigrationScripts().isEmpty();
        result.append(",{\"name\":\"db_migration_scripts\",\"passed\":").append(migrationOk).append("}");

        // 检查回滚脚本
        boolean rollbackOk = pkg.getRollbackScript() != null && !pkg.getRollbackScript().isEmpty();
        result.append(",{\"name\":\"rollback_script\",\"passed\":").append(rollbackOk).append("}");

        boolean allPassed = artifactOk && hashOk && migrationOk && rollbackOk;
        result.append("],\"passed\":").append(allPassed).append("}");

        return result.toString();
    }

    /**
     * 部署后检查
     */
    public String postCheck(Long packageId) {
        DeploymentPackage pkg = getDeploymentPackage(packageId);
        if (pkg == null) {
            throw new IllegalArgumentException("Deployment package not found: " + packageId);
        }

        StringBuilder result = new StringBuilder();
        result.append("{\"checks\":[");

        // 检查部署状态
        boolean statusOk = "DEPLOYING".equals(pkg.getStatus());
        result.append("{\"name\":\"deployment_status\",\"passed\":").append(statusOk).append("}");

        // 检查部署时间
        boolean timeOk = pkg.getDeployedTime() != null;
        result.append(",{\"name\":\"deployed_time\",\"passed\":").append(timeOk).append("}");

        boolean allPassed = statusOk && timeOk;
        result.append("],\"passed\":").append(allPassed).append("}");

        return result.toString();
    }

    // ==================== 部署历史 ====================

    /**
     * 获取部署历史
     */
    public List<DeploymentPackage> getDeploymentHistory(Long tenantId, String targetEnvironment, int limit) {
        StringBuilder sql = new StringBuilder("SELECT id, tenant_id, package_code, package_name, "
                + "version, description, package_type, target_environment, status, artifact_path, "
                + "artifact_hash, artifact_size, config_snapshot, db_migration_scripts, rollback_script, "
                + "deployed_by, deployed_time, rolled_back_by, rolled_back_time, rollback_reason, "
                + "pre_check_result, post_check_result, created_by, created_time, updated_by, updated_time "
                + "FROM ops_deployment_package WHERE tenant_id = ? AND status IN ('DEPLOYED','ROLLED_BACK','FAILED')");
        if (targetEnvironment != null && !targetEnvironment.isEmpty()) {
            sql.append(" AND target_environment = ?");
        }
        sql.append(" ORDER BY deployed_time DESC");

        List<DeploymentPackage> packages = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            ps.setLong(1, tenantId);
            if (targetEnvironment != null && !targetEnvironment.isEmpty()) {
                ps.setString(2, targetEnvironment);
            }
            ps.setMaxRows(limit > 0 ? limit : 50);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    packages.add(mapPackage(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get deployment history failed: " + ex.getMessage(), ex);
        }
        return packages;
    }

    /**
     * 获取当前部署版本
     */
    public DeploymentPackage getCurrentDeployment(Long tenantId, String targetEnvironment) {
        String sql = "SELECT id, tenant_id, package_code, package_name, "
                + "version, description, package_type, target_environment, status, artifact_path, "
                + "artifact_hash, artifact_size, config_snapshot, db_migration_scripts, rollback_script, "
                + "deployed_by, deployed_time, rolled_back_by, rolled_back_time, rollback_reason, "
                + "pre_check_result, post_check_result, created_by, created_time, updated_by, updated_time "
                + "FROM ops_deployment_package WHERE tenant_id = ? AND target_environment = ? "
                + "AND status = 'DEPLOYED' ORDER BY deployed_time DESC";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setString(2, targetEnvironment);
            ps.setMaxRows(1);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPackage(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get current deployment failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    // ==================== 内部方法 ====================

    private void updateStatus(Long packageId, String status, String operatedBy) {
        String sql = "UPDATE ops_deployment_package SET status = ?, updated_by = ?, updated_time = ? "
                + "WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, operatedBy);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(4, packageId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update deployment status failed: " + ex.getMessage(), ex);
        }
    }

    private void updateDeployInfo(Long packageId, String deployedBy) {
        String sql = "UPDATE ops_deployment_package SET deployed_by = ?, deployed_time = ?, "
                + "updated_by = ?, updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, deployedBy);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, deployedBy);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(5, packageId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update deploy info failed: " + ex.getMessage(), ex);
        }
    }

    private void updateRollbackInfo(Long packageId, String rolledBackBy, String rollbackReason) {
        String sql = "UPDATE ops_deployment_package SET rolled_back_by = ?, rolled_back_time = ?, "
                + "rollback_reason = ?, updated_by = ?, updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, rolledBackBy);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, rollbackReason);
            ps.setString(4, rolledBackBy);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(6, packageId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update rollback info failed: " + ex.getMessage(), ex);
        }
    }

    private void updatePreCheckResult(Long packageId, String preCheckResult) {
        String sql = "UPDATE ops_deployment_package SET pre_check_result = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, preCheckResult);
            ps.setLong(2, packageId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update pre-check result failed: " + ex.getMessage(), ex);
        }
    }

    private void updatePostCheckResult(Long packageId, String postCheckResult) {
        String sql = "UPDATE ops_deployment_package SET post_check_result = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, postCheckResult);
            ps.setLong(2, packageId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update post-check result failed: " + ex.getMessage(), ex);
        }
    }

    private void executeRollbackScript(DeploymentPackage pkg) {
        String rollbackScript = pkg.getRollbackScript();
        if (rollbackScript == null || rollbackScript.isEmpty()) {
            log.warn("No rollback script found for package: {}", pkg.getId());
            return;
        }
        // Split by semicolons and execute each statement
        String[] statements = rollbackScript.split(";");
        for (String stmt : statements) {
            String trimmed = stmt.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(trimmed)) {
                ps.executeUpdate();
            } catch (SQLException ex) {
                throw new IllegalStateException("Execute rollback script failed: " + ex.getMessage(), ex);
            }
        }
    }

    private DeploymentPackage mapPackage(ResultSet rs) throws SQLException {
        DeploymentPackage pkg = new DeploymentPackage();
        pkg.setId(rs.getLong("id"));
        pkg.setTenantId(rs.getLong("tenant_id"));
        pkg.setPackageCode(rs.getString("package_code"));
        pkg.setPackageName(rs.getString("package_name"));
        pkg.setVersion(rs.getString("version"));
        pkg.setDescription(rs.getString("description"));
        pkg.setPackageType(rs.getString("package_type"));
        pkg.setTargetEnvironment(rs.getString("target_environment"));
        pkg.setStatus(rs.getString("status"));
        pkg.setArtifactPath(rs.getString("artifact_path"));
        pkg.setArtifactHash(rs.getString("artifact_hash"));
        pkg.setArtifactSize(rs.getLong("artifact_size"));
        pkg.setConfigSnapshot(rs.getString("config_snapshot"));
        pkg.setDbMigrationScripts(rs.getString("db_migration_scripts"));
        pkg.setRollbackScript(rs.getString("rollback_script"));
        pkg.setDeployedBy(rs.getString("deployed_by"));

        Timestamp deployedTime = rs.getTimestamp("deployed_time");
        if (deployedTime != null) {
            pkg.setDeployedTime(deployedTime.toLocalDateTime());
        }

        pkg.setRolledBackBy(rs.getString("rolled_back_by"));

        Timestamp rolledBackTime = rs.getTimestamp("rolled_back_time");
        if (rolledBackTime != null) {
            pkg.setRolledBackTime(rolledBackTime.toLocalDateTime());
        }

        pkg.setRollbackReason(rs.getString("rollback_reason"));
        pkg.setPreCheckResult(rs.getString("pre_check_result"));
        pkg.setPostCheckResult(rs.getString("post_check_result"));
        pkg.setCreatedBy(rs.getString("created_by"));

        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            pkg.setCreatedTime(createdTime.toLocalDateTime());
        }

        pkg.setUpdatedBy(rs.getString("updated_by"));

        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            pkg.setUpdatedTime(updatedTime.toLocalDateTime());
        }

        return pkg;
    }

    private Connection connection() throws SQLException {
        return dataSource.getConnection();
    }
}
