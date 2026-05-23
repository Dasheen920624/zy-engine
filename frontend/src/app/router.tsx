import { lazy, Suspense } from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import { Spin } from "antd";
import { AppLayout } from "@/widgets/AppLayout";

const Dashboard = lazy(() => import("@/pages/Dashboard"));
const Login = lazy(() => import("@/pages/Login"));
const NotFound = lazy(() => import("@/pages/NotFound"));
const StepFlowDemo = lazy(() => import("@/pages/StepFlowDemo"));

export function AppRouter() {
  return (
    <Suspense fallback={<Spin size="large" style={{ display: "block", margin: "20vh auto" }} />}>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route element={<AppLayout />}>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<Dashboard />} />
          {/* GA-PROD-03 7 步流模板演示页（W3 业务域接入前的可视证据） */}
          <Route path="/config/packages" element={<StepFlowDemo />} />
          <Route path="*" element={<NotFound />} />
        </Route>
      </Routes>
    </Suspense>
  );
}
