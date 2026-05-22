import { get, post } from "./client";

/**
 * 适配器中心前端契约（PR-FINAL-12 /adapter/hub 收口）。
 *
 * 端点全景：
 *   /api/adapters/definitions                              GET 业务适配器列表
 *   /api/adapters/definitions/{adapterCode}/{queryCode}    GET 单个业务适配器定义
 *   /api/interop/adapters                                  GET 互联互通适配器列表
 *   /api/interop/cds-hooks                                 GET CDS Hooks 服务列表
 *   /api/interop/smart-apps                                GET SMART on FHIR 应用列表
 *   /api/cdss/triggers                                     GET CDSS 触发点列表（含 filter）
 *   /api/cdss/triggers                                     POST 注册触发点
 *   /api/cdss/triggers/{triggerId}                         POST 更新触发点
 *   /api/cdss/triggers/{triggerCode}/execute               POST 执行触发点（测试用）
 *
 * 业务执行端点（query/cds-hooks/smart-apps POST、definitions POST 批量导入、triggers/match）
 * 是后端服务间调用 / 复杂导入场景，本管理页不暴露。
 *
 * 本地 view 类型不污染架构师专属 api/types.ts。
 * 字段命名遵循后端 raw Map<String,Object> 返回的 snake_case 风格。
 */

/** 业务适配器分类（HIS/EMR/LIS/PACS/RIS/HIE 等院内系统）— 标签前端展示用 */
export const ADAPTER_CATEGORY_LABELS: Record<string, string> = {
  HIS: "HIS 医院信息系统",
  EMR: "EMR 电子病历",
  LIS: "LIS 检验信息系统",
  PACS: "PACS 影像归档",
  RIS: "RIS 放射信息系统",
  HIE: "HIE 区域卫生信息平台",
  CIS: "CIS 临床信息系统",
  OTHER: "其他",
};

/** 互联互通标准（PR-FINAL-12 后端 InteropController 注释列出） */
export const INTEROP_STANDARD_LABELS: Record<string, string> = {
  HL7_V2: "HL7 v2 消息",
  FHIR: "HL7 FHIR R4",
  CDA: "HL7 CDA",
  IHE: "IHE 集成规范",
  CDS_HOOKS: "CDS Hooks",
  SMART_ON_FHIR: "SMART on FHIR",
  DICOM: "DICOM 影像",
  REST: "REST 自定义",
};

/** CDSS 触发点类型（与后端 cdss_trigger_point.trigger_type 对齐） */
export const TRIGGER_TYPES = ["EVENT", "ORDER", "DIAGNOSIS", "ENCOUNTER", "TIMER"] as const;
export type TriggerType = (typeof TRIGGER_TYPES)[number];

export const TRIGGER_TYPE_LABELS: Record<TriggerType, string> = {
  EVENT: "事件触发",
  ORDER: "医嘱触发",
  DIAGNOSIS: "诊断触发",
  ENCOUNTER: "就诊触发",
  TIMER: "定时触发",
};

/** 接入策略（access_strategy）：CDS Hooks / SMART app / 内嵌 / 后端推送 */
export const ACCESS_STRATEGIES = ["CDS_HOOKS", "SMART_APP", "EMBED", "PUSH"] as const;
export type AccessStrategy = (typeof ACCESS_STRATEGIES)[number];

export const ACCESS_STRATEGY_LABELS: Record<AccessStrategy, string> = {
  CDS_HOOKS: "CDS Hooks",
  SMART_APP: "SMART on FHIR",
  EMBED: "内嵌组件",
  PUSH: "后端推送",
};

/** 风险等级（risk_level） */
export const RISK_LEVELS = ["LOW", "MEDIUM", "HIGH", "CRITICAL"] as const;
export type RiskLevel = (typeof RISK_LEVELS)[number];

export const RISK_LEVEL_LABELS: Record<RiskLevel, string> = {
  LOW: "低",
  MEDIUM: "中",
  HIGH: "高",
  CRITICAL: "极高",
};

/**
 * 业务适配器定义（/adapters/definitions raw Map 容错）。
 */
export interface AdapterDefinition {
  adapter_code?: string;
  adapter_name?: string;
  adapter_category?: string;
  query_code?: string;
  query_name?: string;
  query_type?: string;
  endpoint_url?: string;
  request_template?: string | Record<string, unknown>;
  response_mapping?: string | Record<string, unknown>;
  enabled?: boolean | string;
  description?: string;
  tenant_id?: string | number;
  hospital_code?: string;
  created_time?: string;
  updated_time?: string;
  [extraKey: string]: unknown;
}

/**
 * 互联互通适配器（HL7/FHIR/CDA/IHE/DICOM）— /interop/adapters。
 */
export interface InteropAdapter {
  adapter_code?: string;
  adapter_name?: string;
  standard?: string;
  endpoint_url?: string;
  description?: string;
  enabled?: boolean | string;
  [extraKey: string]: unknown;
}

/**
 * CDS Hooks 服务定义（/interop/cds-hooks）。
 */
export interface CdsHooksService {
  service_code?: string;
  hook?: string;
  title?: string;
  description?: string;
  endpoint_url?: string;
  prefetch?: Record<string, string>;
  enabled?: boolean | string;
  [extraKey: string]: unknown;
}

/**
 * SMART on FHIR 应用定义（/interop/smart-apps）。
 */
export interface SmartApp {
  app_code?: string;
  app_name?: string;
  launch_url?: string;
  redirect_url?: string;
  scope?: string;
  client_id?: string;
  enabled?: boolean | string;
  [extraKey: string]: unknown;
}

/**
 * CDSS 触发点（与后端 CdssTriggerPoint 实体对齐，camelCase getters，
 * 返回时被 Jackson 默认序列化为 camelCase；与本组件用 snake_case 不一致。
 * 这里保留 camelCase 字段直接对应；未来 PR-FINAL-16 全局 SNAKE_CASE 后调整一次）。
 */
export interface TriggerPoint {
  id?: number;
  tenantId?: number;
  triggerCode?: string;
  triggerName?: string;
  triggerType?: string;
  businessScenario?: string;
  accessStrategy?: string;
  adapterCode?: string;
  endpointUrl?: string;
  ruleCodes?: string;
  pathwayCodes?: string;
  priority?: number;
  riskLevel?: string;
  timeoutMs?: number;
  enabled?: string;
  description?: string;
  createdBy?: string;
  createdTime?: string;
  updatedBy?: string;
  updatedTime?: string;
}

export interface TriggerPointFilters {
  businessScenario?: string;
  accessStrategy?: string;
}

export interface TriggerExecuteResponse {
  trigger_code?: string;
  matched?: boolean;
  execution_time_ms?: number;
  result?: Record<string, unknown>;
  [extraKey: string]: unknown;
}

function buildQs(params: Record<string, unknown>): string {
  const qs = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    qs.set(key, String(value));
  });
  const str = qs.toString();
  return str ? `?${str}` : "";
}

/** 业务适配器定义列表 */
export async function listAdapterDefinitions(): Promise<AdapterDefinition[]> {
  return get<AdapterDefinition[]>(`/adapters/definitions`);
}

/** 单个业务适配器定义详情 */
export async function getAdapterDefinition(
  adapterCode: string,
  queryCode: string,
): Promise<AdapterDefinition> {
  return get<AdapterDefinition>(
    `/adapters/definitions/${encodeURIComponent(adapterCode)}/${encodeURIComponent(queryCode)}`,
  );
}

/** 互联互通适配器列表（HL7/FHIR/CDA/IHE/DICOM） */
export async function listInteropAdapters(): Promise<InteropAdapter[]> {
  return get<InteropAdapter[]>(`/interop/adapters`);
}

/** CDS Hooks 服务列表 */
export async function listCdsHooksServices(): Promise<CdsHooksService[]> {
  return get<CdsHooksService[]>(`/interop/cds-hooks`);
}

/** SMART on FHIR 应用列表 */
export async function listSmartApps(): Promise<SmartApp[]> {
  return get<SmartApp[]>(`/interop/smart-apps`);
}

/** CDSS 触发点列表（可按 businessScenario / accessStrategy 过滤） */
export async function listTriggerPoints(
  filters: TriggerPointFilters = {},
): Promise<TriggerPoint[]> {
  return get<TriggerPoint[]>(`/cdss/triggers${buildQs({ ...filters })}`);
}

/** 注册新触发点 */
export async function registerTriggerPoint(trigger: TriggerPoint): Promise<TriggerPoint> {
  return post<TriggerPoint>(`/cdss/triggers`, trigger);
}

/** 更新触发点（按 triggerId） */
export async function updateTriggerPoint(
  triggerId: number,
  trigger: TriggerPoint,
): Promise<string> {
  return post<string>(`/cdss/triggers/${triggerId}`, trigger);
}

/** 执行触发点（按 triggerCode + 事件数据，用于管理页测试） */
export async function executeTriggerPoint(
  triggerCode: string,
  eventData: Record<string, unknown>,
): Promise<TriggerExecuteResponse> {
  return post<TriggerExecuteResponse>(
    `/cdss/triggers/${encodeURIComponent(triggerCode)}/execute`,
    eventData,
  );
}
