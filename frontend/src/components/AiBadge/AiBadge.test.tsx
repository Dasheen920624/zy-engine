import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import AiBadge from './AiBadge';
import type { AiBadgeProps } from './AiBadge.types';

const baseProps: AiBadgeProps = {
  confidence: 85,
  model: 'GPT-4o',
  generatedAt: '2025-06-01 10:30',
  reviewStatus: 'accepted',
  onAccept: vi.fn(),
  onModify: vi.fn(),
  onReject: vi.fn(),
};

function getButtonByText(text: string): HTMLElement {
  const btn = screen.getAllByRole('button').find(
    (b) => b.textContent?.replace(/\s/g, '').includes(text),
  );
  if (!btn) throw new Error(`Button with text "${text}" not found`);
  return btn;
}

describe('AiBadge', () => {
  it('renders badge variant with AI label and confidence', () => {
    render(<AiBadge {...baseProps} variant="badge" />);

    expect(screen.getByText('AI 候选')).toBeInTheDocument();
    expect(screen.getByText('85%')).toBeInTheDocument();
  });

  it('renders card variant with action buttons', () => {
    render(<AiBadge {...baseProps} variant="card" />);

    expect(screen.getByText('AI 候选')).toBeInTheDocument();
    expect(getButtonByText('采纳')).toBeTruthy();
    expect(getButtonByText('修改')).toBeTruthy();
    expect(getButtonByText('拒绝')).toBeTruthy();
  });

  it('disables accept button when reviewStatus is pending', () => {
    render(<AiBadge {...baseProps} variant="card" reviewStatus="pending" />);

    expect(getButtonByText('采纳')).toBeDisabled();
  });

  it('does not disable accept button when reviewStatus is accepted', () => {
    render(<AiBadge {...baseProps} variant="card" reviewStatus="accepted" />);

    expect(getButtonByText('采纳')).not.toBeDisabled();
  });

  it('calls onAccept when accept button is clicked', () => {
    const onAccept = vi.fn();
    render(
      <AiBadge {...baseProps} variant="card" reviewStatus="accepted" onAccept={onAccept} />,
    );

    fireEvent.click(getButtonByText('采纳'));
    expect(onAccept).toHaveBeenCalledOnce();
  });

  it('calls onModify when modify button is clicked', () => {
    const onModify = vi.fn();
    render(<AiBadge {...baseProps} variant="card" onModify={onModify} />);

    fireEvent.click(getButtonByText('修改'));
    expect(onModify).toHaveBeenCalledOnce();
  });

  it('calls onReject when reject button is clicked', () => {
    const onReject = vi.fn();
    render(<AiBadge {...baseProps} variant="card" onReject={onReject} />);

    fireEvent.click(getButtonByText('拒绝'));
    expect(onReject).toHaveBeenCalledOnce();
  });

  it('applies high confidence color for confidence >= 80', () => {
    render(<AiBadge {...baseProps} confidence={92} variant="badge" />);

    const confidenceEl = screen.getByText('92%');
    expect(confidenceEl.className).toMatch(/confidenceHigh/);
  });

  it('applies mid confidence color for confidence 60-79', () => {
    render(<AiBadge {...baseProps} confidence={68} variant="badge" />);

    const confidenceEl = screen.getByText('68%');
    expect(confidenceEl.className).toMatch(/confidenceMid/);
  });

  it('applies low confidence color for confidence < 60', () => {
    render(<AiBadge {...baseProps} confidence={42} variant="badge" />);

    const confidenceEl = screen.getByText('42%');
    expect(confidenceEl.className).toMatch(/confidenceLow/);
  });
});
