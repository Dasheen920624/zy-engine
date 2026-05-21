import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ConfigProvider } from "antd";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import PathwayDetail from "../PathwayDetail";
import {
  getPathway,
  nodeCompletionSummary,
  summarizePatientPathwayInstances,
  summarizeVariations,
  type InstanceSummary,
  type NodeCompletionSummary,
  type VariationSummary,
} from "../../../api/pathway";
import type { PathwayDetail as PathwayDetailType } from "../../../api/types";

vi.mock("../../../api/pathway", async () => {
  const actual =
    await vi.importActual<typeof import("../../../api/pathway")>("../../../api/pathway");
  return {
    ...actual,
    getPathway: vi.fn(),
    nodeCompletionSummary: vi.fn(),
    summarizePatientPathwayInstances: vi.fn(),
    summarizeVariations: vi.fn(),
    deletePathway: vi.fn(),
  };
});

vi.mock("@uiw/react-codemirror", () => ({
  default: ({ value }: { value: string }) => (
    <div data-testid="codemirror-mock">{value}</div>
  ),
}));

const PATHWAY_DETAIL: PathwayDetailType = {
  pathway_code: "PATH_AMI_STEMI",
  draft_status: "DRAFT",
  published_versions: ["1.0.0", "2.0.0"],
  active_published_version: "2.0.0",
  selected_version: "2.0.0",
  draft_config: { nodes: [{ code: "N1" }] },
  published_config: { nodes: [{ code: "N1" }, { code: "N2" }] },
  reference_sources: [{ document: "GUIDE_AMI_2025" }],
  reference_warnings: ["节点 N3 来源缺失"],
};

const INSTANCE_SUMMARY: InstanceSummary = {
  total: 23,
  active: 5,
  completed: 16,
  exited: 2,
  terminated: 0,
};

const NODE_COMPLETION: NodeCompletionSummary = {
  total: 5,
  by_node: [
    { node_code: "N1", completed: 23, total: 23, completion_rate: 1 },
    { node_code: "N2", completed: 18, total: 23, completion_rate: 0.78 },
  ],
};

const VARIATION_SUMMARY: VariationSummary = {
  total: 7,
  by_type: [
    { variation_type: "SKIP", count: 3 },
    { variation_type: "DEFER", count: 4 },
  ],
};

function renderPage(code = "PATH_AMI_STEMI") {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <MemoryRouter initialEntries={[`/pathway/templates/${code}`]}>
      <ConfigProvider>
        <QueryClientProvider client={queryClient}>
          <Routes>
            <Route path="/pathway/templates/:code" element={<PathwayDetail />} />
          </Routes>
        </QueryClientProvider>
      </ConfigProvider>
    </MemoryRouter>,
  );
}

describe("PathwayDetail", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getPathway).mockResolvedValue(PATHWAY_DETAIL);
    vi.mocked(summarizePatientPathwayInstances).mockResolvedValue(INSTANCE_SUMMARY);
    vi.mocked(nodeCompletionSummary).mockResolvedValue(NODE_COMPLETION);
    vi.mocked(summarizeVariations).mockResolvedValue(VARIATION_SUMMARY);
  });

  it("renders code, active version and reference warnings", async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText("PATH_AMI_STEMI")).toBeInTheDocument();
    });
    expect(screen.getByText("激活 v2.0.0")).toBeInTheDocument();
    expect(screen.getByText("节点 N3 来源缺失")).toBeInTheDocument();
  });

  it("renders instance statistics", async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText("23")).toBeInTheDocument();
      expect(screen.getByText("16")).toBeInTheDocument();
    });
  });

  it("renders variation statistics", async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText("累计变异")).toBeInTheDocument();
      expect(screen.getByText("7")).toBeInTheDocument();
    });
  });

  it("falls back to 404 result when pathway not found", async () => {
    vi.mocked(getPathway).mockRejectedValue(new Error("not found"));
    renderPage("MISSING_CODE");
    await waitFor(() => {
      expect(screen.getByText("路径未找到")).toBeInTheDocument();
    });
  });
});
