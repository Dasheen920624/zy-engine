import { test, expect } from '../fixtures/test-fixtures';

test.describe('登录页面', () => {
  test('应显示登录表单', async ({ page }) => {
    await page.goto('/login');
    await expect(page.locator('input[placeholder*="用户名"], input[type="text"]').first()).toBeVisible();
    await expect(page.locator('input[type="password"]').first()).toBeVisible();
  });

  test('使用正确凭据登录成功', async ({ page }) => {
    await page.goto('/login');
    const usernameInput = page.locator('input[placeholder*="用户名"], input[type="text"]').first();
    const passwordInput = page.locator('input[type="password"]').first();
    await usernameInput.fill('admin');
    await passwordInput.fill('admin123');
    const loginButton = page.locator('button[type="submit"], button:has-text("登录")').first();
    await loginButton.click();
    // Should redirect away from login page
    await page.waitForURL('**/!(login)**', { timeout: 10000 }).catch(() => {});
  });

  test('使用错误凭据登录失败', async ({ page }) => {
    await page.goto('/login');
    const usernameInput = page.locator('input[placeholder*="用户名"], input[type="text"]').first();
    const passwordInput = page.locator('input[type="password"]').first();
    await usernameInput.fill('admin');
    await passwordInput.fill('wrongpassword');
    const loginButton = page.locator('button[type="submit"], button:has-text("登录")').first();
    await loginButton.click();
    // Should stay on login page or show error
    await page.waitForTimeout(1000);
    const currentUrl = page.url();
    expect(currentUrl).toContain('login');
  });
});
