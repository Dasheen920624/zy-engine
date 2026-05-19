import { get, post } from "./client";
import type { ApiResult } from "./types";

// ─── 字典映射相关类型 ───────────────────────────────────────────────

export type MappingStatus = "PENDING" | "MAPPED" | "CONFLICT" | "AI_CANDIDATE";

export type TermType = "DIAGNOSIS" | "PROCEDURE" | "MEDICATION" | "LAB_TEST" | "ORDER" | "OTHER";

export interface TerminologyItem {
  id: string;
  internal_code: string;
  internal_name: string;
  term_type: TermType;
  status: MappingStatus;
  standard_code?: string;
  standard_name?: string;
  standard_system?: string;
  confidence?: number;
  ai_model?: string;
  ai_generated_at?: string;
  review_status?: "PENDING" | "ACCEPTED" | "REJECTED" | "MODIFIED";
  reviewer?: string;
  reviewed_at?: string;
  conflict_reason?: string;
  created_at: string;
  updated_at: string;
}

export interface AiCandidate {
  id: string;
  terminology_id: string;
  standard_code: string;
  standard_name: string;
  standard_system: string;
  confidence: number;
  model: string;
  generated_at: string;
  review_status: "PENDING" | "ACCEPTED" | "REJECTED" | "MODIFIED";
  source_document?: string;
  source_section?: string;
}

export interface MappingRequest {
  terminology_id: string;
  standard_code: string;
  standard_name: string;
  standard_system: string;
  reviewer: string;
  note?: string;
}

export interface BatchMappingRequest {
  mappings: MappingRequest[];
  reviewer: string;
  note?: string;
}

export interface TerminologyListResponse {
  items: TerminologyItem[];
  total: number;
  page: number;
  size: number;
}

export interface AiCandidateListResponse {
  items: AiCandidate[];
  total: number;
}

// ─── API 函数 ───────────────────────────────────────────────────────

/**
 * 获取未映射列表
 */
export async function fetchPendingMappings(params?: {
  type?: TermType;
  page?: number;
  size?: number;
}): Promise<TerminologyListResponse> {
  const queryParams = new URLSearchParams();
  if (params?.type) queryParams.set("type", params.type);
  if (params?.page) queryParams.set("page", params.page.toString());
  if (params?.size) queryParams.set("size", params.size.toString());
  
  const url = `/terminology/pending-mapping${queryParams.toString() ? `?${queryParams.toString()}` : ""}`;
  return get<TerminologyListResponse>(url);
}

/**
 * 获取已映射列表
 */
export async function fetchMappedItems(params?: {
  type?: TermType;
  page?: number;
  size?: number;
}): Promise<TerminologyListResponse> {
  const queryParams = new URLSearchParams();
  if (params?.type) queryParams.set("type", params.type);
  if (params?.page) queryParams.set("page", params.page.toString());
  if (params?.size) queryParams.set("size", params.size.toString());
  
  const url = `/terminology/mapped${queryParams.toString() ? `?${queryParams.toString()}` : ""}`;
  return get<TerminologyListResponse>(url);
}

/**
 * 获取冲突列表
 */
export async function fetchConflictItems(params?: {
  type?: TermType;
  page?: number;
  size?: number;
}): Promise<TerminologyListResponse> {
  const queryParams = new URLSearchParams();
  if (params?.type) queryParams.set("type", params.type);
  if (params?.page) queryParams.set("page", params.page.toString());
  if (params?.size) queryParams.set("size", params.size.toString());
  
  const url = `/terminology/conflicts${queryParams.toString() ? `?${queryParams.toString()}` : ""}`;
  return get<TerminologyListResponse>(url);
}

/**
 * 获取AI候选列表
 */
export async function fetchAiCandidates(params?: {
  terminology_id?: string;
  page?: number;
  size?: number;
}): Promise<AiCandidateListResponse> {
  const queryParams = new URLSearchParams();
  if (params?.terminology_id) queryParams.set("terminology_id", params.terminology_id);
  if (params?.page) queryParams.set("page", params.page.toString());
  if (params?.size) queryParams.set("size", params.size.toString());
  
  const url = `/terminology/ai-candidates${queryParams.toString() ? `?${queryParams.toString()}` : ""}`;
  return get<AiCandidateListResponse>(url);
}

/**
 * 生成AI候选映射
 */
export async function generateAiCandidates(terminology_id: string): Promise<AiCandidate[]> {
  return post<AiCandidate[]>("/terminology/candidate", { terminology_id });
}

/**
 * 采纳单个映射
 */
export async function acceptMapping(request: MappingRequest): Promise<void> {
  await post("/terminology/mapping", request);
}

/**
 * 批量采纳映射
 */
export async function batchAcceptMappings(request: BatchMappingRequest): Promise<void> {
  await post("/terminology/mapping/batch", request);
}

/**
 * 拒绝AI候选
 */
export async function rejectAiCandidate(candidate_id: string, reason: string): Promise<void> {
  await post(`/terminology/ai-candidates/${candidate_id}/reject`, { reason });
}

/**
 * 修改后采纳AI候选
 */
export async function modifyAndAcceptAiCandidate(
  candidate_id: string,
  modified_mapping: Omit<MappingRequest, "terminology_id" | "reviewer">,
  reviewer: string
): Promise<void> {
  await post(`/terminology/ai-candidates/${candidate_id}/modify`, {
    ...modified_mapping,
    reviewer,
  });
}