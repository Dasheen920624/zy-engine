import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import GraphExplore from "../GraphExplore";

// Mock API
vi.mock("../../../api/graph", () => ({
  getGraphSchema: vi.fn().mockResolvedValue({ nodes: [], edges: [] }),
  queryGraph: vi.fn().mockResolvedValue({ nodes: [], edges: [] }),
}));

// Mock CSS module
vi.mock("../GraphExplore.module.css", () => ({
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

describe("GraphExplore", () => {
  it("应渲染知识图谱页面", () => {
    render(wrapper(<GraphExplore />));
    // 页面应包含标题或关键元素
    const heading = screen.queryByRole("heading");
    expect(heading || document.querySelector(".mk-page-header")).toBeTruthy();
  });
});
