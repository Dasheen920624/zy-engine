import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ConfigProvider } from "antd";
import { describe, expect, it } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

import ConfigPackages from "./tenant/ConfigPackages";
import WorkflowTodos from "./clinical/WorkflowTodos";
import QcAlerts from "./quality/QcAlerts";
import AdminUsers from "./compliance/AdminUsers";
import GraphExplore from "./advanced/GraphExplore";
import Dashboard from "./Dashboard";
import Login from "./Login";

const testQueryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});

function renderPage(page: React.ReactElement) {
  return render(
    <QueryClientProvider client={testQueryClient}>
      <ConfigProvider>{page}</ConfigProvider>
    </QueryClientProvider>,
  );
}

describe("page smoke coverage", () => {
  it("renders the tenant config-packages placeholder", () => {
    renderPage(<ConfigPackages />);
    expect(screen.getByRole("heading", { name: "配置包中心" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /查看实施路线图/ })).toBeInTheDocument();
  });

  it("renders the clinical workflow-todos placeholder", () => {
    renderPage(<WorkflowTodos />);
    expect(screen.getByRole("heading", { name: "待办中心" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /查看实施路线图/ })).toBeInTheDocument();
  });

  it("renders the quality qc-alerts placeholder", () => {
    renderPage(<QcAlerts />);
    expect(screen.getByRole("heading", { name: "质控预警与整改工作台" })).toBeInTheDocument();
  });

  it("renders the compliance admin-users placeholder", () => {
    renderPage(<AdminUsers />);
    expect(screen.getByRole("heading", { name: "用户管理" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /查看实施路线图/ })).toBeInTheDocument();
  });

  it("renders an advanced tool page with advanced-only messaging", () => {
    renderPage(<GraphExplore />);
    expect(screen.getByRole("heading", { name: "图谱查询" })).toBeInTheDocument();
    expect(screen.getAllByText(/高级工具/).length).toBeGreaterThan(0);
  });

  it("renders the dashboard workbench with tenant-lifecycle placeholder", () => {
    renderPage(<Dashboard />);
    expect(screen.getByText("租户生命周期")).toBeInTheDocument();
    expect(screen.getByText("本周建议动作")).toBeInTheDocument();
  });

  it("renders the login page as a focused identity entry", async () => {
    render(
      <ConfigProvider>
        <MemoryRouter>
          <Login />
        </MemoryRouter>
      </ConfigProvider>,
    );

    expect(screen.getByRole("heading", { name: "集团医疗智能中枢" })).toBeInTheDocument();
    expect(screen.getByText("当前任务：确认身份并进入工作台")).toBeInTheDocument();
    expect(screen.getByText("安全审计已开启")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "进入工作台" })).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "院方统一身份认证" }));

    expect(screen.getByRole("button", { name: "用 CAS 登录" })).toBeInTheDocument();
    expect(screen.getByText(/统一身份由医院信息中心配置/)).toBeInTheDocument();
  });
});
