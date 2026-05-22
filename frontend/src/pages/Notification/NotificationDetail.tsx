import React, { useState, useEffect } from 'react';
import { Card, Descriptions, Tag, Button, Space, Typography, Spin, message, Divider } from 'antd';
import { ArrowLeftOutlined, CheckOutlined, DeleteOutlined, ExportOutlined } from '@ant-design/icons';
import { notificationApi } from '../../api/notification';
import type { Notification } from '../../api/notification';
import { useNavigate, useParams } from 'react-router-dom';
import styles from './notificationDetail.module.css';

const { Title, Paragraph, Text } = Typography;

const NotificationDetail: React.FC = () => {
  const { notificationCode } = useParams<{ notificationCode: string }>();
  const [notification, setNotification] = useState<Notification | null>(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  // 加载通知详情
  const loadNotification = async () => {
    if (!notificationCode) return;
    
    setLoading(true);
    try {
      const data = await notificationApi.fetchNotification(notificationCode);
      setNotification(data);
      
      // 如果未读，自动标记为已读
      if (data.status === 'UNREAD') {
        await notificationApi.markAsRead(notificationCode);
        setNotification({ ...data, status: 'READ', readTime: new Date().toISOString() });
      }
    } catch (error) {
      message.error('加载通知失败');
      console.error('Failed to load notification:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadNotification();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [notificationCode]);

  // 标记为已读
  const handleMarkAsRead = async () => {
    if (!notificationCode) return;
    
    try {
      await notificationApi.markAsRead(notificationCode);
      message.success('已标记为已读');
      loadNotification();
    } catch {
      message.error('标记失败');
    }
  };

  // 归档通知
  const handleArchive = async () => {
    if (!notificationCode) return;

    try {
      await notificationApi.archiveNotification(notificationCode);
      message.success('已归档');
      navigate('/notifications');
    } catch {
      message.error('归档失败');
    }
  };

  // 跳转到相关业务
  const handleGoToBusiness = () => {
    if (notification?.businessUrl) {
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

  // 获取状态标签
  const getStatusTag = (status: string) => {
    switch (status) {
      case 'UNREAD': return <Tag color="red">未读</Tag>;
      case 'READ': return <Tag color="green">已读</Tag>;
      case 'ARCHIVED': return <Tag color="default">已归档</Tag>;
      default: return <Tag>{status}</Tag>;
    }
  };

  if (loading) {
    return (
      <div className={styles.loadingContainer}>
        <Spin size="large" />
      </div>
    );
  }

  if (!notification) {
    return (
      <div className={styles.page}>
        <Card>
          <div className={styles.emptyContainer}>
            <Title level={4}>通知不存在</Title>
            <Button onClick={() => navigate('/notifications')}>返回通知列表</Button>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <Card
        title={
          <Space>
            <Button 
              icon={<ArrowLeftOutlined />} 
              onClick={() => navigate('/notifications')}
            >
              返回
            </Button>
            <span>通知详情</span>
          </Space>
        }
        extra={
          <Space>
            {notification.status === 'UNREAD' && (
              <Button icon={<CheckOutlined />} onClick={handleMarkAsRead}>
                标记已读
              </Button>
            )}
            <Button icon={<DeleteOutlined />} onClick={handleArchive}>
              归档
            </Button>
            {notification.businessUrl && (
              <Button 
                type="primary" 
                icon={<ExportOutlined />} 
                onClick={handleGoToBusiness}
              >
                查看相关业务
              </Button>
            )}
          </Space>
        }
      >
        <Descriptions column={2} bordered>
          <Descriptions.Item label="标题" span={2}>
            <Title level={4} className={styles.notificationTitle}>{notification.title}</Title>
          </Descriptions.Item>
          <Descriptions.Item label="类型">
            {getTypeTag(notification.notificationType)}
          </Descriptions.Item>
          <Descriptions.Item label="优先级">
            <Tag color={getPriorityColor(notification.priority)}>
              {notification.priority}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="状态">
            {getStatusTag(notification.status)}
          </Descriptions.Item>
          <Descriptions.Item label="渠道">
            <Tag>{notification.channel}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="发送人">
            {notification.senderName || '系统'}
          </Descriptions.Item>
          <Descriptions.Item label="接收人">
            {notification.recipientName || notification.recipientId}
          </Descriptions.Item>
          <Descriptions.Item label="创建时间">
            {new Date(notification.createdTime).toLocaleString()}
          </Descriptions.Item>
          {notification.readTime && (
            <Descriptions.Item label="阅读时间">
              {new Date(notification.readTime).toLocaleString()}
            </Descriptions.Item>
          )}
          {notification.businessType && (
            <Descriptions.Item label="业务类型">
              <Tag>{notification.businessType}</Tag>
            </Descriptions.Item>
          )}
          {notification.businessId && (
            <Descriptions.Item label="业务ID">
              {notification.businessId}
            </Descriptions.Item>
          )}
        </Descriptions>

        <Divider />

        <div className={styles.contentSection}>
          <Title level={5}>通知内容</Title>
          <Card className={styles.contentCard}>
            <Paragraph className={styles.notificationParagraph}>
              {notification.content}
            </Paragraph>
          </Card>
        </div>

        {notification.businessUrl && (
          <div className={styles.contentSection}>
            <Title level={5}>相关业务</Title>
            <Card>
              <Space>
                <Text>业务链接：</Text>
                <Button 
                  type="link" 
                  onClick={handleGoToBusiness}
                  className={styles.linkButton}
                >
                  {notification.businessUrl}
                </Button>
              </Space>
            </Card>
          </div>
        )}
      </Card>
    </div>
  );
};

export default NotificationDetail;
