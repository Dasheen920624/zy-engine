import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import Dashboard from "./Dashboard";

// Mock CSS module
vi.mock("./Dashboard.module.css", () => ({
  default: {
    pageTitle: "pageTitle",
    pageDescription: "pageDescription",
    metricsRow: "metricsRow",
    statSuffix: "statSuffix",
  },
}));

describe("Dashboard", () => {
  it("应渲染工作台标题", () => {
    render(
      <BrowserRouter>
        <Dashboard />
      </BrowserRouter>
    );
    expect(screen.getByText("工作台")).toBeTruthy();
  });

  it("应显示核心指标", () => {
    render(
      <BrowserRouter>
        <Dashboard />
      </BrowserRouter>
    );
    expect(screen.getByText("已落地后端模块")).toBeTruthy();
    expect(screen.getByText("已落地前端页面")).toBeTruthy();
  });

  it("应显示能力卡片", () => {
    render(
      <BrowserRouter>
        <Dashboard />
      </BrowserRouter>
    );
    expect(screen.getByText("试点准备")).toBeTruthy();
  });
});
