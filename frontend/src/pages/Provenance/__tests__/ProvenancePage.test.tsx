import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import ProvenancePage from "../ProvenancePage";

// Mock API
vi.mock("../../../api/provenance", () => ({
  listSourceDocuments: vi.fn().mockResolvedValue([]),
  listCitations: vi.fn().mockResolvedValue([]),
  listBindings: vi.fn().mockResolvedValue([]),
  getSourceDocument: vi.fn(),
  getCitation: vi.fn(),
  getBinding: vi.fn(),
}));

// Mock store
vi.mock("../../../store/orgContext", () => ({
  getOrgContext: vi.fn().mockReturnValue({ tenant_id: "TENANT_DEMO" }),
  setOrgContext: vi.fn(),
  subscribeOrgContext: vi.fn().mockReturnValue(() => {}),
}));

// Mock CSS module
vi.mock("../ProvenancePage.module.css", () => ({
  default: {},
}));

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false } },
});

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider client={queryClient}>
    {children}
  </QueryClientProvider>
);

describe("ProvenancePage", () => {
  it("应渲染来源追溯标题", () => {
    render(<ProvenancePage />, { wrapper });
    expect(screen.getByText("来源追溯")).toBeTruthy();
  });

  it("应显示三个卡片区域", () => {
    render(<ProvenancePage />, { wrapper });
    expect(screen.getByText("来源文档库")).toBeTruthy();
    expect(screen.getByText("来源文档")).toBeTruthy();
    expect(screen.getByText("引用片段")).toBeTruthy();
    expect(screen.getByText("资产绑定")).toBeTruthy();
  });

  it("应显示刷新按钮", () => {
    render(<ProvenancePage />, { wrapper });
    const refreshButtons = screen.getAllByText("刷新");
    expect(refreshButtons.length).toBeGreaterThanOrEqual(2);
  });
});
