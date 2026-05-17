import { Navigate, RouteObject } from "react-router-dom";
import AppLayout from "../layouts/AppLayout";
import Dashboard from "../pages/Dashboard";
import ProvidersStatus from "../pages/ProvidersStatus";
import DemoValidation from "../pages/DemoValidation";
import ConfigPackages from "../pages/ConfigPackages";
import ProvenancePlaceholder from "../pages/ProvenancePlaceholder";
import NotFound from "../pages/NotFound";

export const routes: RouteObject[] = [
  {
    path: "/",
    element: <AppLayout />,
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },
      { path: "dashboard", element: <Dashboard /> },
      { path: "demo-validation", element: <DemoValidation /> },
      { path: "config-packages", element: <ConfigPackages /> },
      { path: "provenance", element: <ProvenancePlaceholder /> },
      { path: "system/providers", element: <ProvidersStatus /> },
      { path: "*", element: <NotFound /> },
    ],
  },
];
