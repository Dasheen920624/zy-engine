import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import EvalIndicatorSetList from "../EvalIndicatorSetList";

// Mock API
vi.mock("../../../api/eval", () => ({
  listEvalSets: vi.fn().mockResolvedValue([]),
  createEvalSet: vi.fn(),
  updateEvalSet: vi.fn(),
  publishEvalSet: vi.fn(),
  deprecateEvalSet: vi.fn(),
  listEvalIndicators: vi.fn().mockResolvedValue([]),
  createEvalIndicator: vi.fn(),
  updateEvalIndicator: vi.fn(),
  deleteEvalIndicator: vi.fn(),
}));

// Mock CSS module
vi.mock("../evalIndicatorSetList.module.css", () => ({
  default: {
    page: "page",
    detailMeta: "detailMeta",
    fullWidth: "fullWidth",
  },
}));

// Mock SourceInfo
vi.mock("../../../components/SourceInfo/SourceInfo", () => ({
  SourceInfo: () => <div data-testid="source-info" />,
}));

// Mock StatusBadge
vi.mock("../../../components/StatusBadge/StatusBadge", () => ({
  StatusBadge: ({ text }: { text: string }) => <span>{text}</span>,
}));

describe("EvalIndicatorSetList", () => {
  it("应渲染评估指标集页面", () => {
    render(<EvalIndicatorSetList />);
  });

  it("应显示评估指标集标题", () => {
    render(<EvalIndicatorSetList />);
    expect(screen.getByText("评估指标集")).toBeTruthy();
  });

  it("应显示新建指标集按钮", () => {
    render(<EvalIndicatorSetList />);
    expect(screen.getByText("新建指标集")).toBeTruthy();
  });
});
