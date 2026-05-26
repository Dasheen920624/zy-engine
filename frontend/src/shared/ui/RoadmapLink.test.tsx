import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { RoadmapLink } from "./RoadmapLink";

describe("RoadmapLink", () => {
  it("默认按钮文案触发路线图弹窗", async () => {
    render(<RoadmapLink taskIds={["GA-ENG-RULE-01"]} />);
    const trigger = screen.getByRole("button", { name: /查看实施路线图/ });
    await userEvent.click(trigger);
    expect(screen.getByText(/引擎能力规划/)).toBeInTheDocument();
  });

  it("弹窗内展示传入的全部任务 ID", async () => {
    render(<RoadmapLink taskIds={["GA-ENG-RULE-01", "GA-ENG-API-05"]} />);
    await userEvent.click(screen.getByRole("button", { name: /查看实施路线图/ }));
    expect(screen.getByText("GA-ENG-RULE-01")).toBeInTheDocument();
    expect(screen.getByText("GA-ENG-API-05")).toBeInTheDocument();
  });

  it("弹窗内链接指向 docs/backlog.md", async () => {
    render(<RoadmapLink taskIds={["GA-ENG-RULE-01"]} />);
    await userEvent.click(screen.getByRole("button", { name: /查看实施路线图/ }));
    const link = screen.getByRole("link", { name: /docs\/backlog\.md/ });
    expect(link).toHaveAttribute("href", "/docs/backlog.md");
  });
});
