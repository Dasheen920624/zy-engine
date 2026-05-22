import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import EvalResultList from "../EvalResultList";

// Mock API
vi.mock("../../../api/eval", () => ({
  listEvalResults: vi.fn().mockResolvedValue([]),
  executeEvaluation: vi.fn(),
  listEvalSets: vi.fn().mockResolvedValue([]),
}));

// Mock CSS module
vi.mock("../evalResultList.module.css", () => ({
  default: {
    page: "page",
    scoreDetail: "scoreDetail",
    detailStatsRow: "detailStatsRow",
    innerCardSpacing: "innerCardSpacing",
    factItem: "factItem",
    iconSuccess: "iconSuccess",
    iconDanger: "iconDanger",
  },
}));

describe("EvalResultList", () => {
  it("应渲染评估结果页面", () => {
    render(<EvalResultList />);
  });

  it("应显示评估结果标题", () => {
    render(<EvalResultList />);
    expect(screen.getByText("评估结果")).toBeTruthy();
  });

  it("应显示执行评估按钮", () => {
    render(<EvalResultList />);
    expect(screen.getByText("执行评估")).toBeTruthy();
  });
});
