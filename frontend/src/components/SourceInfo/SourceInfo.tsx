import { Card, Typography } from 'antd';
import { FileTextOutlined } from '@ant-design/icons';
import type { SourceInfoProps } from './SourceInfo.types';
import { StatusBadge } from '../StatusBadge';
import type { StatusKey } from '../StatusBadge/StatusBadge.types';

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
      style={{
        cursor: onClickDocument ? 'pointer' : 'default',
        color: onClickDocument ? 'var(--mk-brand-primary)' : 'var(--mk-text-primary)',
        textDecoration: onClickDocument ? 'underline' : 'none',
      }}
    >
      <FileTextOutlined style={{ marginRight: 4 }} />
      {source.documentName}
    </span>
  );

  const sectionTag = source.section ? (
    <Text type="secondary" style={{ fontSize: 'var(--mk-text-xs)' }}>
      {' · '}
      {source.section}
    </Text>
  ) : null;

  const yearTag = source.publishYear ? (
    <Text type="secondary" style={{ fontSize: 'var(--mk-text-xs)' }}>
      {' · '}
      {source.publishYear}
    </Text>
  ) : null;

  const versionTag = version ? (
    <Text type="secondary" style={{ fontSize: 'var(--mk-text-xs)' }}>
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
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: 6,
          background: isMissing ? 'var(--mk-danger-soft)' : 'transparent',
          padding: isMissing ? '2px 8px' : 0,
          borderRadius: 'var(--mk-radius-sm)',
        }}
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
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: 4,
          fontSize: 'var(--mk-text-sm)',
          background: isMissing ? 'var(--mk-danger-soft)' : 'transparent',
          padding: isMissing ? '1px 6px' : 0,
          borderRadius: 'var(--mk-radius-sm)',
        }}
      >
        <FileTextOutlined />
        <Text style={{ fontSize: 'var(--mk-text-sm)' }}>{source.documentName}</Text>
        {versionTag}
        {badge}
      </span>
    );
  }

  /* ── card ───────────────────────────────────────────────────── */
  return (
    <Card
      size="small"
      style={{
        background: isMissing ? 'var(--mk-danger-soft)' : 'var(--mk-bg-panel)',
        borderColor: isMissing ? 'var(--mk-danger-border)' : 'var(--mk-border)',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
        {docLabel}
        {badge}
      </div>

      <div style={{ display: 'flex', gap: 8, alignItems: 'center', color: 'var(--mk-text-secondary)', fontSize: 'var(--mk-text-xs)' }}>
        {source.section && <span>{source.section}</span>}
        {source.publishYear && <span>{source.publishYear}</span>}
        {versionTag}
        {review?.reviewerName && <span>审核人：{review.reviewerName}</span>}
        {review?.reviewedAt && <span>{review.reviewedAt}</span>}
      </div>

      {citation && (
        <Paragraph
          style={{
            marginTop: 8,
            marginBottom: 0,
            fontSize: 'var(--mk-text-sm)',
            color: 'var(--mk-text-secondary)',
            borderLeft: `3px solid var(--mk-brand-primary)`,
            paddingLeft: 8,
          }}
          ellipsis={{ rows: 3, expandable: true, symbol: '展开' }}
        >
          {citation.excerpt}
        </Paragraph>
      )}

      {onClickDocument && (
        <div style={{ marginTop: 8 }}>
          <Text
            role="button"
            tabIndex={0}
            onClick={onClickDocument}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') onClickDocument();
            }}
            style={{ color: 'var(--mk-brand-primary)', cursor: 'pointer', fontSize: 'var(--mk-text-sm)' }}
          >
            查看原文
          </Text>
        </div>
      )}
    </Card>
  );
}
