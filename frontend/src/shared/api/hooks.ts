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
  traceId?: string;
  partial?: {
    successCount: number;
    failureCount: number;
    failures: Array<{ key: string; reason: string; retryable: boolean }>;
  };
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

// ──────────────────────────────────────────
// 规则引擎 · GA-ENG-API-05 & GA-ENG-RULE-01
// ──────────────────────────────────────────
export interface RuleDefinition {
  id: number;
  ruleId: string;
  tenantId: string;
  ruleCode: string;
  name: string;
  ruleType: "DRUG_SAFETY" | "INSURANCE_AUDIT" | "CLINICAL_QUALITY" | string;
  authoringMode: "DSL" | "VISUAL" | string;
  riskLevel: "LOW" | "MEDIUM" | "HIGH";
  status: "DRAFT" | "PUBLISHED" | "OFFLINE" | "ARCHIVED" | string;
  activeVersionId: string | null;
  packageVersion?: string | null;
  applicableOrgUnitId?: string | null;
  createdAt: string;
  createdBy: string;
  updatedAt: string;
}

export interface RuleVersion {
  id: number;
  versionId: string;
  ruleId: string;
  versionNo: number;
  sourceRef: string;
  changeSummary: string;
  dslJson: string;
  explanationJson: string;
  status: "DRAFT" | "PUBLISHED" | string;
  publishedAt?: string | null;
  publishedBy?: string | null;
  createdAt: string;
}

export interface RuleTestCase {
  id: number;
  caseId: string;
  ruleId: string;
  versionId: string;
  caseType: "POSITIVE" | "NEGATIVE" | "BOUNDARY" | "CONFLICT" | string;
  inputPayload: string;
  expectedHit: boolean;
  expectedSeverity: "LOW" | "MEDIUM" | "HIGH" | string;
  expectedActionCode: string;
  lastHit?: boolean | null;
  lastStatus?: "PASS" | "FAIL" | "PENDING" | string;
  lastMessage?: string | null;
  lastRunAt?: string | null;
  createdAt: string;
}

export interface RuleDetailResponse {
  definition: RuleDefinition;
  version: RuleVersion;
  testCases: RuleTestCase[];
}

export interface RuleEvaluationItem {
  ruleId: string;
  ruleCode: string;
  ruleName: string;
  hit: boolean;
  severity: "LOW" | "MEDIUM" | "HIGH" | string;
  actionCode: string;
  explanation: string;
}

export interface RuleEvaluateResponse {
  traceId: string;
  executionId: string;
  highestSeverity: "LOW" | "MEDIUM" | "HIGH" | string;
  items: RuleEvaluationItem[];
}

export interface DiagnoseResponse {
  executionId: string;
  traceId: string;
  ruleId: string;
  inputPayloadSummary: string;
  explanationSnapshot: string;
  confidenceScore?: number;
  riskLevel?: "LOW" | "MEDIUM" | "HIGH";
  statusHistory: Array<{
    status: string;
    changedAt: string;
    changedBy: string;
    summary: string;
  }>;
}

export interface RuleFilterParams {
  status?: string;
  ruleType?: string;
  riskLevel?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export function useRuleDefinitions(params?: RuleFilterParams) {
  return useQuery({
    queryKey: ["rules", "definitions", params ?? {}],
    queryFn: async () => {
      const { data } = await apiClient.get<{ data: PageResponse<RuleDefinition> }>(
        "/engine/rules",
        { params },
      );
      return data.data;
    },
  });
}

export function useRuleDetail(ruleId: string) {
  return useQuery({
    queryKey: ["rules", "detail", ruleId],
    queryFn: async () => {
      if (!ruleId) return null;
      const { data } = await apiClient.get<{ data: RuleDetailResponse }>(`/engine/rules/${ruleId}`);
      return data.data;
    },
    enabled: !!ruleId,
  });
}

export function useCreateRule() {
  return useMutation({
    mutationFn: async (payload: {
      ruleCode: string;
      name: string;
      ruleType: string;
      authoringMode: string;
      riskLevel: string;
      sourceRef: string;
      changeSummary: string;
      dslJson: string;
      explanationJson: string;
    }) => {
      const { data } = await apiClient.post<{ data: { ruleId: string } }>("/engine/rules", payload);
      return data.data;
    },
  });
}

export function useAddTestCase(ruleId: string) {
  return useMutation({
    mutationFn: async (payload: {
      caseType: string;
      inputPayload: string;
      expectedHit: boolean;
      expectedSeverity: string;
      expectedActionCode: string;
    }) => {
      const { data } = await apiClient.post<{ data: RuleTestCase }>(
        `/engine/rules/${ruleId}/test-cases`,
        payload,
      );
      return data.data;
    },
  });
}

export function useSimulateRule(ruleId: string) {
  return useMutation({
    mutationFn: async (payload: { inputPayload: string }) => {
      const { data } = await apiClient.post<{ data: RuleEvaluationItem }>(
        `/engine/rules/${ruleId}/simulate`,
        payload,
      );
      return data.data;
    },
  });
}

export function usePublishRule() {
  return useMutation({
    mutationFn: async (ruleId: string) => {
      const { data } = await apiClient.post<{ data: { versionId: string } }>(
        `/engine/rules/${ruleId}/publish`,
      );
      return data.data;
    },
  });
}

export function useEvaluateRules() {
  return useMutation({
    mutationFn: async (payload: { triggerPoint: string; patientId?: string; payloadJson: string }) => {
      const { data } = await apiClient.post<{ data: RuleEvaluateResponse }>(
        "/engine/rules/evaluate",
        payload,
      );
      return data.data;
    },
  });
}

export function useRuleExecutionDiagnose(executionId: string) {
  return useQuery({
    queryKey: ["rules", "diagnose", executionId],
    queryFn: async () => {
      if (!executionId) return null;
      const { data } = await apiClient.get<{ data: DiagnoseResponse }>(
        `/engine/rules/executions/${executionId}/diagnose`,
      );
      return data.data;
    },
    enabled: !!executionId,
  });
}
