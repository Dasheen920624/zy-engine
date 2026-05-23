-- 通知和消息中心 DDL (PostgreSQL/Kingbase)
-- NOTIFY-001: 通知和消息中心

-- 通知主表
CREATE TABLE IF NOT EXISTS notify_notification (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  org_code VARCHAR(64) NOT NULL,
  notification_code VARCHAR(64) NOT NULL,
  title VARCHAR(200) NOT NULL,
  content TEXT NOT NULL,
  notification_type VARCHAR(32) NOT NULL,
  priority VARCHAR(32) DEFAULT 'NORMAL' NOT NULL,
  status VARCHAR(32) DEFAULT 'UNREAD' NOT NULL,
  sender_id VARCHAR(64),
  sender_name VARCHAR(100),
  recipient_id VARCHAR(64) NOT NULL,
  recipient_name VARCHAR(100),
  business_type VARCHAR(64),
  business_id VARCHAR(64),
  business_url VARCHAR(500),
  channel VARCHAR(32) DEFAULT 'IN_APP' NOT NULL,
  scheduled_time TIMESTAMP,
  sent_time TIMESTAMP,
  read_time TIMESTAMP,
  expire_time TIMESTAMP,
  retry_count INT DEFAULT 0 NOT NULL,
  max_retries INT DEFAULT 3 NOT NULL,
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
  channel_type VARCHAR(32) NOT NULL,
  enabled BOOLEAN DEFAULT TRUE NOT NULL,
  config_json TEXT NOT NULL,
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
  template_type VARCHAR(32) NOT NULL,
  title_template VARCHAR(200),
  content_template TEXT NOT NULL,
  channel VARCHAR(32) NOT NULL,
  enabled BOOLEAN DEFAULT TRUE NOT NULL,
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
  notification_type VARCHAR(32) NOT NULL,
  channel VARCHAR(32) NOT NULL,
  enabled BOOLEAN DEFAULT TRUE NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_notify_subscription UNIQUE (tenant_id, user_id, notification_type, channel)
);

-- 投递日志表
CREATE TABLE IF NOT EXISTS notify_delivery_log (
  id BIGINT PRIMARY KEY,
  notification_id BIGINT NOT NULL,
  channel VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  provider_response TEXT,
  error_message VARCHAR(1000),
  retry_count INT DEFAULT 0 NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_notify_recipient ON notify_notification(tenant_id, recipient_id, status);
CREATE INDEX IF NOT EXISTS idx_notify_type ON notify_notification(tenant_id, notification_type);
CREATE INDEX IF NOT EXISTS idx_notify_business ON notify_notification(tenant_id, business_type, business_id);
CREATE INDEX IF NOT EXISTS idx_notify_created ON notify_notification(tenant_id, created_time DESC);
CREATE INDEX IF NOT EXISTS idx_notify_delivery_notification ON notify_delivery_log(notification_id);
CREATE INDEX IF NOT EXISTS idx_notify_subscription_user ON notify_subscription(tenant_id, user_id);

-- 表注释
COMMENT ON TABLE notify_notification IS '通知主表';
COMMENT ON TABLE notify_channel_config IS '通知渠道配置表';
COMMENT ON TABLE notify_template IS '通知模板表';
COMMENT ON TABLE notify_subscription IS '用户订阅设置表';
COMMENT ON TABLE notify_delivery_log IS '投递日志表';