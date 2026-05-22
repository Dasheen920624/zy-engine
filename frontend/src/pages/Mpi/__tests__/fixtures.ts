import type { IdentityConflict, PatientIdentity, VisitIdentity } from "../../../api/mpi";
import { buildPatientRecord } from "../helpers";
import type { MpiPatientRecord } from "../helpers";

export const sampleIdentities: PatientIdentity[] = [
  {
    id: 101,
    tenantId: "TENANT_DEMO",
    platformPatientId: "P-202605220001",
    identityType: "ID_CARD",
    externalId: "110101199001011234",
    sourceSystem: "HIS",
    status: "ACTIVE",
    confidence: 98,
    manuallyVerified: false,
  },
  {
    id: 102,
    tenantId: "TENANT_DEMO",
    platformPatientId: "P-202605220001",
    identityType: "MOBILE_PHONE",
    externalId: "13812345678",
    sourceSystem: "CRM",
    status: "ACTIVE",
    confidence: 92,
    manuallyVerified: true,
  },
  {
    id: 103,
    tenantId: "TENANT_DEMO",
    platformPatientId: "P-202605220001",
    identityType: "ETHNICITY",
    externalId: "汉族",
    sourceSystem: "EMR",
    status: "ACTIVE",
    confidence: 90,
    manuallyVerified: true,
  },
];

export const sampleVisits: VisitIdentity[] = [
  {
    id: 201,
    tenantId: "TENANT_DEMO",
    platformVisitId: "V-202605220001",
    platformPatientId: "P-202605220001",
    visitType: "OUTPATIENT",
    identityType: "HIS_VISIT_ID",
    externalId: "HIS-V-001",
    sourceSystem: "HIS",
    visitDate: "2026-05-22",
    departmentCode: "CARD",
    status: "ACTIVE",
  },
];

export const sampleConflicts: IdentityConflict[] = [
  {
    id: 301,
    tenantId: "TENANT_DEMO",
    conflictType: "DUPLICATE_EXTERNAL",
    severity: "HIGH",
    patientIdentityIds: "[101,102]",
    conflictDescription: "同一身份证命中两个平台患者 ID",
    status: "PENDING",
  },
];

export const samplePatient: MpiPatientRecord = buildPatientRecord({
  platformPatientId: "P-202605220001",
  identities: sampleIdentities,
  visits: sampleVisits,
  conflicts: sampleConflicts,
});
