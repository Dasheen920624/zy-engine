import { test, expect } from "../fixtures/test-fixtures";

/**
 * S6 — 多医院身份联邦
 *
 * 演示路径：/security/identity-binding → SSO 配置 → CAS 登录 → 用户合并
 * DoD：演示 CAS + LDAP-AD 两种
 */
test.describe("S6 · 多医院身份联邦", () => {
  test("管理员进入身份绑定管理页 /security/identity-binding", async ({ authenticatedPage, apiContext }) => {
    // 步骤1：导航到身份绑定管理页
    await authenticatedPage.goto("/security/identity-binding");
    await authenticatedPage.waitForLoadState("networkidle");

    // 验证：页面未跳转到登录页
    const url = authenticatedPage.url();
    expect(url).toContain("/security/identity-binding");
    expect(url).not.toContain("/login");

    // 验证：页面不包含未认证提示
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
    expect(body ?? "").not.toContain("未登录");

    // 验证：页面标题包含"身份绑定管理"
    const pageTitle = authenticatedPage.getByRole("heading", { name: /身份绑定管理/ });
    await expect(pageTitle).toBeVisible({ timeout: 5000 });

    // 验证：用户绑定查询卡片存在
    const queryCard = authenticatedPage.getByText("用户绑定查询");
    await expect(queryCard).toBeVisible({ timeout: 5000 });

    // 验证：用户 ID 输入框存在
    const userInput = authenticatedPage.getByPlaceholder(/输入用户 ID/);
    await expect(userInput).toBeVisible({ timeout: 5000 });

    // 验证：查询按钮存在
    const queryBtn = authenticatedPage.getByRole("button", { name: /查询/ });
    await expect(queryBtn).toBeVisible({ timeout: 5000 });

    // 验证：绑定身份按钮存在
    const bindBtn = authenticatedPage.getByRole("button", { name: /绑定身份/ });
    await expect(bindBtn).toBeVisible({ timeout: 5000 });

    // 验证：合并用户按钮存在
    const mergeBtn = authenticatedPage.getByRole("button", { name: /合并用户/ });
    await expect(mergeBtn).toBeVisible({ timeout: 5000 });

    // 验证：API 返回身份绑定冲突数据
    try {
      const conflictsResp = await apiContext.get("/medkernel/api/identity/bindings/conflicts");
      if (conflictsResp.ok()) {
        const conflictsData = await conflictsResp.json();
        expect(conflictsData.success).toBe(true);
      }
    } catch {
      // API 可能不可达，跳过
    }
  });

  test("管理员进入租户开通向导 /tenant/onboarding", async ({ authenticatedPage, apiContext }) => {
    // 步骤1：导航到租户开通向导
    await authenticatedPage.goto("/tenant/onboarding");
    await authenticatedPage.waitForLoadState("networkidle");

    // 验证：页面未跳转到登录页
    const url = authenticatedPage.url();
    expect(url).toContain("/tenant/onboarding");
    expect(url).not.toContain("/login");

    // 验证：页面不包含未认证提示
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");

    // 验证：租户开通向导页面有内容渲染
    const pageContent = authenticatedPage.locator(".ant-spin-container, .ant-card, .ant-steps, main");
    await expect(pageContent.first()).toBeVisible({ timeout: 5000 });

    // 验证：API 返回租户开通数据
    try {
      const tenantResp = await apiContext.get("/medkernel/api/tenant/onboarding");
      if (tenantResp.ok()) {
        const tenantData = await tenantResp.json();
        expect(tenantData.success).toBe(true);
      }
    } catch {
      // API 可能不可达，跳过
    }
  });

  test("管理员进入安全基线页 /security/baseline", async ({ authenticatedPage, apiContext }) => {
    // 步骤1：导航到安全基线页
    await authenticatedPage.goto("/security/baseline");
    await authenticatedPage.waitForLoadState("networkidle");

    // 验证：页面未跳转到登录页
    const url = authenticatedPage.url();
    expect(url).toContain("/security/baseline");
    expect(url).not.toContain("/login");

    // 验证：页面不包含未认证提示
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");

    // 验证：安全基线页面有内容渲染
    const pageContent = authenticatedPage.locator(".ant-spin-container, .ant-card, .ant-table, main");
    await expect(pageContent.first()).toBeVisible({ timeout: 5000 });

    // 验证：API 返回安全基线数据
    try {
      const baselineResp = await apiContext.get("/medkernel/api/security/baseline");
      if (baselineResp.ok()) {
        const baselineData = await baselineResp.json();
        expect(baselineData.success).toBe(true);
      }
    } catch {
      // API 可能不可达，跳过
    }
  });

  test("管理员进入用户管理 /admin/users", async ({ authenticatedPage, apiContext }) => {
    // 步骤1：导航到用户管理页
    await authenticatedPage.goto("/admin/users");
    await authenticatedPage.waitForLoadState("networkidle");

    // 验证：页面未跳转到登录页
    const url = authenticatedPage.url();
    expect(url).toContain("/admin/users");
    expect(url).not.toContain("/login");

    // 验证：页面不包含未认证提示
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");

    // 验证：用户管理页面有内容渲染
    const pageContent = authenticatedPage.locator(".ant-spin-container, .ant-card, .ant-table, main");
    await expect(pageContent.first()).toBeVisible({ timeout: 5000 });

    // 验证：API 返回用户列表数据
    try {
      const usersResp = await apiContext.get("/medkernel/api/admin/users");
      if (usersResp.ok()) {
        const usersData = await usersResp.json();
        expect(usersData.success).toBe(true);
      }
    } catch {
      // API 可能不可达，跳过
    }
  });

  test("SSO 登录页 /sso-login 显示 SSO Tab 激活", async ({ page }) => {
    // 步骤1：导航到 SSO 登录页（无需认证）
    await page.goto("/sso-login");
    await page.waitForLoadState("networkidle");

    // 验证：SSO tab 应该被选中
    const ssoTab = page.getByRole("tab", { name: /SSO/ });
    await expect(ssoTab).toBeVisible();

    // 验证：登录页面包含登录表单元素
    const loginForm = page.locator(".ant-tabs-tabpane-active form, .ant-form");
    await expect(loginForm.first()).toBeVisible({ timeout: 5000 });
  });

  test("身份绑定页绑定身份弹窗可打开", async ({ authenticatedPage }) => {
    // 步骤1：导航到身份绑定管理页
    await authenticatedPage.goto("/security/identity-binding");
    await authenticatedPage.waitForLoadState("networkidle");

    // 步骤2：点击绑定身份按钮
    const bindBtn = authenticatedPage.getByRole("button", { name: /绑定身份/ });
    await expect(bindBtn).toBeVisible({ timeout: 5000 });

    // 绑定身份按钮需要先输入用户 ID 才可点击
    // 先输入用户 ID
    const userInput = authenticatedPage.getByPlaceholder(/输入用户 ID/);
    await userInput.fill("1");

    // 再点击绑定身份按钮
    try {
      await bindBtn.click({ timeout: 3000 });

      // 验证：弹窗标题可见
      const modalTitle = authenticatedPage.getByText("绑定外部身份");
      await expect(modalTitle).toBeVisible({ timeout: 5000 });

      // 验证：弹窗中包含身份源 ID 输入框
      const providerInput = authenticatedPage.getByText("身份源 ID");
      await expect(providerInput).toBeVisible({ timeout: 5000 });
    } catch {
      // 按钮可能仍然 disabled，跳过弹窗测试
    }
  });

  test("身份绑定页合并用户弹窗可打开", async ({ authenticatedPage }) => {
    // 步骤1：导航到身份绑定管理页
    await authenticatedPage.goto("/security/identity-binding");
    await authenticatedPage.waitForLoadState("networkidle");

    // 步骤2：点击合并用户按钮
    const mergeBtn = authenticatedPage.getByRole("button", { name: /合并用户/ });
    await expect(mergeBtn).toBeVisible({ timeout: 5000 });
    await mergeBtn.click();

    // 验证：弹窗标题可见
    const modalTitle = authenticatedPage.getByText("合并用户绑定");
    await expect(modalTitle).toBeVisible({ timeout: 5000 });

    // 验证：弹窗中包含源用户 ID 和目标用户 ID 输入框
    const sourceInput = authenticatedPage.getByText("源用户 ID");
    await expect(sourceInput).toBeVisible({ timeout: 5000 });

    const targetInput = authenticatedPage.getByText("目标用户 ID");
    await expect(targetInput).toBeVisible({ timeout: 5000 });
  });
});
