import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import EvalReportPage from "../EvalReportPage";

// Mock API
vi.mock("../../../api/eval", () => ({
  listReports: vi.fn().mockResolvedValue([]),
  exportReport: vi.fn(),
  archiveReport: vi.fn(),
  submitReview: vi.fn(),
  listReviews: vi.fn().mockResolvedValue([]),
  createRectification: vi.fn(),
  autoCreateRectifications: vi.fn().mockResolvedValue([]),
  updateRectificationStatus: vi.fn(),
  listRectifications: vi.fn().mockResolvedValue([]),
  generateReport: vi.fn(),
  reEvaluate: vi.fn(),
  listEvalResults: vi.fn().mockResolvedValue([]),
}));

// Mock CSS module
vi.mock("../evalReportPage.module.css", () => ({
  default: {
    page: "page",
    filterSelect: "filterSelect",
    sectionCardSpacing: "sectionCardSpacing",
    fullWidth: "fullWidth",
    detailStatsRow: "detailStatsRow",
    detailDescriptions: "detailDescriptions",
    detailSectionCard: "detailSectionCard",
    recommendationList: "recommendationList",
    reviewComment: "reviewComment",
    reviewTime: "reviewTime",
    generateHint: "generateHint",
    scoreRed: "scoreRed",
    scoreOrange: "scoreOrange",
    scoreGreen: "scoreGreen",
  },
}));

describe("EvalReportPage", () => {
  it("应渲染评估报告页面", () => {
    render(<EvalReportPage />);
  });

  it("应显示评估报告标题", () => {
    render(<EvalReportPage />);
    expect(screen.getByText("评估报告")).toBeTruthy();
  });

  it("应显示整改任务标题", () => {
    render(<EvalReportPage />);
    expect(screen.getByText("整改任务")).toBeTruthy();
  });

  it("应显示生成报告按钮", () => {
    render(<EvalReportPage />);
    expect(screen.getByText("生成报告")).toBeTruthy();
  });
});
