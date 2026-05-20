import React, { useEffect, useState } from 'react';
import { Card, Table, Button, Input, Modal, Form, Tag, Space, message, Popconfirm, Alert } from 'antd';
import {
  listBindingsByUser,
  bindIdentity,
  unbindIdentity,
  mergeBindings,
  findConflicts,
  type IdentityBinding,
  type BindingConflict,
} from '../api/identityBinding';

const IdentityBindingManagement: React.FC = () => {
  const [userId, setUserId] = useState<string>('');
  const [bindings, setBindings] = useState<IdentityBinding[]>([]);
  const [conflicts, setConflicts] = useState<BindingConflict[]>([]);
  const [bindModalVisible, setBindModalVisible] = useState(false);
  const [mergeModalVisible, setMergeModalVisible] = useState(false);
  const [loading, setLoading] = useState(false);
  const [bindForm] = Form.useForm();
  const [mergeForm] = Form.useForm();

  useEffect(() => {
    loadConflicts();
  }, []);

  const loadConflicts = async () => {
    try {
      const data = await findConflicts();
      setConflicts(data);
    } catch {
      // Ignore errors - may not have permissions
    }
  };

  const loadBindings = async () => {
    if (!userId) {
      message.warning('请输入用户 ID');
      return;
    }
    try {
      setLoading(true);
      const data = await listBindingsByUser(Number(userId));
      setBindings(data);
    } catch (err: unknown) {
      message.error(err instanceof Error ? err.message : '加载绑定列表失败');
    } finally {
      setLoading(false);
    }
  };

  const handleBind = async (values: Record<string, string>) => {
    try {
      await bindIdentity({
        userId: Number(userId),
        providerId: Number(values.providerId),
        externalSubject: values.externalSubject,
        externalDisplayName: values.externalDisplayName,
      });
      message.success('绑定成功');
      setBindModalVisible(false);
      bindForm.resetFields();
      loadBindings();
    } catch (err: unknown) {
      message.error(err instanceof Error ? err.message : '绑定失败');
    }
  };

  const handleUnbind = async (bindingId: number) => {
    try {
      await unbindIdentity(bindingId);
      message.success('解绑成功');
      loadBindings();
    } catch (err: unknown) {
      message.error(err instanceof Error ? err.message : '解绑失败');
    }
  };

  const handleMerge = async (values: Record<string, string>) => {
    try {
      const result = await mergeBindings({
        sourceUserId: Number(values.sourceUserId),
        targetUserId: Number(values.targetUserId),
      });
      message.success(`合并完成：转移 ${result.transferredCount} 个绑定，冲突 ${result.conflictCount} 个`);
      setMergeModalVisible(false);
      mergeForm.resetFields();
      loadBindings();
      loadConflicts();
    } catch (err: unknown) {
      message.error(err instanceof Error ? err.message : '合并失败');
    }
  };

  const bindingColumns = [
    { title: '绑定 ID', dataIndex: 'id', key: 'id' },
    { title: '身份源 ID', dataIndex: 'providerId', key: 'providerId' },
    { title: '外部标识', dataIndex: 'externalSubject', key: 'externalSubject' },
    { title: '外部显示名', dataIndex: 'externalDisplayName', key: 'externalDisplayName' },
    {
      title: '状态',
      dataIndex: 'bindingStatus',
      key: 'bindingStatus',
      render: (status: string) => {
        const colorMap: Record<string, string> = {
          ACTIVE: 'green',
          DETACHED: 'red',
          MERGED: 'orange',
        };
        return <Tag color={colorMap[status] || 'default'}>{status}</Tag>;
      },
    },
    { title: '验证时间', dataIndex: 'lastVerifiedTime', key: 'lastVerifiedTime' },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: IdentityBinding) =>
        record.bindingStatus === 'ACTIVE' ? (
          <Popconfirm title="确认解绑？" onConfirm={() => handleUnbind(record.id)}>
            <Button type="link" danger>解绑</Button>
          </Popconfirm>
        ) : null,
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <h2>身份绑定管理</h2>

      {conflicts.length > 0 && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          message={`发现 ${conflicts.length} 个身份绑定冲突（同一外部身份绑定到多个用户）`}
          description={
            <Table
              size="small"
              pagination={false}
              dataSource={conflicts}
              rowKey={(r) => `${r.providerId}-${r.externalSubject}`}
              columns={[
                { title: '身份源 ID', dataIndex: 'providerId' },
                { title: '外部标识', dataIndex: 'externalSubject' },
                { title: '绑定用户数', dataIndex: 'userCount' },
              ]}
            />
          }
        />
      )}

      <Card title="用户绑定查询" style={{ marginBottom: 16 }}>
        <Space>
          <Input
            placeholder="输入用户 ID"
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            style={{ width: 200 }}
            onPressEnter={loadBindings}
          />
          <Button type="primary" onClick={loadBindings} loading={loading}>查询</Button>
          <Button onClick={() => setBindModalVisible(true)} disabled={!userId}>绑定身份</Button>
          <Button onClick={() => setMergeModalVisible(true)}>合并用户</Button>
        </Space>

        <Table
          style={{ marginTop: 16 }}
          columns={bindingColumns}
          dataSource={bindings}
          rowKey="id"
          loading={loading}
          pagination={false}
        />
      </Card>

      <Modal
        title="绑定外部身份"
        open={bindModalVisible}
        onCancel={() => setBindModalVisible(false)}
        onOk={() => bindForm.submit()}
      >
        <Form form={bindForm} onFinish={handleBind} layout="vertical">
          <Form.Item name="providerId" label="身份源 ID" rules={[{ required: true }]}>
            <Input type="number" />
          </Form.Item>
          <Form.Item name="externalSubject" label="外部标识" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="externalDisplayName" label="外部显示名">
            <Input />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="合并用户绑定"
        open={mergeModalVisible}
        onCancel={() => setMergeModalVisible(false)}
        onOk={() => mergeForm.submit()}
      >
        <Form form={mergeForm} onFinish={handleMerge} layout="vertical">
          <Form.Item name="sourceUserId" label="源用户 ID（将被合并）" rules={[{ required: true }]}>
            <Input type="number" />
          </Form.Item>
          <Form.Item name="targetUserId" label="目标用户 ID（保留）" rules={[{ required: true }]}>
            <Input type="number" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default IdentityBindingManagement;
