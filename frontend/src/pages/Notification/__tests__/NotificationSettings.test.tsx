import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import NotificationSettings from "../NotificationSettings";

// Mock CSS module
vi.mock("../notificationSettings.module.css", () => ({
  default: {
    page: "page",
    settingsCard: "settingsCard",
    channelContainer: "channelContainer",
    channelItem: "channelItem",
    section: "section",
  },
}));

describe("NotificationSettings", () => {
  it("应渲染通知设置页面", () => {
    render(<NotificationSettings />);
  });

  it("应显示通知设置标题", () => {
    render(<NotificationSettings />);
    expect(screen.getByText("通知设置")).toBeTruthy();
  });

  it("应显示保存设置按钮", () => {
    render(<NotificationSettings />);
    expect(screen.getByText("保存设置")).toBeTruthy();
  });

  it("应显示通知类型标签", () => {
    render(<NotificationSettings />);
    expect(screen.getByText("系统通知")).toBeTruthy();
    expect(screen.getByText("工作流通知")).toBeTruthy();
    expect(screen.getByText("告警通知")).toBeTruthy();
    expect(screen.getByText("提醒通知")).toBeTruthy();
  });

  it("应显示渠道名称", () => {
    render(<NotificationSettings />);
    expect(screen.getByText("应用内通知")).toBeTruthy();
    expect(screen.getByText("邮件")).toBeTruthy();
    expect(screen.getByText("短信")).toBeTruthy();
    expect(screen.getByText("企业微信")).toBeTruthy();
  });

  it("应显示通知说明区域", () => {
    render(<NotificationSettings />);
    expect(screen.getByText("通知说明")).toBeTruthy();
  });

  it("应显示渠道说明区域", () => {
    render(<NotificationSettings />);
    expect(screen.getByText("渠道说明")).toBeTruthy();
  });
});
