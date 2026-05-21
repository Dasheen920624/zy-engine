import { get, post, put, del } from "./client";
import type { PathwayListResult, PathwayDetail, ListPathwaysParams } from "./types";
import type { ValidationResult } from "../components/PathwayCanvas/types";

// ─── 本地 view 类型（不污染架构师专属 api/types.ts）──────────────────
//
// 与后端 com.medkernel.dto.* 对齐：PatientPathwayInstance / PathwayVariationRecord /
// PatientNodeState / PatientTaskState / RecommendationCard。

export type InstanceStatus = "ACTIVE" | "COMPLETED" | "EXITED" | "TERMINATED";

export interface PatientPathwayInstance {
  instance_id: string;
  patient_id: string;
  encounter_id: string;
  pathway_code: string;
  version_no?: string;
  status: InstanceStatus;
  current_node_code?: string;
  tenant_id?: string;
  group_code?: string;
  hospital_code?: string;
  campus_code?: string;
  site_code?: string;
  department_code?: string;
  scope_level?: string;
  scope_code?: string;
  org_source?: string;
}

export type TaskStatus = "PENDING" | "COMPLETED" | "SKIPPED" | "FAILED";

export interface PatientTaskState {
  instance_id: string;
  node_code: string;
  task_code: string;
  task_name?: string;
  task_type?: string;
  required: boolean;
  status: TaskStatus;
  operator_id?: string;
  updated_time?: string;
  result?: Record<string, unknown>;
}

export type NodeStatus = "PENDING" | "ACTIVE" | "COMPLETED" | "SKIPPED" | "BLOCKED";

export interface PatientNodeState {
  instance_id: string;
  node_code: string;
  node_name?: string;
  status: NodeStatus;
  enter_time?: string;
  complete_time?: string;
  timeout_flag?: boolean;
  tasks?: PatientTaskState[];
}

export type VariationType =
  | "SKIP"
  | "DEFER"
  | "EXTEND_TIME"
  | "SUBSTITUTE"
  | "EXIT"
  | "ROLLBACK"
  | "MANUAL_OVERRIDE";

export interface PathwayVariationRecord {
  variation_id: string;
  instance_id: string;
  pathway_code: string;
  version_no?: string;
  patient_id?: string;
  encounter_id?: string;
  node_code?: string;
  variation_type: VariationType;
  reason?: string;
  operator_id?: string;
  created_time?: string;
  tenant_id?: string;
  group_code?: string;
  hospital_code?: string;
  campus_code?: string;
  site_code?: string;
  department_code?: string;
  scope_level?: string;
  scope_code?: string;
  org_source?: string;
}

export interface RecommendationCard {
  recommendation_id: string;
  scenario?: string;
  patient_id?: string;
  encounter_id?: string;
  target_code: string;
  target_name?: string;
  score: number;
  confidence?: string;
  action_level?: string;
  supporting_facts?: Array<Record<string, unknown>>;
  missing_facts?: string[];
  evidence_refs?: string[];
  suggested_actions?: string[];
}

export interface PathwayDiffResult {
  pathway_code: string;
  from_version: string;
  to_version: string;
  nodes_added?: Array<Record<string, unknown>>;
  nodes_removed?: Array<Record<string, unknown>>;
  nodes_modified?: Array<Record<string, unknown>>;
  edges_added?: Array<Record<string, unknown>>;
  edges_removed?: Array<Record<string, unknown>>;
  edges_modified?: Array<Record<string, unknown>>;
  tasks_added?: Array<Record<string, unknown>>;
  tasks_removed?: Array<Record<string, unknown>>;
  tasks_modified?: Array<Record<string, unknown>>;
  summary?: {
    nodes_added: number;
    nodes_removed: number;
    nodes_modified: number;
    edges_added: number;
    edges_removed: number;
    edges_modified: number;
    tasks_added: number;
    tasks_removed: number;
    tasks_modified: number;
  };
}

export interface InstanceSummary {
  total: number;
  active: number;
  completed: number;
  exited: number;
  terminated: number;
  variation_count?: number;
  avg_node_count?: number;
}

export interface NodeCompletionSummary {
  total: number;
  by_node?: Array<{ node_code: string; node_name?: string; completed: number; total: number; completion_rate: number }>;
}

export interface NodeStayDurationSummary {
  total: number;
  by_node?: Array<{ node_code: string; node_name?: string; avg_stay_hours: number; max_stay_hours: number; min_stay_hours: number }>;
}

export interface VariationSummary {
  total: number;
  by_type?: Array<{ variation_type: VariationType; count: number }>;
  by_node?: Array<{ node_code: string; count: number }>;
  recent_variations?: PathwayVariationRecord[];
}

export interface PatientPathwayListFilters {
  pathway_code?: string;
  status?: InstanceStatus;
  patient_id?: string;
  encounter_id?: string;
  current_node_code?: string;
  limit?: number;
}

export interface AdmitRequest {
  pathway_code: string;
  version_no?: string;
  patient_id: string;
  encounter_id: string;
  initial_facts?: Record<string, unknown>;
  operator_id?: string;
}

export interface RecordVariationRequest {
  variation_type: VariationType;
  node_code?: string;
  reason: string;
  operator_id?: string;
}

function buildQs(params: Record<string, unknown>): string {
  const qs = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    qs.set(key, String(value));
  });
  const str = qs.toString();
  return str ? `?${str}` : "";
}

// ─── 既有：路径模板 CRUD + 草稿 ─────────────────────────────────────

export async function listPathways(params?: ListPathwaysParams): Promise<PathwayListResult> {
  const qs = new URLSearchParams();
  if (params?.search) qs.set("search", params.search);
  if (params?.status) qs.set("status", params.status);
  if (params?.dept) qs.set("dept", params.dept);
  if (params?.page) qs.set("page", String(params.page));
  if (params?.size) qs.set("size", String(params.size));
  const query = qs.toString();
  return get<PathwayListResult>(`/pathways${query ? `?${query}` : ""}`);
}

export async function getPathway(code: string, versionNo?: string): Promise<PathwayDetail> {
  const qs = versionNo ? `?versionNo=${encodeURIComponent(versionNo)}` : "";
  return get<PathwayDetail>(`/pathways/${encodeURIComponent(code)}${qs}`);
}

export async function createPathway(config: Record<string, unknown>): Promise<Record<string, unknown>> {
  return post("/pathways", config);
}

export async function deletePathway(code: string): Promise<Record<string, unknown>> {
  return del(`/pathways/${encodeURIComponent(code)}`);
}

export async function publishPathway(
  code: string,
  request: Record<string, unknown>,
): Promise<Record<string, unknown>> {
  return post(`/pathways/${encodeURIComponent(code)}/publish`, request);
}

export async function rollbackPathway(
  code: string,
  request: Record<string, unknown>,
): Promise<Record<string, unknown>> {
  return post(`/pathways/${encodeURIComponent(code)}/rollback`, request);
}

export async function savePathwayDraft(
  code: string,
  draft: { nodes: unknown[]; edges: unknown[] },
): Promise<Record<string, unknown>> {
  return put(`/pathways/${encodeURIComponent(code)}/draft`, draft);
}

export async function validatePathway(
  code: string,
  draft: { nodes: unknown[]; edges: unknown[] },
): Promise<ValidationResult> {
  return post<ValidationResult>(`/pathways/${encodeURIComponent(code)}/validate`, draft);
}

export async function submitPathwayReview(
  code: string,
): Promise<Record<string, unknown>> {
  return post(`/pathways/${encodeURIComponent(code)}/submit-review`, {});
}

// ─── 新增（PATHWAY-ENGINE-COMPLETE）：版本对比 / 患者路径 / 实例 / 变异 ─

/** 版本对比：from / to 两个版本的 nodes / edges / tasks 增删改 */
export async function diffPathway(
  code: string,
  fromVersion: string,
  toVersion: string,
): Promise<PathwayDiffResult> {
  return get<PathwayDiffResult>(
    `/pathways/${encodeURIComponent(code)}/diff${buildQs({ from: fromVersion, to: toVersion })}`,
  );
}

// ── 患者路径实例 ──

/** 候选路径推荐（入径前选模板）：基于患者上下文返回推荐卡片 */
export async function recommendPathwayCandidates(
  patientContext: Record<string, unknown>,
): Promise<RecommendationCard[]> {
  return post<RecommendationCard[]>(`/patient-pathways/candidates`, patientContext);
}

/** 入径：把患者纳入某条路径 */
export async function admitPatientPathway(request: AdmitRequest): Promise<PatientPathwayInstance> {
  return post<PatientPathwayInstance>(`/patient-pathways/admit`, request);
}

/** 实例详情（不带节点 / 任务子状态） */
export async function getPatientPathwayInstance(
  instanceId: string,
): Promise<Record<string, unknown>> {
  return get<Record<string, unknown>>(`/patient-pathways/${encodeURIComponent(instanceId)}`);
}

/** 节点状态：含任务列表 */
export async function getNodeState(
  instanceId: string,
  nodeCode: string,
): Promise<PatientNodeState> {
  return get<PatientNodeState>(
    `/patient-pathways/${encodeURIComponent(instanceId)}/nodes/${encodeURIComponent(nodeCode)}`,
  );
}

/** 完成任务 */
export async function completeTask(
  instanceId: string,
  nodeCode: string,
  taskCode: string,
  request: Record<string, unknown> = {},
): Promise<PatientTaskState> {
  return post<PatientTaskState>(
    `/patient-pathways/${encodeURIComponent(instanceId)}/nodes/${encodeURIComponent(nodeCode)}/tasks/${encodeURIComponent(taskCode)}/complete`,
    request,
  );
}

/** 跳过任务 */
export async function skipTask(
  instanceId: string,
  nodeCode: string,
  taskCode: string,
  request: Record<string, unknown> = {},
): Promise<PatientTaskState> {
  return post<PatientTaskState>(
    `/patient-pathways/${encodeURIComponent(instanceId)}/nodes/${encodeURIComponent(nodeCode)}/tasks/${encodeURIComponent(taskCode)}/skip`,
    request,
  );
}

/** 完成节点（不需要逐任务推进时） */
export async function completeNode(
  instanceId: string,
  nodeCode: string,
  request: Record<string, unknown> = {},
): Promise<PatientPathwayInstance> {
  return post<PatientPathwayInstance>(
    `/patient-pathways/${encodeURIComponent(instanceId)}/nodes/${encodeURIComponent(nodeCode)}/complete`,
    request,
  );
}

/** 记录变异：跳节点 / 延迟 / 替换 / 退出 等 */
export async function recordVariation(
  instanceId: string,
  request: RecordVariationRequest,
): Promise<PathwayVariationRecord> {
  return post<PathwayVariationRecord>(
    `/patient-pathways/${encodeURIComponent(instanceId)}/variations`,
    request,
  );
}

// ── 实例查询（列表 / 聚合 / 节点完成度 / 驻留时长）──

export async function listPatientPathwayInstances(
  filters: PatientPathwayListFilters = {},
): Promise<PatientPathwayInstance[]> {
  return get<PatientPathwayInstance[]>(`/pathway-instances${buildQs({ ...filters })}`);
}

export async function summarizePatientPathwayInstances(
  filters: Omit<PatientPathwayListFilters, "limit"> = {},
): Promise<InstanceSummary> {
  return get<InstanceSummary>(`/pathway-instances/summary${buildQs({ ...filters })}`);
}

export async function nodeCompletionSummary(
  filters: Omit<PatientPathwayListFilters, "limit"> = {},
): Promise<NodeCompletionSummary> {
  return get<NodeCompletionSummary>(`/pathway-instances/node-completion${buildQs({ ...filters })}`);
}

export async function nodeStayDurationSummary(
  filters: Omit<PatientPathwayListFilters, "limit"> = {},
): Promise<NodeStayDurationSummary> {
  return get<NodeStayDurationSummary>(
    `/pathway-instances/node-stay-duration${buildQs({ ...filters })}`,
  );
}

// ── 变异记录 ──

export interface VariationListFilters {
  pathway_code?: string;
  patient_id?: string;
  encounter_id?: string;
  variation_type?: VariationType;
  node_code?: string;
  instance_id?: string;
  limit?: number;
}

export async function listVariations(
  filters: VariationListFilters = {},
): Promise<PathwayVariationRecord[]> {
  return get<PathwayVariationRecord[]>(`/pathway-variations${buildQs({ ...filters })}`);
}

export async function summarizeVariations(
  filters: Omit<VariationListFilters, "limit"> = {},
): Promise<VariationSummary> {
  return get<VariationSummary>(`/pathway-variations/summary${buildQs({ ...filters })}`);
}
