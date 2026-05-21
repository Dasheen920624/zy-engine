import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import PathwayTimeline from "../PathwayTimeline";

describe("PathwayTimeline", () => {
  it("shows draft item when draftStatus = DRAFT", () => {
    render(
      <PathwayTimeline
        draftStatus="DRAFT"
        publishedVersions={["1.0.0"]}
        activeVersion="1.0.0"
        selectedVersion="1.0.0"
      />,
    );
    expect(screen.getByText("草稿")).toBeInTheDocument();
    expect(screen.getByText("未发布")).toBeInTheDocument();
  });

  it("marks active version with 激活中 tag", () => {
    render(
      <PathwayTimeline
        draftStatus="NONE"
        publishedVersions={["1.0.0", "2.0.0"]}
        activeVersion="2.0.0"
        selectedVersion="2.0.0"
      />,
    );
    expect(screen.getByText("激活中")).toBeInTheDocument();
    expect(screen.getByText("v2.0.0")).toBeInTheDocument();
    expect(screen.getByText("v1.0.0")).toBeInTheDocument();
  });

  it("calls onDiffVersion when diff button clicked", async () => {
    const onDiff = vi.fn();
    const user = userEvent.setup();
    render(
      <PathwayTimeline
        draftStatus="NONE"
        publishedVersions={["1.0.0", "2.0.0"]}
        activeVersion="2.0.0"
        selectedVersion="2.0.0"
        onDiffVersion={onDiff}
      />,
    );
    const button = screen.getByLabelText("diff-1.0.0");
    await user.click(button);
    expect(onDiff).toHaveBeenCalledWith("1.0.0");
  });

  it("renders 暂无版本 when both draft and published are absent", () => {
    render(
      <PathwayTimeline
        draftStatus="NONE"
        publishedVersions={[]}
        activeVersion={null}
        selectedVersion={null}
      />,
    );
    expect(screen.getByText("暂无版本")).toBeInTheDocument();
  });
});
