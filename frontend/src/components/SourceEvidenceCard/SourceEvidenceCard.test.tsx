import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import SourceEvidenceCard from "./SourceEvidenceCard";
import type { SourceEvidenceCardProps } from "./SourceEvidenceCard.types";

// Mock CSS module
vi.mock("./sourceEvidenceCard.module.css", () => ({
  default: {
    inline: "inline",
    inlineMissing: "inlineMissing",
    inlineHeader: "inlineHeader",
    card: "card",
    cardMissing: "cardMissing",
    cardHeader: "cardHeader",
    cardReviewInfo: "cardReviewInfo",
    compact: "compact",
    compactMissing: "compactMissing",
    citationContainer: "citationContainer",
    citationLabel: "citationLabel",
    citationExcerpt: "citationExcerpt",
  },
}));

const baseProps: SourceEvidenceCardProps = {
  source: {
    documentName: "2023 ACC/AHA AMI 指南",
    documentId: "doc-001",
    section: "4.2",
    publishYear: 2023,
  },
  review: {
    status: "reviewed",
    reviewerName: "张医生",
    reviewedAt: "2026-01-15",
  },
  version: "2.1.0",
};

describe("SourceEvidenceCard", () => {
  it("inline 变体应渲染来源名称", () => {
    render(<SourceEvidenceCard {...baseProps} variant="inline" />);
    expect(screen.getByText("2023 ACC/AHA AMI 指南")).toBeTruthy();
  });

  it("应显示审核状态", () => {
    render(<SourceEvidenceCard {...baseProps} variant="inline" />);
    expect(screen.getByText("已审核")).toBeTruthy();
  });

  it("应显示版本号", () => {
    render(<SourceEvidenceCard {...baseProps} variant="inline" />);
    expect(screen.getByText("v2.1.0")).toBeTruthy();
  });

  it("card 变体应渲染引用片段", () => {
    render(
      <SourceEvidenceCard
        {...baseProps}
        variant="card"
        citation={{ id: "cite-001", excerpt: "推荐使用阿司匹林", pageNumber: 42 }}
      />
    );
    expect(screen.getByText("推荐使用阿司匹林")).toBeTruthy();
  });

  it("compact 变体应渲染来源名称和状态", () => {
    render(<SourceEvidenceCard {...baseProps} variant="compact" />);
    expect(screen.getByText("2023 ACC/AHA AMI 指南")).toBeTruthy();
    expect(screen.getByText("已审核")).toBeTruthy();
  });

  it("pending 状态应显示待审核标签", () => {
    render(
      <SourceEvidenceCard
        {...baseProps}
        review={{ status: "pending" }}
        variant="inline"
      />
    );
    expect(screen.getByText("待审核")).toBeTruthy();
  });

  it("missing 状态应显示来源缺失标签", () => {
    render(
      <SourceEvidenceCard
        {...baseProps}
        review={{ status: "missing" }}
        variant="inline"
      />
    );
    expect(screen.getByText("来源缺失")).toBeTruthy();
  });

  it("点击查看原文应调用 onClickDocument", () => {
    const onClickDocument = vi.fn();
    render(
      <SourceEvidenceCard
        {...baseProps}
        variant="card"
        onClickDocument={onClickDocument}
      />
    );
    fireEvent.click(screen.getByText("查看原文"));
    expect(onClickDocument).toHaveBeenCalledTimes(1);
  });

  it("应显示审核人信息", () => {
    render(<SourceEvidenceCard {...baseProps} variant="inline" />);
    expect(screen.getByText(/张医生/)).toBeTruthy();
  });
});
