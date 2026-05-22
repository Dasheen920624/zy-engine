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
import styles from './embeddedAlert.module.css';

const { Text } = Typography;

// ─── 工具函数 ──────────────────────────────────────────────────────

function severityIcon(severity: AlertSeverity) {
  switch (severity) {
    case 'info':
      return <InfoCircleOutlined className={styles.iconInfo} />;
    case 'warning':
      return <ExclamationCircleOutlined className={styles.iconWarning} />;
    case 'danger':
      return <StopOutlined className={styles.iconDanger} />;
    case 'success':
      return <CheckCircleOutlined className={styles.iconSuccess} />;
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
        background: severityBg(severity),
        border: `1px solid ${severityBorder(severity)}`,
      }}
      className={styles.container}
    >
      {/* 图标 */}
      <span className={styles.icon}>
        {severityIcon(severity)}
      </span>

      {/* 内容 */}
      <div className={styles.content}>
        <div className={styles.titleRow}>
          <Text strong className={styles.title}>
            {title}
          </Text>
          {confidence !== null && confidence !== undefined && (
            <Text type="secondary" className={styles.meta}>
              置信 {confidence}%
            </Text>
          )}
          {ruleRef && (
            <Text
              type="secondary"
              className={styles.ruleRef}
            >
              {ruleRef.code}@{ruleRef.version}
            </Text>
          )}
        </div>
        <Text
          type="secondary"
          className={styles.evidence}
        >
          {evidence}
          {source && (
            <span className={styles.source}>
              [{source.documentName}
              {source.section && ` ${source.section}`}
              {source.publishYear && ` (${source.publishYear})`}]
            </span>
          )}
        </Text>
      </div>

      {/* 操作按钮 */}
      <Space size={4} className={styles.actions}>
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
          className={styles.closeButton}
        >
          <CloseOutlined />
        </span>
      )}
    </div>
  );
}
