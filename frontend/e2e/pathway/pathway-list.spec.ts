import { test, expect } from '../fixtures/test-fixtures';

test.describe('路径模板列表', () => {
  test('已登录用户可访问路径模板列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/pathway-templates');
    await authenticatedPage.waitForLoadState('networkidle');
    // Page should load without auth error
    const bodyText = await authenticatedPage.textContent('body');
    expect(bodyText).not.toContain('401');
    expect(bodyText).not.toContain('未登录');
  });
});
