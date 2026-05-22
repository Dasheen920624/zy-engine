import { Navigate } from "react-router-dom";
import type { RouteObject } from "react-router-dom";
import AppLayout from "../layouts/AppLayout";
import Dashboard from "../pages/Dashboard";
import ProvidersStatus from "../pages/ProvidersStatus";
import DemoValidation from "../pages/DemoValidation";
import ConfigPackages from "../pages/ConfigPackages";
import PackageImportWizard from "../pages/ConfigPackages/PackageImportWizard";
import ProvenancePlaceholder from "../pages/ProvenancePlaceholder";
import WorkflowTodos from "../pages/WorkflowTodos";
import { QualityDashboard, DepartmentDrillDown, EvalIndicatorSetList, EvalResultList } from "../pages/Quality";
import AlertList from "../pages/Quality/AlertList";
import EvalReportPage from "../pages/Quality/EvalReportPage";
import { KnowledgePage } from "../pages/Knowledge";
import { SecurityBaselinePage } from "../pages/Security";
import AlertFatiguePage from "../pages/CDSS/AlertFatiguePage";
import IdentityBindingManagement from "../pages/IdentityBindingManagement";
import { NotificationList, NotificationDetail, NotificationSettings } from "../pages/Notification";
import NotFound from "../pages/NotFound";
import LoginPage from "../pages/auth/LoginPage";
import RequireAuth from "./RequireAuth";
import PlaceholderPage from "../components/PlaceholderPage";
import { MappingWorkbench } from "../pages/Terminology";
import {
  PathwayList,
  PathwayDetail,
  PathwayDiff,
  PathwayEditor,
  PatientPathwayList,
  PatientPathwayDetail,
} from "../pages/Pathway";
import { RuleList, RuleDetail, RuleEditor } from "../pages/Rule";
import { AiWorkflowsPage } from "../pages/AiWorkflows";
import TenantOnboarding from "../pages/Tenant/Onboarding";
import { MpiPatientsPage } from "../pages/Mpi";
import { AuditLogList } from "../pages/Admin/AuditLog";
import { AdapterHubPage } from "../pages/Adapter";

export const routes: RouteObject[] = [
  { path: "/login", element: <LoginPage /> },
  { path: "/sso-login", element: <LoginPage initialTab="sso" /> },
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
          { path: "pathway/templates/:code/edit", element: <PathwayEditor /> },
          { path: "pathway/templates/:code/diff", element: <PathwayDiff /> },
          { path: "pathway/patients", element: <PatientPathwayList /> },
          { path: "pathway/patients/:instanceId", element: <PatientPathwayDetail /> },
          { path: "rule/definitions", element: <RuleList /> },
          { path: "rule/definitions/:code", element: <RuleDetail /> },
          { path: "rule/definitions/:code/edit", element: <RuleEditor /> },
          { path: "rule/validate", element: <PlaceholderPage title="规则校验工作台" /> },
          { path: "graph/explore", element: <PlaceholderPage title="图谱查询工作台" pr="PR-V2-05" /> },
          { path: "terminology/mapping", element: <MappingWorkbench /> },
          { path: "qc/alerts", element: <AlertList /> },
          { path: "qc/dashboard", element: <QualityDashboard /> },
          { path: "qc/eval/sets", element: <EvalIndicatorSetList /> },
          { path: "qc/eval/results", element: <EvalResultList /> },
          { path: "qc/eval/reports", element: <EvalReportPage /> },
          { path: "qc/department/:deptCode", element: <DepartmentDrillDown /> },
          { path: "qc/insurance", element: <PlaceholderPage title="医保智能审核" pr="PR-V2-12" /> },
          { path: "aik/sources", element: <KnowledgePage /> },
          { path: "aik/review", element: <PlaceholderPage title="知识审核台" pr="PR-V2-05" /> },
          { path: "security/baseline", element: <SecurityBaselinePage /> },
          { path: "cdss/fatigue", element: <AlertFatiguePage /> },
          { path: "security/identity-binding", element: <IdentityBindingManagement /> },
          { path: "workflow/todos", element: <WorkflowTodos /> },
          { path: "notifications", element: <NotificationList recipientId="current-user" /> },
          { path: "notifications/:notificationCode", element: <NotificationDetail /> },
          { path: "notifications/settings", element: <NotificationSettings /> },
          { path: "admin/users", element: <PlaceholderPage title="用户管理" pr="PR-V2-04" /> },
          { path: "admin/audit", element: <AuditLogList /> },
          { path: "mpi/patients", element: <MpiPatientsPage /> },
          { path: "adapter/hub", element: <AdapterHubPage /> },
          { path: "dify/workflows", element: <Navigate to="/ai-workflows" replace /> },
          { path: "ai-workflows", element: <AiWorkflowsPage /> },
          { path: "tenant/onboarding", element: <TenantOnboarding /> },
          { path: "*", element: <NotFound /> },
        ],
      },
    ],
  },
  { path: "*", element: <Navigate to="/login" replace /> },
];
