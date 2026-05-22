import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import TrendChart from "../TrendChart";

// Mock CSS module
vi.mock("../trendChart.module.css", () => ({
  default: {
    emptyState: "emptyState",
    legend: "legend",
    legendLine: "legendLine",
    legendLineSuccess: "legendLineSuccess",
    legendLineWarning: "legendLineWarning",
    legendLinePrimary: "legendLinePrimary",
    legendText: "legendText",
  },
}));

describe("TrendChart", () => {
  it("应在无数据时显示暂无数据", () => {
    render(<TrendChart data={[]} />);
    expect(screen.getByText("暂无数据")).toBeTruthy();
  });

  it("应渲染 SVG 趋势图", () => {
    const data = [
      { date: "2026-05-01", pathwayCompletionRate: 80, ruleHitRate: 20, qcRectificationRate: 90, insuranceRiskAmount: 0 },
      { date: "2026-05-02", pathwayCompletionRate: 85, ruleHitRate: 22, qcRectificationRate: 88, insuranceRiskAmount: 0 },
      { date: "2026-05-03", pathwayCompletionRate: 82, ruleHitRate: 18, qcRectificationRate: 92, insuranceRiskAmount: 0 },
    ];
    const { container } = render(<TrendChart data={data} />);
    expect(container.querySelector("svg")).toBeTruthy();
  });

  it("应显示图例标签", () => {
    const data = [
      { date: "2026-05-01", pathwayCompletionRate: 80, ruleHitRate: 20, qcRectificationRate: 90, insuranceRiskAmount: 0 },
    ];
    render(<TrendChart data={data} />);
    expect(screen.getByText("路径完成率")).toBeTruthy();
    expect(screen.getByText("规则命中率")).toBeTruthy();
    expect(screen.getByText("质控整改率")).toBeTruthy();
  });
});
