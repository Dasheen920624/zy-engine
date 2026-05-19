import { Navigate } from "react-router-dom";
import type { RouteObject } from "react-router-dom";
import AppLayout from "../layouts/AppLayout";
import Dashboard from "../pages/Dashboard";
import ProvidersStatus from "../pages/ProvidersStatus";
import DemoValidation from "../pages/DemoValidationPlaceholder";
import ConfigPackages from "../pages/ConfigPackages";
import PackageImportWizard from "../pages/ConfigPackages/PackageImportWizard";
import ProvenancePlaceholder from "../pages/ProvenancePlaceholder";
import WorkflowTodos from "../pages/WorkflowTodos";
import { QualityDashboard, DepartmentDrillDown } from "../pages/Quality";
import NotFound from "../pages/NotFound";
import Login from "../pages/Login";
import RequireAuth from "./RequireAuth";
import PlaceholderPage from "../components/PlaceholderPage";
import { MappingWorkbench } from "../pages/Terminology";
import PathwayList from "../pages/Pathway/PathwayList";
import PathwayDetail from "../pages/Pathway/PathwayDetail";

export const routes: RouteObject[] = [
  { path: "/login", element: <Login /> },
  {
    element: <RequireAuth />,
    children: [
      {
        path: "/",
        element: <AppLayout />,
        children: [
          { index: true, element: <Navigate to="/dashboard" replace /> },
          { path: "dashboard", element: <Dashboard /> },
          { path: "demo-validation", element: <DemoValidation /> },
          { path: "config-packages", element: <Navigate to="/config/packages" replace /> },
          { path: "config/packages", element: <ConfigPackages /> },
          { path: "config/packages/import", element: <PackageImportWizard /> },
          { path: "provenance", element: <ProvenancePlaceholder /> },
          { path: "system/providers", element: <ProvidersStatus /> },
          { path: "pathway/templates", element: <PathwayList /> },
          { path: "pathway/templates/:code", element: <PathwayDetail /> },
          { path: "pathway/templates/:code/edit", element: <PlaceholderPage title="路径模板编辑器" pr="PR-V2-07" /> },
          { path: "pathway/templates/:code/diff", element: <PlaceholderPage title="路径版本对比" pr="PR-V2-07" /> },
          { path: "pathway/patients", element: <PlaceholderPage title="患者路径管理" pr="PR-V2-09" /> },
          { path: "rule/definitions", element: <PlaceholderPage title="规则库" pr="PR-V2-05" /> },
          { path: "rule/definitions/:code/edit", element: <PlaceholderPage title="规则 DSL 编辑器" pr="PR-V2-05" /> },
          { path: "rule/validate", element: <PlaceholderPage title="规则校验工作台" /> },
          { path: "graph/explore", element: <PlaceholderPage title="图谱查询工作台" pr="PR-V2-05" /> },
          { path: "terminology/mapping", element: <MappingWorkbench /> },
          { path: "qc/alerts", element: <PlaceholderPage title="质控预警列表" pr="PR-V2-11" /> },
          { path: "qc/dashboard", element: <QualityDashboard /> },
          { path: "qc/department/:deptCode", element: <DepartmentDrillDown /> },
          { path: "qc/insurance", element: <PlaceholderPage title="医保智能审核" pr="PR-V2-12" /> },
          { path: "aik/review", element: <PlaceholderPage title="知识审核台" pr="PR-V2-05" /> },
          { path: "workflow/todos", element: <WorkflowTodos /> },
          { path: "admin/users", element: <PlaceholderPage title="用户管理" pr="PR-V2-04" /> },
          { path: "admin/audit", element: <PlaceholderPage title="审计日志" pr="PR-V2-04" /> },
          { path: "*", element: <NotFound /> },
        ],
      },
    ],
  },
  { path: "*", element: <Navigate to="/login" replace /> },
];
