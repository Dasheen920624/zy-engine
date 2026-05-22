-- SEC-008 菜单权限 DDL (H2 local file database)
-- 统一菜单按钮和数据权限模型
-- H2-compatible syntax: BIGINT, VARCHAR, CLOB, CURRENT_TIMESTAMP

CREATE TABLE IF NOT EXISTS sec_menu_permission (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  menu_code VARCHAR(64) NOT NULL,
  menu_name VARCHAR(200) NOT NULL,
  menu_path VARCHAR(500),
  menu_icon VARCHAR(200),
  parent_code VARCHAR(64),
  sort_order INT DEFAULT 0 NOT NULL,
  menu_type VARCHAR(32) NOT NULL,           -- DIRECTORY/MENU/BUTTON
  permission_code VARCHAR(128),              -- 权限编码
  permission_name VARCHAR(200),              -- 权限名称
  permission_type VARCHAR(32),               -- MENU/BUTTON/DATA
  data_permission_code VARCHAR(64),          -- 关联数据权限编码
  visible VARCHAR(8) DEFAULT 'TRUE' NOT NULL,-- TRUE/FALSE
  enabled VARCHAR(2) DEFAULT 'Y' NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_menu_permission UNIQUE (tenant_id, menu_code)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_sec_menu_parent ON sec_menu_permission (tenant_id, parent_code, sort_order);
CREATE INDEX IF NOT EXISTS idx_sec_menu_perm_type ON sec_menu_permission (tenant_id, permission_type, permission_code);
