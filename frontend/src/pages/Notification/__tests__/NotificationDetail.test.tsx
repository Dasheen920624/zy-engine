import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import NotificationDetail from "../NotificationDetail";

// Mock API
vi.mock("../../../api/notification", () => ({
  notificationApi: {
    fetchNotification: vi.fn().mockResolvedValue({
      id: 1,
      notificationCode: "NTF-001",
      title: "测试通知标题",
      content: "测试通知内容",
      notificationType: "SYSTEM",
      priority: "NORMAL",
      status: "READ",
      senderName: "系统",
      recipientId: "user-001",
      recipientName: "张三",
      channel: "IN_APP",
      createdTime: "2026-05-20T10:00:00",
      readTime: "2026-05-20T11:00:00",
    }),
    markAsRead: vi.fn(),
    archiveNotification: vi.fn(),
  },
}));

// Mock CSS module
vi.mock("../notificationDetail.module.css", () => ({
  default: {
    page: "page",
    loadingContainer: "loadingContainer",
    emptyContainer: "emptyContainer",
    notificationTitle: "notificationTitle",
    contentSection: "contentSection",
    contentCard: "contentCard",
    notificationParagraph: "notificationParagraph",
    linkButton: "linkButton",
  },
}));

describe("NotificationDetail", () => {
  it("应渲染通知详情页面", () => {
    render(
      <BrowserRouter>
        <NotificationDetail />
      </BrowserRouter>
    );
  });

  it("应显示通知详情标题", async () => {
    render(
      <BrowserRouter>
        <NotificationDetail />
      </BrowserRouter>
    );
    expect(await screen.findByText("通知详情")).toBeTruthy();
  });

  it("应显示返回按钮", async () => {
    render(
      <BrowserRouter>
        <NotificationDetail />
      </BrowserRouter>
    );
    expect(await screen.findByText("返回")).toBeTruthy();
  });

  it("应显示归档按钮", async () => {
    render(
      <BrowserRouter>
        <NotificationDetail />
      </BrowserRouter>
    );
    expect(await screen.findByText("归档")).toBeTruthy();
  });
});
