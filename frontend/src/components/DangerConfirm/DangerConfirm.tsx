import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Input,
  Modal,
  Space,
  Typography,
} from 'antd';
import {
  ExclamationCircleOutlined,
  WarningOutlined,
  StopOutlined,
} from '@ant-design/icons';
import type { DangerConfirmProps, DangerLevel } from './DangerConfirm.types';
import styles from './dangerConfirm.module.css';

const { Text, Paragraph } = Typography;

// ─── 工具函数 ──────────────────────────────────────────────────────

function levelIcon(level: DangerLevel) {
  switch (level) {
    case 'low':
      return <ExclamationCircleOutlined className={styles.iconWarning} />;
    case 'medium':
      return <WarningOutlined className={styles.iconDanger} />;
    case 'high':
      return <StopOutlined className={styles.iconDanger} />;
  }
}

function levelColor(level: DangerLevel): string {
  switch (level) {
    case 'low':
      return 'var(--mk-warning)';
    case 'medium':
    case 'high':
      return 'var(--mk-danger)';
  }
}

function confirmButtonLabel(level: DangerLevel, title: string): string {
  // 按钮文字必须明示后果
  if (level === 'low') return `确认${title}`;
  if (level === 'medium') return `确认${title}`;
  return `确认${title}`;
}

// ─── 组件 ──────────────────────────────────────────────────────

export default function DangerConfirm({
  level,
  title,
  description,
  consequences,
  confirmText,
  reasonRequired,
  irreversibleNote,
  onConfirm,
  onCancel,
  open = true,
  loading = false,
}: DangerConfirmProps) {
  const [inputValue, setInputValue] = useState('');
  const [reason, setReason] = useState('');
  const [delayDone, setDelayDone] = useState(level !== 'high');

  // high 级二次确认延迟
  useEffect(() => {
    if (level === 'high' && open) {
      setDelayDone(false);
      const timer = setTimeout(() => setDelayDone(true), 2000);
      return () => clearTimeout(timer);
    }
  }, [level, open]);

  // 重置
  useEffect(() => {
    if (open) {
      setInputValue('');
      setReason('');
    }
  }, [open]);

  const canConfirm = useCallback(() => {
    if (loading) return false;
    // medium/high 需要输入 confirmText
    if (confirmText && (level === 'medium' || level === 'high')) {
      if (inputValue !== confirmText) return false;
    }
    // high 级必须填原因
    if (reasonRequired && !reason.trim()) return false;
    // high 级延迟
    if (!delayDone) return false;
    return true;
  }, [loading, confirmText, level, inputValue, reasonRequired, reason, delayDone]);

  const handleConfirm = useCallback(async () => {
    if (!canConfirm()) return;
    await onConfirm(reasonRequired ? { reason: reason.trim() } : undefined);
  }, [canConfirm, onConfirm, reasonRequired, reason]);

  return (
    <Modal
      title={
        <Space>
          {levelIcon(level)}
          <span className={level === 'low' ? styles.titleWarning : styles.titleDanger}>{title}</span>
        </Space>
      }
      open={open}
      onCancel={onCancel}
      destroyOnClose
      footer={
        <Space>
          <Button onClick={onCancel}>取消</Button>
          <Button
            type="primary"
            danger={level !== 'low'}
            loading={loading}
            disabled={!canConfirm()}
            onClick={handleConfirm}
          >
            {confirmButtonLabel(level, title)}
          </Button>
        </Space>
      }
    >
      <Paragraph>{description}</Paragraph>

      {/* 后果列表 */}
      {consequences.length > 0 && (
        <div className={styles.consequenceBlock}>
          <Text type="secondary" className={styles.fieldLabel}>
            执行后：
          </Text>
          <ul className={styles.consequenceList}>
            {consequences.map((c, i) => (
              <li key={i}>
                <Text>{c}</Text>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* 不可撤销提示 */}
      {irreversibleNote && (
        <Alert
          type="error"
          showIcon
          message={irreversibleNote}
          style={{ marginBottom: 16 }}
        />
      )}

      {/* medium/high 级需要输入 confirmText */}
      {confirmText && (level === 'medium' || level === 'high') && (
        <div className={styles.confirmBlock}>
          <Text type="secondary" className={styles.fieldLabel}>
            请输入 <Text code>{confirmText}</Text> 以确认操作：
          </Text>
          <Input
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            placeholder={confirmText}
            status={inputValue && inputValue !== confirmText ? 'error' : undefined}
          />
          {inputValue && inputValue !== confirmText && (
            <Text type="danger" className={styles.errorText}>输入不匹配</Text>
          )}
        </div>
      )}

      {/* high 级必须填原因 */}
      {reasonRequired && (
        <div className={styles.confirmBlock}>
          <Text type="secondary" className={styles.fieldLabel}>
            操作原因 <span className={styles.textDanger}>*</span>
          </Text>
          <Input.TextArea
            rows={2}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="请填写操作原因"
          />
        </div>
      )}
    </Modal>
  );
}
