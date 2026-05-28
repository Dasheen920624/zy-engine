-- MedKernel v1.0 GA · GA-SVC-CLINICAL-01 患者主索引（MPI）数据模型（Kingbase）

CREATE TABLE IF NOT EXISTS mpi_patient (
    id                  BIGSERIAL PRIMARY KEY,
    mpi_id              VARCHAR(64)  NOT NULL,
    tenant_id           VARCHAR(64)  NOT NULL,
    masked_name         VARCHAR(128) NOT NULL,
    gender              VARCHAR(10)  NOT NULL,
    age                 INT          NOT NULL,
    id_last4            VARCHAR(10)  NOT NULL,
    merged_count        INT          NOT NULL DEFAULT 0,
    status              VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    merged_into_mpi_id  VARCHAR(64)  NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(64)  NOT NULL DEFAULT 'system',
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(64)  NOT NULL DEFAULT 'system',
    CONSTRAINT uk_mpi_patient_id UNIQUE (mpi_id)
);

CREATE INDEX IF NOT EXISTS idx_mpi_patient_tenant_status ON mpi_patient (tenant_id, status);

COMMENT ON TABLE mpi_patient IS '患者主索引信息表';
COMMENT ON COLUMN mpi_patient.id IS '自增物理主键';
COMMENT ON COLUMN mpi_patient.mpi_id IS '患者主索引 ID';
COMMENT ON COLUMN mpi_patient.tenant_id IS '租户 ID';
COMMENT ON COLUMN mpi_patient.masked_name IS '脱敏患者姓名';
COMMENT ON COLUMN mpi_patient.gender IS '性别 M/F';
COMMENT ON COLUMN mpi_patient.age IS '年龄';
COMMENT ON COLUMN mpi_patient.id_last4 IS '身份证后四位';
COMMENT ON COLUMN mpi_patient.merged_count IS '合并的主索引数量';
COMMENT ON COLUMN mpi_patient.status IS '状态 ACTIVE/MERGED_INTO';
COMMENT ON COLUMN mpi_patient.merged_into_mpi_id IS '被合并入的患者主索引 ID';
COMMENT ON COLUMN mpi_patient.created_at IS '创建时间点';
COMMENT ON COLUMN mpi_patient.created_by IS '创建人';
COMMENT ON COLUMN mpi_patient.updated_at IS '最后更新时间点';
COMMENT ON COLUMN mpi_patient.updated_by IS '最后更新人';
