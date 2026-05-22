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
import styles from './statusBadge.module.css';

interface StatusPreset {
  icon: React.ReactNode;
  text: string;
}

const STATUS_PRESETS: Record<StatusKey, StatusPreset> = {
  draft: {
    icon: <EditOutlined />,
    text: '草稿',
  },
  reviewed: {
    icon: <EyeOutlined />,
    text: '待发布',
  },
  published: {
    icon: <CheckCircleOutlined />,
    text: '已发布',
  },
  synced: {
    icon: <CloudSyncOutlined />,
    text: '已同步',
  },
  active: {
    icon: <PlayCircleOutlined />,
    text: '运行中',
  },
  retired: {
    icon: <StopOutlined />,
    text: '已下线',
  },
  rejected: {
    icon: <CloseCircleOutlined />,
    text: '已拒绝',
  },
  pending: {
    icon: <ClockCircleOutlined />,
    text: '待审核',
  },
  processing: {
    icon: <LoadingOutlined />,
    text: '处理中',
  },
  error: {
    icon: <WarningOutlined />,
    text: '错误',
  },
  missing_source: {
    icon: <ExclamationCircleOutlined />,
    text: '来源缺失',
  },
  ai_candidate: {
    icon: <RobotOutlined />,
    text: 'AI 候选',
  },
  success: {
    icon: <CheckCircleOutlined />,
    text: '成功',
  },
  warning: {
    icon: <AlertOutlined />,
    text: '警告',
  },
  danger: {
    icon: <FireOutlined />,
    text: '危急',
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
  const globalSizeClass = `mk-status-badge--${size}`;
  const sizeClass = size === 'sm' ? styles.sizeSm : styles.sizeMd;
  const toneClass = styles[status];

  if (dotOnly) {
    return (
      <span
        className={`mk-status-badge mk-status-badge--dot-only ${globalSizeClass} ${styles.dotOnly} ${toneClass}`}
        title={title}
        role="status"
        aria-label={title}
      />
    );
  }

  return (
    <Tag
      className={`mk-status-badge ${globalSizeClass} ${styles.badge} ${sizeClass} ${toneClass}`}
      bordered={false}
      title={title}
      role="status"
      aria-label={finalText}
    >
      {showIcon && preset.icon}
      {showText && finalText}
    </Tag>
  );
};
