import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import KnowledgePage from "../KnowledgePage";

// Mock API
vi.mock("../../../api/knowledge", () => ({
  listKnowledgeBases: vi.fn().mockResolvedValue([]),
  getKnowledgeBase: vi.fn(),
  createKnowledgeBase: vi.fn(),
  updateKnowledgeBase: vi.fn(),
  deleteKnowledgeBase: vi.fn(),
}));

// Mock CSS module
vi.mock("../KnowledgePage.module.css", () => ({
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

describe("KnowledgePage", () => {
  it("应渲染知识库页面", () => {
    render(<KnowledgePage />, { wrapper });
    const heading = screen.queryByRole("heading");
    expect(heading || document.querySelector(".mk-page-header")).toBeTruthy();
  });
});
