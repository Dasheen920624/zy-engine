import { describe, it, expect, vi, beforeEach } from "vitest";
import { mockHttpGet, mockHttpPost, resetMocks } from "./testUtils";

vi.mock("../client", () => ({
  http: {
    get: (...args: unknown[]) => mockHttpGet(...args),
    post: (...args: unknown[]) => mockHttpPost(...args),
    put: () => vi.fn(),
    delete: () => vi.fn(),
    patch: () => vi.fn(),
  },
  get: () => vi.fn(),
  post: () => vi.fn(),
  put: () => vi.fn(),
  del: () => vi.fn(),
}));

import * as notification from "../notification";

beforeEach(resetMocks);

describe("notification", () => {
  it("notificationApi.createNotification should POST /api/notifications", async () => {
    mockHttpPost.mockResolvedValueOnce({ data: { id: 1 } });
    await notification.notificationApi.createNotification({
      title: "Test",
      content: "Hello",
      notificationType: "SYSTEM",
      recipientId: "U1",
    });
    expect(mockHttpPost).toHaveBeenCalledWith(
      "/api/notifications",
      expect.objectContaining({ title: "Test" }),
    );
  });

  it("notificationApi.fetchNotifications should GET /api/notifications with params", async () => {
    mockHttpGet.mockResolvedValueOnce({ data: [] });
    await notification.notificationApi.fetchNotifications({ recipientId: "U1", status: "UNREAD" });
    expect(mockHttpGet).toHaveBeenCalledWith(
      "/api/notifications",
      expect.objectContaining({ params: { recipientId: "U1", status: "UNREAD" } }),
    );
  });

  it("notificationApi.fetchNotification should GET /api/notifications/{code}", async () => {
    mockHttpGet.mockResolvedValueOnce({ data: {} });
    await notification.notificationApi.fetchNotification("NOTIF1");
    expect(mockHttpGet).toHaveBeenCalledWith("/api/notifications/NOTIF1");
  });

  it("notificationApi.markAsRead should POST /api/notifications/{code}/read", async () => {
    mockHttpPost.mockResolvedValueOnce({ data: {} });
    await notification.notificationApi.markAsRead("NOTIF1");
    expect(mockHttpPost).toHaveBeenCalledWith("/api/notifications/NOTIF1/read");
  });

  it("notificationApi.batchMarkAsRead should POST /api/notifications/batch-read", async () => {
    mockHttpPost.mockResolvedValueOnce({ data: { successCount: 2 } });
    await notification.notificationApi.batchMarkAsRead(["N1", "N2"]);
    expect(mockHttpPost).toHaveBeenCalledWith(
      "/api/notifications/batch-read",
      { notificationCodes: ["N1", "N2"] },
    );
  });

  it("notificationApi.archiveNotification should POST /api/notifications/{code}/archive", async () => {
    mockHttpPost.mockResolvedValueOnce({ data: {} });
    await notification.notificationApi.archiveNotification("NOTIF1");
    expect(mockHttpPost).toHaveBeenCalledWith("/api/notifications/NOTIF1/archive");
  });

  it("notificationApi.fetchUnreadCount should GET /api/notifications/unread-count", async () => {
    mockHttpGet.mockResolvedValueOnce({ data: { unreadCount: 5 } });
    await notification.notificationApi.fetchUnreadCount("U1");
    expect(mockHttpGet).toHaveBeenCalledWith(
      "/api/notifications/unread-count",
      expect.objectContaining({ params: { recipientId: "U1" } }),
    );
  });

  it("notificationApi.fetchNotificationSummary should GET /api/notifications/summary", async () => {
    mockHttpGet.mockResolvedValueOnce({ data: { total: 10, unread: 3 } });
    await notification.notificationApi.fetchNotificationSummary("U1");
    expect(mockHttpGet).toHaveBeenCalledWith(
      "/api/notifications/summary",
      expect.objectContaining({ params: { recipientId: "U1" } }),
    );
  });

  it("notificationApi.cleanupExpiredNotifications should POST /api/notifications/cleanup", async () => {
    mockHttpPost.mockResolvedValueOnce({ data: { cleanedCount: 5 } });
    await notification.notificationApi.cleanupExpiredNotifications();
    expect(mockHttpPost).toHaveBeenCalledWith("/api/notifications/cleanup");
  });
});
