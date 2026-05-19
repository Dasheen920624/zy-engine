import { get, post } from "./client";
import type {
  QualityAlertListResult,
  QualityAlertSummary,
  ListAlertsParams,
  AssignRequest,
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
