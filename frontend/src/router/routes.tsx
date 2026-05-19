import { Navigate } from "react-router-dom";
import type { RouteObject } from "react-router-dom";
import AppLayout from "../layouts/AppLayout";
import Dashboard from "../pages/Dashboard";
import ProvidersStatus from "../pages/ProvidersStatus";
import DemoValidation from "../pages/DemoValidationPlaceholder";
import ConfigPackages from "../pages/ConfigPackages";
import ProvenancePlaceholder from "../pages/ProvenancePlaceholder";
import NotFound from "../pages/NotFound";
import Login from "../pages/Login";
import RequireAuth from "./RequireAuth";
import PlaceholderPage from "../components/PlaceholderPage";

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
          { path: "config/packages/import", element: <PlaceholderPage title="配置包发布向导" pr="PR-V2-05" /> },
          { path: "provenance", element: <ProvenancePlaceholder /> },
          { path: "system/providers", element: <ProvidersStatus /> },
          { path: "pathway/templates", element: <PlaceholderPage title="路径模板列表" pr="PR-V2-06" /> },
          { path: "pathway/templates/:code/edit", element: <PlaceholderPage title="路径模板编辑器" pr="PR-V2-07" /> },
          { path: "pathway/templates/:code/diff", element: <PlaceholderPage title="路径版本对比" pr="PR-V2-07" /> },
          { path: "pathway/patients", element: <PlaceholderPage title="患者路径管理" pr="PR-V2-09" /> },
          { path: "rule/definitions", element: <PlaceholderPage title="规则库" pr="PR-V2-05" /> },
          { path: "rule/definitions/:code/edit", element: <PlaceholderPage title="规则 DSL 编辑器" pr="PR-V2-05" /> },
          { path: "rule/validate", element: <PlaceholderPage title="规则校验工作台" /> },
          { path: "graph/explore", element: <PlaceholderPage title="图谱查询工作台" pr="PR-V2-05" /> },
          { path: "terminology/mapping", element: <PlaceholderPage title="字典映射工作台" pr="PR-V2-08" /> },
          { path: "qc/alerts", element: <PlaceholderPage title="质控预警列表" pr="PR-V2-11" /> },
          { path: "qc/dashboard", element: <PlaceholderPage title="院级质控驾驶舱" pr="PR-V2-12" /> },
          { path: "qc/insurance", element: <PlaceholderPage title="医保智能审核" pr="PR-V2-12" /> },
          { path: "aik/review", element: <PlaceholderPage title="知识审核台" pr="PR-V2-05" /> },
          { path: "admin/users", element: <PlaceholderPage title="用户管理" pr="PR-V2-04" /> },
          { path: "admin/audit", element: <PlaceholderPage title="审计日志" pr="PR-V2-04" /> },
          { path: "*", element: <NotFound /> },
        ],
      },
    ],
  },
  { path: "*", element: <Navigate to="/login" replace /> },
];
