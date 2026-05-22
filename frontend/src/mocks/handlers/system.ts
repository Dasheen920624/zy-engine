import { http, HttpResponse } from "msw";
import { wrap, baseURL, demoUser } from "./shared";
import type { SystemProviders } from "../../api/types";

export const ssoHandlers = [
  http.get(`${baseURL}/security/sso/providers`, () => HttpResponse.json(wrap([
    {
      id: 1,
      providerCode: "cas-main",
      providerName: "集团 CAS 统一认证",
      providerType: "CAS",
      status: "ACTIVE",
      adapterCode: "cas",
    },
    {
      id: 2,
      providerCode: "ldap-ad",
      providerName: "院内 LDAP-AD",
      providerType: "LDAP-AD",
      status: "ACTIVE",
      adapterCode: "ldap",
    },
  ]))),

  http.post(`${baseURL}/security/sso/providers/:providerId/initiate`, ({ params }) => HttpResponse.json(wrap({
    providerId: Number(params.providerId),
    providerType: "CAS",
    redirectUrl: "/sso-login?providerId=1&code=mock-code&state=mock-state",
    state: "mock-state",
  }))),

  http.get(`${baseURL}/security/sso/callback`, () => HttpResponse.json(wrap({
    token: "mock-jwt-sso-demo",
    user: demoUser,
  }))),

  http.post(`${baseURL}/security/sso/ldap/authenticate`, async ({ request }) => {
    const body = (await request.json().catch(() => ({}))) as { username?: string; password?: string };
    if (body.username && body.password) {
      return HttpResponse.json(
        wrap({
          token: "mock-jwt-ldap-demo",
          user: demoUser,
        }),
      );
    }
    return HttpResponse.json(
      {
        success: false,
        code: "LDAP_AUTH_FAILED",
        message: "LDAP 认证失败",
        data: null,
        trace_id: "mock-ldap-failed",
      },
      { status: 401 },
    );
  }),
];

export const systemHandlers = [
  http.get(`${baseURL}/system/providers`, () => {
    const payload: SystemProviders = {
      run_mode: "HYBRID",
      providers: [
        {
          name: "Database",
          role: "CONFIG_PRIMARY_STORE",
          ready: true,
          status: "READY",
          provider: "Oracle",
          reason: undefined,
        },
        {
          name: "Graph",
          role: "GRAPH_QUERY_PROVIDER",
          ready: false,
          status: "FALLBACK",
          provider: "Neo4jProvider",
          reason: "Neo4j 不可达，已降级 DB Fallback",
        },
        {
          name: "Dify",
          role: "WORKFLOW_PROVIDER",
          ready: true,
          status: "READY",
          provider: "DifyProvider",
          reason: undefined,
        },
        {
          name: "Adapter",
          role: "HOSPITAL_ADAPTER_PROVIDER",
          ready: true,
          status: "READY",
          provider: "AdapterMock",
          reason: undefined,
        },
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
