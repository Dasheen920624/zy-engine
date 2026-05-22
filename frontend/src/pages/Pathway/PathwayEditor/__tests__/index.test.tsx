import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import PathwayEditor from "../index";

// Mock API
vi.mock("../../../../api/pathway", () => ({
  getPathway: vi.fn().mockRejectedValue(new Error("mock")),
  savePathwayDraft: vi.fn().mockResolvedValue({}),
  validatePathway: vi.fn().mockResolvedValue({ valid: true, errors: [] }),
  submitPathwayReview: vi.fn().mockResolvedValue({}),
}));

// Mock Allotment
vi.mock("allotment", () => ({
  Allotment: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  default: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

// Mock PathwayCanvas
vi.mock("../../../../components/PathwayCanvas/PathwayCanvas", () => ({
  default: () => <div data-testid="pathway-canvas" />,
}));

// Mock PathwayEditorHeader
vi.mock("../PathwayEditorHeader", () => ({
  default: () => <div data-testid="editor-header" />,
}));

// Mock StageTree
vi.mock("../StageTree", () => ({
  default: () => <div data-testid="stage-tree" />,
}));

// Mock NodePropertyPanel
vi.mock("../NodePropertyPanel", () => ({
  default: () => <div data-testid="node-property-panel" />,
}));

// Mock UnsavedChangesGuard
vi.mock("../UnsavedChangesGuard", () => ({
  default: () => <div data-testid="unsaved-changes-guard" />,
}));

// Mock CSS module
vi.mock("../PathwayEditor.module.css", () => ({
  default: {
    editorPage: "editorPage",
    editorBody: "editorBody",
    sidePane: "sidePane",
    leftPane: "leftPane",
    rightPane: "rightPane",
    loadingState: "loadingState",
  },
}));

describe("PathwayEditor", () => {
  it("应渲染路径编辑器页面", () => {
    render(
      <BrowserRouter>
        <PathwayEditor />
      </BrowserRouter>
    );
  });

  it("应显示路径画布组件", async () => {
    render(
      <BrowserRouter>
        <PathwayEditor />
      </BrowserRouter>
    );
    expect(await screen.findByTestId("pathway-canvas")).toBeTruthy();
  });

  it("应显示编辑器头部", async () => {
    render(
      <BrowserRouter>
        <PathwayEditor />
      </BrowserRouter>
    );
    expect(await screen.findByTestId("editor-header")).toBeTruthy();
  });

  it("应显示阶段树组件", async () => {
    render(
      <BrowserRouter>
        <PathwayEditor />
      </BrowserRouter>
    );
    expect(await screen.findByTestId("stage-tree")).toBeTruthy();
  });

  it("应显示节点属性面板", async () => {
    render(
      <BrowserRouter>
        <PathwayEditor />
      </BrowserRouter>
    );
    expect(await screen.findByTestId("node-property-panel")).toBeTruthy();
  });
});
