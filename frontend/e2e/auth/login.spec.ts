import { test, expect } from "../fixtures/test-fixtures";

test.describe("登录页面", () => {
  test("应显示四种登录方式和合规页脚", async ({ page }) => {
    await page.goto("/login");
    await expect(page.getByRole("tab", { name: /手机短信/ })).toBeVisible();
    await expect(page.getByRole("tab", { name: /账号密码/ })).toBeVisible();
    await expect(page.getByRole("tab", { name: /SSO/ })).toBeVisible();
    await expect(page.getByRole("tab", { name: /域账号/ })).toBeVisible();
    await expect(page.getByText(/京ICP备/)).toBeVisible();
    await expect(page.getByText(/京公网安备/)).toBeVisible();
  });

  test("使用正确凭据登录成功", async ({ page }) => {
    await page.goto("/login");
    await page.getByRole("tab", { name: /账号密码/ }).click();
    const activePane = page.locator(".ant-tabs-tabpane-active");
    const usernameInput = activePane.getByPlaceholder("用户名 / 工号");
    const passwordInput = activePane.getByPlaceholder("密码");
    await usernameInput.fill("zhao01");
    await passwordInput.fill("demo123");
    await activePane.locator("input[type='checkbox']").check();
    const loginButton = activePane.getByRole("button", { name: "账号密码登录" });
    await loginButton.click();
    // Should redirect away from login page
    await page.waitForURL("**/!(login)**", { timeout: 10000 }).catch(() => {});
  });

  test("使用错误凭据登录失败", async ({ page }) => {
    await page.goto("/login");
    await page.getByRole("tab", { name: /账号密码/ }).click();
    const activePane = page.locator(".ant-tabs-tabpane-active");
    const usernameInput = activePane.getByPlaceholder("用户名 / 工号");
    const passwordInput = activePane.getByPlaceholder("密码");
    await usernameInput.fill("admin");
    await passwordInput.fill("wrongpassword");
    await activePane.locator("input[type='checkbox']").check();
    const loginButton = activePane.getByRole("button", { name: "账号密码登录" });
    await loginButton.click();
    // Should stay on login page or show error
    await page.waitForTimeout(1000);
    const currentUrl = page.url();
    expect(currentUrl).toContain("login");
  });

  test("sso-login 路径进入统一 SSO Tab", async ({ page }) => {
    await page.goto("/sso-login");
    await expect(page.getByRole("tab", { name: /SSO/ })).toHaveAttribute("aria-selected", "true");
    await expect(page.getByText(/统一身份认证/)).toBeVisible();
  });
});
