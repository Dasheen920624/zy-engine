-- OPS-004: 部署包表（离线部署 + 回滚）
-- Oracle DDL for production database.

BEGIN
  EXECUTE IMMEDIATE 'CREATE TABLE ops_deployment_package (
    id NUMBER(20) PRIMARY KEY,
    tenant_id VARCHAR2(64) DEFAULT ''default'' NOT NULL,
    package_code VARCHAR2(64) NOT NULL,
    package_name VARCHAR2(128) NOT NULL,
    version VARCHAR2(64) NOT NULL,
    description VARCHAR2(500),
    package_type VARCHAR2(32) NOT NULL,
    target_environment VARCHAR2(32) NOT NULL,
    status VARCHAR2(32) DEFAULT ''BUILDING'' NOT NULL,
    artifact_path VARCHAR2(500),
    artifact_hash VARCHAR2(128),
    artifact_size NUMBER(20) DEFAULT 0 NOT NULL,
    config_snapshot CLOB,
    db_migration_scripts CLOB,
    rollback_script CLOB,
    deployed_by VARCHAR2(64),
    deployed_time TIMESTAMP,
    rolled_back_by VARCHAR2(64),
    rolled_back_time TIMESTAMP,
    rollback_reason VARCHAR2(500),
    pre_check_result CLOB,
    post_check_result CLOB,
    created_by VARCHAR2(64),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR2(64),
    updated_time TIMESTAMP,
    CONSTRAINT uk_ops_deployment_package UNIQUE (tenant_id, package_code, version)
  )';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'CREATE INDEX idx_ops_deploy_tenant_env_status ON ops_deployment_package (tenant_id, target_environment, status)';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'CREATE INDEX idx_ops_deploy_tenant_env_time ON ops_deployment_package (tenant_id, target_environment, deployed_time)';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

COMMENT ON TABLE ops_deployment_package IS '部署包表';
COMMENT ON COLUMN ops_deployment_package.id IS '主键ID';
COMMENT ON COLUMN ops_deployment_package.tenant_id IS '租户ID';
COMMENT ON COLUMN ops_deployment_package.package_code IS '部署包编码';
COMMENT ON COLUMN ops_deployment_package.package_name IS '部署包名称';
COMMENT ON COLUMN ops_deployment_package.version IS '版本号';
COMMENT ON COLUMN ops_deployment_package.description IS '描述';
COMMENT ON COLUMN ops_deployment_package.package_type IS '包类型（FULL/INCREMENTAL/HOTFIX）';
COMMENT ON COLUMN ops_deployment_package.target_environment IS '目标环境（DEVELOPMENT/STAGING/PRODUCTION）';
COMMENT ON COLUMN ops_deployment_package.status IS '状态（BUILDING/READY/DEPLOYING/DEPLOYED/ROLLING_BACK/ROLLED_BACK/FAILED）';
COMMENT ON COLUMN ops_deployment_package.artifact_path IS '制品路径';
COMMENT ON COLUMN ops_deployment_package.artifact_hash IS '制品哈希';
COMMENT ON COLUMN ops_deployment_package.artifact_size IS '制品大小';
COMMENT ON COLUMN ops_deployment_package.config_snapshot IS '配置快照JSON';
COMMENT ON COLUMN ops_deployment_package.db_migration_scripts IS '数据库迁移脚本JSON';
COMMENT ON COLUMN ops_deployment_package.rollback_script IS '回滚脚本';
COMMENT ON COLUMN ops_deployment_package.deployed_by IS '部署人';
COMMENT ON COLUMN ops_deployment_package.deployed_time IS '部署时间';
COMMENT ON COLUMN ops_deployment_package.rolled_back_by IS '回滚人';
COMMENT ON COLUMN ops_deployment_package.rolled_back_time IS '回滚时间';
COMMENT ON COLUMN ops_deployment_package.rollback_reason IS '回滚原因';
COMMENT ON COLUMN ops_deployment_package.pre_check_result IS '部署前检查结果JSON';
COMMENT ON COLUMN ops_deployment_package.post_check_result IS '部署后检查结果JSON';
COMMENT ON COLUMN ops_deployment_package.created_by IS '创建人';
COMMENT ON COLUMN ops_deployment_package.created_time IS '创建时间';
COMMENT ON COLUMN ops_deployment_package.updated_by IS '更新人';
COMMENT ON COLUMN ops_deployment_package.updated_time IS '更新时间';
