import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import AiGeneratedBadge from "./AiGeneratedBadge";
import type { AiGeneratedBadgeProps } from "./AiGeneratedBadge.types";

// Mock CSS module
vi.mock("./aiGeneratedBadge.module.css", () => ({
  default: {
    badgeTag: "badgeTag",
    confidenceDot: "confidenceDot",
    confidenceHigh: "confidenceHigh",
    confidenceMid: "confidenceMid",
    confidenceLow: "confidenceLow",
    cardContainer: "cardContainer",
    cardHeader: "cardHeader",
    cardContent: "cardContent",
    cardActions: "cardActions",
    aiIcon: "aiIcon",
    aiText: "aiText",
    confidenceRow: "confidenceRow",
    confidenceProgress: "confidenceProgress",
    confidenceText: "confidenceText",
    fullWidth: "fullWidth",
  },
}));

const baseProps: AiGeneratedBadgeProps = {
  confidence: 85,
  model: "GPT-4o",
  generatedAt: "2026-05-19 10:30",
  reviewStatus: "pending",
  onAccept: vi.fn(),
  onModify: vi.fn(),
  onReject: vi.fn(),
};

describe("AiGeneratedBadge", () => {
  it("badge 变体应渲染 AI 候选标签", () => {
    render(<AiGeneratedBadge {...baseProps} variant="badge" />);
    expect(screen.getByText("AI 候选")).toBeTruthy();
  });

  it("card 变体应渲染 AI 候选标题和操作按钮", () => {
    render(<AiGeneratedBadge {...baseProps} variant="card" />);
    expect(screen.getByText("AI 候选")).toBeTruthy();
    expect(screen.getByText("采纳")).toBeTruthy();
    expect(screen.getByText("修改")).toBeTruthy();
    expect(screen.getByText("拒绝")).toBeTruthy();
  });

  it("card 变体应显示模型名称", () => {
    render(<AiGeneratedBadge {...baseProps} variant="card" />);
    expect(screen.getByText("GPT-4o")).toBeTruthy();
  });

  it("card 变体应显示置信度", () => {
    render(<AiGeneratedBadge {...baseProps} variant="card" />);
    expect(screen.getByText("85%")).toBeTruthy();
  });

  it("pending 状态应显示操作按钮", () => {
    render(<AiGeneratedBadge {...baseProps} variant="card" reviewStatus="pending" />);
    expect(screen.getByText("采纳")).toBeTruthy();
  });

  it("accepted 状态不应显示操作按钮", () => {
    render(<AiGeneratedBadge {...baseProps} variant="card" reviewStatus="accepted" />);
    expect(screen.queryByText("采纳")).toBeNull();
  });

  it("点击采纳按钮应调用 onAccept", () => {
    const onAccept = vi.fn();
    render(<AiGeneratedBadge {...baseProps} variant="card" onAccept={onAccept} />);
    fireEvent.click(screen.getByText("采纳"));
    expect(onAccept).toHaveBeenCalledTimes(1);
  });

  it("点击拒绝按钮应调用 onReject", () => {
    const onReject = vi.fn();
    render(<AiGeneratedBadge {...baseProps} variant="card" onReject={onReject} />);
    fireEvent.click(screen.getByText("拒绝"));
    expect(onReject).toHaveBeenCalledTimes(1);
  });

  it("点击修改按钮应调用 onModify", () => {
    const onModify = vi.fn();
    render(<AiGeneratedBadge {...baseProps} variant="card" onModify={onModify} />);
    fireEvent.click(screen.getByText("修改"));
    expect(onModify).toHaveBeenCalledTimes(1);
  });
});
