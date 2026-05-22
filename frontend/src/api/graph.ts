import { get, post } from "./client";

/**
 * 图谱模块前端契约（PR-V3-08）。
 *
 * 仅放本地 view 类型；后端契约对齐 com.medkernel.graph.GraphController。
 *
 * 端点全景：
 *   /api/graph/disease-candidates    疾病候选查询
 *   /api/graph/evidence              证据查询
 *   /api/graph/versions              版本管理
 *   /api/graph/nodes                 节点查询
 *   /api/graph/edges                 边查询
 *   /api/graph/evidences             证据列表
 */

export interface GraphCandidate {
  code: string;
  name: string;
  type: string;
  confidence: number;
  evidence_count: number;
  source_document?: string;
  graph_version: string;
}

export interface GraphEvidence {
  evidence_id: string;
  target_code: string;
  target_name: string;
  evidence_type: string;
  content: string;
  source_document?: string;
  confidence: number;
  graph_version: string;
  created_time: string;
}

export interface GraphNode {
  node_code: string;
  node_name: string;
  node_type: string;
  attributes: Record<string, unknown>;
  graph_version: string;
  created_time: string;
}

export interface GraphEdge {
  edge_id: string;
  from_code: string;
  to_code: string;
  edge_type: string;
  weight: number;
  attributes: Record<string, unknown>;
  graph_version: string;
  created_time: string;
}

export interface GraphVersion {
  version_code: string;
  version_name: string;
  status: "DRAFT" | "ACTIVE" | "ARCHIVED";
  node_count: number;
  edge_count: number;
  evidence_count: number;
  created_by?: string;
  created_time: string;
  activated_by?: string;
  activated_time?: string;
}

export interface DiseaseCandidatesRequest {
  symptoms?: string[];
  findings?: string[];
  patient_context?: Record<string, unknown>;
  limit?: number;
}

export interface EvidenceQueryRequest {
  target_code?: string;
  evidence_type?: string;
  graph_version?: string;
  limit?: number;
}

export interface NodeQueryRequest {
  graph_version?: string;
  node_type?: string;
  keyword?: string;
  limit?: number;
}

export interface EdgeQueryRequest {
  graph_version?: string;
  from_code?: string;
  to_code?: string;
  edge_type?: string;
  limit?: number;
}

/**
 * 查询疾病候选
 */
export async function getDiseaseCandidates(
  request: DiseaseCandidatesRequest,
): Promise<GraphCandidate[]> {
  return post<GraphCandidate[]>("/graph/disease-candidates", request);
}

/**
 * 查询证据
 */
export async function getEvidence(
  request: EvidenceQueryRequest,
): Promise<GraphEvidence[]> {
  return post<GraphEvidence[]>("/graph/evidence", request);
}

/**
 * 查询证据列表
 */
export async function listEvidences(params?: {
  target_code?: string;
  evidence_type?: string;
  graph_version?: string;
  limit?: number;
}): Promise<GraphEvidence[]> {
  const qs = new URLSearchParams();
  if (params?.target_code) qs.set("targetCode", params.target_code);
  if (params?.evidence_type) qs.set("evidenceType", params.evidence_type);
  if (params?.graph_version) qs.set("graphVersion", params.graph_version);
  if (params?.limit) qs.set("limit", String(params.limit));
  const query = qs.toString();
  return get<GraphEvidence[]>(`/graph/evidences${query ? `?${query}` : ""}`);
}

/**
 * 查询单个证据
 */
export async function getEvidenceById(evidenceId: string): Promise<GraphEvidence> {
  return get<GraphEvidence>(`/graph/evidences/${encodeURIComponent(evidenceId)}`);
}

/**
 * 查询图谱版本列表
 */
export async function listGraphVersions(params?: {
  status?: string;
  limit?: number;
}): Promise<GraphVersion[]> {
  const qs = new URLSearchParams();
  if (params?.status) qs.set("status", params.status);
  if (params?.limit) qs.set("limit", String(params.limit));
  const query = qs.toString();
  return get<GraphVersion[]>(`/graph/versions${query ? `?${query}` : ""}`);
}

/**
 * 激活图谱版本
 */
export async function activateGraphVersion(versionCode: string): Promise<GraphVersion> {
  return post<GraphVersion>(`/graph/versions/${encodeURIComponent(versionCode)}/activate`, {});
}

/**
 * 查询节点列表
 */
export async function listNodes(request: NodeQueryRequest): Promise<GraphNode[]> {
  const qs = new URLSearchParams();
  if (request.graph_version) qs.set("graphVersion", request.graph_version);
  if (request.node_type) qs.set("type", request.node_type);
  if (request.keyword) qs.set("keyword", request.keyword);
  if (request.limit) qs.set("limit", String(request.limit));
  const query = qs.toString();
  return get<GraphNode[]>(`/graph/nodes${query ? `?${query}` : ""}`);
}

/**
 * 查询边列表
 */
export async function listEdges(request: EdgeQueryRequest): Promise<GraphEdge[]> {
  const qs = new URLSearchParams();
  if (request.graph_version) qs.set("graphVersion", request.graph_version);
  if (request.from_code) qs.set("fromCode", request.from_code);
  if (request.to_code) qs.set("toCode", request.to_code);
  if (request.edge_type) qs.set("edgeType", request.edge_type);
  if (request.limit) qs.set("limit", String(request.limit));
  const query = qs.toString();
  return get<GraphEdge[]>(`/graph/edges${query ? `?${query}` : ""}`);
}

/**
 * 创建节点
 */
export async function createNode(data: {
  graph_version: string;
  node_code: string;
  node_name: string;
  node_type: string;
  attributes?: Record<string, unknown>;
}): Promise<GraphNode> {
  return post<GraphNode>("/graph/nodes", data);
}

/**
 * 创建边
 */
export async function createEdge(data: {
  graph_version: string;
  from_code: string;
  to_code: string;
  edge_type: string;
  weight?: number;
  attributes?: Record<string, unknown>;
}): Promise<GraphEdge> {
  return post<GraphEdge>("/graph/edges", data);
}