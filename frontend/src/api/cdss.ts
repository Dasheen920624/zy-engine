import { get, post, put } from "./client";

export interface CdssAlert {
  alertId: string;
  triggerPoint: string;
  riskLevel: "INFO" | "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  title: string;
  message: string;
  evidence: Record<string, unknown>[];
  source: {
    documentCode?: string;
    citationId?: string;
    bindingType?: string;
  };
  requiresConfirmation: boolean;
  isBlocking: boolean;
  override?: {
    overrideReason: string;
    overrideType: "ACKNOWLEDGE" | "OVERRIDE" | "ESCALATE";
    overriddenBy: string;
    overriddenAt: string;
    supervisorName?: string;
    isAuditRedLine: boolean;
  };
  patientId: string;
  encounterId: string;
  ruleCode: string;
  ruleVersion: string;
  createdAt: string;
}

export async function evaluateCdss(params: {
  trigger_point: string;
  patient_context: Record<string, unknown>;
}): Promise<CdssAlert[]> {
  return post<CdssAlert[]>("/cdss/evaluate", params);
}

export async function resolveCdssAlert(
  alertId: string,
  params: {
    override_type: "ACKNOWLEDGE" | "OVERRIDE" | "ESCALATE";
    override_reason?: string;
    operator_name?: string;
    supervisor_name?: string;
  },
): Promise<CdssAlert> {
  return post<CdssAlert>(`/cdss/alerts/${encodeURIComponent(alertId)}/resolve`, params);
}

export async function listCdssAlerts(patientId?: string): Promise<CdssAlert[]> {
  const qs = patientId ? `?patientId=${encodeURIComponent(patientId)}` : "";
  return get<CdssAlert[]>(`/cdss/alerts${qs}`);
}

export async function getCdssAlert(alertId: string): Promise<CdssAlert> {
  return get<CdssAlert>(`/cdss/alerts/${encodeURIComponent(alertId)}`);
}

// ==================== 疲劳治理类型 ====================

export interface AlertFatigueConfigData {
  tenant_id: string;
  config_id: string;
  trigger_point?: string;
  risk_level?: string;
  deduplication_enabled: boolean;
  deduplication_window_minutes: number;
  suppression_enabled: boolean;
  suppression_max_alerts_per_hour: number;
  quiet_period_enabled: boolean;
  quiet_period_minutes: number;
  smart_filter_enabled: boolean;
  override_rate_threshold: number;
  status: "ACTIVE" | "DISABLED";
  created_by?: string;
  created_time?: string;
  updated_time?: string;
}

export interface OverrideAnalysis {
  total_alerts: number;
  total_overrides: number;
  total_acknowledges: number;
  total_escalations: number;
  override_rate: number;
  override_by_rule: Record<string, number>;
  override_by_trigger: Record<string, number>;
  override_by_operator: Record<string, number>;
  high_override_rules: Array<{
    rule_code: string;
    override_count: number;
    alert_count: number;
    override_rate: number;
    recommendation: string;
  }>;
}

// ==================== 疲劳治理 API ====================

export async function listFatigueConfigs(): Promise<AlertFatigueConfigData[]> {
  return get<AlertFatigueConfigData[]>("/cdss/fatigue/configs");
}

export async function createFatigueConfig(data: {
  trigger_point?: string;
  risk_level?: string;
  deduplication_enabled?: boolean;
  deduplication_window_minutes?: number;
  suppression_enabled?: boolean;
  suppression_max_alerts_per_hour?: number;
  quiet_period_enabled?: boolean;
  quiet_period_minutes?: number;
  smart_filter_enabled?: boolean;
  override_rate_threshold?: number;
  created_by?: string;
}): Promise<AlertFatigueConfigData> {
  return post<AlertFatigueConfigData>("/cdss/fatigue/configs", data);
}

export async function updateFatigueConfig(
  configId: string,
  data: Record<string, unknown>,
): Promise<AlertFatigueConfigData> {
  return put<AlertFatigueConfigData>(`/cdss/fatigue/configs/${encodeURIComponent(configId)}`, data);
}

export async function getOverrideAnalysis(): Promise<OverrideAnalysis> {
  return get<OverrideAnalysis>("/cdss/fatigue/override-analysis");
}
