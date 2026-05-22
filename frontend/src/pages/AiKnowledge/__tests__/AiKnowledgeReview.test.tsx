import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import AiKnowledgeReview from "../AiKnowledgeReview";

// Mock CSS module
vi.mock("../AiKnowledgeReview.module.css", () => ({
  default: {
    page: "page",
    pageHeader: "pageHeader",
    eyebrow: "eyebrow",
    card: "card",
    loading: "loading",
    codeText: "codeText",
    detailContent: "detailContent",
    contentBlock: "contentBlock",
    reviewForm: "reviewForm",
    formItem: "formItem",
    reviewNoteInput: "reviewNoteInput",
  },
}));

// Mock API
vi.mock("../../../api/aiCandidateReview", () => ({
  listCandidates: vi.fn().mockResolvedValue({ items: [] }),
  getCandidate: vi.fn(),
  reviewCandidate: vi.fn(),
  batchReview: vi.fn(),
  getReviewSummary: vi.fn().mockResolvedValue({
    total: 0,
    pending: 0,
    approved: 0,
    rejected: 0,
    modified: 0,
  }),
  getReviewHistory: vi.fn().mockResolvedValue({ items: [] }),
}));

// Mock orgContext store
vi.mock("../../../store/orgContext", () => ({
  getOrgContext: vi.fn().mockReturnValue({ tenant_id: "TENANT_DEMO" }),
}));

function renderWithQueryClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      {ui}
    </QueryClientProvider>
  );
}

describe("AiKnowledgeReview", () => {
  it("应渲染知识审核台标题", () => {
    renderWithQueryClient(<AiKnowledgeReview />);
    expect(screen.getByText("知识审核台")).toBeTruthy();
  });

  it("应显示审核统计卡片", () => {
    renderWithQueryClient(<AiKnowledgeReview />);
    expect(screen.getByText("审核统计")).toBeTruthy();
  });

  it("应显示待审核候选卡片", () => {
    renderWithQueryClient(<AiKnowledgeReview />);
    expect(screen.getByText("待审核候选")).toBeTruthy();
  });

  it("应显示审核历史卡片", () => {
    renderWithQueryClient(<AiKnowledgeReview />);
    expect(screen.getByText("审核历史")).toBeTruthy();
  });

  it("应显示刷新按钮", () => {
    renderWithQueryClient(<AiKnowledgeReview />);
    const refreshButtons = screen.getAllByText("刷新");
    expect(refreshButtons.length).toBeGreaterThanOrEqual(1);
  });
});
