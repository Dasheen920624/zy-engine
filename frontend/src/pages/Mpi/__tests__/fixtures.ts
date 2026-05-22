import type { IdentityConflict, PatientIdentity, VisitIdentity } from "../../../api/mpi";
import { buildPatientRecord } from "../helpers";
import type { MpiPatientRecord } from "../helpers";

export const sampleIdentities: PatientIdentity[] = [
  {
    id: 101,
    tenant_id: "TENANT_DEMO",
    platform_patient_id: "P-202605220001",
    identity_type: "ID_CARD",
    external_id: "110101199001011234",
    source_system: "HIS",
    status: "ACTIVE",
    confidence: 98,
    manually_verified: false,
  },
  {
    id: 102,
    tenant_id: "TENANT_DEMO",
    platform_patient_id: "P-202605220001",
    identity_type: "MOBILE_PHONE",
    external_id: "13812345678",
    source_system: "CRM",
    status: "ACTIVE",
    confidence: 92,
    manually_verified: true,
  },
  {
    id: 103,
    tenant_id: "TENANT_DEMO",
    platform_patient_id: "P-202605220001",
    identity_type: "ETHNICITY",
    external_id: "汉族",
    source_system: "EMR",
    status: "ACTIVE",
    confidence: 90,
    manually_verified: true,
  },
];

export const sampleVisits: VisitIdentity[] = [
  {
    id: 201,
    tenant_id: "TENANT_DEMO",
    platform_visit_id: "V-202605220001",
    platform_patient_id: "P-202605220001",
    visit_type: "OUTPATIENT",
    identity_type: "HIS_VISIT_ID",
    external_id: "HIS-V-001",
    source_system: "HIS",
    visit_date: "2026-05-22",
    department_code: "CARD",
    status: "ACTIVE",
  },
];

export const sampleConflicts: IdentityConflict[] = [
  {
    id: 301,
    tenant_id: "TENANT_DEMO",
    conflict_type: "DUPLICATE_EXTERNAL",
    severity: "HIGH",
    patient_identity_ids: "[101,102]",
    conflict_description: "同一身份证命中两个平台患者 ID",
    status: "PENDING",
  },
];

export const samplePatient: MpiPatientRecord = buildPatientRecord({
  platform_patient_id: "P-202605220001",
  identities: sampleIdentities,
  visits: sampleVisits,
  conflicts: sampleConflicts,
});
