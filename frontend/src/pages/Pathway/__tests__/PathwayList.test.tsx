import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import PathwayList from "../PathwayList";

// Mock API
vi.mock("../../../api/pathway", () => ({
  listPathways: vi.fn().mockResolvedValue({ items: [], total: 0 }),
}));

// Mock CSS module
vi.mock("../styles.module.css", () => ({
  default: {
    page: "page",
    pageHeader: "pageHeader",
    pageTitle: "pageTitle",
    pageSubtitle: "pageSubtitle",
    listToolbar: "listToolbar",
    searchInput: "searchInput",
    statusFilter: "statusFilter",
    clickableRow: "clickableRow",
    tableEmpty: "tableEmpty",
    emptyActionButton: "emptyActionButton",
  },
}));

// Mock OrgContextSelector
vi.mock("../../../components", () => ({
  StatusBadge: ({ status }: { status: string }) => <span>{status}</span>,
  OrgContextSelector: () => <div data-testid="org-context-selector" />,
}));

// Mock ActionMenu
vi.mock("../components/ActionMenu", () => ({
  default: () => <div data-testid="action-menu" />,
}));

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false } },
});

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider client={queryClient}>
    <BrowserRouter>
      {children}
    </BrowserRouter>
  </QueryClientProvider>
);

describe("PathwayList", () => {
  it("应渲染路径配置页面", () => {
    render(<PathwayList />, { wrapper });
  });

  it("应显示路径配置标题", () => {
    render(<PathwayList />, { wrapper });
    expect(screen.getByText("路径配置")).toBeTruthy();
  });

  it("应显示新建专病路径按钮", () => {
    render(<PathwayList />, { wrapper });
    expect(screen.getByText("新建专病路径")).toBeTruthy();
  });

  it("应显示搜索输入框", () => {
    render(<PathwayList />, { wrapper });
    expect(screen.getByPlaceholderText("搜索路径")).toBeTruthy();
  });

  it("应显示组织上下文选择器", () => {
    render(<PathwayList />, { wrapper });
    expect(screen.getByTestId("org-context-selector")).toBeTruthy();
  });
});
