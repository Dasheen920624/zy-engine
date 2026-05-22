import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router-dom";
import AlertList from "../AlertList";

// Mock CSS module
vi.mock("../alertList.module.css", () => ({
  default: {
    page: "page",
    pageTitle: "pageTitle",
    filterBar: "filterBar",
    deptInput: "deptInput",
    selectNarrow: "selectNarrow",
    summaryBar: "summaryBar",
    severityDotCritical: "severityDotCritical",
    severityDotWarning: "severityDotWarning",
    severityDotInfo: "severityDotInfo",
  },
}));

// Mock API
vi.mock("../../../api/quality", () => ({
  listAlerts: vi.fn().mockResolvedValue({ items: [], total: 0 }),
  getAlertSummary: vi.fn().mockResolvedValue({
    critical: 0,
    warning: 0,
    info: 0,
    overtime: 0,
  }),
}));

// Mock components
vi.mock("../../../components", () => ({
  StatusBadge: ({ status }: { status: string }) => <span data-testid="status-badge">{status}</span>,
  OrgContextSelector: () => <div data-testid="org-context-selector" />,
  SourceInfo: () => <div data-testid="source-info" />,
}));

vi.mock("../components/AssignDialog", () => ({
  default: () => <div data-testid="assign-dialog" />,
}));

function renderWithProviders(ui: React.ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <BrowserRouter>
      <QueryClientProvider client={queryClient}>
        {ui}
      </QueryClientProvider>
    </BrowserRouter>
  );
}

describe("AlertList", () => {
  it("应渲染质控预警标题", () => {
    renderWithProviders(<AlertList />);
    expect(screen.getByText("质控预警")).toBeTruthy();
  });

  it("应显示实时模式开关", () => {
    renderWithProviders(<AlertList />);
    expect(screen.getByText("实时模式")).toBeTruthy();
  });

  it("应显示统计卡片", () => {
    renderWithProviders(<AlertList />);
    expect(screen.getByText("危急")).toBeTruthy();
    expect(screen.getByText("警告")).toBeTruthy();
    expect(screen.getByText("提醒")).toBeTruthy();
    expect(screen.getByText("超时未改")).toBeTruthy();
  });

  it("应显示科室输入框", () => {
    renderWithProviders(<AlertList />);
    expect(screen.getByPlaceholderText("科室")).toBeTruthy();
  });
});
