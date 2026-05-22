import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ConfigProvider } from "antd";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import RuleDetail from "../RuleDetail";
import {
  getRule,
  listRuleExecLogs,
  summarizeRuleExecLogs,
  type RuleDefinition,
  type RuleExecLog,
  type RuleExecLogSummary,
} from "../../../api/rule";

vi.mock("../../../api/rule", async () => {
  const actual = await vi.importActual<typeof import("../../../api/rule")>("../../../api/rule");
  return {
    ...actual,
    getRule: vi.fn(),
    listRuleExecLogs: vi.fn(),
    summarizeRuleExecLogs: vi.fn(),
  };
});

const SAMPLE_RULE: RuleDefinition = {
  rule_code: "R_AMI_STEMI_CANDIDATE",
  rule_name: "AMI/STEMI 候选入径规则",
  rule_type: "PATHWAY_NODE",
  version_no: "1.0.0",
  severity: "HIGH",
  status: "PUBLISHED",
  enabled: true,
  reference_document_code: "GUIDE_AMI_2025",
  reference_citation_id: "AMI_2025_S3_1",
  hospital_code: "HOSPITAL_DEMO",
  published_by: "zhao01",
  published_time: "2026-05-20T10:00:00+08:00",
  rule_json: {
    rule_code: "R_AMI_STEMI_CANDIDATE",
    rule_name: "AMI/STEMI 候选入径规则",
    rule_type: "PATHWAY_NODE",
    version: "1.0.0",
    trigger: { events: ["chief_complaint_recorded"] },
    condition: { field: "chief_complaints.code", in: ["CHEST_PAIN"] },
    result: { hit: { severity: "HIGH" } },
  },
};

const SAMPLE_LOGS: RuleExecLog[] = [
  {
    log_id: "L1",
    trace_id: "T-001",
    rule_code: "R_AMI_STEMI_CANDIDATE",
    hit: true,
    message: "疑似 STEMI",
    elapsed_ms: 23,
    result_status: "SUCCESS",
    created_time: "2026-05-21T08:00:00+08:00",
  },
];

const SAMPLE_SUMMARY: RuleExecLogSummary = {
  total: 12,
  hit_count: 3,
  miss_count: 9,
  error_count: 0,
  avg_elapsed_ms: 42,
};

function renderPage(code = "R_AMI_STEMI_CANDIDATE") {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <MemoryRouter initialEntries={[`/rule/definitions/${code}`]}>
      <ConfigProvider>
        <QueryClientProvider client={queryClient}>
          <Routes>
            <Route path="/rule/definitions/:code" element={<RuleDetail />} />
          </Routes>
        </QueryClientProvider>
      </ConfigProvider>
    </MemoryRouter>,
  );
}

describe("RuleDetail page", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getRule).mockResolvedValue(SAMPLE_RULE);
    vi.mocked(listRuleExecLogs).mockResolvedValue(SAMPLE_LOGS);
    vi.mocked(summarizeRuleExecLogs).mockResolvedValue(SAMPLE_SUMMARY);
  });

  it("renders rule name and meta", async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText("AMI/STEMI 候选入径规则")).toBeInTheDocument();
    });
    expect(screen.getByText("R_AMI_STEMI_CANDIDATE")).toBeInTheDocument();
    expect(screen.getByText("路径节点")).toBeInTheDocument();
  });

  it("renders source citation (ADR-0004)", async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/GUIDE_AMI_2025/)).toBeInTheDocument();
    });
  });

  it("renders exec log timeline", async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText("疑似 STEMI")).toBeInTheDocument();
      expect(screen.getAllByText("命中").length).toBeGreaterThan(0);
    });
  });

  it("renders DSL JSON snapshot", async () => {
    renderPage();
    await waitFor(() => {
      const dsl = screen.getByLabelText("dsl-readonly");
      expect(dsl.textContent).toContain("R_AMI_STEMI_CANDIDATE");
      expect(dsl.textContent).toContain("chief_complaints.code");
    });
  });

  it("falls back to 404 when rule is missing", async () => {
    vi.mocked(getRule).mockRejectedValue(new Error("not found"));
    renderPage("WHATEVER");
    await waitFor(() => {
      expect(screen.getByText("规则未找到")).toBeInTheDocument();
    });
  });
});
