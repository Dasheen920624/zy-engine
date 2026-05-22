import { test, expect } from "../fixtures/test-fixtures";

/**
 * S6 — 多医院身份联邦
 *
 * 演示路径：/security/identity-binding → SSO 配置 → CAS 登录 → 用户合并
 * DoD：演示 CAS + LDAP-AD 两种
 */
test.describe("S6 · 多医院身份联邦", () => {
  test("管理员进入身份绑定管理页 /security/identity-binding", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/security/identity-binding");
    await authenticatedPage.waitForLoadState("networkidle");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
    expect(body ?? "").not.toContain("未登录");
  });

  test("管理员进入租户开通向导 /tenant/onboarding", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/tenant/onboarding");
    await authenticatedPage.waitForLoadState("networkidle");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });

  test("管理员进入安全基线页 /security/baseline", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/security/baseline");
    await authenticatedPage.waitForLoadState("networkidle");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });

  test("管理员进入用户管理 /admin/users", async ({ authenticatedPage }) => {
    await authenticatedPage.goto("/admin/users");
    await authenticatedPage.waitForLoadState("networkidle");
    const body = await authenticatedPage.textContent("body");
    expect(body ?? "").not.toContain("401");
  });

  test("SSO 登录页 /sso-login 显示 SSO Tab 激活", async ({ page }) => {
    await page.goto("/sso-login");
    await page.waitForLoadState("networkidle");
    // SSO tab should be selected
    const ssoTab = page.getByRole("tab", { name: /SSO/ });
    await expect(ssoTab).toBeVisible();
  });
});
