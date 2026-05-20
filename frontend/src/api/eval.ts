import { get, post, put, del } from "./client";

// ==================== 类型定义 ====================

export interface EvalSource {
  document_code?: string;
  citation_id?: string;
  binding_type?: string;
}

export interface EvalOrgContext {
  group_code?: string;
  hospital_code?: string;
  campus_code?: string;
  site_code?: string;
  department_code?: string;
  scope_level?: string;
  scope_code?: string;
  org_source?: string;
}

export interface EvalIndicator {
  tenant_id: string;
  indicator_code: string;
  set_code: string;
  indicator_name: string;
  indicator_type: "SCORE" | "RATE" | "COUNT" | "BOOLEAN";
  weight: number;
  max_value: number;
  threshold_expression?: string;
  risk_level_mapping?: string;
  calc_expression?: string;
  unit?: string;
  description?: string;
  source: EvalSource;
  status: "DRAFT" | "PUBLISHED" | "DEPRECATED";
  created_by?: string;
  created_time?: string;
  updated_time?: string;
}

export interface EvalIndicatorSet {
  tenant_id: string;
  set_code: string;
  set_name: string;
  subject_type: "EMR" | "INSURANCE" | "PATHWAY" | "DEPARTMENT" | "CONFIG";
  description?: string;
  version: string;
  status: "DRAFT" | "PUBLISHED" | "DEPRECATED";
  source: EvalSource;
  org_context: EvalOrgContext;
  created_by?: string;
  created_time?: string;
  updated_time?: string;
  indicators?: EvalIndicator[];
}

// ==================== 指标集 API ====================

export async function listEvalSets(params?: {
  subject_type?: string;
  status?: string;
}): Promise<EvalIndicatorSet[]> {
  const qs = new URLSearchParams();
  if (params?.subject_type) qs.set("subject_type", params.subject_type);
  if (params?.status) qs.set("status", params.status);
  const query = qs.toString();
  return get<EvalIndicatorSet[]>(`/quality/eval/sets${query ? `?${query}` : ""}`);
}

export async function getEvalSet(setCode: string): Promise<EvalIndicatorSet> {
  return get<EvalIndicatorSet>(`/quality/eval/sets/${encodeURIComponent(setCode)}`);
}

export async function createEvalSet(data: {
  set_name: string;
  subject_type: string;
  description?: string;
  document_code?: string;
  citation_id?: string;
  binding_type?: string;
  created_by?: string;
}): Promise<EvalIndicatorSet> {
  return post<EvalIndicatorSet>("/quality/eval/sets", data);
}

export async function updateEvalSet(
  setCode: string,
  data: {
    set_name?: string;
    subject_type?: string;
    description?: string;
    document_code?: string;
    citation_id?: string;
    binding_type?: string;
  },
): Promise<EvalIndicatorSet> {
  return put<EvalIndicatorSet>(`/quality/eval/sets/${encodeURIComponent(setCode)}`, data);
}

export async function publishEvalSet(setCode: string): Promise<EvalIndicatorSet> {
  return post<EvalIndicatorSet>(`/quality/eval/sets/${encodeURIComponent(setCode)}/publish`, {});
}

export async function deprecateEvalSet(setCode: string): Promise<EvalIndicatorSet> {
  return post<EvalIndicatorSet>(`/quality/eval/sets/${encodeURIComponent(setCode)}/deprecate`, {});
}

// ==================== 指标 API ====================

export async function listEvalIndicators(setCode: string): Promise<EvalIndicator[]> {
  return get<EvalIndicator[]>(`/quality/eval/sets/${encodeURIComponent(setCode)}/indicators`);
}

export async function getEvalIndicator(indicatorCode: string): Promise<EvalIndicator> {
  return get<EvalIndicator>(`/quality/eval/indicators/${encodeURIComponent(indicatorCode)}`);
}

export async function createEvalIndicator(
  setCode: string,
  data: {
    indicator_name: string;
    indicator_type: string;
    weight?: number;
    max_value?: number;
    threshold_expression?: string;
    risk_level_mapping?: string;
    calc_expression?: string;
    unit?: string;
    description?: string;
    document_code?: string;
    citation_id?: string;
    binding_type?: string;
    created_by?: string;
  },
): Promise<EvalIndicator> {
  return post<EvalIndicator>(`/quality/eval/sets/${encodeURIComponent(setCode)}/indicators`, data);
}

export async function updateEvalIndicator(
  indicatorCode: string,
  data: {
    indicator_name?: string;
    indicator_type?: string;
    weight?: number;
    max_value?: number;
    threshold_expression?: string;
    risk_level_mapping?: string;
    calc_expression?: string;
    unit?: string;
    description?: string;
    document_code?: string;
    citation_id?: string;
    binding_type?: string;
  },
): Promise<EvalIndicator> {
  return put<EvalIndicator>(`/quality/eval/indicators/${encodeURIComponent(indicatorCode)}`, data);
}

export async function deleteEvalIndicator(indicatorCode: string): Promise<EvalIndicator> {
  return del<EvalIndicator>(`/quality/eval/indicators/${encodeURIComponent(indicatorCode)}`);
}

// ==================== 评分引擎类型 ====================

export interface IndicatorScore {
  indicator_code: string;
  indicator_name: string;
  indicator_type: string;
  weight: number;
  raw_score: number;
  weighted_score: number;
  max_value: number;
  risk_level: string;
  unit?: string;
  threshold_met: boolean;
  explanation: string;
}

export interface EvalFact {
  fact_type: "ABNORMAL" | "MISSING";
  indicator_code: string;
  indicator_name: string;
  description: string;
  severity: string;
}

export interface EvalResultData {
  eval_id: string;
  tenant_id: string;
  set_code: string;
  subject_type: string;
  subject_id: string;
  subject_name: string;
  total_score: number;
  max_possible_score: number;
  score_percentage: number;
  risk_level: string;
  indicator_scores: IndicatorScore[];
  abnormal_facts: EvalFact[];
  missing_facts: EvalFact[];
  evaluated_by: string;
  evaluated_at: string;
  org_context: Record<string, unknown>;
}

// ==================== 评分引擎 API ====================

export async function executeEvaluation(params: {
  set_code: string;
  subject_id: string;
  subject_name?: string;
  input_data: Record<string, unknown>;
}): Promise<EvalResultData> {
  return post<EvalResultData>("/quality/eval/evaluate", params);
}

export async function listEvalResults(params?: {
  set_code?: string;
  subject_type?: string;
}): Promise<EvalResultData[]> {
  const qs = new URLSearchParams();
  if (params?.set_code) qs.set("set_code", params.set_code);
  if (params?.subject_type) qs.set("subject_type", params.subject_type);
  const query = qs.toString();
  return get<EvalResultData[]>(`/quality/eval/results${query ? `?${query}` : ""}`);
}

export async function getEvalResult(evalId: string): Promise<EvalResultData> {
  return get<EvalResultData>(`/quality/eval/results/${encodeURIComponent(evalId)}`);
}

// ==================== 评估报告类型 ====================

export interface EvalReportData {
  report_id: string;
  eval_id: string;
  tenant_id: string;
  set_code: string;
  subject_type: string;
  subject_id: string;
  subject_name: string;
  total_score: number;
  max_possible_score: number;
  score_percentage: number;
  risk_level: string;
  recommendations: string[];
  status: "DRAFT" | "REVIEWED" | "REVIEW_REJECTED" | "CONDITIONALLY_APPROVED" | "ARCHIVED";
  generated_at: string;
  archived_at?: string;
  abnormal_fact_count: number;
  missing_fact_count: number;
  org_context: Record<string, unknown>;
}

export interface EvalReportExport extends EvalReportData {
  indicator_scores: IndicatorScore[];
  abnormal_facts: EvalFact[];
  missing_facts: EvalFact[];
}

export interface EvalReviewData {
  review_id: string;
  report_id: string;
  tenant_id: string;
  reviewer_id: string;
  reviewer_name: string;
  review_result: "APPROVED" | "REJECTED" | "CONDITIONALLY_APPROVED";
  review_comment: string;
  reviewed_at: string;
}

export interface EvalRectificationData {
  rect_id: string;
  report_id: string;
  tenant_id: string;
  title: string;
  description: string;
  assignee_id: string;
  assignee_name: string;
  priority: "HIGH" | "MEDIUM" | "LOW";
  due_date: string;
  related_facts: string;
  status: "PENDING" | "IN_PROGRESS" | "COMPLETED";
  updated_by: string;
  update_note: string;
  created_at: string;
  updated_at?: string;
  completed_at?: string;
  org_context: Record<string, unknown>;
}

// ==================== 评估报告 API ====================

export async function generateReport(evalId: string): Promise<EvalReportData> {
  return post<EvalReportData>("/quality/eval/report/generate", { eval_id: evalId });
}

export async function exportReport(reportId: string): Promise<EvalReportExport> {
  return get<EvalReportExport>(`/quality/eval/report/${encodeURIComponent(reportId)}/export`);
}

export async function getReport(reportId: string): Promise<EvalReportData> {
  return get<EvalReportData>(`/quality/eval/report/${encodeURIComponent(reportId)}`);
}

export async function listReports(params?: {
  status?: string;
  subject_type?: string;
}): Promise<EvalReportData[]> {
  const qs = new URLSearchParams();
  if (params?.status) qs.set("status", params.status);
  if (params?.subject_type) qs.set("subject_type", params.subject_type);
  const query = qs.toString();
  return get<EvalReportData[]>(`/quality/eval/report/list${query ? `?${query}` : ""}`);
}

export async function archiveReport(reportId: string): Promise<EvalReportData> {
  return post<EvalReportData>(`/quality/eval/report/${encodeURIComponent(reportId)}/archive`, {});
}

// ==================== 人工复核 API ====================

export async function submitReview(
  reportId: string,
  data: {
    reviewer_id: string;
    reviewer_name: string;
    review_result: "APPROVED" | "REJECTED" | "CONDITIONALLY_APPROVED";
    review_comment: string;
  },
): Promise<EvalReviewData> {
  return post<EvalReviewData>(`/quality/eval/report/${encodeURIComponent(reportId)}/review`, data);
}

export async function listReviews(reportId: string): Promise<EvalReviewData[]> {
  return get<EvalReviewData[]>(`/quality/eval/report/${encodeURIComponent(reportId)}/reviews`);
}

// ==================== 整改任务 API ====================

export async function createRectification(
  reportId: string,
  data: {
    title: string;
    description?: string;
    assignee_id?: string;
    assignee_name?: string;
    priority?: string;
    due_date?: string;
    related_facts?: string;
  },
): Promise<EvalRectificationData> {
  return post<EvalRectificationData>(`/quality/eval/report/${encodeURIComponent(reportId)}/rectification`, data);
}

export async function autoCreateRectifications(reportId: string): Promise<EvalRectificationData[]> {
  return post<EvalRectificationData[]>(`/quality/eval/report/${encodeURIComponent(reportId)}/rectification/auto`, {});
}

export async function updateRectificationStatus(
  rectId: string,
  data: {
    status: "IN_PROGRESS" | "COMPLETED";
    updated_by?: string;
    update_note?: string;
  },
): Promise<EvalRectificationData> {
  return post<EvalRectificationData>(`/quality/eval/report/rectification/${encodeURIComponent(rectId)}/status`, data);
}

export async function listRectifications(params?: {
  report_id?: string;
  status?: string;
}): Promise<EvalRectificationData[]> {
  const qs = new URLSearchParams();
  if (params?.report_id) qs.set("report_id", params.report_id);
  if (params?.status) qs.set("status", params.status);
  const query = qs.toString();
  return get<EvalRectificationData[]>(`/quality/eval/report/rectification/list${query ? `?${query}` : ""}`);
}

// ==================== 再评估 API ====================

export async function reEvaluate(
  evalId: string,
  inputData: Record<string, unknown>,
): Promise<EvalResultData> {
  return post<EvalResultData>("/quality/eval/report/re-evaluate", {
    eval_id: evalId,
    input_data: inputData,
  });
}
