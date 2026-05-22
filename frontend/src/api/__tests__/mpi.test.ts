import { describe, it, expect, vi, beforeEach } from "vitest";
import { mockHttpGet, mockHttpPost, resetMocks } from "./testUtils";

vi.mock("../client", () => ({
  http: {
    get: (...args: unknown[]) => mockHttpGet(...args),
    post: (...args: unknown[]) => mockHttpPost(...args),
    put: (...args: unknown[]) => vi.fn(),
    delete: (...args: unknown[]) => vi.fn(),
    patch: (...args: unknown[]) => vi.fn(),
  },
  get: (...args: unknown[]) => vi.fn(...args),
  post: (...args: unknown[]) => vi.fn(...args),
  put: (...args: unknown[]) => vi.fn(),
  del: (...args: unknown[]) => vi.fn(),
}));

import * as mpi from "../mpi";

beforeEach(resetMocks);

describe("mpi", () => {
  it("registerPatientIdentity should POST /v1/mpi/patient-identities", async () => {
    mockHttpPost.mockResolvedValueOnce({
      data: { success: true, data: { id: 1, platform_patient_id: "P1" } },
    });
    await mpi.registerPatientIdentity({
      tenant_id: "T1",
      platform_patient_id: "P1",
      identity_type: "HIS_PATIENT_ID",
      external_id: "EXT1",
      source_system: "HIS",
    });
    expect(mockHttpPost).toHaveBeenCalledWith(
      "/v1/mpi/patient-identities",
      expect.objectContaining({ platform_patient_id: "P1" }),
    );
  });

  it("batchRegisterPatientIdentities should POST /v1/mpi/patient-identities/batch", async () => {
    mockHttpPost.mockResolvedValueOnce({
      data: { success: true, data: { registered_count: 2, platform_patient_id: "P1" } },
    });
    await mpi.batchRegisterPatientIdentities({
      tenant_id: "T1",
      platform_patient_id: "P1",
      identities: [{ identity_type: "HIS", external_id: "E1", source_system: "HIS" }],
    });
    expect(mockHttpPost).toHaveBeenCalledWith(
      "/v1/mpi/patient-identities/batch",
      expect.objectContaining({ platform_patient_id: "P1" }),
    );
  });

  it("listPatientIdentities should GET /v1/mpi/patient-identities/{tenant}/{patient}", async () => {
    mockHttpGet.mockResolvedValueOnce({
      data: { success: true, data: [] },
    });
    await mpi.listPatientIdentities("T1", "P1");
    expect(mockHttpGet).toHaveBeenCalledWith("/v1/mpi/patient-identities/T1/P1");
  });

  it("findPatientByExternalId should GET /v1/mpi/patient-identities/external", async () => {
    mockHttpGet.mockResolvedValueOnce({
      data: { success: true, data: {} },
    });
    await mpi.findPatientByExternalId({
      tenant_id: "T1",
      identity_type: "HIS",
      source_system: "HIS",
      external_id: "E1",
    });
    expect(mockHttpGet).toHaveBeenCalled();
    const url = mockHttpGet.mock.calls[0][0] as string;
    expect(url).toContain("/v1/mpi/patient-identities/external");
    expect(url).toContain("tenant_id=T1");
  });

  it("verifyPatientIdentity should POST /v1/mpi/patient-identities/{id}/verify", async () => {
    mockHttpPost.mockResolvedValueOnce({ data: { success: true, data: null } });
    await mpi.verifyPatientIdentity(42, "admin");
    expect(mockHttpPost).toHaveBeenCalledWith(
      "/v1/mpi/patient-identities/42/verify",
      { verified_by: "admin" },
    );
  });

  it("mergePatientIdentities should POST /v1/mpi/patient-identities/merge", async () => {
    mockHttpPost.mockResolvedValueOnce({ data: { success: true, data: null } });
    await mpi.mergePatientIdentities({ source_id: 1, target_id: 2, merged_by: "admin" });
    expect(mockHttpPost).toHaveBeenCalledWith(
      "/v1/mpi/patient-identities/merge",
      { source_id: 1, target_id: 2, merged_by: "admin" },
    );
  });

  it("registerVisitIdentity should POST /v1/mpi/visit-identities", async () => {
    mockHttpPost.mockResolvedValueOnce({
      data: { success: true, data: { id: 1 } },
    });
    await mpi.registerVisitIdentity({
      tenant_id: "T1",
      platform_visit_id: "V1",
      platform_patient_id: "P1",
      visit_type: "OUTPATIENT",
      identity_type: "HIS_VISIT_ID",
      external_id: "E1",
      source_system: "HIS",
    });
    expect(mockHttpPost).toHaveBeenCalledWith(
      "/v1/mpi/visit-identities",
      expect.objectContaining({ platform_visit_id: "V1" }),
    );
  });

  it("listVisitIdentities should GET /v1/mpi/visit-identities/{tenant}/{visit}", async () => {
    mockHttpGet.mockResolvedValueOnce({ data: { success: true, data: [] } });
    await mpi.listVisitIdentities("T1", "V1");
    expect(mockHttpGet).toHaveBeenCalledWith("/v1/mpi/visit-identities/T1/V1");
  });

  it("listPatientVisitIdentities should GET /v1/mpi/visit-identities/patient/{tenant}/{patient}", async () => {
    mockHttpGet.mockResolvedValueOnce({ data: { success: true, data: [] } });
    await mpi.listPatientVisitIdentities("T1", "P1");
    expect(mockHttpGet).toHaveBeenCalledWith("/v1/mpi/visit-identities/patient/T1/P1");
  });

  it("detectConflicts should POST /v1/mpi/conflicts/detect/{tenant}", async () => {
    mockHttpPost.mockResolvedValueOnce({ data: { success: true, data: [] } });
    await mpi.detectConflicts("T1");
    expect(mockHttpPost).toHaveBeenCalledWith("/v1/mpi/conflicts/detect/T1");
  });

  it("getPendingConflicts should GET /v1/mpi/conflicts/pending/{tenant}", async () => {
    mockHttpGet.mockResolvedValueOnce({ data: { success: true, data: [] } });
    await mpi.getPendingConflicts("T1");
    expect(mockHttpGet).toHaveBeenCalledWith("/v1/mpi/conflicts/pending/T1");
  });

  it("resolveConflict should POST /v1/mpi/conflicts/{id}/resolve", async () => {
    mockHttpPost.mockResolvedValueOnce({ data: { success: true, data: null } });
    await mpi.resolveConflict(1, {
      resolution_type: "MERGE",
      resolved_by: "admin",
    });
    expect(mockHttpPost).toHaveBeenCalledWith(
      "/v1/mpi/conflicts/1/resolve",
      expect.objectContaining({ resolution_type: "MERGE" }),
    );
  });
});
