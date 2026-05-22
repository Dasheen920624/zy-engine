import { get, post } from "./client";

/**
 * 来源追溯模块前端契约（FE-008）。
 *
 * 仅放本地 view 类型；后端契约对齐 com.medkernel.provenance.ProvenanceController。
 *
 * 端点全景：
 *   /api/provenance/source-documents    来源文档管理
 *   /api/provenance/citations           引用片段管理
 *   /api/provenance/bindings            资产绑定管理
 */

export interface SourceDocument {
  document_code: string;
  document_name: string;
  source_type: string;
  publisher?: string;
  publish_date?: string;
  version?: string;
  review_status: string;
  content_summary?: string;
  created_by?: string;
  created_time: string;
  updated_time?: string;
}

export interface SourceCitation {
  citation_id: string;
  document_code: string;
  document_name?: string;
  citation_type: string;
  section?: string;
  content: string;
  page_number?: string;
  created_time: string;
}

export interface SourceAssetBinding {
  binding_id: string;
  document_code: string;
  document_name?: string;
  asset_type: string;
  asset_code: string;
  asset_name?: string;
  binding_type: string;
  confidence?: number;
  created_by?: string;
  created_time: string;
}

export interface SourceDocumentFilters {
  source_type?: string;
  review_status?: string;
  publisher?: string;
  keyword?: string;
  limit?: number;
}

export interface CitationFilters {
  document_code?: string;
  citation_type?: string;
  section?: string;
  limit?: number;
}

export interface BindingFilters {
  asset_type?: string;
  asset_code?: string;
  document_code?: string;
  binding_type?: string;
  limit?: number;
}

/**
 * 查询来源文档列表
 */
export async function listSourceDocuments(
  filters?: SourceDocumentFilters,
): Promise<SourceDocument[]> {
  const qs = new URLSearchParams();
  if (filters?.source_type) qs.set("sourceType", filters.source_type);
  if (filters?.review_status) qs.set("reviewStatus", filters.review_status);
  if (filters?.publisher) qs.set("publisher", filters.publisher);
  if (filters?.keyword) qs.set("keyword", filters.keyword);
  if (filters?.limit) qs.set("limit", String(filters.limit));
  const query = qs.toString();
  return get<SourceDocument[]>(`/provenance/source-documents${query ? `?${query}` : ""}`);
}

/**
 * 查询单个来源文档
 */
export async function getSourceDocument(documentCode: string): Promise<SourceDocument> {
  return get<SourceDocument>(`/provenance/source-documents/${encodeURIComponent(documentCode)}`);
}

/**
 * 查询引用片段列表
 */
export async function listCitations(filters?: CitationFilters): Promise<SourceCitation[]> {
  const qs = new URLSearchParams();
  if (filters?.document_code) qs.set("documentCode", filters.document_code);
  if (filters?.citation_type) qs.set("citationType", filters.citation_type);
  if (filters?.section) qs.set("section", filters.section);
  if (filters?.limit) qs.set("limit", String(filters.limit));
  const query = qs.toString();
  return get<SourceCitation[]>(`/provenance/citations${query ? `?${query}` : ""}`);
}

/**
 * 查询单个引用片段
 */
export async function getCitation(citationId: string): Promise<SourceCitation> {
  return get<SourceCitation>(`/provenance/citations/${encodeURIComponent(citationId)}`);
}

/**
 * 查询文档的引用片段
 */
export async function getCitationsByDocument(documentCode: string): Promise<SourceCitation[]> {
  return get<SourceCitation[]>(
    `/provenance/source-documents/${encodeURIComponent(documentCode)}/citations`,
  );
}

/**
 * 查询资产绑定列表
 */
export async function listBindings(filters?: BindingFilters): Promise<SourceAssetBinding[]> {
  const qs = new URLSearchParams();
  if (filters?.asset_type) qs.set("assetType", filters.asset_type);
  if (filters?.asset_code) qs.set("assetCode", filters.asset_code);
  if (filters?.document_code) qs.set("documentCode", filters.document_code);
  if (filters?.binding_type) qs.set("bindingType", filters.binding_type);
  if (filters?.limit) qs.set("limit", String(filters.limit));
  const query = qs.toString();
  return get<SourceAssetBinding[]>(`/provenance/bindings${query ? `?${query}` : ""}`);
}

/**
 * 查询单个资产绑定
 */
export async function getBinding(bindingId: string): Promise<SourceAssetBinding> {
  return get<SourceAssetBinding>(`/provenance/bindings/${encodeURIComponent(bindingId)}`);
}

/**
 * 查询资产的绑定
 */
export async function getBindingsByAsset(
  assetType: string,
  assetCode: string,
): Promise<SourceAssetBinding[]> {
  return get<SourceAssetBinding[]>(
    `/provenance/assets/${encodeURIComponent(assetType)}/${encodeURIComponent(assetCode)}/bindings`,
  );
}

/**
 * 查询文档的绑定
 */
export async function getBindingsByDocument(documentCode: string): Promise<SourceAssetBinding[]> {
  return get<SourceAssetBinding[]>(
    `/provenance/source-documents/${encodeURIComponent(documentCode)}/bindings`,
  );
}