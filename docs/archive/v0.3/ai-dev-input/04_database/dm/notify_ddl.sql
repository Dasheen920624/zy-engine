-- 通知和消息中心 DDL (达梦 DM)
-- NOTIFY-001: 通知和消息中心

-- 通知主表
CREATE TABLE NOTIFY_NOTIFICATION (
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
CREATE TABLE NOTIFY_CHANNEL_CONFIG (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  channel_code VARCHAR(64) NOT NULL,
  channel_name VARCHAR(100) NOT NULL,
  channel_type VARCHAR(32) NOT NULL,
  enabled INT DEFAULT 1 NOT NULL,
  config_json TEXT NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_notify_channel UNIQUE (tenant_id, channel_code)
);

-- 通知模板表
CREATE TABLE NOTIFY_TEMPLATE (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  template_code VARCHAR(64) NOT NULL,
  template_name VARCHAR(100) NOT NULL,
  template_type VARCHAR(32) NOT NULL,
  title_template VARCHAR(200),
  content_template TEXT NOT NULL,
  channel VARCHAR(32) NOT NULL,
  enabled INT DEFAULT 1 NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_notify_template UNIQUE (tenant_id, template_code, channel)
);

-- 用户订阅设置表
CREATE TABLE NOTIFY_SUBSCRIPTION (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  notification_type VARCHAR(32) NOT NULL,
  channel VARCHAR(32) NOT NULL,
  enabled INT DEFAULT 1 NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_notify_subscription UNIQUE (tenant_id, user_id, notification_type, channel)
);

-- 投递日志表
CREATE TABLE NOTIFY_DELIVERY_LOG (
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
CREATE INDEX idx_notify_recipient ON NOTIFY_NOTIFICATION(tenant_id, recipient_id, status);
CREATE INDEX idx_notify_type ON NOTIFY_NOTIFICATION(tenant_id, notification_type);
CREATE INDEX idx_notify_business ON NOTIFY_NOTIFICATION(tenant_id, business_type, business_id);
CREATE INDEX idx_notify_created ON NOTIFY_NOTIFICATION(tenant_id, created_time DESC);
CREATE INDEX idx_notify_delivery_notification ON NOTIFY_DELIVERY_LOG(notification_id);
CREATE INDEX idx_notify_subscription_user ON NOTIFY_SUBSCRIPTION(tenant_id, user_id);

-- 表注释
COMMENT ON TABLE NOTIFY_NOTIFICATION IS '通知主表';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.id IS '主键ID';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.tenant_id IS '租户ID';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.org_code IS '组织编码';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.notification_code IS '通知编码';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.title IS '通知标题';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.content IS '通知内容';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.notification_type IS '通知类型: SYSTEM/WORKFLOW/ALERT/REMINDER';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.priority IS '优先级: LOW/NORMAL/HIGH/URGENT';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.status IS '状态: UNREAD/READ/ARCHIVED';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.sender_id IS '发送人ID';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.sender_name IS '发送人姓名';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.recipient_id IS '接收人ID';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.recipient_name IS '接收人姓名';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.business_type IS '关联业务类型';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.business_id IS '关联业务ID';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.business_url IS '关联业务跳转链接';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.channel IS '通知渠道: IN_APP/EMAIL/SMS/WECHAT';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.scheduled_time IS '定时发送时间';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.sent_time IS '实际发送时间';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.read_time IS '阅读时间';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.expire_time IS '过期时间';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.retry_count IS '重试次数';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.max_retries IS '最大重试次数';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.error_message IS '错误信息';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.created_by IS '创建人';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.created_time IS '创建时间';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.updated_time IS '更新时间';

COMMENT ON TABLE NOTIFY_CHANNEL_CONFIG IS '通知渠道配置表';
COMMENT ON TABLE NOTIFY_TEMPLATE IS '通知模板表';
COMMENT ON TABLE NOTIFY_SUBSCRIPTION IS '用户订阅设置表';
COMMENT ON TABLE NOTIFY_DELIVERY_LOG IS '投递日志表';