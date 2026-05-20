-- GRAPH-005: 同步任务表（Neo4j 图谱同步等）
-- Oracle DDL for production database.
-- 与 OPS-002 共用表结构，GRAPH-005 优先落地图谱同步场景。

BEGIN
  EXECUTE IMMEDIATE 'CREATE TABLE ops_sync_task (
    id NUMBER(20) PRIMARY KEY,
    tenant_id VARCHAR2(64) DEFAULT ''default'' NOT NULL,
    task_code VARCHAR2(64) NOT NULL,
    task_type VARCHAR2(32) NOT NULL,
    target_system VARCHAR2(32) NOT NULL,
    target_version VARCHAR2(64),
    status VARCHAR2(32) DEFAULT ''PENDING'' NOT NULL,
    dry_run NUMBER(1) DEFAULT 0 NOT NULL,
    total_count NUMBER(10) DEFAULT 0 NOT NULL,
    success_count NUMBER(10) DEFAULT 0 NOT NULL,
    failed_count NUMBER(10) DEFAULT 0 NOT NULL,
    skip_count NUMBER(10) DEFAULT 0 NOT NULL,
    error_message VARCHAR2(2000),
    retry_count NUMBER(10) DEFAULT 0 NOT NULL,
    max_retries NUMBER(10) DEFAULT 3 NOT NULL,
    started_time TIMESTAMP,
    finished_time TIMESTAMP,
    duration_ms NUMBER(20),
    triggered_by VARCHAR2(64),
    detail_json CLOB,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_time TIMESTAMP,
    CONSTRAINT uk_ops_sync_task UNIQUE (tenant_id, task_code)
  )';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'CREATE TABLE ops_sync_task_detail (
    id NUMBER(20) PRIMARY KEY,
    tenant_id VARCHAR2(64) DEFAULT ''default'' NOT NULL,
    task_id NUMBER(20) NOT NULL,
    item_type VARCHAR2(32) NOT NULL,
    item_code VARCHAR2(128) NOT NULL,
    operation VARCHAR2(32) NOT NULL,
    status VARCHAR2(32) NOT NULL,
    error_message VARCHAR2(1000),
    neo4j_node_id VARCHAR2(128),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_ops_sync_task_detail UNIQUE (task_id, item_type, item_code)
  )';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'CREATE INDEX idx_ops_sync_task_status ON ops_sync_task (tenant_id, status)';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'CREATE INDEX idx_ops_sync_task_type ON ops_sync_task (tenant_id, task_type)';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'CREATE INDEX idx_ops_sync_task_detail_task ON ops_sync_task_detail (task_id)';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

-- 中文备注
COMMENT ON TABLE ops_sync_task IS '同步任务表';
COMMENT ON COLUMN ops_sync_task.id IS '主键ID';
COMMENT ON COLUMN ops_sync_task.tenant_id IS '租户ID';
COMMENT ON COLUMN ops_sync_task.task_code IS '任务编码';
COMMENT ON COLUMN ops_sync_task.task_type IS '任务类型（GRAPH_SYNC/USER_SYNC/KNOWLEDGE_SYNC）';
COMMENT ON COLUMN ops_sync_task.target_system IS '目标系统（NEO4J/ELASTICSEARCH/EXTERNAL）';
COMMENT ON COLUMN ops_sync_task.target_version IS '目标版本号';
COMMENT ON COLUMN ops_sync_task.status IS '任务状态（PENDING/RUNNING/SUCCESS/FAILED/CANCELLED）';
COMMENT ON COLUMN ops_sync_task.dry_run IS '是否干运行（0=否，1=是）';
COMMENT ON COLUMN ops_sync_task.total_count IS '总记录数';
COMMENT ON COLUMN ops_sync_task.success_count IS '成功数';
COMMENT ON COLUMN ops_sync_task.failed_count IS '失败数';
COMMENT ON COLUMN ops_sync_task.skip_count IS '跳过数';
COMMENT ON COLUMN ops_sync_task.error_message IS '错误信息';
COMMENT ON COLUMN ops_sync_task.retry_count IS '已重试次数';
COMMENT ON COLUMN ops_sync_task.max_retries IS '最大重试次数';
COMMENT ON COLUMN ops_sync_task.started_time IS '开始时间';
COMMENT ON COLUMN ops_sync_task.finished_time IS '完成时间';
COMMENT ON COLUMN ops_sync_task.duration_ms IS '耗时毫秒';
COMMENT ON COLUMN ops_sync_task.triggered_by IS '触发人';
COMMENT ON COLUMN ops_sync_task.detail_json IS '详细信息JSON';
COMMENT ON COLUMN ops_sync_task.created_time IS '创建时间';
COMMENT ON COLUMN ops_sync_task.updated_time IS '更新时间';

COMMENT ON TABLE ops_sync_task_detail IS '同步任务明细表';
COMMENT ON COLUMN ops_sync_task_detail.id IS '主键ID';
COMMENT ON COLUMN ops_sync_task_detail.tenant_id IS '租户ID';
COMMENT ON COLUMN ops_sync_task_detail.task_id IS '任务ID';
COMMENT ON COLUMN ops_sync_task_detail.item_type IS '项目类型（NODE/EDGE/VERSION/EVIDENCE）';
COMMENT ON COLUMN ops_sync_task_detail.item_code IS '项目编码';
COMMENT ON COLUMN ops_sync_task_detail.operation IS '操作类型（CREATE/UPDATE/DELETE/SKIP）';
COMMENT ON COLUMN ops_sync_task_detail.status IS '状态（SUCCESS/FAILED/SKIPPED）';
COMMENT ON COLUMN ops_sync_task_detail.error_message IS '错误信息';
COMMENT ON COLUMN ops_sync_task_detail.neo4j_node_id IS 'Neo4j节点ID';
COMMENT ON COLUMN ops_sync_task_detail.created_time IS '创建时间';
