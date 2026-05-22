package com.medkernel.ops.entity;

import java.time.LocalDateTime;

/**
 * 部署包实体：记录离线部署和回滚信息
 *
 * <p>部署包状态流转：
 * <pre>
 *   BUILDING → READY → DEPLOYING → DEPLOYED
 *                                ↘ FAILED
 *   DEPLOYED → ROLLING_BACK → ROLLED_BACK
 * </pre>
 *
 * <p>部署包类型：
 * <ul>
 *   <li>FULL - 全量部署</li>
 *   <li>INCREMENTAL - 增量部署</li>
 *   <li>HOTFIX - 热修复</li>
 * </ul>
 */
public class DeploymentPackage {

    private Long id;
    private Long tenantId;
    private String packageCode;
    private String packageName;
    private String version;
    private String description;
    private String packageType;       // FULL/INCREMENTAL/HOTFIX
    private String targetEnvironment; // DEVELOPMENT/STAGING/PRODUCTION
    private String status;            // BUILDING/READY/DEPLOYING/DEPLOYED/ROLLING_BACK/ROLLED_BACK/FAILED
    private String artifactPath;      // 制品路径
    private String artifactHash;      // 制品哈希
    private long artifactSize;
    private String configSnapshot;    // 配置快照 JSON
    private String dbMigrationScripts; // 数据库迁移脚本 JSON
    private String rollbackScript;    // 回滚脚本
    private String deployedBy;
    private LocalDateTime deployedTime;
    private String rolledBackBy;
    private LocalDateTime rolledBackTime;
    private String rollbackReason;
    private String preCheckResult;    // 部署前检查结果 JSON
    private String postCheckResult;   // 部署后检查结果 JSON
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getPackageCode() {
        return packageCode;
    }

    public void setPackageCode(String packageCode) {
        this.packageCode = packageCode;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPackageType() {
        return packageType;
    }

    public void setPackageType(String packageType) {
        this.packageType = packageType;
    }

    public String getTargetEnvironment() {
        return targetEnvironment;
    }

    public void setTargetEnvironment(String targetEnvironment) {
        this.targetEnvironment = targetEnvironment;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getArtifactPath() {
        return artifactPath;
    }

    public void setArtifactPath(String artifactPath) {
        this.artifactPath = artifactPath;
    }

    public String getArtifactHash() {
        return artifactHash;
    }

    public void setArtifactHash(String artifactHash) {
        this.artifactHash = artifactHash;
    }

    public long getArtifactSize() {
        return artifactSize;
    }

    public void setArtifactSize(long artifactSize) {
        this.artifactSize = artifactSize;
    }

    public String getConfigSnapshot() {
        return configSnapshot;
    }

    public void setConfigSnapshot(String configSnapshot) {
        this.configSnapshot = configSnapshot;
    }

    public String getDbMigrationScripts() {
        return dbMigrationScripts;
    }

    public void setDbMigrationScripts(String dbMigrationScripts) {
        this.dbMigrationScripts = dbMigrationScripts;
    }

    public String getRollbackScript() {
        return rollbackScript;
    }

    public void setRollbackScript(String rollbackScript) {
        this.rollbackScript = rollbackScript;
    }

    public String getDeployedBy() {
        return deployedBy;
    }

    public void setDeployedBy(String deployedBy) {
        this.deployedBy = deployedBy;
    }

    public LocalDateTime getDeployedTime() {
        return deployedTime;
    }

    public void setDeployedTime(LocalDateTime deployedTime) {
        this.deployedTime = deployedTime;
    }

    public String getRolledBackBy() {
        return rolledBackBy;
    }

    public void setRolledBackBy(String rolledBackBy) {
        this.rolledBackBy = rolledBackBy;
    }

    public LocalDateTime getRolledBackTime() {
        return rolledBackTime;
    }

    public void setRolledBackTime(LocalDateTime rolledBackTime) {
        this.rolledBackTime = rolledBackTime;
    }

    public String getRollbackReason() {
        return rollbackReason;
    }

    public void setRollbackReason(String rollbackReason) {
        this.rollbackReason = rollbackReason;
    }

    public String getPreCheckResult() {
        return preCheckResult;
    }

    public void setPreCheckResult(String preCheckResult) {
        this.preCheckResult = preCheckResult;
    }

    public String getPostCheckResult() {
        return postCheckResult;
    }

    public void setPostCheckResult(String postCheckResult) {
        this.postCheckResult = postCheckResult;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }
}
