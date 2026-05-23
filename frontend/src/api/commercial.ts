import { get, post } from "./client";

// ==================== 类型定义 ====================

export interface LicenseInfo {
  licensee: string;
  license_type: string;
  tier: string;
  issued_date: string;
  expiry_date: string;
  days_remaining: number;
  status: "VALID" | "EXPIRING_SOON" | "EXPIRED";
  features: FeatureAvailability[];
  degradation_mode: boolean;
}

export interface FeatureAvailability {
  feature_key: string;
  feature_name: string;
  available: boolean;
  tier_required: string;
}

export interface UpdateLicenseRequest {
  license_key: string;
}

export interface UsageSummary {
  active_users: number;
  max_users: number;
  api_calls_this_month: number;
  features_used: number;
  total_features: number;
  days_remaining: number;
}

export interface ApiCallCount {
  endpoint: string;
  call_count: number;
  last_called: string;
}

export interface FeatureUsage {
  feature_key: string;
  feature_name: string;
  usage_count: number;
  last_used: string;
}

export interface UsageReport {
  summary: UsageSummary;
  api_calls: ApiCallCount[];
  feature_usage: FeatureUsage[];
}

// ==================== License API ====================

export async function getLicenseInfo(): Promise<LicenseInfo> {
  return get<LicenseInfo>("/commercial/license");
}

export async function updateLicense(params: UpdateLicenseRequest): Promise<LicenseInfo> {
  return post<LicenseInfo>("/commercial/license/update", params);
}

// ==================== Usage API ====================

export async function getUsageReport(): Promise<UsageReport> {
  return get<UsageReport>("/commercial/usage/report");
}

export async function exportUsageReport(): Promise<Blob> {
  const resp = await fetch("/medkernel/api/commercial/usage/export", {
    headers: {
      Authorization: `Bearer ${localStorage.getItem("medkernel_token") || ""}`,
    },
  });
  return resp.blob();
}
