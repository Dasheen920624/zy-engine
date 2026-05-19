import { get, post } from './client';
import type { RuleActionLog, ActionMode, DoctorDecision } from '../embed/OrderSafetyBlocker/types';

// ─── API 请求/响应类型 ─────────────────────────────────────────────

export interface CreateRuleActionLogRequest {
  rule_code: string;
  rule_name?: string;
  action_mode: ActionMode;
  patient_id: string;
  encounter_id?: string;
  order_code: string;
  order_name?: string;
  doctor_id: string;
  doctor_name?: string;
  decision: DoctorDecision;
  reason?: string;
  informed_consent?: boolean;
}

export interface ListRuleActionLogsParams {
  patient_id?: string;
  doctor_id?: string;
  rule_code?: string;
  action_mode?: ActionMode;
  decision?: DoctorDecision;
  limit?: number;
}

// ─── API 函数 ──────────────────────────────────────────────────────

/**
 * 创建规则动作日志。
 * 用于记录医嘱安全拦截（BLOCK 模式）下的医生决策。
 */
export async function createRuleActionLog(
  request: CreateRuleActionLogRequest,
): Promise<RuleActionLog> {
  return post<RuleActionLog>('/rule-action-logs', request);
}

/**
 * 查询单条规则动作日志详情。
 */
export async function fetchRuleActionLog(actionLogId: string): Promise<RuleActionLog> {
  return get<RuleActionLog>(`/rule-action-logs/${actionLogId}`);
}

/**
 * 查询规则动作日志列表。
 */
export async function listRuleActionLogs(
  params?: ListRuleActionLogsParams,
): Promise<RuleActionLog[]> {
  const queryParams = new URLSearchParams();
  if (params?.patient_id) queryParams.set('patient_id', params.patient_id);
  if (params?.doctor_id) queryParams.set('doctor_id', params.doctor_id);
  if (params?.rule_code) queryParams.set('rule_code', params.rule_code);
  if (params?.action_mode) queryParams.set('action_mode', params.action_mode);
  if (params?.decision) queryParams.set('decision', params.decision);
  if (params?.limit) queryParams.set('limit', params.limit.toString());
  const qs = queryParams.toString();
  return get<RuleActionLog[]>(`/rule-action-logs${qs ? `?${qs}` : ''}`);
}
