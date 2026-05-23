import { test, expect } from "../fixtures/medkernel-fixtures";

/**
 * 剧本 S2 · AMI 路径发布（GA-TENANT-01）
 *
 * 医务处场景：路径配置 → 选 AMI 路径 → 发布（应自动进入灰度）
 */
test.describe("S2 · AMI 路径发布（灰度优先）", () => {
  test("医务处一键发布 AMI 路径 → 自动灰度 10%", async ({ page, goto }) => {
    await goto("/pathway/templates");

    // 找到 AMI 路径并发布
    const row = page.locator("tr:has-text('胸痛 AMI 急诊路径')");
    await expect(row).toBeVisible();
    await row.locator("button:has-text('发布')").click();

    // 成功消息含"灰度"
    await expect(page.locator("text=已进入灰度")).toBeVisible({ timeout: 10_000 });
  });

  test("路径配置页支持列管理", async ({ page, goto }) => {
    await goto("/pathway/templates");
    await page.locator("button:has-text('列管理')").click();
    await expect(page.locator("text=保存并分享")).toBeVisible();
  });
});
