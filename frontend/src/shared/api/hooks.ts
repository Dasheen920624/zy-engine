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
    mutationFn: async (payload: {
      triggerPoint: string;
      patientId?: string;
      payloadJson: string;
    }) => {
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

// ==================== Pathway 引擎相关的实体及 DTO 契约 ====================

export type SpecialtyPackageStatus = "DRAFT" | "PUBLISHED" | "OFFLINE";
export type PathwayTemplateStatus = "DRAFT" | "PUBLISHED" | "OFFLINE";
export type PathwayTemplateLevel = "CLINICAL" | "BUSINESS" | string;
export type PathwayNodeType = "START" | "PROCESS" | "BRANCH" | "STOP" | string;
export type PathwayEdgeType = "STANDARD" | "CONDITIONAL" | "EXCEPTION" | "VARIANCE" | string;
export type PatientPathwayStatus = "ACTIVE" | "COMPLETED" | "EXITED" | string;
export type ClinicalClockStatus = "RUNNING" | "COMPLETED" | "OVERDUE" | string;
export type VarianceType =
  | "MEDICAL"
  | "PATIENT_REASON"
  | "RESOURCE_REASON"
  | "DOCTOR_CHOICE"
  | "SYSTEM_REASON";
export type PathwayAdvanceEventType = "COMPLETE" | "VARIANCE" | "EXIT";

export interface SpecialtyPackage {
  id?: number;
  packageId: string;
  packageCode: string;
  diseaseCode: string;
  name: string;
  packageVersion: string;
  status: SpecialtyPackageStatus;
  sourceRef: string;
  description: string;
  publishedAt?: string;
  publishedBy?: string;
  createdAt?: string;
  createdBy?: string;
  traceId?: string;
}

export interface PathwayTemplate {
  id?: number;
  templateId: string;
  packageId: string;
  templateCode: string;
  name: string;
  diseaseCode: string;
  templateVersion: number;
  templateLevel: PathwayTemplateLevel;
  status: PathwayTemplateStatus;
  startNodeCode?: string;
  sourceRef: string;
  description: string;
  entryCriteriaJson?: string;
  exitCriteriaJson?: string;
  createdAt?: string;
  createdBy?: string;
  traceId?: string;
}

export interface PathwayNode {
  id?: number;
  nodeId: string;
  templateId: string;
  nodeCode: string;
  name: string;
  nodeType: PathwayNodeType;
  sortOrder: number;
  responsibleRole?: string;
  dependencyJson?: string;
  timeWindowMinutes?: number;
  terminalFlag: boolean;
  configJson?: string;
  createdAt?: string;
  traceId?: string;
}

export interface PathwayEdge {
  id?: number;
  edgeId: string;
  templateId: string;
  edgeCode: string;
  fromNodeCode: string;
  toNodeCode: string;
  edgeType: PathwayEdgeType;
  conditionJson?: string;
  priority: number;
  createdAt?: string;
  traceId?: string;
}

export interface SpecialtyMetricBinding {
  id?: number;
  bindingId: string;
  templateId: string;
  nodeCode: string;
  metricCode: string;
  createdAt?: string;
}

export interface SpecialtyPackageResponse {
  packageId: string;
  status: SpecialtyPackageStatus;
  traceId: string;
}

export interface PathwayTemplateDetailResponse {
  template: PathwayTemplate;
  nodes: PathwayNode[];
  edges: PathwayEdge[];
  metricBindings: SpecialtyMetricBinding[];
  traceId: string;
}

export interface PathwayTemplatePublishResponse {
  templateId: string;
  status: PathwayTemplateStatus;
  traceId: string;
}

export interface PathwaySimulationResponse {
  templateId: string;
  simulatedPath: string[];
  traceId: string;
}

export interface PatientPathway {
  id?: number;
  patientPathwayId: string;
  patientId: string;
  encounterId?: string;
  templateId: string;
  currentNodeCode?: string;
  status: PatientPathwayStatus;
  enteredAt?: string;
  completedAt?: string;
  exitedAt?: string;
  exitReason?: string;
  lastEventId?: string;
  createdAt?: string;
  traceId?: string;
}

export interface PathwayVariance {
  id?: number;
  varianceId: string;
  patientPathwayId: string;
  nodeCode: string;
  varianceType: VarianceType;
  reason: string;
  resolutionAction: string;
  continueNodeCode?: string;
  createdAt?: string;
  traceId?: string;
}

export interface ClinicalClock {
  id?: number;
  clockId: string;
  patientPathwayId: string;
  nodeCode: string;
  metricCode?: string;
  startedAt: string;
  dueAt: string;
  completedAt?: string;
  status: ClinicalClockStatus;
  createdAt?: string;
  traceId?: string;
}

export interface PatientPathwayDetailResponse {
  patientPathway: PatientPathway;
  variances: PathwayVariance[];
  clocks: ClinicalClock[];
  traceId: string;
}

export interface PathwayAdvanceResponse {
  patientPathwayId: string;
  status: PatientPathwayStatus;
  traceId: string;
}

// 1. SpecialtyPackage Hooks
export function useSpecialtyPackages(params?: { page?: number; size?: number; sort?: string }) {
  return useQuery({
    queryKey: ["pathways", "packages", params ?? {}],
    queryFn: async () => {
      const { data } = await apiClient.get<{ data: PageResponse<SpecialtyPackage> }>(
        "/engine/pathways/packages",
        { params },
      );
      return data.data;
    },
  });
}

export function useCreateSpecialtyPackage() {
  return useMutation({
    mutationFn: async (payload: {
      packageCode: string;
      diseaseCode: string;
      name: string;
      packageVersion: string;
      sourceRef: string;
      description: string;
    }) => {
      const { data } = await apiClient.post<{ data: SpecialtyPackageResponse }>(
        "/engine/pathways/packages",
        payload,
      );
      return data.data;
    },
  });
}

// 2. PathwayTemplate Hooks
export function usePathwayTemplates(params?: {
  status?: PathwayTemplateStatus;
  diseaseCode?: string;
  packageId?: string;
  page?: number;
  size?: number;
  sort?: string;
}) {
  return useQuery({
    queryKey: ["pathways", "templates", params ?? {}],
    queryFn: async () => {
      const { data } = await apiClient.get<{ data: PageResponse<PathwayTemplate> }>(
        "/engine/pathways/templates",
        { params },
      );
      return data.data;
    },
  });
}

export function usePathwayTemplateDetail(templateId: string) {
  return useQuery({
    queryKey: ["pathways", "template-detail", templateId],
    queryFn: async () => {
      if (!templateId) return null;
      const { data } = await apiClient.get<{ data: PathwayTemplateDetailResponse }>(
        `/engine/pathways/templates/${templateId}`,
      );
      return data.data;
    },
    enabled: !!templateId,
  });
}

export function useCreatePathwayTemplate() {
  return useMutation({
    mutationFn: async (payload: {
      packageId: string;
      templateCode: string;
      name: string;
      diseaseCode: string;
      templateLevel: PathwayTemplateLevel;
      sourceRef: string;
      description: string;
      entryCriteriaJson?: string;
      exitCriteriaJson?: string;
      nodes: Array<{
        nodeCode: string;
        name: string;
        nodeType: PathwayNodeType;
        sortOrder: number;
        responsibleRole?: string;
        timeWindowMinutes?: number;
        terminalFlag: boolean;
        configJson?: string;
      }>;
      edges: Array<{
        edgeCode: string;
        fromNodeCode: string;
        toNodeCode: string;
        edgeType: PathwayEdgeType;
        conditionJson?: string;
        priority: number;
      }>;
      metricBindings?: Array<{
        nodeCode: string;
        metricCode: string;
      }>;
    }) => {
      const { data } = await apiClient.post<{ data: PathwayTemplateDetailResponse }>(
        "/engine/pathways/templates",
        payload,
      );
      return data.data;
    },
  });
}

export function usePublishPathwayTemplate() {
  return useMutation({
    mutationFn: async (templateId: string) => {
      const { data } = await apiClient.post<{ data: PathwayTemplatePublishResponse }>(
        `/engine/pathways/templates/${templateId}/publish`,
      );
      return data.data;
    },
  });
}

export function useSimulatePathway(templateId: string) {
  return useMutation({
    mutationFn: async (payload: { startNodeCode?: string; contextJson?: string }) => {
      const { data } = await apiClient.post<{ data: PathwaySimulationResponse }>(
        `/engine/pathways/templates/${templateId}/simulate`,
        payload,
      );
      return data.data;
    },
  });
}

// 3. PatientPathway Hooks
export function useEnterPatientPathway() {
  return useMutation({
    mutationFn: async (payload: {
      patientId: string;
      encounterId?: string;
      templateId: string;
      startNodeCode?: string;
    }) => {
      const { data } = await apiClient.post<{ data: PatientPathwayDetailResponse }>(
        "/engine/pathways/patients",
        payload,
      );
      return data.data;
    },
  });
}

export function usePatientPathwayDetail(patientPathwayId: string) {
  return useQuery({
    queryKey: ["pathways", "patient-detail", patientPathwayId],
    queryFn: async () => {
      if (!patientPathwayId) return null;
      const { data } = await apiClient.get<{ data: PatientPathwayDetailResponse }>(
        `/engine/pathways/patients/${patientPathwayId}`,
      );
      return data.data;
    },
    enabled: !!patientPathwayId,
  });
}

export function useAdvancePatientPathway() {
  return useMutation({
    mutationFn: async (payload: {
      patientPathwayId: string;
      eventType: PathwayAdvanceEventType;
      currentNodeCode?: string;
      requestedNextNodeCode?: string;
      varianceType?: VarianceType;
      varianceReason?: string;
      resolutionAction?: string;
      exitReason?: string;
      eventId?: string;
    }) => {
      const { data } = await apiClient.post<{ data: PathwayAdvanceResponse }>(
        "/engine/pathways/advance",
        payload,
      );
      return data.data;
    },
  });
}

export function usePatientPathwayClocks(patientPathwayId: string) {
  return useQuery({
    queryKey: ["pathways", "patient-clocks", patientPathwayId],
    queryFn: async () => {
      if (!patientPathwayId) return [];
      const { data } = await apiClient.get<{ data: ClinicalClock[] }>(
        `/engine/pathways/${patientPathwayId}/clocks`,
      );
      return data.data;
    },
    enabled: !!patientPathwayId,
  });
}

export function usePatientPathwayDiagnose(patientPathwayId: string) {
  return useQuery({
    queryKey: ["pathways", "patient-diagnose", patientPathwayId],
    queryFn: async () => {
      if (!patientPathwayId) return null;
      const { data } = await apiClient.get<{ data: DiagnoseResponse }>(
        `/engine/pathways/patients/${patientPathwayId}/diagnose`,
      );
      return data.data;
    },
    enabled: !!patientPathwayId,
  });
}
