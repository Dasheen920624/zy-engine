import { render, screen } from "@testing-library/react";
import { ConfigProvider } from "antd";
import { describe, expect, it } from "vitest";
import ConfigPackages from "./tenant/ConfigPackages";
import WorkflowTodos from "./clinical/WorkflowTodos";
import QcAlerts from "./quality/QcAlerts";
import AdminUsers from "./compliance/AdminUsers";
import GraphExplore from "./advanced/GraphExplore";
import Dashboard from "./Dashboard";

function renderPage(page: React.ReactElement) {
  return render(<ConfigProvider>{page}</ConfigProvider>);
}

describe("page smoke coverage", () => {
  it("renders a tenant configuration page", () => {
    renderPage(<ConfigPackages />);
    expect(screen.getByRole("heading", { name: "配置包中心" })).toBeInTheDocument();
    expect(screen.getByText("胸痛 AMI 标准包")).toBeInTheDocument();
  });

  it("renders a clinical list page", () => {
    renderPage(<WorkflowTodos />);
    expect(screen.getByRole("heading", { name: "待办中心" })).toBeInTheDocument();
    expect(screen.getByText(/规则发布审核/)).toBeInTheDocument();
  });

  it("renders a quality alert page", () => {
    renderPage(<QcAlerts />);
    expect(screen.getByRole("heading", { name: "质控预警" })).toBeInTheDocument();
    expect(screen.getByText(/抗菌药使用率/)).toBeInTheDocument();
  });

  it("renders a compliance page", () => {
    renderPage(<AdminUsers />);
    expect(screen.getByRole("heading", { name: "用户管理" })).toBeInTheDocument();
    expect(screen.getByText("医务处主任")).toBeInTheDocument();
  });

  it("renders an advanced tool page with advanced-only messaging", () => {
    renderPage(<GraphExplore />);
    expect(screen.getByRole("heading", { name: "图谱查询" })).toBeInTheDocument();
    expect(screen.getAllByText(/高级工具/).length).toBeGreaterThan(0);
  });

  it("renders the dashboard workbench", () => {
    renderPage(<Dashboard />);
    expect(screen.getByText("租户生命周期")).toBeInTheDocument();
    expect(screen.getByText("本周建议动作")).toBeInTheDocument();
  });
});
