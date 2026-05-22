export interface EnvCheckItem {
  key: string;
  label: string;
  passed: boolean;
  detail?: string;
}

export interface DepartmentInput {
  code: string;
  name: string;
  type: string;
}

export interface WardInput {
  code: string;
  name: string;
  departmentCode: string;
  bedCount?: number;
}

export interface RoleInput {
  code: string;
  name: string;
  permissions: string[];
}

export interface ImplementationDraft {
  currentStep: number;
  envChecks?: Record<string, boolean>;
  departments?: DepartmentInput[];
  wards?: WardInput[];
  roles?: RoleInput[];
  selectedRulePackages?: string[];
  pathwayConfigs?: Record<string, unknown>;
  permissionTemplates?: string[];
  validationPassed?: boolean;
}

const STORAGE_KEY = "medkernel_impl_guide_draft";

export function loadDraft(): ImplementationDraft | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as ImplementationDraft) : null;
  } catch {
    return null;
  }
}

export function saveDraft(draft: ImplementationDraft) {
  // eslint-disable-next-line no-restricted-syntax -- 实施向导进度草稿，非敏感数据
  localStorage.setItem(STORAGE_KEY, JSON.stringify(draft));
}

export function clearDraft() {
  localStorage.removeItem(STORAGE_KEY);
}
