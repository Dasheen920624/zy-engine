import { Typography, Descriptions, Tag, Space, Alert, Button, Checkbox } from 'antd';
import {
  StopOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  SwapOutlined,
} from '@ant-design/icons';
import type { RuleActionLog } from '../../api/ruleActionLog';
import styles from './pharmacistReviewView.module.css';

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
      case 'CONTINUE':
        return <Tag color="blue">坚持使用</Tag>;
      default:
        return <Tag>{decision}</Tag>;
    }
  };

  return (
    <div className={styles.container}>
      {/* 标题 */}
      <div className={styles.header}>
        <StopOutlined className={styles.headerIcon} />
        <Text strong className={styles.headerTitle}>
          药师审方
        </Text>
        <Text type="secondary">患者 {actionLog.patient_id}</Text>
      </div>

      {/* 医嘱信息 */}
      <Descriptions column={2} size="small" className={styles.infoDescriptions}>
        <Descriptions.Item label="医嘱" span={2}>
          <Text strong>{actionLog.order_id}</Text>
        </Descriptions.Item>
        <Descriptions.Item label="规则">
          {actionLog.rule_code}@{actionLog.rule_version}
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
            该医嘱已触发"{actionLog.rule_code}@{actionLog.rule_version}"拦截，医生
            {decisionLabel(actionLog.decision)}
          </span>
        }
        className={styles.warningAlert}
      />

      {/* 医生决策详情 */}
      <div className={styles.decisionBlock}>
        <div className={styles.fieldGroup}>
          <Text type="secondary" className={styles.fieldLabel}>医生</Text>
          <div>
            <Text strong>{actionLog.decision_by}</Text>
            <Text type="secondary" className={styles.fieldTime}>
              {actionLog.decision_time}
            </Text>
          </div>
        </div>

        {actionLog.reason && (
          <div className={styles.fieldGroup}>
            <Text type="secondary" className={styles.fieldLabel}>理由</Text>
            <Paragraph className={styles.reasonParagraph}>
              {actionLog.reason}
            </Paragraph>
          </div>
        )}

        {(actionLog.informed_consent || actionLog.family_notified) && (
          <div>
            <Text type="secondary" className={styles.fieldLabel}>知情同意</Text>
            <div className={styles.consentRow}>
              <Checkbox checked={actionLog.informed_consent} disabled>
                已告知患者及家属出血风险
              </Checkbox>
              <Checkbox checked={actionLog.family_notified} disabled>
                已签署知情同意书
              </Checkbox>
            </div>
          </div>
        )}
      </div>

      {/* 来源 */}
      {actionLog.trace_id && (
        <Text type="secondary" className={styles.traceInfo}>
          Trace ID: {actionLog.trace_id}
        </Text>
      )}

      {/* 操作按钮（非只读模式） */}
      {!readOnly && (
        <Space className={styles.actions}>
          <Button
            type="primary"
            icon={<CheckCircleOutlined />}
            onClick={() => onApprove?.(actionLog.log_id)}
          >
            通过审方
          </Button>
          <Button
            danger
            icon={<CloseCircleOutlined />}
            onClick={() => onReject?.(actionLog.log_id, '')}
          >
            驳回（必须写理由）
          </Button>
          <Button
            icon={<SwapOutlined />}
            onClick={() => onEscalate?.(actionLog.log_id)}
          >
            转主任会诊
          </Button>
        </Space>
      )}
    </div>
  );
}
