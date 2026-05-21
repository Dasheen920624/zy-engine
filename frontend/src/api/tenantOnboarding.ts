import { post } from "./client";

export type TenantPlanCode = "TRIAL" | "STANDARD" | "PROFESSIONAL" | "ENTERPRISE";

export interface TenantInfoInput {
  companyName: string;
  tenantCode: string;
  companyType: string;
  licenseNumber?: string;
  province?: string;
  city?: string;
  address?: string;
  contactName: string;
  contactTitle?: string;
  contactPhone: string;
  contactEmail: string;
}

export interface TenantSubscriptionInput {
  licenseType: TenantPlanCode;
  expectedUsers: string;
  businessNeeds?: string;
}

export interface TenantInitDataInput {
  defaultPackageCodes: string[];
  ssoProviderTypes: string[];
  dataLocalizationConfirmed: boolean;
  smsNotificationEnabled: boolean;
}

export interface TenantOnboardingSubmitInput
  extends TenantInfoInput,
    TenantSubscriptionInput,
    TenantInitDataInput {}

export interface TenantApplication {
  id: number;
  applicationCode: string;
  companyName: string;
  companyType: string;
  contactName: string;
  contactPhone: string;
  contactEmail: string;
  contactTitle?: string;
  province?: string;
  city?: string;
  address?: string;
  licenseNumber?: string;
  licenseType: TenantPlanCode;
  expectedUsers?: string;
  businessNeeds?: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  tenantId?: string;
  createdTime?: string;
  reviewedTime?: string;
}

export interface TenantInvitation {
  id: number;
  invitationCode: string;
  tenantId: string;
  email: string;
  phone?: string;
  invitedBy?: string;
  roleCode: string;
  status: "PENDING" | "ACCEPTED" | "EXPIRED";
  expireTime?: string;
  createdTime?: string;
}

export interface TenantOnboardingResult {
  application: TenantApplication;
  invitation?: TenantInvitation;
  adminUsername: string;
  smsNotificationStatus: "PLANNED" | "SENT" | "DISABLED";
}

export async function submitTenantApplication(
  input: TenantOnboardingSubmitInput,
): Promise<TenantApplication> {
  return post<TenantApplication>("/tenant/onboarding/applications", input);
}

export async function approveTenantApplication(
  applicationCode: string,
): Promise<TenantApplication> {
  return post<TenantApplication>(`/tenant/onboarding/applications/${applicationCode}/review`, {
    action: "APPROVE",
    reviewedBy: "platform-admin",
    comment: "PR-FINAL-10 onboarding wizard approval",
  });
}

export async function sendTenantAdminInvitation(input: {
  tenantId: string;
  email: string;
  phone?: string;
  invitedBy?: string;
}): Promise<TenantInvitation> {
  return post<TenantInvitation>("/tenant/onboarding/invitations", {
    tenantId: input.tenantId,
    email: input.email,
    phone: input.phone,
    invitedBy: input.invitedBy || "platform-admin",
    roleCode: "TENANT_ADMIN",
  });
}
