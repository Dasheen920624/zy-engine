import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import InsuranceAudit from "../InsuranceAudit";

// Mock API
vi.mock("../../../api/auditLog", () => ({
  listAuditLogs: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, page_size: 20, total_pages: 0 }),
}));

// Mock CSS module
vi.mock("../InsuranceAudit.module.css", () => ({
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

describe("InsuranceAudit", () => {
  it("应渲染医保审核页面", () => {
    render(wrapper(<InsuranceAudit />));
    const heading = screen.queryByRole("heading");
    expect(heading || document.querySelector(".mk-page-header")).toBeTruthy();
  });
});
