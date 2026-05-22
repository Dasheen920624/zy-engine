import { Navigate } from "react-router-dom";
import type { RouteObject } from "react-router-dom";
import AppLayout from "../layouts/AppLayout";
import Dashboard from "../pages/Dashboard";
import ProvidersStatus from "../pages/ProvidersStatus";
import DemoValidation from "../pages/DemoValidation";
import ConfigPackages from "../pages/ConfigPackages";
import PackageImportWizard from "../pages/ConfigPackages/PackageImportWizard";
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
import { MappingWorkbench } from "../pages/Terminology";
import {
  PathwayList,
  PathwayDetail,
  PathwayDiff,
  PathwayEditor,
  PatientPathwayList,
  PatientPathwayDetail,
} from "../pages/Pathway";
import { RuleList, RuleDetail, RuleEditor, RuleValidate } from "../pages/Rule";
import { GraphExplore } from "../pages/Graph";
import { AiKnowledgeReview } from "../pages/AiKnowledge";
import { InsuranceAudit } from "../pages/Insurance";
import { ProvenancePage } from "../pages/Provenance";
import { AiWorkflowsPage } from "../pages/AiWorkflows";
import TenantOnboarding from "../pages/Tenant/Onboarding";
import { ImplementationGuidePage } from "../pages/Onboarding";
import { MpiPatientsPage } from "../pages/Mpi";
import { AuditLogList } from "../pages/Admin/AuditLog";
import { UserManagementPage } from "../pages/Admin/UserManagement";
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
          { path: "provenance", element: <ProvenancePage /> },
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
          { path: "rule/validate", element: <RuleValidate /> },
          { path: "graph/explore", element: <GraphExplore /> },
          { path: "terminology/mapping", element: <MappingWorkbench /> },
          { path: "qc/alerts", element: <AlertList /> },
          { path: "qc/dashboard", element: <QualityDashboard /> },
          { path: "qc/eval/sets", element: <EvalIndicatorSetList /> },
          { path: "qc/eval/results", element: <EvalResultList /> },
          { path: "qc/eval/reports", element: <EvalReportPage /> },
          { path: "qc/department/:deptCode", element: <DepartmentDrillDown /> },
          { path: "qc/insurance", element: <InsuranceAudit /> },
          { path: "aik/sources", element: <KnowledgePage /> },
          { path: "aik/review", element: <AiKnowledgeReview /> },
          { path: "security/baseline", element: <SecurityBaselinePage /> },
          { path: "cdss/fatigue", element: <AlertFatiguePage /> },
          { path: "security/identity-binding", element: <IdentityBindingManagement /> },
          { path: "workflow/todos", element: <WorkflowTodos /> },
          { path: "notifications", element: <NotificationList recipientId="current-user" /> },
          { path: "notifications/:notificationCode", element: <NotificationDetail /> },
          { path: "notifications/settings", element: <NotificationSettings /> },
          { path: "admin/users", element: <UserManagementPage /> },
          { path: "admin/audit", element: <AuditLogList /> },
          { path: "mpi/patients", element: <MpiPatientsPage /> },
          { path: "adapter/hub", element: <AdapterHubPage /> },
          { path: "dify/workflows", element: <Navigate to="/ai-workflows" replace /> },
          { path: "ai-workflows", element: <AiWorkflowsPage /> },
          { path: "tenant/onboarding", element: <TenantOnboarding /> },
          { path: "onboarding/implementation-guide", element: <ImplementationGuidePage /> },
          { path: "*", element: <NotFound /> },
        ],
      },
    ],
  },
  { path: "*", element: <Navigate to="/login" replace /> },
];
