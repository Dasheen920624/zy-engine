import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "./client";

/**
 * MedKernel v1.0 GA · React Query hooks（按业务域分组）。
 * 与后端 /api/v1/* 路由一一对应。
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
// 临床运行 · MPI 患者主索引
// ──────────────────────────────────────────
export function useMpiPatients(q?: string) {
  return useQuery({
    queryKey: ["mpi", "patients", q ?? ""],
    queryFn: async () => {
      const { data } = await apiClient.get("/clinical/mpi/patients", { params: { q } });
      return data as Array<{
        mpiId: string;
        maskedName: string;
        gender: string;
        age: number;
        idLast4: string;
        mergedCount: number;
        status: string;
      }>;
    },
  });
}

export function useMpiStats() {
  return useQuery({
    queryKey: ["mpi", "stats"],
    queryFn: async () =>
      (await apiClient.get("/clinical/mpi/stats")).data as Record<string, number>,
  });
}

// ──────────────────────────────────────────
// 试点准备 · 路径模板
// ──────────────────────────────────────────
export function usePathwayTemplates() {
  return useQuery({
    queryKey: ["pathway", "templates"],
    queryFn: async () =>
      (await apiClient.get("/tenant/pathways")).data as Array<{
        id: string;
        name: string;
        disease: string;
        department: string;
        nodes: number;
        status: string;
      }>,
  });
}

export function usePublishPathway() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => (await apiClient.post(`/tenant/pathways/${id}/publish`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["pathway", "templates"] }),
  });
}

// ──────────────────────────────────────────
// 试点准备 · 规则库
// ──────────────────────────────────────────
export function useRules() {
  return useQuery({
    queryKey: ["rules"],
    queryFn: async () =>
      (await apiClient.get("/tenant/rules")).data as Array<{
        id: string;
        name: string;
        category: string;
        severity: string;
        hits: number;
        status: string;
      }>,
  });
}

export interface RuleValidateInput {
  patientMpi: string;
  orderText: string;
}

export interface RuleHit {
  ruleId: string;
  ruleName: string;
  severity: string;
  source: string;
  suggestion: string;
}

export interface RuleValidateOutput {
  patientMpi: string;
  hitCount: number;
  hits: RuleHit[];
}

export function useRuleValidate() {
  return useMutation({
    mutationFn: async (input: RuleValidateInput) =>
      (await apiClient.post<RuleValidateOutput>("/tenant/rules/validate", input)).data,
  });
}

// ──────────────────────────────────────────
// 临床运行 · CDSS
// ──────────────────────────────────────────
export function useCdssAlerts() {
  return useQuery({
    queryKey: ["cdss", "alerts"],
    queryFn: async () =>
      (await apiClient.get("/clinical/cdss/alerts")).data as Array<{
        id: string;
        text: string;
        source: string;
        adoptionRate: number;
        status: string;
        doctor: string;
      }>,
  });
}

export function useCdssDecide() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({
      id,
      decision,
      reason,
    }: {
      id: string;
      decision: "adopt" | "reject";
      reason?: string;
    }) =>
      (
        await apiClient.post(`/clinical/cdss/alerts/${id}/${decision}`, null, {
          params: { reason },
        })
      ).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["cdss", "alerts"] }),
  });
}

// ──────────────────────────────────────────
// 合规运维 · 审计日志
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
// 高级工具 · LLM Gateway
// ──────────────────────────────────────────
export function useLlmProviders() {
  return useQuery({
    queryKey: ["llm", "providers"],
    queryFn: async () =>
      (await apiClient.get("/advanced/llm/providers")).data as Array<{
        id: string;
        name: string;
        local: boolean;
        healthy: boolean;
      }>,
  });
}

// ──────────────────────────────────────────
// 系统 · Health probe
// ──────────────────────────────────────────
export function useSystemRuntime() {
  return useQuery({
    queryKey: ["system", "runtime"],
    queryFn: async () => (await apiClient.get("/system/runtime")).data as Record<string, unknown>,
    refetchInterval: 30_000,
  });
}

// ──────────────────────────────────────────
// W5 · GA-EXT-01 医保 DRG 月更
// ──────────────────────────────────────────
export function useDrgRulesets() {
  return useQuery({
    queryKey: ["drg", "rulesets"],
    queryFn: async () =>
      (await apiClient.get("/quality/insurance/drg/rulesets")).data as Array<{
        version: string;
        effectiveFrom: string;
        groupCount: number;
        source: string;
        status: string;
      }>,
  });
}

export function useDrgSync() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async () => (await apiClient.post("/quality/insurance/drg/sync")).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["drg"] }),
  });
}

// ──────────────────────────────────────────
// W5 · GA-EXT-14 AI 可解释性
// ──────────────────────────────────────────
export interface LlmExplainSource {
  type: string;
  title: string;
  anchor: string;
  publishedAt: string;
}

export interface LlmExplain {
  decisionId: string;
  shortAnswer: string;
  confidence: number;
  confidenceBand: string;
  sources: LlmExplainSource[];
  trainingDataRange: string;
  aiModel: string;
  warning: string;
}

export function useLlmExplain(decisionId?: string) {
  return useQuery({
    queryKey: ["llm", "explain", decisionId ?? ""],
    queryFn: async () =>
      (await apiClient.get<LlmExplain>(`/advanced/llm/explain/${decisionId}`)).data,
    enabled: !!decisionId,
  });
}

// ──────────────────────────────────────────
// W5 · GA-EXT-21 国产化自检
// ──────────────────────────────────────────
export function useDomesticSnapshot() {
  return useQuery({
    queryKey: ["domestic", "snapshot"],
    queryFn: async () =>
      (await apiClient.get("/advanced/domestic/snapshot")).data as Record<string, unknown>,
    refetchInterval: 60_000,
  });
}
