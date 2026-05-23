import React, { useState, useEffect } from 'react';
import { Card, List, Badge, Tag, Button, Space, Typography, Empty, Spin, message, Dropdown, Menu } from 'antd';
import { BellOutlined, CheckOutlined, FilterOutlined } from '@ant-design/icons';
import { notificationApi } from '../../api/notification';
import type { Notification, NotificationSummary } from '../../api/notification';
import { useNavigate } from 'react-router-dom';
import styles from './notificationList.module.css';

const { Text } = Typography;

interface NotificationListProps {
  recipientId: string;
}

const NotificationList: React.FC<NotificationListProps> = ({ recipientId }) => {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [summary, setSummary] = useState<NotificationSummary | null>(null);
  const [loading, setLoading] = useState(false);
  const [filter, setFilter] = useState<string>('ALL');
  const navigate = useNavigate();

  // 加载通知列表
  const loadNotifications = async () => {
    setLoading(true);
    try {
      const [notificationsData, summaryData] = await Promise.all([
        notificationApi.fetchNotifications({ recipientId }),
        notificationApi.fetchNotificationSummary(recipientId)
      ]);
      setNotifications(notificationsData);
      setSummary(summaryData);
    } catch (error) {
      message.error('加载通知失败');
      console.error('Failed to load notifications:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadNotifications();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [recipientId]);

  // 标记为已读
  const handleMarkAsRead = async (notificationCode: string) => {
    try {
      await notificationApi.markAsRead(notificationCode);
      message.success('已标记为已读');
      loadNotifications();
    } catch {
      message.error('标记失败');
    }
  };

  // 批量标记为已读
  const handleBatchMarkAsRead = async () => {
    const unreadCodes = notifications
      .filter(n => n.status === 'UNREAD')
      .map(n => n.notificationCode);

    if (unreadCodes.length === 0) {
      message.info('没有未读通知');
      return;
    }

    try {
      await notificationApi.batchMarkAsRead(unreadCodes);
      message.success(`已标记 ${unreadCodes.length} 条通知为已读`);
      loadNotifications();
    } catch {
      message.error('批量标记失败');
    }
  };

  // 归档通知
  const handleArchive = async (notificationCode: string) => {
    try {
      await notificationApi.archiveNotification(notificationCode);
      message.success('已归档');
      loadNotifications();
    } catch {
      message.error('归档失败');
    }
  };

  // 点击通知
  const handleNotificationClick = (notification: Notification) => {
    // 标记为已读
    if (notification.status === 'UNREAD') {
      notificationApi.markAsRead(notification.notificationCode);
    }
    
    // 跳转到相关业务页面
    if (notification.businessUrl) {
      navigate(notification.businessUrl);
    }
  };

  // 获取优先级颜色
  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'URGENT': return 'red';
      case 'HIGH': return 'orange';
      case 'NORMAL': return 'blue';
      case 'LOW': return 'green';
      default: return 'default';
    }
  };

  // 获取通知类型标签
  const getTypeTag = (type: string) => {
    switch (type) {
      case 'SYSTEM': return <Tag color="purple">系统</Tag>;
      case 'WORKFLOW': return <Tag color="cyan">工作流</Tag>;
      case 'ALERT': return <Tag color="red">告警</Tag>;
      case 'REMINDER': return <Tag color="gold">提醒</Tag>;
      default: return <Tag>{type}</Tag>;
    }
  };

  // 过滤通知
  const filteredNotifications = notifications.filter(n => {
    if (filter === 'ALL') return true;
    if (filter === 'UNREAD') return n.status === 'UNREAD';
    if (filter === 'READ') return n.status === 'READ';
    if (filter === 'ARCHIVED') return n.status === 'ARCHIVED';
    return true;
  });

  // 过滤菜单
  const filterMenu = (
    <Menu onClick={({ key }) => setFilter(key)}>
      <Menu.Item key="ALL">全部</Menu.Item>
      <Menu.Item key="UNREAD">未读</Menu.Item>
      <Menu.Item key="READ">已读</Menu.Item>
      <Menu.Item key="ARCHIVED">已归档</Menu.Item>
    </Menu>
  );

  return (
    <div className={styles.page}>
      <Card
        title={
          <Space>
            <BellOutlined />
            <span>通知中心</span>
            {summary && summary.unread > 0 && (
              <Badge count={summary.unread} className={styles.unreadCountBadge} />
            )}
          </Space>
        }
        extra={
          <Space>
            <Dropdown overlay={filterMenu} trigger={['click']}>
              <Button icon={<FilterOutlined />}>
                {{ ALL: '全部', UNREAD: '未读', READ: '已读', ARCHIVED: '已归档' }[filter] ?? '全部'}
              </Button>
            </Dropdown>
            <Button 
              type="primary"
              icon={<CheckOutlined />} 
              onClick={handleBatchMarkAsRead}
              disabled={!summary || summary.unread === 0}
            >
              全部已读
            </Button>
          </Space>
        }
      >
        {/* 统计卡片 */}
        {summary && (
          <div className={styles.statsContainer}>
            <Card size="small" className={styles.statCard}>
              <div className={styles.statContent}>
                <div className={styles.statNumber}>{summary.total}</div>
                <div>全部通知</div>
              </div>
            </Card>
            <Card size="small" className={styles.statCard}>
              <div className={styles.statContent}>
                <div className={styles.statNumberDanger}>{summary.unread}</div>
                <div>未读</div>
              </div>
            </Card>
            <Card size="small" className={styles.statCard}>
              <div className={styles.statContent}>
                <div className={styles.statNumberSuccess}>{summary.read}</div>
                <div>已读</div>
              </div>
            </Card>
            <Card size="small" className={styles.statCard}>
              <div className={styles.statContent}>
                <div className={styles.statNumberSecondary}>{summary.archived}</div>
                <div>已归档</div>
              </div>
            </Card>
          </div>
        )}

        {/* 通知列表 */}
        <Spin spinning={loading}>
          {filteredNotifications.length === 0 ? (
            <Empty description="暂无通知" />
          ) : (
            <List
              dataSource={filteredNotifications}
              renderItem={(notification) => (
                <List.Item
                  key={notification.notificationCode}
                  className={notification.status === 'UNREAD' ? styles.notificationItemUnread : styles.notificationItem}
                  onClick={() => handleNotificationClick(notification)}
                  actions={[
                    notification.status === 'UNREAD' && (
                      <Button 
                        type="link" 
                        size="small"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleMarkAsRead(notification.notificationCode);
                        }}
                      >
                        标记已读
                      </Button>
                    ),
                    <Button 
                      type="link" 
                      size="small"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleArchive(notification.notificationCode);
                      }}
                    >
                      归档
                    </Button>
                  ].filter(Boolean)}
                >
                  <List.Item.Meta
                    title={
                      <Space>
                        {notification.status === 'UNREAD' && (
                          <Badge status="processing" />
                        )}
                        <Text strong>{notification.title}</Text>
                        {getTypeTag(notification.notificationType)}
                        <Tag color={getPriorityColor(notification.priority)}>
                          {notification.priority}
                        </Tag>
                      </Space>
                    }
                    description={
                      <div>
                        <div className={styles.notificationContent}>{notification.content}</div>
                        <Space>
                          <Text type="secondary">
                            {notification.senderName || '系统'}
                          </Text>
                          <Text type="secondary">
                            {new Date(notification.createdTime).toLocaleString()}
                          </Text>
                        </Space>
                      </div>
                    }
                  />
                </List.Item>
              )}
            />
          )}
        </Spin>
      </Card>
    </div>
  );
};

export default NotificationList;
