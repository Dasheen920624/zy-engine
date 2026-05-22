import { RobotOutlined } from '@ant-design/icons';
import { Button, Card, Space, Tag, Tooltip, Typography } from 'antd';
import type { AiBadgeProps } from './AiBadge.types';
import styles from './aiBadge.module.css';

const { Text } = Typography;

function getConfidenceColor(confidence: number): string {
  if (confidence >= 80) return 'var(--mk-ai-confidence-high)';
  if (confidence >= 60) return 'var(--mk-ai-confidence-mid)';
  return 'var(--mk-ai-confidence-low)';
}

export default function AiBadge({
  confidence,
  model,
  generatedAt,
  reviewStatus,
  onAccept,
  onModify,
  onReject,
  variant = 'badge',
}: AiBadgeProps) {
  const confidenceColor = getConfidenceColor(confidence);

  if (variant === 'card') {
    return (
      <Card
        size="small"
        className={styles.card}
        styles={{
          body: { padding: 'var(--mk-space-4)' },
        }}
      >
        <Space direction="vertical" size="small" style={{ width: '100%' }}>
          <Space>
            <RobotOutlined className={styles.aiIcon} />
            <Text strong className={styles.aiText}>
              AI 候选
            </Text>
            <Tag className={styles.modelTag}>
              {model}
            </Tag>
          </Space>

          <Space size="small">
            <Text type="secondary" className={styles.metaText}>
              置信度:
            </Text>
            <Text
              strong
              style={{ color: confidenceColor, fontSize: 'var(--mk-text-sm)' }}
            >
              {confidence}%
            </Text>
          </Space>

          <Text type="secondary" className={styles.metaText}>
            生成时间: {generatedAt}
          </Text>

          <Space size="small" style={{ marginTop: 'var(--mk-space-1)' }}>
            <Button
              size="small"
              type="primary"
              disabled={reviewStatus === 'pending'}
              onClick={onAccept}
            >
              采纳
            </Button>
            <Button size="small" onClick={onModify}>
              修改
            </Button>
            <Button size="small" danger onClick={onReject}>
              拒绝
            </Button>
          </Space>
        </Space>
      </Card>
    );
  }

  // badge 模式
  return (
    <Tooltip
      title={`模型: ${model} | 生成时间: ${generatedAt} | 状态: ${reviewStatus}`}
    >
      <Tag
        icon={<RobotOutlined />}
        className={styles.badgeTag}
      >
        AI 候选
        <span style={{ color: confidenceColor, fontWeight: 600 }}>
          {confidence}%
        </span>
      </Tag>
    </Tooltip>
  );
}
