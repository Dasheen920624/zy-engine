-- SEC-008 菜单权限 DDL (Oracle)
-- 统一菜单按钮和数据权限模型

CREATE TABLE sec_menu_permission (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  menu_code VARCHAR2(64) NOT NULL,
  menu_name VARCHAR2(200) NOT NULL,
  menu_path VARCHAR2(500),
  menu_icon VARCHAR2(200),
  parent_code VARCHAR2(64),
  sort_order NUMBER(10) DEFAULT 0 NOT NULL,
  menu_type VARCHAR2(32) NOT NULL,           -- DIRECTORY/MENU/BUTTON
  permission_code VARCHAR2(128),              -- 权限编码
  permission_name VARCHAR2(200),              -- 权限名称
  permission_type VARCHAR2(32),               -- MENU/BUTTON/DATA
  data_permission_code VARCHAR2(64),          -- 关联数据权限编码
  visible VARCHAR2(8) DEFAULT 'TRUE' NOT NULL,-- TRUE/FALSE
  enabled VARCHAR2(2) DEFAULT 'Y' NOT NULL,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_menu_permission UNIQUE (tenant_id, menu_code)
);

COMMENT ON TABLE sec_menu_permission IS '菜单权限统一模型表';
COMMENT ON COLUMN sec_menu_permission.menu_code IS '菜单编码，租户内唯一';
COMMENT ON COLUMN sec_menu_permission.menu_name IS '菜单名称';
COMMENT ON COLUMN sec_menu_permission.menu_path IS '菜单路径/路由';
COMMENT ON COLUMN sec_menu_permission.menu_icon IS '菜单图标';
COMMENT ON COLUMN sec_menu_permission.parent_code IS '父级菜单编码';
COMMENT ON COLUMN sec_menu_permission.sort_order IS '排序序号';
COMMENT ON COLUMN sec_menu_permission.menu_type IS '菜单类型: DIRECTORY/MENU/BUTTON';
COMMENT ON COLUMN sec_menu_permission.permission_code IS '权限编码';
COMMENT ON COLUMN sec_menu_permission.permission_name IS '权限名称';
COMMENT ON COLUMN sec_menu_permission.permission_type IS '权限类型: MENU/BUTTON/DATA';
COMMENT ON COLUMN sec_menu_permission.data_permission_code IS '关联数据权限编码';
COMMENT ON COLUMN sec_menu_permission.visible IS '是否可见: TRUE/FALSE';
COMMENT ON COLUMN sec_menu_permission.enabled IS '是否启用: Y/N';
COMMENT ON COLUMN sec_menu_permission.created_by IS '创建人';
COMMENT ON COLUMN sec_menu_permission.created_time IS '创建时间';
COMMENT ON COLUMN sec_menu_permission.updated_by IS '更新人';
COMMENT ON COLUMN sec_menu_permission.updated_time IS '更新时间';

-- 索引
CREATE INDEX idx_sec_menu_parent ON sec_menu_permission (tenant_id, parent_code, sort_order);
CREATE INDEX idx_sec_menu_perm_type ON sec_menu_permission (tenant_id, permission_type, permission_code);
