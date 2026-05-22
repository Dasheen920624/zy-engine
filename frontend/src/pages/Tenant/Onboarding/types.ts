import type {
  TenantInfoInput,
  TenantInitDataInput,
  TenantOnboardingResult,
  TenantSubscriptionInput,
} from "../../../api/tenantOnboarding";

export interface OnboardingDraft {
  info?: TenantInfoInput;
  subscription?: TenantSubscriptionInput;
  initData?: TenantInitDataInput;
}

export type { TenantOnboardingResult };
