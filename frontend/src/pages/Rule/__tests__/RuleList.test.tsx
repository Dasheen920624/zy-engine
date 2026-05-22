import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ConfigProvider } from "antd";
import { MemoryRouter } from "react-router-dom";
import RuleList from "../RuleList";
import { listRules, type RuleDefinition } from "../../../api/rule";

vi.mock("../../../api/rule", async () => {
  const actual = await vi.importActual<typeof import("../../../api/rule")>("../../../api/rule");
  return {
    ...actual,
    listRules: vi.fn(),
  };
});

const SAMPLE: RuleDefinition[] = [
  {
    rule_code: "R_AMI_STEMI_CANDIDATE",
    rule_name: "AMI/STEMI 候选入径规则",
    rule_type: "PATHWAY_NODE",
    version_no: "1.0.0",
    severity: "HIGH",
    status: "PUBLISHED",
    enabled: true,
    rule_json: {},
    reference_document_code: "GUIDE_AMI_2025",
    published_time: "2026-05-20T10:00:00+08:00",
    hospital_code: "HOSPITAL_DEMO",
  },
  {
    rule_code: "R_EMR_ADMISSION_TIMELY",
    rule_name: "入院记录时限",
    rule_type: "TIME_LIMIT_QC",
    version_no: "1.0.0",
    severity: "MEDIUM",
    status: "DRAFT",
    enabled: true,
    rule_json: {},
  },
];

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <MemoryRouter initialEntries={["/rule/definitions"]}>
      <ConfigProvider>
        <QueryClientProvider client={queryClient}>
          <RuleList />
        </QueryClientProvider>
      </ConfigProvider>
    </MemoryRouter>,
  );
}

describe("RuleList page", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listRules).mockResolvedValue(SAMPLE);
  });

  it("renders rule table with code and name", async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText("AMI/STEMI 候选入径规则")).toBeInTheDocument();
      expect(screen.getByText("入院记录时限")).toBeInTheDocument();
    });
    expect(screen.getByText("R_AMI_STEMI_CANDIDATE")).toBeInTheDocument();
  });

  it("renders type and severity tags", async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText("路径节点")).toBeInTheDocument();
      expect(screen.getByText("时限质控")).toBeInTheDocument();
    });
  });

  it("filters rules by local search", async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText("AMI/STEMI 候选入径规则")).toBeInTheDocument();
    });
    const input = screen.getByLabelText("rule-search") as HTMLInputElement;
    fireEvent.change(input, { target: { value: "EMR" } });
    fireEvent.blur(input);
    await waitFor(() => {
      expect(screen.getByText("入院记录时限")).toBeInTheDocument();
      expect(screen.queryByText("AMI/STEMI 候选入径规则")).not.toBeInTheDocument();
    });
  });

  it("shows empty hint when no rules", async () => {
    vi.mocked(listRules).mockResolvedValue([]);
    renderPage();
    await waitFor(() => {
      expect(screen.getByText("暂无规则")).toBeInTheDocument();
    });
  });
});
