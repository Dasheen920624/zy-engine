export type StatusKey =
  | 'draft'
  | 'reviewed'
  | 'published'
  | 'synced'
  | 'active'
  | 'retired'
  | 'rejected'
  | 'pending'
  | 'processing'
  | 'error'
  | 'missing_source'
  | 'ai_candidate'
  | 'success'
  | 'warning'
  | 'danger';

export interface StatusBadgeProps {
  status: StatusKey;
  size?: 'sm' | 'md';
  showIcon?: boolean;
  showText?: boolean;
  text?: string;
  dotOnly?: boolean;
  title?: string;
}
