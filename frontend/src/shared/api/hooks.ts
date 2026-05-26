import { useMutation, useQuery } from "@tanstack/react-query";

import { apiClient } from "./client";

/**
 * MedKernel v1.0 GA · React Query hooks（按业务域分组）。
 * 与后端 /api/v1/* 路由一一对应。
 *
 * GA-ENG-BASE-09 净化：删除 W3-W7 旧业务 hook，仅保留 engine/* 真接口、
 * compliance/audit/* 与 /security/me、/system/* 合法运行底座 hook，
 * 以及 GA-ENG-API-04 上线后接入的字典映射 hook（业务包装阶段会渐进新增其它 engine hook）。
 */

// ──────────────────────────────────────────
// 身份安全 · 当前用户权限画像
// ──────────────────────────────────────────
export interface SecurityProfile {
  userId: string;
  roles: Array<{
    code: string;
    displayName: string;
    source: string;
    scopeLevel: string | null;
    scopeCode: string | null;
  }>;
  permissions: Array<{
    code: string;
    displayName: string;
    risk: "LOW" | "MEDIUM" | "HIGH" | string;
  }>;
  menuKeys: string[];
  dataScope: {
    tenantId: string | null;
    groupId: string | null;
    hospitalId: string | null;
    campusId: string | null;
    siteId: string | null;
    departmentId: string | null;
    wardId: string | null;
    specialtyId: string | null;
  };
}

type SecurityProfileEnvelope = {
  data: SecurityProfile;
};

export function useSecurityProfile() {
  return useQuery({
    queryKey: ["security", "me"],
    queryFn: async () => {
      const response = await apiClient.get<SecurityProfileEnvelope>("/security/me");
      return response.data.data;
    },
    retry: false,
  });
}

// ──────────────────────────────────────────
// 合规运维 · 审计日志（BASE-04 已落地）
// ──────────────────────────────────────────
export type AuditEventRow = {
  id: string;
  eventId: string;
  occurredAt: string;
  user: string | null;
  action: string;
  actionCode: string;
  resourceType: string;
  resourceId: string;
  traceId: string | null;
  signature: string | null;
  status: string;
};

type AuditEventsEnvelope = {
  code: string;
  data: { items: AuditEventRow[]; nextCursor: string | null; hasNext: boolean };
};

type AuditSnapshotEnvelope = {
  code: string;
  data: AuditEventRow;
};

export function useAuditEvents() {
  return useQuery({
    queryKey: ["audit", "events"],
    queryFn: async () => {
      const resp = await apiClient.get<AuditEventsEnvelope>("/compliance/audit/events");
      return resp.data.data?.items ?? [];
    },
  });
}

export function useAuditSnapshot() {
  return useMutation({
    mutationFn: async (reason: string) => {
      const resp = await apiClient.post<AuditSnapshotEnvelope>("/compliance/audit/snapshot", null, {
        params: { reason },
      });
      return resp.data.data;
    },
  });
}

// ──────────────────────────────────────────
// 系统 · Health probe
// ──────────────────────────────────────────
export interface RuntimeFeatureFlag {
  key: string;
  displayName: string;
  enabled: boolean;
  risk: "LOW" | "MEDIUM" | "HIGH" | string;
  owner: string;
  description: string;
}

export interface RuntimeDependencyStatus {
  key: string;
  displayName: string;
  status: "UP" | "DEGRADED" | "DISABLED" | string;
  detail: string;
}

export interface RuntimeBackupReadiness {
  enabled: boolean;
  rpo: string;
  rto: string;
  backupScript: string;
  restoreScript: string;
  checksumPolicy: string;
}

export interface RuntimeDomesticProfile {
  targetOs: string;
  targetJdk: string;
  databaseVendors: string[];
  cryptoAlgorithms: string[];
  evidence: string;
}

export interface RuntimeOperationsSnapshot {
  serviceName: string;
  environment: string;
  deploymentMode: string;
  databaseDialect: string;
  migrationLocation: string;
  activeProfiles: string[];
  healthStatus: "UP" | "DOWN" | "OUT_OF_SERVICE" | "UNKNOWN" | string;
  featureFlags: RuntimeFeatureFlag[];
  dependencies: RuntimeDependencyStatus[];
  backup: RuntimeBackupReadiness;
  domesticProfile: RuntimeDomesticProfile;
  generatedAt: string;
}

type RuntimeOperationsEnvelope = {
  data: RuntimeOperationsSnapshot;
};

export function useRuntimeOperations() {
  return useQuery({
    queryKey: ["system", "operations"],
    queryFn: async () => {
      const response = await apiClient.get<RuntimeOperationsEnvelope>("/system/operations");
      return response.data.data;
    },
    refetchInterval: 30_000,
  });
}

export function useSystemRuntime() {
  return useQuery({
    queryKey: ["system", "runtime"],
    queryFn: async () => (await apiClient.get("/system/runtime")).data as Record<string, unknown>,
    refetchInterval: 30_000,
  });
}

// ──────────────────────────────────────────
// 字典映射 · GA-ENG-API-04 已上线（engine/terminology）
// ──────────────────────────────────────────
export interface TermMapping {
  id: number;
  tenantId: string;
  localTermId: number;
  standardTermId: number;
  sourceSystem: string;
  category: string;
  confidence: number;
  riskLevel: "LOW" | "MEDIUM" | "HIGH";
  status: "DRAFT" | "CONFIRMED" | "SUPERSEDED" | "ROLLED_BACK";
  evidenceText?: string;
  confirmedBy?: string;
  confirmedAt?: string;
  createdAt?: string;
  createdBy?: string;
  updatedAt?: string;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
  hasNext: boolean;
  totalEstimated: boolean;
}

export interface TerminologyMappingsParams {
  page?: number;
  size?: number;
  sort?: string;
  sourceSystem?: string;
  category?: string;
  status?: TermMapping["status"];
  keyword?: string;
}

export function useTerminologyMappings(params?: TerminologyMappingsParams) {
  return useQuery({
    queryKey: ["terminology", "mappings", params ?? {}],
    queryFn: async () => {
      const { data } = await apiClient.get<{ data: PageResponse<TermMapping> }>(
        "/engine/terminology/mappings",
        { params },
      );
      return data.data;
    },
  });
}
