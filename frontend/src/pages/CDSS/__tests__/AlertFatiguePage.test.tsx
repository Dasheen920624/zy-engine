import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import AlertFatiguePage from "../AlertFatiguePage";

// Mock CSS module
vi.mock("../alertFatiguePage.module.css", () => ({
  default: {
    page: "page",
    cardSpacing: "cardSpacing",
    rowSpacing: "rowSpacing",
    rowTopSpacing: "rowTopSpacing",
    innerCardSpacing: "innerCardSpacing",
    fullWidth: "fullWidth",
  },
}));

// Mock API
vi.mock("../../../api/cdss", () => ({
  listFatigueConfigs: vi.fn().mockResolvedValue([]),
  createFatigueConfig: vi.fn(),
  updateFatigueConfig: vi.fn(),
  getOverrideAnalysis: vi.fn().mockResolvedValue({
    total_alerts: 0,
    total_overrides: 0,
    total_acknowledges: 0,
    total_escalations: 0,
    override_rate: 0,
    high_override_rules: [],
    override_by_rule: {},
    override_by_trigger: {},
    override_by_operator: {},
  }),
}));

describe("AlertFatiguePage", () => {
  it("应渲染覆盖模式分析卡片", () => {
    render(<AlertFatiguePage />);
    expect(screen.getByText("覆盖模式分析")).toBeTruthy();
  });

  it("应渲染疲劳治理配置卡片", () => {
    render(<AlertFatiguePage />);
    expect(screen.getByText("疲劳治理配置")).toBeTruthy();
  });

  it("应显示新建配置按钮", () => {
    render(<AlertFatiguePage />);
    expect(screen.getByText("新建配置")).toBeTruthy();
  });

  it("应显示刷新按钮", () => {
    render(<AlertFatiguePage />);
    expect(screen.getByText("刷新")).toBeTruthy();
  });
});
