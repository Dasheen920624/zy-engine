import { describe, it, expect, vi, beforeEach } from "vitest";
import { mockGet, mockPost, mockPut, resetMocks } from "./testUtils";

vi.mock("../client", () => ({
  http: {
    get: (...args: unknown[]) => vi.fn(...args),
    post: (...args: unknown[]) => vi.fn(...args),
    put: (...args: unknown[]) => vi.fn(...args),
    delete: (...args: unknown[]) => vi.fn(...args),
    patch: (...args: unknown[]) => vi.fn(...args),
  },
  get: (...args: unknown[]) => mockGet(...args),
  post: (...args: unknown[]) => mockPost(...args),
  put: (...args: unknown[]) => mockPut(...args),
  del: (...args: unknown[]) => vi.fn(),
}));

import * as cdss from "../cdss";

beforeEach(resetMocks);

describe("cdss", () => {
  it("evaluateCdss should POST /cdss/evaluate", async () => {
    mockPost.mockResolvedValueOnce([]);
    await cdss.evaluateCdss({
      trigger_point: "ORDER_SAVE",
      patient_context: { patient_id: "P1" },
    });
    expect(mockPost).toHaveBeenCalledWith(
      "/cdss/evaluate",
      { trigger_point: "ORDER_SAVE", patient_context: { patient_id: "P1" } },
    );
  });

  it("resolveCdssAlert should POST /cdss/alerts/{alertId}/resolve", async () => {
    mockPost.mockResolvedValueOnce({});
    await cdss.resolveCdssAlert("ALERT1", {
      override_type: "ACKNOWLEDGE",
      override_reason: "Confirmed",
    });
    expect(mockPost).toHaveBeenCalledWith(
      "/cdss/alerts/ALERT1/resolve",
      { override_type: "ACKNOWLEDGE", override_reason: "Confirmed" },
    );
  });

  it("listCdssAlerts should GET /cdss/alerts with optional patientId", async () => {
    mockGet.mockResolvedValueOnce([]);
    await cdss.listCdssAlerts("P1");
    expect(mockGet).toHaveBeenCalledWith("/cdss/alerts?patientId=P1");
  });

  it("listCdssAlerts should GET /cdss/alerts without query", async () => {
    mockGet.mockResolvedValueOnce([]);
    await cdss.listCdssAlerts();
    expect(mockGet).toHaveBeenCalledWith("/cdss/alerts");
  });

  it("getCdssAlert should GET /cdss/alerts/{alertId}", async () => {
    mockGet.mockResolvedValueOnce({});
    await cdss.getCdssAlert("ALERT1");
    expect(mockGet).toHaveBeenCalledWith("/cdss/alerts/ALERT1");
  });

  it("listFatigueConfigs should GET /cdss/fatigue/configs", async () => {
    mockGet.mockResolvedValueOnce([]);
    await cdss.listFatigueConfigs();
    expect(mockGet).toHaveBeenCalledWith("/cdss/fatigue/configs");
  });

  it("createFatigueConfig should POST /cdss/fatigue/configs", async () => {
    mockPost.mockResolvedValueOnce({});
    await cdss.createFatigueConfig({ deduplication_enabled: true });
    expect(mockPost).toHaveBeenCalledWith(
      "/cdss/fatigue/configs",
      expect.objectContaining({ deduplication_enabled: true }),
    );
  });

  it("updateFatigueConfig should PUT /cdss/fatigue/configs/{configId}", async () => {
    mockPut.mockResolvedValueOnce({});
    await cdss.updateFatigueConfig("CFG1", { deduplication_enabled: false });
    expect(mockPut).toHaveBeenCalledWith("/cdss/fatigue/configs/CFG1", { deduplication_enabled: false });
  });

  it("getOverrideAnalysis should GET /cdss/fatigue/override-analysis", async () => {
    mockGet.mockResolvedValueOnce({});
    await cdss.getOverrideAnalysis();
    expect(mockGet).toHaveBeenCalledWith("/cdss/fatigue/override-analysis");
  });
});
