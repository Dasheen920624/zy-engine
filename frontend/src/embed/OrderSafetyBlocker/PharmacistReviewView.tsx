import { Typography, Descriptions, Tag, Space, Alert, Button, Checkbox } from 'antd';
import {
  StopOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  SwapOutlined,
} from '@ant-design/icons';
import type { RuleActionLog } from './types';

const { Text, Paragraph } = Typography;

interface PharmacistReviewViewProps {
  /** 规则动作日志 */
  actionLog: RuleActionLog;
  /** 通过审方回调 */
  onApprove?: (actionLogId: string) => void;
  /** 驳回回调 */
  onReject?: (actionLogId: string, reason: string) => void;
  /** 转主任会诊回调 */
  onEscalate?: (actionLogId: string) => void;
  /** 是否只读模式（非药师角色） */
  readOnly?: boolean;
}

/**
 * 药师审方视图。
 * <p>
 * 展示医生的原始决策（决策、理由、知情同意），
 * 供药师审方时查看并做出通过/驳回/转主任会诊的决定。
 * </p>
 */
export default function PharmacistReviewView({
  actionLog,
  onApprove,
  onReject,
  onEscalate,
  readOnly = false,
}: PharmacistReviewViewProps) {
  const decisionLabel = (decision: string) => {
    switch (decision) {
      case 'CANCEL':
        return <Tag color="red">已取消医嘱</Tag>;
      case 'MODIFY':
        return <Tag color="orange">已修改剂量</Tag>;
      case 'INSIST':
        return <Tag color="blue">坚持使用</Tag>;
      default:
        return <Tag>{decision}</Tag>;
    }
  };

  return (
    <div style={{ border: '1px solid var(--mk-border, #d9d9d9)', borderRadius: 8, padding: 16 }}>
      {/* 标题 */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 16 }}>
        <StopOutlined style={{ color: 'var(--mk-danger, #ff4d4f)', fontSize: 18 }} />
        <Text strong style={{ fontSize: 16 }}>
          药师审方
        </Text>
        <Text type="secondary">患者 {actionLog.patient_id}</Text>
      </div>

      {/* 医嘱信息 */}
      <Descriptions column={2} size="small" style={{ marginBottom: 16 }}>
        <Descriptions.Item label="医嘱" span={2}>
          <Text strong>{actionLog.order_name}</Text>
          <Text type="secondary" style={{ marginLeft: 8 }}>
            ({actionLog.order_code})
          </Text>
        </Descriptions.Item>
        <Descriptions.Item label="规则">
          {actionLog.rule_name || actionLog.rule_code}
        </Descriptions.Item>
        <Descriptions.Item label="动作模式">
          <Tag color="red">{actionLog.action_mode}</Tag>
        </Descriptions.Item>
      </Descriptions>

      {/* 拦截提示 */}
      <Alert
        type="warning"
        showIcon
        message={
          <span>
            该医嘱已触发"{actionLog.rule_name || actionLog.rule_code}"拦截，医生
            {decisionLabel(actionLog.decision)}
          </span>
        }
        style={{ marginBottom: 16 }}
      />

      {/* 医生决策详情 */}
      <div
        style={{
          background: 'var(--mk-bg-secondary, #fafafa)',
          borderRadius: 6,
          padding: 12,
          marginBottom: 16,
        }}
      >
        <div style={{ marginBottom: 8 }}>
          <Text type="secondary" style={{ fontSize: 12 }}>医生</Text>
          <div>
            <Text strong>{actionLog.doctor_name || actionLog.doctor_id}</Text>
            <Text type="secondary" style={{ marginLeft: 8, fontSize: 12 }}>
              {actionLog.created_time}
            </Text>
          </div>
        </div>

        {actionLog.reason && (
          <div style={{ marginBottom: 8 }}>
            <Text type="secondary" style={{ fontSize: 12 }}>理由</Text>
            <Paragraph
              style={{
                margin: 0,
                padding: '8px 12px',
                background: 'var(--mk-bg-white, #fff)',
                borderRadius: 4,
                border: '1px solid var(--mk-border-light, #f0f0f0)',
              }}
            >
              {actionLog.reason}
            </Paragraph>
          </div>
        )}

        {actionLog.informed_consent !== undefined && (
          <div>
            <Text type="secondary" style={{ fontSize: 12 }}>知情同意</Text>
            <div style={{ display: 'flex', gap: 16, marginTop: 4 }}>
              <Checkbox checked={actionLog.informed_consent} disabled>
                已告知患者及家属出血风险
              </Checkbox>
              <Checkbox checked={actionLog.informed_consent} disabled>
                已签署知情同意书
              </Checkbox>
            </div>
          </div>
        )}
      </div>

      {/* 来源 */}
      {actionLog.trace_id && (
        <Text type="secondary" style={{ fontSize: 11, display: 'block', marginBottom: 16 }}>
          Trace ID: {actionLog.trace_id}
        </Text>
      )}

      {/* 操作按钮（非只读模式） */}
      {!readOnly && (
        <Space style={{ display: 'flex', justifyContent: 'flex-end' }}>
          <Button
            type="primary"
            icon={<CheckCircleOutlined />}
            onClick={() => onApprove?.(actionLog.action_log_id)}
          >
            通过审方
          </Button>
          <Button
            danger
            icon={<CloseCircleOutlined />}
            onClick={() => onReject?.(actionLog.action_log_id, '')}
          >
            驳回（必须写理由）
          </Button>
          <Button
            icon={<SwapOutlined />}
            onClick={() => onEscalate?.(actionLog.action_log_id)}
          >
            转主任会诊
          </Button>
        </Space>
      )}
    </div>
  );
}
