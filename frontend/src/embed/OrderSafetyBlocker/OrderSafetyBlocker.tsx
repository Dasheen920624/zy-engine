import { useState, useCallback } from 'react';
import { Modal, Button, Space, Typography, Descriptions, Tag, Radio, Alert } from 'antd';
import {
  StopOutlined,
  ExclamationCircleOutlined,
  EditOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons';
import type { OrderSafetyEvent, DoctorDecision } from './types';
import ReasonDialog from './ReasonDialog';

const { Text, Title } = Typography;

interface OrderSafetyBlockerProps {
  /** 拦截事件 */
  event: OrderSafetyEvent;
  /** 是否可见 */
  visible: boolean;
  /** 取消医嘱回调 */
  onCancelOrder: (eventId: string) => void;
  /** 修改剂量回调 */
  onModifyDosage: (eventId: string) => void;
  /** 坚持使用回调（带理由和知情同意） */
  onInsist: (eventId: string, reason: string, informedConsent: boolean) => void;
  /** 关闭回调（BLOCK 模式下不允许直接关闭） */
  onClose?: () => void;
}

/**
 * 医嘱安全拦截弹窗（BLOCK 模式）。
 * <p>
 * 必须确认以下之一才能继续：
 * <ul>
 *   <li>取消该医嘱</li>
 *   <li>修改为低剂量方案</li>
 *   <li>坚持使用（必须填写理由 ≥ 20 字 + 知情同意复选）</li>
 * </ul>
 * </p>
 *
 * <p>这是整个平台唯一用拦截弹窗的场景——因为出血风险是医疗安全 P0 红线。</p>
 */
export default function OrderSafetyBlocker({
  event,
  visible,
  onCancelOrder,
  onModifyDosage,
  onInsist,
}: OrderSafetyBlockerProps) {
  const [selectedDecision, setSelectedDecision] = useState<DoctorDecision | null>(null);
  const [reasonDialogVisible, setReasonDialogVisible] = useState(false);

  const handleDecision = useCallback(
    (decision: DoctorDecision) => {
      setSelectedDecision(decision);
      switch (decision) {
        case 'CANCEL':
          onCancelOrder(event.event_id);
          break;
        case 'MODIFY':
          onModifyDosage(event.event_id);
          break;
        case 'INSIST':
          setReasonDialogVisible(true);
          break;
      }
    },
    [event.event_id, onCancelOrder, onModifyDosage],
  );

  const handleInsistConfirm = useCallback(
    (reason: string, informedConsent: boolean) => {
      setReasonDialogVisible(false);
      onInsist(event.event_id, reason, informedConsent);
    },
    [event.event_id, onInsist],
  );

  const handleInsistCancel = useCallback(() => {
    setReasonDialogVisible(false);
    setSelectedDecision(null);
  }, []);

  return (
    <>
      <Modal
        open={visible}
        title={
          <Space>
            <StopOutlined style={{ color: 'var(--mk-danger, #ff4d4f)', fontSize: 20 }} />
            <span>医嘱拦截 — {event.rule_name}</span>
          </Space>
        }
        footer={null}
        closable={false}
        maskClosable={false}
        width={560}
        styles={{
          body: { padding: '16px 24px' },
        }}
      >
        {/* 患者信息 */}
        <Descriptions
          column={2}
          size="small"
          style={{ marginBottom: 16 }}
          labelStyle={{ color: 'var(--mk-text-secondary, #8c8c8c)', width: 70 }}
        >
          <Descriptions.Item label="患者">
            {event.patient_name || event.patient_id}
            {event.patient_age && `  ${event.patient_age} 岁`}
          </Descriptions.Item>
          <Descriptions.Item label="住院号">
            {event.admission_no || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="医嘱" span={2}>
            <Text strong>{event.order_name}</Text>
            {event.order_dosage && <Text type="secondary"> {event.order_dosage}</Text>}
          </Descriptions.Item>
        </Descriptions>

        {/* 拦截原因 */}
        <Alert
          type="warning"
          showIcon
          icon={<ExclamationCircleOutlined />}
          message={
            <span>
              <Text strong>⚠️ {event.intercept_reason}</Text>
            </span>
          }
          style={{ marginBottom: 16 }}
        />

        {/* 来源 */}
        {event.source && (
          <div style={{ marginBottom: 16, fontSize: 12, color: 'var(--mk-text-secondary, #8c8c8c)' }}>
            来源：{event.source.documentName}
            {event.source.section && ` ${event.source.section}`}
            {event.source.publishYear && ` (${event.source.publishYear})`}
          </div>
        )}

        {/* 决策选项 */}
        <div style={{ marginBottom: 16 }}>
          <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 8 }}>
            您必须确认以下之一才能继续：
          </Text>
          <Radio.Group
            value={selectedDecision}
            onChange={(e) => handleDecision(e.target.value)}
            style={{ display: 'flex', flexDirection: 'column', gap: 8 }}
          >
            <Radio value="CANCEL">
              <CloseCircleOutlined style={{ color: 'var(--mk-danger, #ff4d4f)', marginRight: 4 }} />
              取消该医嘱
            </Radio>
            <Radio value="MODIFY">
              <EditOutlined style={{ color: 'var(--mk-warning, #faad14)', marginRight: 4 }} />
              修改为低剂量方案
            </Radio>
            <Radio value="INSIST">
              <ExclamationCircleOutlined style={{ color: 'var(--mk-info, #1890ff)', marginRight: 4 }} />
              已评估出血/血栓风险，仍坚持使用（必须填写理由）
            </Radio>
          </Radio.Group>
        </div>

        {/* 底部按钮 */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
          <Button
            danger
            icon={<CloseCircleOutlined />}
            onClick={() => handleDecision('CANCEL')}
          >
            取消该医嘱
          </Button>
          <Button
            icon={<EditOutlined />}
            onClick={() => handleDecision('MODIFY')}
          >
            修改剂量
          </Button>
          <Button
            type="primary"
            icon={<ExclamationCircleOutlined />}
            onClick={() => handleDecision('INSIST')}
          >
            坚持使用
          </Button>
        </div>
      </Modal>

      {/* 坚持使用理由对话框 */}
      <ReasonDialog
        visible={reasonDialogVisible}
        onConfirm={handleInsistConfirm}
        onCancel={handleInsistCancel}
        ruleName={event.rule_name}
        orderName={event.order_name}
      />
    </>
  );
}
