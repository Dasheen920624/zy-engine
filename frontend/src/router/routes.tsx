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
          { path: "provenance", element: <ProvenancePlaceholder /> },
          { path: "system/providers", element: <ProvidersStatus /> },
          { path: "*", element: <NotFound /> },
        ],
      },
    ],
  },
  { path: "*", element: <Navigate to="/login" replace /> },
];
