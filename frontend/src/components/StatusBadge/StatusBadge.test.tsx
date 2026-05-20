import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { StatusBadge } from './StatusBadge';
import type { StatusKey } from './StatusBadge.types';

const STATUS_TEXT_MAP: Record<StatusKey, string> = {
  draft: '草稿',
  reviewed: '待发布',
  published: '已发布',
  synced: '已同步',
  active: '运行中',
  retired: '已下线',
  rejected: '已拒绝',
  pending: '待审核',
  processing: '处理中',
  error: '错误',
  missing_source: '来源缺失',
  ai_candidate: 'AI 候选',
  success: '成功',
  warning: '警告',
  danger: '危急',
};

describe('StatusBadge', () => {
  it('renders each status with correct default text', () => {
    const statuses = Object.keys(STATUS_TEXT_MAP) as StatusKey[];
    for (const status of statuses) {
      const { unmount } = render(<StatusBadge status={status} />);
      expect(screen.getByText(STATUS_TEXT_MAP[status])).toBeInTheDocument();
      unmount();
    }
  });

  it('renders custom text when provided', () => {
    render(<StatusBadge status="error" text="发布失败" />);
    expect(screen.getByText('发布失败')).toBeInTheDocument();
  });

  it('dotOnly mode does not display text', () => {
    const { container } = render(
      <StatusBadge status="success" dotOnly title="已通过" />,
    );
    expect(container.querySelector('.mk-status-badge--dot-only')).toBeTruthy();
    expect(screen.queryByText('成功')).not.toBeInTheDocument();
  });

  it('dotOnly mode has correct aria-label', () => {
    render(<StatusBadge status="success" dotOnly title="已通过" />);
    expect(screen.getByRole('status')).toHaveAttribute('aria-label', '已通过');
  });

  it('throws when dotOnly without title', () => {
    expect(() =>
      render(<StatusBadge status="success" dotOnly />),
    ).toThrow('[StatusBadge] dotOnly mode requires title prop');
  });

  it('applies correct size class for sm', () => {
    const { container } = render(<StatusBadge status="published" size="sm" />);
    expect(container.querySelector('.mk-status-badge--sm')).toBeTruthy();
  });

  it('applies correct size class for md', () => {
    const { container } = render(<StatusBadge status="published" size="md" />);
    expect(container.querySelector('.mk-status-badge--md')).toBeTruthy();
  });

  it('renders role="status" for accessibility', () => {
    render(<StatusBadge status="published" />);
    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('warns and returns null for unknown status', () => {
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});
    // @ts-expect-error testing invalid input
    const { container } = render(<StatusBadge status="invalid" />);
    expect(container.firstChild).toBeNull();
    expect(warnSpy).toHaveBeenCalledWith(
      '[StatusBadge] Unknown status: invalid',
    );
    warnSpy.mockRestore();
  });
});
