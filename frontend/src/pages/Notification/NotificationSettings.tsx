import React, { useState, useEffect } from 'react';
import { Card, Form, Switch, Select, Button, Space, Typography, message, Divider, List, Tag } from 'antd';
import { SaveOutlined, BellOutlined, MailOutlined, MessageOutlined, WechatOutlined } from '@ant-design/icons';

const { Title, Text } = Typography;
const { Option } = Select;

interface NotificationSetting {
  notificationType: string;
  channels: {
    IN_APP: boolean;
    EMAIL: boolean;
    SMS: boolean;
    WECHAT: boolean;
  };
}

const NotificationSettings: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
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

  // 加载设置
  const loadSettings = async () => {
    // 实际项目中应该从后端加载用户的通知设置
    // 这里使用默认设置
    form.setFieldsValue({ settings });
  };

  useEffect(() => {
    loadSettings();
  }, []);

  // 保存设置
  const handleSave = async () => {
    setLoading(true);
    try {
      const values = await form.validateFields();
      // 实际项目中应该调用后端API保存设置
      console.log('Saving notification settings:', values.settings);
      message.success('通知设置已保存');
    } catch (error) {
      message.error('保存失败');
    } finally {
      setLoading(false);
    }
  };

  // 切换渠道
  const handleChannelChange = (typeIndex: number, channel: string, checked: boolean) => {
    const newSettings = [...settings];
    newSettings[typeIndex].channels[channel as keyof typeof newSettings[0]['channels']] = checked;
    setSettings(newSettings);
    form.setFieldsValue({ settings: newSettings });
  };

  return (
    <div style={{ padding: 24 }}>
      <Card
        title={
          <Space>
            <BellOutlined />
            <span>通知设置</span>
          </Space>
        }
        extra={
          <Button 
            type="primary" 
            icon={<SaveOutlined />} 
            onClick={handleSave}
            loading={loading}
          >
            保存设置
          </Button>
        }
      >
        <Form form={form} layout="vertical">
          <Form.List name="settings">
            {(fields, { add, remove }) => (
              <>
                {fields.map((field, index) => (
                  <div key={field.key}>
                    <Card 
                      size="small" 
                      style={{ marginBottom: 16 }}
                      title={
                        <Space>
                          <Tag color={index === 0 ? 'purple' : index === 1 ? 'cyan' : index === 2 ? 'red' : 'gold'}>
                            {getTypeName(settings[index].notificationType)}
                          </Tag>
                          <Text>通知渠道设置</Text>
                        </Space>
                      }
                    >
                      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 16 }}>
                        {Object.entries(settings[index].channels).map(([channel, enabled]) => (
                          <div 
                            key={channel}
                            style={{ 
                              display: 'flex', 
                              alignItems: 'center', 
                              gap: 8,
                              padding: '8px 16px',
                              backgroundColor: '#fafafa',
                              borderRadius: 8,
                              minWidth: 150
                            }}
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
                  </div>
                ))}
              </>
            )}
          </Form.List>

          <Divider />

          <div style={{ marginTop: 16 }}>
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

          <div style={{ marginTop: 16 }}>
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
                    {getChannelIcon(item.channel === '应用内通知' ? 'IN_APP' : 
                                   item.channel === '邮件' ? 'EMAIL' : 
                                   item.channel === '短信' ? 'SMS' : 'WECHAT')}
                    <Text strong>{item.channel}：</Text>
                    <Text type="secondary">{item.desc}</Text>
                  </Space>
                </List.Item>
              )}
            />
          </div>
        </Form>
      </Card>
    </div>
  );
};

export default NotificationSettings;