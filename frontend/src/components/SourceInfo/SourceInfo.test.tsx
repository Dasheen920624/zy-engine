import { describe, expect, it, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ConfigProvider } from 'antd';
import { SourceInfo } from './SourceInfo';
import type { SourceInfoProps } from './SourceInfo.types';

const baseProps: SourceInfoProps = {
  source: {
    documentName: '中国高血压防治指南',
    documentId: 'doc-001',
    section: '第三章',
    publishYear: 2023,
  },
  review: {
    status: 'reviewed',
    reviewerName: '张医生',
    reviewedAt: '2024-01-15',
  },
  version: '2.1.0',
};

function renderWithProvider(props: SourceInfoProps) {
  return render(
    <ConfigProvider>
      <SourceInfo {...props} />
    </ConfigProvider>,
  );
}

describe('SourceInfo', () => {
  it('renders inline variant', () => {
    renderWithProvider({ ...baseProps, variant: 'inline' });
    expect(screen.getByText('中国高血压防治指南')).toBeInTheDocument();
    expect(screen.getByText('已审核')).toBeInTheDocument();
  });

  it('renders card variant with citation', () => {
    renderWithProvider({
      ...baseProps,
      variant: 'card',
      citation: {
        id: 'cite-001',
        excerpt: '这是一段引用片段',
        pageNumber: 42,
      },
    });
    expect(screen.getByText('中国高血压防治指南')).toBeInTheDocument();
    expect(screen.getByText('这是一段引用片段')).toBeInTheDocument();
  });

  it('renders compact variant', () => {
    renderWithProvider({ ...baseProps, variant: 'compact' });
    expect(screen.getByText('中国高血压防治指南')).toBeInTheDocument();
    expect(screen.getByText('已审核')).toBeInTheDocument();
  });

  it('shows danger background when review status is missing', () => {
    const { container } = renderWithProvider({
      ...baseProps,
      review: { status: 'missing' },
      variant: 'inline',
    });
    const wrapper = container.firstElementChild as HTMLElement;
    expect(wrapper.style.background).toBe('var(--mk-danger-soft)');
  });

  it('does not show danger background for reviewed status', () => {
    const { container } = renderWithProvider({
      ...baseProps,
      variant: 'inline',
    });
    const wrapper = container.firstElementChild as HTMLElement;
    expect(wrapper.style.background).not.toBe('var(--mk-danger-soft)');
  });

  it('calls onClickDocument when clicking document name in card variant', () => {
    const handleClick = vi.fn();
    renderWithProvider({
      ...baseProps,
      variant: 'card',
      onClickDocument: handleClick,
    });

    fireEvent.click(screen.getByText('查看原文'));
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('calls onClickDocument when clicking document name in inline variant', () => {
    const handleClick = vi.fn();
    renderWithProvider({
      ...baseProps,
      variant: 'inline',
      onClickDocument: handleClick,
    });

    fireEvent.click(screen.getByText('中国高血压防治指南'));
    expect(handleClick).toHaveBeenCalledTimes(1);
  });
});
