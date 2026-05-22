import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import ImplementationGuidePage from "../ImplementationGuidePage";

// Mock CSS module
vi.mock("../ImplementationGuidePage.module.css", () => ({
  default: {
    mainContainer: "mainContainer",
    section: "section",
    sectionWide: "sectionWide",
    brandLabel: "brandLabel",
    mainTitle: "mainTitle",
    mainDescription: "mainDescription",
    stepsContainer: "stepsContainer",
    progressContainer: "progressContainer",
    contentArea: "contentArea",
    footer: "footer",
    marginBottom16: "marginBottom16",
    iconSuccess: "iconSuccess",
    iconMuted: "iconMuted",
    formInline: "formInline",
    formItemNoMargin: "formItemNoMargin",
    selectSmall: "selectSmall",
    fullWidth: "fullWidth",
    textSmall: "textSmall",
    marginTop16: "marginTop16",
    textRight: "textRight",
    marginRight12: "marginRight12",
    textCenter: "textCenter",
    textCenterWithMargin: "textCenterWithMargin",
    pageContainer: "pageContainer",
    resultList: "resultList",
    permCard: "permCard",
    innerCardSpacing: "innerCardSpacing",
  },
}));

// Mock API client
vi.mock("../../../api/client", () => ({
  get: vi.fn().mockRejectedValue(new Error("mock")),
  post: vi.fn().mockRejectedValue(new Error("mock")),
}));

// Mock ConfigWizardModal
vi.mock("../ConfigWizardModal", () => ({
  default: () => <div data-testid="config-wizard-modal" />,
}));

describe("ImplementationGuidePage", () => {
  it("应渲染实施向导标题", () => {
    render(
      <BrowserRouter>
        <ImplementationGuidePage />
      </BrowserRouter>
    );
    expect(screen.getByText("实施向导")).toBeTruthy();
  });

  it("应显示步骤导航", () => {
    render(
      <BrowserRouter>
        <ImplementationGuidePage />
      </BrowserRouter>
    );
    expect(screen.getByText("环境检查")).toBeTruthy();
    expect(screen.getByText("组织配置")).toBeTruthy();
    expect(screen.getByText("规则导入")).toBeTruthy();
  });

  it("应显示保存进度按钮", () => {
    render(
      <BrowserRouter>
        <ImplementationGuidePage />
      </BrowserRouter>
    );
    expect(screen.getByText("保存进度")).toBeTruthy();
  });

  it("应显示下一步按钮", () => {
    render(
      <BrowserRouter>
        <ImplementationGuidePage />
      </BrowserRouter>
    );
    expect(screen.getByText("下一步")).toBeTruthy();
  });

  it("应显示环境检查内容（第一步）", () => {
    render(
      <BrowserRouter>
        <ImplementationGuidePage />
      </BrowserRouter>
    );
    expect(screen.getByText("开始检查")).toBeTruthy();
    expect(screen.getByText("数据库连接")).toBeTruthy();
  });
});
