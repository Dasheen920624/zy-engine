import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import TenantApplication from "../TenantApplication";

// Mock CSS module
vi.mock("../tenantApplication.module.css", () => ({
  default: {
    page: "page",
    header: "header",
    steps: "steps",
    sectionCard: "sectionCard",
    submitArea: "submitArea",
  },
}));

describe("TenantApplication", () => {
  it("应渲染客户租户开通申请页面", () => {
    render(<TenantApplication />);
  });

  it("应显示申请标题", () => {
    render(<TenantApplication />);
    expect(screen.getByText("客户租户开通申请")).toBeTruthy();
  });

  it("应显示步骤信息", () => {
    render(<TenantApplication />);
    expect(screen.getByText("填写申请")).toBeTruthy();
    expect(screen.getByText("等待审核")).toBeTruthy();
    expect(screen.getByText("开通完成")).toBeTruthy();
  });

  it("应显示企业信息卡片", () => {
    render(<TenantApplication />);
    expect(screen.getByText("企业信息")).toBeTruthy();
  });

  it("应显示联系人信息卡片", () => {
    render(<TenantApplication />);
    expect(screen.getByText("联系人信息")).toBeTruthy();
  });

  it("应显示提交申请按钮", () => {
    render(<TenantApplication />);
    expect(screen.getByText("提交申请")).toBeTruthy();
  });
});
