import { get, post, del } from "./client";
import type { PathwayListResult, PathwayDetail, ListPathwaysParams } from "./types";

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
