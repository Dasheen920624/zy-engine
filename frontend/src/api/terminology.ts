import { get, post } from "./client";
import type {
  TerminologyItem,
  AiCandidate,
  MappingAdoptRequest,
  BatchMappingAdoptRequest,
  MappingSummary,
  MappingStatus,
  ConceptType,
} from "./types";

/** 查询术语映射列表 */
export async function fetchTerminologyMappings(params?: {
  status?: MappingStatus;
  conceptType?: ConceptType;
  sourceSystem?: string;
  keyword?: string;
}): Promise<TerminologyItem[]> {
  const query = new URLSearchParams();
  if (params?.status) query.set("status", params.status);
  if (params?.conceptType) query.set("conceptType", params.conceptType);
  if (params?.sourceSystem) query.set("sourceSystem", params.sourceSystem);
  if (params?.keyword) query.set("keyword", params.keyword);
  const qs = query.toString();
  return get<TerminologyItem[]>(`/terminology/mappings${qs ? `?${qs}` : ""}`);
}

/** 获取映射统计 */
export async function fetchMappingSummary(): Promise<MappingSummary> {
  return get<MappingSummary>("/terminology/mappings/summary");
}

/** 获取 AI 候选映射 */
export async function fetchAiCandidates(params?: {
  conceptType?: ConceptType;
  minConfidence?: number;
}): Promise<AiCandidate[]> {
  const query = new URLSearchParams();
  if (params?.conceptType) query.set("conceptType", params.conceptType);
  if (params?.minConfidence) query.set("minConfidence", String(params.minConfidence));
  const qs = query.toString();
  return get<AiCandidate[]>(`/terminology/ai-candidates${qs ? `?${qs}` : ""}`);
}

/** 采纳单个映射 */
export async function adoptMapping(body: MappingAdoptRequest): Promise<TerminologyItem> {
  return post<TerminologyItem>("/terminology/mappings/adopt", body);
}

/** 批量采纳映射 */
export async function batchAdoptMappings(body: BatchMappingAdoptRequest): Promise<{
  successCount: number;
  failCount: number;
  results: TerminologyItem[];
}> {
  return post("/terminology/mappings/batch-adopt", body);
}

/** 驳回 AI 候选 */
export async function rejectAiCandidate(body: {
  sourceCode: string;
  conceptType: ConceptType;
  operatorId: string;
  comment?: string;
}): Promise<void> {
  return post("/terminology/ai-candidates/reject", body);
}

/** 手动映射（信息科直接指定标准码） */
export async function manualMapping(body: {
  sourceCode: string;
  conceptType: ConceptType;
  standardCode: string;
  standardName?: string;
  operatorId: string;
}): Promise<TerminologyItem> {
  return post<TerminologyItem>("/terminology/mappings/manual", body);
}
