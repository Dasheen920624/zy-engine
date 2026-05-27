-- MedKernel v1.0 GA · GA-ENG-API-01b retrofit V9
-- audit_event 加 outcome / error_code，让业务失败结构化留痕（spec §6.1 第 5 条）。
-- 注：audit_event.status 是审计链状态（RECORDED/SIGNED/TSA_SIGNED/REJECTED），
-- 与业务 outcome（SUCCESS/FAILED）语义不同，因此独立加列而非复用。

ALTER TABLE audit_event ADD COLUMN IF NOT EXISTS outcome VARCHAR(16) NOT NULL DEFAULT 'SUCCESS';
ALTER TABLE audit_event ADD COLUMN IF NOT EXISTS error_code VARCHAR(64) NULL;

ALTER TABLE audit_event ADD CONSTRAINT ck_audit_event_outcome CHECK (outcome IN ('SUCCESS','FAILED'));

CREATE INDEX IF NOT EXISTS idx_audit_event_outcome ON audit_event (tenant_id, outcome, occurred_at);
