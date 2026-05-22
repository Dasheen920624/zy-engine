import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import DepartmentDrillDown from "../DepartmentDrillDown";

// Mock API
vi.mock("../../../api/quality", () => ({
  fetchDepartmentDetail: vi.fn().mockRejectedValue(new Error("mock")),
}));

// Mock CSS module
vi.mock("../departmentDrillDown.module.css", () => ({
  default: {
    page: "page",
    breadcrumb: "breadcrumb",
    pageHeader: "pageHeader",
    pageTitle: "pageTitle",
    periodSelect: "periodSelect",
    kpiRow: "kpiRow",
    kpiDetail: "kpiDetail",
    variationCard: "variationCard",
    iconSuccess: "iconSuccess",
    iconWarning: "iconWarning",
    iconDanger: "iconDanger",
    iconPrimary: "iconPrimary",
    changeArrow: "changeArrow",
  },
}));

// Mock SourceInfo
vi.mock("../../../components", () => ({
  SourceInfo: () => <div data-testid="source-info" />,
}));

describe("DepartmentDrillDown", () => {
  it("应渲染科室质控钻取页面", () => {
    render(
      <BrowserRouter>
        <DepartmentDrillDown />
      </BrowserRouter>
    );
  });

  it("应显示质控钻取标题", async () => {
    render(
      <BrowserRouter>
        <DepartmentDrillDown />
      </BrowserRouter>
    );
    expect(await screen.findByText(/质控钻取/)).toBeTruthy();
  });

  it("应显示刷新按钮", () => {
    render(
      <BrowserRouter>
        <DepartmentDrillDown />
      </BrowserRouter>
    );
    expect(screen.getByText("刷新")).toBeTruthy();
  });

  it("应显示 KPI 卡片标题", async () => {
    render(
      <BrowserRouter>
        <DepartmentDrillDown />
      </BrowserRouter>
    );
    expect(await screen.findByText("路径执行")).toBeTruthy();
    expect(screen.getByText("规则命中")).toBeTruthy();
    expect(screen.getByText("质控问题")).toBeTruthy();
    expect(screen.getByText("医保风险")).toBeTruthy();
  });

  it("应显示变异 TOP10 标题", async () => {
    render(
      <BrowserRouter>
        <DepartmentDrillDown />
      </BrowserRouter>
    );
    expect(await screen.findByText("变异 TOP10")).toBeTruthy();
  });

  it("应显示医生绩效标题", async () => {
    render(
      <BrowserRouter>
        <DepartmentDrillDown />
      </BrowserRouter>
    );
    expect(await screen.findByText("医生绩效")).toBeTruthy();
  });
});
