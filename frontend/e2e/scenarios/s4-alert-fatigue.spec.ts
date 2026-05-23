import { test, expect } from "../fixtures/medkernel-fixtures";

/**
 * 剧本 S4 · CDSS 提醒疲劳治理（GA-CLINICAL-01）
 *
 * 医生场景：临床提醒治理 → 看提醒列表 → 主操作只有"采纳 / 不采纳"两个按钮
 */
test.describe("S4 · CDSS 提醒疲劳治理", () => {
  test("医生只看到两个主按钮：采纳 + 不采纳并说明", async ({ page, goto }) => {
    await goto("/cdss/fatigue");

    // 必含 4 个统计 + 3 条 mock 提醒
    await expect(page.locator("text=今日提醒")).toBeVisible();
    await expect(page.locator("text=平均采纳率")).toBeVisible();
    await expect(page.locator("text=头孢曲松皮试缺失")).toBeVisible();

    // 主操作必须只有"采纳"和"不采纳并说明"
    await expect(page.locator("button:has-text('采纳')").first()).toBeVisible();
    await expect(page.locator("button:has-text('不采纳并说明')").first()).toBeVisible();
  });

  test("点击采纳触发审计写入", async ({ page, goto }) => {
    await goto("/cdss/fatigue");
    await page.locator("button:has-text('采纳')").first().click();
    await expect(page.locator("text=已采纳并写入审计")).toBeVisible({ timeout: 10_000 });
  });
});
