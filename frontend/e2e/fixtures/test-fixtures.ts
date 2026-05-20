import { test as base, expect } from '@playwright/test';

type TestFixtures = {
  authenticatedPage: import('@playwright/test').Page;
  apiContext: import('@playwright/test').APIRequestContext;
};

export const test = base.extend<TestFixtures>({
  authenticatedPage: async ({ page }, use) => {
    // Login via API and inject token
    const response = await page.request.post('/api/auth/login', {
      data: { username: 'admin', password: 'admin123' },
    });
    if (response.ok()) {
      const data = await response.json();
      const token = data?.data?.token;
      if (token) {
        await page.evaluate((t) => {
          localStorage.setItem('token', t);
        }, token);
      }
    }
    await use(page);
  },

  apiContext: async ({ request }, use) => {
    await use(request);
  },
});

export { expect };
