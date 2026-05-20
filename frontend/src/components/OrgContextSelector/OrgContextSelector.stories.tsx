import type { Meta, StoryObj } from '@storybook/react';
import { OrgContextSelector } from './OrgContextSelector';
import type { OrgContext } from './OrgContextSelector.types';

const meta: Meta<typeof OrgContextSelector> = {
  title: 'Components/OrgContextSelector',
  component: OrgContextSelector,
  tags: ['autodocs'],
  argTypes: {
    variant: { control: 'radio', options: ['inline', 'dropdown'] },
    level: { control: 'radio', options: ['hospital', 'department'] },
  },
};

export default meta;
type Story = StoryObj<typeof OrgContextSelector>;

const baseCurrent: OrgContext = {
  tenantId: 'TENANT_001',
  groupCode: 'GROUP_A',
  hospitalCode: 'HOSP_001',
};

const baseScopes: OrgContext[] = [
  {
    tenantId: 'TENANT_001',
    groupCode: 'GROUP_A',
    hospitalCode: 'HOSP_001',
  },
  {
    tenantId: 'TENANT_001',
    groupCode: 'GROUP_A',
    hospitalCode: 'HOSP_002',
  },
  {
    tenantId: 'TENANT_001',
    groupCode: 'GROUP_B',
    hospitalCode: 'HOSP_003',
  },
];

export const DropdownVariant: Story = {
  args: {
    current: baseCurrent,
    allowedScopes: baseScopes,
    onChange: (_next: OrgContext) => { /* onChange */ },
    level: 'hospital',
    variant: 'dropdown',
  },
};

export const InlineVariant: Story = {
  args: {
    current: baseCurrent,
    allowedScopes: baseScopes,
    onChange: (_next: OrgContext) => { /* onChange */ },
    level: 'hospital',
    variant: 'inline',
  },
};

const multiHospitalScopes: OrgContext[] = [
  {
    tenantId: 'TENANT_001',
    groupCode: '华东集团',
    hospitalCode: '上海第一人民医院',
    campusCode: '虹口院区',
    siteCode: 'SITE_01',
    departmentCode: '心内科',
  },
  {
    tenantId: 'TENANT_001',
    groupCode: '华东集团',
    hospitalCode: '上海第一人民医院',
    campusCode: '虹口院区',
    siteCode: 'SITE_01',
    departmentCode: '神经外科',
  },
  {
    tenantId: 'TENANT_001',
    groupCode: '华东集团',
    hospitalCode: '上海第一人民医院',
    campusCode: '松江院区',
    siteCode: 'SITE_02',
    departmentCode: '骨科',
  },
  {
    tenantId: 'TENANT_001',
    groupCode: '华东集团',
    hospitalCode: '瑞金医院',
    campusCode: '总院',
    siteCode: 'SITE_03',
    departmentCode: '急诊科',
  },
  {
    tenantId: 'TENANT_001',
    groupCode: '华南集团',
    hospitalCode: '广州中山医院',
    campusCode: '主院区',
    siteCode: 'SITE_04',
    departmentCode: '呼吸科',
  },
];

export const MultiHospital: Story = {
  args: {
    current: multiHospitalScopes[0],
    allowedScopes: multiHospitalScopes,
    onChange: (_next: OrgContext) => { /* onChange */ },
    level: 'department',
    variant: 'dropdown',
  },
};
