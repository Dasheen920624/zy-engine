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
import AiWorkflows from "./advanced/AiWorkflows";
import Provenance from "./advanced/Provenance";
import Dashboard from "./Dashboard";
import Login from "./Login";
import AdapterHub from "./tenant/AdapterHub";
import EmbedLaunch from "./clinical/EmbedLaunch";
import QcEvalResults from "./quality/QcEvalResults";
import PatientPathways from "./clinical/PatientPathways";
import Mpi from "./clinical/Mpi";
import CdssFatigue from "./clinical/CdssFatigue";

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
  it("renders the tenant config-packages console", () => {
    renderPage(<ConfigPackages />);
    expect(screen.getByRole("heading", { name: "配置包中心" })).toBeInTheDocument();
    expect(screen.getByText(/一键创建知识配置包草稿/)).toBeInTheDocument();
  });

  it("renders the tenant adapter-hub console", () => {
    renderPage(<AdapterHub />);
    expect(screen.getByRole("heading", { name: "第三方对接总线与页面集成" })).toBeInTheDocument();
    expect(screen.getByText(/Webhook 回调订阅安全自研沙箱/)).toBeInTheDocument();
    expect(screen.getByText(/重试死信与接口存证队列/)).toBeInTheDocument();
  });

  it("renders the clinical embed-launch console in fallback isolation state", () => {
    renderPage(
      <MemoryRouter initialEntries={["/embed/launch"]}>
        <EmbedLaunch />
      </MemoryRouter>,
    );
    expect(screen.getByText(/页面嵌入式临床建议会话已安全隔离/)).toBeInTheDocument();
  });

  it("renders the clinical workflow-todos console", () => {
    renderPage(<WorkflowTodos />);
    expect(screen.getByRole("heading", { name: "工作流协同待办中心" })).toBeInTheDocument();
    expect(screen.getByText(/高危红线挂起待办/)).toBeInTheDocument();
  });

  it("renders the quality qc-alerts placeholder", () => {
    renderPage(<QcAlerts />);
    expect(screen.getByRole("heading", { name: "质控预警与整改工作台" })).toBeInTheDocument();
  });

  it("renders the compliance admin-users console", () => {
    renderPage(<AdminUsers />);
    expect(screen.getByRole("heading", { name: "用户与角色数据范围管理" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "新增角色分配关系" })).toBeInTheDocument();
  });

  it("renders an advanced tool page with advanced-only messaging", () => {
    renderPage(<GraphExplore />);
    expect(screen.getByRole("heading", { name: "图谱查询" })).toBeInTheDocument();
    expect(screen.getAllByText(/高级工具/).length).toBeGreaterThan(0);
  });

  it("renders the advanced ai-workflows engine workbench", () => {
    renderPage(<AiWorkflows />);
    expect(screen.getByRole("heading", { name: "大模型网关与 AI 工作流配置" })).toBeInTheDocument();
    expect(screen.getByText(/混合路由去向策略/)).toBeInTheDocument();
    expect(screen.getByText(/AI 推理脱敏与降级物理沙盒输入端/)).toBeInTheDocument();
  });

  it("renders the advanced provenance audit console", () => {
    renderPage(<Provenance />);
    expect(screen.getByRole("heading", { name: "来源与临床证据追溯" })).toBeInTheDocument();
    expect(screen.getByText(/数字生命周期全景存证条目/)).toBeInTheDocument();
    expect(screen.getByText(/Isolated 子事务合规日志流/)).toBeInTheDocument();
  });

  it("renders the dashboard workbench with tenant-lifecycle placeholder", () => {
    renderPage(<Dashboard />);
    expect(screen.getByText(/租户.*生命周期/)).toBeInTheDocument();
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

  it("renders the quality qc-eval-results console", () => {
    renderPage(<QcEvalResults />);
    expect(screen.getByRole("heading", { name: "评估结果" })).toBeInTheDocument();
    expect(screen.getByText(/总评估病例库/)).toBeInTheDocument();
  });

  it("renders the clinical patient-pathways console", () => {
    renderPage(<PatientPathways />);
    expect(screen.getByRole("heading", { name: "患者路径" })).toBeInTheDocument();
    expect(screen.getByText(/患者 ID 检索/)).toBeInTheDocument();
  });

  it("renders the clinical mpi console", () => {
    renderPage(<Mpi />);
    expect(screen.getByRole("heading", { name: "患者主索引 MPI" })).toBeInTheDocument();
    expect(screen.getByText(/活跃患者主索引/)).toBeInTheDocument();
  });

  it("renders the clinical cdss-fatigue console", () => {
    renderPage(<CdssFatigue />);
    expect(screen.getByRole("heading", { name: "智能建议治理" })).toBeInTheDocument();
    expect(screen.getByText("全部状态")).toBeInTheDocument();
  });
});
