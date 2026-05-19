import React, { useState, useEffect } from 'react';
import { 
  Card, Table, Button, Space, Typography, Tag, Modal, Form, Input, 
  Select, message, Tooltip, Popconfirm, Descriptions 
} from 'antd';
import { 
  PlusOutlined, 
  DeleteOutlined, 
  CopyOutlined, 
  KeyOutlined,
  ApiOutlined,
  ExclamationCircleOutlined 
} from '@ant-design/icons';

const { Title, Text, Paragraph } = Typography;
const { Option } = Select;

interface ServiceAccount {
  accountCode: string;
  accountName: string;
  accountType: string;
  clientId: string;
  status: string;
  rateLimit: number;
  createdTime: string;
  lastUsedTime?: string;
}

const ServiceAccountManagement: React.FC = () => {
  const [accounts, setAccounts] = useState<ServiceAccount[]>([]);
  const [loading, setLoading] = useState(false);
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [secretModalVisible, setSecretModalVisible] = useState(false);
  const [newSecret, setNewSecret] = useState<string | null>(null);
  const [form] = Form.useForm();

  // 加载服务账号列表
  const loadAccounts = async () => {
    setLoading(true);
    try {
      // 实际项目中应该调用API
      const mockAccounts: ServiceAccount[] = [
        {
          accountCode: 'SA-001',
          accountName: 'API 集成账号',
          accountType: 'API_CLIENT',
          clientId: 'client-abc123',
          status: 'ACTIVE',
          rateLimit: 1000,
          createdTime: '2026-05-15T10:00:00',
          lastUsedTime: '2026-05-20T15:30:00'
        },
        {
          accountCode: 'SA-002',
          accountName: 'Webhook 回调',
          accountType: 'WEBHOOK',
          clientId: 'client-def456',
          status: 'ACTIVE',
          rateLimit: 500,
          createdTime: '2026-05-18T14:00:00'
        }
      ];
      setAccounts(mockAccounts);
    } catch (error) {
      message.error('加载服务账号失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAccounts();
  }, []);

  // 创建服务账号
  const handleCreate = async () => {
    try {
      const values = await form.validateFields();
      
      // 实际项目中应该调用API
      const newAccount: ServiceAccount = {
        accountCode: 'SA-' + Date.now(),
        accountName: values.accountName,
        accountType: values.accountType,
        clientId: 'client-' + Math.random().toString(36).substr(2, 8),
        status: 'ACTIVE',
        rateLimit: values.rateLimit || 1000,
        createdTime: new Date().toISOString()
      };

      // 模拟返回客户端密钥
      const secret = 'secret-' + Math.random().toString(36).substr(2, 16);
      setNewSecret(secret);
      setSecretModalVisible(true);
      
      setAccounts([...accounts, newAccount]);
      setCreateModalVisible(false);
      form.resetFields();
      message.success('服务账号创建成功');
    } catch (error) {
      message.error('创建失败');
    }
  };

  // 吊销服务账号
  const handleRevoke = async (accountCode: string) => {
    try {
      // 实际项目中应该调用API
      setAccounts(accounts.map(acc => 
        acc.accountCode === accountCode 
          ? { ...acc, status: 'REVOKED' }
          : acc
      ));
      message.success('服务账号已吊销');
    } catch (error) {
      message.error('吊销失败');
    }
  };

  // 复制到剪贴板
  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    message.success('已复制到剪贴板');
  };

  // 表格列定义
  const columns = [
    {
      title: '账号名称',
      dataIndex: 'accountName',
      key: 'accountName',
      render: (text: string, record: ServiceAccount) => (
        <Space>
          <ApiOutlined />
          <Text strong>{text}</Text>
        </Space>
      )
    },
    {
      title: '账号类型',
      dataIndex: 'accountType',
      key: 'accountType',
      render: (type: string) => {
        const typeMap: Record<string, { color: string; text: string }> = {
          'API_CLIENT': { color: 'blue', text: 'API 客户端' },
          'SERVICE_USER': { color: 'green', text: '服务用户' },
          'WEBHOOK': { color: 'purple', text: 'Webhook' }
        };
        const config = typeMap[type] || { color: 'default', text: type };
        return <Tag color={config.color}>{config.text}</Tag>;
      }
    },
    {
      title: 'Client ID',
      dataIndex: 'clientId',
      key: 'clientId',
      render: (text: string) => (
        <Space>
          <Text code>{text}</Text>
          <Tooltip title="复制">
            <Button 
              type="text" 
              size="small" 
              icon={<CopyOutlined />}
              onClick={() => copyToClipboard(text)}
            />
          </Tooltip>
        </Space>
      )
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'ACTIVE' ? 'green' : 'red'}>
          {status === 'ACTIVE' ? '活跃' : '已吊销'}
        </Tag>
      )
    },
    {
      title: '速率限制',
      dataIndex: 'rateLimit',
      key: 'rateLimit',
      render: (limit: number) => `${limit} 次/分钟`
    },
    {
      title: '最后使用',
      dataIndex: 'lastUsedTime',
      key: 'lastUsedTime',
      render: (time: string) => time ? new Date(time).toLocaleString() : '-'
    },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: ServiceAccount) => (
        <Space>
          {record.status === 'ACTIVE' && (
            <Popconfirm
              title="确定要吊销此服务账号吗？"
              description="吊销后将无法恢复，所有使用此账号的集成将立即失效。"
              onConfirm={() => handleRevoke(record.accountCode)}
              okText="确定"
              cancelText="取消"
            >
              <Button 
                type="link" 
                danger 
                icon={<DeleteOutlined />}
              >
                吊销
              </Button>
            </Popconfirm>
          )}
        </Space>
      )
    }
  ];

  return (
    <div style={{ padding: 24 }}>
      <Card
        title={
          <Space>
            <KeyOutlined />
            <span>服务账号管理</span>
          </Space>
        }
        extra={
          <Button 
            type="primary" 
            icon={<PlusOutlined />}
            onClick={() => setCreateModalVisible(true)}
          >
            创建服务账号
          </Button>
        }
      >
        <Paragraph type="secondary" style={{ marginBottom: 16 }}>
          服务账号用于 API 集成和自动化任务。每个服务账号都有独立的 Client ID 和 Client Secret。
        </Paragraph>

        <Table
          columns={columns}
          dataSource={accounts}
          rowKey="accountCode"
          loading={loading}
        />
      </Card>

      {/* 创建服务账号弹窗 */}
      <Modal
        title="创建服务账号"
        open={createModalVisible}
        onOk={handleCreate}
        onCancel={() => {
          setCreateModalVisible(false);
          form.resetFields();
        }}
        okText="创建"
        cancelText="取消"
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="accountName"
            label="账号名称"
            rules={[{ required: true, message: '请输入账号名称' }]}
          >
            <Input placeholder="请输入服务账号名称" />
          </Form.Item>

          <Form.Item
            name="accountType"
            label="账号类型"
            rules={[{ required: true, message: '请选择账号类型' }]}
          >
            <Select placeholder="请选择账号类型">
              <Option value="API_CLIENT">API 客户端</Option>
              <Option value="SERVICE_USER">服务用户</Option>
              <Option value="WEBHOOK">Webhook</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="rateLimit"
            label="速率限制（次/分钟）"
            initialValue={1000}
          >
            <Select>
              <Option value={100}>100 次/分钟</Option>
              <Option value={500}>500 次/分钟</Option>
              <Option value={1000}>1000 次/分钟</Option>
              <Option value={5000}>5000 次/分钟</Option>
              <Option value={10000}>10000 次/分钟</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="description"
            label="描述"
          >
            <Input.TextArea rows={3} placeholder="请输入服务账号用途描述" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 显示客户端密钥弹窗 */}
      <Modal
        title={
          <Space>
            <ExclamationCircleOutlined style={{ color: '#faad14' }} />
            <span>客户端密钥</span>
          </Space>
        }
        open={secretModalVisible}
        onOk={() => setSecretModalVisible(false)}
        onCancel={() => setSecretModalVisible(false)}
        okText="我已保存"
        cancelButtonProps={{ style: { display: 'none' } }}
      >
        <div style={{ marginBottom: 16 }}>
          <Paragraph>
            <Text strong>请立即复制并安全保存客户端密钥！</Text>
          </Paragraph>
          <Paragraph type="danger">
            此密钥仅显示一次，关闭后将无法再次查看。
          </Paragraph>
        </div>

        <Descriptions bordered column={1}>
          <Descriptions.Item label="Client ID">
            <Text code>client-xxxx</Text>
          </Descriptions.Item>
          <Descriptions.Item label="Client Secret">
            <Space>
              <Text code>{newSecret}</Text>
              <Button 
                type="link" 
                icon={<CopyOutlined />}
                onClick={() => copyToClipboard(newSecret || '')}
              >
                复制
              </Button>
            </Space>
          </Descriptions.Item>
        </Descriptions>
      </Modal>
    </div>
  );
};

export default ServiceAccountManagement;