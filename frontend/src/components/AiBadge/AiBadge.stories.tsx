import type { Meta, StoryObj } from '@storybook/react';
import AiBadge from './AiBadge';
import type { AiBadgeProps } from './AiBadge.types';
import styles from './aiBadge.stories.module.css';

const meta: Meta<AiBadgeProps> = {
  title: 'Components/AiBadge',
  component: AiBadge,
  tags: ['autodocs'],
  argTypes: {
    confidence: { control: { type: 'range', min: 0, max: 100 } },
    model: { control: 'text' },
    generatedAt: { control: 'text' },
    reviewStatus: {
      control: { type: 'select' },
      options: ['pending', 'accepted', 'rejected', 'modified'],
    },
    variant: {
      control: { type: 'radio' },
      options: ['badge', 'card'],
    },
    onAccept: { action: 'accept' },
    onModify: { action: 'modify' },
    onReject: { action: 'reject' },
  },
};

export default meta;
type Story = StoryObj<AiBadgeProps>;

export const BadgeVariant: Story = {
  render: () => (
    <div className={styles.badgeRow}>
      <AiBadge
        confidence={92}
        model="GPT-4o"
        generatedAt="2025-06-01 10:30"
        reviewStatus="accepted"
        variant="badge"
      />
      <AiBadge
        confidence={68}
        model="Claude-3"
        generatedAt="2025-06-01 11:00"
        reviewStatus="pending"
        variant="badge"
      />
      <AiBadge
        confidence={42}
        model="Gemini-1.5"
        generatedAt="2025-06-01 12:00"
        reviewStatus="rejected"
        variant="badge"
      />
    </div>
  ),
};

export const CardVariant: Story = {
  render: () => (
    <AiBadge
      confidence={85}
      model="GPT-4o"
      generatedAt="2025-06-01 10:30"
      reviewStatus="accepted"
      variant="card"
      onAccept={() => {}}
      onModify={() => {}}
      onReject={() => {}}
    />
  ),
};

export const PendingReview: Story = {
  render: () => (
    <AiBadge
      confidence={72}
      model="Claude-3"
      generatedAt="2025-06-01 11:00"
      reviewStatus="pending"
      variant="card"
      onAccept={() => {}}
      onModify={() => {}}
      onReject={() => {}}
    />
  ),
};
