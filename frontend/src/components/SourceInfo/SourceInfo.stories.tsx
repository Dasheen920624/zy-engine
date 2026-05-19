import type { Meta, StoryObj } from '@storybook/react';
import { SourceInfo } from './SourceInfo';
import type { SourceInfoProps } from './SourceInfo.types';

const meta: Meta<SourceInfoProps> = {
  title: 'Components/SourceInfo',
  component: SourceInfo,
  tags: ['autodocs'],
  argTypes: {
    variant: {
      control: { type: 'inline-radio' },
      options: ['inline', 'card', 'compact'],
    },
  },
};

export default meta;
type Story = StoryObj<SourceInfoProps>;

export const InlineVariant: Story = {
  args: {
    source: {
      documentName: '中国高血压防治指南',
      documentId: 'doc-001',
      section: '第三章 降压药物治疗',
      publishYear: 2023,
    },
    review: {
      status: 'reviewed',
      reviewerName: '张医生',
      reviewedAt: '2024-01-15',
    },
    version: '2.1.0',
    variant: 'inline',
  },
};

export const CardVariant: Story = {
  args: {
    source: {
      documentName: '中国2型糖尿病防治指南',
      documentId: 'doc-002',
      section: '第四章 胰岛素治疗',
      publishYear: 2022,
    },
    citation: {
      id: 'cite-001',
      excerpt:
        '对于新诊断的2型糖尿病患者，若HbA1c≥9.0%且伴有明显高血糖症状，可考虑起始胰岛素强化治疗，以迅速缓解高血糖毒性、保护β细胞功能。',
      pageNumber: 45,
    },
    review: {
      status: 'reviewed',
      reviewerName: '李医生',
      reviewedAt: '2024-03-20',
    },
    version: '1.5.0',
    variant: 'card',
    onClickDocument: () => { /* 查看原文 */ },
  },
};

export const MissingSource: Story = {
  args: {
    source: {
      documentName: '急性心肌梗死诊断与治疗指南',
      documentId: 'doc-003',
    },
    review: {
      status: 'missing',
    },
    version: '0.1.0',
    variant: 'card',
  },
};
