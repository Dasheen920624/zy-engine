import { http } from './client';

// 通知类型定义
export interface Notification {
  id: number;
  notificationCode: string;
  title: string;
  content: string;
  notificationType: 'SYSTEM' | 'WORKFLOW' | 'ALERT' | 'REMINDER';
  priority: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
  status: 'UNREAD' | 'READ' | 'ARCHIVED';
  senderId?: string;
  senderName?: string;
  recipientId: string;
  recipientName?: string;
  businessType?: string;
  businessId?: string;
  businessUrl?: string;
  channel: 'IN_APP' | 'EMAIL' | 'SMS' | 'WECHAT';
  scheduledTime?: string;
  sentTime?: string;
  readTime?: string;
  expireTime?: string;
  createdTime: string;
  updatedTime?: string;
}

export interface NotificationSummary {
  total: number;
  unread: number;
  read: number;
  archived: number;
}

export interface CreateNotificationRequest {
  title: string;
  content: string;
  notificationType: 'SYSTEM' | 'WORKFLOW' | 'ALERT' | 'REMINDER';
  priority?: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
  recipientId: string;
  recipientName?: string;
  senderId?: string;
  senderName?: string;
  businessType?: string;
  businessId?: string;
  businessUrl?: string;
  channel?: 'IN_APP' | 'EMAIL' | 'SMS' | 'WECHAT';
  scheduledTime?: string;
  expireTime?: string;
}

// 渠道配置类型
export interface ChannelConfig {
  id?: number;
  channelCode: string;
  channelName: string;
  channelType: 'EMAIL' | 'SMS' | 'WECHAT' | 'WEBHOOK';
  enabled: boolean;
  configJson?: string;
  createdBy?: string;
}

// 通知模板类型
export interface NotificationTemplate {
  id?: number;
  templateCode: string;
  templateName: string;
  templateType: 'SYSTEM' | 'WORKFLOW' | 'ALERT' | 'REMINDER';
  titleTemplate?: string;
  contentTemplate: string;
  channel: 'IN_APP' | 'EMAIL' | 'SMS' | 'WECHAT';
  enabled: boolean;
  createdBy?: string;
}

// 用户订阅设置类型
export interface NotificationSubscription {
  id?: number;
  userId: string;
  notificationType: 'SYSTEM' | 'WORKFLOW' | 'ALERT' | 'REMINDER';
  channel: 'IN_APP' | 'EMAIL' | 'SMS' | 'WECHAT';
  enabled: boolean;
}

// 投递日志类型
export interface DeliveryLog {
  id?: number;
  notificationId: number;
  channel: string;
  status: 'PENDING' | 'SENT' | 'FAILED' | 'CANCELLED';
  providerResponse?: string;
  errorMessage?: string;
  retryCount: number;
  createdTime?: string;
}

// 通知 API
export const notificationApi = {
  // 创建通知
  async createNotification(data: CreateNotificationRequest): Promise<Notification> {
    const response = await http.post('/api/notifications', data);
    return response.data;
  },

  // 查询通知列表
  async fetchNotifications(params?: {
    recipientId?: string;
    status?: string;
    notificationType?: string;
    priority?: string;
  }): Promise<Notification[]> {
    const response = await http.get('/api/notifications', { params });
    return response.data;
  },

  // 获取通知详情
  async fetchNotification(notificationCode: string): Promise<Notification> {
    const response = await http.get(`/api/notifications/${notificationCode}`);
    return response.data;
  },

  // 标记为已读
  async markAsRead(notificationCode: string): Promise<Notification> {
    const response = await http.post(`/api/notifications/${notificationCode}/read`);
    return response.data;
  },

  // 批量标记为已读
  async batchMarkAsRead(notificationCodes: string[]): Promise<{ successCount: number }> {
    const response = await http.post('/api/notifications/batch-read', { notificationCodes });
    return response.data;
  },

  // 归档通知
  async archiveNotification(notificationCode: string): Promise<Notification> {
    const response = await http.post(`/api/notifications/${notificationCode}/archive`);
    return response.data;
  },

  // 获取未读通知数量
  async fetchUnreadCount(recipientId: string): Promise<number> {
    const response = await http.get('/api/notifications/unread-count', {
      params: { recipientId }
    });
    return response.data.unreadCount;
  },

  // 获取通知统计
  async fetchNotificationSummary(recipientId: string): Promise<NotificationSummary> {
    const response = await http.get('/api/notifications/summary', {
      params: { recipientId }
    });
    return response.data;
  },

  // 清理过期通知
  async cleanupExpiredNotifications(): Promise<number> {
    const response = await http.post('/api/notifications/cleanup');
    return response.data.cleanedCount;
  },

  // 渠道配置
  async saveChannelConfig(data: ChannelConfig): Promise<ChannelConfig> {
    const response = await http.post('/api/notifications/channels', data);
    return response.data;
  },

  async fetchChannelConfigs(): Promise<ChannelConfig[]> {
    const response = await http.get('/api/notifications/channels');
    return response.data;
  },

  async fetchChannelConfig(channelCode: string): Promise<ChannelConfig> {
    const response = await http.get(`/api/notifications/channels/${channelCode}`);
    return response.data;
  },

  // 通知模板
  async saveTemplate(data: NotificationTemplate): Promise<NotificationTemplate> {
    const response = await http.post('/api/notifications/templates', data);
    return response.data;
  },

  async fetchTemplates(): Promise<NotificationTemplate[]> {
    const response = await http.get('/api/notifications/templates');
    return response.data;
  },

  async fetchTemplate(templateCode: string, channel?: string): Promise<NotificationTemplate> {
    const response = await http.get(`/api/notifications/templates/${templateCode}`, {
      params: { channel: channel ?? 'IN_APP' }
    });
    return response.data;
  },

  // 用户订阅设置
  async saveSubscription(data: NotificationSubscription): Promise<NotificationSubscription> {
    const response = await http.post('/api/notifications/subscriptions', data);
    return response.data;
  },

  async batchSaveSubscriptions(subscriptions: NotificationSubscription[]): Promise<{ savedCount: number }> {
    const response = await http.post('/api/notifications/subscriptions/batch', { subscriptions });
    return response.data;
  },

  async fetchSubscriptions(userId: string): Promise<NotificationSubscription[]> {
    const response = await http.get('/api/notifications/subscriptions', {
      params: { userId }
    });
    return response.data;
  },

  async updateSubscription(
    notificationType: string,
    channel: string,
    userId: string,
    enabled: boolean
  ): Promise<{ updated: boolean }> {
    const response = await http.put(`/api/notifications/subscriptions/${notificationType}/${channel}`, {
      userId,
      enabled
    });
    return response.data;
  },

  // 投递日志
  async fetchDeliveryLogs(notificationId: number): Promise<DeliveryLog[]> {
    const response = await http.get(`/api/notifications/${notificationId}/delivery-logs`);
    return response.data;
  },

  // 工作流联动通知
  async createWorkflowNotification(data: {
    taskCode: string;
    businessType?: string;
    title?: string;
    description?: string;
    assignedTo: string;
    createdBy?: string;
  }): Promise<Notification> {
    const response = await http.post('/api/notifications/workflow', data);
    return response.data;
  }
};