-- 通知和消息中心 DDL (H2 本地开发库)
-- NOTIFY-001: 通知和消息中心

-- 通知主表
CREATE TABLE IF NOT EXISTS NOTIFY_NOTIFICATION (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  org_code VARCHAR2(64) NOT NULL,
  notification_code VARCHAR2(64) NOT NULL,
  title VARCHAR2(200) NOT NULL,
  content CLOB NOT NULL,
  notification_type VARCHAR2(32) NOT NULL,  -- SYSTEM/WORKFLOW/ALERT/REMINDER
  priority VARCHAR2(32) DEFAULT 'NORMAL' NOT NULL,  -- LOW/NORMAL/HIGH/URGENT
  status VARCHAR2(32) DEFAULT 'UNREAD' NOT NULL,  -- UNREAD/READ/ARCHIVED
  sender_id VARCHAR2(64),
  sender_name VARCHAR2(100),
  recipient_id VARCHAR2(64) NOT NULL,
  recipient_name VARCHAR2(100),
  business_type VARCHAR2(64),  -- 关联业务类型
  business_id VARCHAR2(64),    -- 关联业务ID
  business_url VARCHAR2(500),  -- 关联业务跳转链接
  channel VARCHAR2(32) DEFAULT 'IN_APP' NOT NULL,  -- IN_APP/EMAIL/SMS/WECHAT
  scheduled_time TIMESTAMP,    -- 定时发送时间
  sent_time TIMESTAMP,         -- 实际发送时间
  read_time TIMESTAMP,         -- 阅读时间
  expire_time TIMESTAMP,       -- 过期时间
  retry_count NUMBER(10) DEFAULT 0 NOT NULL,
  max_retries NUMBER(10) DEFAULT 3 NOT NULL,
  error_message VARCHAR2(1000),
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_notify_notification UNIQUE (tenant_id, notification_code)
);

-- 通知渠道配置表
CREATE TABLE IF NOT EXISTS NOTIFY_CHANNEL_CONFIG (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  channel_code VARCHAR2(64) NOT NULL,
  channel_name VARCHAR2(100) NOT NULL,
  channel_type VARCHAR2(32) NOT NULL,  -- EMAIL/SMS/WECHAT/WEBHOOK
  enabled NUMBER(1) DEFAULT 1 NOT NULL,
  config_json CLOB NOT NULL,  -- 渠道配置（SMTP/短信网关/企微配置等）
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_notify_channel UNIQUE (tenant_id, channel_code)
);

-- 通知模板表
CREATE TABLE IF NOT EXISTS NOTIFY_TEMPLATE (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  template_code VARCHAR2(64) NOT NULL,
  template_name VARCHAR2(100) NOT NULL,
  template_type VARCHAR2(32) NOT NULL,  -- SYSTEM/WORKFLOW/ALERT/REMINDER
  title_template VARCHAR2(200),
  content_template CLOB NOT NULL,
  channel VARCHAR2(32) NOT NULL,  -- IN_APP/EMAIL/SMS/WECHAT
  enabled NUMBER(1) DEFAULT 1 NOT NULL,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_notify_template UNIQUE (tenant_id, template_code, channel)
);

-- 用户订阅设置表
CREATE TABLE IF NOT EXISTS NOTIFY_SUBSCRIPTION (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  user_id VARCHAR2(64) NOT NULL,
  notification_type VARCHAR2(32) NOT NULL,  -- SYSTEM/WORKFLOW/ALERT/REMINDER
  channel VARCHAR2(32) NOT NULL,  -- IN_APP/EMAIL/SMS/WECHAT
  enabled NUMBER(1) DEFAULT 1 NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_notify_subscription UNIQUE (tenant_id, user_id, notification_type, channel)
);

-- 投递日志表
CREATE TABLE IF NOT EXISTS NOTIFY_DELIVERY_LOG (
  id NUMBER(20) PRIMARY KEY,
  notification_id NUMBER(20) NOT NULL,
  channel VARCHAR2(32) NOT NULL,
  status VARCHAR2(32) NOT NULL,  -- PENDING/SENT/FAILED/CANCELLED
  provider_response CLOB,
  error_message VARCHAR2(1000),
  retry_count NUMBER(10) DEFAULT 0 NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_notify_recipient ON NOTIFY_NOTIFICATION(tenant_id, recipient_id, status);
CREATE INDEX IF NOT EXISTS idx_notify_type ON NOTIFY_NOTIFICATION(tenant_id, notification_type);
CREATE INDEX IF NOT EXISTS idx_notify_business ON NOTIFY_NOTIFICATION(tenant_id, business_type, business_id);
CREATE INDEX IF NOT EXISTS idx_notify_created ON NOTIFY_NOTIFICATION(tenant_id, created_time DESC);
CREATE INDEX IF NOT EXISTS idx_notify_delivery_notification ON NOTIFY_DELIVERY_LOG(notification_id);
CREATE INDEX IF NOT EXISTS idx_notify_subscription_user ON NOTIFY_SUBSCRIPTION(tenant_id, user_id);
