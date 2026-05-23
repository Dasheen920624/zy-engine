import { describe, it, expect, vi, beforeEach } from "vitest";
import { mockGet, mockPost, mockDel, resetMocks } from "./testUtils";

vi.mock("../client", () => ({
  http: {
    get: () => vi.fn(),
    post: () => vi.fn(),
    put: () => vi.fn(),
    delete: () => vi.fn(),
    patch: () => vi.fn(),
  },
  get: (...args: unknown[]) => mockGet(...args),
  post: (...args: unknown[]) => mockPost(...args),
  put: () => vi.fn(),
  del: (...args: unknown[]) => mockDel(...args),
}));

import * as rule from "../rule";

beforeEach(resetMocks);

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
