import { get, post } from "./client";

/**
 * AI 工作流引擎前端契约（PR-FINAL-13，ADR-0013 去 Dify 化前端收口）。
 *
 * 端点全景：
 *   /api/model-gateway/providers                    LLM Provider 列表
 *   /api/model-gateway/degradation-chains           6 种 callType 的降级链
 *   /api/model-gateway/providers/{type}/status      单 Provider 状态
 *   /api/model-gateway/invoke                       LLM 调用（含 call_type）
 *   /api/dify/workflows                             工作流模板列表
 *   /api/dify/workflows/{code}                      模板详情
 *   /api/dify/workflows/run                         运行工作流
 *   /api/dify/workflows/stats                       调用统计聚合
 *
 * 本地 view 类型不污染架构师专属 api/types.ts。
 */

/**
 * 6 种调用类型（与 ModelGatewayService.DEFAULT_DEGRADATION_CHAINS keys 对齐）。
 *
 * - RESEARCH    医学研究综合（深度 + 准确性）
 * - EXTRACT     抽取（实体/关系/事实）
 * - EMBEDDING   向量嵌入
 * - RERANK      二阶段重排
 * - CRITIC      AI 评审 / 校验
 * - WORKFLOW    Dify 多步工作流（保留 Dify 的唯一场景）
 */
export type CallType = "RESEARCH" | "EXTRACT" | "EMBEDDING" | "RERANK" | "CRITIC" | "WORKFLOW";

export const CALL_TYPE_LABELS: Record<CallType, string> = {
  RESEARCH: "医学研究",
  EXTRACT: "信息抽取",
  EMBEDDING: "向量嵌入",
  RERANK: "二阶重排",
  CRITIC: "AI 评审",
  WORKFLOW: "多步工作流",
};

export const CALL_TYPE_DESCRIPTIONS: Record<CallType, string> = {
  RESEARCH: "深度研究 / 综合判断（如 AMI 风险分层、卒中溶栓决策）",
  EXTRACT: "结构化抽取（病历实体 / 检验数值 / 时间线）",
  EMBEDDING: "文本向量化（用于知识检索 / 相似度匹配）",
  RERANK: "检索后二阶段重排（保证关键文档排序靠前）",
  CRITIC: "AI 自我评审 / 内容合规校验（医学事实校验、来源缺失检查）",
  WORKFLOW: "Dify 多步流程编排（复杂跨系统调用，仅 WORKFLOW 走 Dify）",
};

/**
 * Provider 类型。
 *
 * 8 家国产大模型：QIANWEN / DEEPSEEK / KIMI / ZHIPU / DOUBAO / YI / BAICHUAN / STEPFUN
 * Ollama 本地：OLLAMA_LOCAL
 * Dify（仅 WORKFLOW）：DIFY
 * LOCAL 规则兜底：LOCAL
 */
export type ProviderType =
  | "QIANWEN"
  | "DEEPSEEK"
  | "KIMI"
  | "ZHIPU"
  | "DOUBAO"
  | "YI"
  | "BAICHUAN"
  | "STEPFUN"
  | "OLLAMA_LOCAL"
  | "DIFY"
  | "LOCAL";

export const PROVIDER_LABELS: Record<ProviderType, string> = {
  QIANWEN: "通义千问",
  DEEPSEEK: "DeepSeek",
  KIMI: "Kimi",
  ZHIPU: "智谱 GLM",
  DOUBAO: "豆包",
  YI: "Yi（零一万物）",
  BAICHUAN: "百川",
  STEPFUN: "阶跃星辰",
  OLLAMA_LOCAL: "Ollama 本地",
  DIFY: "Dify（仅 WORKFLOW）",
  LOCAL: "LOCAL 规则兜底",
};

/** 是否为国产大模型（用于 UI 分组显示） */
export const DOMESTIC_PROVIDERS: ProviderType[] = [
  "QIANWEN",
  "DEEPSEEK",
  "KIMI",
  "ZHIPU",
  "DOUBAO",
  "YI",
  "BAICHUAN",
  "STEPFUN",
];

export interface ProviderInfo {
  provider_type: string;
  provider_name?: string;
  ready: boolean;
  status: "READY" | "UNAVAILABLE" | "NOT_FOUND" | string;
  registered?: boolean;
  reason?: string | null;
}

export interface DegradationChain {
  call_type: string;
  chain: string;
  providers: ProviderInfo[];
}

export interface AllDegradationChains {
  [callType: string]: DegradationChain;
}

export interface InvokeRequest {
  call_type: CallType;
  prompt?: string;
  payload?: Record<string, unknown>;
  /** Provider 偏好（可选；默认走降级链）*/
  preferred_provider?: ProviderType;
  /** 患者上下文（用于 EXTRACT / RESEARCH 等场景）*/
  patient_context?: Record<string, unknown>;
}

export interface InvokeResponse {
  call_type: string;
  used_provider: string;
  result: Record<string, unknown> | string;
  elapsed_ms?: number;
  trace_id?: string;
  degraded?: boolean;
  degraded_reason?: string;
}

export interface WorkflowTemplate {
  workflow_code: string;
  workflow_name?: string;
  workflow_version?: string;
  description?: string;
  dify_app_code?: string;
  timeout_ms?: number;
  retry_count?: number;
  input_defaults?: Record<string, unknown>;
  input_mappings?: Record<string, string>;
  required_inputs?: string[];
  degraded_outputs?: Record<string, unknown>;
  reference_document_code?: string;
  reference_binding_type?: string;
}

export interface WorkflowRunRequest {
  workflow_code: string;
  workflow_version?: string;
  inputs?: Record<string, unknown>;
  patient_context?: Record<string, unknown>;
}

export interface WorkflowInvocationStats {
  total: number;
  success: number;
  failed: number;
  degraded: number;
  avg_elapsed_ms?: number;
  by_provider?: Array<{ provider: string; count: number }>;
  by_workflow?: Array<{ workflow_code: string; count: number; success: number; failed: number }>;
  recent?: Array<{
    invocation_id: string;
    workflow_code: string;
    workflow_version?: string;
    status: "SUCCESS" | "FAILED" | "DEGRADED";
    provider: string;
    elapsed_ms: number;
    created_time: string;
    patient_id?: string;
    encounter_id?: string;
  }>;
}

export interface WorkflowStatsFilters {
  workflow_code?: string;
  workflow_version?: string;
  status?: "SUCCESS" | "FAILED" | "DEGRADED";
  provider?: string;
  patient_id?: string;
  encounter_id?: string;
  limit?: number;
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

/** Provider 全量列表（含状态） */
export async function listProviders(): Promise<ProviderInfo[]> {
  return get<ProviderInfo[]>(`/model-gateway/providers`);
}

/** 单 Provider 状态 */
export async function getProviderStatus(providerType: ProviderType | string): Promise<ProviderInfo> {
  return get<ProviderInfo>(
    `/model-gateway/providers/${encodeURIComponent(providerType)}/status`,
  );
}

/** 全部 callType 降级链 */
export async function listDegradationChains(): Promise<AllDegradationChains> {
  return get<AllDegradationChains>(`/model-gateway/degradation-chains`);
}

/** 指定 callType 降级链 */
export async function getDegradationChain(callType: CallType): Promise<DegradationChain> {
  return get<DegradationChain>(`/model-gateway/degradation-chains${buildQs({ call_type: callType })}`);
}

/** LLM 调用（一般供调试 / 演示页用，不在生产前端使用） */
export async function invokeModelGateway(
  callType: CallType,
  body: Omit<InvokeRequest, "call_type">,
): Promise<InvokeResponse> {
  return post<InvokeResponse>(`/model-gateway/invoke${buildQs({ call_type: callType })}`, body);
}

/** 工作流模板列表 */
export async function listWorkflowTemplates(): Promise<WorkflowTemplate[]> {
  return get<WorkflowTemplate[]>(`/dify/workflows`);
}

/** 单个工作流模板详情 */
export async function getWorkflowTemplate(
  workflowCode: string,
  workflowVersion?: string,
): Promise<WorkflowTemplate> {
  return get<WorkflowTemplate>(
    `/dify/workflows/${encodeURIComponent(workflowCode)}${buildQs({ workflowVersion })}`,
  );
}

/** 运行工作流（演示页用） */
export async function runWorkflow(request: WorkflowRunRequest): Promise<Record<string, unknown>> {
  return post<Record<string, unknown>>(`/dify/workflows/run`, request);
}

/** 工作流调用统计 */
export async function workflowInvocationStats(
  filters: WorkflowStatsFilters = {},
): Promise<WorkflowInvocationStats> {
  return get<WorkflowInvocationStats>(`/dify/workflows/stats${buildQs({ ...filters })}`);
}
