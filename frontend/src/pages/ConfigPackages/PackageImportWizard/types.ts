import type {
  ImportUploadResult,
  ImportValidateResult,
  ImportSourceCheckResult,
  ImportImpactResult,
} from "@/api/types";

export interface WizardContext {
  uploadId: string;
  uploadResult: ImportUploadResult | null;
  validateResult: ImportValidateResult | null;
  sourceCheckResult: ImportSourceCheckResult | null;
  impactResult: ImportImpactResult | null;
  targetEnvironment: string;
  confirmPackageName: string;
  confirmReason: string;
}

export const INITIAL_WIZARD_CONTEXT: WizardContext = {
  uploadId: "",
  uploadResult: null,
  validateResult: null,
  sourceCheckResult: null,
  impactResult: null,
  targetEnvironment: "production",
  confirmPackageName: "",
  confirmReason: "",
};

export const WIZARD_STEPS = [
  { title: "上传", description: "上传配置包文件" },
  { title: "校验", description: "校验包完整性" },
  { title: "来源检查", description: "来源审核状态" },
  { title: "影响评估", description: "评估影响范围" },
  { title: "确认发布", description: "确认并发布" },
] as const;

export const DRAFT_STORAGE_KEY = "mk-import-wizard-draft";

export function loadDraft(): Partial<WizardContext> | null {
  try {
    const raw = sessionStorage.getItem(DRAFT_STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function saveDraft(ctx: WizardContext): void {
  try {
    sessionStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(ctx));
  } catch {
    // localStorage 满或不可用时静默忽略
  }
}

export function clearDraft(): void {
  try {
    sessionStorage.removeItem(DRAFT_STORAGE_KEY);
  } catch {
    // ignore
  }
}
