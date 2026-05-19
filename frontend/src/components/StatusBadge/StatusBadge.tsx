import React from 'react';
import { Tag } from 'antd';
import {
  EditOutlined,
  EyeOutlined,
  CheckCircleOutlined,
  CloudSyncOutlined,
  PlayCircleOutlined,
  StopOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
  LoadingOutlined,
  WarningOutlined,
  ExclamationCircleOutlined,
  RobotOutlined,
  AlertOutlined,
  FireOutlined,
} from '@ant-design/icons';
import type { StatusBadgeProps, StatusKey } from './StatusBadge.types';

interface StatusPreset {
  icon: React.ReactNode;
  text: string;
  color: string;
  bgColor: string;
}

const STATUS_PRESETS: Record<StatusKey, StatusPreset> = {
  draft: {
    icon: <EditOutlined />,
    text: '草稿',
    color: 'var(--mk-text-tertiary)',
    bgColor: 'var(--mk-bg-disabled)',
  },
  reviewed: {
    icon: <EyeOutlined />,
    text: '待发布',
    color: 'var(--mk-brand-primary)',
    bgColor: 'var(--mk-brand-primary-soft)',
  },
  published: {
    icon: <CheckCircleOutlined />,
    text: '已发布',
    color: 'var(--mk-success)',
    bgColor: 'var(--mk-success-soft)',
  },
  synced: {
    icon: <CloudSyncOutlined />,
    text: '已同步',
    color: 'var(--mk-success)',
    bgColor: 'var(--mk-success-soft)',
  },
  active: {
    icon: <PlayCircleOutlined />,
    text: '运行中',
    color: 'var(--mk-success)',
    bgColor: 'var(--mk-success-soft)',
  },
  retired: {
    icon: <StopOutlined />,
    text: '已下线',
    color: 'var(--mk-text-tertiary)',
    bgColor: 'var(--mk-bg-disabled)',
  },
  rejected: {
    icon: <CloseCircleOutlined />,
    text: '已拒绝',
    color: 'var(--mk-danger)',
    bgColor: 'var(--mk-danger-soft)',
  },
  pending: {
    icon: <ClockCircleOutlined />,
    text: '待审核',
    color: 'var(--mk-warning)',
    bgColor: 'var(--mk-warning-soft)',
  },
  processing: {
    icon: <LoadingOutlined />,
    text: '处理中',
    color: 'var(--mk-brand-primary)',
    bgColor: 'var(--mk-brand-primary-soft)',
  },
  error: {
    icon: <WarningOutlined />,
    text: '错误',
    color: 'var(--mk-danger)',
    bgColor: 'var(--mk-danger-soft)',
  },
  missing_source: {
    icon: <ExclamationCircleOutlined />,
    text: '来源缺失',
    color: 'var(--mk-warning)',
    bgColor: 'var(--mk-warning-soft)',
  },
  ai_candidate: {
    icon: <RobotOutlined />,
    text: 'AI 候选',
    color: 'var(--mk-ai-primary)',
    bgColor: 'var(--mk-ai-soft)',
  },
  success: {
    icon: <CheckCircleOutlined />,
    text: '成功',
    color: 'var(--mk-success)',
    bgColor: 'var(--mk-success-soft)',
  },
  warning: {
    icon: <AlertOutlined />,
    text: '警告',
    color: 'var(--mk-warning)',
    bgColor: 'var(--mk-warning-soft)',
  },
  danger: {
    icon: <FireOutlined />,
    text: '危急',
    color: 'var(--mk-danger)',
    bgColor: 'var(--mk-danger-soft)',
  },
};

export const StatusBadge: React.FC<StatusBadgeProps> = ({
  status,
  size = 'md',
  showIcon = true,
  showText = true,
  text,
  dotOnly = false,
  title,
}) => {
  const preset = STATUS_PRESETS[status];

  if (!preset) {
    console.warn(`[StatusBadge] Unknown status: ${status}`);
    return null;
  }

  if (dotOnly && !title) {
    throw new Error('[StatusBadge] dotOnly mode requires title prop');
  }

  const finalText = text ?? preset.text;
  const sizeClass = `mk-status-badge--${size}`;

  if (dotOnly) {
    return (
      <span
        className={`mk-status-badge mk-status-badge--dot-only ${sizeClass}`}
        style={{
          display: 'inline-block',
          width: 8,
          height: 8,
          borderRadius: '50%',
          backgroundColor: preset.color,
        }}
        title={title}
        role="status"
        aria-label={title}
      />
    );
  }

  return (
    <Tag
      className={`mk-status-badge ${sizeClass}`}
      bordered={false}
      style={{
        color: preset.color,
        backgroundColor: preset.bgColor,
        borderRadius: 'var(--mk-radius-sm)',
        fontSize: size === 'sm' ? 'var(--mk-text-xs)' : 'var(--mk-text-sm)',
        lineHeight: 'var(--mk-leading-tight)',
        marginInlineEnd: 0,
        display: 'inline-flex',
        alignItems: 'center',
        gap: 'var(--mk-space-1)',
      }}
      title={title}
      role="status"
      aria-label={finalText}
    >
      {showIcon && preset.icon}
      {showText && finalText}
    </Tag>
  );
};
