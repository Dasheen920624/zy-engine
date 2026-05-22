import { RobotOutlined } from '@ant-design/icons';
import { Button, Card, Space, Tag, Tooltip, Typography } from 'antd';
import type { AiBadgeProps } from './AiBadge.types';
import styles from './aiBadge.module.css';

const { Text } = Typography;

function getConfidenceClass(confidence: number): string {
  if (confidence >= 80) return styles.confidenceHigh;
  if (confidence >= 60) return styles.confidenceMid;
  return styles.confidenceLow;
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
  const confidenceClass = getConfidenceClass(confidence);

  if (variant === 'card') {
    return (
      <Card
        size="small"
        className={styles.card}
        styles={{
          body: { padding: 'var(--mk-space-4)' },
        }}
      >
        <Space direction="vertical" size="small" className={styles.fullWidth}>
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
            <Text strong className={`${styles.confidenceText} ${confidenceClass}`}>
              {confidence}%
            </Text>
          </Space>

          <Text type="secondary" className={styles.metaText}>
            生成时间: {generatedAt}
          </Text>

          <Space size="small" className={styles.actionRow}>
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
        <span className={`${styles.confidenceStrong} ${confidenceClass}`}>
          {confidence}%
        </span>
      </Tag>
    </Tooltip>
  );
}
