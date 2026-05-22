import { Card, Tooltip, message } from 'antd';
import { CopyOutlined, LinkOutlined } from '@ant-design/icons';
import type { TracedCardProps } from './TracedCard.types';
import styles from './tracedCard.module.css';

export default function TracedCard({
  traceId,
  apiPath,
  timestamp,
  children,
  variant = 'default',
  showTraceByDefault = false,
}: TracedCardProps) {
  const [messageApi, contextHolder] = message.useMessage();

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(traceId);
      messageApi.success('TraceId 已复制到剪贴板');
    } catch {
      messageApi.error('复制失败');
    }
  };

  return (
    <Card
      className={`${styles.card} ${variant === 'highlight' ? styles.highlight : ''}`}
      styles={{ body: { padding: 'var(--mk-density-card-padding)' } }}
    >
      {contextHolder}

      {showTraceByDefault && (
        <div className={styles.traceInfo}>
          <span>TraceId: {traceId}</span>
          {apiPath && <span> | {apiPath}</span>}
          {timestamp && <span> | {timestamp}</span>}
        </div>
      )}

      {children}

      <div className={`traced-card-actions ${styles.actions}`}>
        <Tooltip title={traceId}>
          <LinkOutlined className={styles.actionIcon} />
        </Tooltip>
        <CopyOutlined
          className={styles.actionIcon}
          onClick={handleCopy}
        />
      </div>
    </Card>
  );
}
