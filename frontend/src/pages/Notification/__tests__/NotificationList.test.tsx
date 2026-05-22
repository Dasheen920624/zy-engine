import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import NotificationList from "../NotificationList";

// Mock API
vi.mock("../../../api/notification", () => ({
  listNotifications: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, page_size: 20, total_pages: 0 }),
  markAsRead: vi.fn(),
  markAllAsRead: vi.fn(),
}));

// Mock CSS module
vi.mock("../NotificationList.module.css", () => ({
  default: {},
}));

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false } },
});

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider client={queryClient}>
    {children}
  </QueryClientProvider>
);

describe("NotificationList", () => {
  it("应渲染通知列表页面", () => {
    render(wrapper(<NotificationList />));
    const heading = screen.queryByRole("heading");
    expect(heading || document.querySelector(".mk-page-header")).toBeTruthy();
  });
});
