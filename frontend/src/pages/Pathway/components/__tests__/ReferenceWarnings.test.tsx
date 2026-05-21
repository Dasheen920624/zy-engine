import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import ReferenceWarnings from "../ReferenceWarnings";

describe("ReferenceWarnings", () => {
  it("renders nothing when warnings is empty", () => {
    const { container } = render(<ReferenceWarnings warnings={[]} />);
    expect(container.firstChild).toBeNull();
  });

  it("renders count and items", () => {
    render(
      <ReferenceWarnings
        warnings={[
          "节点 N1 缺失来源引用",
          { message: "节点 N2 引用文档已下线" },
          { reason: "任务 T1 引用版本过旧" },
        ]}
      />,
    );
    expect(screen.getByText(/3 项/)).toBeInTheDocument();
    expect(screen.getByText("节点 N1 缺失来源引用")).toBeInTheDocument();
    expect(screen.getByText("节点 N2 引用文档已下线")).toBeInTheDocument();
    expect(screen.getByText("任务 T1 引用版本过旧")).toBeInTheDocument();
  });
});
