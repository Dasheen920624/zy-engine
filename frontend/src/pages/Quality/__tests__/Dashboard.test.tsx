import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import QualityDashboard from "../Dashboard";

// Mock CSS module
vi.mock("../Dashboard.module.css", () => ({
  default: {
    page: "page",
    pageHeader: "pageHeader",
    pageTitle: "pageTitle",
    periodSelect: "periodSelect",
    kpiRow: "kpiRow",
    kpiContent: "kpiContent",
    chartCard: "chartCard",
    starRating: "starRating",
    changeArrow: "changeArrow",
    iconSuccess: "iconSuccess",
    iconWarning: "iconWarning",
    iconDanger: "iconDanger",
    iconPrimary: "iconPrimary",
  },
}));

// Mock API
vi.mock("../../../api/quality", () => ({
  fetchDashboardKpis: vi.fn().mockRejectedValue(new Error("mock")),
  fetchDepartmentRanking: vi.fn().mockRejectedValue(new Error("mock")),
  fetchTrendData: vi.fn().mockRejectedValue(new Error("mock")),
}));

// Mock TrendChart
vi.mock("../components/TrendChart", () => ({
  default: () => <div data-testid="trend-chart" />,
}));

// Mock SourceInfo
vi.mock("../../../components", () => ({
  SourceInfo: () => <div data-testid="source-info" />,
}));

describe("QualityDashboard", () => {
  it("应渲染院级质控驾驶舱标题", () => {
    render(
      <BrowserRouter>
        <QualityDashboard />
      </BrowserRouter>
    );
    expect(screen.getByText("院级质控驾驶舱")).toBeTruthy();
  });

  it("应显示 KPI 卡片标题", () => {
    render(
      <BrowserRouter>
        <QualityDashboard />
      </BrowserRouter>
    );
    expect(screen.getByText("路径执行")).toBeTruthy();
    expect(screen.getByText("规则命中")).toBeTruthy();
    expect(screen.getByText("质控问题")).toBeTruthy();
    expect(screen.getByText("医保风险")).toBeTruthy();
  });

  it("应显示刷新和导出按钮", () => {
    render(
      <BrowserRouter>
        <QualityDashboard />
      </BrowserRouter>
    );
    expect(screen.getByText("刷新")).toBeTruthy();
    expect(screen.getByText("导出周报")).toBeTruthy();
  });

  it("应显示科室排名表格", () => {
    render(
      <BrowserRouter>
        <QualityDashboard />
      </BrowserRouter>
    );
    expect(screen.getByText("科室排名（点击钻取）")).toBeTruthy();
  });

  it("应显示趋势图卡片", () => {
    render(
      <BrowserRouter>
        <QualityDashboard />
      </BrowserRouter>
    );
    expect(screen.getByText(/路径完成率趋势/)).toBeTruthy();
  });
});
