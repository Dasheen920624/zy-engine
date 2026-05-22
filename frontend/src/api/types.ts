// 后端统一返回信封。与 medkernel-mvp 的 ApiResult 对齐。
export interface ApiResult<T = unknown> {
  success: boolean;
  code: string;
  message: string;
  /** i18n 消息键，前端可据此查找本地化文案 */
  message_key?: string;
  data: T | null;
  trace_id: string;
}

// 统一分页响应。与后端 PagedResult 对齐。
export interface PagedResult<T> {
  items: T[];
  total: number;
  page: number;
  page_size: number;
  total_pages: number;
}

// 用户信息（SEC-001）
export interface UserInfo {
  id: number;
  tenant_id: number;
  username: string;
  display_name: string;
  email?: string;
  phone?: string;
  avatar_url?: string;
  status: string;
  roles: string[];
  permissions: string[];
  org_scopes: Array<{
    scope_level: string;
    scope_code: string;
    scope_name: string;
  }>;
  last_login_time?: string;
}

// 组织上下文五段式。Header / Query / Body 三方合并，Body 优先。
export interface OrgContext {
  tenant_id?: string;
  group_code?: string;
  hospital_code?: string;
  campus_code?: string;
  site_code?: string;
  department_code?: string;
}

// Provider 运行状态（前端视图层使用的数组项，与 system.ts 适配层输出对齐）
// 后端 HealthController 实际返回 providers: { database, graph, dify } 对象结构，
// system.ts 中 fetchSystemProviders 会将其展平为数组以保持组件简洁。
export interface ProviderStatus {
  name: string;                    // 内部 key（"database"/"graph"/"dify"），来自展平的 Map.Entry.key
  role: string;                    // 角色描述（CONFIG_PRIMARY_STORE / GRAPH_QUERY_PROVIDER / WORKFLOW_PROVIDER）
  ready: boolean;                  // 是否可用
  status: string;                  // READY / DISABLED / MISCONFIGURED / FALLBACK
  provider: string;                // 实际 provider 名（如 LOCAL_H2_FILE / NEO4J / DIFY 等）
  reason?: string | null;          // degraded_reason 展平后的别名
}

export interface SystemProviders {
  run_mode: "DB_ONLY" | "HYBRID" | "FULL_INTEGRATION" | "IN_MEMORY_DEMO";
  providers: ProviderStatus[];
  timestamp?: string;
}

// 后端原始结构（用于 system.ts 内部适配，不要直接暴露给视图层）
export interface RawProviderEntry {
  role?: string;
  configured?: boolean;
  ready?: boolean;
  status?: string;
  provider?: string;
  degraded_reason?: string | null;
}

export interface RawSystemProviders {
  service?: string;
  run_mode?: "DB_ONLY" | "HYBRID" | "FULL_INTEGRATION" | "IN_MEMORY_DEMO";
  providers?: Record<string, RawProviderEntry> | RawProviderEntry[];
}

// ─── 规则引擎 (FE-003) ───────────────────────────────────────────────

export type ScenarioCode =
  | "PATHWAY_ENTRY"
  | "AMI_RECOMMEND"
  | "EMR_QC"
  | "INSURANCE_QC"
  | "ORDER_SAFETY"
  | "FOLLOWUP"
  | "OPERATION";

export type Severity = "HIGH" | "MEDIUM" | "LOW" | "INFO";

export interface EvaluateRequest {
  scenario_code: ScenarioCode;
  rule_package_code?: string;
  rule_package_version?: string;
  operator_id?: string;
  patient_context: Record<string, unknown> & {
    patient?: { patient_id: string; gender?: string; age?: number };
    encounter?: {
      encounter_id: string;
      visit_type?: string;
      department_code?: string;
      arrival_time?: string;
    };
    facts?: Record<string, unknown>;
  };
  // 组织上下文（body 显式声明，优先级高于 Header）
  tenant_id?: string;
  hospital_code?: string;
  campus_code?: string;
  department_code?: string;
}

export interface HitItem {
  rule_code: string;
  rule_name?: string;
  rule_version?: string;
  version_no?: string;
  package_code?: string;
  package_version?: string;
  severity: Severity;
  hit?: boolean;
  action_type?: string;
  message: string;
  condition_summary?: string;
  facts_matched?: Record<string, unknown>;
  actions?: string[];
  evidence?: Record<string, unknown>[];
  reference_document_code?: string;
  reference_citation_id?: string;
  reference_binding_type?: string;
  suggested_actions?: string[];
  source_document?: {
    title: string;
    institution?: string;
    version?: string;
    section?: string;
    evidence_level?: string;
    reviewer?: string;
    summary?: string;
  };
}

export interface EvaluateResponse {
  result_id: string;
  batch_id?: string;
  scenario_code: ScenarioCode;
  package_code?: string;
  rule_package_code?: string;
  package_version?: string;
  rule_package_version?: string;
  evaluated_count: number;
  hit_count: number;
  elapsed_ms: number;
  trace_id: string;
  hits?: HitItem[];
  results?: HitItem[];
  warnings?: Array<{ severity?: string; code?: string; message?: string }>;
  org_source?: string;
  tenant_id?: string;
  hospital_code?: string;
  campus_code?: string;
  department_code?: string;
  scope_level?: string;
  scope_code?: string;
  created_time?: string;
}

export interface BatchEvaluateRequest {
  scenario_code: ScenarioCode;
  rule_package_code?: string;
  rule_package_version?: string;
  operator_id?: string;
  items: Array<{
    case_id: string;
    patient_context: EvaluateRequest["patient_context"];
  }>;
}

export interface BatchEvaluateResponse {
  batch_id: string;
  scenario_code: ScenarioCode;
  results: EvaluateResponse[];
  total_evaluated: number;
  total_hit: number;
  elapsed_ms: number;
}

export interface RuleEngineResultSummary {
  result_id: string;
  batch_id?: string;
  scenario_code: ScenarioCode;
  package_code?: string;
  package_version?: string;
  patient_id?: string;
  encounter_id?: string;
  evaluated_count: number;
  hit_count: number;
  elapsed_ms: number;
  source?: string;
  created_time?: string;
}

// ─── 配置包 (FE-004) ───────────────────────────────────────────────

export type AssetType = "RULE" | "PATH" | "GRAPH" | "DIFY" | "WORKFLOW" | "TERMINOLOGY" | "ADAPTER" | "MIXED";

export type PackageStatus = "DRAFT" | "REVIEWED" | "PUBLISHED" | "SYNCED" | "ACTIVE" | "RETIRED";

export type ScopeLevel = "PLATFORM" | "GROUP" | "HOSPITAL" | "CAMPUS" | "SITE" | "DEPARTMENT";

export interface ConfigPackageSummary {
  tenant_id: string;
  package_code: string;
  package_version: string;
  asset_type: AssetType;
  scope_level: ScopeLevel;
  scope_code: string;
  scope_reference?: string;
  status: PackageStatus;
  base_version?: string;
  target_version?: string;
  content_hash: string;
  created_by?: string;
  reviewed_by?: string;
  approved_by?: string;
  created_time?: string;
  reviewed_time?: string;
  published_time?: string;
}

export interface ManifestItem {
  asset_code: string;
  asset_type: string;
  version?: string;
  change_type?: "ADDED" | "MODIFIED" | "UNCHANGED" | "REMOVED";
}

export interface ReviewIssue {
  severity: "ERROR" | "WARNING" | "INFO";
  field: string;
  message: string;
}

export interface SourceReview {
  enabled: boolean;
  blocked: boolean;
  missing_count: number;
  expired_count: number;
  unreviewed_count: number;
  allow_publish: boolean;
  message?: string;
}

export interface ConfigPackageReview extends ConfigPackageSummary {
  declared_content_hash?: string;
  ready_to_publish: boolean;
  issues: ReviewIssue[];
  summary?: {
    asset_count: number;
    manifest_keys: string[];
    full_snapshot_present: boolean;
    diff_present: boolean;
    scope_exists: boolean;
  };
  manifest?: Record<string, unknown>;
  source_review?: SourceReview;
  scope_reference?: string;
}

export interface ConfigPackageDetail extends ConfigPackageSummary {
  declared_content_hash?: string;
  manifest?: Record<string, unknown>;
  diff?: Record<string, unknown>;
  full_snapshot?: Record<string, unknown>;
}

export interface PublishRequest {
  approved_by: string;
  approved_note?: string;
}

// ─── 配置包导入 (FE-004-import) ───────────────────────────────────

export interface ImportUploadResult {
  upload_id: string;
  filename: string;
  file_size: number;
  content_type: string;
}

export interface ImportValidateRequest {
  upload_id: string;
}

export interface ImportValidateResult {
  upload_id: string;
  manifest: {
    package_code: string;
    package_version: string;
    asset_type: AssetType;
    scope_level: ScopeLevel;
    scope_code: string;
    base_version?: string;
    items: ManifestItem[];
  };
  validation_results: Array<{
    check: string;
    status: "PASS" | "FAIL" | "WARN";
    message: string;
  }>;
  valid: boolean;
}

export interface ImportSourceCheckRequest {
  upload_id: string;
}

export interface ImportSourceCheckResult {
  upload_id: string;
  source_review: {
    enabled: boolean;
    blocked: boolean;
    missing_count: number;
    expired_count: number;
    unreviewed_count: number;
    allow_publish: boolean;
    details: Array<{
      asset_code: string;
      asset_type: string;
      status: "reviewed" | "pending" | "rejected" | "missing";
      document_name?: string;
      reviewer?: string;
      reviewed_at?: string;
    }>;
  };
}

export interface ImportImpactRequest {
  upload_id: string;
  target_environment?: string;
}

export interface ImportImpactResult {
  upload_id: string;
  target_environment: string;
  impact: {
    assets_affected: number;
    assets_added: number;
    assets_modified: number;
    assets_removed: number;
    scopes_affected: string[];
    conflicts: Array<{
      asset_code: string;
      conflict_type: string;
      existing_version: string;
      new_version: string;
    }>;
  };
}

export interface ImportConfirmRequest {
  upload_id: string;
  approved_by: string;
  approved_note: string;
  target_environment?: string;
}

// 路径模板相关（PR-V2-06）
export type PathwayStatus = "DRAFT" | "PUBLISHED" | "NONE";

export interface PathwaySummary {
  pathway_code: string;
  pathway_name: string;
  status: PathwayStatus;
  draft_status: "DRAFT" | "NONE";
  published_versions: string[];
  latest_published_version: string | null;
  active_published_version: string | null;
  specialty_code: string | null;
  disease_code: string | null;
  dept: string | null;
  instance_count: number;
  completion_rate: number;
}

export interface PathwayListResult {
  items: PathwaySummary[];
  total: number;
  page: number;
  size: number;
  total_pages: number;
}

export interface PathwayDetail {
  pathway_code: string;
  draft_status: "DRAFT" | "NONE";
  published_versions: string[];
  active_published_version: string | null;
  selected_version: string | null;
  draft_config: Record<string, unknown> | null;
  published_config: Record<string, unknown> | null;
  reference_sources: unknown[];
  reference_warnings: unknown[];
}

export interface ListPathwaysParams {
  search?: string;
  status?: string;
  dept?: string;
  page?: number;
  size?: number;
}

// 质控预警相关（PR-V2-11）
export type AlertSeverity = "CRITICAL" | "WARNING" | "INFO";
export type AlertStatus = "PENDING" | "IN_PROGRESS" | "RESOLVED";

export interface QualityAlert {
  id: string;
  time: string;
  patient_id: string;
  encounter_id: string;
  doctor: string | null;
  rule_code: string;
  rule_name: string | null;
  severity: AlertSeverity;
  message: string | null;
  scenario_code: string | null;
  dept: string | null;
  status: AlertStatus;
  overtime: boolean;
}

export interface QualityAlertListResult {
  items: QualityAlert[];
  total: number;
  page: number;
  size: number;
  total_pages: number;
}

export interface QualityAlertSummary {
  critical: number;
  warning: number;
  info: number;
  overtime: number;
  total: number;
}

export interface ListAlertsParams {
  dept?: string;
  severity?: string;
  date?: string;
  status?: string;
  page?: number;
  size?: number;
}

export interface AssignRequest {
  assignee: string;
  assignee_role: string;
  deadline?: string;
  note?: string;
  assigned_by: string;
}

// 通用错误码（与 00_总入口 §9 一致）
export type ApiErrorCode =
  | "SUCCESS"
  | "VALIDATION_ERROR"
  | "DATA_MISSING"
  | "CONFIG_NOT_FOUND"
  | "ENGINE_TIMEOUT"
  | "ADAPTER_TIMEOUT"
  | "DIFY_TIMEOUT"
  | "DB_ERROR"
  | "UNKNOWN_ERROR"
  | "NO_RULES_MATCHED"
  | "PENDING_MAPPING"
  | "MISSING_SOURCE"
  | "UNAUTHORIZED"
  | "FORBIDDEN"
  | "LOGIN_FAILED"
  | "USER_LOCKED";

export class ApiError extends Error {
  public readonly code: ApiErrorCode;
  public readonly traceId: string;
  public readonly httpStatus?: number;
  public readonly messageKey?: string;

  constructor(code: ApiErrorCode, message: string, traceId = "", httpStatus?: number, messageKey?: string) {
    super(message);
    this.code = code;
    this.traceId = traceId;
    this.httpStatus = httpStatus;
    this.messageKey = messageKey;
    this.name = "ApiError";
  }
}

// ─── 待办工作流 (WF-001) ───────────────────────────────────────────────

export type BusinessType = "REVIEW" | "PUBLISH" | "ROLLBACK" | "RECTIFY" | "KNOWLEDGE" | "COMPLIANCE" | "SYNC";

export type TodoStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED" | "EXPIRED";

export type TodoPriority = "URGENT" | "HIGH" | "NORMAL" | "LOW";

export type AssignedType = "USER" | "ROLE" | "GROUP";

export interface TodoTask {
  taskCode: string;
  businessType: BusinessType;
  businessCode: string;
  businessVersion?: string;
  title: string;
  description?: string;
  priority: TodoPriority;
  status: TodoStatus;
  assignedType: AssignedType;
  assignedTo?: string;
  createdBy: string;
  dueTime?: string;
  completedBy?: string;
  completedTime?: string;
  completedComment?: string;
  cancelledBy?: string;
  cancelledTime?: string;
  cancelReason?: string;
  createdTime: string;
  updatedTime?: string;
}

export interface ApprovalAction {
  actionType: string;
  actionResult: string;
  operatorId: string;
  operatorName?: string;
  comment?: string;
  delegateTo?: string;
  delegateToName?: string;
  createdTime: string;
}

export interface TodoTaskDetail extends TodoTask {
  actions?: ApprovalAction[];
}

export interface TodoSummary {
  totalPending: number;
  urgentCount: number;
  highCount: number;
  normalCount: number;
  lowCount: number;
  overdueCount: number;
  byBusinessType: Record<BusinessType, number>;
}

export interface ApprovalRequest {
  operatorId: string;
  operatorName?: string;
  comment?: string;
}

export interface DelegateRequest extends ApprovalRequest {
  delegateTo: string;
  delegateToName?: string;
}

export interface AddSignRequest extends ApprovalRequest {
  addSignTo: string;
}

export interface CancelRequest {
  operatorId: string;
  reason?: string;
}

// ─── 术语字典映射 (PR-V2-08) ───────────────────────────────────────────────

export type MappingStatus = "UNMAPPED" | "MAPPED" | "CONFLICT" | "AI_CANDIDATE";

export type ConceptType = "DIAGNOSIS" | "PROCEDURE" | "DRUG" | "LAB" | "OBSERVATION" | "DEVICE";

export interface TerminologyItem {
  id: number;
  sourceSystem: string;
  sourceCode: string;
  sourceName: string;
  conceptType: ConceptType;
  mappingStatus: MappingStatus;
  standardCode?: string;
  standardName?: string;
  confidence?: number;
  proposedBy?: string;
  proposedTime?: string;
  reviewedBy?: string;
  reviewedTime?: string;
  reviewComment?: string;
  occurrenceCount?: number;
  lastOccurrenceTime?: string;
}

export interface AiCandidate {
  sourceCode: string;
  sourceName: string;
  conceptType: ConceptType;
  proposedStandardCode: string;
  proposedStandardName: string;
  confidence: number;
  mappingSource: string;
  evidence?: string;
}

export interface MappingAdoptRequest {
  sourceCode: string;
  conceptType: ConceptType;
  standardCode: string;
  standardName?: string;
  operatorId: string;
  comment?: string;
}

export interface BatchMappingAdoptRequest {
  items: MappingAdoptRequest[];
  operatorId: string;
}

export interface MappingSummary {
  totalUnmapped: number;
  totalMapped: number;
  totalConflict: number;
  totalAiCandidate: number;
  byConceptType: Record<ConceptType, number>;
}

// ─── 院级质控驾驶舱 (PR-V2-12) ───────────────────────────────────────────────

/** 路径执行 KPI */
export interface PathwayKpi {
  totalEnrolled: number;
  completed: number;
  variationRate: number;
  enrolledChange: number;
  completedChange: number;
  variationRateChange: number;
}

/** 规则命中 KPI */
export interface RuleKpi {
  realtimeBlock: number;
  softReminder: number;
  hitRate: number;
  blockChange: number;
  reminderChange: number;
  hitRateChange: number;
}

/** 质控问题 KPI */
export interface QcKpi {
  totalIssues: number;
  closedIssues: number;
  rectificationRate: number;
  totalChange: number;
  closedChange: number;
  rectificationRateChange: number;
}

/** 医保风险 KPI */
export interface InsuranceKpi {
  potentialRefund: number;
  refundChange: number;
  riskLevel: "LOW" | "MEDIUM" | "HIGH";
}

/** 驾驶舱 4 KPI 聚合 */
export interface DashboardKpis {
  tenantId: string;
  period: string;
  departmentCode: string | null;
  generatedTime: string;
  pathway: PathwayKpi;
  rule: RuleKpi;
  qc: QcKpi;
  insurance: InsuranceKpi;
}

/** 科室排名项 */
export interface DepartmentRank {
  name: string;
  enrolled: number;
  completionRate: number;
  rectificationRate: number;
  ruleHitRate: number;
  stars: number;
}

/** 科室排名响应 */
export interface DepartmentRankingResponse {
  tenantId: string;
  period: string;
  departments: DepartmentRank[];
  total: number;
}

/** 变异 TOP 项 */
export interface VariationItem {
  pathwayCode: string;
  variationNode: string;
  count: number;
  reason: string;
}

/** 医生绩效项 */
export interface DoctorPerformance {
  name: string;
  cases: number;
  completionRate: number;
  rectificationRate: number;
  ruleHitRate: number;
}

/** 科室钻取详情 */
export interface DepartmentDetail {
  tenantId: string;
  departmentCode: string;
  period: string;
  kpis: {
    pathway: Pick<PathwayKpi, "totalEnrolled" | "completed" | "variationRate">;
    rule: Pick<RuleKpi, "realtimeBlock" | "softReminder" | "hitRate">;
    qc: Pick<QcKpi, "totalIssues" | "closedIssues" | "rectificationRate">;
    insurance: Pick<InsuranceKpi, "potentialRefund" | "refundChange">;
  };
  topVariations: VariationItem[];
  doctorPerformance: DoctorPerformance[];
}

/** 趋势数据项 */
export interface TrendDay {
  date: string;
  pathwayCompletionRate: number;
  ruleHitRate: number;
  qcRectificationRate: number;
  insuranceRiskAmount: number;
}

/** 趋势响应 */
export interface TrendResponse {
  tenantId: string;
  days: number;
  departmentCode: string | null;
  trend: TrendDay[];
}
