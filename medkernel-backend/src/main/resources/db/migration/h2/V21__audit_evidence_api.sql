-- MedKernel v1.0 GA · GA-ENG-EVID-01 合规证据链防篡改存证（H2 PostgreSQL 兼容模式）

CREATE TABLE IF NOT EXISTS evidence_snapshot (
    id                  BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    evidence_id         VARCHAR(64)  NOT NULL,
    tenant_id           VARCHAR(64)  NOT NULL,
    trace_id            VARCHAR(128) NULL,
    evidence_type       VARCHAR(64)  NOT NULL,
    action              VARCHAR(32)  NOT NULL,
    subject_type        VARCHAR(128) NOT NULL,
    subject_id          VARCHAR(64)  NOT NULL,
    evidence_summary    VARCHAR(512) NOT NULL,
    payload_snapshot    VARCHAR(4000) NOT NULL,
    payload_hash        VARCHAR(128) NOT NULL,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(64)   NOT NULL DEFAULT 'system',
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(64)   NOT NULL DEFAULT 'system',
    CONSTRAINT uk_evidence_snapshot UNIQUE (evidence_id)
);

CREATE INDEX IF NOT EXISTS idx_evd_tenant ON evidence_snapshot (tenant_id, evidence_type);
CREATE INDEX IF NOT EXISTS idx_evd_trace ON evidence_snapshot (trace_id);

COMMENT ON TABLE evidence_snapshot IS '可信数据审计快照与防篡改存证表';
COMMENT ON COLUMN evidence_snapshot.id IS '自增物理主键';
COMMENT ON COLUMN evidence_snapshot.evidence_id IS '全局唯一存证证据 ID';
COMMENT ON COLUMN evidence_snapshot.tenant_id IS '租户 ID';
COMMENT ON COLUMN evidence_snapshot.trace_id IS '全链路追踪 traceId';
COMMENT ON COLUMN evidence_snapshot.evidence_type IS '证据资产类型 (KNOWLEDGE_SOURCE, TERM_MAPPING, RULE_PUBLISH 等)';
COMMENT ON COLUMN evidence_snapshot.action IS '关键操作动作 (CREATE, RELEASE, FEEDBACK 等)';
COMMENT ON COLUMN evidence_snapshot.subject_type IS '业务关联的实体对象类型';
COMMENT ON COLUMN evidence_snapshot.subject_id IS '业务关联实体对象全局唯一 ID';
COMMENT ON COLUMN evidence_snapshot.evidence_summary IS '证据简要描述';
COMMENT ON COLUMN evidence_snapshot.payload_snapshot IS '业务发生时的完整明文 JSON 快照报文';
COMMENT ON COLUMN evidence_snapshot.payload_hash IS '防伪 SHA-256 数字指纹签名';
COMMENT ON COLUMN evidence_snapshot.created_at IS '证据生成创建时点';
COMMENT ON COLUMN evidence_snapshot.created_by IS '操作人账户';
COMMENT ON COLUMN evidence_snapshot.updated_at IS '最后更新时点';
COMMENT ON COLUMN evidence_snapshot.updated_by IS '最后更新人';

