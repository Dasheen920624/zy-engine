import { get, post } from "./client";
import type {
  QualityAlertListResult,
  QualityAlertSummary,
  ListAlertsParams,
  AssignRequest,
  DashboardKpis,
  DepartmentRankingResponse,
  TrendResponse,
  DepartmentDetail,
} from "./types";

export async function listAlerts(params?: ListAlertsParams): Promise<QualityAlertListResult> {
  const qs = new URLSearchParams();
  if (params?.dept) qs.set("dept", params.dept);
  if (params?.severity) qs.set("severity", params.severity);
  if (params?.date) qs.set("date", params.date);
  if (params?.status) qs.set("status", params.status);
  if (params?.page) qs.set("page", String(params.page));
  if (params?.size) qs.set("size", String(params.size));
  const query = qs.toString();
  return get<QualityAlertListResult>(`/quality/alerts${query ? `?${query}` : ""}`);
}

export async function getAlertSummary(): Promise<QualityAlertSummary> {
  return get<QualityAlertSummary>("/quality/alerts/summary");
}

export async function assignProblem(
  alertId: string,
  request: AssignRequest,
): Promise<Record<string, unknown>> {
  return post(`/quality/problems/${encodeURIComponent(alertId)}/assign`, request);
}

export async function fetchDashboardKpis(params?: { period?: string; departmentCode?: string }): Promise<DashboardKpis> {
  const qs = new URLSearchParams();
  if (params?.period) qs.set("period", params.period);
  if (params?.departmentCode) qs.set("department_code", params.departmentCode);
  const query = qs.toString();
  return get<DashboardKpis>(`/quality/dashboard/kpis${query ? `?${query}` : ""}`);
}

export async function fetchDepartmentRanking(params?: { period?: string }): Promise<DepartmentRankingResponse> {
  const qs = new URLSearchParams();
  if (params?.period) qs.set("period", params.period);
  const query = qs.toString();
  return get<DepartmentRankingResponse>(`/quality/dashboard/department-ranking${query ? `?${query}` : ""}`);
}

export async function fetchTrendData(params?: { days?: number; departmentCode?: string }): Promise<TrendResponse> {
  const qs = new URLSearchParams();
  if (params?.days) qs.set("days", String(params.days));
  if (params?.departmentCode) qs.set("department_code", params.departmentCode);
  const query = qs.toString();
  return get<TrendResponse>(`/quality/dashboard/trend${query ? `?${query}` : ""}`);
}

export async function fetchDepartmentDetail(deptCode: string, params?: { period?: string }): Promise<DepartmentDetail> {
  const qs = new URLSearchParams();
  if (params?.period) qs.set("period", params.period);
  const query = qs.toString();
  return get<DepartmentDetail>(`/quality/department/${encodeURIComponent(deptCode)}${query ? `?${query}` : ""}`);
}
