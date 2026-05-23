import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import MappingWorkbench from "../MappingWorkbench";

// Mock API
vi.mock("../../../api/terminology", () => ({
  listMappings: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, page_size: 20, total_pages: 0 }),
  createMapping: vi.fn(),
  updateMapping: vi.fn(),
  deleteMapping: vi.fn(),
}));

// Mock CSS module
vi.mock("../MappingWorkbench.module.css", () => ({
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

describe("MappingWorkbench", () => {
  it("应渲染术语映射工作台页面", () => {
    render(<MappingWorkbench />, { wrapper });
    const heading = screen.queryByRole("heading");
    expect(heading || document.querySelector(".mk-page-header")).toBeTruthy();
  });
});
