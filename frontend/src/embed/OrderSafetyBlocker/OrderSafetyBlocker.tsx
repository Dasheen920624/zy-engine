import { useState, useCallback } from 'react';
import { Modal, Button, Typography, Space, Divider, Tag } from 'antd';
import {
  StopOutlined,
  WarningOutlined,
  ExclamationCircleOutlined,
  MedicineBoxOutlined,
  EditOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons';
import type { OrderSafetyBlockerProps } from './OrderSafetyBlocker.types';
import ReasonDialog from './ReasonDialog';
import { recordDecision } from '../../api/ruleActionLog';
import styles from './OrderSafetyBlocker.module.css';

const { Text, Title, Paragraph } = Typography;

export default function OrderSafetyBlocker({
  open,
  ruleCode,
  ruleVersion,
  patientId,
  encounterId,
  orderId,
  actionMode,
  severity,
  title,
  patientInfo,
  orderInfo,
  riskDescription,
  evidence,
  source,
  onDecision,
  onClose,
}: OrderSafetyBlockerProps) {
  const [reasonDialogOpen, setReasonDialogOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [successDialogOpen, setSuccessDialogOpen] = useState(false);

  const handleCancelOrder = useCallback(async () => {
    setLoading(true);
    try {
      await onDecision('CANCEL');
      setSuccessDialogOpen(true);
    } finally {
      setLoading(false);
    }
  }, [onDecision]);

  const handleModifyDose = useCallback(async () => {
    setLoading(true);
    try {
      await onDecision('MODIFY');
      setSuccessDialogOpen(true);
    } finally {
      setLoading(false);
    }
  }, [onDecision]);

  const handleContinue = useCallback(() => {
    setReasonDialogOpen(true);
  }, []);

  const handleReasonConfirm = useCallback(
    async (data: { reason: string; informedConsent: boolean; familyNotified: boolean }) => {
      setLoading(true);
      try {
        await recordDecision({
          rule_code: ruleCode,
          rule_version: ruleVersion,
          patient_id: patientId,
          encounter_id: encounterId,
          order_id: orderId,
          action_mode: actionMode,
          decision: 'CONTINUE',
          decision_by: 'current_user', // 实际应从上下文获取
          reason: data.reason,
          informed_consent: data.informedConsent,
          family_notified: data.familyNotified,
        });
        await onDecision('CONTINUE', data);
        setReasonDialogOpen(false);
        setSuccessDialogOpen(true);
      } finally {
        setLoading(false);
      }
    },
    [ruleCode, ruleVersion, patientId, encounterId, orderId, actionMode, onDecision]
  );

  const handleReasonCancel = useCallback(() => {
    setReasonDialogOpen(false);
  }, []);

  const handleSuccessClose = useCallback(() => {
    setSuccessDialogOpen(false);
    onClose();
  }, [onClose]);

  const severityIcon = () => {
    switch (severity) {
      case 'CRITICAL':
        return <StopOutlined className={styles.iconCritical} />;
      case 'HIGH':
        return <WarningOutlined className={styles.iconHigh} />;
      case 'MEDIUM':
        return <ExclamationCircleOutlined className={styles.iconMedium} />;
      default:
        return <ExclamationCircleOutlined className={styles.iconDefault} />;
    }
  };

  const severityColor = () => {
    switch (severity) {
      case 'CRITICAL':
        return 'var(--mk-danger)';
      case 'HIGH':
        return 'var(--mk-warning)';
      case 'MEDIUM':
        return 'var(--mk-info)';
      default:
        return 'var(--mk-info)';
    }
  };

  return (
    <>
      <Modal
        title={
          <Space>
            {severityIcon()}
            <span>医嘱拦截 — {title}</span>
          </Space>
        }
        open={open}
        footer={null}
        width={560}
        maskClosable={false}
        closable={false}
        destroyOnClose
      >
        <div className={styles.marginBottom16}>
          <div className={styles.patientRow}>
            <Text strong>患者：{patientInfo.name} {patientInfo.age}岁</Text>
            <Text type="secondary">住院号 {patientInfo.patientId}</Text>
          </div>
          <div className={styles.patientRow}>
            <Text>医嘱：{orderInfo.name}</Text>
            <Text type="secondary">{orderInfo.dose} {orderInfo.frequency}</Text>
          </div>
        </div>

        <Divider className={styles.dividerCompact} />

        <div className={styles.marginBottom16}>
          <Tag color={severityColor()} className={styles.marginBottom8}>
            风险等级：{severity}
          </Tag>
          <Paragraph className={styles.marginBottom8}>
            <WarningOutlined className={styles.iconWarning} />
            {riskDescription}
          </Paragraph>
          <Paragraph type="secondary" className={styles.evidenceText}>
            {evidence}
          </Paragraph>
          {source && (
            <Text type="secondary" className={styles.sourceText}>
              来源：{source.documentName}
              {source.section && ` ${source.section}`}
              {source.publishYear && ` (${source.publishYear})`}
            </Text>
          )}
        </div>

        <Divider className={styles.dividerCompact} />

        <div className={styles.marginBottom16}>
          <Text strong>您必须确认以下之一才能继续：</Text>
          <ul className={styles.bulletList}>
            <li>
              <Text>已评估出血/血栓风险，仍坚持使用（必须填写理由）</Text>
            </li>
            <li>
              <Text>修改为低剂量方案</Text>
            </li>
            <li>
              <Text>取消该医嘱</Text>
            </li>
          </ul>
        </div>

        <Space className={styles.actionsBar}>
          <Button
            icon={<CloseCircleOutlined />}
            onClick={handleCancelOrder}
            loading={loading}
          >
            取消该医嘱
          </Button>
          <Button
            icon={<EditOutlined />}
            onClick={handleModifyDose}
            loading={loading}
          >
            修改剂量
          </Button>
          <Button
            type="primary"
            danger
            icon={<MedicineBoxOutlined />}
            onClick={handleContinue}
            loading={loading}
          >
            坚持使用
          </Button>
        </Space>
      </Modal>

      <ReasonDialog
        open={reasonDialogOpen}
        onConfirm={handleReasonConfirm}
        onCancel={handleReasonCancel}
        loading={loading}
      />

      <Modal
        title="医嘱已记录"
        open={successDialogOpen}
        onOk={handleSuccessClose}
        onCancel={handleSuccessClose}
        footer={[
          <Button key="close" onClick={handleSuccessClose}>
            关闭
          </Button>,
        ]}
        width={400}
      >
        <div className={styles.successContent}>
          <MedicineBoxOutlined className={styles.iconSuccess} />
          <Title level={4} className={styles.successTitle}>决策已记录</Title>
          <Text type="secondary">
            您的决策已写入审计，药师审方时会看到您的理由
          </Text>
        </div>
      </Modal>
    </>
  );
}