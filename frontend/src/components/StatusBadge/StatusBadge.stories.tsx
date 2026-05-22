import type { Meta, StoryObj } from '@storybook/react';
import { StatusBadge } from './StatusBadge';
import type { StatusKey } from './StatusBadge.types';
import styles from './statusBadge.stories.module.css';

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
    <div className={styles.columnStack}>
      {ALL_STATUSES.map((s) => (
        <StatusBadge key={s} status={s} />
      ))}
    </div>
  ),
};

export const Sizes: Story = {
  render: () => (
    <div className={styles.columnStackWide}>
      <div>
        <strong>sm:</strong>
        <div className={styles.rowWrap}>
          {ALL_STATUSES.map((s) => (
            <StatusBadge key={s} status={s} size="sm" />
          ))}
        </div>
      </div>
      <div>
        <strong>md:</strong>
        <div className={styles.rowWrap}>
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
    <div className={styles.dotRow}>
      {ALL_STATUSES.map((s) => (
        <StatusBadge key={s} status={s} dotOnly title={s} />
      ))}
    </div>
  ),
};
