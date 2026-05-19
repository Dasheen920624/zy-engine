import { useCallback, useEffect, useRef } from 'react';
import { Button, Space, Typography } from 'antd';
import {
  CheckCircleOutlined,
  CloseOutlined,
  ExclamationCircleOutlined,
  InfoCircleOutlined,
  StopOutlined,
} from '@ant-design/icons';
import type { EmbeddedAlertProps, AlertSeverity } from './EmbeddedAlert.types';

const { Text } = Typography;

// ─── 工具函数 ──────────────────────────────────────────────────────

function severityIcon(severity: AlertSeverity) {
  switch (severity) {
    case 'info':
      return <InfoCircleOutlined style={{ color: 'var(--mk-info)' }} />;
    case 'warning':
      return <ExclamationCircleOutlined style={{ color: 'var(--mk-warning)' }} />;
    case 'danger':
      return <StopOutlined style={{ color: 'var(--mk-danger)' }} />;
    case 'success':
      return <CheckCircleOutlined style={{ color: 'var(--mk-success)' }} />;
  }
}

function severityBg(severity: AlertSeverity): string {
  switch (severity) {
    case 'info':
      return 'var(--mk-info-soft)';
    case 'warning':
      return 'var(--mk-warning-soft)';
    case 'danger':
      return 'var(--mk-danger-soft)';
    case 'success':
      return 'var(--mk-success-soft)';
  }
}

function severityBorder(severity: AlertSeverity): string {
  switch (severity) {
    case 'info':
      return 'var(--mk-info-border)';
    case 'warning':
      return 'var(--mk-warning-border)';
    case 'danger':
      return 'var(--mk-danger-border)';
    case 'success':
      return 'var(--mk-success-border)';
  }
}

// ─── 组件 ──────────────────────────────────────────────────────

export default function EmbeddedAlert({
  severity,
  title,
  evidence,
  source,
  ruleRef,
  confidence,
  actions,
  onClose,
  autoHide,
}: EmbeddedAlertProps) {
  const timerRef = useRef<ReturnType<typeof setTimeout>>();

  // 自动消失
  useEffect(() => {
    if (autoHide && severity === 'success') {
      timerRef.current = setTimeout(() => {
        onClose?.();
      }, autoHide);
      return () => clearTimeout(timerRef.current);
    }
  }, [autoHide, severity, onClose]);

  const handleClose = useCallback(() => {
    onClose?.();
  }, [onClose]);

  // 按钮 intent 映射
  const buttonType = (intent: string) => {
    switch (intent) {
      case 'primary':
        return 'primary';
      case 'secondary':
        return 'default';
      case 'tertiary':
        return 'link';
      default:
        return 'default';
    }
  };

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 12,
        padding: '8px 12px',
        background: severityBg(severity),
        border: `1px solid ${severityBorder(severity)}`,
        borderRadius: 4,
        maxHeight: 80,
        overflow: 'hidden',
        fontSize: 13,
      }}
    >
      {/* 图标 */}
      <span style={{ flexShrink: 0, fontSize: 16 }}>
        {severityIcon(severity)}
      </span>

      {/* 内容 */}
      <div style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', gap: 2 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Text strong style={{ fontSize: 13, whiteSpace: 'nowrap' }}>
            {title}
          </Text>
          {confidence !== null && confidence !== undefined && (
            <Text type="secondary" style={{ fontSize: 11, whiteSpace: 'nowrap' }}>
              置信 {confidence}%
            </Text>
          )}
          {ruleRef && (
            <Text
              type="secondary"
              style={{ fontSize: 11, fontFamily: 'var(--mk-font-mono)', whiteSpace: 'nowrap' }}
            >
              {ruleRef.code}@{ruleRef.version}
            </Text>
          )}
        </div>
        <Text
          type="secondary"
          style={{
            fontSize: 12,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
          }}
        >
          {evidence}
          {source && (
            <span style={{ marginLeft: 8, opacity: 0.7 }}>
              [{source.documentName}
              {source.section && ` ${source.section}`}
              {source.publishYear && ` (${source.publishYear})`}]
            </span>
          )}
        </Text>
      </div>

      {/* 操作按钮 */}
      <Space size={4} style={{ flexShrink: 0 }}>
        {actions.slice(0, 3).map((action, i) => (
          <Button
            key={i}
            size="small"
            type={buttonType(action.intent) as 'primary' | 'default' | 'link'}
            danger={severity === 'danger' && action.intent === 'primary'}
            onClick={action.onClick}
          >
            {action.text}
          </Button>
        ))}
      </Space>

      {/* 关闭按钮 */}
      {onClose && (
        <span
          onClick={handleClose}
          style={{
            flexShrink: 0,
            cursor: 'pointer',
            opacity: 0.5,
            fontSize: 12,
          }}
        >
          <CloseOutlined />
        </span>
      )}
    </div>
  );
}
