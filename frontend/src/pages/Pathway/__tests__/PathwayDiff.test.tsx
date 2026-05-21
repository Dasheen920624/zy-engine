import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ConfigProvider } from "antd";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import PathwayDiff from "../PathwayDiff";
import { diffPathway, getPathway, type PathwayDiffResult } from "../../../api/pathway";
import type { PathwayDetail as PathwayDetailType } from "../../../api/types";

vi.mock("../../../api/pathway", async () => {
  const actual =
    await vi.importActual<typeof import("../../../api/pathway")>("../../../api/pathway");
  return {
    ...actual,
    getPathway: vi.fn(),
    diffPathway: vi.fn(),
  };
});

vi.mock("@uiw/react-codemirror", () => ({
  default: ({ value }: { value: string }) => (
    <div data-testid="codemirror-mock">{value}</div>
  ),
}));

const PATHWAY_DETAIL: PathwayDetailType = {
  pathway_code: "PATH_AMI_STEMI",
  draft_status: "NONE",
  published_versions: ["1.0.0", "2.0.0", "3.0.0"],
  active_published_version: "3.0.0",
  selected_version: "3.0.0",
  draft_config: null,
  published_config: { nodes: [{ code: "N1" }, { code: "N2" }] },
  reference_sources: [],
  reference_warnings: [],
};

const DIFF: PathwayDiffResult = {
  pathway_code: "PATH_AMI_STEMI",
  from_version: "2.0.0",
  to_version: "3.0.0",
  nodes_added: [{ node_code: "N3", node_name: "PCI 决策" }],
  nodes_removed: [],
  nodes_modified: [{ node_code: "N1" }],
  edges_added: [],
  edges_removed: [],
  edges_modified: [],
  tasks_added: [{ task_code: "T_DAPT" }],
  tasks_removed: [],
  tasks_modified: [],
  summary: {
    nodes_added: 1,
    nodes_removed: 0,
    nodes_modified: 1,
    edges_added: 0,
    edges_removed: 0,
    edges_modified: 0,
    tasks_added: 1,
    tasks_removed: 0,
    tasks_modified: 0,
  },
};

function renderPage(code = "PATH_AMI_STEMI", from?: string, to?: string) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const search = new URLSearchParams();
  if (from) search.set("from", from);
  if (to) search.set("to", to);
  const qs = search.toString() ? `?${search.toString()}` : "";
  return render(
    <MemoryRouter initialEntries={[`/pathway/templates/${code}/diff${qs}`]}>
      <ConfigProvider>
        <QueryClientProvider client={queryClient}>
          <Routes>
            <Route path="/pathway/templates/:code/diff" element={<PathwayDiff />} />
          </Routes>
        </QueryClientProvider>
      </ConfigProvider>
    </MemoryRouter>,
  );
}

describe("PathwayDiff page", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getPathway).mockResolvedValue(PATHWAY_DETAIL);
    vi.mocked(diffPathway).mockResolvedValue(DIFF);
  });

  it("auto-picks from/to versions and runs diff", async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/版本对比/)).toBeInTheDocument();
    });
    await waitFor(() => {
      expect(vi.mocked(diffPathway)).toHaveBeenCalled();
    });
  });

  it("renders diff summary numbers", async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/节点变更/)).toBeInTheDocument();
    });
    expect(screen.getByText(/PCI 决策/)).toBeInTheDocument();
  });

  it("renders alert when published versions < 2", async () => {
    vi.mocked(getPathway).mockResolvedValue({
      ...PATHWAY_DETAIL,
      published_versions: ["1.0.0"],
      active_published_version: "1.0.0",
    });
    renderPage("ONLY_ONE");
    await waitFor(() => {
      expect(screen.getByText(/已发布版本不足 2 个/)).toBeInTheDocument();
    });
  });
});
