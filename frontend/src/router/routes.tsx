import { Navigate, RouteObject } from "react-router-dom";
import AppLayout from "../layouts/AppLayout";
import Dashboard from "../pages/Dashboard";
import ProvidersStatus from "../pages/ProvidersStatus";
import DemoValidationPlaceholder from "../pages/DemoValidationPlaceholder";
import ConfigPackagesPlaceholder from "../pages/ConfigPackagesPlaceholder";
import ProvenancePlaceholder from "../pages/ProvenancePlaceholder";
import NotFound from "../pages/NotFound";

export const routes: RouteObject[] = [
  {
    path: "/",
    element: <AppLayout />,
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },
      { path: "dashboard", element: <Dashboard /> },
      { path: "demo-validation", element: <DemoValidationPlaceholder /> },
      { path: "config-packages", element: <ConfigPackagesPlaceholder /> },
      { path: "provenance", element: <ProvenancePlaceholder /> },
      { path: "system/providers", element: <ProvidersStatus /> },
      { path: "*", element: <NotFound /> },
    ],
  },
];
