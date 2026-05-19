import { get } from "./client";

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

/** 获取驾驶舱 4 KPI 聚合数据 */
export async function fetchDashboardKpis(params?: {
  period?: string;
  departmentCode?: string;
}): Promise<DashboardKpis> {
  const query = new URLSearchParams();
  if (params?.period) query.set("period", params.period);
  if (params?.departmentCode) query.set("departmentCode", params.departmentCode);
  const qs = query.toString();
  return get<DashboardKpis>(`/quality/dashboard${qs ? `?${qs}` : ""}`);
}

/** 获取科室排名列表 */
export async function fetchDepartmentRanking(params?: {
  period?: string;
}): Promise<DepartmentRankingResponse> {
  const query = new URLSearchParams();
  if (params?.period) query.set("period", params.period);
  const qs = query.toString();
  return get<DepartmentRankingResponse>(`/quality/dashboard/departments${qs ? `?${qs}` : ""}`);
}

/** 获取科室钻取详情 */
export async function fetchDepartmentDetail(
  deptCode: string,
  params?: { period?: string }
): Promise<DepartmentDetail> {
  const query = new URLSearchParams();
  if (params?.period) query.set("period", params.period);
  const qs = query.toString();
  return get<DepartmentDetail>(
    `/quality/dashboard/department/${encodeURIComponent(deptCode)}${qs ? `?${qs}` : ""}`
  );
}

/** 获取趋势数据 */
export async function fetchTrendData(params?: {
  days?: number;
  departmentCode?: string;
}): Promise<TrendResponse> {
  const query = new URLSearchParams();
  if (params?.days) query.set("days", String(params.days));
  if (params?.departmentCode) query.set("departmentCode", params.departmentCode);
  const qs = query.toString();
  return get<TrendResponse>(`/quality/dashboard/trend${qs ? `?${qs}` : ""}`);
}
