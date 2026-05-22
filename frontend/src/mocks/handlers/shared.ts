import type {
  ApiResult,
  UserInfo,
} from "../../api/types";

export function wrap<T>(data: T, traceId = "fe-mock-trace"): ApiResult<T> {
  return {
    success: true,
    code: "SUCCESS",
    message: "OK",
    data,
    trace_id: traceId,
  };
}

export const baseURL = import.meta.env.VITE_API_BASE_URL || "/medkernel/api";

export const demoUser: UserInfo = {
  id: 1002,
  tenant_id: 1,
  username: "zhao01",
  display_name: "赵医生",
  email: "zhao01@example.org",
  phone: "13800000002",
  status: "ACTIVE",
  roles: ["DOCTOR"],
  permissions: [
    "dashboard:view",
    "demo-validation:run",
    "config-package:view",
    "config-package:review",
    "source:view",
  ],
  org_scopes: [
    {
      scope_level: "HOSPITAL",
      scope_code: "HOSPITAL_DEMO",
      scope_name: "演示医院",
    },
  ],
  last_login_time: new Date().toISOString(),
};
