import { http } from "./client";

export type PatientIdentityStatus = "ACTIVE" | "INACTIVE" | "MERGED" | "CONFLICT" | string;
export type VisitIdentityStatus = "ACTIVE" | "INACTIVE" | "MERGED" | string;
export type ConflictSeverity = "HIGH" | "MEDIUM" | "LOW" | string;
export type ConflictStatus = "PENDING" | "IN_PROGRESS" | "RESOLVED" | "DISMISSED" | string;
export type ConflictResolutionType = "MERGE" | "SPLIT" | "KEEP_BOTH" | "MANUAL_LINK";

export interface PatientIdentity {
  id: number;
  tenantId: string;
  platformPatientId: string;
  identityType: string;
  externalId: string;
  idHash?: string;
  sourceSystem: string;
  status: PatientIdentityStatus;
  confidence?: number;
  manuallyVerified?: boolean;
  verifiedBy?: string;
  verifiedTime?: string;
  mergedToId?: number;
  remarks?: string;
  createdTime?: string;
  updatedTime?: string;
}

export interface VisitIdentity {
  id: number;
  tenantId: string;
  platformVisitId: string;
  platformPatientId: string;
  visitType: string;
  identityType: string;
  externalId: string;
  idHash?: string;
  sourceSystem: string;
  visitDate?: string;
  departmentCode?: string;
  status: VisitIdentityStatus;
  remarks?: string;
  createdTime?: string;
  updatedTime?: string;
}

export interface IdentityConflict {
  id: number;
  tenantId: string;
  conflictType: string;
  severity: ConflictSeverity;
  patientIdentityIds?: string;
  visitIdentityIds?: string;
  conflictDescription?: string;
  status: ConflictStatus;
  resolutionType?: ConflictResolutionType;
  resolutionNotes?: string;
  resolvedBy?: string;
  resolvedTime?: string;
  targetPatientIdentityId?: number;
  createdTime?: string;
  updatedTime?: string;
}

export interface RegisterPatientIdentityRequest {
  tenantId: string;
  platformPatientId: string;
  identityType: string;
  externalId: string;
  sourceSystem: string;
}

export interface RegisterVisitIdentityRequest {
  tenantId: string;
  platformVisitId: string;
  platformPatientId: string;
  visitType: string;
  identityType: string;
  externalId: string;
  sourceSystem: string;
  visitDate?: string;
  departmentCode?: string;
}

export interface ResolveConflictRequest {
  resolutionType: ConflictResolutionType;
  resolutionNotes?: string;
  resolvedBy: string;
  targetPatientIdentityId?: number;
}

export interface FindPatientByExternalIdRequest {
  tenantId: string;
  identityType: string;
  sourceSystem: string;
  externalId: string;
}

export const PATIENT_IDENTITY_TYPE_LABELS: Record<string, string> = {
  PLATFORM_PATIENT_ID: "平台患者 ID",
  HIS_PATIENT_ID: "HIS 患者号",
  EMR_PATIENT_ID: "EMR 患者号",
  INSURANCE_ID: "医保号",
  OUTPATIENT_ID: "门诊号",
  INPATIENT_ID: "住院号",
  PHYSICAL_CARD_NO: "实体就诊卡",
  ID_CARD: "居民身份证",
  MOBILE_PHONE: "手机号",
};

export const VISIT_IDENTITY_TYPE_LABELS: Record<string, string> = {
  HIS_VISIT_ID: "HIS 就诊号",
  EMR_VISIT_ID: "EMR 就诊号",
  INSURANCE_SETTLEMENT_ID: "医保结算号",
  OUTPATIENT_NO: "门诊流水号",
  INPATIENT_NO: "住院流水号",
};

export const ETHNICITIES = [
  "汉族",
  "蒙古族",
  "回族",
  "藏族",
  "维吾尔族",
  "苗族",
  "彝族",
  "壮族",
  "布依族",
  "朝鲜族",
  "满族",
  "侗族",
  "瑶族",
  "白族",
  "土家族",
  "哈尼族",
  "哈萨克族",
  "傣族",
  "黎族",
  "傈僳族",
  "佤族",
  "畲族",
  "高山族",
  "拉祜族",
  "水族",
  "东乡族",
  "纳西族",
  "景颇族",
  "柯尔克孜族",
  "土族",
  "达斡尔族",
  "仫佬族",
  "羌族",
  "布朗族",
  "撒拉族",
  "毛南族",
  "仡佬族",
  "锡伯族",
  "阿昌族",
  "普米族",
  "塔吉克族",
  "怒族",
  "乌孜别克族",
  "俄罗斯族",
  "鄂温克族",
  "德昂族",
  "保安族",
  "裕固族",
  "京族",
  "塔塔尔族",
  "独龙族",
  "鄂伦春族",
  "赫哲族",
  "门巴族",
  "珞巴族",
  "基诺族",
] as const;

export type Ethnicity = (typeof ETHNICITIES)[number];

interface ApiEnvelope<T> {
  success?: boolean;
  data?: T;
}

function unwrapRawOrEnvelope<T>(payload: T | ApiEnvelope<T>): T {
  if (payload && typeof payload === "object" && "success" in payload && "data" in payload) {
    return (payload as ApiEnvelope<T>).data as T;
  }
  return payload as T;
}

async function rawGet<T>(url: string): Promise<T> {
  const response = await http.get<T | ApiEnvelope<T>>(url);
  return unwrapRawOrEnvelope<T>(response.data);
}

async function rawPost<T>(url: string, body?: unknown): Promise<T> {
  const response = await http.post<T | ApiEnvelope<T>>(url, body);
  return unwrapRawOrEnvelope<T>(response.data);
}

function qs(params: Record<string, string | number | undefined>): string {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === "") return;
    query.set(key, String(value));
  });
  const text = query.toString();
  return text ? `?${text}` : "";
}

export async function registerPatientIdentity(
  request: RegisterPatientIdentityRequest,
): Promise<PatientIdentity> {
  return rawPost<PatientIdentity>("/v1/mpi/patient-identities", request);
}

export async function batchRegisterPatientIdentities(request: {
  tenantId: string;
  platformPatientId: string;
  identities: Array<Pick<RegisterPatientIdentityRequest, "identityType" | "externalId" | "sourceSystem">>;
}): Promise<{ registeredCount: number; platformPatientId: string }> {
  return rawPost<{ registeredCount: number; platformPatientId: string }>(
    "/v1/mpi/patient-identities/batch",
    request,
  );
}

export async function listPatientIdentities(
  tenantId: string,
  platformPatientId: string,
): Promise<PatientIdentity[]> {
  return rawGet<PatientIdentity[]>(
    `/v1/mpi/patient-identities/${encodeURIComponent(tenantId)}/${encodeURIComponent(platformPatientId)}`,
  );
}

export async function findPatientByExternalId(
  request: FindPatientByExternalIdRequest,
): Promise<PatientIdentity> {
  return rawGet<PatientIdentity>(
    `/v1/mpi/patient-identities/external${qs({
      tenantId: request.tenantId,
      identityType: request.identityType,
      sourceSystem: request.sourceSystem,
      externalId: request.externalId,
    })}`,
  );
}

export async function verifyPatientIdentity(identityId: number, verifiedBy: string): Promise<void> {
  await rawPost<void>(`/v1/mpi/patient-identities/${encodeURIComponent(identityId)}/verify`, {
    verifiedBy,
  });
}

export async function mergePatientIdentities(request: {
  sourceId: number;
  targetId: number;
  mergedBy: string;
}): Promise<void> {
  await rawPost<void>("/v1/mpi/patient-identities/merge", request);
}

export async function registerVisitIdentity(
  request: RegisterVisitIdentityRequest,
): Promise<VisitIdentity> {
  return rawPost<VisitIdentity>("/v1/mpi/visit-identities", request);
}

export async function listVisitIdentities(
  tenantId: string,
  platformVisitId: string,
): Promise<VisitIdentity[]> {
  return rawGet<VisitIdentity[]>(
    `/v1/mpi/visit-identities/${encodeURIComponent(tenantId)}/${encodeURIComponent(platformVisitId)}`,
  );
}

export async function listPatientVisitIdentities(
  tenantId: string,
  platformPatientId: string,
): Promise<VisitIdentity[]> {
  return rawGet<VisitIdentity[]>(
    `/v1/mpi/visit-identities/patient/${encodeURIComponent(tenantId)}/${encodeURIComponent(platformPatientId)}`,
  );
}

export async function detectConflicts(tenantId: string): Promise<IdentityConflict[]> {
  return rawPost<IdentityConflict[]>(`/v1/mpi/conflicts/detect/${encodeURIComponent(tenantId)}`);
}

export async function getPendingConflicts(tenantId: string): Promise<IdentityConflict[]> {
  return rawGet<IdentityConflict[]>(`/v1/mpi/conflicts/pending/${encodeURIComponent(tenantId)}`);
}

export async function resolveConflict(
  conflictId: number,
  request: ResolveConflictRequest,
): Promise<void> {
  await rawPost<void>(`/v1/mpi/conflicts/${encodeURIComponent(conflictId)}/resolve`, request);
}
