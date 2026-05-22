import { test as base, expect } from "@playwright/test";

/**
 * 组织上下文配置，用于注入请求 Header。
 * 与前端 api/client.ts 中的 applyOrgHeaders 保持一致。
 */
export interface OrgContext {
  tenant_id?: string;
  group_code?: string;
  hospital_code?: string;
  campus_code?: string;
  site_code?: string;
  department_code?: string;
}

type TestFixtures = {
  authenticatedPage: import("@playwright/test").Page;
  apiContext: import("@playwright/test").APIRequestContext;
  orgContext: OrgContext;
};

/** 默认组织上下文（演示租户） */
const DEFAULT_ORG_CONTEXT: OrgContext = {
  tenant_id: "TENANT_DEMO",
  group_code: "GROUP_DEMO",
  hospital_code: "HOSPITAL_001",
};

export const test = base.extend<TestFixtures>({
  /** 组织上下文 fixture，测试可覆盖 */
  // eslint-disable-next-line no-empty-pattern
  orgContext: async ({}, use: (v: OrgContext) => Promise<void>) => {
    await use(DEFAULT_ORG_CONTEXT);
  },

  /**
   * 已认证页面 fixture。
   *
   * 流程：
   * 1. 通过 /api/auth/login 获取 JWT token
   * 2. 将 token 写入 localStorage（与前端 store/auth.ts 一致）
   * 3. 将组织上下文写入 localStorage（与前端 store/orgContext.ts 一致）
   * 4. 注入额外请求 Header，确保 API 请求携带组织上下文
   */
  authenticatedPage: async ({ page, orgContext }, use) => {
    // 通过 API 登录获取 token
    const response = await page.request.post("/medkernel/api/auth/login", {
      data: { username: "admin", password: "admin123" },
    });

    let token: string | undefined;
    if (response.ok()) {
      const data = await response.json();
      token = data?.data?.token;
    }

    // 注入 token 到 localStorage（前端 getToken 读取的 key）
    if (token) {
      await page.evaluate((t) => {
        localStorage.setItem("token", t);
      }, token);
    }

    // 注入组织上下文到 localStorage（前端 getOrgContext 读取的 key）
    await page.evaluate((org) => {
      localStorage.setItem("orgContext", JSON.stringify(org));
    }, orgContext);

    // 为页面所有请求自动附加组织上下文 Header
    await page.route("**/medkernel/api/**", async (route) => {
      const headers: Record<string, string> = {
        ...route.request().headers(),
      };
      if (token) {
        headers["Authorization"] = `Bearer ${token}`;
      }
      if (orgContext.tenant_id) headers["X-Tenant-Id"] = orgContext.tenant_id;
      if (orgContext.group_code) headers["X-Group-Code"] = orgContext.group_code;
      if (orgContext.hospital_code) headers["X-Hospital-Code"] = orgContext.hospital_code;
      if (orgContext.campus_code) headers["X-Campus-Code"] = orgContext.campus_code;
      if (orgContext.site_code) headers["X-Site-Code"] = orgContext.site_code;
      if (orgContext.department_code) headers["X-Department-Code"] = orgContext.department_code;
      await route.continue({ headers });
    });

    await use(page);
  },

  /**
   * API 请求上下文 fixture。
   *
   * 自动携带 token 和组织上下文 Header，用于直接调用后端 API 验证数据。
   */
  apiContext: async ({ request, orgContext }, use) => {
    // 先登录获取 token
    const loginResp = await request.post("/medkernel/api/auth/login", {
      data: { username: "admin", password: "admin123" },
    });

    let token: string | undefined;
    if (loginResp.ok()) {
      const data = await loginResp.json();
      token = data?.data?.token;
    }

    // 构建带认证和组织上下文的 headers
    const headers: Record<string, string> = {};
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }
    if (orgContext.tenant_id) headers["X-Tenant-Id"] = orgContext.tenant_id;
    if (orgContext.group_code) headers["X-Group-Code"] = orgContext.group_code;
    if (orgContext.hospital_code) headers["X-Hospital-Code"] = orgContext.hospital_code;
    if (orgContext.campus_code) headers["X-Campus-Code"] = orgContext.campus_code;
    if (orgContext.site_code) headers["X-Site-Code"] = orgContext.site_code;
    if (orgContext.department_code) headers["X-Department-Code"] = orgContext.department_code;

    // 创建带默认 headers 的 API 上下文
    const apiCtx = await request.context().then(() => request);

    // 通过给每个请求附加 headers 的方式实现
    // Playwright 的 request fixture 已自带 baseURL，这里直接使用
    // 但我们需要在每次调用时手动传入 headers
    // 为简化使用，将 headers 挂到 apiContext 上
    const enhancedApiContext = Object.assign(apiCtx, { _defaultHeaders: headers });

    await use(enhancedApiContext as never);
  },
});

export { expect };

/**
 * 辅助函数：获取带认证和组织上下文 headers 的请求选项。
 * 用于 apiContext 调用时传入。
 */
export function apiHeaders(orgContext: OrgContext, token?: string): Record<string, string> {
  const headers: Record<string, string> = {};
  if (token) headers["Authorization"] = `Bearer ${token}`;
  if (orgContext.tenant_id) headers["X-Tenant-Id"] = orgContext.tenant_id;
  if (orgContext.group_code) headers["X-Group-Code"] = orgContext.group_code;
  if (orgContext.hospital_code) headers["X-Hospital-Code"] = orgContext.hospital_code;
  if (orgContext.campus_code) headers["X-Campus-Code"] = orgContext.campus_code;
  if (orgContext.site_code) headers["X-Site-Code"] = orgContext.site_code;
  if (orgContext.department_code) headers["X-Department-Code"] = orgContext.department_code;
  return headers;
}
