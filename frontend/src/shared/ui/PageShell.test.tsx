import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Button } from "antd";
import { PageShell } from "./PageShell";

describe("PageShell", () => {
  it("renders one page heading, description, primary action, and extras", () => {
    render(
      <PageShell
        title="配置包中心"
        description="导入、校验、发布和回滚院内配置"
        primary={<Button type="primary">导入配置包</Button>}
        extras={<Button>保存视图</Button>}
      >
        <div>页面内容</div>
      </PageShell>,
    );

    expect(screen.getByRole("heading", { name: "配置包中心" })).toBeInTheDocument();
    expect(screen.getByText("导入、校验、发布和回滚院内配置")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "导入配置包" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "保存视图" })).toBeInTheDocument();
    expect(screen.getByText("页面内容")).toBeInTheDocument();
  });
});
