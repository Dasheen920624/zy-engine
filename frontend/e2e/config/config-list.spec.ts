import { test, expect } from '../fixtures/test-fixtures';

test.describe('配置包列表', () => {
  test('已登录用户可访问配置包列表', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/config-packages');
    await authenticatedPage.waitForLoadState('networkidle');
    const bodyText = await authenticatedPage.textContent('body');
    expect(bodyText).not.toContain('401');
    expect(bodyText).not.toContain('未登录');
  });
});
