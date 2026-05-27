-- MedKernel v1.0 GA · GA-ENG-API-13 大规模列表 API（PostgreSQL）

CREATE TABLE IF NOT EXISTS large_list_export_job (
    id                        BIGSERIAL PRIMARY KEY,
    job_id                    VARCHAR(64)   NOT NULL,
    tenant_id                 VARCHAR(64)   NOT NULL,
    resource_type             VARCHAR(64)   NOT NULL,
    filter_criteria           TEXT          NULL,
    status                    VARCHAR(32)   NOT NULL DEFAULT 'PENDING',
    file_name                 VARCHAR(255)  NULL,
    file_path                 VARCHAR(512)  NULL,
    file_size                 BIGINT        NOT NULL DEFAULT 0,
    error_message             VARCHAR(512)  NULL,
    time_cost_ms              BIGINT        NOT NULL DEFAULT 0,
    trace_id                  VARCHAR(128)  NULL,
    created_at                TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    updated_at                TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    CONSTRAINT uk_large_list_job UNIQUE (job_id)
);

CREATE INDEX idx_large_list_job_tenant ON large_list_export_job (tenant_id, resource_type);

COMMENT ON TABLE large_list_export_job IS '大规模数据异步导出任务表';
COMMENT ON COLUMN large_list_export_job.id IS '自增主键';
COMMENT ON COLUMN large_list_export_job.job_id IS '异步导出任务全局唯一ID';
COMMENT ON COLUMN large_list_export_job.tenant_id IS '租户ID';
COMMENT ON COLUMN large_list_export_job.resource_type IS '导出的列表资源类型(如AUDIT_LOG等)';
COMMENT ON COLUMN large_list_export_job.filter_criteria IS '导出时提交的过滤筛选条件Json结构';
COMMENT ON COLUMN large_list_export_job.status IS '任务状态(PENDING,RUNNING,SUCCESS,FAILED)';
COMMENT ON COLUMN large_list_export_job.file_name IS '导出的物理文件名称';
COMMENT ON COLUMN large_list_export_job.file_path IS '物理存储路径或文件相对Key';
COMMENT ON COLUMN large_list_export_job.file_size IS '文件大小(字节)';
COMMENT ON COLUMN large_list_export_job.error_message IS '失败时存储的错误堆栈或异常描述';
COMMENT ON COLUMN large_list_export_job.time_cost_ms IS '导出耗时(毫秒)';
COMMENT ON COLUMN large_list_export_job.trace_id IS '请求链路追踪ID';
COMMENT ON COLUMN large_list_export_job.created_at IS '创建时间';
COMMENT ON COLUMN large_list_export_job.created_by IS '创建人账户或系统标识';
COMMENT ON COLUMN large_list_export_job.updated_at IS '最后更新时间';
COMMENT ON COLUMN large_list_export_job.updated_by IS '最后修改人账户或系统标识';
