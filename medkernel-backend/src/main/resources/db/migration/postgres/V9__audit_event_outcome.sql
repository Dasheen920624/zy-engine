ALTER TABLE audit_event ADD COLUMN IF NOT EXISTS outcome VARCHAR(16) NOT NULL DEFAULT 'SUCCESS';
ALTER TABLE audit_event ADD COLUMN IF NOT EXISTS error_code VARCHAR(64) NULL;

ALTER TABLE audit_event ADD CONSTRAINT ck_audit_event_outcome CHECK (outcome IN ('SUCCESS','FAILED'));

CREATE INDEX IF NOT EXISTS idx_audit_event_outcome ON audit_event (tenant_id, outcome, occurred_at);
