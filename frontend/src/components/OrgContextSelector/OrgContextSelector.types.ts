export interface OrgContext {
  tenantId: string;
  groupCode?: string;
  hospitalCode: string;
  campusCode?: string;
  siteCode?: string;
  departmentCode?: string;
}

export interface OrgContextSelectorProps {
  current: OrgContext;
  allowedScopes: OrgContext[];
  onChange: (next: OrgContext) => void;
  level?: 'hospital' | 'department';
  variant?: 'inline' | 'dropdown';
}
