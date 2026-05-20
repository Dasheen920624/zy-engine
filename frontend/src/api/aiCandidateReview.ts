import { get, post } from "./client";

// ==================== 类型定义 ====================

export interface AiCandidateReview {
  id: number;
  tenantId: number;
  candidateCode: string;
  candidateType: string;
  candidateName: string;
  sourceCode: string;
  sourceName: string;
  modelProvider: string;
  modelName: string;
  confidence: number;
  candidateContent: string;
  reviewStatus: string;
  reviewedBy: string;
  reviewedTime: string;
  reviewNote: string;
  modifiedContent: string;
  qualityFindings: string;
  priority: string;
  createdBy: string;
  createdTime: string;
}

export interface ReviewSummary {
  total: number;
  pending: number;
  approved: number;
  rejected: number;
  modified: number;
}

// ==================== API ====================

export async function listCandidates(params?: {
  candidateType?: string;
  reviewStatus?: string;
  priority?: string;
  limit?: number;
}): Promise<{ items: AiCandidateReview[] }> {
  const qs = new URLSearchParams();
  if (params?.candidateType) qs.set("candidateType", params.candidateType);
  if (params?.reviewStatus) qs.set("reviewStatus", params.reviewStatus);
  if (params?.priority) qs.set("priority", params.priority);
  if (params?.limit) qs.set("limit", String(params.limit));
  const query = qs.toString();
  return get(`/knowledge/candidates${query ? `?${query}` : ""}`);
}

export async function getCandidate(candidateId: number): Promise<AiCandidateReview> {
  return get(`/knowledge/candidates/${candidateId}`);
}

export async function reviewCandidate(
  candidateId: number,
  data: { reviewStatus: string; reviewNote?: string; modifiedContent?: string }
): Promise<void> {
  return post(`/knowledge/candidates/${candidateId}/review`, data);
}

export async function batchReview(data: {
  candidateIds: number[];
  reviewStatus: string;
  reviewNote?: string;
}): Promise<void> {
  return post("/knowledge/candidates/batch-review", data);
}

export async function getReviewSummary(): Promise<ReviewSummary> {
  return get("/knowledge/candidates/summary");
}

export async function getReviewHistory(params?: {
  limit?: number;
}): Promise<{ items: AiCandidateReview[] }> {
  const qs = new URLSearchParams();
  if (params?.limit) qs.set("limit", String(params.limit));
  const query = qs.toString();
  return get(`/knowledge/candidates/history${query ? `?${query}` : ""}`);
}
