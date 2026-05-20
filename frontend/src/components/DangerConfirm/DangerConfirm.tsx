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

const { Text, Paragraph } = Typography;

// ─── 工具函数 ──────────────────────────────────────────────────────

function levelIcon(level: DangerLevel) {
  switch (level) {
    case 'low':
      return <ExclamationCircleOutlined style={{ color: 'var(--mk-warning)' }} />;
    case 'medium':
      return <WarningOutlined style={{ color: 'var(--mk-danger)' }} />;
    case 'high':
      return <StopOutlined style={{ color: 'var(--mk-danger)' }} />;
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
          <span style={{ color: levelColor(level) }}>{title}</span>
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
        <div style={{ marginBottom: 16 }}>
          <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 4 }}>
            执行后：
          </Text>
          <ul style={{ margin: 0, paddingLeft: 20 }}>
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
        <div style={{ marginBottom: 12 }}>
          <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 4 }}>
            请输入 <Text code>{confirmText}</Text> 以确认操作：
          </Text>
          <Input
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            placeholder={confirmText}
            status={inputValue && inputValue !== confirmText ? 'error' : undefined}
          />
          {inputValue && inputValue !== confirmText && (
            <Text type="danger" style={{ fontSize: 12 }}>输入不匹配</Text>
          )}
        </div>
      )}

      {/* high 级必须填原因 */}
      {reasonRequired && (
        <div style={{ marginBottom: 12 }}>
          <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 4 }}>
            操作原因 <span style={{ color: 'var(--mk-danger)' }}>*</span>
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
