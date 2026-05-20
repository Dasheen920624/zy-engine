import React, { useState } from "react";
import { Modal, Input, Form, Button, Space, Typography, Alert } from "antd";
import { ExclamationCircleOutlined, CheckCircleOutlined } from "@ant-design/icons";
import type { CdssAlert } from "../../api/cdss";
import { resolveCdssAlert } from "../../api/cdss";

const { Text } = Typography;
const { TextArea } = Input;

interface CdssAlertDialogProps {
  alert: CdssAlert | null;
  visible: boolean;
  onClose: () => void;
  onResolved: () => void;
}

const RISK_LEVEL_CONFIG: Record<string, { color: string; label: string }> = {
  INFO: { color: "var(--mk-primary)", label: "信息提示" },
  LOW: { color: "var(--mk-success)", label: "低风险" },
  MEDIUM: { color: "var(--mk-warning)", label: "中风险" },
  HIGH: { color: "var(--mk-danger)", label: "高风险" },
  CRITICAL: { color: "var(--mk-danger)", label: "危急阻断" },
};

const CdssAlertDialog: React.FC<CdssAlertDialogProps> = ({ alert, visible, onClose, onResolved }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  if (!alert) return null;

  const riskConfig = RISK_LEVEL_CONFIG[alert.riskLevel] ?? RISK_LEVEL_CONFIG.INFO;
  const isCritical = alert.riskLevel === "CRITICAL";

  const handleResolve = async (overrideType: "ACKNOWLEDGE" | "OVERRIDE" | "ESCALATE") => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      await resolveCdssAlert(alert.alertId, {
        override_type: overrideType,
        override_reason: values.override_reason,
        operator_name: values.operator_name,
        supervisor_name: values.supervisor_name,
      });
      onResolved();
      onClose();
    } catch {
      // validation or API error
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={
        <Space>
          <ExclamationCircleOutlined style={{ color: riskConfig.color, fontSize: 20 }} />
          <span>{alert.title}</span>
          <Text type="secondary" style={{ fontSize: 12 }}>
            [{riskConfig.label}]
          </Text>
        </Space>
      }
      open={visible}
      onCancel={onClose}
      footer={null}
      width={560}
    >
      <div style={{ marginBottom: 16 }}>
        {alert.isBlocking && (
          <Alert
            type="error"
            message="此告警为阻断级别，操作已被阻止"
            description="如需继续操作，必须提供覆盖原因并获得上级确认"
            showIcon
            style={{ marginBottom: 12 }}
          />
        )}

        <Text>{alert.message}</Text>

        {alert.evidence.length > 0 && (
          <div style={{ marginTop: 12 }}>
            <Text strong>来源证据：</Text>
            {alert.evidence.map((ev: Record<string, unknown>, i: number) => (
              <div key={i} style={{ padding: "4px 0", paddingLeft: 8 }}>
                <Text type="secondary">{String(ev.excerpt ?? ev.description ?? JSON.stringify(ev))}</Text>
              </div>
            ))}
          </div>
        )}

        {alert.source.documentCode && (
          <div style={{ marginTop: 8 }}>
            <Text type="secondary">
              参考文档：{alert.source.documentCode}
              {alert.source.citationId && ` / 引用：${alert.source.citationId}`}
            </Text>
          </div>
        )}

        {alert.ruleCode && (
          <div style={{ marginTop: 4 }}>
            <Text type="secondary">
              触发规则：{alert.ruleCode} {alert.ruleVersion && `(v${alert.ruleVersion})`}
            </Text>
          </div>
        )}
      </div>

      <Form form={form} layout="vertical">
        <Form.Item
          name="operator_name"
          label="操作人"
          rules={[{ required: true, message: "请输入操作人姓名" }]}
        >
          <Input placeholder="请输入姓名" />
        </Form.Item>

        <Form.Item
          name="override_reason"
          label="处理原因"
          rules={[{ required: isCritical, message: "阻断级告警必须填写原因" }]}
        >
          <TextArea rows={2} placeholder="请输入处理原因" />
        </Form.Item>

        {isCritical && (
          <Form.Item
            name="supervisor_name"
            label="上级确认人"
            rules={[{ required: true, message: "阻断级告警必须由上级确认" }]}
          >
            <Input placeholder="请输入上级确认人姓名" />
          </Form.Item>
        )}

        <Form.Item>
          <Space>
            <Button onClick={onClose}>取消</Button>
            <Button
              type="default"
              icon={<CheckCircleOutlined />}
              loading={loading}
              onClick={() => handleResolve("ACKNOWLEDGE")}
            >
              确认知悉
            </Button>
            {!isCritical && (
              <Button
                type="primary"
                danger
                loading={loading}
                onClick={() => handleResolve("OVERRIDE")}
              >
                覆盖继续
              </Button>
            )}
            {isCritical && (
              <Button
                type="primary"
                loading={loading}
                onClick={() => handleResolve("ESCALATE")}
              >
                上报上级
              </Button>
            )}
          </Space>
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default CdssAlertDialog;
