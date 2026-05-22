import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import DryRunResultPanel from "./DryRunResultPanel";
import type { DryRunResultPanelProps } from "./DryRunResultPanel.types";

// Mock CSS module
vi.mock("./dryRunResultPanel.module.css", () => ({
  default: {
    panelCard: "panelCard",
    loadingContainer: "loadingContainer",
    emptyState: "emptyState",
    scrollableResults: "scrollableResults",
    listItem: "listItem",
    itemContent: "itemContent",
    itemHeader: "itemHeader",
    smallText: "smallText",
    messageParagraph: "messageParagraph",
    detailsBlock: "detailsBlock",
    detailsPre: "detailsPre",
  },
}));

const sampleResults: DryRunResultPanelProps["results"] = [
  {
    id: "1",
    status: "success",
    title: "规则 R001",
    message: "规则执行成功",
    timestamp: "2026-05-19 10:30",
    duration: 120,
  },
  {
    id: "2",
    status: "error",
    title: "规则 R002",
    message: "规则执行失败：参数缺失",
    timestamp: "2026-05-19 10:31",
    duration: 50,
  },
  {
    id: "3",
    status: "warning",
    title: "规则 R003",
    message: "规则命中但置信度偏低",
    timestamp: "2026-05-19 10:32",
    duration: 80,
  },
];

describe("DryRunResultPanel", () => {
  it("应渲染标题", () => {
    render(<DryRunResultPanel results={sampleResults} />);
    expect(screen.getByText("测试运行结果")).toBeTruthy();
  });

  it("应渲染结果列表", () => {
    render(<DryRunResultPanel results={sampleResults} />);
    expect(screen.getByText("规则 R001")).toBeTruthy();
    expect(screen.getByText("规则 R002")).toBeTruthy();
    expect(screen.getByText("规则 R003")).toBeTruthy();
  });

  it("应显示成功/失败/警告计数标签", () => {
    render(<DryRunResultPanel results={sampleResults} />);
    expect(screen.getByText("1 成功")).toBeTruthy();
    expect(screen.getByText("1 失败")).toBeTruthy();
    expect(screen.getByText("1 警告")).toBeTruthy();
  });

  it("应显示结果消息", () => {
    render(<DryRunResultPanel results={sampleResults} />);
    expect(screen.getByText("规则执行成功")).toBeTruthy();
    expect(screen.getByText("规则执行失败：参数缺失")).toBeTruthy();
  });

  it("应显示持续时间", () => {
    render(<DryRunResultPanel results={sampleResults} showDuration />);
    expect(screen.getByText("120ms")).toBeTruthy();
  });

  it("应显示时间戳", () => {
    render(<DryRunResultPanel results={sampleResults} showTimestamp />);
    expect(screen.getByText("2026-05-19 10:30")).toBeTruthy();
  });

  it("空结果应显示空状态", () => {
    render(<DryRunResultPanel results={[]} />);
    expect(screen.getByText("暂无测试结果")).toBeTruthy();
  });

  it("loading 状态应显示加载指示器", () => {
    render(<DryRunResultPanel results={[]} loading />);
    expect(screen.getByText("正在运行测试...")).toBeTruthy();
  });

  it("应显示重新运行按钮", () => {
    const onRetry = vi.fn();
    render(<DryRunResultPanel results={sampleResults} onRetry={onRetry} />);
    fireEvent.click(screen.getByText("重新运行"));
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it("应显示清除结果按钮", () => {
    const onClear = vi.fn();
    render(<DryRunResultPanel results={sampleResults} onClear={onClear} />);
    fireEvent.click(screen.getByText("清除结果"));
    expect(onClear).toHaveBeenCalledTimes(1);
  });

  it("应渲染 details 信息", () => {
    const resultsWithDetails = [
      {
        id: "1",
        status: "error" as const,
        title: "规则 R001",
        message: "执行失败",
        details: { error: "参数缺失", code: "PARAM_MISSING" },
      },
    ];
    render(<DryRunResultPanel results={resultsWithDetails} />);
    expect(screen.getByText(/PARAM_MISSING/)).toBeTruthy();
  });
});
