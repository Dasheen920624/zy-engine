import React, { useState, useEffect, useCallback } from 'react';
import { Card, Form, Switch, Button, Space, Typography, message, Divider, List, Tag, Spin } from 'antd';
import { SaveOutlined, BellOutlined, MailOutlined, MessageOutlined, WechatOutlined } from '@ant-design/icons';
import { notificationApi } from '../../api/notification';
import type { NotificationSubscription } from '../../api/notification';
import styles from './notificationSettings.module.css';

const { Title, Text } = Typography;

interface NotificationSetting {
  notificationType: string;
  channels: {
    IN_APP: boolean;
    EMAIL: boolean;
    SMS: boolean;
    WECHAT: boolean;
  };
}

const typeColorMap: Record<string, string> = {
  SYSTEM: 'purple',
  WORKFLOW: 'cyan',
  ALERT: 'red',
  REMINDER: 'gold',
};

const CHANNELS = ['IN_APP', 'EMAIL', 'SMS', 'WECHAT'] as const;

const NotificationSettings: React.FC<{ userId?: string }> = ({ userId = 'current-user' }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [settings, setSettings] = useState<NotificationSetting[]>([
    {
      notificationType: 'SYSTEM',
      channels: { IN_APP: true, EMAIL: false, SMS: false, WECHAT: false }
    },
    {
      notificationType: 'WORKFLOW',
      channels: { IN_APP: true, EMAIL: true, SMS: false, WECHAT: true }
    },
    {
      notificationType: 'ALERT',
      channels: { IN_APP: true, EMAIL: true, SMS: true, WECHAT: true }
    },
    {
      notificationType: 'REMINDER',
      channels: { IN_APP: true, EMAIL: true, SMS: false, WECHAT: false }
    }
  ]);

  // 获取通知类型名称
  const getTypeName = (type: string) => {
    switch (type) {
      case 'SYSTEM': return '系统通知';
      case 'WORKFLOW': return '工作流通知';
      case 'ALERT': return '告警通知';
      case 'REMINDER': return '提醒通知';
      default: return type;
    }
  };

  // 获取渠道图标
  const getChannelIcon = (channel: string) => {
    switch (channel) {
      case 'IN_APP': return <BellOutlined />;
      case 'EMAIL': return <MailOutlined />;
      case 'SMS': return <MessageOutlined />;
      case 'WECHAT': return <WechatOutlined />;
      default: return null;
    }
  };

  // 获取渠道名称
  const getChannelName = (channel: string) => {
    switch (channel) {
      case 'IN_APP': return '应用内通知';
      case 'EMAIL': return '邮件';
      case 'SMS': return '短信';
      case 'WECHAT': return '企业微信';
      default: return channel;
    }
  };

  // 从后端加载订阅设置
  const loadSettings = useCallback(async () => {
    setLoading(true);
    try {
      const subscriptions = await notificationApi.fetchSubscriptions(userId);

      if (subscriptions && subscriptions.length > 0) {
        // 将后端订阅数据映射到本地状态
        const newSettings = settings.map(setting => {
          const updatedChannels = { ...setting.channels };
          for (const channel of CHANNELS) {
            const sub = subscriptions.find(
              s => s.notificationType === setting.notificationType && s.channel === channel
            );
            if (sub) {
              updatedChannels[channel] = sub.enabled;
            }
          }
          return { ...setting, channels: updatedChannels };
        });
        setSettings(newSettings);
      }
    } catch {
      // 后端不可用时使用默认设置
    } finally {
      setLoading(false);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userId]);

  useEffect(() => {
    loadSettings();
  }, [loadSettings]);

  // 保存设置到后端
  const handleSave = async () => {
    setSaving(true);
    try {
      // 构建订阅列表
      const subscriptions: NotificationSubscription[] = [];
      for (const setting of settings) {
        for (const channel of CHANNELS) {
          subscriptions.push({
            userId,
            notificationType: setting.notificationType as NotificationSubscription['notificationType'],
            channel: channel as NotificationSubscription['channel'],
            enabled: setting.channels[channel]
          });
        }
      }

      await notificationApi.batchSaveSubscriptions(subscriptions);
      message.success('通知设置已保存');
    } catch {
      message.error('保存失败');
    } finally {
      setSaving(false);
    }
  };

  // 切换渠道
  const handleChannelChange = (typeIndex: number, channel: string, checked: boolean) => {
    const newSettings = [...settings];
    newSettings[typeIndex].channels[channel as keyof typeof newSettings[0]['channels']] = checked;
    setSettings(newSettings);
  };

  return (
    <div className={styles.page}>
      <Card
        title={
          <Space>
            <BellOutlined />
            <span>通知设置</span>
          </Space>
        }
        extra={
          <Space>
            <Button onClick={loadSettings} loading={loading}>
              刷新
            </Button>
            <Button
              type="primary"
              icon={<SaveOutlined />}
              onClick={handleSave}
              loading={saving}
            >
              保存设置
            </Button>
          </Space>
        }
      >
        <Spin spinning={loading}>
          <Form form={form} layout="vertical">
            {settings.map((setting, index) => (
              <Card
                key={setting.notificationType}
                size="small"
                className={styles.settingsCard}
                title={
                  <Space>
                    <Tag color={typeColorMap[setting.notificationType] ?? 'default'}>
                      {getTypeName(setting.notificationType)}
                    </Tag>
                    <Text>通知渠道设置</Text>
                  </Space>
                }
              >
                <div className={styles.channelContainer}>
                  {Object.entries(setting.channels).map(([channel, enabled]) => (
                    <div
                      key={channel}
                      className={styles.channelItem}
                    >
                      {getChannelIcon(channel)}
                      <Text>{getChannelName(channel)}</Text>
                      <Switch
                        checked={enabled}
                        onChange={(checked) => handleChannelChange(index, channel, checked)}
                      />
                    </div>
                  ))}
                </div>
              </Card>
            ))}

            <Divider />

            <div className={styles.section}>
              <Title level={5}>通知说明</Title>
              <List
                size="small"
                bordered
                dataSource={[
                  { type: '系统通知', desc: '系统维护、版本更新、安全提醒等' },
                  { type: '工作流通知', desc: '待办任务、审批提醒、任务分配等' },
                  { type: '告警通知', desc: '业务异常、质控预警、安全告警等' },
                  { type: '提醒通知', desc: '定时提醒、过期提醒、超时提醒等' }
                ]}
                renderItem={(item) => (
                  <List.Item>
                    <Text strong>{item.type}：</Text>
                    <Text type="secondary">{item.desc}</Text>
                  </List.Item>
                )}
              />
            </div>

            <div className={styles.section}>
              <Title level={5}>渠道说明</Title>
              <List
                size="small"
                bordered
                dataSource={[
                  { channel: '应用内通知', desc: '在应用内显示通知消息，支持实时推送' },
                  { channel: '邮件', desc: '发送邮件通知，适合重要事项和定时汇总' },
                  { channel: '短信', desc: '发送短信通知，适合紧急告警和重要提醒' },
                  { channel: '企业微信', desc: '通过企业微信推送，适合团队协作通知' }
                ]}
                renderItem={(item) => (
                  <List.Item>
                    <Space>
                      {getChannelIcon(
                        { '应用内通知': 'IN_APP', '邮件': 'EMAIL', '短信': 'SMS', '企业微信': 'WECHAT' }[item.channel] ?? 'IN_APP'
                      )}
                      <Text strong>{item.channel}：</Text>
                      <Text type="secondary">{item.desc}</Text>
                    </Space>
                  </List.Item>
                )}
              />
            </div>
          </Form>
        </Spin>
      </Card>
    </div>
  );
};

export default NotificationSettings;
