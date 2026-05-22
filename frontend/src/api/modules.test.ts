import { describe, it, expect, vi, beforeEach } from "vitest";

// ─── Mock setup ──────────────────────────────────────────────────────────────
// Some modules import `http` (axios instance) directly, others import thin
// wrappers (`get`, `post`, `put`, `del`).  We mock both patterns.

const mockHttpGet = vi.fn();
const mockHttpPost = vi.fn();
const mockHttpPut = vi.fn();
const mockHttpDelete = vi.fn();
const mockHttpPatch = vi.fn();

const mockGet = vi.fn();
const mockPost = vi.fn();
const mockPut = vi.fn();
const mockDel = vi.fn();

vi.mock("./client", () => ({
  http: {
    get: (...args: unknown[]) => mockHttpGet(...args),
    post: (...args: unknown[]) => mockHttpPost(...args),
    put: (...args: unknown[]) => mockHttpPut(...args),
    delete: (...args: unknown[]) => mockHttpDelete(...args),
    patch: (...args: unknown[]) => mockHttpPatch(...args),
  },
  get: (...args: unknown[]) => mockGet(...args),
  post: (...args: unknown[]) => mockPost(...args),
  put: (...args: unknown[]) => mockPut(...args),
  del: (...args: unknown[]) => mockDel(...args),
}));

// ─── Imports (after mock) ────────────────────────────────────────────────────

import * as auth from "./auth";
import * as rule from "./rule";
import * as pathway from "./pathway";
import * as mpi from "./mpi";
import * as knowledge from "./knowledge";
import * as notification from "./notification";
import * as quality from "./quality";
import * as cdss from "./cdss";
import * as adapterHub from "./adapterHub";
import * as aiWorkflows from "./aiWorkflows";
import * as configPackage from "./configPackage";
import * as graph from "./graph";
import * as provenance from "./provenance";
import * as securityBaseline from "./securityBaseline";
import * as auditLog from "./auditLog";
import * as userAdmin from "./userAdmin";
import * as system from "./system";
import * as evalApi from "./eval";
import * as clinicalEvent from "./clinicalEvent";
import * as sso from "./sso";
import * as tenantOnboarding from "./tenantOnboarding";
import * as identityBinding from "./identityBinding";
import * as ruleActionLog from "./ruleActionLog";
import * as terminology from "./terminology";
import * as aiCandidateReview from "./aiCandidateReview";
import * as workflow from "./workflow";

// ─── Helpers ─────────────────────────────────────────────────────────────────

/** Default mock return values so that API functions don't crash. */
function resetMocks() {
  vi.clearAllMocks();

  // `http` is the raw axios instance; callers typically access resp.data / resp.data.data
  const httpResp = { data: { success: true, data: {} } };
  mockHttpGet.mockResolvedValue(httpResp);
  mockHttpPost.mockResolvedValue(httpResp);
  mockHttpPut.mockResolvedValue(httpResp);
  mockHttpDelete.mockResolvedValue(httpResp);
  mockHttpPatch.mockResolvedValue(httpResp);

  // Thin wrappers already unwrap ApiResult, so they resolve to the inner data
  mockGet.mockResolvedValue({});
  mockPost.mockResolvedValue({});
  mockPut.mockResolvedValue({});
  mockDel.mockResolvedValue(undefined);
}

beforeEach(resetMocks);

// ─── 1. auth ─────────────────────────────────────────────────────────────────

describe("auth", () => {
  it("login should POST /auth/login with credentials", async () => {
    mockHttpPost.mockResolvedValueOnce({
      data: { success: true, data: { token: "t", user: { id: 1, username: "a" } } },
    });
    await auth.login({ username: "admin", password: "123" });
    expect(mockHttpPost).toHaveBeenCalledWith(
      "/auth/login",
      { username: "admin", password: "123" },
    );
  });

  it("fetchCurrentUser should GET /auth/me", async () => {
    mockHttpGet.mockResolvedValueOnce({
      data: { success: true, data: { id: 1, username: "admin" } },
    });
    await auth.fetchCurrentUser();
    expect(mockHttpGet).toHaveBeenCalledWith("/auth/me");
  });

  it("logout should POST /auth/logout", async () => {
    await auth.logout();
    expect(mockHttpPost).toHaveBeenCalledWith("/auth/logout");
  });
});

// ─── 2. rule ─────────────────────────────────────────────────────────────────

describe("rule", () => {
  it("listRules should GET /rules with optional filters", async () => {
    mockGet.mockResolvedValueOnce([]);
    await rule.listRules({ rule_type: "SAFETY", status: "PUBLISHED" });
    expect(mockGet).toHaveBeenCalled();
    const url = mockGet.mock.calls[0][0] as string;
    expect(url).toMatch(/^\/rules\?/);
    expect(url).toContain("rule_type=SAFETY");
    expect(url).toContain("status=PUBLISHED");
  });

  it("listRules should GET /rules without query when no filters", async () => {
    mockGet.mockResolvedValueOnce([]);
    await rule.listRules();
    expect(mockGet).toHaveBeenCalledWith("/rules");
  });

  it("getRule should GET /rules/{ruleCode}", async () => {
    mockGet.mockResolvedValueOnce({});
    await rule.getRule("R001");
    expect(mockGet).toHaveBeenCalledWith("/rules/R001");
  });

  it("getRule should include versionNo query param", async () => {
    mockGet.mockResolvedValueOnce({});
    await rule.getRule("R001", "2.0");
    expect(mockGet).toHaveBeenCalledWith("/rules/R001?versionNo=2.0");
  });

  it("publishRule should POST /rules/{ruleCode}/publish", async () => {
    mockPost.mockResolvedValueOnce({});
    await rule.publishRule("R001", { version_no: "2.0" });
    expect(mockPost).toHaveBeenCalledWith("/rules/R001/publish", { version_no: "2.0" });
  });

  it("importRules should POST /rules with rule data", async () => {
    mockPost.mockResolvedValueOnce([]);
    const rules = [{ rule_code: "R001", rule_name: "Test" }] as rule.RuleDefinition[];
    await rule.importRules(rules);
    expect(mockPost).toHaveBeenCalledWith("/rules", rules);
  });

  it("deleteRule should DELETE /rules/{ruleCode}", async () => {
    mockDel.mockResolvedValueOnce(undefined);
    await rule.deleteRule("R001");
    expect(mockDel).toHaveBeenCalledWith("/rules/R001");
  });

  it("simulateRule should POST /rules/simulate", async () => {
    mockPost.mockResolvedValueOnce({});
    await rule.simulateRule({
      rule_code: "R001",
      patient_context: { patient_id: "P1" },
    });
    expect(mockPost).toHaveBeenCalledWith("/rules/simulate", {
      rule_code: "R001",
      patient_context: { patient_id: "P1" },
    });
  });

  it("evaluateRules should POST /rules/evaluate", async () => {
    mockPost.mockResolvedValueOnce([]);
    await rule.evaluateRules({ patient_id: "P1", scenario_code: "EMR_QC" } as never);
    expect(mockPost).toHaveBeenCalledWith("/rules/evaluate", { patient_id: "P1", scenario_code: "EMR_QC" });
  });

  it("listRuleExecLogs should GET /rules/exec-logs with query", async () => {
    mockGet.mockResolvedValueOnce([]);
    await rule.listRuleExecLogs({ rule_code: "R001", limit: 10 });
    expect(mockGet).toHaveBeenCalled();
    const url = mockGet.mock.calls[0][0] as string;
    expect(url).toMatch(/^\/rules\/exec-logs\?/);
    expect(url).toContain("rule_code=R001");
    expect(url).toContain("limit=10");
  });

  it("getRuleExecLog should GET /rules/exec-logs/{logId}", async () => {
    mockGet.mockResolvedValueOnce({});
    await rule.getRuleExecLog("LOG1");
    expect(mockGet).toHaveBeenCalledWith("/rules/exec-logs/LOG1");
  });

  it("summarizeRuleExecLogs should GET /rules/exec-logs/summary", async () => {
    mockGet.mockResolvedValueOnce({});
    await rule.summarizeRuleExecLogs({});
    expect(mockGet).toHaveBeenCalledWith("/rules/exec-logs/summary");
  });

  it("evaluateRuleEngine should POST /rule-engine/evaluate", async () => {
    mockPost.mockResolvedValueOnce({});
    await rule.evaluateRuleEngine({ patient_id: "P1" } as never);
    expect(mockPost).toHaveBeenCalledWith("/rule-engine/evaluate", { patient_id: "P1" });
  });
});

// ─── 3. pathway ──────────────────────────────────────────────────────────────

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

// ─── 4. mpi ──────────────────────────────────────────────────────────────────

describe("mpi", () => {
  // mpi uses `http` directly via internal rawGet/rawPost helpers

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

// ─── 5. knowledge ────────────────────────────────────────────────────────────

describe("knowledge", () => {
  it("listKnowledgeSources should GET /knowledge/sources with optional params", async () => {
    mockGet.mockResolvedValueOnce([]);
    await knowledge.listKnowledgeSources({ source_type: "GUIDELINE" });
    expect(mockGet).toHaveBeenCalled();
    const url = mockGet.mock.calls[0][0] as string;
    expect(url).toContain("/knowledge/sources?");
    expect(url).toContain("source_type=GUIDELINE");
  });

  it("listKnowledgeSources should GET /knowledge/sources without query", async () => {
    mockGet.mockResolvedValueOnce([]);
    await knowledge.listKnowledgeSources();
    expect(mockGet).toHaveBeenCalledWith("/knowledge/sources");
  });

  it("getKnowledgeSource should GET /knowledge/sources/{sourceCode}", async () => {
    mockGet.mockResolvedValueOnce({});
    await knowledge.getKnowledgeSource("SRC1");
    expect(mockGet).toHaveBeenCalledWith("/knowledge/sources/SRC1");
  });

  it("registerKnowledgeSource should POST /knowledge/sources", async () => {
    mockPost.mockResolvedValueOnce({});
    await knowledge.registerKnowledgeSource({
      source_name: "Test",
      source_type: "GUIDELINE",
    });
    expect(mockPost).toHaveBeenCalledWith(
      "/knowledge/sources",
      expect.objectContaining({ source_name: "Test" }),
    );
  });

  it("updateKnowledgeSource should PUT /knowledge/sources/{sourceCode}", async () => {
    mockPut.mockResolvedValueOnce({});
    await knowledge.updateKnowledgeSource("SRC1", { source_name: "Updated" });
    expect(mockPut).toHaveBeenCalledWith("/knowledge/sources/SRC1", { source_name: "Updated" });
  });

  it("reviewKnowledgeSource should POST /knowledge/sources/{sourceCode}/review", async () => {
    mockPost.mockResolvedValueOnce({});
    await knowledge.reviewKnowledgeSource("SRC1", {
      review_status: "APPROVED",
      reviewed_by: "admin",
    });
    expect(mockPost).toHaveBeenCalledWith(
      "/knowledge/sources/SRC1/review",
      { review_status: "APPROVED", reviewed_by: "admin" },
    );
  });

  it("listKnowledgeSubscriptions should GET /knowledge/subscriptions with optional params", async () => {
    mockGet.mockResolvedValueOnce([]);
    await knowledge.listKnowledgeSubscriptions({ topic_type: "DISEASE" });
    expect(mockGet).toHaveBeenCalled();
    const url = mockGet.mock.calls[0][0] as string;
    expect(url).toContain("/knowledge/subscriptions?");
    expect(url).toContain("topic_type=DISEASE");
  });

  it("createKnowledgeSubscription should POST /knowledge/subscriptions", async () => {
    mockPost.mockResolvedValueOnce({});
    await knowledge.createKnowledgeSubscription({
      topic_type: "DISEASE",
      topic_name: "AMI",
    });
    expect(mockPost).toHaveBeenCalledWith(
      "/knowledge/subscriptions",
      expect.objectContaining({ topic_name: "AMI" }),
    );
  });

  it("updateKnowledgeSubscription should PUT /knowledge/subscriptions/{id}", async () => {
    mockPut.mockResolvedValueOnce({});
    await knowledge.updateKnowledgeSubscription("SUB1", { topic_name: "Updated" });
    expect(mockPut).toHaveBeenCalledWith("/knowledge/subscriptions/SUB1", { topic_name: "Updated" });
  });

  it("pauseKnowledgeSubscription should POST /knowledge/subscriptions/{id}/pause", async () => {
    mockPost.mockResolvedValueOnce({});
    await knowledge.pauseKnowledgeSubscription("SUB1");
    expect(mockPost).toHaveBeenCalledWith("/knowledge/subscriptions/SUB1/pause", {});
  });

  it("cancelKnowledgeSubscription should POST /knowledge/subscriptions/{id}/cancel", async () => {
    mockPost.mockResolvedValueOnce({});
    await knowledge.cancelKnowledgeSubscription("SUB1");
    expect(mockPost).toHaveBeenCalledWith("/knowledge/subscriptions/SUB1/cancel", {});
  });
});

// ─── 6. notification ─────────────────────────────────────────────────────────

describe("notification", () => {
  // notification exports `notificationApi` object that uses `http` directly

  it("notificationApi.createNotification should POST /api/notifications", async () => {
    mockHttpPost.mockResolvedValueOnce({ data: { id: 1 } });
    await notification.notificationApi.createNotification({
      title: "Test",
      content: "Hello",
      notificationType: "SYSTEM",
      recipientId: "U1",
    });
    expect(mockHttpPost).toHaveBeenCalledWith(
      "/api/notifications",
      expect.objectContaining({ title: "Test" }),
    );
  });

  it("notificationApi.fetchNotifications should GET /api/notifications with params", async () => {
    mockHttpGet.mockResolvedValueOnce({ data: [] });
    await notification.notificationApi.fetchNotifications({ recipientId: "U1", status: "UNREAD" });
    expect(mockHttpGet).toHaveBeenCalledWith(
      "/api/notifications",
      expect.objectContaining({ params: { recipientId: "U1", status: "UNREAD" } }),
    );
  });

  it("notificationApi.fetchNotification should GET /api/notifications/{code}", async () => {
    mockHttpGet.mockResolvedValueOnce({ data: {} });
    await notification.notificationApi.fetchNotification("NOTIF1");
    expect(mockHttpGet).toHaveBeenCalledWith("/api/notifications/NOTIF1");
  });

  it("notificationApi.markAsRead should POST /api/notifications/{code}/read", async () => {
    mockHttpPost.mockResolvedValueOnce({ data: {} });
    await notification.notificationApi.markAsRead("NOTIF1");
    expect(mockHttpPost).toHaveBeenCalledWith("/api/notifications/NOTIF1/read");
  });

  it("notificationApi.batchMarkAsRead should POST /api/notifications/batch-read", async () => {
    mockHttpPost.mockResolvedValueOnce({ data: { successCount: 2 } });
    await notification.notificationApi.batchMarkAsRead(["N1", "N2"]);
    expect(mockHttpPost).toHaveBeenCalledWith(
      "/api/notifications/batch-read",
      { notificationCodes: ["N1", "N2"] },
    );
  });

  it("notificationApi.archiveNotification should POST /api/notifications/{code}/archive", async () => {
    mockHttpPost.mockResolvedValueOnce({ data: {} });
    await notification.notificationApi.archiveNotification("NOTIF1");
    expect(mockHttpPost).toHaveBeenCalledWith("/api/notifications/NOTIF1/archive");
  });

  it("notificationApi.fetchUnreadCount should GET /api/notifications/unread-count", async () => {
    mockHttpGet.mockResolvedValueOnce({ data: { unreadCount: 5 } });
    await notification.notificationApi.fetchUnreadCount("U1");
    expect(mockHttpGet).toHaveBeenCalledWith(
      "/api/notifications/unread-count",
      expect.objectContaining({ params: { recipientId: "U1" } }),
    );
  });

  it("notificationApi.fetchNotificationSummary should GET /api/notifications/summary", async () => {
    mockHttpGet.mockResolvedValueOnce({ data: { total: 10, unread: 3 } });
    await notification.notificationApi.fetchNotificationSummary("U1");
    expect(mockHttpGet).toHaveBeenCalledWith(
      "/api/notifications/summary",
      expect.objectContaining({ params: { recipientId: "U1" } }),
    );
  });

  it("notificationApi.cleanupExpiredNotifications should POST /api/notifications/cleanup", async () => {
    mockHttpPost.mockResolvedValueOnce({ data: { cleanedCount: 5 } });
    await notification.notificationApi.cleanupExpiredNotifications();
    expect(mockHttpPost).toHaveBeenCalledWith("/api/notifications/cleanup");
  });
});

// ─── 7. quality ──────────────────────────────────────────────────────────────

describe("quality", () => {
  it("listAlerts should GET /quality/alerts with optional params", async () => {
    mockGet.mockResolvedValueOnce({ items: [], total: 0 });
    await quality.listAlerts({ dept: "CARDIO", severity: "HIGH" });
    expect(mockGet).toHaveBeenCalled();
    const url = mockGet.mock.calls[0][0] as string;
    expect(url).toContain("/quality/alerts?");
    expect(url).toContain("dept=CARDIO");
    expect(url).toContain("severity=HIGH");
  });

  it("listAlerts should GET /quality/alerts without query", async () => {
    mockGet.mockResolvedValueOnce({ items: [], total: 0 });
    await quality.listAlerts();
    expect(mockGet).toHaveBeenCalledWith("/quality/alerts");
  });

  it("getAlertSummary should GET /quality/alerts/summary", async () => {
    mockGet.mockResolvedValueOnce({});
    await quality.getAlertSummary();
    expect(mockGet).toHaveBeenCalledWith("/quality/alerts/summary");
  });

  it("assignProblem should POST /quality/problems/{alertId}/assign", async () => {
    mockPost.mockResolvedValueOnce({});
    await quality.assignProblem("A1", { assignee_id: "U1" } as never);
    expect(mockPost).toHaveBeenCalledWith("/quality/problems/A1/assign", { assignee_id: "U1" });
  });

  it("fetchDashboardKpis should GET /quality/dashboard/kpis", async () => {
    mockGet.mockResolvedValueOnce({});
    await quality.fetchDashboardKpis({ period: "2024-Q1" });
    expect(mockGet).toHaveBeenCalled();
    const url = mockGet.mock.calls[0][0] as string;
    expect(url).toContain("/quality/dashboard/kpis?");
    expect(url).toContain("period=2024-Q1");
  });

  it("fetchDepartmentRanking should GET /quality/dashboard/department-ranking", async () => {
    mockGet.mockResolvedValueOnce({});
    await quality.fetchDepartmentRanking({ period: "2024-Q1" });
    expect(mockGet).toHaveBeenCalled();
    const url = mockGet.mock.calls[0][0] as string;
    expect(url).toContain("/quality/dashboard/department-ranking?");
  });

  it("fetchTrendData should GET /quality/dashboard/trend", async () => {
    mockGet.mockResolvedValueOnce({});
    await quality.fetchTrendData({ days: 30 });
    expect(mockGet).toHaveBeenCalled();
    const url = mockGet.mock.calls[0][0] as string;
    expect(url).toContain("/quality/dashboard/trend?");
    expect(url).toContain("days=30");
  });

  it("fetchDepartmentDetail should GET /quality/department/{code}", async () => {
    mockGet.mockResolvedValueOnce({});
    await quality.fetchDepartmentDetail("CARDIO", { period: "2024-Q1" });
    expect(mockGet).toHaveBeenCalled();
    const url = mockGet.mock.calls[0][0] as string;
    expect(url).toContain("/quality/department/CARDIO?");
  });
});

// ─── 8. cdss ─────────────────────────────────────────────────────────────────

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

// ─── Remaining modules: basic export verification ────────────────────────────

describe("adapterHub", () => {
  it("should export API functions", () => {
    expect(typeof adapterHub).toBe("object");
    expect(typeof adapterHub.listAdapterDefinitions).toBe("function");
    expect(typeof adapterHub.getAdapterDefinition).toBe("function");
    expect(typeof adapterHub.listInteropAdapters).toBe("function");
    expect(typeof adapterHub.listCdsHooksServices).toBe("function");
    expect(typeof adapterHub.listSmartApps).toBe("function");
    expect(typeof adapterHub.listTriggerPoints).toBe("function");
    expect(typeof adapterHub.registerTriggerPoint).toBe("function");
    expect(typeof adapterHub.updateTriggerPoint).toBe("function");
    expect(typeof adapterHub.executeTriggerPoint).toBe("function");
  });
});

describe("aiWorkflows", () => {
  it("should export API functions", () => {
    expect(typeof aiWorkflows).toBe("object");
    expect(typeof aiWorkflows.listProviders).toBe("function");
    expect(typeof aiWorkflows.getProviderStatus).toBe("function");
    expect(typeof aiWorkflows.listDegradationChains).toBe("function");
    expect(typeof aiWorkflows.getDegradationChain).toBe("function");
    expect(typeof aiWorkflows.invokeModelGateway).toBe("function");
    expect(typeof aiWorkflows.listWorkflowTemplates).toBe("function");
    expect(typeof aiWorkflows.getWorkflowTemplate).toBe("function");
    expect(typeof aiWorkflows.runWorkflow).toBe("function");
    expect(typeof aiWorkflows.workflowInvocationStats).toBe("function");
  });
});

describe("configPackage", () => {
  it("should export API functions", () => {
    expect(typeof configPackage).toBe("object");
    expect(typeof configPackage.listPackages).toBe("function");
    expect(typeof configPackage.getPackageDetail).toBe("function");
    expect(typeof configPackage.reviewPackage).toBe("function");
    expect(typeof configPackage.publishPackage).toBe("function");
    expect(typeof configPackage.exportPackage).toBe("function");
    expect(typeof configPackage.importPackageUpload).toBe("function");
    expect(typeof configPackage.importPackageValidate).toBe("function");
    expect(typeof configPackage.importPackageSourceCheck).toBe("function");
    expect(typeof configPackage.importPackageImpact).toBe("function");
    expect(typeof configPackage.importPackageConfirm).toBe("function");
  });
});

describe("graph", () => {
  it("should export API functions", () => {
    expect(typeof graph).toBe("object");
    expect(typeof graph.getDiseaseCandidates).toBe("function");
    expect(typeof graph.getEvidence).toBe("function");
    expect(typeof graph.listEvidences).toBe("function");
    expect(typeof graph.getEvidenceById).toBe("function");
    expect(typeof graph.listGraphVersions).toBe("function");
    expect(typeof graph.activateGraphVersion).toBe("function");
    expect(typeof graph.listNodes).toBe("function");
    expect(typeof graph.listEdges).toBe("function");
    expect(typeof graph.createNode).toBe("function");
    expect(typeof graph.createEdge).toBe("function");
  });
});

describe("provenance", () => {
  it("should export API functions", () => {
    expect(typeof provenance).toBe("object");
    expect(typeof provenance.listSourceDocuments).toBe("function");
    expect(typeof provenance.getSourceDocument).toBe("function");
    expect(typeof provenance.listCitations).toBe("function");
    expect(typeof provenance.getCitation).toBe("function");
    expect(typeof provenance.getCitationsByDocument).toBe("function");
    expect(typeof provenance.listBindings).toBe("function");
    expect(typeof provenance.getBinding).toBe("function");
    expect(typeof provenance.getBindingsByAsset).toBe("function");
    expect(typeof provenance.getBindingsByDocument).toBe("function");
  });
});

describe("securityBaseline", () => {
  it("should export API functions", () => {
    expect(typeof securityBaseline).toBe("object");
    expect(typeof securityBaseline.getAuditChainStatus).toBe("function");
    expect(typeof securityBaseline.verifyAuditChain).toBe("function");
    expect(typeof securityBaseline.listKeyVersions).toBe("function");
    expect(typeof securityBaseline.getActiveKey).toBe("function");
    expect(typeof securityBaseline.rotateKey).toBe("function");
    expect(typeof securityBaseline.revokeKey).toBe("function");
    expect(typeof securityBaseline.getSecurityBaseline).toBe("function");
    expect(typeof securityBaseline.performVulnerabilityScan).toBe("function");
  });
});

describe("auditLog", () => {
  it("should export API functions", () => {
    expect(typeof auditLog).toBe("object");
    expect(typeof auditLog.listAuditLogs).toBe("function");
    expect(typeof auditLog.summarizeAuditLogs).toBe("function");
    expect(typeof auditLog.verifyAuditChain).toBe("function");
    expect(typeof auditLog.getAuditChainStatus).toBe("function");
  });
});

describe("userAdmin", () => {
  it("should export API functions", () => {
    expect(typeof userAdmin).toBe("object");
    expect(typeof userAdmin.listUsers).toBe("function");
    expect(typeof userAdmin.getUserDetail).toBe("function");
    expect(typeof userAdmin.updateUserStatus).toBe("function");
    expect(typeof userAdmin.unlockUser).toBe("function");
    expect(typeof userAdmin.assignRoles).toBe("function");
    expect(typeof userAdmin.resetPassword).toBe("function");
    expect(typeof userAdmin.listRoles).toBe("function");
    expect(typeof userAdmin.importUsers).toBe("function");
  });
});

describe("system", () => {
  it("should export API functions", () => {
    expect(typeof system).toBe("object");
    expect(typeof system.fetchSystemProviders).toBe("function");
    expect(typeof system.fetchOrgContext).toBe("function");
  });
});

describe("eval", () => {
  it("should export API functions", () => {
    expect(typeof evalApi).toBe("object");
    expect(typeof evalApi.listEvalSets).toBe("function");
    expect(typeof evalApi.getEvalSet).toBe("function");
    expect(typeof evalApi.createEvalSet).toBe("function");
    expect(typeof evalApi.updateEvalSet).toBe("function");
    expect(typeof evalApi.publishEvalSet).toBe("function");
    expect(typeof evalApi.deprecateEvalSet).toBe("function");
    expect(typeof evalApi.listEvalIndicators).toBe("function");
    expect(typeof evalApi.getEvalIndicator).toBe("function");
    expect(typeof evalApi.createEvalIndicator).toBe("function");
    expect(typeof evalApi.updateEvalIndicator).toBe("function");
    expect(typeof evalApi.deleteEvalIndicator).toBe("function");
    expect(typeof evalApi.executeEvaluation).toBe("function");
    expect(typeof evalApi.listEvalResults).toBe("function");
    expect(typeof evalApi.getEvalResult).toBe("function");
    expect(typeof evalApi.generateReport).toBe("function");
    expect(typeof evalApi.exportReport).toBe("function");
    expect(typeof evalApi.getReport).toBe("function");
    expect(typeof evalApi.listReports).toBe("function");
    expect(typeof evalApi.archiveReport).toBe("function");
    expect(typeof evalApi.submitReview).toBe("function");
    expect(typeof evalApi.listReviews).toBe("function");
    expect(typeof evalApi.createRectification).toBe("function");
    expect(typeof evalApi.autoCreateRectifications).toBe("function");
    expect(typeof evalApi.updateRectificationStatus).toBe("function");
    expect(typeof evalApi.listRectifications).toBe("function");
    expect(typeof evalApi.reEvaluate).toBe("function");
  });
});

describe("clinicalEvent", () => {
  it("should export REST API functions and ClinicalEventClient class", () => {
    expect(typeof clinicalEvent).toBe("object");
    expect(typeof clinicalEvent.fetchEmbedConfig).toBe("function");
    expect(typeof clinicalEvent.fetchEmbedAlerts).toBe("function");
    expect(typeof clinicalEvent.executeAlertAction).toBe("function");
    expect(typeof clinicalEvent.ClinicalEventClient).toBe("function");
  });
});

describe("sso", () => {
  it("should export API functions", () => {
    expect(typeof sso).toBe("object");
    expect(typeof sso.listSsoProviders).toBe("function");
    expect(typeof sso.initiateSso).toBe("function");
    expect(typeof sso.handleSsoCallback).toBe("function");
    expect(typeof sso.ldapAuthenticate).toBe("function");
  });
});

describe("tenantOnboarding", () => {
  it("should export API functions", () => {
    expect(typeof tenantOnboarding).toBe("object");
    expect(typeof tenantOnboarding.submitTenantApplication).toBe("function");
    expect(typeof tenantOnboarding.approveTenantApplication).toBe("function");
    expect(typeof tenantOnboarding.sendTenantAdminInvitation).toBe("function");
  });
});

describe("identityBinding", () => {
  it("should export API functions", () => {
    expect(typeof identityBinding).toBe("object");
    expect(typeof identityBinding.listBindingsByUser).toBe("function");
    expect(typeof identityBinding.bindIdentity).toBe("function");
    expect(typeof identityBinding.unbindIdentity).toBe("function");
    expect(typeof identityBinding.mergeBindings).toBe("function");
    expect(typeof identityBinding.findConflicts).toBe("function");
  });
});

describe("ruleActionLog", () => {
  it("should export API functions", () => {
    expect(typeof ruleActionLog).toBe("object");
    expect(typeof ruleActionLog.recordDecision).toBe("function");
    expect(typeof ruleActionLog.fetchActionLogs).toBe("function");
    expect(typeof ruleActionLog.fetchActionLog).toBe("function");
    expect(typeof ruleActionLog.fetchActionLogsByPatient).toBe("function");
    expect(typeof ruleActionLog.fetchActionLogsByOrder).toBe("function");
  });
});

describe("terminology", () => {
  it("should export API functions", () => {
    expect(typeof terminology).toBe("object");
    expect(typeof terminology.fetchTerminologyMappings).toBe("function");
    expect(typeof terminology.fetchMappingSummary).toBe("function");
    expect(typeof terminology.fetchAiCandidates).toBe("function");
    expect(typeof terminology.adoptMapping).toBe("function");
    expect(typeof terminology.batchAdoptMappings).toBe("function");
    expect(typeof terminology.rejectAiCandidate).toBe("function");
    expect(typeof terminology.manualMapping).toBe("function");
  });
});

describe("aiCandidateReview", () => {
  it("should export API functions", () => {
    expect(typeof aiCandidateReview).toBe("object");
    expect(typeof aiCandidateReview.listCandidates).toBe("function");
    expect(typeof aiCandidateReview.getCandidate).toBe("function");
    expect(typeof aiCandidateReview.reviewCandidate).toBe("function");
    expect(typeof aiCandidateReview.batchReview).toBe("function");
    expect(typeof aiCandidateReview.getReviewSummary).toBe("function");
    expect(typeof aiCandidateReview.getReviewHistory).toBe("function");
  });
});

describe("workflow", () => {
  it("should export API functions", () => {
    expect(typeof workflow).toBe("object");
    expect(typeof workflow.fetchTodoTasks).toBe("function");
    expect(typeof workflow.fetchTodoDetail).toBe("function");
    expect(typeof workflow.fetchTodoSummary).toBe("function");
    expect(typeof workflow.createTodoTask).toBe("function");
    expect(typeof workflow.approveTask).toBe("function");
    expect(typeof workflow.rejectTask).toBe("function");
    expect(typeof workflow.delegateTask).toBe("function");
    expect(typeof workflow.cancelTask).toBe("function");
    expect(typeof workflow.addSignTask).toBe("function");
  });
});
