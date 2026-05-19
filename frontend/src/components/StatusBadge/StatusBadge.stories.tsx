import type { Meta, StoryObj } from '@storybook/react';
import { StatusBadge } from './StatusBadge';
import type { StatusKey } from './StatusBadge.types';

const meta: Meta<typeof StatusBadge> = {
  title: 'Components/StatusBadge',
  component: StatusBadge,
  parameters: { layout: 'centered' },
  argTypes: {
    status: {
      control: 'select',
      options: [
        'draft',
        'reviewed',
        'published',
        'synced',
        'active',
        'retired',
        'rejected',
        'pending',
        'processing',
        'error',
        'missing_source',
        'ai_candidate',
        'success',
        'warning',
        'danger',
      ] satisfies StatusKey[],
    },
    size: { control: 'radio', options: ['sm', 'md'] },
    showIcon: { control: 'boolean' },
    showText: { control: 'boolean' },
    dotOnly: { control: 'boolean' },
  },
  args: {
    size: 'md',
    showIcon: true,
    showText: true,
    dotOnly: false,
  },
};

export default meta;
type Story = StoryObj<typeof StatusBadge>;

const ALL_STATUSES: StatusKey[] = [
  'draft',
  'reviewed',
  'published',
  'synced',
  'active',
  'retired',
  'rejected',
  'pending',
  'processing',
  'error',
  'missing_source',
  'ai_candidate',
  'success',
  'warning',
  'danger',
];

export const AllStatuses: Story = {
  render: () => (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      {ALL_STATUSES.map((s) => (
        <StatusBadge key={s} status={s} />
      ))}
    </div>
  ),
};

export const Sizes: Story = {
  render: () => (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div>
        <strong>sm:</strong>
        <div style={{ display: 'flex', gap: 8, marginTop: 4 }}>
          {ALL_STATUSES.map((s) => (
            <StatusBadge key={s} status={s} size="sm" />
          ))}
        </div>
      </div>
      <div>
        <strong>md:</strong>
        <div style={{ display: 'flex', gap: 8, marginTop: 4 }}>
          {ALL_STATUSES.map((s) => (
            <StatusBadge key={s} status={s} size="md" />
          ))}
        </div>
      </div>
    </div>
  ),
};

export const DotOnly: Story = {
  render: () => (
    <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
      {ALL_STATUSES.map((s) => (
        <StatusBadge key={s} status={s} dotOnly title={s} />
      ))}
    </div>
  ),
};
