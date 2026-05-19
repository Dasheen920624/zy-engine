import { useState, useCallback } from 'react';
import { Modal, Form, Input, Checkbox, Button, Typography, Space, Alert } from 'antd';
import { ExclamationCircleOutlined } from '@ant-design/icons';

const { Text } = Typography;
const { TextArea } = Input;

interface ReasonDialogProps {
  /** 是否可见 */
  visible: boolean;
  /** 确认回调（带理由和知情同意） */
  onConfirm: (reason: string, informedConsent: boolean) => void;
  /** 取消回调 */
  onCancel: () => void;
  /** 规则名称 */
  ruleName: string;
  /** 医嘱名称 */
  orderName: string;
}

/**
 * "坚持使用"理由对话框。
 * <p>
 * 要求：
 * <ul>
 *   <li>理由 ≥ 20 字</li>
 *   <li>勾选"已告知患者及家属出血风险"</li>
 *   <li>勾选"已签署知情同意书"</li>
 * </ul>
 * </p>
 */
export default function ReasonDialog({
  visible,
  onConfirm,
  onCancel,
  ruleName,
  orderName,
}: ReasonDialogProps) {
  const [form] = Form.useForm();
  const [reasonLength, setReasonLength] = useState(0);
  const [submitting, setSubmitting] = useState(false);

  const handleOk = useCallback(async () => {
    try {
      setSubmitting(true);
      const values = await form.validateFields();
      onConfirm(values.reason, true);
    } catch {
      // 表单校验失败，不关闭
    } finally {
      setSubmitting(false);
    }
  }, [form, onConfirm]);

  const handleCancel = useCallback(() => {
    form.resetFields();
    setReasonLength(0);
    onCancel();
  }, [form, onCancel]);

  const handleReasonChange = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      setReasonLength(e.target.value.length);
    },
    [],
  );

  return (
    <Modal
      open={visible}
      title={
        <Space>
          <ExclamationCircleOutlined style={{ color: 'var(--mk-warning, #faad14)' }} />
          <span>填写坚持使用理由</span>
        </Space>
      }
      onOk={handleOk}
      onCancel={handleCancel}
      okText="确认坚持使用并记录"
      cancelText="取消"
      okButtonProps={{ loading: submitting, danger: true }}
      width={480}
      maskClosable={false}
    >
      <Alert
        type="info"
        message={
          <span>
            规则 <Text strong>{ruleName}</Text> 拦截了医嘱 <Text strong>{orderName}</Text>
          </span>
        }
        style={{ marginBottom: 16 }}
      />

      <Text type="secondary" style={{ display: 'block', marginBottom: 8 }}>
        请说明您评估了哪些因素后决定坚持使用：
      </Text>

      <Form form={form} layout="vertical">
        <Form.Item
          name="reason"
          rules={[
            { required: true, message: '请填写理由' },
            {
              validator: (_, value) => {
                if (value && value.trim().length < 20) {
                  return Promise.reject(new Error('理由不少于 20 字'));
                }
                return Promise.resolve();
              },
            },
          ]}
          extra={
            <Text
              type={reasonLength < 20 ? 'danger' : 'secondary'}
              style={{ fontSize: 11 }}
            >
              {reasonLength}/20 字（最少 20 字）
            </Text>
          }
        >
          <TextArea
            rows={4}
            placeholder="例如：患者已确诊深静脉血栓，血栓风险高于出血风险，已与家属沟通，签署知情同意。"
            onChange={handleReasonChange}
            maxLength={500}
            showCount
          />
        </Form.Item>

        <Form.Item
          name="informedRisk"
          valuePropName="checked"
          rules={[
            {
              validator: (_, value) =>
                value ? Promise.resolve() : Promise.reject(new Error('必须确认已告知出血风险')),
            },
          ]}
        >
          <Checkbox>已告知患者及家属出血风险</Checkbox>
        </Form.Item>

        <Form.Item
          name="signedConsent"
          valuePropName="checked"
          rules={[
            {
              validator: (_, value) =>
                value ? Promise.resolve() : Promise.reject(new Error('必须确认已签署知情同意书')),
            },
          ]}
        >
          <Checkbox>已签署知情同意书</Checkbox>
        </Form.Item>
      </Form>

      <Alert
        type="warning"
        showIcon
        message="您的决策将写入审计记录，药师审方时会看到您的理由。"
        style={{ marginTop: 8 }}
      />
    </Modal>
  );
}
