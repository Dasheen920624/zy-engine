import { describe, it, expect, vi, beforeEach } from "vitest";
import { mockGet, mockPost, mockPut, mockDel, resetMocks } from "./testUtils";

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
  del: (...args: unknown[]) => mockDel(...args),
}));

import * as pathway from "../pathway";

beforeEach(resetMocks);

describe("pathway", () => {
  it("listPathways should GET /pathways with optional params", async () => {
    mockGet.mockResolvedValueOnce({ items: [], total: 0 });
    await pathway.listPathways({ search: "AMI", status: "PUBLISHED" });
    expect(mockGet).toHaveBeenCalled();
    const url = mockGet.mock.calls[0][0] as string;
    expect(url).toMatch(/^\/pathways\?/);
    expect(url).toContain("search=AMI");
    expect(url).toContain("status=PUBLISHED");
  });

  it("getPathway should GET /pathways/{code}", async () => {
    mockGet.mockResolvedValueOnce({});
    await pathway.getPathway("PW001");
    expect(mockGet).toHaveBeenCalledWith("/pathways/PW001");
  });

  it("getPathway should include versionNo", async () => {
    mockGet.mockResolvedValueOnce({});
    await pathway.getPathway("PW001", "2.0");
    expect(mockGet).toHaveBeenCalledWith("/pathways/PW001?versionNo=2.0");
  });

  it("createPathway should POST /pathways", async () => {
    mockPost.mockResolvedValueOnce({});
    await pathway.createPathway({ name: "AMI" });
    expect(mockPost).toHaveBeenCalledWith("/pathways", { name: "AMI" });
  });

  it("deletePathway should DELETE /pathways/{code}", async () => {
    mockDel.mockResolvedValueOnce({});
    await pathway.deletePathway("PW001");
    expect(mockDel).toHaveBeenCalledWith("/pathways/PW001");
  });

  it("publishPathway should POST /pathways/{code}/publish", async () => {
    mockPost.mockResolvedValueOnce({});
    await pathway.publishPathway("PW001", { version: "1.0" });
    expect(mockPost).toHaveBeenCalledWith("/pathways/PW001/publish", { version: "1.0" });
  });

  it("rollbackPathway should POST /pathways/{code}/rollback", async () => {
    mockPost.mockResolvedValueOnce({});
    await pathway.rollbackPathway("PW001", { version: "1.0" });
    expect(mockPost).toHaveBeenCalledWith("/pathways/PW001/rollback", { version: "1.0" });
  });

  it("savePathwayDraft should PUT /pathways/{code}/draft", async () => {
    mockPut.mockResolvedValueOnce({});
    await pathway.savePathwayDraft("PW001", { nodes: [], edges: [] });
    expect(mockPut).toHaveBeenCalledWith("/pathways/PW001/draft", { nodes: [], edges: [] });
  });

  it("validatePathway should POST /pathways/{code}/validate", async () => {
    mockPost.mockResolvedValueOnce({ valid: true });
    await pathway.validatePathway("PW001", { nodes: [], edges: [] });
    expect(mockPost).toHaveBeenCalledWith("/pathways/PW001/validate", { nodes: [], edges: [] });
  });

  it("submitPathwayReview should POST /pathways/{code}/submit-review", async () => {
    mockPost.mockResolvedValueOnce({});
    await pathway.submitPathwayReview("PW001");
    expect(mockPost).toHaveBeenCalledWith("/pathways/PW001/submit-review", {});
  });

  it("diffPathway should GET /pathways/{code}/diff with from/to", async () => {
    mockGet.mockResolvedValueOnce({});
    await pathway.diffPathway("PW001", "1.0", "2.0");
    expect(mockGet).toHaveBeenCalled();
    const url = mockGet.mock.calls[0][0] as string;
    expect(url).toContain("/pathways/PW001/diff");
    expect(url).toContain("from=1.0");
    expect(url).toContain("to=2.0");
  });

  it("recommendPathwayCandidates should POST /patient-pathways/candidates", async () => {
    mockPost.mockResolvedValueOnce([]);
    await pathway.recommendPathwayCandidates({ patient_id: "P1" });
    expect(mockPost).toHaveBeenCalledWith("/patient-pathways/candidates", { patient_id: "P1" });
  });

  it("admitPatientPathway should POST /patient-pathways/admit", async () => {
    mockPost.mockResolvedValueOnce({});
    await pathway.admitPatientPathway({
      pathway_code: "PW001",
      patient_id: "P1",
      encounter_id: "E1",
    });
    expect(mockPost).toHaveBeenCalledWith("/patient-pathways/admit", {
      pathway_code: "PW001",
      patient_id: "P1",
      encounter_id: "E1",
    });
  });

  it("getPatientPathwayInstance should GET /patient-pathways/{instanceId}", async () => {
    mockGet.mockResolvedValueOnce({});
    await pathway.getPatientPathwayInstance("INST1");
    expect(mockGet).toHaveBeenCalledWith("/patient-pathways/INST1");
  });

  it("getNodeState should GET /patient-pathways/{instanceId}/nodes/{nodeCode}", async () => {
    mockGet.mockResolvedValueOnce({});
    await pathway.getNodeState("INST1", "N1");
    expect(mockGet).toHaveBeenCalledWith("/patient-pathways/INST1/nodes/N1");
  });

  it("completeTask should POST .../tasks/{taskCode}/complete", async () => {
    mockPost.mockResolvedValueOnce({});
    await pathway.completeTask("INST1", "N1", "T1", { result: "ok" });
    expect(mockPost).toHaveBeenCalledWith(
      "/patient-pathways/INST1/nodes/N1/tasks/T1/complete",
      { result: "ok" },
    );
  });

  it("skipTask should POST .../tasks/{taskCode}/skip", async () => {
    mockPost.mockResolvedValueOnce({});
    await pathway.skipTask("INST1", "N1", "T1");
    expect(mockPost).toHaveBeenCalledWith(
      "/patient-pathways/INST1/nodes/N1/tasks/T1/skip",
      {},
    );
  });

  it("completeNode should POST .../nodes/{nodeCode}/complete", async () => {
    mockPost.mockResolvedValueOnce({});
    await pathway.completeNode("INST1", "N1");
    expect(mockPost).toHaveBeenCalledWith(
      "/patient-pathways/INST1/nodes/N1/complete",
      {},
    );
  });

  it("recordVariation should POST .../variations", async () => {
    mockPost.mockResolvedValueOnce({});
    await pathway.recordVariation("INST1", {
      variation_type: "SKIP",
      reason: "test",
    });
    expect(mockPost).toHaveBeenCalledWith(
      "/patient-pathways/INST1/variations",
      { variation_type: "SKIP", reason: "test" },
    );
  });

  it("listPatientPathwayInstances should GET /pathway-instances", async () => {
    mockGet.mockResolvedValueOnce([]);
    await pathway.listPatientPathwayInstances({ pathway_code: "PW001" });
    expect(mockGet).toHaveBeenCalled();
    const url = mockGet.mock.calls[0][0] as string;
    expect(url).toContain("/pathway-instances?");
    expect(url).toContain("pathway_code=PW001");
  });

  it("summarizePatientPathwayInstances should GET /pathway-instances/summary", async () => {
    mockGet.mockResolvedValueOnce({});
    await pathway.summarizePatientPathwayInstances({});
    expect(mockGet).toHaveBeenCalledWith("/pathway-instances/summary");
  });

  it("nodeCompletionSummary should GET /pathway-instances/node-completion", async () => {
    mockGet.mockResolvedValueOnce({});
    await pathway.nodeCompletionSummary({});
    expect(mockGet).toHaveBeenCalledWith("/pathway-instances/node-completion");
  });

  it("nodeStayDurationSummary should GET /pathway-instances/node-stay-duration", async () => {
    mockGet.mockResolvedValueOnce({});
    await pathway.nodeStayDurationSummary({});
    expect(mockGet).toHaveBeenCalledWith("/pathway-instances/node-stay-duration");
  });

  it("listVariations should GET /pathway-variations", async () => {
    mockGet.mockResolvedValueOnce([]);
    await pathway.listVariations({ pathway_code: "PW001" });
    expect(mockGet).toHaveBeenCalled();
    const url = mockGet.mock.calls[0][0] as string;
    expect(url).toContain("/pathway-variations?");
  });

  it("summarizeVariations should GET /pathway-variations/summary", async () => {
    mockGet.mockResolvedValueOnce({});
    await pathway.summarizeVariations({});
    expect(mockGet).toHaveBeenCalledWith("/pathway-variations/summary");
  });
});
