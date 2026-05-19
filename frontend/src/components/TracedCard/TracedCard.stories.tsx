import type { Meta, StoryObj } from '@storybook/react';
import TracedCard from './TracedCard';
import type { TracedCardProps } from './TracedCard.types';

const meta: Meta<TracedCardProps> = {
  title: 'Components/TracedCard',
  component: TracedCard,
  tags: ['autodocs'],
  argTypes: {
    variant: {
      control: 'select',
      options: ['default', 'highlight'],
    },
    showTraceByDefault: {
      control: 'boolean',
    },
  },
  args: {
    traceId: 'trace-abc-123-def-456',
    apiPath: '/medkernel/api/v1/diagnose',
    timestamp: '2026-05-19T10:30:00Z',
    children: '这是卡片内容区域，可以放置任意子元素。',
    variant: 'default',
    showTraceByDefault: false,
  },
};

export default meta;
type Story = StoryObj<TracedCardProps>;

export const DefaultCard: Story = {
  args: {
    variant: 'default',
  },
};

export const HighlightCard: Story = {
  args: {
    variant: 'highlight',
  },
};

export const ShowTraceByDefault: Story = {
  args: {
    showTraceByDefault: true,
  },
};
