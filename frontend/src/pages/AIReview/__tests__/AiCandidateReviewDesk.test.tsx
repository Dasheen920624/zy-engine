import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import AiCandidateReviewDesk from "../AiCandidateReviewDesk";

// Mock CSS module
vi.mock("../aiCandidateReviewDesk.module.css", () => ({
  default: {
    page: "page",
    headerRow: "headerRow",
    titleZeroMargin: "titleZeroMargin",
    statsRow: "statsRow",
    filterCard: "filterCard",
    filterRow: "filterRow",
    selectMedium: "selectMedium",
    selectSmall: "selectSmall",
    formField: "formField",
  },
}));

// Mock API
vi.mock("../../../api/aiCandidateReview", () => ({
  listCandidates: vi.fn().mockRejectedValue(new Error("mock")),
  reviewCandidate: vi.fn(),
  batchReview: vi.fn(),
  getReviewSummary: vi.fn().mockRejectedValue(new Error("mock")),
  getReviewHistory: vi.fn().mockRejectedValue(new Error("mock")),
}));

describe("AiCandidateReviewDesk", () => {
  it("应渲染 AI 候选配置审核台标题", () => {
    render(<AiCandidateReviewDesk />);
    expect(screen.getByText("AI 候选配置审核台")).toBeTruthy();
  });

  it("应显示审核统计卡片", () => {
    render(<AiCandidateReviewDesk />);
    expect(screen.getByText("待审核")).toBeTruthy();
    expect(screen.getByText("已通过")).toBeTruthy();
    expect(screen.getByText("已驳回")).toBeTruthy();
    expect(screen.getByText("已修改")).toBeTruthy();
  });

  it("应显示刷新按钮", () => {
    render(<AiCandidateReviewDesk />);
    expect(screen.getByText("刷新")).toBeTruthy();
  });

  it("应显示审核历史按钮", () => {
    render(<AiCandidateReviewDesk />);
    expect(screen.getByText("审核历史")).toBeTruthy();
  });

  it("应显示筛选器", () => {
    render(<AiCandidateReviewDesk />);
    expect(screen.getByPlaceholderText("候选类型")).toBeTruthy();
    expect(screen.getByPlaceholderText("审核状态")).toBeTruthy();
  });
});
