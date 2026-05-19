import { get, post } from "./client";

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
