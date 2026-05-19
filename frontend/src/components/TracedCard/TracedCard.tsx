import { Card, Tooltip, message } from 'antd';
import { CopyOutlined, LinkOutlined } from '@ant-design/icons';
import type { TracedCardProps } from './TracedCard.types';

const styles: Record<string, React.CSSProperties> = {
  card: {
    position: 'relative',
    padding: 'var(--mk-density-card-padding)',
    borderRadius: 'var(--mk-radius-md)',
    boxShadow: 'var(--mk-shadow-sm)',
  },
  highlight: {
    borderLeft: '3px solid var(--mk-brand-primary)',
    boxShadow: 'var(--mk-shadow-md)',
  },
  actions: {
    position: 'absolute',
    right: 8,
    bottom: 8,
    display: 'flex',
    gap: 4,
    opacity: 0,
    transition: 'opacity var(--mk-duration-fast) var(--mk-ease-standard)',
  },
  traceInfo: {
    fontSize: 'var(--mk-text-xs)',
    color: 'var(--mk-text-tertiary)',
    fontFamily: 'var(--mk-font-mono)',
    marginBottom: 8,
  },
};

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

  const cardStyle: React.CSSProperties = {
    ...styles.card,
    ...(variant === 'highlight' ? styles.highlight : {}),
  };

  return (
    <Card
      style={cardStyle}
      styles={{ body: { padding: 'var(--mk-density-card-padding)' } }}
      onMouseEnter={(e) => {
        const actions = e.currentTarget.querySelector<HTMLElement>('.traced-card-actions');
        if (actions) actions.style.opacity = '1';
      }}
      onMouseLeave={(e) => {
        const actions = e.currentTarget.querySelector<HTMLElement>('.traced-card-actions');
        if (actions) actions.style.opacity = '0';
      }}
    >
      {contextHolder}

      {showTraceByDefault && (
        <div style={styles.traceInfo}>
          <span>TraceId: {traceId}</span>
          {apiPath && <span> | {apiPath}</span>}
          {timestamp && <span> | {timestamp}</span>}
        </div>
      )}

      {children}

      <div className="traced-card-actions" style={styles.actions}>
        <Tooltip title={traceId}>
          <LinkOutlined style={{ color: 'var(--mk-text-tertiary)', cursor: 'pointer' }} />
        </Tooltip>
        <CopyOutlined
          style={{ color: 'var(--mk-text-tertiary)', cursor: 'pointer' }}
          onClick={handleCopy}
        />
      </div>
    </Card>
  );
}
