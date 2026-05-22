import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import PathwayCanvas from "./PathwayCanvas";

// Mock @antv/x6
vi.mock("@antv/x6", () => ({
  Graph: vi.fn().mockImplementation(() => ({
    clearCells: vi.fn(),
    addNode: vi.fn(),
    addEdge: vi.fn(),
    getNodes: vi.fn().mockReturnValue([]),
    getEdges: vi.fn().mockReturnValue([]),
    getSelectedCells: vi.fn().mockReturnValue([]),
    getCellById: vi.fn(),
    on: vi.fn(),
    off: vi.fn(),
    dispose: vi.fn(),
    centerContent: vi.fn(),
    zoomTo: vi.fn(),
  })),
  Shape: {
    Edge: vi.fn().mockImplementation(() => ({})),
  },
}));

// Mock CSS
vi.mock("./PathwayCanvas.css", () => ({}));

const mockPathway = {
  code: "AMI_STEMI",
  name: "AMI/STEMI",
  version: "v2.2",
  status: "DRAFT",
  nodes: [
    { id: "start", type: "start", label: "开始", x: 300, y: 40, properties: {} },
    { id: "end", type: "end", label: "结束", x: 300, y: 760, properties: {} },
  ],
  edges: [
    { id: "e1", source: "start", target: "end" },
  ],
};

describe("PathwayCanvas", () => {
  it("应渲染画布容器", () => {
    const { container } = render(
      <PathwayCanvas pathway={mockPathway as never} mode="view" />
    );
    expect(container.querySelector(".pathway-canvas-wrapper")).toBeTruthy();
  });

  it("应显示工具栏按钮", () => {
    render(<PathwayCanvas pathway={mockPathway as never} mode="view" />);
    expect(screen.getByTitle("重置缩放")).toBeTruthy();
    expect(screen.getByTitle("自动布局")).toBeTruthy();
  });

  it("应显示缩放重置按钮文本", () => {
    render(<PathwayCanvas pathway={mockPathway as never} mode="view" />);
    expect(screen.getByText("100%")).toBeTruthy();
  });

  it("应显示布局按钮文本", () => {
    render(<PathwayCanvas pathway={mockPathway as never} mode="view" />);
    expect(screen.getByText("布局")).toBeTruthy();
  });
});
