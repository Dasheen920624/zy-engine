-- SEC-012: 多身份源绑定、合并和解绑 DDL (DM 达梦)
-- 用户合并操作表、用户解绑操作表

-- 1. 用户合并操作表：记录用户合并操作
CREATE TABLE sec_user_merge (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  source_user_id BIGINT NOT NULL,
  target_user_id BIGINT NOT NULL,
  merge_reason VARCHAR(500),
  merge_status VARCHAR(32) NOT NULL,
  merged_by VARCHAR(64),
  merged_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT fk_sec_user_merge_source FOREIGN KEY (source_user_id) REFERENCES sec_user(id),
  CONSTRAINT fk_sec_user_merge_target FOREIGN KEY (target_user_id) REFERENCES sec_user(id)
);

-- 2. 用户解绑操作表：记录身份源解绑操作
CREATE TABLE sec_user_unbind (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  binding_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  unbind_reason VARCHAR(500),
  unbind_status VARCHAR(32) NOT NULL,
  previous_status VARCHAR(32) NOT NULL,
  new_status VARCHAR(32) NOT NULL,
  unbound_by VARCHAR(64),
  unbound_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
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