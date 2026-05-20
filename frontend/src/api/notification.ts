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
  }
};