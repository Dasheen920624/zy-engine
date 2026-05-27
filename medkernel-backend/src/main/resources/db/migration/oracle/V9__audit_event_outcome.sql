ALTER TABLE audit_event ADD outcome VARCHAR2(16) DEFAULT 'SUCCESS' NOT NULL;
ALTER TABLE audit_event ADD error_code VARCHAR2(64) NULL;

ALTER TABLE audit_event ADD CONSTRAINT ck_audit_event_outcome CHECK (outcome IN ('SUCCESS','FAILED'));

CREATE INDEX idx_audit_event_outcome ON audit_event (tenant_id, outcome, occurred_at);
