import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import OnboardingWizard from "../OnboardingWizard";

// Mock API
vi.mock("../../../../api/tenantOnboarding", () => ({
  submitTenantApplication: vi.fn().mockResolvedValue({ applicationCode: "APP-001" }),
  approveTenantApplication: vi.fn().mockResolvedValue({ tenantId: "T-001", applicationCode: "APP-001" }),
  sendTenantAdminInvitation: vi.fn().mockResolvedValue({ invitationId: "INV-001" }),
}));

// Mock OnboardingSuccess
vi.mock("../OnboardingSuccess", () => ({
  default: () => <div data-testid="onboarding-success" />,
}));

// Mock step components
vi.mock("../steps/Step1Info", () => ({
  default: () => <div data-testid="step1-info" />,
}));
vi.mock("../steps/Step2Subscription", () => ({
  default: () => <div data-testid="step2-subscription" />,
}));
vi.mock("../steps/Step3InitData", () => ({
  default: () => <div data-testid="step3-initdata" />,
}));

// Mock CSS module
vi.mock("../styles.module.css", () => ({
  default: {
    page: "page",
    header: "header",
    eyebrow: "eyebrow",
    wizardSurface: "wizardSurface",
    steps: "steps",
    stepBody: "stepBody",
    actions: "actions",
  },
}));

describe("OnboardingWizard", () => {
  it("应渲染租户开通向导页面", () => {
    render(
      <BrowserRouter>
        <OnboardingWizard />
      </BrowserRouter>
    );
  });

  it("应显示租户开通向导标题", () => {
    render(
      <BrowserRouter>
        <OnboardingWizard />
      </BrowserRouter>
    );
    expect(screen.getByText("租户开通向导")).toBeTruthy();
  });

  it("应显示步骤信息", () => {
    render(
      <BrowserRouter>
        <OnboardingWizard />
      </BrowserRouter>
    );
    expect(screen.getByText("租户信息")).toBeTruthy();
    expect(screen.getByText("套餐选择")).toBeTruthy();
    expect(screen.getByText("初始化")).toBeTruthy();
  });

  it("应显示下一步按钮", () => {
    render(
      <BrowserRouter>
        <OnboardingWizard />
      </BrowserRouter>
    );
    expect(screen.getByText("下一步")).toBeTruthy();
  });

  it("应显示第一步内容", () => {
    render(
      <BrowserRouter>
        <OnboardingWizard />
      </BrowserRouter>
    );
    expect(screen.getByTestId("step1-info")).toBeTruthy();
  });
});
