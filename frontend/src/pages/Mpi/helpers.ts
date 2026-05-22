import type {
  Ethnicity,
  IdentityConflict,
  PatientIdentity,
  PatientIdentityStatus,
  VisitIdentity,
} from "../../api/mpi";
import { PATIENT_IDENTITY_TYPE_LABELS, VISIT_IDENTITY_TYPE_LABELS } from "../../api/mpi";

const ID_CARD_TYPES = new Set([
  "ID_CARD",
  "NATIONAL_ID",
  "CITIZEN_ID",
  "RESIDENT_ID_CARD",
  "IDENTITY_CARD",
  "身份证",
]);

const PHONE_TYPES = new Set(["PHONE", "MOBILE", "MOBILE_PHONE", "CONTACT_PHONE", "手机号"]);

const NAME_TYPES = new Set(["NAME", "PATIENT_NAME", "姓名"]);

const ETHNICITY_TYPES = new Set(["ETHNICITY", "NATION", "民族"]);

export interface MpiPatientRecord {
  platformPatientId: string;
  displayName: string;
  idCardNo?: string;
  phone?: string;
  ethnicity?: Ethnicity;
  status: PatientIdentityStatus;
  confidence: number;
  identityCount: number;
  verifiedCount: number;
  visitCount: number;
  conflictCount: number;
  sourceSystems: string[];
  identities: PatientIdentity[];
  visits: VisitIdentity[];
  updatedTime?: string;
}

export function maskIdCard(value?: string, reveal = false): string {
  if (!value) return "未登记";
  if (reveal || value.length <= 8) return value;
  return `${value.slice(0, 4)}${"*".repeat(Math.max(4, value.length - 8))}${value.slice(-4)}`;
}

export function maskPhone(value?: string, reveal = false): string {
  if (!value) return "未登记";
  if (reveal || value.length <= 7) return value;
  return `${value.slice(0, 3)}****${value.slice(-4)}`;
}

export function maskExternalId(identity: PatientIdentity, reveal = false): string {
  if (ID_CARD_TYPES.has(identity.identityType)) return maskIdCard(identity.externalId, reveal);
  if (PHONE_TYPES.has(identity.identityType)) return maskPhone(identity.externalId, reveal);
  if (!identity.externalId) return "未登记";
  if (reveal || identity.externalId.length <= 8) return identity.externalId;
  return `${identity.externalId.slice(0, 4)}****${identity.externalId.slice(-4)}`;
}

export function patientIdentityTypeLabel(type: string): string {
  return PATIENT_IDENTITY_TYPE_LABELS[type] || type;
}

export function visitIdentityTypeLabel(type: string): string {
  return VISIT_IDENTITY_TYPE_LABELS[type] || type;
}

export function statusLabel(status: string): string {
  const labels: Record<string, string> = {
    ACTIVE: "有效",
    INACTIVE: "停用",
    MERGED: "已合并",
    CONFLICT: "冲突",
    PENDING: "待处理",
    IN_PROGRESS: "处理中",
    RESOLVED: "已解决",
    DISMISSED: "已忽略",
  };
  return labels[status] || status;
}

export function statusColor(status: string): string {
  const colors: Record<string, string> = {
    ACTIVE: "success",
    INACTIVE: "default",
    MERGED: "processing",
    CONFLICT: "error",
    PENDING: "warning",
    IN_PROGRESS: "processing",
    RESOLVED: "success",
    DISMISSED: "default",
  };
  return colors[status] || "default";
}

export function severityColor(severity: string): string {
  const colors: Record<string, string> = {
    HIGH: "error",
    MEDIUM: "warning",
    LOW: "processing",
  };
  return colors[severity] || "default";
}

export function formatTime(value?: string): string {
  if (!value) return "—";
  return value.replace("T", " ").slice(0, 16);
}

export function parseIdList(value?: string): number[] {
  if (!value) return [];
  try {
    const parsed = JSON.parse(value) as unknown;
    if (Array.isArray(parsed)) {
      return parsed.map((item) => Number(item)).filter((item) => Number.isFinite(item));
    }
  } catch {
    return value
      .split(",")
      .map((item) => Number(item.trim()))
      .filter((item) => Number.isFinite(item));
  }
  return [];
}

function findExternalId(identities: PatientIdentity[], types: Set<string>): string | undefined {
  return identities.find((identity) => types.has(identity.identityType))?.externalId;
}

function deriveStatus(identities: PatientIdentity[]): PatientIdentityStatus {
  if (identities.some((identity) => identity.status === "CONFLICT")) return "CONFLICT";
  if (identities.some((identity) => identity.status === "ACTIVE")) return "ACTIVE";
  return identities[0]?.status || "INACTIVE";
}

export function buildPatientRecord(input: {
  platformPatientId: string;
  identities: PatientIdentity[];
  visits?: VisitIdentity[];
  conflicts?: IdentityConflict[];
}): MpiPatientRecord {
  const identities = input.identities;
  const visits = input.visits ?? [];
  const sourceSystems = Array.from(new Set(identities.map((identity) => identity.sourceSystem))).filter(Boolean);
  const confidenceValues = identities
    .map((identity) => identity.confidence)
    .filter((confidence): confidence is number => typeof confidence === "number");
  const averageConfidence = confidenceValues.length
    ? Math.round(confidenceValues.reduce((sum, value) => sum + value, 0) / confidenceValues.length)
    : 0;
  const conflictCount =
    input.conflicts?.filter((conflict) => conflict.status !== "RESOLVED").length ??
    identities.filter((identity) => identity.status === "CONFLICT").length;
  const name = findExternalId(identities, NAME_TYPES);
  const ethnicity = findExternalId(identities, ETHNICITY_TYPES) as Ethnicity | undefined;
  const newestTime = [...identities.map((identity) => identity.updatedTime), ...visits.map((visit) => visit.updatedTime)]
    .filter((value): value is string => Boolean(value))
    .sort()
    .at(-1);

  return {
    platformPatientId: input.platformPatientId,
    displayName: name || `患者 ${input.platformPatientId.slice(-4) || input.platformPatientId}`,
    idCardNo: findExternalId(identities, ID_CARD_TYPES),
    phone: findExternalId(identities, PHONE_TYPES),
    ethnicity,
    status: deriveStatus(identities),
    confidence: averageConfidence,
    identityCount: identities.length,
    verifiedCount: identities.filter((identity) => identity.manuallyVerified).length,
    visitCount: visits.length,
    conflictCount,
    sourceSystems,
    identities,
    visits,
    updatedTime: newestTime,
  };
}
