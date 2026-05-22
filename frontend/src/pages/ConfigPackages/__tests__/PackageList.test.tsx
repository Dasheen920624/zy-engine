import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router-dom";
import PackageList from "../PackageList";
import type { FilterState } from "../index";

// Mock CSS module
vi.mock("../PackageList.module.css", () => ({
  default: {
    filterCard: "filterCard",
    filterBar: "filterBar",
    filterField: "filterField",
    filterFieldWide: "filterFieldWide",
    filterLabel: "filterLabel",
    filterSelect: "filterSelect",
    monoText: "monoText",
    monoCode: "monoCode",
    textSmall: "textSmall",
    environmentText: "environmentText",
    environmentProd: "environmentProd",
    environmentTest: "environmentTest",
    marginBottom12: "marginBottom12",
    packageRow: "packageRow",
    packageRowSelected: "packageRowSelected",
    iconMuted: "iconMuted",
  },
}));

// Mock API
vi.mock("@/api/configPackage", () => ({
  listPackages: vi.fn().mockResolvedValue([]),
  exportPackage: vi.fn().mockResolvedValue({}),
}));

// Mock StatusBadge
vi.mock("@/components/StatusBadge", () => ({
  StatusBadge: ({ status }: { status: string }) => <span data-testid="status-badge">{status}</span>,
}));

// Mock OrgContextSelector
vi.mock("../../../components", () => ({
  OrgContextSelector: () => <div data-testid="org-context-selector" />,
}));

const defaultFilters: FilterState = {};
const defaultOnFilterChange = vi.fn();
const defaultOnFilterReset = vi.fn();

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

describe("PackageList", () => {
  it("应渲染筛选工具栏", () => {
    renderWithProviders(
      <PackageList
        filters={defaultFilters}
        onFilterChange={defaultOnFilterChange}
        onFilterReset={defaultOnFilterReset}
        selectedPkg={null}
        onSelectPkg={vi.fn()}
      />
    );
    expect(screen.getByText("配置内容")).toBeTruthy();
    expect(screen.getByText("状态")).toBeTruthy();
    expect(screen.getByText("组织范围")).toBeTruthy();
  });

  it("应显示重置和刷新按钮", () => {
    renderWithProviders(
      <PackageList
        filters={defaultFilters}
        onFilterChange={defaultOnFilterChange}
        onFilterReset={defaultOnFilterReset}
        selectedPkg={null}
        onSelectPkg={vi.fn()}
      />
    );
    expect(screen.getByText("重置")).toBeTruthy();
    expect(screen.getByText("刷新")).toBeTruthy();
  });

  it("应显示导入配置按钮", () => {
    renderWithProviders(
      <PackageList
        filters={defaultFilters}
        onFilterChange={defaultOnFilterChange}
        onFilterReset={defaultOnFilterReset}
        selectedPkg={null}
        onSelectPkg={vi.fn()}
      />
    );
    expect(screen.getByText("导入配置")).toBeTruthy();
  });

  it("应显示配置包列表卡片", () => {
    renderWithProviders(
      <PackageList
        filters={defaultFilters}
        onFilterChange={defaultOnFilterChange}
        onFilterReset={defaultOnFilterReset}
        selectedPkg={null}
        onSelectPkg={vi.fn()}
      />
    );
    expect(screen.getByText(/待处理配置包/)).toBeTruthy();
  });
});
