-- SEC-012: 多身份源绑定、合并和解绑 DDL (Oracle)
-- 用户合并操作表、用户解绑操作表

-- 1. 用户合并操作表：记录用户合并操作
CREATE TABLE sec_user_merge (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20) NOT NULL,
  source_user_id NUMBER(20) NOT NULL,
  target_user_id NUMBER(20) NOT NULL,
  merge_reason VARCHAR2(500),
  merge_status VARCHAR2(32) NOT NULL,
  merged_by VARCHAR2(64),
  merged_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT fk_sec_user_merge_source FOREIGN KEY (source_user_id) REFERENCES sec_user(id),
  CONSTRAINT fk_sec_user_merge_target FOREIGN KEY (target_user_id) REFERENCES sec_user(id)
);

-- 2. 用户解绑操作表：记录身份源解绑操作
CREATE TABLE sec_user_unbind (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20) NOT NULL,
  binding_id NUMBER(20) NOT NULL,
  user_id NUMBER(20) NOT NULL,
  unbind_reason VARCHAR2(500),
  unbind_status VARCHAR2(32) NOT NULL,
  previous_status VARCHAR2(32) NOT NULL,
  new_status VARCHAR2(32) NOT NULL,
  unbound_by VARCHAR2(64),
  unbound_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  CONSTRAINT fk_sec_user_unbind_binding FOREIGN KEY (binding_id) REFERENCES sec_identity_binding(id),
  CONSTRAINT fk_sec_user_unbind_user FOREIGN KEY (user_id) REFERENCES sec_user(id)
);

-- 创建索引
CREATE INDEX idx_sec_user_merge_tenant ON sec_user_merge(tenant_id);
CREATE INDEX idx_sec_user_merge_source ON sec_user_merge(source_user_id);
CREATE INDEX idx_sec_user_merge_target ON sec_user_merge(target_user_id);
CREATE INDEX idx_sec_user_merge_status ON sec_user_merge(merge_status);

CREATE INDEX idx_sec_user_unbind_tenant ON sec_user_unbind(tenant_id);
CREATE INDEX idx_sec_user_unbind_binding ON sec_user_unbind(binding_id);
CREATE INDEX idx_sec_user_unbind_user ON sec_user_unbind(user_id);
CREATE INDEX idx_sec_user_unbind_status ON sec_user_unbind(unbind_status);