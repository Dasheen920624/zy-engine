import { test as base, expect } from "@playwright/test";

/**
 * MedKernel E2E 6 大客户剧本通用 fixture。
 *
 * 提供：
 * - 默认 mock 用户身份（医务处张三）
 * - 路由跳转助手
 * - 通用断言（菜单可见 / 标题正确 / 无 console error）
 */
export const test = base.extend<{
  goto: (path: string) => Promise<void>;
}>({
  goto: async ({ page }, use) => {
    await use(async (path: string) => {
      await page.goto(path);
      // 等到 AppLayout 渲染完毕
      await page.waitForSelector('[class*="ant-layout"]', { timeout: 10_000 });
    });
  },
});

export { expect };
