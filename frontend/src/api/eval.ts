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
