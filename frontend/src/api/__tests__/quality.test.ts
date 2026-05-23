import { describe, it, expect, vi, beforeEach } from "vitest";
import { mockGet, mockPost, resetMocks } from "./testUtils";

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
  del: () => vi.fn(),
}));

import * as quality from "../quality";

beforeEach(resetMocks);

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
