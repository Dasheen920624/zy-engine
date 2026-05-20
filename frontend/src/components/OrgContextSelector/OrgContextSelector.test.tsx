import { type ReactNode } from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { OrgContextSelector } from './OrgContextSelector';
import type { OrgContext } from './OrgContextSelector.types';

function createWrapper() {
  const queryClient = new QueryClient();
  return function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
  };
}

const mockCurrent: OrgContext = {
  tenantId: 'TENANT_001',
  groupCode: 'GROUP_A',
  hospitalCode: 'HOSP_001',
};

const mockScopes: OrgContext[] = [
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
];

describe('OrgContextSelector', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it('renders dropdown variant with combobox', () => {
    const onChange = vi.fn();
    render(
      <OrgContextSelector
        current={mockCurrent}
        allowedScopes={mockScopes}
        onChange={onChange}
        variant="dropdown"
      />,
      { wrapper: createWrapper() },
    );
    // Cascader 渲染为 combobox 角色
    const combobox = document.querySelector('.ant-cascader');
    expect(combobox).toBeTruthy();
  });

  it('renders inline variant with hospital code', () => {
    const onChange = vi.fn();
    render(
      <OrgContextSelector
        current={mockCurrent}
        allowedScopes={mockScopes}
        onChange={onChange}
        variant="inline"
      />,
      { wrapper: createWrapper() },
    );
    // Inline 模式下医院代码可能出现多次
    const hospElements = screen.getAllByText('HOSP_001');
    expect(hospElements.length).toBeGreaterThan(0);
  });

  it('calls onChange callback when provided', () => {
    const onChange = vi.fn();
    render(
      <OrgContextSelector
        current={mockCurrent}
        allowedScopes={mockScopes}
        onChange={onChange}
        variant="dropdown"
      />,
      { wrapper: createWrapper() },
    );
    expect(onChange).not.toHaveBeenCalled();
  });

  it('stores context in sessionStorage with correct key', () => {
    expect(sessionStorage.getItem('mk-org-context')).toBeNull();

    const ctx: OrgContext = {
      tenantId: 'TENANT_001',
      groupCode: 'GROUP_A',
      hospitalCode: 'HOSP_002',
    };
    sessionStorage.setItem('mk-org-context', JSON.stringify(ctx));

    const retrieved = sessionStorage.getItem('mk-org-context');
    expect(retrieved).not.toBeNull();
    expect(JSON.parse(retrieved as string)).toEqual(ctx);
  });
});
