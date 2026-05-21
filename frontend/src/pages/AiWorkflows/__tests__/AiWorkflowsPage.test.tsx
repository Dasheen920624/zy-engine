import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ConfigProvider } from "antd";
import { MemoryRouter } from "react-router-dom";
import AiWorkflowsPage from "../AiWorkflowsPage";
import {
  listDegradationChains,
  listProviders,
  listWorkflowTemplates,
  workflowInvocationStats,
} from "../../../api/aiWorkflows";

vi.mock("../../../api/aiWorkflows", async () => {
  const actual =
    await vi.importActual<typeof import("../../../api/aiWorkflows")>(
      "../../../api/aiWorkflows",
    );
  return {
    ...actual,
    listProviders: vi.fn(),
    listDegradationChains: vi.fn(),
    listWorkflowTemplates: vi.fn(),
    workflowInvocationStats: vi.fn(),
  };
});

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <MemoryRouter initialEntries={["/ai-workflows"]}>
      <ConfigProvider>
        <QueryClientProvider client={queryClient}>
          <AiWorkflowsPage />
        </QueryClientProvider>
      </ConfigProvider>
    </MemoryRouter>,
  );
}

describe("AiWorkflowsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listProviders).mockResolvedValue([
      { provider_type: "QIANWEN", ready: true, status: "READY" },
      { provider_type: "LOCAL", ready: true, status: "READY" },
    ]);
    vi.mocked(listDegradationChains).mockResolvedValue({
      RESEARCH: {
        call_type: "RESEARCH",
        chain: "QIANWEN,LOCAL",
        providers: [],
      },
    });
    vi.mocked(listWorkflowTemplates).mockResolvedValue([]);
    vi.mocked(workflowInvocationStats).mockResolvedValue({
      total: 0,
      success: 0,
      failed: 0,
      degraded: 0,
    });
  });

  it("renders page title and hero banner with 8 国产大模型 narrative", async () => {
    renderPage();
    expect(screen.getByText("AI 工作流引擎")).toBeInTheDocument();
    expect(screen.getByText(/8 家国产大模型直连/)).toBeInTheDocument();
    expect(screen.getByText(/Ollama 本地兜底/)).toBeInTheDocument();
    expect(screen.getByText(/LOCAL 规则兜底/)).toBeInTheDocument();
    expect(screen.getByText(/Dify 仅 WORKFLOW/)).toBeInTheDocument();
  });

  it("renders 3 tabs", () => {
    renderPage();
    expect(screen.getByRole("tab", { name: /Provider 状态/ })).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: /降级链/ })).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: /多步工作流/ })).toBeInTheDocument();
  });

  it("renders provider cards in default Provider tab", async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText("通义千问")).toBeInTheDocument();
    });
  });

  it("shows refresh button", () => {
    renderPage();
    expect(screen.getByText("刷新全部")).toBeInTheDocument();
  });
});
