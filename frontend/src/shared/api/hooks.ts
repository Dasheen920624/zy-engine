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

// ==================== 推荐/CDSS 引擎相关的实体及 DTO 契约 ====================

export type RecommendationCardStatus = "PENDING" | "ACCEPTED" | "REJECTED" | "EXPIRED" | string;
export type RecommendationCardType =
  | "DRUG_SAFETY"
  | "INSURANCE_AUDIT"
  | "CLINICAL_QUALITY"
  | string;
export type RecommendationRiskLevel = "LOW" | "MEDIUM" | "HIGH" | string;
export type RecommendationInterruptLevel = "NONE" | "SOFT" | "HARD" | string;
export type RecommendationSourceType = "GUIDELINE" | "LITERATURE" | "REGULATION" | string;
export type RecommendationTriggerStatus = "SUCCESS" | "FAILED" | string;
export type RecommendationFeedbackType = "ACCEPT" | "REJECT" | string;
export type RecommendationFatigueSignalType = "MUTE" | "WARNING" | "BLOCK" | string;

export interface RecommendationCard {
  id?: number;
  cardId: string;
  tenantId: string;
  triggerId: string;
  patientId: string;
  encounterId?: string;
  scenarioCode: string;
  cardType: RecommendationCardType;
  title: string;
  summary: string;
  riskLevel: RecommendationRiskLevel;
  interruptLevel: RecommendationInterruptLevel;
  status: RecommendationCardStatus;
  changeSummary?: string;
  createdAt?: string;
  createdBy?: string;
  traceId?: string;
}

export interface RecommendationSource {
  id?: number;
  sourceId: string;
  cardId: string;
  sourceType: RecommendationSourceType;
  title: string;
  content: string;
  evidenceLevel?: string;
  authorityScore?: number;
  sourceRef: string;
  createdAt?: string;
}

export interface RecommendationFeedback {
  id?: number;
  feedbackId: string;
  cardId: string;
  feedbackType: RecommendationFeedbackType;
  rejectReason?: string;
  comments?: string;
  physicianId: string;
  createdAt?: string;
}

export interface RecommendationFatigueSignal {
  id?: number;
  signalId: string;
  tenantId: string;
  fatigueKey: string;
  signalType: RecommendationFatigueSignalType;
  triggerCount: number;
  governanceThreshold: number;
  summary?: string;
  createdAt?: string;
}

export interface RecommendationCardDetailResponse {
  card: RecommendationCard;
  sources: RecommendationSource[];
  feedback?: RecommendationFeedback;
  fatigueSignals: RecommendationFatigueSignal[];
  traceId: string;
}

export interface RecommendationTriggerResponse {
  triggerId: string;
  cardCount: number;
  traceId: string;
}

export interface RecommendationFeedbackResponse {
  cardId: string;
  status: RecommendationCardStatus;
  traceId: string;
}

// 1. Trigger Hooks
export function useCreateRecommendationTrigger() {
  return useMutation({
    mutationFn: async (payload: {
      patientId: string;
      encounterId?: string;
      scenarioCode: string;
      diseaseCode?: string;
      payloadJson: string;
    }) => {
      const { data } = await apiClient.post<{ data: RecommendationTriggerResponse }>(
        "/engine/recommendations/triggers",
        payload,
      );
      return data.data;
    },
  });
}

// 2. Card Hooks
export function useRecommendationCards(params?: {
  status?: RecommendationCardStatus;
  riskLevel?: RecommendationRiskLevel;
  scenarioCode?: string;
  patientId?: string;
  page?: number;
  size?: number;
  sort?: string;
}) {
  return useQuery({
    queryKey: ["recommendations", "cards", params ?? {}],
    queryFn: async () => {
      const { data } = await apiClient.get<{ data: PageResponse<RecommendationCard> }>(
        "/engine/recommendations/cards",
        { params },
      );
      return data.data;
    },
  });
}

export function useRecommendationCardDetail(cardId: string) {
  return useQuery({
    queryKey: ["recommendations", "card-detail", cardId],
    queryFn: async () => {
      if (!cardId) return null;
      const { data } = await apiClient.get<{ data: RecommendationCardDetailResponse }>(
        `/engine/recommendations/cards/${cardId}`,
      );
      return data.data;
    },
    enabled: !!cardId,
  });
}

export function useRecommendationCardSources(cardId: string) {
  return useQuery({
    queryKey: ["recommendations", "card-sources", cardId],
    queryFn: async () => {
      if (!cardId) return [];
      const { data } = await apiClient.get<{ data: RecommendationSource[] }>(
        `/engine/recommendations/cards/${cardId}/sources`,
      );
      return data.data;
    },
    enabled: !!cardId,
  });
}

// 3. Feedback Hook
export function useSubmitRecommendationFeedback(cardId: string) {
  return useMutation({
    mutationFn: async (payload: {
      feedbackType: RecommendationFeedbackType;
      rejectReason?: string;
      comments?: string;
      physicianId?: string;
    }) => {
      const { data } = await apiClient.post<{ data: RecommendationFeedbackResponse }>(
        `/engine/recommendations/cards/${cardId}/feedback`,
        payload,
      );
      return data.data;
    },
  });
}

// 4. Fatigue Signal Hooks
export function useRecommendationFatigueSignals(params?: {
  fatigueKey?: string;
  signalType?: RecommendationFatigueSignalType;
  page?: number;
  size?: number;
  sort?: string;
}) {
  return useQuery({
    queryKey: ["recommendations", "fatigue-signals", params ?? {}],
    queryFn: async () => {
      const { data } = await apiClient.get<{ data: PageResponse<RecommendationFatigueSignal> }>(
        "/engine/recommendations/fatigue-signals",
        { params },
      );
      return data.data;
    },
  });
}

// 5. Diagnose Hook
export function useRecommendationTriggerDiagnose(triggerId: string) {
  return useQuery({
    queryKey: ["recommendations", "trigger-diagnose", triggerId],
    queryFn: async () => {
      if (!triggerId) return null;
      const { data } = await apiClient.get<{ data: DiagnoseResponse }>(
        `/engine/recommendations/triggers/${triggerId}/diagnose`,
      );
      return data.data;
    },
    enabled: !!triggerId,
  });
}

// ==================== 评估质控引擎相关的实体及 DTO 契约 ====================

export type EvaluationIndicatorStatus =
  | "DRAFT"
  | "PENDING_REVIEW"
  | "PUBLISHED"
  | "ACTIVE"
  | "OFFLINE"
  | "ARCHIVED";

export type EvaluationSubjectType =
  | "PATIENT"
  | "MEDICAL_RECORD"
  | "DEPARTMENT"
  | "DOCTOR"
  | "DISEASE"
  | "PATHWAY"
  | "CLAIM"
  | "FOLLOWUP";

export type EvaluationResultLevel = "PASS" | "ATTENTION" | "NON_COMPLIANT" | "CRITICAL";

export type QualityFindingSeverity = "P0" | "P1" | "P2" | "P3";

export type QualityFindingStatus = "NEW" | "ASSIGNED" | "REMEDIATING" | "CLOSED" | "WAIVED";

export type RectificationTaskStatus = "ASSIGNED" | "SUBMITTED" | "RETURNED" | "CLOSED" | "WAIVED";

export type RectificationReviewDecision = "APPROVED" | "RETURNED" | "WAIVED";

export interface EvaluationIndicator {
  id?: number;
  indicatorId: string;
  tenantId: string;
  indicatorCode: string;
  versionNo: number;
  name: string;
  subjectType: EvaluationSubjectType;
  denominatorDefinition?: string;
  numeratorDefinition?: string;
  exclusionDefinition?: string;
  scoringDefinition?: string;
  timeWindow: string;
  organizationScope: string;
  responsibleDepartmentId: string;
  sourceRef: string;
  packageVersion?: string;
  status: EvaluationIndicatorStatus;
  publishedAt?: string;
  publishedBy?: string;
  activatedAt?: string;
  createdAt?: string;
  createdBy?: string;
  traceId?: string;
}

export interface EvaluationResult {
  id?: number;
  resultId: string;
  tenantId: string;
  runId: string;
  indicatorId: string;
  indicatorCode: string;
  indicatorVersion: number;
  subjectType: EvaluationSubjectType;
  subjectRefId: string;
  scoreValue?: number;
  resultLevel: EvaluationResultLevel;
  hitFlag: boolean;
  evidenceSummary: string;
  sourceRef?: string;
  responsibleDepartmentId?: string;
  createdAt?: string;
}

export interface QualityFinding {
  id?: number;
  findingId: string;
  tenantId: string;
  runId: string;
  resultId: string;
  indicatorId: string;
  findingCode: string;
  title: string;
  description: string;
  severity: QualityFindingSeverity;
  status: QualityFindingStatus;
  evidenceSummary: string;
  responsibleDepartmentId?: string;
  dueAt?: string;
  createdAt?: string;
}

export interface RectificationTask {
  id?: number;
  taskId: string;
  tenantId: string;
  findingId: string;
  responsibleDepartmentId: string;
  assigneeUserId?: string;
  status: RectificationTaskStatus;
  dueAt: string;
  rectificationSummary?: string;
  evidenceRef?: string;
  submittedAt?: string;
  submittedBy?: string;
  closedAt?: string;
  createdAt?: string;
}

export interface RectificationReview {
  id?: number;
  reviewId: string;
  tenantId: string;
  findingId: string;
  taskId: string;
  decision: RectificationReviewDecision;
  comments?: string;
  evidenceRef?: string;
  reviewedBy: string;
  reviewedAt: string;
}

export interface QualityFindingDetailResponse {
  finding: QualityFinding;
  task?: RectificationTask;
  reviews: RectificationReview[];
}

export interface EvaluationRunResponse {
  runId: string;
  status: string;
  resultCount: number;
  findingCount: number;
  taskCount: number;
  traceId: string;
}

export interface RectificationResponse {
  taskId: string;
  findingStatus: QualityFindingStatus;
  taskStatus: RectificationTaskStatus;
  traceId: string;
}

export interface RectificationReviewResponse {
  reviewId: string;
  findingStatus: QualityFindingStatus;
  taskStatus: RectificationTaskStatus;
  traceId: string;
}

// 1. Indicator Lifecycle Hooks
export function useEvaluationIndicators(params?: {
  status?: EvaluationIndicatorStatus;
  subjectType?: EvaluationSubjectType;
  indicatorCode?: string;
  page?: number;
  size?: number;
  sort?: string;
}) {
  return useQuery({
    queryKey: ["evaluations", "indicators", params ?? {}],
    queryFn: async () => {
      const { data } = await apiClient.get<{ data: PageResponse<EvaluationIndicator> }>(
        "/engine/evaluations/indicators",
        { params },
      );
      return data.data;
    },
  });
}

export function useCreateEvaluationIndicator() {
  return useMutation({
    mutationFn: async (payload: {
      indicatorCode: string;
      versionNo: number;
      name: string;
      subjectType: EvaluationSubjectType;
      denominatorDefinition: string;
      numeratorDefinition: string;
      exclusionDefinition?: string;
      scoringDefinition?: string;
      timeWindow: string;
      organizationScope: string;
      responsibleDepartmentId: string;
      sourceRef: string;
      packageVersion?: string;
    }) => {
      const { data } = await apiClient.post<{ data: EvaluationIndicator }>(
        "/engine/evaluations/indicators",
        payload,
      );
      return data.data;
    },
  });
}

export function useSubmitEvaluationIndicator() {
  return useMutation({
    mutationFn: async (indicatorId: string) => {
      const { data } = await apiClient.post<{ data: EvaluationIndicator }>(
        `/engine/evaluations/indicators/${indicatorId}/submit`,
      );
      return data.data;
    },
  });
}

export function usePublishEvaluationIndicator() {
  return useMutation({
    mutationFn: async (indicatorId: string) => {
      const { data } = await apiClient.post<{ data: EvaluationIndicator }>(
        `/engine/evaluations/indicators/${indicatorId}/publish`,
      );
      return data.data;
    },
  });
}

export function useActivateEvaluationIndicator() {
  return useMutation({
    mutationFn: async (indicatorId: string) => {
      const { data } = await apiClient.post<{ data: EvaluationIndicator }>(
        `/engine/evaluations/indicators/${indicatorId}/activate`,
      );
      return data.data;
    },
  });
}

// 2. Evaluation Results Hooks
export function useEvaluationResults(params?: {
  indicatorCode?: string;
  resultLevel?: EvaluationResultLevel;
  responsibleDepartmentId?: string;
  page?: number;
  size?: number;
  sort?: string;
}) {
  return useQuery({
    queryKey: ["evaluations", "results", params ?? {}],
    queryFn: async () => {
      const { data } = await apiClient.get<{ data: PageResponse<EvaluationResult> }>(
        "/engine/evaluations/results",
        { params },
      );
      return data.data;
    },
  });
}

// 3. Quality Findings & PDCA Rectification Hooks
export function useQualityFindings(params?: {
  severity?: QualityFindingSeverity;
  status?: QualityFindingStatus;
  responsibleDepartmentId?: string;
  page?: number;
  size?: number;
  sort?: string;
}) {
  return useQuery({
    queryKey: ["evaluations", "findings", params ?? {}],
    queryFn: async () => {
      const { data } = await apiClient.get<{ data: PageResponse<QualityFinding> }>(
        "/engine/evaluations/findings",
        { params },
      );
      return data.data;
    },
  });
}

export function useQualityFindingDetail(findingId: string) {
  return useQuery({
    queryKey: ["evaluations", "finding-detail", findingId],
    queryFn: async () => {
      if (!findingId) return null;
      const { data } = await apiClient.get<{ data: QualityFindingDetailResponse }>(
        `/engine/evaluations/findings/${findingId}`,
      );
      return data.data;
    },
    enabled: !!findingId,
  });
}

export function useSubmitRectification(findingId: string) {
  return useMutation({
    mutationFn: async (payload: {
      request: { rectificationSummary: string; evidenceRef: string };
      idempotencyKey?: string;
    }) => {
      const headers = payload.idempotencyKey
        ? { "Idempotency-Key": payload.idempotencyKey }
        : undefined;
      const { data } = await apiClient.post<{ data: RectificationResponse }>(
        `/engine/evaluations/findings/${findingId}/rectification`,
        payload.request,
        { headers },
      );
      return data.data;
    },
  });
}

export function useReviewRectification(findingId: string) {
  return useMutation({
    mutationFn: async (payload: {
      request: { decision: RectificationReviewDecision; comment: string; evidenceRef?: string };
      idempotencyKey?: string;
    }) => {
      const headers = payload.idempotencyKey
        ? { "Idempotency-Key": payload.idempotencyKey }
        : undefined;
      const { data } = await apiClient.post<{ data: RectificationReviewResponse }>(
        `/engine/evaluations/findings/${findingId}/review`,
        payload.request,
        { headers },
      );
      return data.data;
    },
  });
}

// 4. Quality Audit Run & Sandbox calculations
export function useEvaluateSnapshot() {
  return useMutation({
    mutationFn: async (payload: {
      contextSnapshotId: string;
      scenarioCode: string;
      packageVersion?: string;
    }) => {
      const { data } = await apiClient.post<{ data: EvaluationRunResponse }>(
        "/engine/evaluations/evaluate-snapshot",
        payload,
      );
      return data.data;
    },
  });
}

export function useEvaluationRunDiagnose(runId: string) {
  return useQuery({
    queryKey: ["evaluations", "run-diagnose", runId],
    queryFn: async () => {
      if (!runId) return null;
      const { data } = await apiClient.get<{ data: DiagnoseResponse }>(
        `/engine/evaluations/runs/${runId}/diagnose`,
      );
      return data.data;
    },
    enabled: !!runId,
  });
}

export const DEMO_SNAPSHOTS = [
  {
    id: "ctx-vte-demo-1",
    name: "患者李建国 - 术后静脉血栓高危风险已评估病例 (达标样板)",
    desc: "出院诊断：脑梗死、深静脉血栓形成。已于入院24小时内规范进行了静脉血栓评估并开具预防性用药医嘱。",
  },
  {
    id: "ctx-vte-demo-2",
    name: "患者张淑芳 - 剖宫产术后未录入血栓评估病例 (缺陷样板)",
    desc: "剖宫产术后24小时，医嘱与病历中均未记录下肢深静脉血栓风险分层评估及预防性护理，判定触发质控缺陷派单。",
  },
];

// ==================== 智能随访引擎相关的实体及 DTO 契约 ====================

export type FollowupPlanStatus = "DRAFT" | "ACTIVE" | "COMPLETED" | "CANCELLED";
export type FollowupTaskType = "QUESTIONNAIRE" | "EXAM" | "LAB" | "OUTPATIENT";
export type FollowupTaskStatus = "PENDING" | "COMPLETED" | "OVERDUE" | "CANCELLED";
export type FollowupEventType = "ABNORMAL_RETURN" | "RESULT_INFLOW";

export interface FollowupTaskDetailResponse {
  taskId: string;
  taskType: FollowupTaskType;
  dueDate: string;
  status: FollowupTaskStatus;
}

export interface FollowupPlanDetailResponse {
  planId: string;
  tenantId: string;
  patientId: string;
  encounterId: string;
  diseaseCode: string;
  status: FollowupPlanStatus;
  tasks: FollowupTaskDetailResponse[];
}

export interface FollowupPlanGenerateRequest {
  patientId: string;
  encounterId: string;
  pathwayId?: string;
  diseaseCode?: string;
  riskLevel?: string;
  taskTypes: string[];
}

export interface FollowupQuestionnaireSubmitRequest {
  taskId: string;
  formData: string; // JSON string
  executorId?: string;
  executorType?: string;
}

export interface FollowupAbnormalReportRequest {
  planId: string;
  eventType: FollowupEventType;
  payload: string; // JSON string or text description
  triggeredBy?: string;
}

export interface FollowupPlansParams {
  patientId?: string;
  page?: number;
  size?: number;
  sort?: string;
}

// 1. 获取随访计划分页
export function useFollowupPlans(params?: FollowupPlansParams) {
  return useQuery({
    queryKey: ["followup", "plans", params ?? {}],
    queryFn: async () => {
      const { data } = await apiClient.get<{ data: PageResponse<FollowupPlanDetailResponse> }>(
        "/engine/followup/plans",
        { params },
      );
      return data.data;
    },
  });
}

// 2. 智能生成随访计划
export function useGenerateFollowupPlan() {
  return useMutation({
    mutationFn: async (payload: FollowupPlanGenerateRequest) => {
      const { data } = await apiClient.post<{ data: FollowupPlanDetailResponse }>(
        "/engine/followup/plans/generate",
        payload,
      );
      return data.data;
    },
  });
}

// 3. 获取随访计划详情
export function useFollowupPlanDetail(planId: string) {
  return useQuery({
    queryKey: ["followup", "plan-detail", planId],
    queryFn: async () => {
      if (!planId) return null;
      const { data } = await apiClient.get<{ data: FollowupPlanDetailResponse }>(
        `/engine/followup/plans/${planId}`,
      );
      return data.data;
    },
    enabled: !!planId,
  });
}

// 4. 提交问卷并完成任务
export function useSubmitFollowupQuestionnaire() {
  return useMutation({
    mutationFn: async (payload: {
      taskId: string;
      request: FollowupQuestionnaireSubmitRequest;
    }) => {
      const { data } = await apiClient.post<void>(
        `/engine/followup/tasks/${payload.taskId}/questionnaires`,
        payload.request,
      );
      return data;
    },
  });
}

// 5. 上报随访异常事件
export function useReportFollowupAbnormal() {
  return useMutation({
    mutationFn: async (payload: FollowupAbnormalReportRequest) => {
      const { data } = await apiClient.post<void>(
        "/engine/followup/events/report-abnormal",
        payload,
      );
      return data;
    },
  });
}
