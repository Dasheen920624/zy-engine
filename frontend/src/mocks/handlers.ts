import { http, HttpResponse } from "msw";
import { ApiResult, SystemProviders } from "../api/types";

function wrap<T>(data: T, traceId = "fe-mock-trace"): ApiResult<T> {
  return {
    success: true,
    code: "SUCCESS",
    message: "OK",
    data,
    trace_id: traceId,
  };
}

const baseURL = import.meta.env.VITE_API_BASE_URL || "/zy-engine/api";

export const handlers = [
  http.get(`${baseURL}/system/providers`, () => {
    const payload: SystemProviders = {
      run_mode: "HYBRID",
      providers: [
        { name: "Database", ready: true, provider: "Oracle", reason: undefined },
        { name: "Graph", ready: false, provider: "Neo4jProvider", reason: "Neo4j 不可达，已降级 DB Fallback" },
        { name: "Dify", ready: true, provider: "DifyProvider", reason: undefined },
        { name: "Adapter", ready: true, provider: "AdapterMock", reason: undefined },
      ],
      timestamp: new Date().toISOString(),
    };
    return HttpResponse.json(wrap(payload));
  }),

  http.get(`${baseURL}/system/org-context`, () => {
    return HttpResponse.json(
      wrap({
        tenant_id: "TENANT_DEMO",
        hospital_code: "HOSPITAL_DEMO",
        department_code: "DEPT_CARDIOLOGY",
        inheritance_order: ["DEPARTMENT", "SITE", "CAMPUS", "HOSPITAL", "GROUP", "PLATFORM"],
      }),
    );
  }),
];
