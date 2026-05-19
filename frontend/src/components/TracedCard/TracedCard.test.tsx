import { describe, expect, it, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ConfigProvider } from 'antd';
import TracedCard from './TracedCard';

function renderWithProvider(ui: React.ReactElement) {
  return render(<ConfigProvider>{ui}</ConfigProvider>);
}

const defaultProps = {
  traceId: 'trace-test-123',
  timestamp: '2026-05-19T10:30:00Z',
  children: '卡片内容',
};

describe('TracedCard', () => {
  it('renders default variant', () => {
    const { container } = renderWithProvider(
      <TracedCard {...defaultProps} variant="default" />,
    );
    const card = container.querySelector('.ant-card');
    expect(card).toBeInTheDocument();
    expect(screen.getByText('卡片内容')).toBeInTheDocument();
  });

  it('renders highlight variant with left border', () => {
    const { container } = renderWithProvider(
      <TracedCard {...defaultProps} variant="highlight" />,
    );
    const card = container.querySelector('.ant-card') as HTMLElement;
    expect(card).toBeInTheDocument();
    expect(card.style.borderLeft).toBe('3px solid var(--mk-brand-primary)');
  });

  it('shows traceId info when showTraceByDefault is true', () => {
    renderWithProvider(
      <TracedCard {...defaultProps} showTraceByDefault apiPath="/api/test" />,
    );
    expect(screen.getByText(/trace-test-123/)).toBeInTheDocument();
    expect(screen.getByText(/\/api\/test/)).toBeInTheDocument();
  });

  it('hides traceId info when showTraceByDefault is false', () => {
    renderWithProvider(
      <TracedCard {...defaultProps} showTraceByDefault={false} />,
    );
    expect(screen.queryByText(/trace-test-123/)).not.toBeInTheDocument();
  });

  it('shows action icons on hover', () => {
    const { container } = renderWithProvider(
      <TracedCard {...defaultProps} />,
    );
    const card = container.querySelector('.ant-card') as HTMLElement;
    const actions = container.querySelector('.traced-card-actions') as HTMLElement;
    expect(actions.style.opacity).toBe('0');

    fireEvent.mouseEnter(card);
    expect(actions.style.opacity).toBe('1');

    fireEvent.mouseLeave(card);
    expect(actions.style.opacity).toBe('0');
  });

  it('copies traceId to clipboard on copy icon click', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.assign(navigator, { clipboard: { writeText } });

    const { container } = renderWithProvider(
      <TracedCard {...defaultProps} />,
    );

    const copyIcon = container.querySelector('.anticon-copy') as HTMLElement;
    fireEvent.click(copyIcon);

    expect(writeText).toHaveBeenCalledWith('trace-test-123');
  });
});
