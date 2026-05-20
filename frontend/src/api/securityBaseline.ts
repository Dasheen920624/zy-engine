import { get, post } from "./client";

// ==================== 类型定义 ====================

export interface AuditChainStatus {
  total_records: number;
  last_record_hash: string;
  hash_algorithm: string;
  is_genesis: boolean;
}

export interface AuditChainVerifyResult {
  total: number;
  passed: number;
  failed: number;
  chain_intact: boolean;
  last_record_hash: string;
  failures: Array<{
    chain_id: number;
    audit_log_id: number;
    error: string;
    expected?: string;
    actual?: string;
  }>;
}

export interface KeyVersion {
  key_id: number;
  key_alias: string;
  key_hash: string;
  algorithm: string;
  status: "ACTIVE" | "GRACE" | "RETIRED" | "REVOKED";
  created_at: string;
  activated_at: string;
  retired_at?: string;
  revoked_at?: string;
  grace_period_hours: number;
  rotated_by: string;
}

export interface SecurityBaselineStatus {
  jwt_algorithm: string;
  active_key_id: number;
  total_key_versions: number;
  grace_keys: number;
  retired_keys: number;
  revoked_keys: number;
  password_hash_algorithm: string;
  tls_min_version: string;
  hsts_enabled: boolean;
  sbom_format: string;
  audit_chain: AuditChainStatus;
}

export interface VulnerabilityScanResult {
  scan_id: string;
  scan_type: string;
  scanned_at: string;
  total_dependencies: number;
  critical_count: number;
  high_count: number;
  medium_count: number;
  low_count: number;
  findings: Array<{
    dependency: string;
    version: string;
    severity: string;
    cve: string;
    recommendation: string;
  }>;
}

// ==================== 审计链 API ====================

export async function getAuditChainStatus(): Promise<AuditChainStatus> {
  return get<AuditChainStatus>("/security/audit-chain/status");
}

export async function verifyAuditChain(): Promise<AuditChainVerifyResult> {
  return post<AuditChainVerifyResult>("/security/audit-chain/verify", {});
}

// ==================== 密钥轮换 API ====================

export async function listKeyVersions(): Promise<KeyVersion[]> {
  return get<KeyVersion[]>("/security/keys");
}

export async function getActiveKey(): Promise<KeyVersion> {
  return get<KeyVersion>("/security/keys/active");
}

export async function rotateKey(params: {
  key_alias?: string;
  key_material: string;
  rotated_by?: string;
}): Promise<KeyVersion> {
  return post<KeyVersion>("/security/keys/rotate", params);
}

export async function revokeKey(
  keyId: number,
  params: { revoked_by: string },
): Promise<KeyVersion> {
  return post<KeyVersion>(`/security/keys/${keyId}/revoke`, params);
}

// ==================== 安全基线 API ====================

export async function getSecurityBaseline(): Promise<SecurityBaselineStatus> {
  return get<SecurityBaselineStatus>("/security/baseline");
}

export async function performVulnerabilityScan(): Promise<VulnerabilityScanResult> {
  return post<VulnerabilityScanResult>("/security/vulnerability-scan", {});
}
