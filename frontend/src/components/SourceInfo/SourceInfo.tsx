import { Card, Typography } from 'antd';
import { FileTextOutlined } from '@ant-design/icons';
import type { SourceInfoProps } from './SourceInfo.types';
import { StatusBadge } from '../StatusBadge';
import type { StatusKey } from '../StatusBadge/StatusBadge.types';
import styles from './sourceInfo.module.css';

const { Text, Paragraph } = Typography;

const reviewStatusToBadgeStatus: Record<NonNullable<SourceInfoProps['review']>['status'], StatusKey> = {
  reviewed: 'reviewed',
  pending: 'pending',
  rejected: 'rejected',
  missing: 'missing_source',
};

const statusLabelMap: Record<NonNullable<SourceInfoProps['review']>['status'], string> = {
  reviewed: '已审核',
  pending: '待审核',
  rejected: '已驳回',
  missing: '来源缺失',
};

export function SourceInfo({
  source,
  citation,
  review,
  version,
  variant = 'inline',
  onClickDocument,
}: SourceInfoProps) {
  const isMissing = review?.status === 'missing';

  const docLabel = (
    <span
      role="button"
      tabIndex={0}
      onClick={onClickDocument}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') onClickDocument?.();
      }}
      className={onClickDocument ? styles.docLabelClickable : styles.docLabel}
    >
      <FileTextOutlined className={styles.docIcon} />
      {source.documentName}
    </span>
  );

  const sectionTag = source.section ? (
    <Text type="secondary" className={styles.metaTag}>
      {' · '}
      {source.section}
    </Text>
  ) : null;

  const yearTag = source.publishYear ? (
    <Text type="secondary" className={styles.metaTag}>
      {' · '}
      {source.publishYear}
    </Text>
  ) : null;

  const versionTag = version ? (
    <Text type="secondary" className={styles.metaTag}>
      v{version}
    </Text>
  ) : null;

  const badge = review ? (
    <StatusBadge status={reviewStatusToBadgeStatus[review.status]} text={statusLabelMap[review.status]} />
  ) : null;

  /* ── inline ─────────────────────────────────────────────────── */
  if (variant === 'inline') {
    return (
      <span
        className={`${styles.inlineContainer} ${isMissing ? styles.inlineContainerMissing : ''}`}
      >
        {docLabel}
        {sectionTag}
        {yearTag}
        {versionTag}
        {badge}
      </span>
    );
  }

  /* ── compact ────────────────────────────────────────────────── */
  if (variant === 'compact') {
    return (
      <span
        className={`${styles.compactContainer} ${isMissing ? styles.compactContainerMissing : ''}`}
      >
        <FileTextOutlined />
        <Text className={styles.compactName}>{source.documentName}</Text>
        {versionTag}
        {badge}
      </span>
    );
  }

  /* ── card ───────────────────────────────────────────────────── */
  return (
    <Card
      size="small"
      className={isMissing ? styles.cardMissing : styles.cardNormal}
    >
      <div className={styles.cardHeader}>
        {docLabel}
        {badge}
      </div>

      <div className={styles.cardMeta}>
        {source.section && <span>{source.section}</span>}
        {source.publishYear && <span>{source.publishYear}</span>}
        {versionTag}
        {review?.reviewerName && <span>审核人：{review.reviewerName}</span>}
        {review?.reviewedAt && <span>{review.reviewedAt}</span>}
      </div>

      {citation && (
        <Paragraph
          className={styles.citationBlock}
          ellipsis={{ rows: 3, expandable: true, symbol: '展开' }}
        >
          {citation.excerpt}
        </Paragraph>
      )}

      {onClickDocument && (
        <div className={styles.viewOriginal}>
          <Text
            role="button"
            tabIndex={0}
            onClick={onClickDocument}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') onClickDocument();
            }}
            className={styles.viewOriginalLink}
          >
            查看原文
          </Text>
        </div>
      )}
    </Card>
  );
}
