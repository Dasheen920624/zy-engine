import { get, post, put } from "./client";

// ==================== 类型定义 ====================

export interface KnowledgeSourceLicense {
  license_type?: string;
  license_scope?: string;
  redistribution_allowed?: boolean;
  commercial_use_allowed?: boolean;
  export_allowed?: boolean;
}

export interface KnowledgeSourceRegistry {
  tenant_id: string;
  source_code: string;
  source_name: string;
  source_type: string;
  publisher?: string;
  region?: string;
  language?: string;
  release_version?: string;
  release_date?: string;
  effective_date?: string;
  expiry_date?: string;
  authority_level?: string;
  license: KnowledgeSourceLicense;
  fetch_method?: string;
  source_uri?: string;
  raw_hash?: string;
  parsed_hash?: string;
  review_status: "PENDING" | "APPROVED" | "REJECTED" | "DEPRECATED";
  reviewed_by?: string;
  reviewed_time?: string;
  description?: string;
  created_by?: string;
  created_time?: string;
  updated_time?: string;
}

export interface KnowledgeSubscription {
  tenant_id: string;
  subscription_id: string;
  subscriber_id?: string;
  subscriber_name?: string;
  topic_type: "DISEASE" | "DEPARTMENT" | "GUIDELINE" | "INSURANCE" | "DRUG" | "QUALITY";
  topic_code?: string;
  topic_name: string;
  source_types?: string[];
  auto_sync?: boolean;
  sync_frequency?: "DAILY" | "WEEKLY" | "MONTHLY" | "MANUAL";
  status: "ACTIVE" | "PAUSED" | "CANCELLED";
  created_by?: string;
  created_time?: string;
  updated_time?: string;
}

// ==================== 来源注册 API ====================

export async function listKnowledgeSources(params?: {
  source_type?: string;
  review_status?: string;
  authority_level?: string;
}): Promise<KnowledgeSourceRegistry[]> {
  const qs = new URLSearchParams();
  if (params?.source_type) qs.set("source_type", params.source_type);
  if (params?.review_status) qs.set("review_status", params.review_status);
  if (params?.authority_level) qs.set("authority_level", params.authority_level);
  const query = qs.toString();
  return get<KnowledgeSourceRegistry[]>(`/knowledge/sources${query ? `?${query}` : ""}`);
}

export async function getKnowledgeSource(sourceCode: string): Promise<KnowledgeSourceRegistry> {
  return get<KnowledgeSourceRegistry>(`/knowledge/sources/${encodeURIComponent(sourceCode)}`);
}

export async function registerKnowledgeSource(data: {
  source_code?: string;
  source_name: string;
  source_type: string;
  publisher?: string;
  region?: string;
  language?: string;
  release_version?: string;
  release_date?: string;
  effective_date?: string;
  expiry_date?: string;
  authority_level?: string;
  license_scope?: string;
  license_type?: string;
  redistribution_allowed?: boolean;
  commercial_use_allowed?: boolean;
  export_allowed?: boolean;
  fetch_method?: string;
  source_uri?: string;
  description?: string;
  created_by?: string;
}): Promise<KnowledgeSourceRegistry> {
  return post<KnowledgeSourceRegistry>("/knowledge/sources", data);
}

export async function updateKnowledgeSource(
  sourceCode: string,
  data: Record<string, unknown>,
): Promise<KnowledgeSourceRegistry> {
  return put<KnowledgeSourceRegistry>(`/knowledge/sources/${encodeURIComponent(sourceCode)}`, data);
}

export async function reviewKnowledgeSource(
  sourceCode: string,
  data: { review_status: "APPROVED" | "REJECTED"; reviewed_by: string },
): Promise<KnowledgeSourceRegistry> {
  return post<KnowledgeSourceRegistry>(`/knowledge/sources/${encodeURIComponent(sourceCode)}/review`, data);
}

// ==================== 知识订阅 API ====================

export async function listKnowledgeSubscriptions(params?: {
  topic_type?: string;
  status?: string;
  subscriber_id?: string;
}): Promise<KnowledgeSubscription[]> {
  const qs = new URLSearchParams();
  if (params?.topic_type) qs.set("topic_type", params.topic_type);
  if (params?.status) qs.set("status", params.status);
  if (params?.subscriber_id) qs.set("subscriber_id", params.subscriber_id);
  const query = qs.toString();
  return get<KnowledgeSubscription[]>(`/knowledge/subscriptions${query ? `?${query}` : ""}`);
}

export async function createKnowledgeSubscription(data: {
  subscriber_id?: string;
  subscriber_name?: string;
  topic_type: string;
  topic_code?: string;
  topic_name: string;
  source_types?: string[];
  auto_sync?: boolean;
  sync_frequency?: string;
  created_by?: string;
}): Promise<KnowledgeSubscription> {
  return post<KnowledgeSubscription>("/knowledge/subscriptions", data);
}

export async function updateKnowledgeSubscription(
  subscriptionId: string,
  data: Record<string, unknown>,
): Promise<KnowledgeSubscription> {
  return put<KnowledgeSubscription>(`/knowledge/subscriptions/${encodeURIComponent(subscriptionId)}`, data);
}

export async function pauseKnowledgeSubscription(subscriptionId: string): Promise<KnowledgeSubscription> {
  return post<KnowledgeSubscription>(`/knowledge/subscriptions/${encodeURIComponent(subscriptionId)}/pause`, {});
}

export async function cancelKnowledgeSubscription(subscriptionId: string): Promise<KnowledgeSubscription> {
  return post<KnowledgeSubscription>(`/knowledge/subscriptions/${encodeURIComponent(subscriptionId)}/cancel`, {});
}
