# 公共组件参考实现 — StatusBadge

> 用途：实现 V2 13 个公共组件之一时复制本样板。  
> 关联 PR：PR-V2-02 公共组件库 v1（C01-C05）

## 文件结构（每个组件 5 个文件）

```
frontend/src/components/StatusBadge/
├── StatusBadge.types.ts          类型定义
├── StatusBadge.tsx               组件实现
├── StatusBadge.stories.tsx       Storybook（≥ 3 个故事）
├── StatusBadge.test.tsx          单元测试（覆盖率 ≥ 80%）
└── index.ts                      统一导出
```

## 1. types.ts

```typescript
// frontend/src/components/StatusBadge/StatusBadge.types.ts
import type React from 'react';

/**
 * 全平台统一状态值。新增状态必须同步：
 * 1. 本枚举
 * 2. 03_设计系统.md §4.2 表格
 * 3. STATUS_PRESETS 常量
 */
export type StatusKey =
  | 'draft'           // 草稿
  | 'reviewed'        // 待发布
  | 'published'       // 已发布
  | 'synced'          // 已同步
  | 'active'          // 运行中
  | 'retired'         // 已下线
  | 'rejected'        // 已拒绝
  | 'pending'         // 待审核
  | 'processing'      // 处理中
  | 'error'           // 错误
  | 'missing_source'  // 来源缺失
  | 'ai_candidate'    // AI 候选
  | 'success'         // 成功
  | 'warning'         // 警告
  | 'danger';         // 危急

export interface StatusBadgeProps {
  status: StatusKey;
  size?: 'sm' | 'md';
  showIcon?: boolean;
  showText?: boolean;
  text?: string;
  dotOnly?: boolean;
  title?: string;
}
```

## 2. tsx（核心实现）

```tsx
// frontend/src/components/StatusBadge/StatusBadge.tsx
import React from 'react';
import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  LinkOutlined,
  LoadingOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  RobotOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import type { StatusBadgeProps, StatusKey } from './StatusBadge.types';
import './StatusBadge.css';

const STATUS_PRESETS: Record<
  StatusKey,
  { icon: React.ReactNode; text: string; color: string }
> = {
  draft:          { icon: <EditOutlined />,             text: '草稿',     color: 'var(--mk-text-tertiary)' },
  reviewed:       { icon: <ClockCircleOutlined />,      text: '待发布',   color: 'var(--mk-info)' },
  published:      { icon: <CheckCircleOutlined />,      text: '已发布',   color: 'var(--mk-success)' },
  synced:         { icon: <LinkOutlined />,             text: '已同步',   color: 'var(--mk-success)' },
  active:         { icon: <PlayCircleOutlined />,       text: '运行中',   color: 'var(--mk-success)' },
  retired:        { icon: <PauseCircleOutlined />,      text: '已下线',   color: 'var(--mk-text-tertiary)' },
  rejected:       { icon: <CloseCircleOutlined />,      text: '已拒绝',   color: 'var(--mk-danger)' },
  pending:        { icon: <ClockCircleOutlined />,      text: '待审核',   color: 'var(--mk-warning)' },
  processing:     { icon: <LoadingOutlined />,          text: '处理中',   color: 'var(--mk-info)' },
  error:          { icon: <CloseCircleOutlined />,      text: '错误',     color: 'var(--mk-danger)' },
  missing_source: { icon: <WarningOutlined />,          text: '来源缺失', color: 'var(--mk-warning)' },
  ai_candidate:   { icon: <RobotOutlined />,            text: 'AI 候选',  color: 'var(--mk-ai-primary)' },
  success:        { icon: <CheckCircleOutlined />,      text: '成功',     color: 'var(--mk-success)' },
  warning:        { icon: <ExclamationCircleOutlined />,text: '警告',     color: 'var(--mk-warning)' },
  danger:         { icon: <ExclamationCircleOutlined />,text: '危急',     color: 'var(--mk-danger)' },
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

  // 强制：dotOnly 必须配 title（无障碍）
  if (dotOnly && !title) {
    throw new Error('[StatusBadge] dotOnly mode requires title prop');
  }

  const finalText = text ?? preset.text;
  const className = `mk-status-badge mk-status-badge--${size} mk-status-badge--${status}`;

  if (dotOnly) {
    return (
      <span
        className={`${className} mk-status-badge--dot-only`}
        style={{ background: preset.color }}
        title={title}
        role="status"
        aria-label={title}
      />
    );
  }

  return (
    <span className={className} style={{ color: preset.color }} role="status" aria-label={finalText}>
      {showIcon && <span className="zy-status-badge__icon">{preset.icon}</span>}
      {showText && <span className="zy-status-badge__text">{finalText}</span>}
    </span>
  );
};

export default StatusBadge;
```

## 3. CSS

```css
/* frontend/src/components/StatusBadge/StatusBadge.css */
.mk-status-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--mk-space-1);
  font-weight: var(--mk-weight-medium);
  line-height: var(--mk-leading-tight);
}

.mk-status-badge--sm { font-size: var(--mk-text-xs); }
.mk-status-badge--md { font-size: var(--mk-text-sm); }

.mk-status-badge--dot-only {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: var(--mk-radius-full);
  vertical-align: middle;
}

.mk-status-badge__icon {
  display: inline-flex;
  align-items: center;
}
```

## 4. stories.tsx（Storybook）

```tsx
// frontend/src/components/StatusBadge/StatusBadge.stories.tsx
import type { Meta, StoryObj } from '@storybook/react';
import { StatusBadge } from './StatusBadge';

const meta: Meta<typeof StatusBadge> = {
  title: 'Components/StatusBadge',
  component: StatusBadge,
  parameters: { layout: 'centered' },
};
export default meta;
type Story = StoryObj<typeof StatusBadge>;

export const Default: Story = {
  args: { status: 'published' },
};

export const AllStatuses: Story = {
  render: () => (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      {(['draft','reviewed','published','synced','active','retired','rejected','pending','processing','error','missing_source','ai_candidate','success','warning','danger'] as const).map(s => (
        <StatusBadge key={s} status={s} />
      ))}
    </div>
  ),
};

export const DotOnlyMode: Story = {
  args: { status: 'success', dotOnly: true, title: '已通过' },
};

export const CustomText: Story = {
  args: { status: 'error', text: '发布失败' },
};
```

## 5. test.tsx（单元测试）

```tsx
// frontend/src/components/StatusBadge/StatusBadge.test.tsx
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { StatusBadge } from './StatusBadge';

describe('StatusBadge', () => {
  it('renders default status with icon and text', () => {
    render(<StatusBadge status="published" />);
    expect(screen.getByText('已发布')).toBeInTheDocument();
  });

  it('renders custom text when provided', () => {
    render(<StatusBadge status="error" text="发布失败" />);
    expect(screen.getByText('发布失败')).toBeInTheDocument();
  });

  it('throws when dotOnly without title', () => {
    expect(() => render(<StatusBadge status="success" dotOnly />)).toThrow();
  });

  it('renders dot-only with title for a11y', () => {
    render(<StatusBadge status="success" dotOnly title="已通过" />);
    expect(screen.getByRole('status')).toHaveAttribute('aria-label', '已通过');
  });

  it('warns and returns null for unknown status', () => {
    // @ts-expect-error testing invalid input
    const { container } = render(<StatusBadge status="invalid" />);
    expect(container.firstChild).toBeNull();
  });
});
```

## 6. index.ts

```typescript
// frontend/src/components/StatusBadge/index.ts
export { StatusBadge, default } from './StatusBadge';
export type { StatusBadgeProps, StatusKey } from './StatusBadge.types';
```

## 关键约束（不许变）

1. ✅ 必须 import [`StatusBadge.css`](frontend/src/components/StatusBadge/StatusBadge.css)，**禁止**用内联样式硬编码颜色
2. ✅ 必须用 `var(--mk-*)` token，不允许硬编码颜色
3. ✅ `dotOnly` 模式**强制**抛错若缺 `title`
4. ✅ 必须有 `role="status"` 和 `aria-label`（无障碍）
5. ✅ 未知 status 必须 `console.warn` 而非 silent fail
6. ✅ Storybook 必须 ≥ 3 个 story（Default、AllStatuses、变体）
7. ✅ 单元测试覆盖率 ≥ 80%（含 a11y、错误处理、自定义文字）

## 其它 12 个组件同此模式

| 组件 | 用途 | 文件结构 | 关键约束 |
|---|---|---|---|
| SourceInfo | 来源信息条 | 同上 5 文件 | `missing` 状态橙红警告底 |
| AiBadge | AI 候选标识 | 同上 5 文件 | 置信度颜色按 §2.1 ai-confidence-* |
| OrgContextSelector | 组织选择器 | 同上 5 文件 | 切换后 invalidate react-query |
| TracedCard | 带 traceId 卡片 | 同上 5 文件 | 鼠标悬浮显示 traceId |
| DangerConfirm | 危险确认 | 同上 5 文件 | high 级二次确认动画延迟 |
| StepWizard | 多步向导 | 同上 5 文件 | 草稿保存到 localStorage |
| EmptyState | 空状态 | 同上 5 文件 | title 必填中文 |
| ErrorState | 错误状态 | 同上 5 文件 | 具体错误描述 + retry 入口 |
| AuditTrail | 审计时间轴 | 同上 5 文件 | 支持 onLoadMore |
| RuleDslEditor | 规则 DSL 编辑 | 同上 5 文件 | 双模式可切换 |
| PathwayCanvas | 路径画布 | 同上 5 文件 | AntV X6 封装 |
| EmbeddedAlert | 嵌入预警条 | 同上 5 文件 | 高度 ≤ 80px |

详见 [`03_设计系统.md §4`](../../03_设计系统.md#4-13-个核心组件-api)。
