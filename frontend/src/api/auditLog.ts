import { get, post } from "./client";

/**
 * 审计日志前端契约（PR-FINAL-09 /admin/audit 等保 2.0 三级收口）。
 *
 * 端点全景：
 *   /api/audit-logs                                    列表（8 个 filter + limit）
 *   /api/audit-logs/summary                            聚合（同样 8 filter，返回按维度分布）
 *   /api/security/admin/audit-chain/verify (POST)      校验指定审计表链完整性
 *   /api/security/admin/audit-chain/status             所有审计表的校验状态
 *
 * 本地 view 类型不污染架构师专属 api/types.ts。
 * 字段命名遵循后端 raw Map<String,Object> 返回的 snake_case 风格，未来 PR-FINAL-16
 * Jackson SNAKE_CASE 全局后契约不变。
 */

/**
 * 受支持的审计表（与 SecurityAdminController.isValidAuditTable 同步）。
 * 任何超出此白名单的表名 verifyAuditChain 都会拒绝。
 */
export const AUDIT_TABLES = [
  "engine_audit_log",
  "sec_auth_audit_log",
  "sec_sso_audit_log",
] as const;
export type AuditTableName = (typeof AUDIT_TABLES)[number];

export const AUDIT_TABLE_LABELS: Record<AuditTableName, string> = {
  engine_audit_log: "引擎主审计表",
  sec_auth_audit_log: "认证审计表",
  sec_sso_audit_log: "SSO 审计表",
};

/**
 * 引擎类型枚举（actionType + engineType 的常见组合用于筛选下拉）。
 * 与后端 EnginePersistenceService.listAuditLogs 查询参数对齐。
 */
export const ENGINE_TYPES = ["CDSS", "PATHWAY", "RULE", "ADAPTER", "WORKFLOW", "AUDIT"] as const;
export type EngineType = (typeof ENGINE_TYPES)[number];

export const ENGINE_TYPE_LABELS: Record<EngineType, string> = {
  CDSS: "CDSS 提醒",
  PATHWAY: "临床路径",
  RULE: "规则引擎",
  ADAPTER: "适配器",
  WORKFLOW: "工作流",
  AUDIT: "审计自身",
};

export const ACTION_TYPES = [
  "PUBLISH",
  "ROLLBACK",
  "EXECUTE",
  "OVERRIDE",
  "DELETE",
  "VERIFY",
  "EXPORT",
  "IMPORT",
] as const;
export type ActionType = (typeof ACTION_TYPES)[number];

export const ACTION_TYPE_LABELS: Record<ActionType, string> = {
  PUBLISH: "发布",
  ROLLBACK: "回滚",
  EXECUTE: "执行",
  OVERRIDE: "覆盖",
  DELETE: "删除",
  VERIFY: "校验",
  EXPORT: "导出",
  IMPORT: "导入",
};

/**
 * 单条审计日志（后端返 raw Map，字段以 EnginePersistenceService.listAuditLogs 实际列名为准）。
 * 用 `unknown` 兜底未知字段，UI 已知字段强类型展示。
 */
export interface AuditLogEntry {
  id?: number | string;
  trace_id?: string;
  engine_type?: string;
  action_type?: string;
  target_type?: string;
  target_code?: string;
  target_version?: string;
  patient_id?: string;
  encounter_id?: string;
  operator_id?: string;
  operator_name?: string;
  tenant_id?: string;
  hospital_code?: string;
  client_ip?: string;
  user_agent?: string;
  request_payload?: string | Record<string, unknown>;
  response_payload?: string | Record<string, unknown>;
  signature?: string;
  prev_signature?: string;
  signature_valid?: boolean;
  created_time?: string;
  [extraKey: string]: unknown;
}

export interface AuditLogFilters {
  trace_id?: string;
  engine_type?: string;
  action_type?: string;
  target_type?: string;
  target_code?: string;
  patient_id?: string;
  encounter_id?: string;
  operator_id?: string;
  /** 默认 20，最大 200（由后端 EnginePersistenceService 内限制）*/
  limit?: number | string;
}

/**
 * 聚合统计（/audit-logs/summary 返回结构，从 raw Map 提取常用字段）。
 * 实际字段以 EnginePersistenceService.summarizeAuditLogs 实现为准；本地 view 类型容错。
 */
export interface AuditLogSummary {
  total?: number;
  by_engine_type?: Array<{ engine_type: string; count: number }>;
  by_action_type?: Array<{ action_type: string; count: number }>;
  by_operator?: Array<{ operator_id: string; count: number }>;
  by_date?: Array<{ date: string; count: number }>;
  [extraKey: string]: unknown;
}

/**
 * 审计链校验单表结果（POST /audit-chain/verify）。
 */
export interface AuditChainCheckpoint {
  checkpoint_id?: number | string;
  checkpoint_time?: string;
  chain_status?: "VALID" | "BROKEN" | "EMPTY" | string;
  total_records?: number;
  valid_records?: number;
  broken_records?: number;
  first_broken_id?: number | string | null;
}

/**
 * 多表校验状态（GET /audit-chain/status）。当前后端实现返
 * `{ table_name: { status: "NOT_VERIFIED" } }`，预留 last_verified_time / chain_status 字段。
 */
export interface AuditChainStatusEntry {
  status?: "VALID" | "BROKEN" | "NOT_VERIFIED" | "EMPTY" | string;
  last_verified_time?: string;
  total_records?: number;
  broken_records?: number;
  first_broken_id?: number | string | null;
  [extraKey: string]: unknown;
}

export type AuditChainStatusMap = Record<string, AuditChainStatusEntry>;

function buildQs(params: Record<string, unknown>): string {
  const qs = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    qs.set(key, String(value));
  });
  const str = qs.toString();
  return str ? `?${str}` : "";
}

/** 审计日志列表（按 filter 过滤；返回 raw 列表，UI 自行渲染/分页客户端） */
export async function listAuditLogs(filters: AuditLogFilters = {}): Promise<AuditLogEntry[]> {
  return get<AuditLogEntry[]>(`/audit-logs${buildQs({ ...filters })}`);
}

/** 审计日志聚合（同 filter，返回 by_engine_type / by_action_type 等维度统计） */
export async function summarizeAuditLogs(filters: AuditLogFilters = {}): Promise<AuditLogSummary> {
  return get<AuditLogSummary>(`/audit-logs/summary${buildQs({ ...filters })}`);
}

/** 触发指定审计表链完整性校验（POST，会写一条 checkpoint 到 sec_audit_chain_checkpoint） */
export async function verifyAuditChain(tableName: AuditTableName): Promise<AuditChainCheckpoint> {
  return post<AuditChainCheckpoint>(`/security/admin/audit-chain/verify`, {
    table_name: tableName,
  });
}

/** 获取所有受支持审计表的最近校验状态（GET，不会写） */
export async function getAuditChainStatus(): Promise<AuditChainStatusMap> {
  return get<AuditChainStatusMap>(`/security/admin/audit-chain/status`);
}
