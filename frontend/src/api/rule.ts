import { del, get, post } from "./client";
import type {
  EvaluateRequest,
  EvaluateResponse,
  HitItem,
  Severity,
} from "./types";

/**
 * 规则模块前端契约（PR-FINAL-11）。
 *
 * 仅放本地 view 类型；后端契约对齐 com.medkernel.rule.RuleDefinition / RuleExecLogEntry。
 * `frontend/src/api/types.ts` 是架构师专属共享文件，不在此 PR 修改。
 *
 * 端点全景：
 *   /api/rules                       列表 / 导入
 *   /api/rules/{ruleCode}            详情
 *   /api/rules/{ruleCode}/publish    发布
 *   /api/rules/evaluate              评估（单条）
 *   /api/rules/simulate              试运行
 *   /api/rules/exec-logs             触发历史
 *   /api/rules/exec-logs/{logId}     单条触发详情
 *   /api/rules/exec-logs/summary     触发聚合
 *   /api/rule-engine/evaluate        场景化评估（外部入口）
 */

export type RuleType =
  | "TIME_LIMIT_QC"
  | "CONTENT_QC"
  | "PATHWAY_NODE"
  | "SAFETY"
  | "FOLLOWUP"
  | "OPERATION";

export type RuleStatus = "DRAFT" | "REVIEWED" | "PUBLISHED" | "RETIRED";

export type ActionMode = "NOTICE" | "SOFT" | "BLOCK";

export interface RuleDefinition {
  rule_code: string;
  rule_name: string;
  rule_type: RuleType;
  version_no: string;
  package_code?: string;
  package_version?: string;
  status: RuleStatus;
  severity: Severity;
  enabled: boolean;
  published_by?: string;
  published_time?: string;
  tenant_id?: string;
  group_code?: string;
  hospital_code?: string;
  campus_code?: string;
  site_code?: string;
  department_code?: string;
  scope_level?: string;
  scope_code?: string;
  org_source?: string;
  reference_document_code?: string;
  reference_citation_id?: string;
  reference_binding_type?: string;
  action_mode?: ActionMode;
  decision_required?: boolean;
  /** DSL 主体（与 rule_dsl.schema.json 对齐） */
  rule_json: Record<string, unknown>;
}

export interface RuleExecLog {
  log_id: string;
  trace_id: string;
  rule_code: string;
  rule_version?: string;
  patient_id?: string;
  encounter_id?: string;
  hit: boolean;
  severity?: Severity;
  message?: string;
  elapsed_ms: number;
  result_status?: string;
  error_code?: string;
  error_message?: string;
  actions?: string[];
  evidence?: Array<Record<string, unknown>>;
  created_time?: string;
  tenant_id?: string;
  group_code?: string;
  hospital_code?: string;
  campus_code?: string;
  site_code?: string;
  department_code?: string;
  scope_level?: string;
  scope_code?: string;
  org_source?: string;
}

export interface RuleExecLogQuery {
  rule_code?: string;
  trace_id?: string;
  patient_id?: string;
  encounter_id?: string;
  result_status?: "SUCCESS" | "ERROR";
  hit?: "true" | "false";
  limit?: number;
}

export interface RuleExecLogSummary {
  total: number;
  hit_count: number;
  miss_count: number;
  error_count: number;
  avg_elapsed_ms: number;
  by_rule?: Array<{ rule_code: string; total: number; hit: number }>;
}

export interface RuleListFilters {
  rule_type?: RuleType;
  status?: RuleStatus;
  search?: string;
}

export interface SimulateRequest {
  rule_code?: string;
  rule_json?: Record<string, unknown>;
  patient_context: EvaluateRequest["patient_context"];
}

export interface PublishRuleRequest {
  package_code?: string;
  package_version?: string;
  version_no?: string;
  reviewer_comment?: string;
}

function buildQuery(params: Record<string, unknown>): string {
  const qs = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    qs.set(key, String(value));
  });
  const str = qs.toString();
  return str ? `?${str}` : "";
}

/** 规则列表（后端按 OrganizationContextService 解析 Header 自动过滤组织范围） */
export async function listRules(filters?: RuleListFilters): Promise<RuleDefinition[]> {
  return get<RuleDefinition[]>(`/rules${buildQuery(filters ?? {})}`);
}

/** 单条规则详情（可附 versionNo 拿历史版本） */
export async function getRule(ruleCode: string, versionNo?: string): Promise<RuleDefinition> {
  return get<RuleDefinition>(
    `/rules/${encodeURIComponent(ruleCode)}${buildQuery({ versionNo })}`,
  );
}

/** 发布规则（DRAFT → PUBLISHED） */
export async function publishRule(
  ruleCode: string,
  request: PublishRuleRequest,
): Promise<RuleDefinition> {
  return post<RuleDefinition>(`/rules/${encodeURIComponent(ruleCode)}/publish`, request);
}

/** 导入或更新规则（支持单条 / 批量） */
export async function importRules(
  rules: RuleDefinition | RuleDefinition[],
): Promise<RuleDefinition[]> {
  return post<RuleDefinition[]>(`/rules`, rules);
}

/** 删除单条规则 */
export async function deleteRule(ruleCode: string): Promise<void> {
  await del<void>(`/rules/${encodeURIComponent(ruleCode)}`);
}

/** 单条试运行：给定 patient_context + 单规则 DSL，返回命中详情 */
export async function simulateRule(request: SimulateRequest): Promise<HitItem> {
  return post<HitItem>(`/rules/simulate`, request);
}

/** 标准评估（按规则集 + 场景） */
export async function evaluateRules(request: EvaluateRequest): Promise<HitItem[]> {
  return post<HitItem[]>(`/rules/evaluate`, request);
}

/** 触发历史列表 */
export async function listRuleExecLogs(query: RuleExecLogQuery): Promise<RuleExecLog[]> {
  return get<RuleExecLog[]>(`/rules/exec-logs${buildQuery({ ...query })}`);
}

/** 单条触发详情 */
export async function getRuleExecLog(logId: string): Promise<RuleExecLog> {
  return get<RuleExecLog>(`/rules/exec-logs/${encodeURIComponent(logId)}`);
}

/** 触发聚合统计 */
export async function summarizeRuleExecLogs(
  query: Omit<RuleExecLogQuery, "limit">,
): Promise<RuleExecLogSummary> {
  return get<RuleExecLogSummary>(`/rules/exec-logs/summary${buildQuery({ ...query })}`);
}

/** 场景化评估（用于试运行面板对接 /api/rule-engine/* 外部入口） */
export async function evaluateRuleEngine(
  request: EvaluateRequest,
): Promise<EvaluateResponse> {
  return post<EvaluateResponse>(`/rule-engine/evaluate`, request);
}
