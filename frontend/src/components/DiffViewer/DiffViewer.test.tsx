import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import DiffViewer from "./DiffViewer";
import type { DiffViewerProps } from "./DiffViewer.types";

// Mock CSS module
vi.mock("./diffViewer.module.css", () => ({
  default: {
    unifiedContainer: "unifiedContainer",
    splitContainer: "splitContainer",
    splitPanel: "splitPanel",
    panelHeader: "panelHeader",
    line: "line",
    lineAdd: "lineAdd",
    lineRemove: "lineRemove",
    lineNormal: "lineNormal",
    linePrefix: "linePrefix",
    linePrefixAdd: "linePrefixAdd",
    linePrefixRemove: "linePrefixRemove",
    linePrefixNormal: "linePrefixNormal",
    lineNumber: "lineNumber",
    titleContainer: "titleContainer",
    loadingContainer: "loadingContainer",
  },
}));

const baseProps: DiffViewerProps = {
  oldContent: "line1\nline2\nline3",
  newContent: "line1\nmodified\nline3",
};

describe("DiffViewer", () => {
  it("应渲染差异内容", () => {
    render(<DiffViewer {...baseProps} />);
    expect(screen.getByText("line1")).toBeTruthy();
    expect(screen.getByText("modified")).toBeTruthy();
    expect(screen.getByText("line3")).toBeTruthy();
  });

  it("应渲染标题", () => {
    render(<DiffViewer {...baseProps} title="配置差异" />);
    expect(screen.getByText("配置差异")).toBeTruthy();
  });

  it("unified 模式应显示添加和删除行", () => {
    const { container } = render(<DiffViewer {...baseProps} mode="unified" />);
    expect(container.querySelector(".lineAdd") || container.querySelector("[class*='lineAdd']")).toBeTruthy();
    expect(container.querySelector(".lineRemove") || container.querySelector("[class*='lineRemove']")).toBeTruthy();
  });

  it("split 模式应显示旧版本和新版本面板", () => {
    render(<DiffViewer {...baseProps} mode="split" oldTitle="旧版本" newTitle="新版本" />);
    expect(screen.getByText("旧版本")).toBeTruthy();
    expect(screen.getByText("新版本")).toBeTruthy();
  });

  it("loading 状态应显示加载指示器", () => {
    render(<DiffViewer {...baseProps} loading />);
    expect(screen.getByText("加载差异中...")).toBeTruthy();
  });

  it("无内容时应显示空状态", () => {
    render(<DiffViewer oldContent="" newContent="" />);
    expect(screen.getByText("无内容可对比")).toBeTruthy();
  });

  it("应显示行号", () => {
    const { container } = render(<DiffViewer {...baseProps} showLineNumbers />);
    const lineNumbers = container.querySelectorAll(".lineNumber");
    expect(lineNumbers.length).toBeGreaterThan(0);
  });
});
