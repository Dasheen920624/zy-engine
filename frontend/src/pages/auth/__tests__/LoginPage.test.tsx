import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import { App } from "antd";
import LoginPage from "../LoginPage";

// Mock CSS module
vi.mock("../styles.module.css", () => ({
  default: {
    shell: "shell",
    surface: "surface",
    brandPanel: "brandPanel",
    logoMark: "logoMark",
    securityStrip: "securityStrip",
    loginCard: "loginCard",
    cardHeader: "cardHeader",
    ssoCollapse: "ssoCollapse",
    ssoCollapseLabel: "ssoCollapseLabel",
    timeoutAlert: "timeoutAlert",
    callbackCard: "callbackCard",
  },
}));

// Mock auth API
vi.mock("../../../api/auth", () => ({
  login: vi.fn(),
}));

// Mock SSO API
vi.mock("../../../api/sso", () => ({
  listSsoProviders: vi.fn().mockResolvedValue([]),
  initiateSso: vi.fn(),
  handleSsoCallback: vi.fn(),
}));

// Mock auth store
vi.mock("../../../store/auth", () => ({
  setAuth: vi.fn(),
}));

// Mock config
vi.mock("../config", () => ({
  loginRuntimeConfig: {
    defaultMethod: "password",
    icpNumber: "京ICP备20260521号-1",
    psbNumber: "京公网安备 11000002026021 号",
    profile: "demo",
    cryptoSuite: "SM2",
    appVersion: "v1.0-ga",
    lockThreshold: 5,
    sessionTimeoutMinutes: 30,
  },
  isDemoProfile: vi.fn().mockReturnValue(true),
}));

// Mock sub-components
vi.mock("../ComplianceFooter", () => ({
  ComplianceFooter: () => <div data-testid="compliance-footer" />,
}));
vi.mock("../DemoHint", () => ({
  DemoHint: () => <div data-testid="demo-hint" />,
}));
vi.mock("../tabs/PasswordTab", () => ({
  PasswordTab: ({ onSubmit: _onSubmit }: { onSubmit: (v: unknown) => void }) => (
    <div data-testid="password-tab" />
  ),
}));
vi.mock("../tabs/SsoTab", () => ({
  SsoTab: () => <div data-testid="sso-tab" />,
}));

function renderLoginPage(props?: { initialTab?: "password" | "sso" }) {
  return render(
    <BrowserRouter>
      <App>
        <LoginPage {...props} />
      </App>
    </BrowserRouter>
  );
}

describe("LoginPage", () => {
  it("应渲染登录页面", () => {
    renderLoginPage();
    expect(screen.getByText("登录 MedKernel")).toBeTruthy();
  });

  it("应显示品牌信息", () => {
    renderLoginPage();
    expect(screen.getByText("集团医疗智能中枢")).toBeTruthy();
  });

  it("应显示密码登录表单", () => {
    renderLoginPage();
    expect(screen.getByTestId("password-tab")).toBeTruthy();
  });

  it("应显示会话过期提示", () => {
    renderLoginPage();
    expect(screen.getByText(/距会话过期 2 分钟时/)).toBeTruthy();
  });

  it("应显示合规底部信息", () => {
    renderLoginPage();
    expect(screen.getByTestId("compliance-footer")).toBeTruthy();
  });
});
