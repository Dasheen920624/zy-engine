import { request } from './request';

export type ActionMode = 'NOTICE' | 'SOFT' | 'BLOCK';

export interface RuleActionLog {
  log_id: string;
  rule_code: string;
  rule_version: string;
  patient_id: string;
  encounter_id: string;
  order_id: string;
  action_mode: ActionMode;
  decision: 'CONTINUE' | 'MODIFY' | 'CANCEL';
  decision_by: string;
  decision_time: string;
  reason: string;
  informed_consent: boolean;
  family_notified: boolean;
  trace_id: string;
  tenant_id: string;
  group_code: string;
  hospital_code: string;
  campus_code: string;
  site_code: string;
  department_code: string;
  scope_level: string;
  scope_code: string;
  org_source: string;
  created_time: string;
}

export interface RecordDecisionRequest {
  rule_code: string;
  rule_version: string;
  patient_id: string;
  encounter_id: string;
  order_id: string;
  action_mode: ActionMode;
  decision: 'CONTINUE' | 'MODIFY' | 'CANCEL';
  decision_by: string;
  reason?: string;
  informed_consent?: boolean;
  family_notified?: boolean;
}

/**
 * 记录用户决策
 */
export async function recordDecision(request: RecordDecisionRequest): Promise<RuleActionLog> {
  const response = await request.post<RuleActionLog>('/api/rule-action-logs', request);
  return response.data;
}

/**
 * 查询决策日志列表
 */
export async function fetchActionLogs(params: {
  patient_id?: string;
  encounter_id?: string;
  rule_code?: string;
  decision?: string;
  decision_by?: string;
  limit?: number;
}): Promise<RuleActionLog[]> {
  const response = await request.get<RuleActionLog[]>('/api/rule-action-logs', { params });
  return response.data;
}

/**
 * 根据ID获取决策日志
 */
export async function fetchActionLog(logId: string): Promise<RuleActionLog> {
  const response = await request.get<RuleActionLog>(`/api/rule-action-logs/${logId}`);
  return response.data;
}

/**
 * 根据患者查询决策日志
 */
export async function fetchActionLogsByPatient(
  patientId: string,
  encounterId?: string
): Promise<RuleActionLog[]> {
  const params = encounterId ? { encounter_id: encounterId } : {};
  const response = await request.get<RuleActionLog[]>(
    `/api/rule-action-logs/patient/${patientId}`,
    { params }
  );
  return response.data;
}

/**
 * 根据订单查询决策日志
 */
export async function fetchActionLogsByOrder(orderId: string): Promise<RuleActionLog[]> {
  const response = await request.get<RuleActionLog[]>(`/api/rule-action-logs/order/${orderId}`);
  return response.data;
}