-- 通知和消息中心 DDL (H2 本地开发库)
-- NOTIFY-001: 通知和消息中心
-- H2-compatible syntax: BIGINT instead of NUMBER(20), VARCHAR instead of VARCHAR2, etc.

-- 通知主表
CREATE TABLE IF NOT EXISTS notify_notification (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  org_code VARCHAR(64) NOT NULL,
  notification_code VARCHAR(64) NOT NULL,
  title VARCHAR(200) NOT NULL,
  content CLOB NOT NULL,
  notification_type VARCHAR(32) NOT NULL,  -- SYSTEM/WORKFLOW/ALERT/REMINDER
  priority VARCHAR(32) DEFAULT 'NORMAL' NOT NULL,  -- LOW/NORMAL/HIGH/URGENT
  status VARCHAR(32) DEFAULT 'UNREAD' NOT NULL,  -- UNREAD/READ/ARCHIVED
  sender_id VARCHAR(64),
  sender_name VARCHAR(100),
  recipient_id VARCHAR(64) NOT NULL,
  recipient_name VARCHAR(100),
  business_type VARCHAR(64),  -- 关联业务类型
  business_id VARCHAR(64),    -- 关联业务ID
  business_url VARCHAR(500),  -- 关联业务跳转链接
  channel VARCHAR(32) DEFAULT 'IN_APP' NOT NULL,  -- IN_APP/EMAIL/SMS/WECHAT
  scheduled_time TIMESTAMP,    -- 定时发送时间
  sent_time TIMESTAMP,         -- 实际发送时间
  read_time TIMESTAMP,         -- 阅读时间
  expire_time TIMESTAMP,       -- 过期时间
  retry_count INTEGER DEFAULT 0 NOT NULL,
  max_retries INTEGER DEFAULT 3 NOT NULL,
  error_message VARCHAR(1000),
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_notify_notification UNIQUE (tenant_id, notification_code)
);

-- 通知渠道配置表
CREATE TABLE IF NOT EXISTS notify_channel_config (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  channel_code VARCHAR(64) NOT NULL,
  channel_name VARCHAR(100) NOT NULL,
  channel_type VARCHAR(32) NOT NULL,  -- EMAIL/SMS/WECHAT/WEBHOOK
  enabled SMALLINT DEFAULT 1 NOT NULL,
  config_json CLOB NOT NULL,  -- 渠道配置（SMTP/短信网关/企微配置等）
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_notify_channel UNIQUE (tenant_id, channel_code)
);

-- 通知模板表
CREATE TABLE IF NOT EXISTS notify_template (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  template_code VARCHAR(64) NOT NULL,
  template_name VARCHAR(100) NOT NULL,
  template_type VARCHAR(32) NOT NULL,  -- SYSTEM/WORKFLOW/ALERT/REMINDER
  title_template VARCHAR(200),
  content_template CLOB NOT NULL,
  channel VARCHAR(32) NOT NULL,  -- IN_APP/EMAIL/SMS/WECHAT
  enabled SMALLINT DEFAULT 1 NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_notify_template UNIQUE (tenant_id, template_code, channel)
);

-- 用户订阅设置表
CREATE TABLE IF NOT EXISTS notify_subscription (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  notification_type VARCHAR(32) NOT NULL,  -- SYSTEM/WORKFLOW/ALERT/REMINDER
  channel VARCHAR(32) NOT NULL,  -- IN_APP/EMAIL/SMS/WECHAT
  enabled SMALLINT DEFAULT 1 NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_notify_subscription UNIQUE (tenant_id, user_id, notification_type, channel)
);

-- 投递日志表
CREATE TABLE IF NOT EXISTS notify_delivery_log (
  id BIGINT PRIMARY KEY,
  notification_id BIGINT NOT NULL,
  channel VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,  -- PENDING/SENT/FAILED/CANCELLED
  provider_response CLOB,
  error_message VARCHAR(1000),
  retry_count INTEGER DEFAULT 0 NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_notify_recipient ON notify_notification(tenant_id, recipient_id, status);
CREATE INDEX IF NOT EXISTS idx_notify_type ON notify_notification(tenant_id, notification_type);
CREATE INDEX IF NOT EXISTS idx_notify_business ON notify_notification(tenant_id, business_type, business_id);
CREATE INDEX IF NOT EXISTS idx_notify_created ON notify_notification(tenant_id, created_time);
CREATE INDEX IF NOT EXISTS idx_notify_delivery_notification ON notify_delivery_log(notification_id);
CREATE INDEX IF NOT EXISTS idx_notify_subscription_user ON notify_subscription(tenant_id, user_id);
