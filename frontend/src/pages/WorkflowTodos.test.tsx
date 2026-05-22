import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import WorkflowTodos from "./WorkflowTodos";

// Mock CSS module
vi.mock("./workflowTodos.module.css", () => ({
  default: {
    alertSpacing: "alertSpacing",
    statsRow: "statsRow",
    filterCard: "filterCard",
    filterSelect: "filterSelect",
    detailCard: "detailCard",
    fullWidth: "fullWidth",
    detailTitle: "detailTitle",
  },
}));

// Mock API
vi.mock("../api/workflow", () => ({
  fetchTodoTasks: vi.fn().mockRejectedValue(new Error("mock")),
  fetchTodoSummary: vi.fn().mockRejectedValue(new Error("mock")),
  approveTask: vi.fn(),
  rejectTask: vi.fn(),
  delegateTask: vi.fn(),
}));

// Mock OrgContextSelector
vi.mock("../components", () => ({
  OrgContextSelector: () => <div data-testid="org-context-selector" />,
}));

describe("WorkflowTodos", () => {
  it("应渲染待办中心标题", async () => {
    render(<WorkflowTodos />);
    expect(screen.getByText("待办中心")).toBeTruthy();
  });

  it("应显示统计卡片", async () => {
    render(<WorkflowTodos />);
    expect(screen.getByText("待处理")).toBeTruthy();
    expect(screen.getByText("紧急")).toBeTruthy();
    expect(screen.getByText("高优先级")).toBeTruthy();
    expect(screen.getByText("已过期")).toBeTruthy();
  });

  it("应显示筛选栏", async () => {
    render(<WorkflowTodos />);
    expect(screen.getByText("状态筛选") || screen.getByPlaceholderText("状态筛选")).toBeTruthy();
  });

  it("应显示待办列表卡片", async () => {
    render(<WorkflowTodos />);
    expect(screen.getByText("待办列表")).toBeTruthy();
  });

  it("应显示刷新按钮", async () => {
    render(<WorkflowTodos />);
    expect(screen.getByText("刷新")).toBeTruthy();
  });
});
