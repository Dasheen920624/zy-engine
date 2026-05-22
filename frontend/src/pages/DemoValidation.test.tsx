import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import DemoValidation from "./DemoValidation";

// Mock CSS module
vi.mock("./demoValidation.module.css", () => ({
  default: {
    mainContent: "mainContent",
    steps: "steps",
    traceInfo: "traceInfo",
  },
}));

// Mock API client
vi.mock("../api/client", () => ({
  get: vi.fn().mockRejectedValue(new Error("mock")),
  post: vi.fn().mockRejectedValue(new Error("mock")),
}));

function renderWithQueryClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      {ui}
    </QueryClientProvider>
  );
}

describe("DemoValidation", () => {
  it("应渲染演示与校验工作台标题", () => {
    renderWithQueryClient(<DemoValidation />);
    expect(screen.getByText("演示与校验工作台")).toBeTruthy();
  });

  it("应显示场景选择器", () => {
    renderWithQueryClient(<DemoValidation />);
    expect(screen.getByText("AMI 入径")).toBeTruthy();
  });

  it("应显示执行验证按钮", () => {
    renderWithQueryClient(<DemoValidation />);
    expect(screen.getByText("执行验证")).toBeTruthy();
  });

  it("应显示刷新记录按钮", () => {
    renderWithQueryClient(<DemoValidation />);
    expect(screen.getByText("刷新记录")).toBeTruthy();
  });

  it("应显示验证结果卡片", () => {
    renderWithQueryClient(<DemoValidation />);
    expect(screen.getByText("验证结果")).toBeTruthy();
  });

  it("应显示规则命中与证据卡片", () => {
    renderWithQueryClient(<DemoValidation />);
    expect(screen.getByText("规则命中与证据")).toBeTruthy();
  });

  it("应显示最近验证记录卡片", () => {
    renderWithQueryClient(<DemoValidation />);
    expect(screen.getByText("最近验证记录")).toBeTruthy();
  });
});
