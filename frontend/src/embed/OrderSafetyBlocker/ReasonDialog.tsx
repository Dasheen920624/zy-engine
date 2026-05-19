import { useState } from 'react';
import { Modal, Form, Input, Checkbox, Button, Typography, Space } from 'antd';
import type { ReasonDialogProps } from './OrderSafetyBlocker.types';

const { TextArea } = Input;
const { Text } = Typography;

export default function ReasonDialog({
  open,
  onConfirm,
  onCancel,
  loading = false,
}: ReasonDialogProps) {
  const [form] = Form.useForm();
  const [reasonLength, setReasonLength] = useState(0);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      if (values.reason.length < 20) {
        return;
      }
      onConfirm({
        reason: values.reason,
        informedConsent: values.informedConsent || false,
        familyNotified: values.familyNotified || false,
      });
    } catch (error) {
      // Validation failed
    }
  };

  const handleCancel = () => {
    form.resetFields();
    setReasonLength(0);
    onCancel();
  };

  const handleReasonChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setReasonLength(e.target.value.length);
  };

  return (
    <Modal
      title="填写坚持使用理由"
      open={open}
      onCancel={handleCancel}
      footer={null}
      width={520}
      maskClosable={false}
      destroyOnClose
    >
      <div style={{ marginBottom: 16 }}>
        <Text type="secondary">
          请说明您评估了哪些因素后决定坚持使用该医嘱：
        </Text>
      </div>

      <Form form={form} layout="vertical">
        <Form.Item
          name="reason"
          label="坚持使用理由"
          rules={[
            { required: true, message: '请填写坚持使用理由' },
            {
              validator: (_, value) => {
                if (value && value.length < 20) {
                  return Promise.reject('理由至少需要20个字');
                }
                return Promise.resolve();
              },
            },
          ]}
        >
          <TextArea
            rows={4}
            placeholder="请详细说明您对出血/血栓风险的评估，以及坚持使用的临床依据..."
            onChange={handleReasonChange}
            showCount
            maxLength={500}
          />
        </Form.Item>

        <div style={{ marginBottom: 8 }}>
          <Text type="secondary" style={{ fontSize: 12 }}>
            当前字数：{reasonLength}/20（最少需要20个字）
          </Text>
        </div>

        <Form.Item
          name="informedConsent"
          valuePropName="checked"
          rules={[
            {
              validator: (_, value) => {
                if (!value) {
                  return Promise.reject('必须勾选知情同意确认');
                }
                return Promise.resolve();
              },
            },
          ]}
        >
          <Checkbox>已告知患者及家属出血风险，并签署知情同意书</Checkbox>
        </Form.Item>

        <Form.Item
          name="familyNotified"
          valuePropName="checked"
        >
          <Checkbox>已与家属充分沟通病情及治疗方案</Checkbox>
        </Form.Item>

        <Form.Item>
          <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
            <Button onClick={handleCancel}>取消</Button>
            <Button
              type="primary"
              danger
              onClick={handleOk}
              loading={loading}
            >
              确认坚持使用并记录
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </Modal>
  );
}